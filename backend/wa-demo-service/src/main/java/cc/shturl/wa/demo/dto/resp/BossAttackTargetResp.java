package cc.shturl.wa.demo.dto.resp;

public record BossAttackTargetResp(
        Long userId,
        Integer attack,
        Integer shieldBefore,
        Integer absorbedDamage,
        Integer hpBefore,
        Integer hpDamage,
        Integer hpAfter,
        boolean dead
) {
}
