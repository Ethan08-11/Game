package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class WorkDayQuotaTest {

    @Test
    void november2026Is21Days() {
        assertThat(WorkDayQuota.days(YearMonth.of(2026, 11))).isEqualTo(21);
    }

    @Test
    void october2026Stays24Days() {
        assertThat(WorkDayQuota.days(YearMonth.of(2026, 10))).isEqualTo(24);
    }

    @Test
    void ordinaryMonthSubtractsFourRestDays() {
        assertThat(WorkDayQuota.days(YearMonth.of(2026, 12))).isEqualTo(27);
        assertThat(WorkDayQuota.days(YearMonth.of(2026, 9))).isEqualTo(16);
    }
}
