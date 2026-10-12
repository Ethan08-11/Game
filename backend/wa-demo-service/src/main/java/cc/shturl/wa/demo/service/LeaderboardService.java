package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaderboardService {
    List<LeaderboardResp> listLeaderboard(Long currentUserId, String type, int page, int size);
    LeaderboardResp getMyRank(Long currentUserId, String type);
    void ensureCurrentMonth();
    void ensureDailyTopSnapshot();
    /**
     * 用当天 0 点的金币、已打工作日和周任务，重写这一天锁定的潜在前五。
     */
    int relockOpeningProjectedRoster(LocalDate day);
    boolean teamTouchesDailyTop(Collection<Long> userIds);
}
