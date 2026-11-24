package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.PublicAccountNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 每个内部版账号对应一个对外版账号，名字是 HIH 加原来的名字。密码与头像沿用本人。
 */
@Component
@Order(71)
public class PublicMirrorAccountBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PublicMirrorAccountBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public PublicMirrorAccountBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("users") || !tableExists("user_profiles") || !columnExists("users", "audience")) {
            return;
        }
        List<InternalUser> internals = jdbcTemplate.query("""
                SELECT u.id, u.username, u.password_hash, u.avatar_url, p.display_name
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE u.status = 1
                  AND UPPER(IFNULL(u.audience, 'INTERNAL')) = 'INTERNAL'
                ORDER BY u.id
                """, (rs, rowNum) -> new InternalUser(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("avatar_url"),
                rs.getString("display_name")));
        int created = 0;
        for (InternalUser user : internals) {
            if (user.passwordHash == null || user.passwordHash.isBlank()) {
                continue;
            }
            String mirror = PublicAccountNames.mirrorName(user.username, user.displayName);
            if (mirror.length() < 4 || mirror.length() > 50) {
                log.warn("Skip public mirror for user id {} because the name does not fit.", user.id);
                continue;
            }
            Long newId = jdbcTemplate.query("""
                    SELECT id FROM users WHERE username = ?
                    """, (rs, rowNum) -> rs.getLong("id"), mirror).stream().findFirst().orElse(null);
            if (newId == null) {
                jdbcTemplate.update("""
                        INSERT INTO users (username, password_hash, avatar_url, status, audience, created_at, updated_at)
                        VALUES (?, ?, ?, 1, 'PUBLIC', NOW(), NOW())
                        """, mirror, user.passwordHash, user.avatarUrl);
                newId = jdbcTemplate.queryForObject(
                        "SELECT id FROM users WHERE username = ?",
                        Long.class,
                        mirror);
                created++;
                log.info("Created public account {}.", mirror);
            }
            if (newId == null) {
                continue;
            }
            jdbcTemplate.update("""
                    INSERT INTO user_profiles (user_id, display_name, level, exp, win_count, lose_count, draw_count, money, weekly_money, created_at, updated_at)
                    SELECT ?, ?, 1, 0, 0, 0, 0, 0, 0, NOW(), NOW()
                    FROM DUAL
                    WHERE NOT EXISTS (SELECT 1 FROM user_profiles WHERE user_id = ?)
                    """, newId, mirror, newId);
        }
        int linked = linkPublicFriends();
        if (created > 0 || linked > 0) {
            log.info("Public mirror accounts created {}, friend rows added {}.", created, linked);
        }
    }

    private int linkPublicFriends() {
        if (!tableExists("friendships")) {
            return 0;
        }
        List<Long> publicIds = jdbcTemplate.query("""
                SELECT id FROM users
                WHERE status = 1 AND UPPER(IFNULL(audience, 'INTERNAL')) = 'PUBLIC'
                """, (rs, rowNum) -> rs.getLong("id"));
        int linked = 0;
        for (Long userId : publicIds) {
            Integer inserted = jdbcTemplate.update("""
                    INSERT INTO friendships (user_id, friend_id, status, created_at, updated_at)
                    SELECT ?, u.id, 1, NOW(), NOW()
                    FROM users u
                    WHERE u.id <> ?
                      AND u.status = 1
                      AND UPPER(IFNULL(u.audience, 'INTERNAL')) = 'PUBLIC'
                      AND NOT EXISTS (
                        SELECT 1 FROM friendships f
                        WHERE (f.user_id = ? AND f.friend_id = u.id)
                           OR (f.user_id = u.id AND f.friend_id = ?)
                      )
                    """, userId, userId, userId, userId);
            linked += inserted == null ? 0 : inserted;
        }
        return linked;
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
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private record InternalUser(long id, String username, String passwordHash, String avatarUrl, String displayName) {
    }
}
