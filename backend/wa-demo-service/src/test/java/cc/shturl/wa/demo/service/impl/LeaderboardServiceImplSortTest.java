package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.WorkDayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplSortTest {

    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private WorkDayService workDayService;

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

        assertThat(service.teamTouchesDailyTop(List.of(5L, 99L), LocalDate.of(2026, 10, 8))).isTrue();
        assertThat(service.teamTouchesDailyTop(List.of(6L, 99L), LocalDate.of(2026, 10, 8))).isFalse();
        assertThat(service.highPressureRoster()).hasSize(5).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("高压：第六名把未领或本局完成档金币算进去能进前五则算高压")
    void projectedGoldThatWouldEnterTopFiveIsHighPressure() {
        when(userProfileMapper.selectList(any())).thenReturn(List.of(
                profile(1L, 150L, 3, 0, 0),
                profile(2L, 140L, 2, 0, 0),
                profile(3L, 130L, 2, 0, 0),
                profile(4L, 120L, 1, 0, 0),
                profile(5L, 110L, 1, 0, 0),
                profile(6L, 100L, 1, 0, 0)));

        assertThat(service.ranksAsTopFive(6L, 0L)).isFalse();
        assertThat(service.ranksAsTopFive(6L, 11L)).isTrue();
        assertThat(service.ranksAsTopFive(6L, 50L)).isTrue();
    }

    @Test
    @DisplayName("高压：已完成未领的每日金币会计入前五，名单仍最多 5 人")
    void unclaimedCompletedDailyGoldCanTriggerHighPressure() {
        stubSixPlayerBoard();
        when(jdbcTemplate.queryForList(anyString(), any(Object.class))).thenReturn(List.of(taskRow(
                6L, "T-DAILY-MATCH-2", "{\"amount\":50}", 2)));

        assertThat(service.teamTouchesDailyTop(List.of(6L), LocalDate.of(2026, 10, 8))).isTrue();
        assertThat(service.highPressureRoster()).hasSize(5).contains(6L).doesNotContain(5L);
    }

    @Test
    @DisplayName("高压：休息日未领金币不计入潜在前五")
    void restDayPendingGoldDoesNotTriggerHighPressure() {
        stubSixPlayerBoard();
        when(workDayService.snapshot(6L)).thenReturn(new WorkDayService.Snapshot(24, 24, true, false));
        when(jdbcTemplate.queryForList(anyString(), any(Object.class))).thenReturn(List.of(taskRow(
                6L, "T-DAILY-MATCH-2", "{\"amount\":50}", 2)));

        assertThat(service.teamTouchesDailyTop(List.of(6L), LocalDate.of(2026, 10, 8))).isFalse();
    }

    @Test
    @DisplayName("高压：每月 1 日至 7 日不针对潜在前五")
    void firstWeekOfMonthDisablesHighPressure() {
        stubSixPlayerBoard();

        assertThat(LeaderboardServiceImpl.highPressureEnabledOn(LocalDate.of(2026, 10, 1))).isFalse();
        assertThat(LeaderboardServiceImpl.highPressureEnabledOn(LocalDate.of(2026, 10, 7))).isFalse();
        assertThat(LeaderboardServiceImpl.highPressureEnabledOn(LocalDate.of(2026, 10, 8))).isTrue();
        assertThat(service.teamTouchesDailyTop(List.of(1L), LocalDate.of(2026, 10, 2))).isFalse();
        assertThat(service.teamTouchesDailyTop(List.of(1L), LocalDate.of(2026, 10, 8))).isTrue();
    }

    private void stubSixPlayerBoard() {
        when(userProfileMapper.selectList(any())).thenReturn(List.of(
                profile(1L, 150L, 3, 0, 0),
                profile(2L, 140L, 2, 0, 0),
                profile(3L, 130L, 2, 0, 0),
                profile(4L, 120L, 1, 0, 0),
                profile(5L, 110L, 1, 0, 0),
                profile(6L, 100L, 1, 0, 0)));
    }

    private static Map<String, Object> taskRow(long userId, String code, String rewardValue, int status) {
        Map<String, Object> row = new HashMap<>();
        row.put("user_id", userId);
        row.put("task_code", code);
        row.put("reward_value", rewardValue);
        row.put("status", status);
        return row;
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
