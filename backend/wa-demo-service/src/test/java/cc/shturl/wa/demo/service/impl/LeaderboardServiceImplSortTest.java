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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
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
    @DisplayName("高压：开打天数和周任务相同时，按已到手金币取前五")
    void liveTotalTopFiveIsHighPressureEvenWithoutSnapshot() {
        stubSixPlayerBoard();
        stubProjection(8, 0, false);

        LocalDate today = LocalDate.of(2026, 10, 8);
        assertThat(service.teamTouchesDailyTop(List.of(5L, 99L), today)).isTrue();
        assertThat(service.teamTouchesDailyTop(List.of(6L, 99L), today)).isFalse();
        assertThat(service.highPressureRoster(today)).hasSize(5).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("高压：预测总分多 11 就能挤进前五")
    void projectedGoldThatWouldEnterTopFiveIsHighPressure() {
        stubSixPlayerBoard();
        stubProjection(8, 0, false);

        assertThat(service.ranksAsTopFive(6L, 0L, LocalDate.of(2026, 10, 8))).isFalse();
        assertThat(service.ranksAsTopFive(6L, 11L, LocalDate.of(2026, 10, 8))).isTrue();
        assertThat(service.ranksAsTopFive(6L, 50L, LocalDate.of(2026, 10, 8))).isTrue();
    }

    @Test
    @DisplayName("高压：少打的工作日按全胜 150 计入后能挤掉已到手金币更高的人")
    void fewerWorkDaysCanEnterProjectedTopFive() {
        stubSixPlayerBoard();
        stubProjectionExcept(6L, 1, 0, false, 8, 0, false);

        LocalDate today = LocalDate.of(2026, 10, 8);
        assertThat(service.teamTouchesDailyTop(List.of(6L), today)).isTrue();
        assertThat(service.highPressureRoster(today)).hasSize(5).contains(6L).doesNotContain(5L);
    }

    @Test
    @DisplayName("高压：没开打的人不进潜在前五")
    void playerWhoHasNotStartedIsExcluded() {
        stubSixPlayerBoard();
        stubProjectionExcept(6L, 0, 0, false, 8, 0, false);

        assertThat(service.teamTouchesDailyTop(List.of(6L), LocalDate.of(2026, 10, 8))).isFalse();
        assertThat(service.highPressureRoster(LocalDate.of(2026, 10, 8))).doesNotContain(6L);
    }

    @Test
    @DisplayName("高压：周次不够领满时，没做完的只按还能领到的次数算")
    void unfinishedWeeklyCountsOnlyWhileWindowsRemain() {
        assertThat(LeaderboardServiceImpl.openWeeklyWindows(LocalDate.of(2026, 10, 8))).isEqualTo(4);
        assertThat(LeaderboardServiceImpl.openWeeklyWindows(LocalDate.of(2026, 10, 27))).isEqualTo(1);
        assertThat(LeaderboardServiceImpl.claimableWeeklyCount(4, 0, false)).isEqualTo(4);
        assertThat(LeaderboardServiceImpl.claimableWeeklyCount(4, 2, true)).isEqualTo(2);
        assertThat(LeaderboardServiceImpl.claimableWeeklyCount(1, 0, false)).isEqualTo(1);

        stubSixPlayerBoard();
        stubProjectionExcept(6L, 8, 0, false, 8, 2, true);
        assertThat(service.highPressureRoster(LocalDate.of(2026, 10, 8))).contains(6L);

        stubProjectionExcept(6L, 8, 0, false, 8, 2, false);
        assertThat(service.highPressureRoster(LocalDate.of(2026, 10, 27))).doesNotContain(6L);
    }

    @Test
    @DisplayName("高压：每月日历第一周不针对潜在前五")
    void firstWeekOfMonthDisablesHighPressure() {
        stubSixPlayerBoard();

        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 10, 1))).isZero();
        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 10, 4))).isZero();
        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 10, 5))).isEqualTo(1);
        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 10, 31))).isEqualTo(4);
        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 6, 1))).isZero();
        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 6, 7))).isZero();
        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 6, 8))).isEqualTo(1);
        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 3, 1))).isZero();
        assertThat(LeaderboardServiceImpl.calendarWeekOfMonth(LocalDate.of(2026, 3, 31))).isEqualTo(5);

        assertThat(LeaderboardServiceImpl.highPressureEnabledOn(LocalDate.of(2026, 10, 4))).isFalse();
        assertThat(LeaderboardServiceImpl.highPressureEnabledOn(LocalDate.of(2026, 10, 5))).isTrue();
        stubProjection(8, 0, false);
        assertThat(service.teamTouchesDailyTop(List.of(1L), LocalDate.of(2026, 10, 2))).isFalse();
        assertThat(service.teamTouchesDailyTop(List.of(1L), LocalDate.of(2026, 10, 5))).isTrue();
    }

    private void stubProjection(int workDays, int claims, boolean currentWeekClaimed) {
        doReturn(List.of(
                projectionRow(1L, workDays, claims, currentWeekClaimed),
                projectionRow(2L, workDays, claims, currentWeekClaimed),
                projectionRow(3L, workDays, claims, currentWeekClaimed),
                projectionRow(4L, workDays, claims, currentWeekClaimed),
                projectionRow(5L, workDays, claims, currentWeekClaimed),
                projectionRow(6L, workDays, claims, currentWeekClaimed)))
                .when(jdbcTemplate).queryForList(anyString(), any(Object.class), any(Object.class), any(Object.class), any(Object.class), any(Object.class));
    }

    private void stubProjectionExcept(long userId, int workDays, int claims, boolean currentWeekClaimed,
                                      int othersWorkDays, int othersClaims, boolean othersCurrentWeekClaimed) {
        doReturn(List.of(
                projectionRow(1L, userId == 1L ? workDays : othersWorkDays, userId == 1L ? claims : othersClaims, userId == 1L ? currentWeekClaimed : othersCurrentWeekClaimed),
                projectionRow(2L, userId == 2L ? workDays : othersWorkDays, userId == 2L ? claims : othersClaims, userId == 2L ? currentWeekClaimed : othersCurrentWeekClaimed),
                projectionRow(3L, userId == 3L ? workDays : othersWorkDays, userId == 3L ? claims : othersClaims, userId == 3L ? currentWeekClaimed : othersCurrentWeekClaimed),
                projectionRow(4L, userId == 4L ? workDays : othersWorkDays, userId == 4L ? claims : othersClaims, userId == 4L ? currentWeekClaimed : othersCurrentWeekClaimed),
                projectionRow(5L, userId == 5L ? workDays : othersWorkDays, userId == 5L ? claims : othersClaims, userId == 5L ? currentWeekClaimed : othersCurrentWeekClaimed),
                projectionRow(6L, userId == 6L ? workDays : othersWorkDays, userId == 6L ? claims : othersClaims, userId == 6L ? currentWeekClaimed : othersCurrentWeekClaimed)))
                .when(jdbcTemplate).queryForList(anyString(), any(Object.class), any(Object.class), any(Object.class), any(Object.class), any(Object.class));
    }

    private static Map<String, Object> projectionRow(long userId, int workDays, int claims, boolean currentWeekClaimed) {
        Map<String, Object> row = new HashMap<>();
        row.put("user_id", userId);
        row.put("work_days", workDays);
        row.put("weekly_claims", claims);
        row.put("current_week_claimed", currentWeekClaimed ? 1 : 0);
        return row;
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
