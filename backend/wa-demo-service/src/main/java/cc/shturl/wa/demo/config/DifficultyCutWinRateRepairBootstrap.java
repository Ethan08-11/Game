package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.impl.LeaderboardServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 降难度作废 Ethan 两局、Kade 一局后，胜率可能没从排行榜扣掉。
 * 按本月剩余有效对局（不含作废）重算两人胜负。
 */
@Component
@Order(18)
public class DifficultyCutWinRateRepairBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DifficultyCutWinRateRepairBootstrap.class);
    private static final String PATCH_ID = "repair_ethan_kade_winrate_20261002";
    private static final String[] USERS = {"ethan", "kade"};

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public DifficultyCutWinRateRepairBootstrap(JdbcTemplate jdbcTemplate,
                                               PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("users") || !tableExists("user_profiles") || !tableExists("matches")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                for (String username : USERS) {
                    rebuildWinRate(username);
                }
                markPatchApplied();
            });
        } catch (Exception e) {
            log.error("Ethan/Kade win-rate repair failed.", e);
        }
    }

    private void rebuildWinRate(String username) {
        Long userId = findUserId(username);
        if (userId == null) {
            log.warn("Win-rate repair skipped; user {} not found.", username);
            return;
        }
        LocalDate monthStart = LeaderboardServiceImpl.currentMonthStart();
        LocalDateTime windowStart = monthStart.atStartOfDay();
        LocalDateTime windowEnd = monthStart.plusMonths(1).atStartOfDay();
        Map<String, Object> before = jdbcTemplate.query("""
                SELECT IFNULL(win_count, 0) AS win_count,
                       IFNULL(lose_count, 0) AS lose_count,
                       IFNULL(draw_count, 0) AS draw_count
                FROM user_profiles
                WHERE user_id = ?
                LIMIT 1
                """, rs -> rs.next() ? Map.of(
                "win_count", rs.getInt("win_count"),
                "lose_count", rs.getInt("lose_count"),
                "draw_count", rs.getInt("draw_count")) : Map.of(), userId);
        Map<String, Object> counts = jdbcTemplate.query("""
                SELECT IFNULL(SUM(m.winner_type = 1), 0) AS wins,
                       IFNULL(SUM(m.winner_type = 2), 0) AS losses
                FROM match_players mp
                INNER JOIN matches m ON m.id = mp.match_id
                WHERE mp.user_id = ?
                  AND m.status = 2
                  AND IFNULL(m.winner_type, 0) IN (1, 2)
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                """, rs -> rs.next() ? Map.of(
                "wins", rs.getInt("wins"),
                "losses", rs.getInt("losses")) : Map.of("wins", 0, "losses", 0),
                userId, Timestamp.valueOf(windowStart), Timestamp.valueOf(windowEnd));
        int wins = number(counts.get("wins"));
        int losses = number(counts.get("losses"));
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = ?,
                    lose_count = ?,
                    draw_count = 0
                WHERE user_id = ?
                """, wins, losses, userId);
        log.warn("Rebuilt {} win-rate userId={} month={}: {}/{} -> {}/{}",
                username, userId, monthStart,
                before.get("win_count"), before.get("lose_count"),
                wins, losses);
    }

    private Long findUserId(String username) {
        String key = username.toLowerCase();
        List<Long> ids = jdbcTemplate.query("""
                SELECT u.id
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE LOWER(u.username) = ?
                   OR LOWER(IFNULL(p.display_name, '')) = ?
                ORDER BY CASE WHEN LOWER(u.username) = ? THEN 0 ELSE 1 END, u.id
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), key, key, key);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
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
