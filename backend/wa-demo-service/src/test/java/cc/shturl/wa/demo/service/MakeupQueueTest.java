package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MakeupQueueTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 1);

    @Test
    @DisplayName("本月第一天没有更早的局可补")
    void firstDayHasNoMakeup() {
        assertThat(MakeupQueue.next(START, START, Map.of(), Set.of())).isNull();
    }

    @Test
    @DisplayName("昨天一把没打，先补第 1 局，这一天共欠 3 局")
    void emptyYesterdayStartsAtSlotOne() {
        LocalDate yesterday = LocalDate.of(2026, 11, 15);
        MakeupQueue.Next next = MakeupQueue.next(yesterday.plusDays(1), START,
                played(yesterday.plusDays(1), Map.of(yesterday, 0)), Set.of());
        assertThat(next.day()).isEqualTo(yesterday);
        assertThat(next.slot()).isEqualTo(1);
        assertThat(next.owedCount()).isEqualTo(3);
        assertThat(MakeupQueue.finishGold(next.slot())).isEqualTo(30);
    }

    @Test
    @DisplayName("昨天打了两把，只欠第 3 局")
    void twoPlayedLeavesThirdSlot() {
        LocalDate yesterday = LocalDate.of(2026, 11, 15);
        MakeupQueue.Next next = MakeupQueue.next(yesterday.plusDays(1), START,
                played(yesterday.plusDays(1), Map.of(yesterday, 2)), Set.of());
        assertThat(next.slot()).isEqualTo(3);
        assertThat(next.owedCount()).isEqualTo(1);
        assertThat(MakeupQueue.finishGold(3)).isEqualTo(50);
    }

    @Test
    @DisplayName("更早的空档排在昨天前面")
    void olderGapComesFirst() {
        LocalDate older = LocalDate.of(2026, 11, 10);
        LocalDate yesterday = LocalDate.of(2026, 11, 15);
        MakeupQueue.Next next = MakeupQueue.next(yesterday.plusDays(1), START,
                played(yesterday.plusDays(1), Map.of(older, 0, yesterday, 2)), Set.of());
        assertThat(next.day()).isEqualTo(older);
        assertThat(next.slot()).isEqualTo(1);
        assertThat(next.owedCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("放弃过的补打档不再出现，下一档顺延")
    void claimedSlotIsSkipped() {
        LocalDate day = LocalDate.of(2026, 11, 10);
        LocalDate today = LocalDate.of(2026, 11, 16);
        MakeupQueue.Next next = MakeupQueue.next(today, START,
                played(today, Map.of(day, 0)),
                Set.of(new MakeupQueue.DaySlot(day, 1)));
        assertThat(next.day()).isEqualTo(day);
        assertThat(next.slot()).isEqualTo(2);
        assertThat(MakeupQueue.finishGold(2)).isEqualTo(40);
        assertThat(next.owedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("当天三局都打完就没有欠账")
    void fullDayLeavesNothing() {
        LocalDate today = LocalDate.of(2026, 11, 16);
        assertThat(MakeupQueue.next(today, START, played(today, Map.of()), Set.of())).isNull();
    }

    /** 没写明的日期按当天 3 局都打完。 */
    private static Map<LocalDate, Integer> played(LocalDate today, Map<LocalDate, Integer> gaps) {
        Map<LocalDate, Integer> used = new HashMap<>();
        for (LocalDate day = START; day.isBefore(today); day = day.plusDays(1)) {
            used.put(day, gaps.getOrDefault(day, 3));
        }
        return used;
    }
}
