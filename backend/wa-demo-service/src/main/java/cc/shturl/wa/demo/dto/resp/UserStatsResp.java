package cc.shturl.wa.demo.dto.resp;

public record UserStatsResp(Long userId, String username, Integer level, Integer exp, Integer winCount,
                            Integer loseCount, Integer drawCount, Integer totalMatches, Integer winRate) {
}
