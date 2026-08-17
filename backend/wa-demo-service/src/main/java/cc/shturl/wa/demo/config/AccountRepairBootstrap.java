package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 补齐注册较早、尚未自动加好友的账号；并按运营要求重置指定测试号密码。
 */
@Component
@Order(2)
public class AccountRepairBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountRepairBootstrap.class);
    private static final String SEAN_USERNAME = "Sean";
    private static final String SEAN_PASSWORD = "123";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AccountRepairBootstrap(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("users") || !tableExists("friendships")) {
            return;
        }
        repairSeanAccount();
        backfillMissingFriends();
    }

    private void repairSeanAccount() {
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM users WHERE LOWER(username) = 'sean'",
                (rs, rowNum) -> rs.getLong("id"));
        if (ids.isEmpty()) {
            log.info("No Sean account found; skip password repair.");
            return;
        }
        Long seanId = ids.get(0);
        jdbcTemplate.update(
                "UPDATE users SET username = ?, password_hash = ? WHERE id = ?",
                SEAN_USERNAME,
                passwordEncoder.encode(SEAN_PASSWORD),
                seanId);
        if (tableExists("user_profiles")) {
            jdbcTemplate.update(
                    "UPDATE user_profiles SET display_name = ? WHERE user_id = ? AND (display_name IS NULL OR display_name = '' OR LOWER(display_name) = 'sean')",
                    SEAN_USERNAME,
                    seanId);
        }
        int linked = linkUserToExistingPlayers(seanId);
        log.info("Repaired Sean account id={}, linked {} missing friend rows.", seanId, linked);
    }

    private void backfillMissingFriends() {
        List<Long> isolated = jdbcTemplate.query(
                """
                        SELECT u.id FROM users u
                        WHERE u.status = 1
                          AND NOT EXISTS (
                            SELECT 1 FROM friendships f
                            WHERE f.status = 1 AND (f.user_id = u.id OR f.friend_id = u.id)
                          )
                        """,
                (rs, rowNum) -> rs.getLong("id"));
        int total = 0;
        for (Long userId : isolated) {
            total += linkUserToExistingPlayers(userId);
        }
        if (total > 0) {
            log.info("Backfilled {} friend rows for {} accounts without friends.", total, isolated.size());
        }
    }

    private int linkUserToExistingPlayers(Long userId) {
        Integer inserted = jdbcTemplate.update(
                """
                        INSERT INTO friendships (user_id, friend_id, status, created_at, updated_at)
                        SELECT ?, u.id, 1, NOW(), NOW()
                        FROM users u
                        WHERE u.id <> ?
                          AND u.status = 1
                          AND NOT EXISTS (
                            SELECT 1 FROM friendships f
                            WHERE (f.user_id = ? AND f.friend_id = u.id)
                               OR (f.user_id = u.id AND f.friend_id = ?)
                          )
                        """,
                userId, userId, userId, userId);
        return inserted == null ? 0 : inserted;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }
}
