package cc.shturl.wa.demo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 补打队列只决定下一档，不改已经记过的场次和金币。
 * 先补打过但没满 3 局的缺档，从最早的那天开始；再补最近一个一把没打的日子，按第 1、2、3 局往前。
 */
public final class MakeupQueue {
    public static final int WIN_GOLD = 10;

    private MakeupQueue() {
    }

    public record DaySlot(LocalDate day, int slot) {
    }

    public record Next(LocalDate day, int slot, int owedCount) {
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
        LocalDate yesterday = today.minusDays(1);
        List<DaySlot> partial = new ArrayList<>();
        List<DaySlot> empty = new ArrayList<>();
        for (LocalDate day = countStart; !day.isAfter(yesterday); day = day.plusDays(1)) {
            int used = 0;
            if (usedSlotsByDay != null) {
                used = Math.max(usedSlotsByDay.getOrDefault(day, 0), 0);
            }
            int occupied = Math.min(used, 3);
            List<DaySlot> bucket = occupied > 0 ? partial : empty;
            for (int slot = occupied + 1; slot <= 3; slot++) {
                DaySlot owedSlot = new DaySlot(day, slot);
                if (claimed != null && claimed.contains(owedSlot)) {
                    continue;
                }
                bucket.add(owedSlot);
            }
        }
        empty.sort(Comparator.comparing(DaySlot::day).reversed().thenComparingInt(DaySlot::slot));
        List<DaySlot> ordered = new ArrayList<>(partial.size() + empty.size());
        ordered.addAll(partial);
        ordered.addAll(empty);
        if (ordered.isEmpty()) {
            return null;
        }
        DaySlot first = ordered.get(0);
        return new Next(first.day(), first.slot(), ordered.size());
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
