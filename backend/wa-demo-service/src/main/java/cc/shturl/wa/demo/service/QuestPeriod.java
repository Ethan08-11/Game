package cc.shturl.wa.demo.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 每日 / 每周任务按自然日 0 点（Asia/Shanghai）切换。
 * Ethan 的前三局补记仍在当天 20:00 触发，补的是当天 0 点到次日 0 点这一档。
 */
public final class QuestPeriod {
    public static final LocalTime RESET_AT = LocalTime.MIDNIGHT;
    public static final LocalTime ETHAN_FILL_AT = LocalTime.of(20, 0);

    private QuestPeriod() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(WorkDayQuota.ZONE);
    }

    /** 当前进行中的每日 period_key 日期。0 点起算新的一天。 */
    public static LocalDate currentDailyDate() {
        return dailyDate(now());
    }

    public static LocalDate dailyDate(LocalDateTime at) {
        return at.toLocalDate();
    }

    /** 刚结束的每日（昨天）。 */
    public static LocalDate endedDailyDate(LocalDateTime at) {
        return dailyDate(at).minusDays(1);
    }

    public static LocalDate currentWeeklyStart() {
        return weeklyStart(now());
    }

    /** 周常 period_key：周一 0 点开启新的一周。 */
    public static LocalDate weeklyStart(LocalDateTime at) {
        return at.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate weeklyStartForDaily(LocalDate dailyDate) {
        if (dailyDate == null) {
            return currentWeeklyStart();
        }
        return weeklyStart(dailyDate.atStartOfDay());
    }

    public static LocalDateTime windowStart(LocalDate dailyDate) {
        return dailyDate.atStartOfDay();
    }

    public static LocalDateTime windowEnd(LocalDate dailyDate) {
        return dailyDate.plusDays(1).atStartOfDay();
    }

    public static long secondsUntilDailyReset() {
        ZonedDateTime now = ZonedDateTime.now(WorkDayQuota.ZONE);
        ZonedDateTime next = now.toLocalDate().plusDays(1).atTime(RESET_AT).atZone(WorkDayQuota.ZONE);
        return Math.max(Duration.between(now, next).getSeconds(), 0L);
    }
}
