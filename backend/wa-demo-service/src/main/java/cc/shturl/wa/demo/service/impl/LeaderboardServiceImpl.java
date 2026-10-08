package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.LeaderboardService;
import cc.shturl.wa.demo.service.QuestPeriod;
import cc.shturl.wa.demo.service.WeeklyClaimLimit;
import cc.shturl.wa.demo.service.WorkDayQuota;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardServiceImpl.class);
    private static final ZoneId LEADERBOARD_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MIN_WINRATE_MATCHES = 20;
    private static final int HIGH_PRESSURE_CAP = 5;
    /** 一天三档对局加三档胜利：30+10+40+10+50+10。 */
    static final long FULL_CLEAR_DAY_GOLD = 150L;
    /** 周任务一次。本月最多 {@link WeeklyClaimLimit#PER_MONTH} 次。 */
    static final long WEEKLY_TASK_GOLD = 500L;
    private static final DateTimeFormatter WEEK_KEY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<LeaderboardResp> listLeaderboard(Long currentUserId, String type, int page, int size) {
        ensureCurrentMonth();
        boolean winRateBoard = isWinRateBoard(type);
        List<UserProfile> eligible = new ArrayList<>();
        for (UserProfile profile : userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery())) {
            if (profile.getUserId() == null) {
                continue;
            }
            if (winRateBoard) {
                if (totalMatches(profile) < MIN_WINRATE_MATCHES) {
                    continue;
                }
            } else if (!hasMonthlyStats(profile)) {
                continue;
            }
            eligible.add(profile);
        }
        sortEligible(eligible, winRateBoard);

        int fromIndex = 0;
        int toIndex = eligible.size();
        if (size > 0) {
            int safePage = Math.max(page, 1);
            fromIndex = (safePage - 1) * size;
            if (fromIndex >= eligible.size()) {
                return List.of();
            }
            toIndex = Math.min(fromIndex + size, eligible.size());
        }
        List<UserProfile> pageProfiles = eligible.subList(fromIndex, toIndex);
        Map<Long, User> users = loadUsers(pageProfiles);
        int displayRank = fromIndex + 1;
        List<LeaderboardResp> withRank = new ArrayList<>(pageProfiles.size());
        for (UserProfile profile : pageProfiles) {
            withRank.add(toResp(displayRank++, profile, users.get(profile.getUserId())));
        }
        return withRank;
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
                            0L, 0d, 0, 0);
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
                int reset = resetMonthStats();
                log.info("Leaderboard month rolled to {}, reset {} profiles.", monthStart, reset);
            }
        } catch (Exception e) {
            log.warn("Skip monthly leaderboard roll: {}", e.getMessage());
        }
    }

    public static LocalDate currentMonthStart() {
        return LocalDate.now(LEADERBOARD_ZONE).with(TemporalAdjusters.firstDayOfMonth());
    }

    public int resetMonthStats() {
        return jdbcTemplate.update("""
                UPDATE user_profiles
                SET money = 0,
                    weekly_money = 0,
                    win_count = 0,
                    lose_count = 0,
                    draw_count = 0,
                    money_reached_at = NULL
                """);
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Shanghai")
    public void ensureDailyTopSnapshot() {
        ensureCurrentMonth();
        if (!dailyTopTableExists()) {
            return;
        }
        Date day = Date.valueOf(LocalDate.now(LEADERBOARD_ZONE));
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaderboard_daily_top WHERE day_date = ?",
                Integer.class,
                day);
        if (existing != null && existing > 0) {
            return;
        }
        List<LeaderboardResp> top = listLeaderboard(null, "total", 1, 0).stream()
                .filter(item -> item.userId() != null && item.money() != null && item.money() > 0)
                .limit(HIGH_PRESSURE_CAP)
                .toList();
        int rank = 1;
        for (LeaderboardResp item : top) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO leaderboard_daily_top(day_date, rank_no, user_id, money) VALUES (?, ?, ?, ?)",
                        day, rank, item.userId(), item.money());
                rank++;
            } catch (Exception e) {
                log.warn("Skip daily top snapshot row rank={} user={}: {}", rank, item.userId(), e.getMessage());
                return;
            }
        }
        log.info("Snapshotted {} daily top leaderboard users for {}.", top.size(), day);
    }

    @Override
    public boolean teamTouchesDailyTop(Collection<Long> userIds) {
        return teamTouchesDailyTop(userIds, LocalDate.now(LEADERBOARD_ZONE));
    }

    boolean teamTouchesDailyTop(Collection<Long> userIds, LocalDate today) {
        if (!highPressureEnabledOn(today)) {
            return false;
        }
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        Set<Long> team = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (team.isEmpty()) {
            return false;
        }
        return highPressureRoster(today).stream().anyMatch(team::contains);
    }

    /**
     * 每月按日历周（周一开始，含 1 日的那一周为第一周）切换。
     * 第一周高压不生效，第二周起恢复。
     */
    static boolean highPressureEnabledOn(LocalDate day) {
        return day != null && calendarWeekOfMonth(day) >= 1;
    }

    /** 0 起算。多于五周时调用方自行封顶。 */
    static int calendarWeekOfMonth(LocalDate day) {
        LocalDate first = day.withDayOfMonth(1);
        int leading = first.getDayOfWeek().getValue() - 1;
        return (leading + day.getDayOfMonth() - 1) / 7;
    }

    Set<Long> highPressureRoster() {
        return highPressureRoster(LocalDate.now(LEADERBOARD_ZONE));
    }

    /**
     * 预测总金币 = 已到手金币 + 剩余工作日全胜 150 + 本月还来得及领的周任务。
     * 没开打的人不进名单。周次够领满剩余次数时，没做完的也计入；不够时按还能领到的最高次数。
     */
    Set<Long> highPressureRoster(LocalDate today) {
        LinkedHashSet<Long> roster = new LinkedHashSet<>();
        Projection projection = loadProjection(today);
        List<UserProfile> eligible = new ArrayList<>();
        for (UserProfile profile : userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery())) {
            if (profile.getUserId() == null || !projection.started(profile.getUserId())) {
                continue;
            }
            UserProfile row = copyRankProfile(profile);
            row.setMoney(projectedMoney(row, projection));
            eligible.add(row);
        }
        sortEligible(eligible, false);
        for (UserProfile profile : eligible) {
            if (safeMoney(profile) <= 0) {
                continue;
            }
            roster.add(profile.getUserId());
            if (roster.size() >= HIGH_PRESSURE_CAP) {
                return roster;
            }
        }
        ensureDailyTopSnapshot();
        for (Long userId : snapshotUserIdsInRankOrder()) {
            if (userId == null) {
                continue;
            }
            roster.add(userId);
            if (roster.size() >= HIGH_PRESSURE_CAP) {
                break;
            }
        }
        return roster;
    }

    private List<Long> snapshotUserIdsInRankOrder() {
        if (!dailyTopTableExists()) {
            return List.of();
        }
        try {
            return jdbcTemplate.query(
                    "SELECT user_id FROM leaderboard_daily_top WHERE day_date = ? ORDER BY rank_no ASC",
                    (rs, i) -> rs.getLong(1),
                    Date.valueOf(LocalDate.now(LEADERBOARD_ZONE)));
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean dailyTopTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                "leaderboard_daily_top");
        return count != null && count > 0;
    }

    boolean ranksAsTopFive(Long userId, long extra) {
        return ranksAsTopFive(userId, extra, LocalDate.now(LEADERBOARD_ZONE));
    }

    boolean ranksAsTopFive(Long userId, long extra, LocalDate today) {
        if (userId == null || today == null) {
            return false;
        }
        Projection projection = loadProjection(today);
        List<UserProfile> eligible = new ArrayList<>();
        UserProfile self = null;
        for (UserProfile profile : userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery())) {
            if (profile.getUserId() == null) {
                continue;
            }
            UserProfile row = copyRankProfile(profile);
            boolean selfRow = userId.equals(row.getUserId());
            if (!selfRow && !projection.started(row.getUserId())) {
                continue;
            }
            if (selfRow && !projection.started(row.getUserId()) && extra <= 0) {
                continue;
            }
            long projected = projection.started(row.getUserId())
                    ? projectedMoney(row, projection)
                    : safeMoney(row);
            if (selfRow) {
                projected += Math.max(extra, 0L);
                self = row;
            }
            row.setMoney(projected);
            eligible.add(row);
        }
        if (self == null) {
            return false;
        }
        if (safeMoney(self) <= 0) {
            return false;
        }
        sortEligible(eligible, false);
        int rank = 0;
        for (UserProfile profile : eligible) {
            if (safeMoney(profile) <= 0) {
                continue;
            }
            rank++;
            if (userId.equals(profile.getUserId())) {
                return rank <= HIGH_PRESSURE_CAP;
            }
            if (rank >= HIGH_PRESSURE_CAP) {
                return false;
            }
        }
        return false;
    }

    /** 从今天起、本月内还没结束、并且还能领到金币的周次数。 */
    static int openWeeklyWindows(LocalDate today) {
        if (today == null) {
            return 0;
        }
        YearMonth month = YearMonth.from(today);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int windows = 0;
        while (!monday.isAfter(monthEnd)) {
            LocalDate sunday = monday.plusDays(6);
            if (!sunday.isBefore(today)) {
                LocalDate claimStart = monday.isBefore(today) ? today : monday;
                LocalDate claimEnd = sunday.isAfter(monthEnd) ? monthEnd : sunday;
                if (!claimStart.isAfter(claimEnd)) {
                    windows++;
                }
            }
            monday = monday.plusWeeks(1);
        }
        return windows;
    }

    /** 剩余可领次数和还开着的周次，取较小的那个。本周已经领过就不再占一个窗口。 */
    static int claimableWeeklyCount(int openWeeks, int claimedThisMonth, boolean currentWeekClaimed) {
        int slotsLeft = Math.max(0, WeeklyClaimLimit.PER_MONTH - Math.max(claimedThisMonth, 0));
        int windows = Math.max(openWeeks, 0);
        if (currentWeekClaimed) {
            windows = Math.max(0, windows - 1);
        }
        return Math.min(slotsLeft, windows);
    }

    private long projectedMoney(UserProfile profile, Projection projection) {
        int used = projection.workDays(profile.getUserId());
        long daily = (long) Math.max(0, projection.quota - used) * FULL_CLEAR_DAY_GOLD;
        int weeks = claimableWeeklyCount(
                projection.openWeeks,
                projection.claims(profile.getUserId()),
                projection.currentWeekClaimed(profile.getUserId()));
        return safeMoney(profile) + daily + weeks * WEEKLY_TASK_GOLD;
    }

    private Projection loadProjection(LocalDate today) {
        YearMonth month = YearMonth.from(today);
        int quota = WorkDayQuota.days(month);
        int openWeeks = openWeeklyWindows(today);
        Map<Long, int[]> facts = new HashMap<>();
        try {
            LocalDate start = WorkDayQuota.countStart(month);
            LocalDate end = month.plusMonths(1).atDay(1);
            String[] bounds = WeeklyClaimLimit.claimedAtBounds(month);
            String weekKey = QuestPeriod.weeklyStart(today.atStartOfDay()).format(WEEK_KEY);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT p.user_id AS user_id,
                           (SELECT COUNT(*) FROM user_month_work_days w
                             WHERE w.user_id = p.user_id AND w.day_date >= ? AND w.day_date < ?) AS work_days,
                           (SELECT COUNT(*) FROM user_tasks ut
                             INNER JOIN tasks t ON t.id = ut.task_id
                             WHERE ut.user_id = p.user_id
                               AND LOWER(t.task_type) = 'weekly'
                               AND ut.status >= 3
                               AND ut.claimed_at >= ? AND ut.claimed_at < ?) AS weekly_claims,
                           (SELECT COUNT(*) FROM user_tasks ut
                             INNER JOIN tasks t ON t.id = ut.task_id
                             WHERE ut.user_id = p.user_id
                               AND t.task_code = 'T-WEEKLY-TEAM-10'
                               AND ut.period_key = ?
                               AND ut.status >= 3) AS current_week_claimed
                    FROM user_profiles p
                    """,
                    Date.valueOf(start), Date.valueOf(end), bounds[0], bounds[1], weekKey);
            for (Map<String, Object> row : rows) {
                Long userId = number(row.get("user_id"));
                if (userId == null) {
                    continue;
                }
                facts.put(userId, new int[] {
                        (int) numberOrZero(row.get("work_days")),
                        (int) numberOrZero(row.get("weekly_claims")),
                        numberOrZero(row.get("current_week_claimed")) > 0 ? 1 : 0
                });
            }
        } catch (Exception e) {
            log.warn("Skip projected high-pressure gold: {}", e.getMessage());
        }
        return new Projection(quota, openWeeks, facts);
    }

    private static Long number(Object raw) {
        return raw instanceof Number number ? number.longValue() : null;
    }

    private static long numberOrZero(Object raw) {
        return raw instanceof Number number ? number.longValue() : 0L;
    }

    private record Projection(int quota, int openWeeks, Map<Long, int[]> facts) {
        boolean started(Long userId) {
            return workDays(userId) > 0;
        }

        int workDays(Long userId) {
            return fact(userId, 0);
        }

        int claims(Long userId) {
            return fact(userId, 1);
        }

        boolean currentWeekClaimed(Long userId) {
            return fact(userId, 2) > 0;
        }

        private int fact(Long userId, int index) {
            int[] row = facts.get(userId);
            if (row == null || index >= row.length) {
                return 0;
            }
            return row[index];
        }
    }

    private UserProfile copyRankProfile(UserProfile source) {
        UserProfile copy = new UserProfile();
        copy.setUserId(source.getUserId());
        copy.setMoney(source.getMoney());
        copy.setWinCount(source.getWinCount());
        copy.setLoseCount(source.getLoseCount());
        copy.setDrawCount(source.getDrawCount());
        copy.setMoneyReachedAt(source.getMoneyReachedAt());
        return copy;
    }

    private boolean isWinRateBoard(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.trim();
        return "weekly".equalsIgnoreCase(normalized) || "winrate".equalsIgnoreCase(normalized);
    }

    private void sortEligible(List<UserProfile> eligible, boolean winRateBoard) {
        Comparator<UserProfile> byUserId = Comparator.comparing(
                UserProfile::getUserId, Comparator.nullsLast(Comparator.naturalOrder()));
        if (winRateBoard) {
            eligible.sort(Comparator
                    .comparingDouble(this::exactWinRate).reversed()
                    .thenComparing(this::safeWinCount, Comparator.reverseOrder())
                    .thenComparing(byUserId));
        } else {
            eligible.sort(Comparator
                    .comparingLong(this::safeMoney).reversed()
                    .thenComparing(Comparator.comparingDouble(this::exactWinRate).reversed())
                    .thenComparing(this::safeWinCount, Comparator.reverseOrder())
                    .thenComparing(this::totalMatches, Comparator.reverseOrder())
                    .thenComparing(UserProfile::getMoneyReachedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(byUserId));
        }
    }

    private Map<Long, User> loadUsers(List<UserProfile> profiles) {
        Set<Long> ids = profiles.stream()
                .map(UserProfile::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> users = new HashMap<>();
        for (User user : userMapper.selectBatchIds(ids)) {
            if (user != null && user.getId() != null) {
                users.put(user.getId(), user);
            }
        }
        return users;
    }

    private LeaderboardResp toResp(int rank, UserProfile profile) {
        return toResp(rank, profile, userMapper.selectById(profile.getUserId()));
    }

    private LeaderboardResp toResp(int rank, UserProfile profile, User user) {
        long money = safeMoney(profile);
        int wins = safeWinCount(profile);
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

    private boolean hasMonthlyStats(UserProfile profile) {
        return safeMoney(profile) > 0 || totalMatches(profile) > 0;
    }

    private long safeMoney(UserProfile profile) {
        return profile.getMoney() == null ? 0L : profile.getMoney();
    }

    private int safeWinCount(UserProfile profile) {
        return profile.getWinCount() == null ? 0 : profile.getWinCount();
    }

    private int totalMatches(UserProfile profile) {
        int wins = safeWinCount(profile);
        int losses = profile.getLoseCount() == null ? 0 : profile.getLoseCount();
        int draws = profile.getDrawCount() == null ? 0 : profile.getDrawCount();
        return wins + losses + draws;
    }

    private double exactWinRate(UserProfile profile) {
        int wins = profile.getWinCount() == null ? 0 : profile.getWinCount();
        int losses = profile.getLoseCount() == null ? 0 : profile.getLoseCount();
        int draws = profile.getDrawCount() == null ? 0 : profile.getDrawCount();
        int total = wins + losses + draws;
        if (total <= 0) {
            return 0d;
        }
        return wins / (double) total;
    }

    private double winRatePercent(UserProfile profile) {
        int wins = profile.getWinCount() == null ? 0 : profile.getWinCount();
        int total = totalMatches(profile);
        if (total <= 0) {
            return 0d;
        }
        return BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
