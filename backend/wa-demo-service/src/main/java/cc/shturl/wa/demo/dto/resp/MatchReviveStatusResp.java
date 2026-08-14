package cc.shturl.wa.demo.dto.resp;

import java.time.LocalDateTime;

public record MatchReviveStatusResp(
        Long matchId,
        Long userId,
        boolean reviveEnabled,
        boolean canRevive,
        Integer reviveCount,
        Integer reviveLimit,
        Integer currentHp,
        Integer maxHp,
        Integer remainingSeconds,
        LocalDateTime lastReviveAt,
        Integer reviveStatus,
        String message
) {
}
