package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EthanIdleDailyFillSlotsTest {

    @Test
    @DisplayName("前三局缺几局就补几局")
    void missingSlotsFollowsUnplayedQuota() {
        assertThat(EthanIdleDailyFillService.missingSlots(0, 0)).isEqualTo(3);
        assertThat(EthanIdleDailyFillService.missingSlots(2, 0)).isEqualTo(1);
        assertThat(EthanIdleDailyFillService.missingSlots(2, 3)).isZero();
        assertThat(EthanIdleDailyFillService.missingSlots(3, 0)).isZero();
    }

    @Test
    @DisplayName("真实胜负在前，没打的名额补成胜利")
    void filledSlotsKeepRealResultsAheadOfAutoWins() {
        assertThat(EthanIdleDailyFillService.filledSlots(List.of(1, 1), 1)).containsExactly(1, 1, 1);
        assertThat(EthanIdleDailyFillService.filledSlots(List.of(1, 2), 1)).containsExactly(1, 2, 1);
        assertThat(EthanIdleDailyFillService.filledSlots(List.of(), 3)).containsExactly(1, 1, 1);
    }
}
