package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 仅在库完全为空时补建登录表；不覆盖已导入的正式数据/密码。
 */
@Component
@Order(1)
public class AuthSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthSchemaBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public AuthSchemaBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                Integer.class);
        if (tables != null && tables > 0) {
            log.info("Database already has {} tables; skip auth schema bootstrap.", tables);
            return;
        }
        log.warn("Database has no tables; creating minimal users/user_profiles. Import wa_demo最终版.sql for full data.");
        ensureUsersTable();
        ensureUserProfilesTable();
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
                  weekly_money bigint NOT NULL DEFAULT 0,
                  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
