package cc.shturl.wa.demo.dto.resp;

public record MatchRoundResp(
        Long matchId,
        Integer roundNo,
        String phase,
        Long firstPlayerUserId,
        Long chosenByUserId,
        Long currentActorUserId,
        Long version
) {
}
