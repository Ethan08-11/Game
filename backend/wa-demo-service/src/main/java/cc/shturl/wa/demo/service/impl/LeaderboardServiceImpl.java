package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.LeaderboardService;
import cc.shturl.wa.demo.service.QuestPeriod;
import cc.shturl.wa.demo.service.WorkDayService;
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
import java.time.LocalDate;
import java.time.ZoneId;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardServiceImpl.class);
    private static final ZoneId LEADERBOARD_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MIN_WINRATE_MATCHES = 20;
    private static final int HIGH_PRESSURE_CAP = 5;
    private static final Pattern REWARD_AMOUNT = Pattern.compile("\"amount\"\\s*:\\s*(-?\\d+)");

    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final WorkDayService workDayService;

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
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        Set<Long> team = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (team.isEmpty()) {
            return false;
        }
        return highPressureRoster().stream().anyMatch(team::contains);
    }

    Set<Long> highPressureRoster() {
        LinkedHashSet<Long> roster = new LinkedHashSet<>();
        Map<Long, Long> extras = pendingDailyGoldByUser();
        List<UserProfile> eligible = new ArrayList<>();
        for (UserProfile profile : userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery())) {
            if (profile.getUserId() == null) {
                continue;
            }
            UserProfile row = copyRankProfile(profile);
            row.setMoney(safeMoney(row) + extras.getOrDefault(row.getUserId(), 0L));
            if (hasMonthlyStats(row)) {
                eligible.add(row);
            }
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
        if (roster.size() >= HIGH_PRESSURE_CAP) {
            return roster;
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
        if (userId == null) {
            return false;
        }
        List<UserProfile> eligible = new ArrayList<>();
        UserProfile self = null;
        for (UserProfile profile : userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery())) {
            if (profile.getUserId() == null) {
                continue;
            }
            UserProfile row = copyRankProfile(profile);
            if (userId.equals(row.getUserId())) {
                row.setMoney(safeMoney(row) + Math.max(extra, 0L));
                self = row;
            }
            if (userId.equals(row.getUserId()) || hasMonthlyStats(profile)) {
                eligible.add(row);
            }
        }
        if (self == null) {
            if (extra <= 0) {
                return false;
            }
            self = new UserProfile();
            self.setUserId(userId);
            self.setMoney(extra);
            eligible.add(self);
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

    private Map<Long, Long> pendingDailyGoldByUser() {
        Map<Long, Long> extras = new HashMap<>();
        String period = QuestPeriod.currentDailyDate().toString();
        try {
            Map<Integer, Long> matchGold = matchSlotGoldBySlot();
            Map<Long, List<Map<String, Object>>> rowsByUser = new HashMap<>();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT ut.user_id AS user_id, t.task_code AS task_code,
                           t.reward_value AS reward_value, ut.status AS status,
                           ut.progress_value AS progress_value
                    FROM user_tasks ut
                    INNER JOIN tasks t ON t.id = ut.task_id
                    WHERE ut.period_key = ?
                      AND t.task_code IN (
                        'T-DAILY-SLOT',
                        'T-DAILY-MATCH-1','T-DAILY-MATCH-2','T-DAILY-MATCH-3',
                        'T-DAILY-WIN-1','T-DAILY-WIN-2','T-DAILY-WIN-3')
                    """, period);
            for (Map<String, Object> row : rows) {
                Object userRaw = row.get("user_id");
                if (!(userRaw instanceof Number userNumber)) {
                    continue;
                }
                rowsByUser.computeIfAbsent(userNumber.longValue(), id -> new ArrayList<>()).add(row);
            }
            for (Map.Entry<Long, List<Map<String, Object>>> entry : rowsByUser.entrySet()) {
                Long userId = entry.getKey();
                if (!goldPayable(userId)) {
                    continue;
                }
                extras.put(userId, extraFromTaskRows(entry.getValue(), matchGold));
            }
            return extras;
        } catch (Exception e) {
            log.debug("Skip pending daily gold: {}", e.getMessage());
            return extras;
        }
    }

    private static long extraFromTaskRows(List<Map<String, Object>> rows, Map<Integer, Long> matchGold) {
        long extra = 0L;
        int slotProgress = 0;
        Set<Integer> finishedMatchSlots = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String code = String.valueOf(row.get("task_code"));
            Object statusRaw = row.get("status");
            int status = statusRaw instanceof Number statusNumber ? statusNumber.intValue() : 0;
            if ("T-DAILY-SLOT".equals(code)) {
                Object progressRaw = row.get("progress_value");
                if (progressRaw instanceof Number progressNumber) {
                    slotProgress = Math.max(progressNumber.intValue(), 0);
                }
                continue;
            }
            if (code.startsWith("T-DAILY-MATCH-") && status >= 2) {
                try {
                    finishedMatchSlots.add(Integer.parseInt(code.substring("T-DAILY-MATCH-".length())));
                } catch (NumberFormatException ignored) {
                    // skip malformed task codes
                }
            }
            if (status == 2) {
                extra += rewardAmount(row.get("reward_value"));
            }
        }
        int nextSlot = slotProgress + 1;
        if (nextSlot >= 1 && nextSlot <= 3 && !finishedMatchSlots.contains(nextSlot)) {
            extra += matchGold.getOrDefault(nextSlot, 0L);
        }
        return extra;
    }

    private boolean goldPayable(Long userId) {
        try {
            if (workDayService == null) {
                return true;
            }
            WorkDayService.Snapshot snap = workDayService.snapshot(userId);
            return snap == null || !snap.restDay();
        } catch (Exception e) {
            return true;
        }
    }

    private Map<Integer, Long> matchSlotGoldBySlot() {
        Map<Integer, Long> amounts = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT task_code, reward_value
                    FROM tasks
                    WHERE task_code IN ('T-DAILY-MATCH-1','T-DAILY-MATCH-2','T-DAILY-MATCH-3')
                    """);
            for (Map<String, Object> row : rows) {
                String code = String.valueOf(row.get("task_code"));
                if (!code.startsWith("T-DAILY-MATCH-")) {
                    continue;
                }
                try {
                    int slot = Integer.parseInt(code.substring("T-DAILY-MATCH-".length()));
                    amounts.put(slot, rewardAmount(row.get("reward_value")));
                } catch (NumberFormatException ignored) {
                    // skip malformed task codes
                }
            }
        } catch (Exception e) {
            log.debug("Skip daily match gold catalog: {}", e.getMessage());
        }
        return amounts;
    }

    private static long rewardAmount(Object raw) {
        if (raw == null) {
            return 0L;
        }
        Matcher matcher = REWARD_AMOUNT.matcher(String.valueOf(raw));
        if (!matcher.find()) {
            return 0L;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0L;
        }
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
