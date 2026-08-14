package cc.shturl.wa.demo.dto.resp;

public record MatchPlayerStateResp(
        Long userId,
        Integer seatNo,
        String deptType,
        Integer maxHp,
        Integer currentHp,
        Integer shield,
        Integer actionPoints,
        Integer endedTurn,
        String playerStatus,
        Integer handCount,
        Integer deckCount,
        Integer discardCount
) {
}
