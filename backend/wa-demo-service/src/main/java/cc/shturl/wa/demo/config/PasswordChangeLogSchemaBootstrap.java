package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 账号改密审计：users.password_changed_at + password_change_logs，供运营在 MySQL 里查看统计。
 * 密码只存 BCrypt 哈希，不落明文。
 */
@Component
@Order(4)
public class PasswordChangeLogSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeLogSchemaBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public PasswordChangeLogSchemaBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("users")) {
            return;
        }
        ensurePasswordChangedAtColumn();
        ensureLogTable();
        log.info("Password change audit tables ready.");
    }

    private void ensurePasswordChangedAtColumn() {
        if (columnExists("users", "password_changed_at")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `users`
                ADD COLUMN `password_changed_at` datetime NULL COMMENT '最近一次成功改密时间'
                AFTER `last_login_at`
                """);
        log.info("Added users.password_changed_at.");
    }

    private void ensureLogTable() {
        if (tableExists("password_change_logs")) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE `password_change_logs` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `user_id` bigint NULL,
                  `username` varchar(50) NOT NULL,
                  `success` tinyint NOT NULL DEFAULT 0 COMMENT '1成功 0失败',
                  `reason` varchar(64) NULL,
                  `client_ip` varchar(64) NULL,
                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_pwd_log_user` (`user_id`, `created_at`),
                  KEY `idx_pwd_log_created` (`created_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='改密审计：不含明文密码，便于查看与统计'
                """);
        log.info("Created password_change_logs.");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }
}
