package cc.shturl.wa.demo.dto.resp;

import java.util.List;

public record EndTurnResp(
        Long matchId,
        Long userId,
        Integer endedTurn,
        Integer discardedCount,
        boolean allPlayersEnded,
        boolean bossAttackResolved,
        Integer resolvedRound,
        List<BossAttackTargetResp> bossAttackTargets,
        boolean matchEnded,
        Integer winnerType,
        Integer currentRound,
        String phase,
        Long version,
        Integer handCount,
        Integer deckCount,
        Integer discardCount
) {
}
