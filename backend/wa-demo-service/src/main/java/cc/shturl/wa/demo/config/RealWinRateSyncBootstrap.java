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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 将排行榜胜负同步为 2026-09-12 至 2026-09-21 的真实对局（不含作废）。
 * Ethan 单独改为 49 胜 1 负，展示胜率 98%；Riley 改为 28 胜 0 负，展示胜率 100%。
 */
@Component
@Order(11)
public class RealWinRateSyncBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RealWinRateSyncBootstrap.class);
    private static final String PATCH_ID = "sync_real_winrate_sep12_20260921";

    private static final Map<String, Record> COUNTS;

    static {
        Map<String, Record> counts = new LinkedHashMap<>();
        counts.put("amy", new Record(12, 1));
        counts.put("ancy", new Record(44, 4));
        counts.put("carl", new Record(18, 0));
        counts.put("charlene", new Record(12, 1));
        counts.put("chrissy", new Record(5, 1));
        counts.put("colin", new Record(24, 0));
        counts.put("daniel", new Record(34, 4));
        counts.put("duane", new Record(10, 0));
        counts.put("ethan", new Record(49, 1));
        counts.put("felicity", new Record(12, 2));
        counts.put("harry", new Record(35, 1));
        counts.put("iris", new Record(54, 3));
        counts.put("kade", new Record(18, 2));
        counts.put("kinyond", new Record(1, 0));
        counts.put("nico", new Record(29, 1));
        counts.put("riley", new Record(28, 0));
        counts.put("sandra", new Record(19, 0));
        counts.put("sean", new Record(5, 0));
        counts.put("tancy", new Record(3, 1));
        COUNTS = Map.copyOf(counts);
    }

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RealWinRateSyncBootstrap(JdbcTemplate jdbcTemplate,
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
            for (Map.Entry<String, Record> entry : COUNTS.entrySet()) {
                applyCounts(entry.getKey(), entry.getValue());
            }
            markPatchApplied();
        });
    }

    private void applyCounts(String username, Record next) {
        String key = username.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.id AS user_id, u.username,
                       IFNULL(p.win_count, 0) AS win_count,
                       IFNULL(p.lose_count, 0) AS lose_count,
                       IFNULL(p.draw_count, 0) AS draw_count
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE LOWER(u.username) = ?
                   OR LOWER(IFNULL(p.display_name, '')) = ?
                ORDER BY CASE WHEN LOWER(u.username) = ? THEN 0 ELSE 1 END, u.id
                LIMIT 1
                """, key, key, key);
        if (rows.isEmpty()) {
            log.warn("Sync real win-rate skipped; user {} not found.", username);
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
                """, next.wins(), next.losses(), userId);
        log.warn("Sync real win-rate user={} id={}: {}/{}/{} -> {}/{}/0, rows={}",
                row.get("username"), userId,
                row.get("win_count"), row.get("lose_count"), row.get("draw_count"),
                next.wins(), next.losses(), updated);
    }

    record Record(int wins, int losses) {
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
}
