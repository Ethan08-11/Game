package cc.shturl.wa.demo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

/** 每月可领金币的工作日定额：日历天数减 4 天单休；2026 年 9 月从 12 日起算 16 天；2026 年 10 月扣 7 天休假。 */
public final class WorkDayQuota {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final YearMonth LAUNCH_MONTH = YearMonth.of(2026, 9);
    public static final LocalDate LAUNCH_COUNT_START = LocalDate.of(2026, 9, 12);
    public static final YearMonth OCTOBER_VACATION_MONTH = YearMonth.of(2026, 10);
    private static final int LAUNCH_MONTH_DAYS = 16;
    private static final int REST_DAYS_PER_MONTH = 4;
    private static final int OCTOBER_VACATION_DAYS = 7;

    private WorkDayQuota() {
    }

    public static int days(YearMonth month) {
        if (month == null) {
            month = YearMonth.now(ZONE);
        }
        if (LAUNCH_MONTH.equals(month)) {
            return LAUNCH_MONTH_DAYS;
        }
        int restDays = OCTOBER_VACATION_MONTH.equals(month) ? OCTOBER_VACATION_DAYS : REST_DAYS_PER_MONTH;
        return Math.max(month.lengthOfMonth() - restDays, 1);
    }

    public static LocalDate countStart(YearMonth month) {
        if (month == null) {
            month = YearMonth.now(ZONE);
        }
        if (LAUNCH_MONTH.equals(month)) {
            return LAUNCH_COUNT_START;
        }
        return month.atDay(1);
    }
}
