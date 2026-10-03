package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyTeammateHintsTest {

    @Test
    @DisplayName("本周上线并且打过的人排在只上线或没来过的人前面")
    void playedAndOnlineComesFirst() {
        List<WeeklyTeammateHints.Candidate> ordered = WeeklyTeammateHints.order(List.of(
                new WeeklyTeammateHints.Candidate(2, "只上线", WeeklyTeammateHints.priority(false, true)),
                new WeeklyTeammateHints.Candidate(3, "上线且打过", WeeklyTeammateHints.priority(true, true)),
                new WeeklyTeammateHints.Candidate(4, "没来过", WeeklyTeammateHints.priority(false, false))
        ), 1L, LocalDate.of(2026, 10, 5));

        assertThat(ordered).extracting(WeeklyTeammateHints.Candidate::userId).containsExactly(3L, 2L, 4L);
    }

    @Test
    @DisplayName("未领取前只露出 3 名推荐")
    void revealOnlyThree() {
        List<WeeklyTeammateHints.Candidate> ordered = List.of(
                new WeeklyTeammateHints.Candidate(1, "a", 3),
                new WeeklyTeammateHints.Candidate(2, "b", 3),
                new WeeklyTeammateHints.Candidate(3, "c", 3),
                new WeeklyTeammateHints.Candidate(4, "d", 3),
                new WeeklyTeammateHints.Candidate(5, "e", 2)
        );
        assertThat(WeeklyTeammateHints.reveal(ordered)).extracting(WeeklyTeammateHints.Candidate::userId)
                .containsExactly(1L, 2L, 3L);
    }
}
