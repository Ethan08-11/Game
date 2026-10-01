package cc.shturl.wa.demo.dto.resp;

import java.util.List;

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
        Integer discardCount,
        List<MatchCardResp> hand
) {
}
