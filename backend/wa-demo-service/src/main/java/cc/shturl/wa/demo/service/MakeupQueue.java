package cc.shturl.wa.demo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 补打队列只决定下一档，不改已经记过的场次和金币。
 * 始终补最近一天没打满的局。打过但没满的，还欠就是漏掉的那几局；一把没打，这一天还欠 3 局。
 */
public final class MakeupQueue {
    public static final int WIN_GOLD = 10;

    private MakeupQueue() {
    }

    public record DaySlot(LocalDate day, int slot) {
    }

    /** owedCount 只算正在补的这一天还剩几局。partialDebt 为真表示这一天打过但没满。 */
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
        LocalDate yesterday = today.minusDays(1);
        for (LocalDate day = yesterday; !day.isBefore(countStart); day = day.minusDays(1)) {
            int used = 0;
            if (usedSlotsByDay != null) {
                used = Math.max(usedSlotsByDay.getOrDefault(day, 0), 0);
            }
            int occupied = Math.min(used, 3);
            DaySlot first = null;
            int owed = 0;
            for (int slot = occupied + 1; slot <= 3; slot++) {
                DaySlot owedSlot = new DaySlot(day, slot);
                if (claimed != null && claimed.contains(owedSlot)) {
                    continue;
                }
                if (first == null) {
                    first = owedSlot;
                }
                owed++;
            }
            if (first != null) {
                return new Next(first.day(), first.slot(), owed, occupied > 0);
            }
        }
        return null;
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
