package cc.shturl.wa.demo.dto.resp;

public record CardEffectResp(
        String effectType,
        String triggerTiming,
        String targetType,
        Long targetUserId,
        Integer baseValue,
        Integer actualValue,
        Integer beforeValue,
        Integer afterValue,
        boolean scheduled,
        Integer triggerRound,
        Long pendingEffectId
) {
}
