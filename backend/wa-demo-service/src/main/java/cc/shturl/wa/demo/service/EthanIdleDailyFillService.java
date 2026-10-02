package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.mapper.UserProfileMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ethan 当日 20:00 前一局都没打时，自动记 3 场胜利、领完刚结束那一档每日金币。
 * 任务日界与其他玩家相同（20:00 切到新一天），不预领新一天的任务，方便继续组队领金币。
 */
@Component
@Order(21)
@RequiredArgsConstructor
public class EthanIdleDailyFillService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(EthanIdleDailyFillService.class);
    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 10, 1);
    private static final int AUTO_WINS = 3;
    private static final int WIN_EXP = 100;
    private static final String WEEKLY_TEAM_CODE = "T-WEEKLY-TEAM-10";
    private static final List<String> DAILY_CODES = List.of(
            "T-DAILY-SLOT",
            "T-DAILY-MATCH-1", "T-DAILY-WIN-1",
            "T-DAILY-MATCH-2", "T-DAILY-WIN-2",
            "T-DAILY-MATCH-3", "T-DAILY-WIN-3"
    );

    private final JdbcTemplate jdbcTemplate;
    private final UserProfileMapper userProfileMapper;
    private final WorkDayService workDayService;
    private final LeaderboardService leaderboardService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(ApplicationArguments args) {
        LocalDate ended = QuestPeriod.endedDailyDate(QuestPeriod.now());
        fillIfIdle(ended.minusDays(1));
        fillIfIdle(ended);
        restoreVisibleDailyIfPreclaimed();
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Shanghai")
    public void fillEndedDayAtEightPm() {
        fillIfIdle(QuestPeriod.endedDailyDate(QuestPeriod.now()));
        restoreVisibleDailyIfPreclaimed();
    }

    @Scheduled(cron = "0 10 20 * * *", zone = "Asia/Shanghai")
    public void fillEndedDayIfMissed() {
        fillIfIdle(QuestPeriod.endedDailyDate(QuestPeriod.now()));
        restoreVisibleDailyIfPreclaimed();
    }

    public void fillIfIdle(LocalDate day) {
        if (day == null || day.isBefore(FIRST_DAY)) {
            return;
        }
        if (!tableExists("users") || !tableExists("matches") || !tableExists("tasks")) {
            return;
        }
        ensurePatchTable();
        String patchId = patchId(day);
        if (patchApplied(patchId)) {
            return;
        }
        Long userId = findEthanId();
        if (userId == null) {
            log.warn("Ethan idle daily fill skipped; user not found.");
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> doFill(userId, day, patchId));
    }

    private void doFill(Long userId, LocalDate day, String patchId) {
        if (patchApplied(patchId)) {
            return;
        }
        if (countedMatches(userId, day) > 0) {
            markPatch(patchId);
            log.info("Ethan played on {}, skip idle auto-fill.", day);
            return;
        }
        int created = insertAutoWins(userId, day);
        ensureProfile(userId);
        leaderboardService.ensureCurrentMonth();
        long gold = completeAndClaimDaily(userId, day);
        gold += bumpWeeklyTeam(userId, day, created);
        int exp = created * WIN_EXP;
        userProfileMapper.applyMatchSettlement(userId, created, 0, 0, exp, gold);
        markPatch(patchId);
        log.warn("Ethan idle auto-fill {}: +{} wins, +{} gold, +{} exp, weekly +{}.",
                day, created, gold, exp, created);
    }

    private int countedMatches(Long userId, LocalDate day) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM match_players mp
                INNER JOIN matches m ON m.id = mp.match_id
                WHERE mp.user_id = ?
                  AND m.status = 2
                  AND m.winner_type IN (1, 2)
                  AND COALESCE(m.started_at, m.created_at) >= ?
                  AND COALESCE(m.started_at, m.created_at) < ?
                """, Integer.class, userId, Timestamp.valueOf(QuestPeriod.windowStart(day)),
                Timestamp.valueOf(QuestPeriod.windowEnd(day)));
        return count == null ? 0 : count;
    }

    private int insertAutoWins(Long userId, LocalDate day) {
        Long customerId = queryId("SELECT id FROM customer_types WHERE IFNULL(status, 1) = 1 ORDER BY id LIMIT 1");
        Long bullyId = queryId("SELECT id FROM bullies WHERE IFNULL(status, 1) = 1 ORDER BY id LIMIT 1");
        String bossName = queryString("SELECT bully_name FROM bullies WHERE id = ?", bullyId);
        if (customerId == null || bullyId == null) {
            log.warn("Ethan idle auto-fill {} skipped match rows; catalog missing.", day);
            return 0;
        }
        int created = 0;
        for (int slot = 1; slot <= AUTO_WINS; slot++) {
            String code = autoMatchCode(day, slot);
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM matches WHERE match_code = ?", Integer.class, code);
            if (exists != null && exists > 0) {
                continue;
            }
            LocalDateTime start = QuestPeriod.windowEnd(day).minusMinutes(11L - slot);
            LocalDateTime end = start.plusMinutes(1);
            jdbcTemplate.update("""
                    INSERT INTO matches (
                      match_code, room_id, customer_type_id, bully_id, boss_name,
                      boss_satisfaction_target, boss_initial_satisfaction, boss_final_satisfaction,
                      status, phase, current_round, boss_max_hp, boss_current_hp,
                      boss_base_attack, boss_current_attack, boss_current_shield,
                      winner_type, version, duration_seconds, started_at, ended_at
                    ) VALUES (
                      ?, NULL, ?, ?, ?, 0, 0, 0,
                      2, 'FINISHED', 1, 150, 0,
                      17, 17, 0,
                      1, 1, 60, ?, ?
                    )
                    """, code, customerId, bullyId, bossName == null ? "硬扛恶霸" : bossName, start, end);
            Long matchId = queryId("SELECT id FROM matches WHERE match_code = ?", code);
            if (matchId == null) {
                continue;
            }
            jdbcTemplate.update("""
                    INSERT INTO match_players (
                      match_id, user_id, seat_no, dept_type, max_hp, current_hp,
                      shield, base_action_points, action_points, ended_turn, player_status,
                      revive_count, revive_limit, revive_status, result_type
                    ) VALUES (
                      ?, ?, 1, 'sales', 50, 50,
                      0, 3, 3, 1, 'ACTIVE',
                      0, 1, 0, 1
                    )
                    """, matchId, userId);
            created++;
        }
        return created;
    }

    /**
     * 若当前可见每日任务被 20:00 补记预领、但本窗口并没有实打对局，则清回未完成，
     * 与其他玩家同一刷新档，仍可组队领金币。已发金币不扣回。
     */
    private void restoreVisibleDailyIfPreclaimed() {
        if (!tableExists("users") || !tableExists("tasks") || !tableExists("user_tasks")) {
            return;
        }
        Long userId = findEthanId();
        if (userId == null) {
            return;
        }
        LocalDate current = QuestPeriod.currentDailyDate();
        if (countedMatches(userId, current) > 0) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> resetDailyTasks(userId, current));
    }

    private void resetDailyTasks(Long userId, LocalDate day) {
        String period = day.toString();
        int updated = 0;
        for (String code : DAILY_CODES) {
            Map<String, Object> task = queryTask(code);
            if (task == null) {
                continue;
            }
            long taskId = ((Number) task.get("id")).longValue();
            int target = Math.max(intVal(task.get("target_count"), 1), 1);
            updated += jdbcTemplate.update("""
                    UPDATE user_tasks
                    SET progress_value = 0,
                        target_value = ?,
                        status = 0,
                        completed_at = NULL,
                        claimed_at = NULL
                    WHERE user_id = ? AND task_id = ? AND period_key = ?
                      AND IFNULL(status, 0) > 0
                    """, target, userId, taskId, period);
        }
        if (updated > 0) {
            log.warn("Restored Ethan visible daily tasks for {} ({} rows); idle fill does not pre-claim the live board.",
                    period, updated);
        }
    }

    private long completeAndClaimDaily(Long userId, LocalDate day) {
        if (day == null) {
            return 0L;
        }
        String period = day.toString();
        boolean payGold = workDayService.allowGold(userId, day);
        long gold = 0L;
        LocalDateTime now = QuestPeriod.now();
        for (String code : DAILY_CODES) {
            Map<String, Object> task = queryTask(code);
            if (task == null) {
                continue;
            }
            long taskId = ((Number) task.get("id")).longValue();
            int target = Math.max(intVal(task.get("target_count"), 1), 1);
            int progress = slotProgress(code);
            int nextStatus = "T-DAILY-SLOT".equals(code) ? 1 : 3;
            jdbcTemplate.update("""
                    INSERT INTO user_tasks (
                      user_id, task_id, period_key, progress_value, target_value,
                      status, completed_at, claimed_at
                    ) VALUES (?, ?, ?, ?, ?, 0, NULL, NULL)
                    ON DUPLICATE KEY UPDATE id = id
                    """, userId, taskId, period, 0, target);
            Integer status = jdbcTemplate.queryForObject("""
                    SELECT status FROM user_tasks
                    WHERE user_id = ? AND task_id = ? AND period_key = ?
                    LIMIT 1
                    """, Integer.class, userId, taskId, period);
            if (status != null && status >= 3) {
                continue;
            }
            long reward = 0L;
            if (nextStatus == 3 && payGold && "money".equalsIgnoreCase(String.valueOf(task.get("reward_type")))) {
                reward = rewardAmount(String.valueOf(task.get("reward_value")));
            }
            jdbcTemplate.update("""
                    UPDATE user_tasks
                    SET progress_value = ?,
                        target_value = ?,
                        status = ?,
                        completed_at = CASE WHEN ? >= 2 THEN IFNULL(completed_at, ?) ELSE completed_at END,
                        claimed_at = CASE WHEN ? >= 3 THEN ? ELSE claimed_at END
                    WHERE user_id = ? AND task_id = ? AND period_key = ?
                    """, progress, target, nextStatus, nextStatus, now, nextStatus, now, userId, taskId, period);
            gold += reward;
        }
        return gold;
    }

    private long bumpWeeklyTeam(Long userId, LocalDate day, int added) {
        Map<String, Object> task = queryTask(WEEKLY_TEAM_CODE);
        if (task == null) {
            return 0L;
        }
        long taskId = ((Number) task.get("id")).longValue();
        int target = Math.max(intVal(task.get("target_count"), 10), 1);
        String period = QuestPeriod.weeklyStartForDaily(day).toString();
        jdbcTemplate.update("""
                INSERT INTO user_tasks (
                  user_id, task_id, period_key, progress_value, target_value,
                  extra_data, status, completed_at, claimed_at
                ) VALUES (?, ?, ?, 0, ?, CAST('[]' AS JSON), 0, NULL, NULL)
                ON DUPLICATE KEY UPDATE id = id
                """, userId, taskId, period, target);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT extra_data, status FROM user_tasks
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                LIMIT 1
                """, userId, taskId, period);
        if (rows.isEmpty()) {
            return 0L;
        }
        int status = intVal(rows.get(0).get("status"), 0);
        if (status >= 3) {
            return 0L;
        }
        Set<Long> ids = parseIdSet(extraDataString(rows.get(0).get("extra_data")));
        if (status < 2 && added > 0) {
            List<Long> candidates = jdbcTemplate.query(
                    "SELECT id FROM users WHERE id <> ? ORDER BY id",
                    (rs, i) -> rs.getLong(1), userId);
            int slot = 1;
            while (ids.size() < target && slot <= added) {
                Long next = nextTeammateId(ids, candidates, day, slot);
                if (next != null) {
                    ids.add(next);
                }
                slot++;
            }
        }
        int progress = Math.min(ids.size(), target);
        int nextStatus = progress >= target ? 3 : Math.max(status, progress > 0 ? 1 : 0);
        LocalDateTime now = QuestPeriod.now();
        long gold = 0L;
        if (nextStatus >= 3 && status < 3
                && workDayService.allowGold(userId, day)
                && "money".equalsIgnoreCase(String.valueOf(task.get("reward_type")))) {
            gold = rewardAmount(String.valueOf(task.get("reward_value")));
        }
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET progress_value = ?,
                    target_value = ?,
                    extra_data = CAST(? AS JSON),
                    status = ?,
                    completed_at = CASE WHEN ? >= 2 THEN IFNULL(completed_at, ?) ELSE completed_at END,
                    claimed_at = CASE WHEN ? >= 3 THEN ? ELSE claimed_at END
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                """, progress, target, writeIdSet(ids), nextStatus, nextStatus, now, nextStatus, now,
                userId, taskId, period);
        return gold;
    }

    private Long nextTeammateId(Set<Long> used, List<Long> candidates, LocalDate day, int slot) {
        for (Long candidate : candidates) {
            if (candidate != null && used.add(candidate)) {
                return candidate;
            }
        }
        long synthetic = -(day.toEpochDay() * 10L + slot);
        return used.contains(synthetic) ? null : synthetic;
    }

    private String extraDataString(Object value) {
        if (value == null) {
            return "[]";
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? "[]" : text;
    }

    private Set<Long> parseIdSet(String json) {
        Set<Long> ids = new LinkedHashSet<>();
        if (json == null || json.isBlank() || "null".equalsIgnoreCase(json)) {
            return ids;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isArray()) {
                return ids;
            }
            for (JsonNode item : node) {
                if (item.isNumber()) {
                    ids.add(item.longValue());
                }
            }
        } catch (Exception ignored) {
            return ids;
        }
        return ids;
    }

    private String writeIdSet(Set<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            return "[]";
        }
    }

    private int slotProgress(String code) {
        if ("T-DAILY-SLOT".equals(code)) {
            return AUTO_WINS;
        }
        if (code.startsWith("T-DAILY-MATCH-")) {
            return Integer.parseInt(code.substring("T-DAILY-MATCH-".length()));
        }
        return 1;
    }

    private Map<String, Object> queryTask(String code) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, task_code, reward_type, reward_value, target_count
                FROM tasks WHERE task_code = ? AND IFNULL(status, 1) = 1 LIMIT 1
                """, code);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private long rewardAmount(String rewardValue) {
        if (rewardValue == null || rewardValue.isBlank() || "null".equals(rewardValue)) {
            return 0L;
        }
        try {
            JsonNode node = objectMapper.readTree(rewardValue);
            return node.path("amount").asLong(0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long findEthanId() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.id AS user_id
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE LOWER(u.username) = 'ethan'
                   OR LOWER(IFNULL(p.display_name, '')) = 'ethan'
                ORDER BY CASE WHEN LOWER(u.username) = 'ethan' THEN 0 ELSE 1 END, u.id
                LIMIT 1
                """);
        if (rows.isEmpty()) {
            return null;
        }
        return ((Number) rows.get(0).get("user_id")).longValue();
    }

    private void ensureProfile(Long userId) {
        jdbcTemplate.update("""
                INSERT INTO user_profiles (user_id, display_name, win_count, lose_count, draw_count, money, created_at, updated_at)
                SELECT u.id, u.username, 0, 0, 0, 0, NOW(), NOW()
                FROM users u
                WHERE u.id = ?
                  AND NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id)
                """, userId);
    }

    private String autoMatchCode(LocalDate day, int slot) {
        return "AE" + day.toString().replace("-", "") + slot;
    }

    private String patchId(LocalDate day) {
        return "ethan_idle_daily_" + day;
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

    private boolean patchApplied(String patchId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_patches WHERE patch_id = ?", Integer.class, patchId);
        return count != null && count > 0;
    }

    private void markPatch(String patchId) {
        jdbcTemplate.update("INSERT IGNORE INTO schema_patches(patch_id) VALUES (?)", patchId);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }

    private Long queryId(String sql) {
        List<Long> rows = jdbcTemplate.query(sql, (rs, i) -> rs.getLong(1));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Long queryId(String sql, Object arg) {
        List<Long> rows = jdbcTemplate.query(sql, (rs, i) -> rs.getLong(1), arg);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String queryString(String sql, Object arg) {
        List<String> rows = jdbcTemplate.query(sql, (rs, i) -> rs.getString(1), arg);
        if (rows.isEmpty() || rows.get(0) == null) {
            return null;
        }
        return rows.get(0);
    }

    private int intVal(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }
}
