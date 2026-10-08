package cc.shturl.wa.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 周任务按北京时间自然月计次。一个月里的日历周可能有 5 个，最多只发 4 次。
 */
public final class WeeklyClaimLimit {
    public static final int PER_MONTH = 4;
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter STORED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private WeeklyClaimLimit() {
    }

    public static int countThisMonth(JdbcTemplate jdbcTemplate, Long userId) {
        if (jdbcTemplate == null || userId == null) {
            return 0;
        }
        LocalDateTime[] window = storedWindow(YearMonth.now(SHANGHAI), ZoneId.systemDefault());
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.user_id = ?
                  AND LOWER(t.task_type) = 'weekly'
                  AND ut.status >= 3
                  AND ut.claimed_at >= ?
                  AND ut.claimed_at < ?
                """, Integer.class, userId, STORED.format(window[0]), STORED.format(window[1]));
        return count == null ? 0 : count;
    }

    public static boolean allowed(JdbcTemplate jdbcTemplate, Long userId) {
        return countThisMonth(jdbcTemplate, userId) < PER_MONTH;
    }

    /** 与 {@link #countThisMonth} 相同的 claimed_at 字符串边界，供批量统计。 */
    public static String[] claimedAtBounds(YearMonth month) {
        LocalDateTime[] window = storedWindow(month, ZoneId.systemDefault());
        return new String[] { STORED.format(window[0]), STORED.format(window[1]) };
    }

    /** 北京时间该月起止，换成库里 claimed_at 使用的系统时区墙钟。用字符串比较，避免 JDBC 时区再加 8 小时。 */
    static LocalDateTime[] storedWindow(YearMonth month, ZoneId storedZone) {
        ZonedDateTime start = month.atDay(1).atStartOfDay(SHANGHAI);
        ZonedDateTime end = month.plusMonths(1).atDay(1).atStartOfDay(SHANGHAI);
        ZoneId zone = storedZone == null ? ZoneId.systemDefault() : storedZone;
        return new LocalDateTime[] {
                start.withZoneSameInstant(zone).toLocalDateTime(),
                end.withZoneSameInstant(zone).toLocalDateTime()
        };
    }
}
