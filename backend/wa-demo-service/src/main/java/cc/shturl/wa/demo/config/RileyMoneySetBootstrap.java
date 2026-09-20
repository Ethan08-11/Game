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
 * 将 Riley 排行榜金币一次性改为 1700。
 */
@Component
@Order(9)
public class RileyMoneySetBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RileyMoneySetBootstrap.class);
    private static final String PATCH_ID = "set_riley_money_1700_20260920";
    private static final long TARGET_MONEY = 1700L;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RileyMoneySetBootstrap(JdbcTemplate jdbcTemplate,
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
            setRileyMoney();
            markPatchApplied();
        });
    }

    private void setRileyMoney() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.id AS user_id, u.username, IFNULL(p.money, 0) AS money
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE LOWER(u.username) = 'riley'
                   OR LOWER(IFNULL(p.display_name, '')) = 'riley'
                ORDER BY CASE WHEN LOWER(u.username) = 'riley' THEN 0 ELSE 1 END, u.id
                LIMIT 1
                """);
        if (rows.isEmpty()) {
            log.warn("Set Riley money skipped; user not found.");
            return;
        }
        Map<String, Object> row = rows.get(0);
        long userId = ((Number) row.get("user_id")).longValue();
        long before = ((Number) row.get("money")).longValue();
        jdbcTemplate.update("""
                INSERT INTO user_profiles (user_id, display_name, win_count, lose_count, draw_count, money, created_at, updated_at)
                SELECT u.id, u.username, 0, 0, 0, 0, NOW(), NOW()
                FROM users u
                WHERE u.id = ?
                  AND NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id)
                """, userId);
        int updated = jdbcTemplate.update(
                "UPDATE user_profiles SET money = ? WHERE user_id = ?",
                TARGET_MONEY, userId);
        log.warn("Set Riley money user={} id={}: {} -> {}, rows={}",
                row.get("username"), userId, before, TARGET_MONEY, updated);
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
