package cc.shturl.wa.demo.service;

import java.time.YearMonth;
import java.time.ZoneId;

/** 每月可领金币的工作日定额：日历天数减 4 天单休；2026 年 9 月发布月固定 16 天。 */
public final class WorkDayQuota {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final YearMonth LAUNCH_MONTH = YearMonth.of(2026, 9);
    private static final int LAUNCH_MONTH_DAYS = 16;
    private static final int REST_DAYS_PER_MONTH = 4;

    private WorkDayQuota() {
    }

    public static int days(YearMonth month) {
        if (month == null) {
            month = YearMonth.now(ZONE);
        }
        if (LAUNCH_MONTH.equals(month)) {
            return LAUNCH_MONTH_DAYS;
        }
        return Math.max(month.lengthOfMonth() - REST_DAYS_PER_MONTH, 1);
    }
}
