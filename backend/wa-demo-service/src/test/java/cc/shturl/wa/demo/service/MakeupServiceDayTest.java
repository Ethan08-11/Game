package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MakeupServiceDayTest {

    @Test
    @DisplayName("UTC 墙钟凌晨局算到上海当天，不滑到前一天")
    void earlyMorningMatchStaysOnShanghaiDay() {
        assertThat(MakeupService.shanghaiDay("2026-11-17 17:58:26"))
                .isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(MakeupService.shanghaiDay("2026-11-17 18:13:02"))
                .isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(MakeupService.shanghaiDay("2026-11-18 13:51:00"))
                .isEqualTo(LocalDate.of(2026, 11, 18));
    }
}
