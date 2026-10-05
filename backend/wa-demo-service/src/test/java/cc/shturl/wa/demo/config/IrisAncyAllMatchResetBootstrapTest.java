package cc.shturl.wa.demo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IrisAncyAllMatchResetBootstrapTest {

    @Test
    @DisplayName("上海 0 点对应 DATETIME 的 UTC 墙钟前一天 16:00")
    void utcWallStartUsesStoredClock() {
        assertThat(IrisAncyAllMatchResetBootstrap.utcWallStart(LocalDate.of(2026, 10, 1)))
                .isEqualTo("2026-09-30 16:00:00");
        assertThat(IrisAncyAllMatchResetBootstrap.utcWallStart(LocalDate.of(2026, 10, 5)))
                .isEqualTo("2026-10-04 16:00:00");
    }

    @Test
    @DisplayName("UTC 墙钟对局时间换算回上海自然日")
    void shanghaiDayFromUtcWall() {
        assertThat(IrisAncyAllMatchResetBootstrap.shanghaiDay("2026-10-01 15:17:30"))
                .isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(IrisAncyAllMatchResetBootstrap.shanghaiDay("2026-10-01 16:05:37"))
                .isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(IrisAncyAllMatchResetBootstrap.shanghaiDay("2026-10-04 21:07:03"))
                .isEqualTo(LocalDate.of(2026, 10, 5));
    }
}
