package cc.shturl.wa.demo.dto.resp;

import java.util.List;

public record MatchStateResp(
        Long matchId,
        String matchCode,
        Long roomId,
        Integer status,
        String phase,
        Integer currentRound,
        Long version,
        CustomerInfoResp customer,
        Long bullyId,
        String bossName,
        Integer bossMaxHp,
        Integer bossCurrentHp,
        Integer bossBaseAttack,
        Integer bossCurrentAttack,
        Integer customerTriggered,
        String customerEffectType,
        Integer customerEffectValue,
        Long firstPlayerUserId,
        Long firstPlayerChosenByUserId,
        Boolean waitingReconnect,
        Integer reconnectRemainingSeconds,
        List<MatchPlayerStateResp> players,
        List<MatchCardResp> hand,
        Integer winnerType
) {
}
