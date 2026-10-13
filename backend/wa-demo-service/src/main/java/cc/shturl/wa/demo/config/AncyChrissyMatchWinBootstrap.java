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
 * 把 Ancy / Chrissy 2026-10-08 的 1970、1971 从失败改成胜利。
 * 两把都已经发过失败经验 30，胜利应为 100，每把补 70。
 * 1970 是 Ancy 当天第 3 把、Chrissy 当天第 2 把，各补赢局 10 金币。
 * 1971 是 Ancy 当天第 4 把，不补金币。
 */
@Component
@Order(45)
public class AncyChrissyMatchWinBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AncyChrissyMatchWinBootstrap.class);
    static final String PATCH_ID = "ancy_chrissy_matches_1970_1971_win_20261008";
    private static final int EXP_DELTA = 70;
    private static final int WIN_GOLD = 10;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AncyChrissyMatchWinBootstrap(JdbcTemplate jdbcTemplate,
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
            log.error("Ancy/Chrissy win restore failed.", e);
        }
    }

    private boolean restore() {
        int updated = jdbcTemplate.update("""
                UPDATE matches
                SET winner_type = 1,
                    phase = 'FINISHED',
                    status = 2
                WHERE id IN (1970, 1971) AND status = 2 AND winner_type = 2
                """);
        if (updated != 2) {
            log.warn("Skip Ancy/Chrissy win restore; updated {} matches.", updated);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE match_players
                SET result_type = 1,
                    player_status = 'ACTIVE'
                WHERE match_id IN (1970, 1971) AND result_type = 2
                """);
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = IFNULL(win_count, 0) + 2,
                    lose_count = GREATEST(0, IFNULL(lose_count, 0) - 2),
                    exp = IFNULL(exp, 0) + ?
                WHERE user_id IN (10, 13)
                """, EXP_DELTA * 2);
        grantWinGold(10L, "T-DAILY-WIN-3");
        grantWinGold(13L, "T-DAILY-WIN-2");
        log.warn("Restored matches 1970 and 1971 as wins for Ancy and Chrissy.");
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
                  AND ut.period_key = '2026-10-08'
                  AND t.task_code = ?
                  AND ut.status < 3
                """, userId, taskCode);
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
