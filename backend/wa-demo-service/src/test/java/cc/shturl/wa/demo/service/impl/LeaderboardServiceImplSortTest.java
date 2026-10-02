package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplSortTest {

    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private LeaderboardServiceImpl service;

    @Test
    @DisplayName("总榜：同金币同胜率时胜场多者在前，再比总场，最后 userId")
    void totalBoardBreaksTiesByWinsThenMatchesThenUserId() {
        UserProfile fewerWins = profile(3L, 100L, 4, 1, 0);
        UserProfile moreWinsLaterId = profile(2L, 100L, 8, 2, 0);
        UserProfile moreWinsEarlierId = profile(1L, 100L, 8, 2, 0);
        UserProfile moreMatchesZeroWin = profile(5L, 50L, 0, 5, 0);
        UserProfile fewerMatchesZeroWin = profile(4L, 50L, 0, 1, 0);
        when(userProfileMapper.selectList(any())).thenReturn(List.of(
                fewerWins, moreWinsLaterId, moreWinsEarlierId, moreMatchesZeroWin, fewerMatchesZeroWin));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());

        List<LeaderboardResp> list = service.listLeaderboard(null, "total", 1, 0);

        assertThat(list).extracting(LeaderboardResp::userId)
                .containsExactly(1L, 2L, 3L, 5L, 4L);
    }

    @Test
    @DisplayName("胜率保留两位小数：15 胜 4 负为 78.95")
    void winRateKeepsTwoDecimals() {
        UserProfile profile = profile(1L, 10L, 15, 4, 0);
        when(userProfileMapper.selectList(any())).thenReturn(List.of(profile));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());

        List<LeaderboardResp> list = service.listLeaderboard(null, "total", 1, 0);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).winRate()).isCloseTo(78.95, within(0.001));
    }

    @Test
    @DisplayName("总榜：同金币时先比精确胜率再比胜场，先攒到金币不能压过更高胜率")
    void sameGoldRanksByWinRateThenWinsBeforeReachedAt() {
        UserProfile laterBetterRate = profile(1L, 150L, 8, 0, 0);
        laterBetterRate.setMoneyReachedAt(LocalDateTime.of(2026, 10, 1, 20, 0));
        UserProfile earlierLowerRate = profile(2L, 150L, 3, 1, 0);
        earlierLowerRate.setMoneyReachedAt(LocalDateTime.of(2026, 10, 1, 11, 0));
        when(userProfileMapper.selectList(any())).thenReturn(List.of(laterBetterRate, earlierLowerRate));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());

        List<LeaderboardResp> list = service.listLeaderboard(null, "total", 1, 0);

        assertThat(list).extracting(LeaderboardResp::userId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("高压：当前总榜金币前五算高压，第六名不算")
    void liveTotalTopFiveIsHighPressureEvenWithoutSnapshot() {
        when(userProfileMapper.selectList(any())).thenReturn(List.of(
                profile(1L, 150L, 3, 0, 0),
                profile(2L, 140L, 2, 0, 0),
                profile(3L, 130L, 2, 0, 0),
                profile(4L, 120L, 1, 0, 0),
                profile(5L, 110L, 1, 0, 0),
                profile(6L, 100L, 1, 0, 0)));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());

        assertThat(service.teamTouchesDailyTop(List.of(5L, 99L))).isTrue();
        assertThat(service.teamTouchesDailyTop(List.of(6L, 99L))).isFalse();
    }

    private static UserProfile profile(long userId, long money, int wins, int losses, int draws) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setMoney(money);
        profile.setWinCount(wins);
        profile.setLoseCount(losses);
        profile.setDrawCount(draws);
        return profile;
    }
}
