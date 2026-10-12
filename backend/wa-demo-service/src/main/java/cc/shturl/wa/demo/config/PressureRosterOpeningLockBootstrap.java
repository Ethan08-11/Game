package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.LeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

/**
 * 2026-10-12 的高压名单在 0 点被写成了到手金币前五。
 * 改成当天 0 点的预测总金币前五，之后白天不再重算。
 */
@Component
@Order(43)
public class PressureRosterOpeningLockBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PressureRosterOpeningLockBootstrap.class);
    static final String PATCH_ID = "lock_pressure_roster_20261012";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 12);

    private final JdbcTemplate jdbcTemplate;
    private final LeaderboardService leaderboardService;
    private final TransactionTemplate transactionTemplate;

    public PressureRosterOpeningLockBootstrap(JdbcTemplate jdbcTemplate,
                                               LeaderboardService leaderboardService,
                                               PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.leaderboardService = leaderboardService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("leaderboard_daily_top") || !tableExists("user_profiles")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                int locked = leaderboardService.relockOpeningProjectedRoster(DAY);
                if (locked <= 0) {
                    throw new IllegalStateException("Opening pressure roster for " + DAY + " is empty.");
                }
                markPatchApplied();
                log.warn("Locked opening projected roster for {} with {} players.", DAY, locked);
            });
        } catch (Exception e) {
            log.error("Opening pressure roster lock failed.", e);
        }
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
