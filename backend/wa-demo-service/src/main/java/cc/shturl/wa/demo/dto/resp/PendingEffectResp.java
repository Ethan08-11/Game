package cc.shturl.wa.demo.dto.resp;

public record PendingEffectResp(
        Long id,
        String effectType,
        String targetType,
        Long targetUserId,
        Integer effectValue,
        Integer triggerRound,
        Integer remainingTriggers,
        String status
) {
}
