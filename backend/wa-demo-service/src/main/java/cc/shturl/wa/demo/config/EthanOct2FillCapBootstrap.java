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
 * 2026-10-02 Ethan 当天已有 2 场有效对局，补打仍写了 3 场胜利，超过每日前 3 局。
 * 删掉多出来的 2 场，收回 2 场胜和 200 经验。
 * 多出来的队友让 9 月 28 日那周凑满 10 人。队友改回 9 人，已经领走的 500 不收回。
 */
@Component
@Order(48)
public class EthanOct2FillCapBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EthanOct2FillCapBootstrap.class);
    static final String PATCH_ID = "ethan_oct2_fill_over_cap_20261002";
    private static final long ETHAN_ID = 33L;
    private static final String WEEK = "2026-09-28";
    private static final String LEGIT_TEAMMATES = "[9,10,11,27,12,26,13,28,15]";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public EthanOct2FillCapBootstrap(JdbcTemplate jdbcTemplate,
                                      PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("matches") || !tableExists("user_profiles") || !tableExists("user_tasks")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (repair()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Ethan Oct 2 fill cap repair failed.", e);
        }
    }

    private boolean repair() {
        jdbcTemplate.update("""
                DELETE mp FROM match_players mp
                INNER JOIN matches m ON m.id = mp.match_id
                WHERE m.match_code IN ('AE202610022', 'AE202610023')
                  AND m.winner_type = 1
                """);
        int removed = jdbcTemplate.update("""
                DELETE FROM matches
                WHERE match_code IN ('AE202610022', 'AE202610023')
                  AND winner_type = 1
                """);
        if (removed != 2) {
            log.warn("Skip Ethan Oct 2 fill repair; removed {} extra matches.", removed);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = GREATEST(IFNULL(win_count, 0) - 2, 0),
                    exp = GREATEST(IFNULL(exp, 0) - 200, 0)
                WHERE user_id = ?
                """, ETHAN_ID);
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.progress_value = 9,
                    ut.extra_data = CAST(? AS JSON),
                    ut.status = 3,
                    ut.completed_at = IFNULL(ut.completed_at, '2026-10-05 02:50:57'),
                    ut.claimed_at = IFNULL(ut.claimed_at, '2026-10-05 02:50:57'),
                    ut.updated_at = NOW()
                WHERE ut.user_id = ?
                  AND ut.period_key = ?
                  AND t.task_code = 'T-WEEKLY-TEAM-10'
                  AND ut.status >= 1
                """, LEGIT_TEAMMATES, ETHAN_ID, WEEK);
        log.warn("Removed Ethan Oct 2 extra fills. Sep 28 weekly gold stays claimed.");
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
