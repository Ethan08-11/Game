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
        assertThat(next.partialDebt()).isFalse();
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
        assertThat(next.partialDebt()).isTrue();
        assertThat(MakeupQueue.finishGold(3)).isEqualTo(50);
    }

    @Test
    @DisplayName("打过但没满的缺档，排在更早的空档前面")
    void partialGapBeforeOlderEmptyDay() {
        LocalDate older = LocalDate.of(2026, 11, 10);
        LocalDate yesterday = LocalDate.of(2026, 11, 15);
        MakeupQueue.Next next = MakeupQueue.next(yesterday.plusDays(1), START,
                played(yesterday.plusDays(1), Map.of(older, 0, yesterday, 2)), Set.of());
        assertThat(next.day()).isEqualTo(yesterday);
        assertThat(next.slot()).isEqualTo(3);
        assertThat(next.owedCount()).isEqualTo(1);
        assertThat(next.partialDebt()).isTrue();
    }

    @Test
    @DisplayName("两天都没打时，先补最近一天的第 1 局")
    void newestEmptyDayComesFirst() {
        LocalDate older = LocalDate.of(2026, 11, 10);
        LocalDate newer = LocalDate.of(2026, 11, 15);
        MakeupQueue.Next next = MakeupQueue.next(newer.plusDays(1), START,
                played(newer.plusDays(1), Map.of(older, 0, newer, 0)), Set.of());
        assertThat(next.day()).isEqualTo(newer);
        assertThat(next.slot()).isEqualTo(1);
        assertThat(next.owedCount()).isEqualTo(3);
        assertThat(next.partialDebt()).isFalse();
    }

    @Test
    @DisplayName("两个没打满的日子，先补最近那天漏掉的局")
    void newerPartialComesFirst() {
        LocalDate older = LocalDate.of(2026, 11, 10);
        LocalDate newer = LocalDate.of(2026, 11, 15);
        MakeupQueue.Next next = MakeupQueue.next(newer.plusDays(1), START,
                played(newer.plusDays(1), Map.of(older, 1, newer, 2)), Set.of());
        assertThat(next.day()).isEqualTo(newer);
        assertThat(next.slot()).isEqualTo(3);
        assertThat(next.owedCount()).isEqualTo(1);
        assertThat(next.partialDebt()).isTrue();
    }

    @Test
    @DisplayName("最近一天一把没打，就补这一天，不先补更早没打满的日子")
    void newestDayWinsOverOlderPartial() {
        LocalDate older = LocalDate.of(2026, 11, 10);
        LocalDate newer = LocalDate.of(2026, 11, 15);
        MakeupQueue.Next next = MakeupQueue.next(newer.plusDays(1), START,
                played(newer.plusDays(1), Map.of(older, 1, newer, 0)), Set.of());
        assertThat(next.day()).isEqualTo(newer);
        assertThat(next.slot()).isEqualTo(1);
        assertThat(next.owedCount()).isEqualTo(3);
        assertThat(next.partialDebt()).isFalse();
    }

    @Test
    @DisplayName("月底可以把同一天剩下的第 2、第 3 局补完")
    void lastDayContinuesTheSameOwedDay() {
        LocalDate last = LocalDate.of(2026, 11, 30);
        LocalDate owed = LocalDate.of(2026, 11, 29);
        assertThat(MakeupQueue.monthEndContinuation(last, owed, 2,
                java.util.List.of(new MakeupQueue.DaySlot(owed, 1)))).isTrue();
        assertThat(MakeupQueue.monthEndContinuation(last, owed, 3,
                java.util.List.of(new MakeupQueue.DaySlot(owed, 1), new MakeupQueue.DaySlot(owed, 2)))).isTrue();
        assertThat(MakeupQueue.monthEndContinuation(last.minusDays(1), owed, 2,
                java.util.List.of(new MakeupQueue.DaySlot(owed, 1)))).isFalse();
        assertThat(MakeupQueue.monthEndContinuation(last, owed.minusDays(1), 1,
                java.util.List.of(new MakeupQueue.DaySlot(owed, 1)))).isFalse();
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
        assertThat(next.partialDebt()).isFalse();
    }

    @Test
    @DisplayName("正常打下去还能领满 21 天，最近一天没打也不欠")
    void emptyRecentDayIsClearWhenDailyQuotaStillFills() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        assertThat(MakeupQueue.monthClear(today, START, 21, played(today, Map.of(LocalDate.of(2026, 11, 19), 0)), Set.of(), Set.of())).isTrue();
    }

    @Test
    @DisplayName("剩下的天数不够领满每日任务，最近一天没打也算欠")
    void emptyRecentDayIsNotClearWhenDailyQuotaCannotFill() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        Map<LocalDate, Integer> used = new HashMap<>();
        for (LocalDate day = START; day.isBefore(today); day = day.plusDays(1)) {
            used.put(day, day.getDayOfMonth() <= 8 ? 3 : 0);
        }
        assertThat(MakeupQueue.monthClear(today, START, 21, used, Set.of(), Set.of())).isFalse();
    }

    @Test
    @DisplayName("还有打过没满的漏档，不算正常打")
    void openPartialBlocksClearMonth() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        assertThat(MakeupQueue.monthClear(today, START, 21, played(today, Map.of(LocalDate.of(2026, 11, 10), 2)), Set.of(), Set.of())).isFalse();
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
