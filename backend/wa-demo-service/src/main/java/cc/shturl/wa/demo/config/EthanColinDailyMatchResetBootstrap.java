package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.QuestPeriod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作废今日 Ethan 第 1 局、Colin 第 1/2 局：回退胜负、金币与每日任务进度。
 */
@Component
@Order(15)
public class EthanColinDailyMatchResetBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EthanColinDailyMatchResetBootstrap.class);
    private static final String PATCH_ID = "reset_ethan1_colin12_daily_20261002";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public EthanColinDailyMatchResetBootstrap(JdbcTemplate jdbcTemplate,
                                              PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("users") || !tableExists("matches") || !tableExists("user_tasks")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                resetToday();
                markPatchApplied();
            });
        } catch (Exception e) {
            log.error("Ethan/Colin daily match reset failed.", e);
        }
    }

    private void resetToday() {
        LocalDate day = QuestPeriod.currentDailyDate();
        LocalDateTime windowStart = QuestPeriod.windowStart(day);
        LocalDateTime windowEnd = QuestPeriod.windowEnd(day);
        String period = day.toString();
        Set<Long> matchIds = new LinkedHashSet<>();
        matchIds.addAll(nthMatches("ethan", windowStart, windowEnd, 1));
        matchIds.addAll(nthMatches("colin", windowStart, windowEnd, 2));
        if (matchIds.isEmpty()) {
            log.warn("Ethan/Colin daily match reset skipped; no matches in window {} ~ {}.", windowStart, windowEnd);
            return;
        }
        for (Long matchId : matchIds) {
            voidMatch(matchId);
        }
        rebuildDailyBoard("ethan", period, windowStart, windowEnd);
        rebuildDailyBoard("colin", period, windowStart, windowEnd);
        log.warn("Reset Ethan first / Colin first-two daily matches matchIds={} period={}", matchIds, period);
    }

    private List<Long> nthMatches(String username, LocalDateTime windowStart, LocalDateTime windowEnd, int limit) {
        Long userId = findUserId(username);
        if (userId == null) {
            log.warn("Daily match reset skipped; user {} not found.", username);
            return List.of();
        }
        List<Long> ids = jdbcTemplate.query("""
                SELECT m.id
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND IFNULL(m.winner_type, 0) <> 3
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                ORDER BY COALESCE(m.started_at, m.created_at, m.id), m.id
                LIMIT ?
                """, (rs, rowNum) -> rs.getLong("id"),
                userId, Timestamp.valueOf(windowStart), Timestamp.valueOf(windowEnd), limit);
        log.warn("Daily match reset {} userId={} picked {}", username, userId, ids);
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
        int status = number(match.get("status"));
        int winnerType = number(match.get("winner_type"));
        boolean settled = status == 2 && (winnerType == 1 || winnerType == 2);
        List<Long> playerIds = jdbcTemplate.query(
                "SELECT user_id FROM match_players WHERE match_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                matchId);
        closeMatch(matchId, match.get("room_id"));
        if (settled) {
            for (Long playerId : playerIds) {
                reverseWinLose(playerId, winnerType);
            }
        }
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

    private void rebuildDailyBoard(String username, String period, LocalDateTime windowStart, LocalDateTime windowEnd) {
        Long userId = findUserId(username);
        if (userId == null) {
            return;
        }
        List<Integer> remainingWinners = jdbcTemplate.query("""
                SELECT m.winner_type
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND m.status = 2
                  AND IFNULL(m.winner_type, 0) IN (1, 2)
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                ORDER BY COALESCE(m.started_at, m.created_at, m.id), m.id
                """, (rs, rowNum) -> rs.getInt(1),
                userId, Timestamp.valueOf(windowStart), Timestamp.valueOf(windowEnd));
        int remaining = remainingWinners.size();
        long clawed = 0L;
        clawed += syncTask(userId, period, "T-DAILY-SLOT", remaining, Math.max(remaining, 1), false);
        for (int slot = 1; slot <= 3; slot++) {
            clawed += syncTask(userId, period, "T-DAILY-MATCH-" + slot, remaining, slot, remaining >= slot);
            boolean wonSlot = slot <= remaining && remainingWinners.get(slot - 1) == 1;
            clawed += syncTask(userId, period, "T-DAILY-WIN-" + slot, wonSlot ? 1 : 0, 1, wonSlot);
        }
        log.warn("Rebuilt {} daily board remaining={} clawedGold={}", username, remaining, clawed);
    }

    private long syncTask(long userId, String period, String taskCode, int progress, int target, boolean complete) {
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList("""
                SELECT id, reward_value FROM tasks WHERE task_code = ? LIMIT 1
                """, taskCode);
        if (tasks.isEmpty()) {
            return 0L;
        }
        long taskId = ((Number) tasks.get(0).get("id")).longValue();
        long reward = "T-DAILY-SLOT".equals(taskCode) ? 0L : rewardAmount(tasks.get(0).get("reward_value"));
        Integer status = jdbcTemplate.query("""
                SELECT status FROM user_tasks
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                LIMIT 1
                """, rs -> rs.next() ? rs.getInt(1) : null, userId, taskId, period);
        if (status == null) {
            jdbcTemplate.update("""
                    INSERT INTO user_tasks (
                      user_id, task_id, period_key, progress_value, target_value,
                      status, completed_at, claimed_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, NOW(), NOW())
                    """, userId, taskId, period, progress, target, complete ? 2 : (progress > 0 ? 1 : 0));
            return 0L;
        }
        long clawed = 0L;
        int nextStatus;
        if (complete && status >= 3) {
            nextStatus = 3;
        } else if (complete) {
            nextStatus = 2;
        } else {
            nextStatus = progress > 0 ? 1 : 0;
            if (status >= 3 && reward > 0) {
                jdbcTemplate.update("""
                        UPDATE user_profiles
                        SET money = GREATEST(0, IFNULL(money, 0) - ?),
                            weekly_money = GREATEST(0, IFNULL(weekly_money, 0) - ?)
                        WHERE user_id = ?
                        """, reward, reward, userId);
                clawed = reward;
            }
        }
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET progress_value = ?,
                    target_value = GREATEST(IFNULL(target_value, 0), ?),
                    status = ?,
                    completed_at = CASE WHEN ? THEN IFNULL(completed_at, NOW()) ELSE NULL END,
                    claimed_at = CASE WHEN ? THEN claimed_at ELSE NULL END,
                    updated_at = NOW()
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                """, progress, target, nextStatus, complete, nextStatus >= 3, userId, taskId, period);
        return clawed;
    }

    private long rewardAmount(Object raw) {
        if (raw == null) {
            return 0L;
        }
        try {
            JsonNode node = raw instanceof JsonNode json ? json : JSON.readTree(String.valueOf(raw));
            if (node.has("amount")) {
                return node.get("amount").asLong(0L);
            }
        } catch (Exception ignored) {
            return 0L;
        }
        return 0L;
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
