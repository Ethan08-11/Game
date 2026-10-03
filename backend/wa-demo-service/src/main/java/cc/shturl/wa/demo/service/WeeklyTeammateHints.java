package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.WeeklyPlayerHint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 周任务未领取前，展示本周已经组过的队友，并按天追加推荐。
 * 推荐优先本周上线并且打过对局的人，不写入任务进度。
 */
@Component
public class WeeklyTeammateHints {
    static final int PER_DAY = 3;
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter STORED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String WEEKLY_CODE = "T-WEEKLY-TEAM-10";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public WeeklyTeammateHints(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Hints load(Long userId) {
        if (userId == null) {
            return Hints.NONE;
        }
        try {
            LocalDate today = QuestPeriod.currentDailyDate();
            LocalDate weekStart = QuestPeriod.weeklyStartForDaily(today);
            String period = weekStart.toString();
            TaskRow task = loadWeeklyTask(userId, period);
            if (task != null && task.status >= 3) {
                return Hints.NONE;
            }
            Set<Long> already = task == null ? Set.of() : parseIds(task.extraData);
            List<WeeklyPlayerHint> teammates = namesOf(already);
            List<Candidate> pool = candidates(userId, weekStart, weekStart.plusWeeks(1), already);
            int dayIndex = (int) Math.max(ChronoUnit.DAYS.between(weekStart, today), 0);
            List<WeeklyPlayerHint> suggestions = reveal(order(pool, userId, weekStart), dayIndex).stream()
                    .map(item -> new WeeklyPlayerHint(item.userId, item.name))
                    .toList();
            return new Hints(teammates, suggestions);
        } catch (Exception e) {
            return Hints.NONE;
        }
    }

    private TaskRow loadWeeklyTask(Long userId, String period) {
        List<TaskRow> rows = jdbcTemplate.query("""
                SELECT ut.status, ut.extra_data
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.user_id = ? AND t.task_code = ? AND ut.period_key = ?
                LIMIT 1
                """, (rs, row) -> new TaskRow(rs.getInt("status"), rs.getString("extra_data")),
                userId, WEEKLY_CODE, period);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<WeeklyPlayerHint> namesOf(Set<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String marks = String.join(",", ids.stream().map(id -> "?").toList());
        List<WeeklyPlayerHint> rows = jdbcTemplate.query("""
                SELECT u.id AS user_id,
                       COALESCE(NULLIF(TRIM(p.display_name), ''), u.username) AS name
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE u.id IN (""" + marks + """
                )
                ORDER BY name, u.id
                """, (rs, row) -> new WeeklyPlayerHint(rs.getLong("user_id"), rs.getString("name")),
                ids.toArray());
        return rows;
    }

    private List<Candidate> candidates(Long userId, LocalDate fromDay, LocalDate toDay, Set<Long> exclude) {
        String[] window = storedRange(fromDay, toDay);
        List<Candidate> rows = jdbcTemplate.query("""
                SELECT u.id AS user_id,
                       COALESCE(NULLIF(TRIM(p.display_name), ''), u.username) AS name,
                       (
                         SELECT COUNT(*)
                         FROM match_players mp
                         INNER JOIN matches m ON m.id = mp.match_id
                         WHERE mp.user_id = u.id
                           AND m.status IN (1, 2)
                           AND m.started_at >= ? AND m.started_at < ?
                       ) AS played_count,
                       CASE WHEN u.last_login_at >= ? AND u.last_login_at < ? THEN 1 ELSE 0 END AS online_flag
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE u.status = 1 AND u.id <> ?
                """, (rs, row) -> new Candidate(
                        rs.getLong("user_id"),
                        rs.getString("name"),
                        priority(rs.getInt("played_count") > 0, rs.getInt("online_flag") > 0)),
                window[0], window[1], window[0], window[1], userId);
        List<Candidate> eligible = new ArrayList<>();
        for (Candidate row : rows) {
            if (row.name == null || row.name.isBlank() || exclude.contains(row.userId)) {
                continue;
            }
            eligible.add(row);
        }
        return eligible;
    }

    static List<Candidate> order(List<Candidate> input, long self, LocalDate weekStart) {
        long epoch = weekStart.toEpochDay();
        return input.stream()
                .sorted(Comparator.comparingInt((Candidate item) -> item.priority).reversed()
                        .thenComparingInt(item -> tieBreak(self, item.userId, epoch))
                        .thenComparingLong(item -> item.userId))
                .toList();
    }

    static List<Candidate> reveal(List<Candidate> ordered, int dayIndex) {
        int count = (Math.max(dayIndex, 0) + 1) * PER_DAY;
        if (ordered.size() <= count) {
            return ordered;
        }
        return ordered.subList(0, count);
    }

    static int priority(boolean played, boolean online) {
        if (played && online) {
            return 3;
        }
        if (played) {
            return 2;
        }
        if (online) {
            return 1;
        }
        return 0;
    }

    static int tieBreak(long self, long other, long weekEpochDay) {
        long mixed = self * 1315423911L + other * 2654435761L + weekEpochDay;
        return (int) Math.floorMod(mixed, 1_000_000L);
    }

    private String[] storedRange(LocalDate fromDay, LocalDate toDay) {
        ZoneId stored = ZoneId.systemDefault();
        LocalDateTime from = fromDay.atStartOfDay(SHANGHAI).withZoneSameInstant(stored).toLocalDateTime();
        LocalDateTime to = toDay.atStartOfDay(SHANGHAI).withZoneSameInstant(stored).toLocalDateTime();
        return new String[] { STORED.format(from), STORED.format(to) };
    }

    private Set<Long> parseIds(String json) {
        Set<Long> ids = new LinkedHashSet<>();
        if (json == null || json.isBlank()) {
            return ids;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node != null && node.isArray()) {
                for (JsonNode item : node) {
                    if (item.isNumber()) {
                        ids.add(item.longValue());
                    }
                }
            }
        } catch (Exception ignored) {
            return ids;
        }
        return ids;
    }

    public record Hints(List<WeeklyPlayerHint> teammates, List<WeeklyPlayerHint> suggestions) {
        static final Hints NONE = new Hints(List.of(), List.of());
    }

    record Candidate(long userId, String name, int priority) {
    }

    private record TaskRow(int status, String extraData) {
    }
}
