package cc.shturl.wa.demo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 补打队列只决定下一档，不改已经记过的场次和金币。
 * 漏打优先：正常打过但没满，或者已经补过至少一档还没补满。再补一把没打的日子。同一类里补最近的一天。
 */
public final class MakeupQueue {
    public static final int WIN_GOLD = 10;

    private MakeupQueue() {
    }

    public record DaySlot(LocalDate day, int slot) {
    }

    /** owedCount 只算正在补的这一天还剩几局。partialDebt 为真表示漏打。 */
    public record Next(LocalDate day, int slot, int owedCount, boolean partialDebt) {
    }

    public static int finishGold(int slot) {
        return switch (slot) {
            case 1 -> 30;
            case 2 -> 40;
            case 3 -> 50;
            default -> 0;
        };
    }

    /**
     * @param usedSlotsByDay 该日正常结束或放弃、且不是补打的对局数
     * @param claimed        已经补过或放弃补打的档
     */
    public static Next next(LocalDate today, LocalDate countStart,
                            Map<LocalDate, Integer> usedSlotsByDay,
                            Set<DaySlot> claimed) {
        if (today == null || countStart == null || !today.isAfter(countStart)) {
            return null;
        }
        Next partial = scan(today, countStart, usedSlotsByDay, claimed, true);
        if (partial != null) {
            return partial;
        }
        return scan(today, countStart, usedSlotsByDay, claimed, false);
    }

    /** partialOnly 为真时只看漏打，否则只看一把没打的日子。都从昨天往前找最近的一天。 */
    private static Next scan(LocalDate today, LocalDate countStart,
                             Map<LocalDate, Integer> usedSlotsByDay,
                             Set<DaySlot> claimed, boolean partialOnly) {
        LocalDate yesterday = today.minusDays(1);
        for (LocalDate day = yesterday; !day.isBefore(countStart); day = day.minusDays(1)) {
            Gap gap = gap(day, usedOn(usedSlotsByDay, day), claimed);
            if (gap.owed() <= 0 || partialOnly != gap.leaked()) {
                continue;
            }
            return new Next(day, gap.slot(), gap.owed(), gap.leaked());
        }
        return null;
    }

    /** 正常打过，或已经补过至少一档，都算漏打。补过的档不再排。 */
    private record Gap(int slot, int owed, boolean leaked) {
    }

    private static int usedOn(Map<LocalDate, Integer> usedSlotsByDay, LocalDate day) {
        if (usedSlotsByDay == null || day == null) {
            return 0;
        }
        return Math.max(usedSlotsByDay.getOrDefault(day, 0), 0);
    }

    private static Gap gap(LocalDate day, int used, Set<DaySlot> claimed) {
        int played = Math.min(used, 3);
        int first = 0;
        int owed = 0;
        boolean touched = played > 0;
        for (int slot = 1; slot <= 3; slot++) {
            boolean madeUp = claimed != null && claimed.contains(new DaySlot(day, slot));
            if (slot <= played || madeUp) {
                if (madeUp) {
                    touched = true;
                }
                continue;
            }
            if (first == 0) {
                first = slot;
            }
            owed++;
        }
        return new Gap(first, owed, touched && owed > 0);
    }

    /**
     * 不欠：没有打过却没打满的漏档，并且从今天起照常打，这个月的每日任务还能领满。
     * 已经打满或已经记过工作日的日期算领过；今天到月底每天还能再领一天。
     */
    public static boolean monthClear(LocalDate today, LocalDate countStart, int quota,
                                     Map<LocalDate, Integer> usedSlotsByDay,
                                     Set<LocalDate> recordedDays,
                                     Set<DaySlot> claimed) {
        if (today == null || countStart == null || quota <= 0 || hasOpenPartial(today, countStart, usedSlotsByDay, claimed)) {
            return false;
        }
        int covered = 0;
        LocalDate yesterday = today.minusDays(1);
        for (LocalDate day = countStart; !day.isAfter(yesterday); day = day.plusDays(1)) {
            int used = 0;
            if (usedSlotsByDay != null) {
                used = Math.max(usedSlotsByDay.getOrDefault(day, 0), 0);
            }
            if (used >= 3 || (recordedDays != null && recordedDays.contains(day))) {
                covered++;
            }
        }
        long ahead = ChronoUnit.DAYS.between(today, YearMonth.from(today).atEndOfMonth()) + 1;
        return covered + ahead >= quota;
    }

    private static boolean hasOpenPartial(LocalDate today, LocalDate countStart,
                                          Map<LocalDate, Integer> usedSlotsByDay,
                                          Set<DaySlot> claimed) {
        if (!today.isAfter(countStart)) {
            return false;
        }
        LocalDate yesterday = today.minusDays(1);
        for (LocalDate day = countStart; !day.isAfter(yesterday); day = day.plusDays(1)) {
            if (gap(day, usedOn(usedSlotsByDay, day), claimed).leaked()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 本月最后一天，正在补的那一天如果还欠后面的档，允许当天继续补完。
     * 不跨到另一个欠账日，避免把整月没排到的空档一次补掉。
     */
    public static boolean monthEndContinuation(LocalDate today, LocalDate nextDay, int nextSlot,
                                               List<DaySlot> settledToday) {
        if (today == null || nextDay == null || nextSlot < 1 || settledToday == null || settledToday.isEmpty()) {
            return false;
        }
        if (!today.equals(YearMonth.from(today).atEndOfMonth())) {
            return false;
        }
        LocalDate owed = null;
        int maxSlot = 0;
        for (DaySlot slot : settledToday) {
            if (slot == null || slot.day() == null) {
                return false;
            }
            if (owed == null) {
                owed = slot.day();
            } else if (!owed.equals(slot.day())) {
                return false;
            }
            maxSlot = Math.max(maxSlot, slot.slot());
        }
        return nextDay.equals(owed) && nextSlot > maxSlot && nextSlot <= 3;
    }
}
