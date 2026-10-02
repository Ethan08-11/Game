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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 上一轮作废今日 Ethan 第 1 局、Colin 第 1/2 局时，胜负和金币可能没回退。
 * 再跑一次：含已 winner_type=3 的局，按霸凌者血量推断胜负，回退所有参战者胜率，并重算 Ethan/Colin 每日任务金币。
 */
@Component
@Order(16)
public class EthanColinLeaderboardRepairBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EthanColinLeaderboardRepairBootstrap.class);
    private static final String PATCH_ID = "repair_ethan_colin_leaderboard_20261002b";
    private static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public EthanColinLeaderboardRepairBootstrap(JdbcTemplate jdbcTemplate,
                                                PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("users") || !tableExists("matches") || !tableExists("user_profiles")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                repair();
                markPatchApplied();
            });
        } catch (Exception e) {
            log.error("Ethan/Colin leaderboard repair failed.", e);
        }
    }

    private void repair() {
        LocalDate day = QuestPeriod.currentDailyDate();
        LocalDateTime windowStart = QuestPeriod.windowStart(day);
        LocalDateTime windowEnd = QuestPeriod.windowEnd(day);
        String start = windowStart.format(SQL_TIME);
        String end = windowEnd.format(SQL_TIME);
        String period = day.toString();

        List<Map<String, Object>> ethanMatches = nthMatches("ethan", start, end, 1);
        List<Map<String, Object>> colinMatches = nthMatches("colin", start, end, 2);
        Set<Long> matchIds = new LinkedHashSet<>();
        List<Map<String, Object>> all = new ArrayList<>();
        for (Map<String, Object> row : ethanMatches) {
            if (matchIds.add(((Number) row.get("id")).longValue())) {
                all.add(row);
            }
        }
        for (Map<String, Object> row : colinMatches) {
            if (matchIds.add(((Number) row.get("id")).longValue())) {
                all.add(row);
            }
        }
        if (all.isEmpty()) {
            log.warn("Leaderboard repair found no Ethan/Colin matches in {} ~ {}; still rebuilding daily boards.",
                    start, end);
        }
        for (Map<String, Object> row : all) {
            reverseAndVoid(row);
        }
        long ethanClawed = rebuildDailyBoard("ethan", period, start, end);
        long colinClawed = rebuildDailyBoard("colin", period, start, end);
        log.warn("Repaired Ethan/Colin leaderboard matchIds={} period={} ethanClawed={} colinClawed={}",
                matchIds, period, ethanClawed, colinClawed);
    }

    private List<Map<String, Object>> nthMatches(String username, String start, String end, int limit) {
        Long userId = findUserId(username);
        if (userId == null) {
            log.warn("Leaderboard repair skipped; user {} not found.", username);
            return List.of();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT m.id, m.room_id, m.status, m.winner_type, m.boss_current_hp,
                       m.current_round, m.duration_seconds, m.started_at, m.ended_at, m.created_at
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                ORDER BY COALESCE(m.started_at, m.created_at, m.id), m.id
                LIMIT ?
                """, userId, start, end, limit);
        log.warn("Leaderboard repair {} userId={} picked {}", username, userId,
                rows.stream().map(row -> row.get("id")).toList());
        return rows;
    }

    private void reverseAndVoid(Map<String, Object> match) {
        long matchId = ((Number) match.get("id")).longValue();
        int storedWinner = number(match.get("winner_type"));
        int inferred = inferWinner(match);
        if (inferred == 0) {
            log.warn("Skip reverse matchId={} storedWinner={} hp={} round={} duration={}",
                    matchId, storedWinner, match.get("boss_current_hp"),
                    match.get("current_round"), match.get("duration_seconds"));
            if (storedWinner != 3) {
                closeMatch(matchId, match.get("room_id"));
            }
            return;
        }
        List<Long> playerIds = jdbcTemplate.query(
                "SELECT user_id FROM match_players WHERE match_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                matchId);
        if (inferred == 1 || inferred == 2) {
            for (Long playerId : playerIds) {
                reverseWinLose(playerId, inferred);
                log.warn("Reversed matchId={} userId={} inferredWinner={} storedWinner={}",
                        matchId, playerId, inferred, storedWinner);
            }
        } else {
            log.warn("Skip reverse matchId={} storedWinner={} inferred={}", matchId, storedWinner, inferred);
        }
        if (storedWinner != 3) {
            closeMatch(matchId, match.get("room_id"));
        }
    }

    private int inferWinner(Map<String, Object> match) {
        int stored = number(match.get("winner_type"));
        if (stored == 1 || stored == 2) {
            return stored;
        }
        int round = number(match.get("current_round"));
        int duration = number(match.get("duration_seconds"));
        if (round <= 0 && duration <= 0 && stored == 0) {
            return 0;
        }
        int bossHp = number(match.get("boss_current_hp"));
        if (bossHp <= 0) {
            return 1;
        }
        return stored == 3 || stored == 0 ? 2 : 0;
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

    private long rebuildDailyBoard(String username, String period, String start, String end) {
        Long userId = findUserId(username);
        if (userId == null) {
            return 0L;
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
                userId, start, end);
        int remaining = remainingWinners.size();
        long clawed = 0L;
        clawed += syncTask(userId, period, "T-DAILY-SLOT", remaining, Math.max(remaining, 1), false);
        for (int slot = 1; slot <= 3; slot++) {
            clawed += syncTask(userId, period, "T-DAILY-MATCH-" + slot, remaining, slot, remaining >= slot);
            boolean wonSlot = slot <= remaining && remainingWinners.get(slot - 1) == 1;
            clawed += syncTask(userId, period, "T-DAILY-WIN-" + slot, wonSlot ? 1 : 0, 1, wonSlot);
        }
        log.warn("Rebuilt {} daily board remaining={} clawedGold={} moneyNow={}",
                username, remaining, clawed, currentMoney(userId));
        return clawed;
    }

    private Long currentMoney(long userId) {
        return jdbcTemplate.query("""
                SELECT IFNULL(money, 0) FROM user_profiles WHERE user_id = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : 0L, userId);
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
        Map<String, Object> row = jdbcTemplate.query("""
                SELECT id, status, progress_value FROM user_tasks
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                LIMIT 1
                """, rs -> {
            if (!rs.next()) {
                return null;
            }
            return Map.of(
                    "id", rs.getLong("id"),
                    "status", rs.getInt("status"),
                    "progress", rs.getInt("progress_value"));
        }, userId, taskId, period);
        if (row == null) {
            jdbcTemplate.update("""
                    INSERT INTO user_tasks (
                      user_id, task_id, period_key, progress_value, target_value,
                      status, completed_at, claimed_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, NOW(), NOW())
                    """, userId, taskId, period, progress, target, complete ? 2 : (progress > 0 ? 1 : 0));
            return 0L;
        }
        int status = ((Number) row.get("status")).intValue();
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
