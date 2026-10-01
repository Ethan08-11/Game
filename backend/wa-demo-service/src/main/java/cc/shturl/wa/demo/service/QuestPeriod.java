package cc.shturl.wa.demo.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 每日 / 每周任务与 Ethan 未打补记均在 20:00（Asia/Shanghai）切换。
 * 每日 period 覆盖「前一天 20:00 至当天 20:00」；周常在周一 20:00 开启新的一周。
 */
public final class QuestPeriod {
    public static final LocalTime RESET_AT = LocalTime.of(20, 0);

    private QuestPeriod() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(WorkDayQuota.ZONE);
    }

    /** 当前进行中的每日 period_key 日期。20:00 起算下一天。 */
    public static LocalDate currentDailyDate() {
        return dailyDate(now());
    }

    public static LocalDate dailyDate(LocalDateTime at) {
        if (at.toLocalTime().isBefore(RESET_AT)) {
            return at.toLocalDate();
        }
        return at.toLocalDate().plusDays(1);
    }

    /** 20:00 触发时要补记的那一天（刚结束的每日）。 */
    public static LocalDate endedDailyDate(LocalDateTime at) {
        return dailyDate(at).minusDays(1);
    }

    public static LocalDate currentWeeklyStart() {
        return weeklyStart(now());
    }

    /** 周常 period_key：周一 20:00 开启新的一周。 */
    public static LocalDate weeklyStart(LocalDateTime at) {
        return at.minusHours(RESET_AT.getHour())
                .minusMinutes(RESET_AT.getMinute())
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate weeklyStartForDaily(LocalDate dailyDate) {
        if (dailyDate == null) {
            return currentWeeklyStart();
        }
        return weeklyStart(dailyDate.atTime(RESET_AT).minusSeconds(1));
    }

    public static LocalDateTime windowStart(LocalDate dailyDate) {
        return dailyDate.minusDays(1).atTime(RESET_AT);
    }

    public static LocalDateTime windowEnd(LocalDate dailyDate) {
        return dailyDate.atTime(RESET_AT);
    }

    public static long secondsUntilDailyReset() {
        ZonedDateTime now = ZonedDateTime.now(WorkDayQuota.ZONE);
        ZonedDateTime next = now.with(RESET_AT).withSecond(0).withNano(0);
        if (!now.toLocalTime().isBefore(RESET_AT)) {
            next = next.plusDays(1);
        }
        return Math.max(Duration.between(now, next).getSeconds(), 0L);
    }
}
