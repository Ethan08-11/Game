package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class QuestPeriodTest {

    @Test
    @DisplayName("每日 period 按自然日 0 点切换，20:00 仍属当天")
    void dailyDateRollsAtMidnight() {
        assertThat(QuestPeriod.dailyDate(LocalDateTime.of(2026, 10, 1, 19, 59)))
                .isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(QuestPeriod.dailyDate(LocalDateTime.of(2026, 10, 1, 20, 0)))
                .isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(QuestPeriod.dailyDate(LocalDateTime.of(2026, 10, 2, 0, 0)))
                .isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(QuestPeriod.endedDailyDate(LocalDateTime.of(2026, 10, 2, 0, 0)))
                .isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    @DisplayName("周常 period 在周一 0 点开启新的一周")
    void weeklyStartRollsMondayMidnight() {
        assertThat(QuestPeriod.weeklyStart(LocalDateTime.of(2026, 10, 4, 23, 59)))
                .isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(QuestPeriod.weeklyStart(LocalDateTime.of(2026, 10, 5, 0, 0)))
                .isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(QuestPeriod.weeklyStart(LocalDateTime.of(2026, 10, 5, 20, 0)))
                .isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(QuestPeriod.weeklyStartForDaily(LocalDate.of(2026, 10, 5)))
                .isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(QuestPeriod.weeklyStartForDaily(LocalDate.of(2026, 10, 6)))
                .isEqualTo(LocalDate.of(2026, 10, 5));
    }
}
