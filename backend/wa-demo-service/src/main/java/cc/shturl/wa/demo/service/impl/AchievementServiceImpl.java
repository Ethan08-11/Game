package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.AchievementResp;
import cc.shturl.wa.demo.dto.resp.UserAchievementResp;
import cc.shturl.wa.demo.entity.AchievementDefs;
import cc.shturl.wa.demo.entity.UserAchievements;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.enums.FriendshipStatus;
import cc.shturl.wa.demo.mapper.AchievementDefsMapper;
import cc.shturl.wa.demo.mapper.UserAchievementsMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.AchievementService;
import cc.shturl.wa.demo.service.QuestPeriod;
import cc.shturl.wa.demo.service.WorkDayQuota;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {
    private final AchievementDefsMapper achievementDefsMapper;
    private final UserAchievementsMapper userAchievementsMapper;
    private final UserProfileMapper userProfileMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<AchievementResp> listAchievements(String category) {
        List<AchievementDefs> defs = achievementDefsMapper.selectList(Wrappers.<AchievementDefs>lambdaQuery()
                .eq(AchievementDefs::getStatus, 1)
                .eq(category != null && !category.isBlank(), AchievementDefs::getCategory, category)
                .orderByAsc(AchievementDefs::getSortNo, AchievementDefs::getId));
        return defs.stream().map(this::toResp).toList();
    }

    @Override
    public void bump(Long userId, String conditionType, int delta) {
        if (userId == null || conditionType == null || conditionType.isBlank() || delta <= 0) {
            return;
        }
        List<AchievementDefs> defs = achievementDefsMapper.selectList(Wrappers.<AchievementDefs>lambdaQuery()
                .eq(AchievementDefs::getStatus, 1)
                .eq(AchievementDefs::getConditionType, conditionType));
        Snapshot snapshot = globalSnapshot();
        for (AchievementDefs def : defs) {
            UserAchievements row = findOrCreate(userId, def.getId());
            int stored = row.getProgressValue() == null ? 0 : row.getProgressValue();
            applyRow(row, stored + delta, targetOf(def, snapshot));
        }
    }

    @Override
    public void refreshProgress(Long userId) {
        if (userId != null) {
            syncProgress(userId);
        }
    }

    @Override
    public List<UserAchievementResp> listMyAchievements(Long userId) {
        if (userId != null) {
            syncProgress(userId);
        }
        List<UserAchievements> records = userAchievementsMapper.selectList(Wrappers.<UserAchievements>lambdaQuery()
                .eq(UserAchievements::getUserId, userId));
        return records.stream().map(record -> {
            AchievementDefs def = achievementDefsMapper.selectById(record.getAchievementId());
            return new UserAchievementResp(record.getId(), record.getUserId(), record.getAchievementId(),
                    def == null ? null : def.getAchievementCode(),
                    def == null ? null : def.getAchievementName(),
                    record.getProgressValue(), record.getUnlockStatus(), record.getUnlockedAt(),
                    record.getClaimedStatus(), record.getClaimedAt());
        }).toList();
    }

    private AchievementResp toResp(AchievementDefs def) {
        return new AchievementResp(def.getId(), def.getAchievementCode(), def.getAchievementName(),
                def.getCategory(), def.getDescription(), def.getConditionType(), def.getConditionValue(),
                def.getRewardType(), def.getRewardValue(), def.getSortNo(), def.getStatus(),
                def.getDifficulty() == null ? 1 : def.getDifficulty(),
                targetOf(def, globalSnapshot()));
    }

    private void syncProgress(Long userId) {
        Snapshot snapshot = snapshot(userId);
        List<AchievementDefs> defs = achievementDefsMapper.selectList(Wrappers.<AchievementDefs>lambdaQuery()
                .eq(AchievementDefs::getStatus, 1));
        List<AchievementDefs> regular = new ArrayList<>();
        List<AchievementDefs> eggs = new ArrayList<>();
        for (AchievementDefs def : defs) {
            if ("all_unlocked".equals(def.getConditionType())) {
                eggs.add(def);
            } else {
                regular.add(def);
            }
        }
        for (AchievementDefs def : regular) {
            applySnapshot(userId, def, snapshot);
        }
        int unlockedRegular = countUnlocked(userId);
        Snapshot eggSnapshot = snapshot.withAllOthersUnlocked(
                !regular.isEmpty() && unlockedRegular >= regular.size() ? 1 : 0);
        for (AchievementDefs def : eggs) {
            applySnapshot(userId, def, eggSnapshot);
        }
    }

    private void applySnapshot(Long userId, AchievementDefs def, Snapshot snapshot) {
        UserAchievements row = findOrCreate(userId, def.getId());
        int stored = row.getProgressValue() == null ? 0 : row.getProgressValue();
        int progress = Math.max(stored, progressOf(def, snapshot));
        applyRow(row, progress, targetOf(def, snapshot));
    }

    private UserAchievements findOrCreate(Long userId, Long achievementId) {
        UserAchievements row = userAchievementsMapper.selectOne(Wrappers.<UserAchievements>lambdaQuery()
                .eq(UserAchievements::getUserId, userId)
                .eq(UserAchievements::getAchievementId, achievementId)
                .last("LIMIT 1"));
        if (row != null) {
            return row;
        }
        row = new UserAchievements();
        row.setUserId(userId);
        row.setAchievementId(achievementId);
        row.setProgressValue(0);
        row.setUnlockStatus(0);
        row.setClaimedStatus(0);
        userAchievementsMapper.insert(row);
        return row;
    }

    private void applyRow(UserAchievements row, int progress, int target) {
        boolean unlock = target > 0 && progress >= target;
        row.setProgressValue(progress);
        if (unlock && (row.getUnlockStatus() == null || row.getUnlockStatus() == 0)) {
            row.setUnlockStatus(1);
            if (row.getUnlockedAt() == null) {
                row.setUnlockedAt(LocalDateTime.now());
            }
        }
        userAchievementsMapper.updateById(row);
    }

    private int countUnlocked(Long userId) {
        return queryCount("""
                SELECT COUNT(*) FROM user_achievements ua
                INNER JOIN achievement_defs ad ON ad.id = ua.achievement_id
                WHERE ua.user_id = ? AND ua.unlock_status = 1 AND ad.status = 1
                  AND (ad.condition_type IS NULL OR ad.condition_type <> 'all_unlocked')
                """, userId);
    }

    private int progressOf(AchievementDefs def, Snapshot snapshot) {
        String type = def.getConditionType() == null ? "" : def.getConditionType();
        return switch (type) {
            case "win_count" -> snapshot.wins;
            case "match_count" -> snapshot.matches;
            case "task_complete_count" -> snapshot.tasksClaimed;
            case "friend_count" -> snapshot.friends;
            case "work_day_count" -> snapshot.workDaysMonth;
            case "work_day_week" -> snapshot.workDaysWeek;
            case "work_day_half" -> snapshot.workDaysMonth;
            case "work_day_october" -> snapshot.workDaysOctober;
            case "card_count", "card_half", "card_all" -> snapshot.cards;
            case "unique_teammate" -> snapshot.uniqueTeammates;
            case "daily_three_wins" -> snapshot.dailyPerfect;
            case "hp_win_streak" -> snapshot.hpWinStreak;
            case "three_harsh_no_revive" -> snapshot.harshCustomers;
            case "low_hp_no_revive" -> snapshot.lowHpNoRevive;
            case "dept_no_revive" -> snapshot.deptNoRevive;
            case "fast_clear" -> snapshot.fastClears;
            case "perfect_week" -> snapshot.perfectWeek;
            case "perfect_month" -> snapshot.perfectMonth;
            case "customer_kinds" -> snapshot.customerKinds;
            case "clutch_win" -> snapshot.clutchWins;
            case "all_unlocked" -> snapshot.allOthersUnlocked;
            default -> 0;
        };
    }

    private int targetOf(AchievementDefs def, Snapshot snapshot) {
        String type = def.getConditionType() == null ? "" : def.getConditionType();
        if ("card_half".equals(type)) {
            return Math.max(snapshot.collectibleTotal / 2, 1);
        }
        if ("card_all".equals(type)) {
            return Math.max(snapshot.collectibleTotal, 1);
        }
        if ("work_day_half".equals(type)) {
            return Math.max(WorkDayQuota.days(YearMonth.now(WorkDayQuota.ZONE)) / 2, 1);
        }
        if ("dept_win".equals(type) || "dept_no_revive".equals(type)) {
            return jsonInt(def.getConditionValue(), "sales");
        }
        int count = jsonInt(def.getConditionValue(), "count");
        return Math.max(count, 1);
    }

    private Snapshot globalSnapshot() {
        int collectible = queryCount(
                "SELECT COUNT(*) FROM cards WHERE status = 1 AND IFNULL(require_unlock, 0) = 1");
        return new Snapshot(0, 0, 0, 0, 0, collectible, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private Snapshot snapshot(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        int wins = profile == null || profile.getWinCount() == null ? 0 : profile.getWinCount();
        int losses = profile == null || profile.getLoseCount() == null ? 0 : profile.getLoseCount();
        int friends = queryCount("""
                SELECT COUNT(*) FROM (
                  SELECT friend_id AS other_id FROM friendships WHERE status = ? AND user_id = ?
                  UNION
                  SELECT user_id AS other_id FROM friendships WHERE status = ? AND friend_id = ?
                ) t
                """, FriendshipStatus.ACCEPTED.getCode(), userId, FriendshipStatus.ACCEPTED.getCode(), userId);
        int tasksClaimed = queryCount(
                "SELECT COUNT(*) FROM user_tasks WHERE user_id = ? AND status >= 3", userId);
        int cards = queryCount(
                "SELECT COUNT(*) FROM user_card_pools WHERE user_id = ? AND IFNULL(unlocked_status, 1) = 1", userId);
        int collectible = queryCount(
                "SELECT COUNT(*) FROM cards WHERE status = 1 AND IFNULL(require_unlock, 0) = 1");
        LocalDateTime now = QuestPeriod.now();
        LocalDate today = QuestPeriod.dailyDate(now);
        YearMonth month = YearMonth.from(today);
        int workDaysMonth = queryCount(
                "SELECT COUNT(*) FROM user_month_work_days WHERE user_id = ? AND day_date >= ? AND day_date < ?",
                userId, month.atDay(1).toString(), month.plusMonths(1).atDay(1).toString());
        LocalDate weekStart = QuestPeriod.weeklyStart(now);
        int workDaysWeek = queryCount(
                "SELECT COUNT(*) FROM user_month_work_days WHERE user_id = ? AND day_date >= ? AND day_date < ?",
                userId, weekStart.toString(), weekStart.plusWeeks(1).toString());
        int workDaysOctober = WorkDayQuota.OCTOBER_VACATION_MONTH.equals(month) ? workDaysMonth : 0;
        int uniqueTeammates = queryUniqueTeammates(userId, weekStart.toString());
        int dailyDone = queryCount("""
                SELECT COUNT(DISTINCT t.task_code)
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.user_id = ? AND ut.period_key = ?
                  AND t.task_code IN (
                    'T-DAILY-MATCH-1','T-DAILY-MATCH-2','T-DAILY-MATCH-3',
                    'T-DAILY-WIN-1','T-DAILY-WIN-2','T-DAILY-WIN-3')
                  AND ut.status >= 2
                """, userId, today.toString());
        int dailyPerfect = dailyDone >= 6 ? 1 : 0;
        Hidden hidden = hiddenProgress(userId);
        return new Snapshot(wins, wins + losses, friends, tasksClaimed, cards, collectible,
                workDaysMonth, workDaysWeek, workDaysOctober, uniqueTeammates, dailyPerfect, 0,
                hidden.hpWinStreak, hidden.harshCustomers, hidden.lowHpNoRevive,
                hidden.deptNoRevive, hidden.fastClears, hidden.perfectWeek,
                perfectMonth(userId), customerKinds(userId), clutchWins(userId));
    }

    private int queryUniqueTeammates(Long userId, String weekKey) {
        try {
            List<String> extras = jdbcTemplate.queryForList("""
                    SELECT ut.extra_data
                    FROM user_tasks ut
                    INNER JOIN tasks t ON t.id = ut.task_id
                    WHERE ut.user_id = ? AND t.task_code = 'T-WEEKLY-TEAM-10' AND ut.period_key = ?
                    """, String.class, userId, weekKey);
            if (extras.isEmpty() || extras.get(0) == null || extras.get(0).isBlank()) {
                return 0;
            }
            JsonNode node = objectMapper.readTree(extras.get(0));
            return node.isArray() ? node.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int queryCount(String sql, Object... args) {
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return count == null ? 0 : count;
        } catch (Exception e) {
            return 0;
        }
    }

    private int jsonInt(String json, String field) {
        if (json == null || json.isBlank()) {
            return 0;
        }
        try {
            return objectMapper.readTree(json).path(field).asInt(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private Hidden hiddenProgress(Long userId) {
        return new Hidden(
                hpWinStreak(userId),
                harshCustomers(userId),
                lowHpNoRevive(userId),
                deptNoRevive(userId),
                fastClears(userId),
                perfectWeek(userId));
    }

    private int hpWinStreak(Long userId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT m.winner_type AS winner_type,
                           (
                             SELECT COUNT(*) FROM match_actions a
                             WHERE a.match_id = m.id AND a.action_type = 'high_pressure'
                           ) AS high_pressure
                    FROM matches m
                    INNER JOIN match_players mp ON mp.match_id = m.id
                    WHERE mp.user_id = ? AND m.status = 2 AND IFNULL(m.winner_type, 0) IN (1, 2)
                    ORDER BY m.id
                    """, userId);
            int streak = 0;
            int best = 0;
            for (Map<String, Object> row : rows) {
                int winner = number(row.get("winner_type"));
                boolean pressure = number(row.get("high_pressure")) > 0;
                if (winner == 1 && pressure) {
                    streak++;
                    best = Math.max(best, streak);
                } else {
                    streak = 0;
                }
            }
            return best;
        } catch (Exception e) {
            return 0;
        }
    }

    private int harshCustomers(Long userId) {
        return queryCount("""
                SELECT COUNT(DISTINCT ct.customer_code)
                FROM matches m
                INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                INNER JOIN customer_types ct ON ct.id = m.customer_type_id
                WHERE m.status = 2 AND m.winner_type = 1
                  AND ct.customer_code IN ('CUSTOMER_TIMID', 'CUSTOMER_ANXIOUS', 'CUSTOMER_HARSH')
                  AND NOT EXISTS (
                    SELECT 1 FROM match_players mp
                    WHERE mp.match_id = m.id AND IFNULL(mp.revive_count, 0) > 0
                  )
                """, userId);
    }

    private int lowHpNoRevive(Long userId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM matches m
                INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                WHERE m.status = 2 AND m.winner_type = 1
                  AND (SELECT COUNT(*) FROM match_players mp WHERE mp.match_id = m.id) >= 2
                  AND NOT EXISTS (
                    SELECT 1 FROM match_players mp
                    WHERE mp.match_id = m.id AND IFNULL(mp.revive_count, 0) > 0
                  )
                  AND NOT EXISTS (
                    SELECT 1 FROM match_players mp
                    WHERE mp.match_id = m.id AND (mp.min_hp IS NULL OR mp.min_hp > 5)
                  )
                """, userId);
    }

    private int deptNoRevive(Long userId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT me.dept_type AS dept_type, COUNT(*) AS wins
                    FROM matches m
                    INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                    WHERE m.status = 2 AND m.winner_type = 1
                      AND me.dept_type IN ('sales', 'purchase')
                      AND NOT EXISTS (
                        SELECT 1 FROM match_players mp
                        WHERE mp.match_id = m.id AND IFNULL(mp.revive_count, 0) > 0
                      )
                    GROUP BY me.dept_type
                    """, userId);
            int sales = 0;
            int purchase = 0;
            for (Map<String, Object> row : rows) {
                int wins = number(row.get("wins"));
                String dept = String.valueOf(row.get("dept_type"));
                if ("sales".equals(dept)) {
                    sales = wins;
                } else if ("purchase".equals(dept)) {
                    purchase = wins;
                }
            }
            return Math.min(sales, purchase);
        } catch (Exception e) {
            return 0;
        }
    }

    private int fastClears(Long userId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM matches m
                INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                WHERE m.status = 2 AND m.winner_type = 1
                  AND IFNULL(m.current_round, 99) <= 8
                  AND NOT EXISTS (
                    SELECT 1 FROM match_players mp
                    WHERE mp.match_id = m.id AND IFNULL(mp.revive_count, 0) > 0
                  )
                """, userId);
    }

    private int perfectWeek(Long userId) {
        try {
            List<String> days = jdbcTemplate.queryForList("""
                    SELECT ut.period_key
                    FROM user_tasks ut
                    INNER JOIN tasks t ON t.id = ut.task_id
                    WHERE ut.user_id = ? AND ut.status >= 2
                      AND t.task_code IN (
                        'T-DAILY-MATCH-1','T-DAILY-MATCH-2','T-DAILY-MATCH-3',
                        'T-DAILY-WIN-1','T-DAILY-WIN-2','T-DAILY-WIN-3')
                    GROUP BY ut.period_key
                    HAVING COUNT(DISTINCT t.task_code) = 6
                    """, String.class, userId);
            Map<LocalDate, Integer> weeks = new HashMap<>();
            for (String day : days) {
                if (day == null || day.isBlank()) {
                    continue;
                }
                LocalDate date = LocalDate.parse(day);
                LocalDate week = QuestPeriod.weeklyStartForDaily(date);
                weeks.merge(week, 1, Integer::sum);
            }
            int best = 0;
            for (int count : weeks.values()) {
                best = Math.max(best, count);
            }
            return best;
        } catch (Exception e) {
            return 0;
        }
    }

    private int perfectMonth(Long userId) {
        try {
            List<String> days = jdbcTemplate.queryForList("""
                    SELECT ut.period_key
                    FROM user_tasks ut
                    INNER JOIN tasks t ON t.id = ut.task_id
                    WHERE ut.user_id = ? AND ut.status >= 2
                      AND t.task_code IN (
                        'T-DAILY-MATCH-1','T-DAILY-MATCH-2','T-DAILY-MATCH-3',
                        'T-DAILY-WIN-1','T-DAILY-WIN-2','T-DAILY-WIN-3')
                    GROUP BY ut.period_key
                    HAVING COUNT(DISTINCT t.task_code) = 6
                    """, String.class, userId);
            Map<YearMonth, Integer> months = new HashMap<>();
            for (String day : days) {
                if (day == null || day.isBlank()) {
                    continue;
                }
                YearMonth month = YearMonth.from(LocalDate.parse(day));
                months.merge(month, 1, Integer::sum);
            }
            int best = 0;
            for (int count : months.values()) {
                best = Math.max(best, count);
            }
            return best;
        } catch (Exception e) {
            return 0;
        }
    }

    private int customerKinds(Long userId) {
        return queryCount("""
                SELECT COUNT(DISTINCT ct.customer_code)
                FROM matches m
                INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                INNER JOIN customer_types ct ON ct.id = m.customer_type_id
                WHERE m.status = 2 AND m.winner_type = 1
                """, userId);
    }

    private int clutchWins(Long userId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM matches m
                INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                WHERE m.status = 2 AND m.winner_type = 1
                  AND me.min_hp IS NOT NULL AND me.min_hp <= 10
                """, userId);
    }

    private int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof Boolean flag) {
            return flag ? 1 : 0;
        }
        return 0;
    }

    private record Hidden(int hpWinStreak, int harshCustomers, int lowHpNoRevive, int deptNoRevive,
                          int fastClears, int perfectWeek) {
    }

    private record Snapshot(int wins, int matches, int friends, int tasksClaimed, int cards, int collectibleTotal,
                            int workDaysMonth, int workDaysWeek, int workDaysOctober, int uniqueTeammates,
                            int dailyPerfect, int allOthersUnlocked,
                            int hpWinStreak, int harshCustomers, int lowHpNoRevive, int deptNoRevive,
                            int fastClears, int perfectWeek,
                            int perfectMonth, int customerKinds, int clutchWins) {
        Snapshot withAllOthersUnlocked(int value) {
            return new Snapshot(wins, matches, friends, tasksClaimed, cards, collectibleTotal,
                    workDaysMonth, workDaysWeek, workDaysOctober, uniqueTeammates, dailyPerfect, value,
                    hpWinStreak, harshCustomers, lowHpNoRevive, deptNoRevive, fastClears, perfectWeek,
                    perfectMonth, customerKinds, clutchWins);
        }
    }
}
