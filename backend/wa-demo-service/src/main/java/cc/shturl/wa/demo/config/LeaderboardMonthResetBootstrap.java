package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.impl.LeaderboardServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;

/**
 * 本月排行榜一次性清零（金币、胜/负/平）。之后每月 1 日由 {@link LeaderboardServiceImpl#ensureCurrentMonth()} 自动清。
 */
@Component
@Order(5)
public class LeaderboardMonthResetBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardMonthResetBootstrap.class);
    private static final String PATCH_ID = "reset_leaderboard_month_20260911";

    private final JdbcTemplate jdbcTemplate;
    private final LeaderboardServiceImpl leaderboardService;
    private final TransactionTemplate transactionTemplate;

    public LeaderboardMonthResetBootstrap(JdbcTemplate jdbcTemplate,
                                          LeaderboardServiceImpl leaderboardService,
                                          PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.leaderboardService = leaderboardService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("user_profiles")) {
            return;
        }
        ensurePatchTable();
        if (patchApplied()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            int reset = leaderboardService.resetMonthStats();
            if (tableExists("leaderboard_week")) {
                jdbcTemplate.update(
                        "UPDATE leaderboard_week SET week_start = ? WHERE id = 1",
                        Date.valueOf(LeaderboardServiceImpl.currentMonthStart()));
            }
            markPatchApplied();
            log.info("Reset this month's leaderboard for {} profiles.", reset);
        });
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

    private boolean patchApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_patches WHERE patch_id = ?",
                Integer.class,
                PATCH_ID);
        return count != null && count > 0;
    }

    private void markPatchApplied() {
        jdbcTemplate.update("INSERT INTO schema_patches(patch_id) VALUES (?)", PATCH_ID);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }
}
