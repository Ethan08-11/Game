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
import java.util.List;
import java.util.Map;

/**
 * 作废 Charlene 最近一局：清房间、不占今日任务槽、回退该局已记的胜负。
 */
@Component
@Order(14)
public class CharleneLastMatchResetBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CharleneLastMatchResetBootstrap.class);
    private static final String PATCH_ID = "reset_charlene_last_match_20261001";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public CharleneLastMatchResetBootstrap(JdbcTemplate jdbcTemplate,
                                           PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("users") || !tableExists("matches") || !tableExists("match_players")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                resetCharleneLastMatch();
                markPatchApplied();
            });
        } catch (Exception e) {
            log.error("Charlene last-match reset failed.", e);
        }
    }

    private void resetCharleneLastMatch() {
        Long userId = findUserId("charlene");
        if (userId == null) {
            log.warn("Charlene last-match reset skipped; user not found.");
            return;
        }
        List<Map<String, Object>> matches = jdbcTemplate.queryForList("""
                SELECT m.id, m.room_id, m.status, m.phase, m.winner_type,
                       m.started_at, m.ended_at, m.created_at
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                ORDER BY m.id DESC
                LIMIT 1
                """, userId);
        if (matches.isEmpty()) {
            log.warn("Charlene last-match reset skipped; no match found for userId={}.", userId);
            return;
        }
        Map<String, Object> match = matches.get(0);
        long matchId = ((Number) match.get("id")).longValue();
        int status = number(match.get("status"));
        int winnerType = number(match.get("winner_type"));
        boolean settled = status == 2 && (winnerType == 1 || winnerType == 2);

        List<Long> playerIds = jdbcTemplate.query(
                "SELECT user_id FROM match_players WHERE match_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                matchId);

        closeMatch(matchId, match.get("room_id"));
        if (settled) {
            String period = periodKey(match);
            for (Long playerId : playerIds) {
                reverseSettlement(playerId, winnerType, period);
            }
        }
        log.warn("Reset Charlene last match matchId={} status={} winnerType={} settled={} players={}",
                matchId, status, winnerType, settled, playerIds);
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

    private void reverseSettlement(long userId, int winnerType, String period) {
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
        Integer slot = jdbcTemplate.query("""
                SELECT ut.progress_value
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.user_id = ? AND t.task_code = 'T-DAILY-SLOT' AND ut.period_key = ?
                LIMIT 1
                """, rs -> rs.next() ? rs.getInt(1) : null, userId, period);
        if (slot == null || slot <= 0) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.progress_value = GREATEST(0, IFNULL(ut.progress_value, 0) - 1),
                    ut.status = 1,
                    ut.completed_at = NULL
                WHERE ut.user_id = ? AND t.task_code = 'T-DAILY-SLOT' AND ut.period_key = ?
                """, userId, period);
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.completed_at = CASE
                        WHEN GREATEST(0, IFNULL(ut.progress_value, 0) - 1) >= IFNULL(ut.target_value, 1) THEN ut.completed_at
                        ELSE NULL END,
                    ut.status = CASE
                        WHEN GREATEST(0, IFNULL(ut.progress_value, 0) - 1) >= IFNULL(ut.target_value, 1) THEN 2
                        WHEN GREATEST(0, IFNULL(ut.progress_value, 0) - 1) > 0 THEN 1
                        ELSE 0 END,
                    ut.progress_value = GREATEST(0, IFNULL(ut.progress_value, 0) - 1)
                WHERE ut.user_id = ?
                  AND ut.period_key = ?
                  AND t.task_code IN ('T-DAILY-MATCH-1', 'T-DAILY-MATCH-2', 'T-DAILY-MATCH-3')
                  AND ut.status < 3
                """, userId, period);
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.progress_value = 0,
                    ut.status = 0,
                    ut.completed_at = NULL
                WHERE ut.user_id = ?
                  AND ut.period_key = ?
                  AND t.task_code = ?
                  AND ut.status < 3
                """, userId, period, "T-DAILY-WIN-" + slot);
    }

    private String periodKey(Map<String, Object> match) {
        for (String column : List.of("ended_at", "started_at", "created_at")) {
            String key = dateKey(match.get(column));
            if (key != null) {
                return key;
            }
        }
        return LocalDate.now(ZONE).toString();
    }

    private String dateKey(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate().toString();
        }
        if (value instanceof java.time.LocalDateTime dateTime) {
            return dateTime.toLocalDate().toString();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(ZONE).toLocalDate().toString();
        }
        return null;
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
