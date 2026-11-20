package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.LeaderboardService;
import cc.shturl.wa.demo.service.MakeupQueue;
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
import java.util.HashSet;
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
    private static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        LocalDate today = LocalDate.now(LEADERBOARD_ZONE);
        Date day = Date.valueOf(today);
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaderboard_daily_top WHERE day_date = ?",
                Integer.class,
                day);
        if (existing != null && existing > 0) {
            return;
        }
        int locked = writeLockedRoster(today, projectedRanking(today));
        log.info("Locked {} projected top players for {}.", locked, today);
    }

    @Override
    public int relockOpeningProjectedRoster(LocalDate day) {
        if (day == null || !dailyTopTableExists()) {
            return 0;
        }
        List<UserProfile> ranked = rankWith(loadOpeningProjection(day), goldClaimedOn(day));
        jdbcTemplate.update("DELETE FROM leaderboard_daily_top WHERE day_date = ?", Date.valueOf(day));
        int locked = writeLockedRoster(day, ranked);
        log.info("Relocked opening projected roster for {} with {} players.", day, locked);
        return locked;
    }

    private int writeLockedRoster(LocalDate day, List<UserProfile> ranked) {
        int rank = 1;
        for (UserProfile profile : ranked) {
            if (profile.getUserId() == null || safeMoney(profile) <= 0) {
                continue;
            }
            try {
                jdbcTemplate.update(
                        "INSERT INTO leaderboard_daily_top(day_date, rank_no, user_id, money) VALUES (?, ?, ?, ?)",
                        Date.valueOf(day), rank, profile.getUserId(), safeMoney(profile));
            } catch (Exception e) {
                log.warn("Skip locked roster row rank={} user={}: {}", rank, profile.getUserId(), e.getMessage());
                return rank - 1;
            }
            rank++;
            if (rank > HIGH_PRESSURE_CAP) {
                break;
            }
        }
        return rank - 1;
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
        return lockedPressureRoster(today).stream().anyMatch(team::contains);
    }

    /** 当天 0 点锁定的潜在前五。白天不再按实时金币重算。 */
    private Set<Long> lockedPressureRoster(LocalDate today) {
        if (today == null) {
            return Set.of();
        }
        try {
            List<Long> ids = jdbcTemplate.query(
                    "SELECT user_id FROM leaderboard_daily_top WHERE day_date = ? ORDER BY rank_no ASC",
                    (rs, i) -> rs.getLong(1),
                    Date.valueOf(today));
            if (ids == null || ids.isEmpty()) {
                return Set.of();
            }
            return new LinkedHashSet<>(ids);
        } catch (Exception e) {
            log.warn("Skip locked pressure roster for {}: {}", today, e.getMessage());
            return Set.of();
        }
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
     * 预测总金币 = 已到手金币 + 还来得及打满的整天 × 150 + 今天没打完和过去没打满的档按全胜补上 + 本月还来得及领的周任务。
     * 这些漏档不另占工作日。一把没打的天数补上要扣掉后面一天，不另加金币。没开打的人不进名单。已经结束又没领的周次不能补，不计入。
     * 只算还没结束的周：本周没领也算一次。周次够领满剩余次数时计入；不够时按还能领到的最高次数。
     * 工作日同样只算从今天到月底还来得及打的天数。定额还剩得更多、日历不够时，多出来的天数不计入。
     * 这个结果只在每天 0 点写入锁定名单。对局高压读锁定名单，不读这里的实时排名。
     */
    Set<Long> highPressureRoster(LocalDate today) {
        LinkedHashSet<Long> roster = new LinkedHashSet<>();
        for (UserProfile profile : projectedRanking(today)) {
            roster.add(profile.getUserId());
            if (roster.size() >= HIGH_PRESSURE_CAP) {
                return roster;
            }
        }
        return roster;
    }

    private List<UserProfile> projectedRanking(LocalDate today) {
        return rankWith(loadProjection(today), Map.of());
    }

    private List<UserProfile> rankWith(Projection projection, Map<Long, Long> spentSinceOpen) {
        List<UserProfile> eligible = new ArrayList<>();
        for (UserProfile profile : userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery())) {
            if (profile.getUserId() == null || !projection.started(profile.getUserId())) {
                continue;
            }
            UserProfile row = copyRankProfile(profile);
            long spent = spentSinceOpen.getOrDefault(profile.getUserId(), 0L);
            row.setMoney(Math.max(0L, safeMoney(row) - spent));
            row.setMoney(projectedMoney(row, projection));
            eligible.add(row);
        }
        sortEligible(eligible, false);
        List<UserProfile> ranked = new ArrayList<>();
        for (UserProfile profile : eligible) {
            if (safeMoney(profile) <= 0) {
                continue;
            }
            ranked.add(profile);
        }
        return ranked;
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

    /** 从今天到统计窗口结束，含今天，还能新开工作日的日历天数。 */
    static int remainingCalendarDays(LocalDate today) {
        if (today == null) {
            return 0;
        }
        YearMonth month = YearMonth.from(today);
        LocalDate start = WorkDayQuota.countStart(month);
        LocalDate end = month.atEndOfMonth();
        LocalDate from = today.isBefore(start) ? start : today;
        if (from.isAfter(end)) {
            return 0;
        }
        return (int) (end.toEpochDay() - from.toEpochDay() + 1);
    }

    /** 定额里还没打的天数，和日历上还来得及打的天数，取较小的那个。 */
    static int achievableWorkDays(int quota, int used, int daysLeft) {
        int remainingQuota = Math.max(0, quota - Math.max(used, 0));
        return Math.min(remainingQuota, Math.max(daysLeft, 0));
    }

    /** 还没结束的周次数。周日已经过去的周不能再领。本周还没结束就算一次。 */
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

    /** 本月剩余名额和还开着的周次，取较小的那个。已经错过的周不补。本周已经领过就不再占一个窗口。 */
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
        int daysLeft = Math.max(projection.daysLeft, 0);
        if (projection.playedToday(profile.getUserId())) {
            daysLeft = Math.max(0, daysLeft - 1);
        }
        long daily = (long) achievableWorkDays(projection.quota, used, daysLeft) * FULL_CLEAR_DAY_GOLD;
        int weeks = claimableWeeklyCount(
                projection.openWeeks,
                projection.claims(profile.getUserId()),
                projection.currentWeekClaimed(profile.getUserId()));
        return safeMoney(profile) + daily + weeks * WEEKLY_TASK_GOLD + projection.leaked(profile.getUserId());
    }

    /**
     * 已经记过工作日、但没打满的档，按全胜补上。不另占工作日。
     * 一把没打的日子不在这里加。月底可以把同一天剩下的档补完，所以有两档可补时多算 1 次。
     */
    static long leakedMakeupGold(LocalDate today, Map<LocalDate, Integer> occupiedByDay,
                                 Set<MakeupQueue.DaySlot> claimed, boolean usedMakeupToday) {
        if (today == null || occupiedByDay == null || occupiedByDay.isEmpty()) {
            return 0L;
        }
        List<Integer> golds = new ArrayList<>();
        boolean pair = false;
        for (Map.Entry<LocalDate, Integer> entry : occupiedByDay.entrySet()) {
            LocalDate day = entry.getKey();
            if (day == null || !day.isBefore(today)) {
                continue;
            }
            int occupied = Math.min(3, Math.max(entry.getValue() == null ? 0 : entry.getValue(), 0));
            if (occupied <= 0 || occupied >= 3) {
                continue;
            }
            int missing = 0;
            for (int slot = occupied + 1; slot <= 3; slot++) {
                if (claimed != null && claimed.contains(new MakeupQueue.DaySlot(day, slot))) {
                    continue;
                }
                golds.add(MakeupQueue.finishGold(slot) + MakeupQueue.WIN_GOLD);
                missing++;
            }
            if (missing >= 2) {
                pair = true;
            }
        }
        int budget = remainingCalendarDays(today);
        if (usedMakeupToday) {
            budget = Math.max(0, budget - 1);
        }
        if (budget > 0 && pair) {
            budget++;
        }
        if (budget <= 0 || golds.isEmpty()) {
            return 0L;
        }
        golds.sort(Comparator.reverseOrder());
        long sum = 0L;
        for (int i = 0; i < Math.min(budget, golds.size()); i++) {
            sum += golds.get(i);
        }
        return sum;
    }

    /** 今天已经占了工作日、但没打满的档。这些局今天还能正常打完，不占补打次数。 */
    static long todayRemainderGold(int occupied, LocalDate today, Set<MakeupQueue.DaySlot> claimed) {
        if (today == null || occupied <= 0 || occupied >= 3) {
            return 0L;
        }
        long sum = 0L;
        for (int slot = occupied + 1; slot <= 3; slot++) {
            if (claimed != null && claimed.contains(new MakeupQueue.DaySlot(today, slot))) {
                continue;
            }
            sum += MakeupQueue.finishGold(slot) + MakeupQueue.WIN_GOLD;
        }
        return sum;
    }

    /** 当天 0 点之后领走的金币。实领时间按库存的墙钟日期切，不把前一晚 20:00 的补记算进第二天。 */
    private Map<Long, Long> goldClaimedOn(LocalDate day) {
        String start = day.atStartOfDay().format(SQL_TIME);
        String end = day.plusDays(1).atStartOfDay().format(SQL_TIME);
        Map<Long, Long> spent = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT ut.user_id AS user_id,
                       SUM(CAST(JSON_UNQUOTE(JSON_EXTRACT(t.reward_value, '$.amount')) AS SIGNED)) AS claimed
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.status >= 3
                  AND LOWER(IFNULL(t.reward_type, '')) = 'money'
                  AND ut.claimed_at >= ?
                  AND ut.claimed_at < ?
                GROUP BY ut.user_id
                """, start, end);
        for (Map<String, Object> row : rows) {
            Long userId = number(row.get("user_id"));
            if (userId != null) {
                spent.put(userId, numberOrZero(row.get("claimed")));
            }
        }
        return spent;
    }

    /** 当天 0 点之前的工作日和周任务，用来还原开盘时的预测排名。 */
    private Projection loadOpeningProjection(LocalDate day) {
        YearMonth month = YearMonth.from(day);
        int quota = WorkDayQuota.days(month);
        int openWeeks = openWeeklyWindows(day);
        Map<Long, int[]> facts = new HashMap<>();
        try {
            LocalDate start = WorkDayQuota.countStart(month);
            String[] bounds = WeeklyClaimLimit.claimedAtBounds(month);
            String opening = day.atStartOfDay().format(SQL_TIME);
            String claimEnd = opening.compareTo(bounds[1]) < 0 ? opening : bounds[1];
            String weekKey = QuestPeriod.weeklyStart(day.atStartOfDay()).format(WEEK_KEY);
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
                               AND ut.status >= 3
                               AND ut.claimed_at < ?) AS current_week_claimed
                    FROM user_profiles p
                    """,
                    Date.valueOf(start), Date.valueOf(day), bounds[0], claimEnd, weekKey, opening);
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
            log.warn("Skip opening projected roster for {}: {}", day, e.getMessage());
        }
        return new Projection(quota, openWeeks, remainingCalendarDays(day), facts, loadLeakedMakeupGold(day));
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
            String todayKey = today.format(WEEK_KEY);
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
                               AND ut.status >= 3) AS current_week_claimed,
                           (SELECT COUNT(*) FROM user_month_work_days w
                             WHERE w.user_id = p.user_id AND w.day_date = '%s') AS played_today
                    FROM user_profiles p
                    """.formatted(todayKey),
                    Date.valueOf(start), Date.valueOf(end), bounds[0], bounds[1], weekKey);
            for (Map<String, Object> row : rows) {
                Long userId = number(row.get("user_id"));
                if (userId == null) {
                    continue;
                }
                facts.put(userId, new int[] {
                        (int) numberOrZero(row.get("work_days")),
                        (int) numberOrZero(row.get("weekly_claims")),
                        numberOrZero(row.get("current_week_claimed")) > 0 ? 1 : 0,
                        numberOrZero(row.get("played_today")) > 0 ? 1 : 0
                });
            }
        } catch (Exception e) {
            log.warn("Skip projected high-pressure gold: {}", e.getMessage());
        }
        return new Projection(quota, openWeeks, remainingCalendarDays(today), facts, loadLeakedMakeupGold(today));
    }

    /** 已记工作日上还没补的漏档。查询失败时不加，预测退回原来的工作日和周任务。 */
    private Map<Long, Long> loadLeakedMakeupGold(LocalDate today) {
        Map<Long, Long> gold = new HashMap<>();
        if (today == null) {
            return gold;
        }
        try {
            YearMonth month = YearMonth.from(today);
            LocalDate start = WorkDayQuota.countStart(month);
            Map<Long, Map<LocalDate, Integer>> occupied = new HashMap<>();
            Map<Long, Integer> todayOccupied = new HashMap<>();
            List<Map<String, Object>> slotRows = jdbcTemplate.queryForList("""
                    SELECT w.user_id AS user_id, w.day_date AS day_date, ut.progress_value AS occupied
                    FROM user_month_work_days w
                    LEFT JOIN tasks t ON t.task_code = 'T-DAILY-SLOT'
                    LEFT JOIN user_tasks ut ON ut.user_id = w.user_id AND ut.task_id = t.id
                      AND ut.period_key = DATE_FORMAT(w.day_date, '%Y-%m-%d')
                    WHERE w.day_date >= ? AND w.day_date < ?
                    """, Date.valueOf(start), Date.valueOf(today.plusDays(1)));
            if (slotRows != null) {
                for (Map<String, Object> row : slotRows) {
                    Long userId = number(row.get("user_id"));
                    LocalDate day = asDate(row.get("day_date"));
                    if (userId == null || day == null) {
                        continue;
                    }
                    int slots = (int) numberOrZero(row.get("occupied"));
                    if (day.equals(today)) {
                        todayOccupied.merge(userId, slots, Math::max);
                    } else {
                        occupied.computeIfAbsent(userId, ignored -> new HashMap<>()).merge(day, slots, Math::max);
                    }
                }
            }
            Map<Long, Set<MakeupQueue.DaySlot>> claimed = new HashMap<>();
            List<Map<String, Object>> claimRows = jdbcTemplate.queryForList("""
                    SELECT user_id, owed_day, slot_no
                    FROM user_makeup_claims
                    WHERE owed_day >= ? AND owed_day < ?
                    """, Date.valueOf(start), Date.valueOf(today));
            if (claimRows != null) {
                for (Map<String, Object> row : claimRows) {
                    Long userId = number(row.get("user_id"));
                    LocalDate day = asDate(row.get("owed_day"));
                    int slot = (int) numberOrZero(row.get("slot_no"));
                    if (userId == null || day == null || slot < 1) {
                        continue;
                    }
                    claimed.computeIfAbsent(userId, ignored -> new HashSet<>()).add(new MakeupQueue.DaySlot(day, slot));
                }
            }
            Set<Long> usedToday = new HashSet<>();
            List<Map<String, Object>> usedRows = jdbcTemplate.queryForList("""
                    SELECT DISTINCT user_id
                    FROM user_makeup_claims
                    WHERE created_at >= ? AND created_at < ?
                    """, today.atStartOfDay().format(SQL_TIME), today.plusDays(1).atStartOfDay().format(SQL_TIME));
            if (usedRows != null) {
                for (Map<String, Object> row : usedRows) {
                    Long userId = number(row.get("user_id"));
                    if (userId != null) {
                        usedToday.add(userId);
                    }
                }
            }
            Set<Long> players = new HashSet<>(occupied.keySet());
            players.addAll(todayOccupied.keySet());
            for (Long userId : players) {
                Set<MakeupQueue.DaySlot> claimedSlots = claimed.get(userId);
                long amount = leakedMakeupGold(today, occupied.get(userId), claimedSlots, usedToday.contains(userId));
                amount += todayRemainderGold(todayOccupied.getOrDefault(userId, 0), today, claimedSlots);
                if (amount > 0) {
                    gold.put(userId, amount);
                }
            }
        } catch (Exception e) {
            log.warn("Skip leaked makeup gold: {}", e.getMessage());
        }
        return gold;
    }

    private static LocalDate asDate(Object raw) {
        if (raw instanceof Date date) {
            return date.toLocalDate();
        }
        if (raw instanceof LocalDate localDate) {
            return localDate;
        }
        if (raw == null) {
            return null;
        }
        String text = raw.toString();
        if (text.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(text.substring(0, 10));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long number(Object raw) {
        return raw instanceof Number number ? number.longValue() : null;
    }

    private static long numberOrZero(Object raw) {
        return raw instanceof Number number ? number.longValue() : 0L;
    }

    private record Projection(int quota, int openWeeks, int daysLeft, Map<Long, int[]> facts, Map<Long, Long> leakedGold) {
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

        boolean playedToday(Long userId) {
            return fact(userId, 3) > 0;
        }

        long leaked(Long userId) {
            if (leakedGold == null || userId == null) {
                return 0L;
            }
            Long amount = leakedGold.get(userId);
            return amount == null ? 0L : amount;
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
