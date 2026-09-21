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
 * Ancy 第 2 局掉线误判占槽，一次性补偿排行榜金币 10。
 */
@Component
@Order(13)
public class AncyMoneyBonusBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AncyMoneyBonusBootstrap.class);
    private static final String PATCH_ID = "bonus_ancy_money_10_20260921";
    private static final long MONEY_BONUS = 10L;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AncyMoneyBonusBootstrap(JdbcTemplate jdbcTemplate,
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
            addAncyMoney();
            markPatchApplied();
        });
    }

    private void addAncyMoney() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.id AS user_id, u.username, IFNULL(p.money, 0) AS money
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE LOWER(u.username) = 'ancy'
                   OR LOWER(IFNULL(p.display_name, '')) = 'ancy'
                ORDER BY CASE WHEN LOWER(u.username) = 'ancy' THEN 0 ELSE 1 END, u.id
                LIMIT 1
                """);
        if (rows.isEmpty()) {
            log.warn("Ancy money bonus skipped; user not found.");
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
                "UPDATE user_profiles SET money = IFNULL(money, 0) + ? WHERE user_id = ?",
                MONEY_BONUS, userId);
        log.warn("Ancy money bonus user={} id={}: {} -> {}, rows={}",
                row.get("username"), userId, before, before + MONEY_BONUS, updated);
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
