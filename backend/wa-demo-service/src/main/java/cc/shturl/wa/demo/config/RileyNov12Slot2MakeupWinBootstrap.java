package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Riley 2026-11-20 与 Amy 的 2794 局是补 11 月 12 日第 2 局，只记 Riley。
 * 这局已经胜利，但没写成补打，被当成当天第 4 局。补上完成和胜利，发放 40 + 10，任务显示已领取。
 */
@Component
@Order(58)
public class RileyNov12Slot2MakeupWinBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RileyNov12Slot2MakeupWinBootstrap.class);
    static final String PATCH_ID = "riley_match_2794_nov12_slot2_makeup_win";
    private static final long MATCH_ID = 2794L;
    private static final long RILEY_ID = 30L;
    private static final long AMY_ID = 24L;
    private static final int FINISH_GOLD = 40;
    private static final int WIN_GOLD = 10;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RileyNov12Slot2MakeupWinBootstrap(JdbcTemplate jdbcTemplate,
                                              PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("matches") || !tableExists("match_players") || !tableExists("user_makeup_claims")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (restore()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Riley match 2794 makeup win restore failed.", e);
        }
    }

    private boolean restore() {
        Integer match = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM matches m
                WHERE m.id = ?
                  AND m.status = 2
                  AND IFNULL(m.winner_type, 0) = 1
                  AND EXISTS (SELECT 1 FROM match_players mp WHERE mp.match_id = m.id AND mp.user_id = ?)
                  AND EXISTS (SELECT 1 FROM match_players mp WHERE mp.match_id = m.id AND mp.user_id = ?)
                """, Integer.class, MATCH_ID, RILEY_ID, AMY_ID);
        if (match == null || match != 1) {
            log.warn("Skip Riley makeup restore; match {} is not the settled win.", MATCH_ID);
            return false;
        }
        int tagged = jdbcTemplate.update("""
                UPDATE match_players
                SET makeup_owed_day = '2026-11-12',
                    makeup_slot = 2
                WHERE match_id = ?
                  AND user_id = ?
                  AND makeup_slot IS NULL
                """, MATCH_ID, RILEY_ID);
        if (tagged == 1) {
            jdbcTemplate.update("""
                    UPDATE user_tasks ut
                    INNER JOIN tasks t ON t.id = ut.task_id AND t.task_code = 'T-DAILY-SLOT'
                    SET ut.progress_value = ut.progress_value - 1,
                        ut.updated_at = NOW()
                    WHERE ut.user_id = ?
                      AND ut.period_key = '2026-11-20'
                      AND ut.progress_value > 3
                    """, RILEY_ID);
        }
        List<Integer> paidRows = jdbcTemplate.query("""
                SELECT gold_paid
                FROM user_makeup_claims
                WHERE user_id = ? AND owed_day = '2026-11-12' AND slot_no = 2
                """, (rs, row) -> rs.getInt("gold_paid"), RILEY_ID);
        Integer paid = paidRows.isEmpty() ? null : paidRows.get(0);
        int already = paid == null ? 0 : paid;
        int due = FINISH_GOLD + WIN_GOLD;
        if (paid == null) {
            jdbcTemplate.update("""
                    INSERT INTO user_makeup_claims(
                        user_id, owed_day, slot_no, match_id, gold_paid,
                        finish_gold, win_gold, finish_claimed, win_claimed, created_at)
                    VALUES (?, '2026-11-12', 2, ?, ?, ?, ?, 1, 1, NOW())
                    """, RILEY_ID, MATCH_ID, due, FINISH_GOLD, WIN_GOLD);
        } else {
            jdbcTemplate.update("""
                    UPDATE user_makeup_claims
                    SET match_id = ?,
                        gold_paid = ?,
                        finish_gold = ?,
                        win_gold = ?,
                        finish_claimed = 1,
                        win_claimed = 1
                    WHERE user_id = ? AND owed_day = '2026-11-12' AND slot_no = 2
                    """, MATCH_ID, Math.max(already, due), FINISH_GOLD, WIN_GOLD, RILEY_ID);
        }
        int grant = Math.max(0, due - already);
        if (grant > 0) {
            jdbcTemplate.update("""
                    UPDATE user_profiles
                    SET money = IFNULL(money, 0) + ?,
                        weekly_money = IFNULL(weekly_money, 0) + ?,
                        money_reached_at = NOW()
                    WHERE user_id = ?
                    """, grant, grant, RILEY_ID);
        }
        log.warn("Recorded match {} as Riley's won makeup for 2026-11-12 slot 2, gold {}.", MATCH_ID, grant);
        return true;
    }

    private void ensurePatchTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS schema_patches (
                  patch_id varchar(64) NOT NULL,
                  applied_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (patch_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次性数据修复标记'
                """);
    }

    private void markPatchApplied() {
        jdbcTemplate.update("INSERT INTO schema_patches(patch_id) VALUES (?)", PATCH_ID);
    }

    private boolean patchApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_patches WHERE patch_id = ?",
                Integer.class,
                PATCH_ID);
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }
}
