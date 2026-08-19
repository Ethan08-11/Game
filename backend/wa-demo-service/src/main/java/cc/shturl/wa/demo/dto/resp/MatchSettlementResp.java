package cc.shturl.wa.demo.dto.resp;

import java.util.List;

public record MatchSettlementResp(
        Long matchId,
        String matchCode,
        Integer winnerType,
        boolean victory,
        Integer totalRounds,
        Integer durationSeconds,
        Integer bossMaxHp,
        Integer bossRemainingHp,
        List<PlayerSettlement> players
) {
    public record PlayerSettlement(
            Long userId,
            Integer seatNo,
            String deptType,
            Integer resultType,
            Integer maxHp,
            Integer remainingHp,
            Integer damageDealt,
            Integer damageTaken,
            Integer healingDone,
            Integer shieldGranted,
            Integer cardsPlayed,
            Integer actionPointsUsed,
            Integer expAwarded,
            Long moneyAwarded,
            Long unlockedCardId,
            String unlockedCardName,
            String unlockedCardImageUrl
    ) {
    }
}
