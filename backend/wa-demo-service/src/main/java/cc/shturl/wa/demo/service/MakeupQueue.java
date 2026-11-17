package cc.shturl.wa.demo.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * 本月更早没打完的局，按日期从早到晚、同一天按第 1、2、3 局排成补打队列。
 * 一天正常打完或放弃的局数占掉前面的档，补打结算过的档不再出现。
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
        Next first = null;
        int owed = 0;
        for (LocalDate day = countStart; !day.isAfter(yesterday); day = day.plusDays(1)) {
            int used = 0;
            if (usedSlotsByDay != null) {
                used = Math.max(usedSlotsByDay.getOrDefault(day, 0), 0);
            }
            int occupied = Math.min(used, 3);
            for (int slot = occupied + 1; slot <= 3; slot++) {
                if (claimed != null && claimed.contains(new DaySlot(day, slot))) {
                    continue;
                }
                owed++;
                if (first == null) {
                    first = new Next(day, slot, 0);
                }
            }
        }
        if (first == null) {
            return null;
        }
        return new Next(first.day(), first.slot(), owed);
    }
}
