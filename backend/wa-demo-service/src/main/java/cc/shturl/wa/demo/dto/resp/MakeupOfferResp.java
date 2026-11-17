package cc.shturl.wa.demo.dto.resp;

/**
 * 当前可补的最早一档。available 为 false 时今天不能再勾选。
 */
public record MakeupOfferResp(
        boolean available,
        String owedDay,
        int slot,
        int finishGold,
        int winGold,
        boolean usedToday,
        boolean paysGold,
        int owedCount
) {
    public static MakeupOfferResp none() {
        return new MakeupOfferResp(false, null, 0, 0, 0, false, false, 0);
    }
}
