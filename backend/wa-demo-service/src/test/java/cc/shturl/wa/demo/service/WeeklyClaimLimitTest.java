package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyClaimLimitTest {

    @Test
    @DisplayName("按北京时间自然月统计，库存的 UTC 墙钟从当月 1 日 0 点前 8 小时起")
    void octoberWindowUsesShanghaiMonthOnUtcClock() {
        LocalDateTime[] window = WeeklyClaimLimit.storedWindow(YearMonth.of(2026, 10), ZoneOffset.UTC);
        assertThat(window[0]).isEqualTo(LocalDateTime.of(2026, 9, 30, 16, 0));
        assertThat(window[1]).isEqualTo(LocalDateTime.of(2026, 10, 31, 16, 0));
    }
}
