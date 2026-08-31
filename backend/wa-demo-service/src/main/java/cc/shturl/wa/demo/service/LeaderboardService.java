package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;

import java.util.List;

public interface LeaderboardService {
    List<LeaderboardResp> listLeaderboard(Long currentUserId, String type, int page, int size);
    LeaderboardResp getMyRank(Long currentUserId, String type);
    void ensureCurrentMonth();
}
