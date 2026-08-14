package cc.shturl.wa.demo.dto.resp;

public record CardEffectDetailResp(
        Long effectId,
        Long cardId,
        Integer effectOrder,
        String effectScope,
        String effectType,
        String triggerTiming,
        Integer triggerDelay,
        Integer remainingTriggers,
        String stackRule,
        Integer durationRounds,
        Integer value,
        String targetRule,
        String extraData
) {
}
