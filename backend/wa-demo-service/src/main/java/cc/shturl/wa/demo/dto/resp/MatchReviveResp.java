package cc.shturl.wa.demo.dto.resp;

import java.time.LocalDateTime;

public record MatchReviveResp(
        Long matchId,
        Long userId,
        Integer beforeHp,
        Integer afterHp,
        Integer reviveCount,
        Integer reviveStatus,
        Integer currentRound,
        Long version,
        LocalDateTime revivedAt,
        String message
) {
}
