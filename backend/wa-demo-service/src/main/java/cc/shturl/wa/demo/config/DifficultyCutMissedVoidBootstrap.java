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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 上次降难度作废把上海时间的 0 点绑成 Timestamp，驱动按 Asia/Shanghai 又加了 8 小时。
 * 今天 16:00 前结束的对局没被选中，补丁却已记完成，Ethan 仍是 3 胜 2 负（60%）。
 * 这里按对局实际写入的 UTC 墙钟补作废，并回退这些局的胜负。
 */
@Component
@Order(19)
public class DifficultyCutMissedVoidBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DifficultyCutMissedVoidBootstrap.class);
    private static final String PATCH_ID = "diff_cut_missed_void_ethan_kade_20261002";
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final ZoneId STORED_CLOCK = ZoneId.of("UTC");
    private static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public DifficultyCutMissedVoidBootstrap(JdbcTemplate jdbcTemplate,
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
                voidMissedMatches();
                markPatchApplied();
            });
        } catch (Exception e) {
            log.error("Missed difficulty-cut void failed.", e);
        }
    }

    private void voidMissedMatches() {
        LocalDate day = LocalDate.now(SHANGHAI);
        String start = day.atStartOfDay(SHANGHAI).withZoneSameInstant(STORED_CLOCK).toLocalDateTime().format(SQL_TIME);
        String end = day.plusDays(1).atStartOfDay(SHANGHAI).withZoneSameInstant(STORED_CLOCK).toLocalDateTime().format(SQL_TIME);
        Set<Long> matchIds = new LinkedHashSet<>();
        matchIds.addAll(latestRealMatches("ethan", start, end, 2));
        matchIds.addAll(latestRealMatches("kade", start, end, 1));
        if (matchIds.isEmpty()) {
            log.warn("Missed difficulty-cut void found nothing in {} ~ {}.", start, end);
            return;
        }
        for (Long matchId : matchIds) {
            voidMatch(matchId);
        }
        log.warn("Missed difficulty-cut void matchIds={} window={} ~ {}", matchIds, start, end);
    }

    private List<Long> latestRealMatches(String username, String start, String end, int limit) {
        Long userId = findUserId(username);
        if (userId == null) {
            log.warn("Missed difficulty-cut void skipped; user {} not found.", username);
            return List.of();
        }
        List<Long> ids = jdbcTemplate.query("""
                SELECT m.id
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND IFNULL(m.winner_type, 0) IN (1, 2)
                  AND IFNULL(m.match_code, '') NOT LIKE 'AE%'
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                ORDER BY COALESCE(m.started_at, m.created_at, m.id) DESC, m.id DESC
                LIMIT ?
                """, (rs, rowNum) -> rs.getLong("id"), userId, start, end, limit);
        log.warn("Missed difficulty-cut void {} userId={} picked {}", username, userId, ids);
        return ids;
    }

    private void voidMatch(long matchId) {
        List<Map<String, Object>> matches = jdbcTemplate.queryForList("""
                SELECT id, room_id, status, winner_type FROM matches WHERE id = ? LIMIT 1
                """, matchId);
        if (matches.isEmpty()) {
            return;
        }
        Map<String, Object> match = matches.get(0);
        int winnerType = number(match.get("winner_type"));
        if (winnerType != 1 && winnerType != 2) {
            return;
        }
        List<Long> playerIds = jdbcTemplate.query(
                "SELECT user_id FROM match_players WHERE match_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                matchId);
        for (Long playerId : playerIds) {
            reverseWinLose(playerId, winnerType);
            log.warn("Reversed missed void matchId={} userId={} winnerType={}", matchId, playerId, winnerType);
        }
        closeMatch(matchId, match.get("room_id"));
    }

    private void closeMatch(long matchId, Object roomId) {
        jdbcTemplate.update("""
                UPDATE room_members rm
                INNER JOIN matches m ON m.room_id = rm.room_id
                SET rm.left_at = IFNULL(rm.left_at, NOW()),
                    rm.online_status = 0,
                    rm.ready_status = 0
                WHERE m.id = ? AND rm.left_at IS NULL
                """, matchId);
        jdbcTemplate.update("""
                UPDATE game_rooms r
                INNER JOIN matches m ON m.room_id = r.id
                SET r.status = 3,
                    r.closed_at = IFNULL(r.closed_at, NOW()),
                    r.player_count = 0
                WHERE m.id = ?
                """, matchId);
        if (roomId != null) {
            jdbcTemplate.update("""
                    UPDATE game_rooms
                    SET match_id = NULL
                    WHERE id = ? AND match_id = ?
                    """, ((Number) roomId).longValue(), matchId);
        }
        jdbcTemplate.update("""
                UPDATE match_players
                SET result_type = 3,
                    player_status = 'LEFT'
                WHERE match_id = ?
                """, matchId);
        jdbcTemplate.update("""
                UPDATE matches
                SET status = 2,
                    phase = 'FINISHED',
                    winner_type = 3,
                    ended_at = IFNULL(ended_at, NOW())
                WHERE id = ?
                """, matchId);
    }

    private void reverseWinLose(long userId, int winnerType) {
        if (winnerType == 1) {
            jdbcTemplate.update("""
                    UPDATE user_profiles
                    SET win_count = GREATEST(0, IFNULL(win_count, 0) - 1),
                        exp = GREATEST(0, IFNULL(exp, 0) - 100)
                    WHERE user_id = ?
                    """, userId);
        } else if (winnerType == 2) {
            jdbcTemplate.update("""
                    UPDATE user_profiles
                    SET lose_count = GREATEST(0, IFNULL(lose_count, 0) - 1)
                    WHERE user_id = ?
                    """, userId);
        }
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
