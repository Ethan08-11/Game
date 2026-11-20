package cc.shturl.wa.demo.dto.resp;

/**
 * 当前可补的最早一档。available 为 false 时今天不能勾选。
 * 任务页展示 questSlot 这一档：还没打时是下一档，打完没领时停在这一档。
 */
public record MakeupOfferResp(
        boolean available,
        String owedDay,
        int slot,
        int finishGold,
        int winGold,
        boolean usedToday,
        boolean paysGold,
        int owedCount,
        boolean dailyReady,
        String questDay,
        int questSlot,
        int questFinishGold,
        int questWinGold,
        boolean finishReady,
        boolean winReady,
        boolean partialDebt,
        boolean monthClear
) {
    public static MakeupOfferResp none() {
        return new MakeupOfferResp(false, null, 0, 0, 0, false, false, 0,
                false, null, 0, 0, 0, false, false, false, false);
    }
}
