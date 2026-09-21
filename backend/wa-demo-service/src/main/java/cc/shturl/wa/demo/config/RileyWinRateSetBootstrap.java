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
import java.util.Map;

/**
 * 将 Riley 排行榜胜率一次性改为 100%（28 胜 0 负）。
 */
@Component
@Order(12)
public class RileyWinRateSetBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RileyWinRateSetBootstrap.class);
    private static final String PATCH_ID = "set_riley_winrate_100_20260921";
    private static final int TARGET_WINS = 28;
    private static final int TARGET_LOSSES = 0;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RileyWinRateSetBootstrap(JdbcTemplate jdbcTemplate,
                                    PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("users") || !tableExists("user_profiles")) {
            return;
        }
        ensurePatchTable();
        if (patchApplied()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            setRileyWinRate();
            markPatchApplied();
        });
    }

    private void setRileyWinRate() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.id AS user_id, u.username,
                       IFNULL(p.win_count, 0) AS win_count,
                       IFNULL(p.lose_count, 0) AS lose_count,
                       IFNULL(p.draw_count, 0) AS draw_count
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE LOWER(u.username) = 'riley'
                   OR LOWER(IFNULL(p.display_name, '')) = 'riley'
                ORDER BY CASE WHEN LOWER(u.username) = 'riley' THEN 0 ELSE 1 END, u.id
                LIMIT 1
                """);
        if (rows.isEmpty()) {
            log.warn("Set Riley win-rate skipped; user not found.");
            return;
        }
        Map<String, Object> row = rows.get(0);
        long userId = ((Number) row.get("user_id")).longValue();
        jdbcTemplate.update("""
                INSERT INTO user_profiles (user_id, display_name, win_count, lose_count, draw_count, money, created_at, updated_at)
                SELECT u.id, u.username, 0, 0, 0, 0, NOW(), NOW()
                FROM users u
                WHERE u.id = ?
                  AND NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id)
                """, userId);
        int updated = jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = ?,
                    lose_count = ?,
                    draw_count = 0
                WHERE user_id = ?
                """, TARGET_WINS, TARGET_LOSSES, userId);
        log.warn("Set Riley win-rate user={} id={}: {}/{}/{} -> {}/{}/0, rows={}",
                row.get("username"), userId,
                row.get("win_count"), row.get("lose_count"), row.get("draw_count"),
                TARGET_WINS, TARGET_LOSSES, updated);
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
