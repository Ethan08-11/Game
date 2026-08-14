package cc.shturl.wa.demo.dto.resp;

import java.util.List;

public record MatchActionResp(
        Long matchId,
        Long actionId,
        String clientActionId,
        String actionType,
        Long actorUserId,
        Long cardInstanceId,
        Long cardId,
        String cardName,
        String targetType,
        Long targetUserId,
        Integer remainingActionPoints,
        Integer appliedMultiplier,
        List<CardEffectResp> effects,
        Long version,
        boolean matchEnded,
        Integer winnerType
) {
}
