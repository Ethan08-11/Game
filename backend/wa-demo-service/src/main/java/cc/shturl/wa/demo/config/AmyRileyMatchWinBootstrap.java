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
 * 把 Amy / Riley 2026-10-16 的第 2202 局从失败改成胜利。
 * Amy 剩 1 血打出 Kinyond 倒下后，霸凌者出手没有进入复活，本局被记成失败。
 * 失败已经发过 30 经验，胜利应为 100，各补 70。
 * 这是 Amy 当天第 1 把、Riley 当天第 2 把，各补赢局 10 金币。
 */
@Component
@Order(47)
public class AmyRileyMatchWinBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AmyRileyMatchWinBootstrap.class);
    static final String PATCH_ID = "amy_riley_match_2202_win_20261016";
    private static final long MATCH_ID = 2202L;
    private static final long AMY_ID = 24L;
    private static final long RILEY_ID = 30L;
    private static final int EXP_DELTA = 70;
    private static final int WIN_GOLD = 10;
    private static final String PERIOD = "2026-10-16";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AmyRileyMatchWinBootstrap(JdbcTemplate jdbcTemplate,
                                      PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("matches") || !tableExists("match_players") || !tableExists("user_profiles")) {
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
            log.error("Amy/Riley match 2202 win restore failed.", e);
        }
    }

    private boolean restore() {
        int updated = jdbcTemplate.update("""
                UPDATE matches
                SET winner_type = 1,
                    phase = 'FINISHED',
                    status = 2
                WHERE id = ? AND status = 2 AND winner_type = 2
                """, MATCH_ID);
        if (updated != 1) {
            log.warn("Skip Amy/Riley restore; match {} is not a settled loss.", MATCH_ID);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE match_players
                SET result_type = 1,
                    player_status = 'ACTIVE'
                WHERE match_id = ?
                """, MATCH_ID);
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = IFNULL(win_count, 0) + 1,
                    lose_count = GREATEST(IFNULL(lose_count, 0) - 1, 0),
                    exp = IFNULL(exp, 0) + ?
                WHERE user_id IN (?, ?)
                """, EXP_DELTA, AMY_ID, RILEY_ID);
        grantWinGold(AMY_ID, "T-DAILY-WIN-1");
        grantWinGold(RILEY_ID, "T-DAILY-WIN-2");
        log.warn("Restored match {} as a win for Amy and Riley.", MATCH_ID);
        return true;
    }

    private void grantWinGold(long userId, String taskCode) {
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET money = IFNULL(money, 0) + ?,
                    weekly_money = IFNULL(weekly_money, 0) + ?,
                    money_reached_at = NOW()
                WHERE user_id = ?
                """, WIN_GOLD, WIN_GOLD, userId);
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.progress_value = 1,
                    ut.status = 3,
                    ut.completed_at = IFNULL(ut.completed_at, NOW()),
                    ut.claimed_at = IFNULL(ut.claimed_at, NOW()),
                    ut.updated_at = NOW()
                WHERE ut.user_id = ?
                  AND ut.period_key = ?
                  AND t.task_code = ?
                  AND ut.status < 3
                """, userId, PERIOD, taskCode);
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
