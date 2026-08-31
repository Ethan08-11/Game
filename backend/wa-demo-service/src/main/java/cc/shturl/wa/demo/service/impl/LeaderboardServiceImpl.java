package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.LeaderboardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardServiceImpl.class);
    private static final ZoneId LEADERBOARD_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MIN_WINRATE_MATCHES = 20;

    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<LeaderboardResp> listLeaderboard(Long currentUserId, String type, int page, int size) {
        ensureCurrentMonth();
        boolean winRateBoard = isWinRateBoard(type);
        List<UserProfile> profiles = userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery());
        List<LeaderboardResp> ranked = new ArrayList<>();
        for (UserProfile profile : profiles) {
            if (profile.getUserId() == null) {
                continue;
            }
            if (winRateBoard && totalMatches(profile) < MIN_WINRATE_MATCHES) {
                continue;
            }
            ranked.add(toResp(0, profile));
        }
        if (winRateBoard) {
            ranked.sort(Comparator
                    .comparing(LeaderboardResp::winRate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(LeaderboardResp::winCount, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(LeaderboardResp::userId, Comparator.nullsLast(Comparator.naturalOrder())));
        } else {
            ranked.sort(Comparator
                    .comparing(LeaderboardResp::money, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(LeaderboardResp::winRate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(LeaderboardResp::userId, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        int displayRank = 1;
        List<LeaderboardResp> withRank = new ArrayList<>();
        for (LeaderboardResp item : ranked) {
            withRank.add(new LeaderboardResp(displayRank++, item.userId(), item.username(),
                    item.displayName(), item.avatarUrl(), item.money(), item.winRate(),
                    item.winCount(), item.loseCount()));
        }

        if (size <= 0) {
            return withRank;
        }
        int safePage = Math.max(page, 1);
        int fromIndex = (safePage - 1) * size;
        if (fromIndex >= withRank.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, withRank.size());
        return withRank.subList(fromIndex, toIndex);
    }

    @Override
    public LeaderboardResp getMyRank(Long currentUserId, String type) {
        List<LeaderboardResp> ranked = listLeaderboard(currentUserId, type, 1, 0);
        return ranked.stream()
                .filter(item -> currentUserId.equals(item.userId()))
                .findFirst()
                .orElseGet(() -> {
                    UserProfile profile = userProfileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery()
                            .eq(UserProfile::getUserId, currentUserId)
                            .last("LIMIT 1"));
                    if (profile != null) {
                        return toResp(ranked.size() + 1, profile);
                    }
                    User user = userMapper.selectById(currentUserId);
                    return new LeaderboardResp(ranked.size() + 1, currentUserId,
                            user == null ? null : user.getUsername(),
                            user == null ? null : user.getUsername(),
                            user == null ? null : user.getAvatarUrl(),
                            0L, 0, 0, 0);
                });
    }

    @Override
    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Shanghai")
    public void ensureCurrentMonth() {
        LocalDate monthStart = currentMonthStart();
        try {
            int claimed = jdbcTemplate.update(
                    "UPDATE leaderboard_week SET week_start = ? WHERE id = 1 AND week_start < ?",
                    Date.valueOf(monthStart), Date.valueOf(monthStart));
            if (claimed > 0) {
                log.info("Leaderboard month rolled to {}.", monthStart);
            }
        } catch (Exception e) {
            log.warn("Skip monthly leaderboard roll: {}", e.getMessage());
        }
    }

    public static LocalDate currentMonthStart() {
        return LocalDate.now(LEADERBOARD_ZONE).with(TemporalAdjusters.firstDayOfMonth());
    }

    private boolean isWinRateBoard(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.trim();
        return "weekly".equalsIgnoreCase(normalized) || "winrate".equalsIgnoreCase(normalized);
    }

    private LeaderboardResp toResp(int rank, UserProfile profile) {
        User user = userMapper.selectById(profile.getUserId());
        long money = profile.getMoney() == null ? 0L : profile.getMoney();
        int wins = profile.getWinCount() == null ? 0 : profile.getWinCount();
        int losses = profile.getLoseCount() == null ? 0 : profile.getLoseCount();
        return new LeaderboardResp(
                rank,
                profile.getUserId(),
                user == null ? null : user.getUsername(),
                profile.getDisplayName() == null ? (user == null ? null : user.getUsername()) : profile.getDisplayName(),
                user == null ? null : user.getAvatarUrl(),
                money,
                winRatePercent(profile),
                wins,
                losses);
    }

    private int totalMatches(UserProfile profile) {
        int wins = profile.getWinCount() == null ? 0 : profile.getWinCount();
        int losses = profile.getLoseCount() == null ? 0 : profile.getLoseCount();
        int draws = profile.getDrawCount() == null ? 0 : profile.getDrawCount();
        return wins + losses + draws;
    }

    private int winRatePercent(UserProfile profile) {
        int wins = profile.getWinCount() == null ? 0 : profile.getWinCount();
        int losses = profile.getLoseCount() == null ? 0 : profile.getLoseCount();
        int draws = profile.getDrawCount() == null ? 0 : profile.getDrawCount();
        int total = wins + losses + draws;
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(wins * 100.0 / total);
    }
}
