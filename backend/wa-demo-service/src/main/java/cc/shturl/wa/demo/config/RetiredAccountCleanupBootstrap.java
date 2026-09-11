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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 下线废弃测试号 Tahncy、Test，并从好友关系与对局记录里清掉。
 */
@Component
@Order(4)
public class RetiredAccountCleanupBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RetiredAccountCleanupBootstrap.class);
    private static final String PATCH_ID = "delete_accounts_tahncy_test_20260911";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RetiredAccountCleanupBootstrap(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("users")) {
            return;
        }
        ensurePatchTable();
        if (patchApplied()) {
            return;
        }
        List<Long> userIds = jdbcTemplate.query(
                "SELECT id FROM users WHERE LOWER(username) IN ('tahncy', 'test')",
                (rs, rowNum) -> rs.getLong("id"));
        if (userIds.isEmpty()) {
            markPatchApplied();
            log.info("No Tahncy/Test accounts to delete.");
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                deleteUserGraph(userIds);
            } finally {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            markPatchApplied();
        });
        log.info("Deleted retired accounts Tahncy/Test: ids={}", userIds);
    }

    private void deleteUserGraph(List<Long> userIds) {
        List<Long> matchIds = queryColumnIds("match_players",
                "SELECT DISTINCT match_id FROM match_players WHERE user_id IN (" + placeholders(userIds) + ")",
                userIds);
        Set<Long> roomIds = new LinkedHashSet<>();
        roomIds.addAll(queryColumnIds("room_members",
                "SELECT DISTINCT room_id FROM room_members WHERE user_id IN (" + placeholders(userIds) + ")",
                userIds));
        roomIds.addAll(queryColumnIds("game_rooms",
                "SELECT id FROM game_rooms WHERE host_user_id IN (" + placeholders(userIds) + ")",
                userIds));

        deleteByColumn("match_pending_effects", "match_id", matchIds);
        deleteByColumn("match_actions", "match_id", matchIds);
        deleteByColumn("match_cards", "match_id", matchIds);
        deleteByColumn("match_revive_logs", "match_id", matchIds);
        deleteByColumn("match_rounds", "match_id", matchIds);
        deleteByColumn("match_replays", "match_id", matchIds);
        deleteByColumn("match_players", "match_id", matchIds);
        deleteByColumn("matches", "id", matchIds);

        deleteByColumn("room_invites", "room_id", roomIds);
        deleteByColumn("room_invites", "from_user_id", userIds);
        deleteByColumn("room_invites", "to_user_id", userIds);
        deleteByColumn("room_members", "room_id", roomIds);
        deleteByColumn("room_members", "user_id", userIds);
        deleteByColumn("game_rooms", "id", roomIds);
        deleteByColumn("game_rooms", "host_user_id", userIds);

        deleteByColumn("friend_remark", "user_id", userIds);
        deleteByColumn("friend_remark", "friend_user_id", userIds);
        deleteByColumn("friendships", "user_id", userIds);
        deleteByColumn("friendships", "friend_id", userIds);
        deleteByColumn("user_achievements", "user_id", userIds);
        deleteByColumn("user_card_pools", "user_id", userIds);
        deleteByColumn("user_tasks", "user_id", userIds);
        deleteByColumn("user_profiles", "user_id", userIds);
        deleteByColumn("users", "id", userIds);
    }

    private List<Long> queryColumnIds(String table, String sql, List<Long> args) {
        if (!tableExists(table) || args.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args.toArray()));
    }

    private void deleteByColumn(String table, String column, Collection<Long> ids) {
        if (!tableExists(table) || !columnExists(table, column) || ids == null || ids.isEmpty()) {
            return;
        }
        jdbcTemplate.update(
                "DELETE FROM `" + table + "` WHERE `" + column + "` IN (" + placeholders(ids) + ")",
                ids.toArray());
    }

    private static String placeholders(Collection<Long> ids) {
        return ids.stream().map(id -> "?").collect(Collectors.joining(","));
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
