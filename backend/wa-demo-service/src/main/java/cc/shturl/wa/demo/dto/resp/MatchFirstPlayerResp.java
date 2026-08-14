package cc.shturl.wa.demo.dto.resp;

public record MatchFirstPlayerResp(
        Long matchId,
        Integer roundNo,
        Long chosenByUserId,
        Long firstPlayerUserId,
        Long secondPlayerUserId,
        Long version,
        String phase
) {
}
