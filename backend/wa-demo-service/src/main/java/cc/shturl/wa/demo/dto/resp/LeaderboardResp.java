package cc.shturl.wa.demo.dto.resp;

public record LeaderboardResp(Integer rank, Long userId, String username, String displayName,
                              String avatarUrl, Long money, Integer winRate) {
}
