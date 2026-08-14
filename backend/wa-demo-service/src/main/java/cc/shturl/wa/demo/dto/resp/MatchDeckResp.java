package cc.shturl.wa.demo.dto.resp;

import java.util.List;

public record MatchDeckResp(
        Long matchId,
        Long userId,
        Integer totalCount,
        List<MatchCardResp> cards
) {
}
