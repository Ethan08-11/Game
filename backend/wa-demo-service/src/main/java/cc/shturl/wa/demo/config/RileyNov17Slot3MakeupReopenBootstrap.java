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

/**
 * Riley 2026-11-22 的 2840 局是补 11 月 17 日第 3 局，当天正常局只有 2 局。
 * 每日任务被标成已领后，这档完成金币被记成 0。改回可领取，不直接入账。
 */
@Component
@Order(62)
public class RileyNov17Slot3MakeupReopenBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RileyNov17Slot3MakeupReopenBootstrap.class);
    static final String PATCH_ID = "riley_nov17_slot3_makeup_reopen";
    private static final long RILEY_ID = 30L;
    private static final long MATCH_ID = 2840L;
    private static final int FINISH_GOLD = 50;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RileyNov17Slot3MakeupReopenBootstrap(JdbcTemplate jdbcTemplate,
                                                PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("user_makeup_claims") || !tableExists("matches")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (reopen()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Riley Nov 17 slot 3 makeup reopen failed.", e);
        }
    }

    private boolean reopen() {
        Integer match = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM matches m
                INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                WHERE m.id = ?
                  AND m.status = 2
                  AND IFNULL(m.winner_type, 0) = 2
                  AND me.makeup_owed_day = '2026-11-17'
                  AND me.makeup_slot = 3
                  AND (
                    SELECT COUNT(*)
                    FROM matches n
                    INNER JOIN match_players np ON np.match_id = n.id AND np.user_id = ?
                    WHERE n.status = 2
                      AND IFNULL(n.winner_type, 0) IN (1, 2)
                      AND np.makeup_slot IS NULL
                      AND DATE(DATE_ADD(COALESCE(n.ended_at, n.started_at, n.created_at), INTERVAL 8 HOUR))
                          = '2026-11-17'
                  ) < 3
                """, Integer.class, RILEY_ID, MATCH_ID, RILEY_ID);
        if (match == null || match != 1) {
            log.warn("Skip Riley makeup reopen; match {} is not the uncovered Nov 17 slot 3 loss.", MATCH_ID);
            return false;
        }
        int updated = jdbcTemplate.update("""
                UPDATE user_makeup_claims
                SET finish_gold = ?,
                    win_gold = 0,
                    finish_claimed = 0,
                    win_claimed = 1,
                    gold_paid = 0
                WHERE user_id = ?
                  AND match_id = ?
                  AND owed_day = '2026-11-17'
                  AND slot_no = 3
                  AND gold_paid = 0
                  AND finish_gold = 0
                  AND finish_claimed = 1
                """, FINISH_GOLD, RILEY_ID, MATCH_ID);
        if (updated != 1) {
            log.warn("Skip Riley makeup reopen; claim for match {} was not the closed zero-gold row.", MATCH_ID);
            return false;
        }
        log.warn("Reopened Riley match {} as claimable makeup finish gold {} for 2026-11-17 slot 3.",
                MATCH_ID, FINISH_GOLD);
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
