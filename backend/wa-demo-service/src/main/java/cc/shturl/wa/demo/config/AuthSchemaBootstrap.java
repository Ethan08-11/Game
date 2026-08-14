package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Zeabur 上完整 SQL 常未导完时，保证登录所需的 users / user_profiles 存在。
 */
@Component
@Order(1)
public class AuthSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthSchemaBootstrap.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AuthSchemaBootstrap(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureUsersTable();
        ensureUserProfilesTable();
        ensureSeedUser();
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                Integer.class);
        log.info("Auth schema ready. Database has {} tables.", tables);
    }

    private void ensureUsersTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id bigint NOT NULL AUTO_INCREMENT,
                  username varchar(50) NOT NULL,
                  password_hash varchar(255) NOT NULL,
                  email varchar(100) NULL,
                  phone varchar(20) NULL,
                  avatar_url varchar(255) NULL,
                  status tinyint NOT NULL DEFAULT 1,
                  last_login_at datetime NULL,
                  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void ensureUserProfilesTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_profiles (
                  id bigint NOT NULL AUTO_INCREMENT,
                  user_id bigint NOT NULL,
                  display_name varchar(50) NULL,
                  signature varchar(200) NULL,
                  gender tinyint NULL,
                  level int NOT NULL DEFAULT 1,
                  exp int NOT NULL DEFAULT 0,
                  win_count int NOT NULL DEFAULT 0,
                  lose_count int NOT NULL DEFAULT 0,
                  draw_count int NOT NULL DEFAULT 0,
                  money bigint NOT NULL DEFAULT 0,
                  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void ensureSeedUser() {
        String hash = passwordEncoder.encode("123456");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, "ethan");
        if (count != null && count > 0) {
            // 完整 SQL 未导完时 ethan 可能已有但密码未知；Zeabur 部署统一重置便于登录
            jdbcTemplate.update("UPDATE users SET password_hash = ?, status = 1 WHERE username = ?", hash, "ethan");
            log.warn("Reset existing user ethan password to 123456 for Zeabur login.");
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO users (username, password_hash, status)
                VALUES (?, ?, 1)
                """, "ethan", hash);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "ethan");
        jdbcTemplate.update("""
                INSERT INTO user_profiles (user_id, display_name, level, exp, win_count, lose_count, draw_count, money)
                VALUES (?, 'ethan', 1, 0, 0, 0, 0, 0)
                """, userId);
        log.warn("Seeded default user ethan / 123456 (change after login).");
    }
}
