package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.QuestPeriod;
import cc.shturl.wa.demo.service.WorkDayQuota;
import cc.shturl.wa.demo.service.impl.LeaderboardServiceImpl;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作废 Iris / Ancy 账号全部未作废对局，按本月剩余有效局重算所有参战者胜负，
 * 并回退受影响日期的每日任务。旧月对局只作废，不改当前排行榜。
 */
@Component
@Order(27)
public class IrisAncyAllMatchResetBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IrisAncyAllMatchResetBootstrap.class);
    static final String PATCH_ID = "reset_iris_ancy_all_matches_20261005";
    private static final String[] USERS = {"iris", "ancy"};
    private static final ZoneId STORED_CLOCK = ZoneOffset.UTC;
    private static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public IrisAncyAllMatchResetBootstrap(JdbcTemplate jdbcTemplate,
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
                resetAll();
                markPatchApplied();
            });
        } catch (Exception e) {
            log.error("Iris/Ancy all-match reset failed.", e);
        }
    }

    private void resetAll() {
        LocalDate monthStart = LeaderboardServiceImpl.currentMonthStart();
        String monthWall = utcWallStart(monthStart);
        Set<Long> matchIds = new LinkedHashSet<>();
        Set<Long> playerIds = new LinkedHashSet<>();
        Set<String> dailyKeys = new LinkedHashSet<>();
        for (String username : USERS) {
            Long userId = findUserId(username);
            if (userId == null) {
                log.warn("Iris/Ancy reset skipped; user {} not found.", username);
                continue;
            }
            playerIds.add(userId);
            for (Map<String, Object> row : openMatches(userId)) {
                long matchId = ((Number) row.get("id")).longValue();
                if (!matchIds.add(matchId)) {
                    continue;
                }
                List<Long> players = jdbcTemplate.query(
                        "SELECT user_id FROM match_players WHERE match_id = ?",
                        (rs, rowNum) -> rs.getLong("user_id"),
                        matchId);
                playerIds.addAll(players);
                LocalDate day = shanghaiDay(stringValue(row.get("ended_wall")));
                closeMatch(matchId, row.get("room_id"));
                int winnerType = number(row.get("winner_type"));
                boolean october = !day.isBefore(monthStart);
                if (october && (winnerType == 1 || winnerType == 2)) {
                    for (Long playerId : players) {
                        reverseExp(playerId, winnerType);
                        dailyKeys.add(playerId + "|" + day);
                    }
                } else if (october) {
                    for (Long playerId : players) {
                        dailyKeys.add(playerId + "|" + day);
                    }
                }
            }
        }
        playerIds.remove(null);
        for (Long playerId : playerIds) {
            rebuildMonthWinRate(playerId, monthWall);
        }
        for (String key : dailyKeys) {
            int split = key.indexOf('|');
            long playerId = Long.parseLong(key.substring(0, split));
            LocalDate day = LocalDate.parse(key.substring(split + 1));
            rebuildDailyBoard(playerId, day.toString(), utcWallStart(day), utcWallStart(day.plusDays(1)));
        }
        for (String username : USERS) {
            resetWeekly(username);
        }
        log.warn("Reset Iris/Ancy matches matchIds={} players={} dailyKeys={}",
                matchIds, playerIds, dailyKeys.size());
    }

    private List<Map<String, Object>> openMatches(long userId) {
        return jdbcTemplate.queryForList("""
                SELECT m.id, m.room_id, m.status, m.winner_type,
                       DATE_FORMAT(COALESCE(m.ended_at, m.started_at, m.created_at), '%Y-%m-%d %H:%i:%s') AS ended_wall
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND IFNULL(m.winner_type, 0) <> 3
                ORDER BY m.id
                """, userId);
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
        if (roomId instanceof Number number) {
            jdbcTemplate.update("""
                    UPDATE game_rooms
                    SET match_id = NULL
                    WHERE id = ? AND match_id = ?
                    """, number.longValue(), matchId);
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

    private void reverseExp(long userId, int winnerType) {
        if (winnerType != 1) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET exp = GREATEST(0, IFNULL(exp, 0) - 100)
                WHERE user_id = ?
                """, userId);
    }

    private void rebuildMonthWinRate(long userId, String monthWall) {
        Map<String, Object> before = jdbcTemplate.query("""
                SELECT IFNULL(win_count, 0) AS win_count,
                       IFNULL(lose_count, 0) AS lose_count
                FROM user_profiles
                WHERE user_id = ?
                LIMIT 1
                """, rs -> rs.next() ? Map.of(
                "win_count", rs.getInt("win_count"),
                "lose_count", rs.getInt("lose_count")) : Map.of(), userId);
        Map<String, Object> counts = jdbcTemplate.query("""
                SELECT IFNULL(SUM(m.winner_type = 1), 0) AS wins,
                       IFNULL(SUM(m.winner_type = 2), 0) AS losses
                FROM match_players mp
                INNER JOIN matches m ON m.id = mp.match_id
                WHERE mp.user_id = ?
                  AND m.status = 2
                  AND IFNULL(m.winner_type, 0) IN (1, 2)
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                """, rs -> rs.next() ? Map.of(
                "wins", rs.getInt("wins"),
                "losses", rs.getInt("losses")) : Map.of("wins", 0, "losses", 0),
                userId, monthWall);
        int wins = number(counts.get("wins"));
        int losses = number(counts.get("losses"));
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = ?,
                    lose_count = ?,
                    draw_count = 0
                WHERE user_id = ?
                """, wins, losses, userId);
        log.warn("Rebuilt month win-rate userId={}: {}/{} -> {}/{}",
                userId, before.get("win_count"), before.get("lose_count"), wins, losses);
    }

    private void rebuildDailyBoard(long userId, String period, String start, String end) {
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
        log.warn("Rebuilt daily board userId={} period={} remaining={} clawedGold={}",
                userId, period, remaining, clawed);
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

    private void resetWeekly(String username) {
        Long userId = findUserId(username);
        if (userId == null || !tableExists("user_tasks")) {
            return;
        }
        LocalDate week = QuestPeriod.weeklyStartForDaily(QuestPeriod.currentDailyDate());
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.progress_value = 0,
                    ut.status = 0,
                    ut.completed_at = NULL,
                    ut.extra_data = CAST('[]' AS JSON),
                    ut.updated_at = NOW()
                WHERE ut.user_id = ?
                  AND t.task_code = 'T-WEEKLY-TEAM-10'
                  AND ut.period_key = ?
                  AND ut.status < 3
                """, userId, week.toString());
    }

    static String utcWallStart(LocalDate shanghaiDay) {
        return shanghaiDay.atStartOfDay(WorkDayQuota.ZONE)
                .withZoneSameInstant(STORED_CLOCK)
                .toLocalDateTime()
                .format(SQL_TIME);
    }

    static LocalDate shanghaiDay(String utcWall) {
        if (utcWall == null || utcWall.isBlank()) {
            return LocalDate.now(WorkDayQuota.ZONE);
        }
        String text = utcWall.trim();
        if (text.length() > 19) {
            text = text.substring(0, 19);
        }
        LocalDateTime wall = LocalDateTime.parse(text, SQL_TIME);
        return wall.atZone(STORED_CLOCK).withZoneSameInstant(WorkDayQuota.ZONE).toLocalDate();
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
