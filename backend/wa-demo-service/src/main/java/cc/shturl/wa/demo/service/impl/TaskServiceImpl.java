package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.resp.MyTaskBoardResp;
import cc.shturl.wa.demo.dto.resp.TaskClaimResp;
import cc.shturl.wa.demo.dto.resp.TaskResp;
import cc.shturl.wa.demo.dto.resp.UserTaskResp;
import cc.shturl.wa.demo.entity.Tasks;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.entity.UserTask;
import cc.shturl.wa.demo.mapper.TaskMapper;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.mapper.UserTaskMapper;
import cc.shturl.wa.demo.service.LeaderboardService;
import cc.shturl.wa.demo.service.TaskService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String WEEKLY_TEAM_CODE = "T-WEEKLY-TEAM-10";

    private final TaskMapper taskMapper;
    private final UserTaskMapper userTaskMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final LeaderboardService leaderboardService;
    private final ObjectMapper objectMapper;

    @Override
    public List<TaskResp> listTasks(String taskType) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(taskType != null && !taskType.isBlank(), Tasks::getTaskType, taskType)
                .eq(Tasks::getStatus, 1)
                .orderByAsc(Tasks::getSortNo, Tasks::getId));
        return tasks.stream().map(this::toTaskResp).toList();
    }

    @Override
    public List<UserTaskResp> listMyTasks(Long userId) {
        return listMyTaskBoard(userId).tasks();
    }

    @Override
    @Transactional
    public MyTaskBoardResp listMyTaskBoard(Long userId) {
        if (!userExists(userId)) {
            return new MyTaskBoardResp(List.of(), 0, 0, false, secondsUntilDailyReset());
        }
        ensureDefaultTasks(userId);
        recordLogin(userId);
        List<UserTaskResp> visible = currentVisibleTasks(userId);
        int remainingMoney = 0;
        int claimable = 0;
        for (UserTaskResp item : visible) {
            int status = item.status() == null ? 0 : item.status();
            if (status < 3 && "daily".equalsIgnoreCase(item.taskType()) && "money".equalsIgnoreCase(item.rewardType())) {
                remainingMoney += rewardAmount(item.rewardValue());
            }
            if (status == 2) {
                claimable++;
            }
        }
        return new MyTaskBoardResp(visible, remainingMoney, claimable, false, secondsUntilDailyReset());
    }

    @Override
    @Transactional
    public TaskClaimResp claimTask(Long userId, Long userTaskId) {
        UserTask userTask = userTaskMapper.selectById(userTaskId);
        if (userTask == null) {
            throw new BusinessException("任务记录不存在");
        }
        if (!userId.equals(userTask.getUserId())) {
            throw new BusinessException("无权领取该任务奖励");
        }
        if (userTask.getStatus() == null || userTask.getStatus() < 2) {
            throw new BusinessException("任务尚未完成，无法领取奖励");
        }
        if (userTask.getStatus() == 3) {
            throw new BusinessException("任务奖励已领取");
        }
        Tasks task = taskMapper.selectById(userTask.getTaskId());
        long money = 0L;
        int exp = 0;
        if (task != null) {
            int amount = rewardAmount(task.getRewardValue());
            if ("money".equalsIgnoreCase(task.getRewardType())) {
                money = amount;
            } else if ("exp".equalsIgnoreCase(task.getRewardType())) {
                exp = amount;
            }
        }
        if (money > 0 || exp > 0) {
            leaderboardService.ensureCurrentMonth();
            userProfileMapper.applyMatchSettlement(userId, 0, 0, 0, exp, money);
        }
        userTask.setStatus(3);
        userTask.setClaimedAt(java.time.LocalDateTime.now());
        userTaskMapper.updateById(userTask);
        return new TaskClaimResp(userTask.getId(), userTask.getTaskId(), userTask.getProgressValue(),
                userTask.getTargetValue(), userTask.getStatus(), "奖励领取成功", money, exp);
    }

    @Override
    @Transactional
    public void recordLogin(Long userId) {
        if (!userExists(userId)) {
            return;
        }
        ensureDefaultTasks(userId);
        LocalDate today = LocalDate.now(ZONE);
        UserProfile profile = userProfileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery()
                .eq(UserProfile::getUserId, userId));
        if (profile == null) {
            bumpByProgressType(userId, "LOGIN_COUNT", 1, null);
            return;
        }
        LocalDate last = profile.getLastTaskLoginDate();
        if (today.equals(last)) {
            bumpByProgressType(userId, "LOGIN_COUNT", 1, null);
            syncLoginStreakProgress(userId, profile.getLoginStreak() == null ? 1 : profile.getLoginStreak());
            return;
        }
        int streak = profile.getLoginStreak() == null ? 0 : profile.getLoginStreak();
        boolean broken = last != null && !last.plusDays(1).equals(today);
        if (broken) {
            resetStreakTasks(userId);
            streak = 1;
        } else if (last != null && last.plusDays(1).equals(today)) {
            streak = streak + 1;
        } else {
            streak = 1;
        }
        profile.setLoginStreak(streak);
        profile.setLastTaskLoginDate(today);
        userProfileMapper.updateById(profile);
        bumpByProgressType(userId, "LOGIN_COUNT", 1, null);
        syncLoginStreakProgress(userId, streak);
    }

    @Override
    @Transactional
    public void recordMatchResult(Long userId, String resultType, Long teammateId) {
        if (!userExists(userId)) {
            log.warn("Skip match task progress for missing user {}", userId);
            return;
        }
        int previous = dailyMatchProgress(userId);
        bumpByProgressType(userId, "MATCH_COUNT", 1, null);
        int slot = previous + 1;
        if (slot <= 3) {
            if ("WIN".equalsIgnoreCase(resultType)) {
                completeMatchSlotWin(userId, slot);
            }
            if (teammateId != null) {
                addUniqueTeammate(userId, teammateId);
            }
        }
        if ("LOSE".equalsIgnoreCase(resultType)) {
            bumpByProgressType(userId, "LOSE_COUNT", 1, null);
        }
        if (teammateId != null) {
            bumpByProgressType(userId, "COOP_MATCH_COUNT", 1, null);
        }
    }

    @Override
    @Transactional
    public void recordAdWatch(Long userId) {
        if (!userExists(userId)) {
            return;
        }
        int slot = dailyMatchProgress(userId) + 1;
        if (slot >= 1 && slot <= 3) {
            completeMatchSlotWin(userId, slot);
        }
    }

    @Override
    @Transactional
    public void recordRoomFormation(Long userId, Long teammateId) {
        if (!userExists(userId)) {
            log.warn("Skip room task progress for missing user {}", userId);
            return;
        }
        bumpByProgressType(userId, "TEAM_COUNT", 1, null);
    }

    @Override
    @Transactional
    public void recordBattleAction(Long userId, String deptType, int cardsPlayed, int damageDealt) {
        if (!userExists(userId)) {
            return;
        }
        if (cardsPlayed > 0) {
            bumpByProgressType(userId, "CARD_PLAY_COUNT", cardsPlayed, null);
            if (deptType != null && !deptType.isBlank()) {
                bumpByProgressType(userId, "PLAY_DEPT", cardsPlayed, deptType);
            }
        }
        if (damageDealt > 0) {
            bumpByProgressType(userId, "DAMAGE_DEALT", damageDealt, null);
        }
    }

    private void bumpByProgressType(Long userId, String progressType, int delta, String deptType) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getStatus, 1));
        for (Tasks task : tasks) {
            if (!matchesProgressType(task, progressType)) {
                continue;
            }
            if ("PLAY_DEPT".equals(progressType) && !deptMatches(task, deptType)) {
                continue;
            }
            if ("MATCH_SLOT_WIN".equals(progressType)) {
                continue;
            }
            UserTask userTask = findOrCreateUserTask(userId, task, periodKeyFor(task));
            if (userTask == null || userTask.getId() == null) {
                continue;
            }
            if (userTask.getStatus() != null && userTask.getStatus() >= 2) {
                continue;
            }
            int next = (userTask.getProgressValue() == null ? 0 : userTask.getProgressValue()) + delta;
            applyProgress(userTask, task, next);
        }
    }

    private boolean matchesProgressType(Tasks task, String progressType) {
        String actual = task.getProgressType();
        if (progressType.equalsIgnoreCase(actual)) {
            return true;
        }
        String condition = task.getConditionType();
        if (condition == null || condition.isBlank()) {
            return false;
        }
        if ("CARD_PLAY_COUNT".equals(progressType)) {
            return "card_play_count".equalsIgnoreCase(condition)
                    || "play_card".equalsIgnoreCase(condition);
        }
        if ("WIN_COUNT".equals(progressType)) {
            return "win_count".equalsIgnoreCase(condition);
        }
        if ("MATCH_COUNT".equals(progressType)) {
            return "match_count".equalsIgnoreCase(condition);
        }
        if ("MATCH_SLOT_WIN".equals(progressType)) {
            return "match_slot_win".equalsIgnoreCase(condition);
        }
        if ("DISTINCT_TEAMMATE_COUNT".equals(progressType)) {
            return "distinct_teammate".equalsIgnoreCase(condition);
        }
        if ("LOGIN_COUNT".equals(progressType)) {
            return "login_count".equalsIgnoreCase(condition) || "login_days".equalsIgnoreCase(condition);
        }
        return condition.equalsIgnoreCase(progressType);
    }

    private void syncLoginStreakProgress(Long userId, int streak) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getStatus, 1)
                .eq(Tasks::getProgressType, "LOGIN_STREAK"));
        for (Tasks task : tasks) {
            UserTask userTask = findOrCreateUserTask(userId, task, periodKeyFor(task));
            if (userTask == null || userTask.getId() == null) {
                continue;
            }
            if (userTask.getStatus() != null && userTask.getStatus() >= 3) {
                continue;
            }
            applyProgress(userTask, task, streak);
        }
    }

    private void resetStreakTasks(Long userId) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getProgressType, "LOGIN_STREAK"));
        for (Tasks task : tasks) {
            UserTask userTask = userTaskMapper.selectOne(Wrappers.<UserTask>lambdaQuery()
                    .eq(UserTask::getUserId, userId)
                    .eq(UserTask::getTaskId, task.getId())
                    .eq(UserTask::getPeriodKey, periodKeyFor(task)));
            if (userTask == null) {
                continue;
            }
            userTask.setProgressValue(0);
            userTask.setStatus(0);
            userTask.setCompletedAt(null);
            userTask.setClaimedAt(null);
            userTaskMapper.updateById(userTask);
        }
    }

    private void applyProgress(UserTask userTask, Tasks task, int next) {
        int target = userTask.getTargetValue() == null || userTask.getTargetValue() <= 0
                ? (task.getTargetCount() == null ? 1 : task.getTargetCount())
                : userTask.getTargetValue();
        userTask.setTargetValue(target);
        userTask.setProgressValue(next);
        if (next >= target) {
            userTask.setStatus(2);
            if (userTask.getCompletedAt() == null) {
                userTask.setCompletedAt(java.time.LocalDateTime.now());
            }
        } else {
            userTask.setStatus(next > 0 ? 1 : 0);
        }
        userTaskMapper.updateById(userTask);
    }

    private int dailyMatchProgress(Long userId) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getStatus, 1)
                .eq(Tasks::getProgressType, "MATCH_COUNT"));
        int max = 0;
        for (Tasks task : tasks) {
            UserTask userTask = userTaskMapper.selectOne(Wrappers.<UserTask>lambdaQuery()
                    .eq(UserTask::getUserId, userId)
                    .eq(UserTask::getTaskId, task.getId())
                    .eq(UserTask::getPeriodKey, periodKeyFor(task)));
            if (userTask == null || userTask.getProgressValue() == null) {
                continue;
            }
            max = Math.max(max, userTask.getProgressValue());
        }
        return max;
    }

    private void completeMatchSlotWin(Long userId, int slot) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getStatus, 1)
                .eq(Tasks::getProgressType, "MATCH_SLOT_WIN"));
        for (Tasks task : tasks) {
            if (jsonInt(task.getConditionValue(), "slot") != slot) {
                continue;
            }
            UserTask userTask = findOrCreateUserTask(userId, task, periodKeyFor(task));
            if (userTask == null || userTask.getId() == null) {
                continue;
            }
            if (userTask.getStatus() != null && userTask.getStatus() >= 2) {
                continue;
            }
            applyProgress(userTask, task, 1);
        }
    }

    private void addUniqueTeammate(Long userId, Long teammateId) {
        Tasks task = taskMapper.selectOne(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getTaskCode, WEEKLY_TEAM_CODE)
                .eq(Tasks::getStatus, 1)
                .last("LIMIT 1"));
        if (task == null) {
            return;
        }
        UserTask userTask = findOrCreateUserTask(userId, task, periodKeyFor(task));
        if (userTask == null || userTask.getId() == null) {
            return;
        }
        if (userTask.getStatus() != null && userTask.getStatus() >= 2) {
            return;
        }
        Set<Long> ids = parseIdSet(userTask.getExtraData());
        if (!ids.add(teammateId)) {
            return;
        }
        userTask.setExtraData(writeIdSet(ids));
        applyProgress(userTask, task, ids.size());
    }

    private Set<Long> parseIdSet(String json) {
        Set<Long> ids = new LinkedHashSet<>();
        JsonNode node = readJson(json);
        if (node == null || !node.isArray()) {
            return ids;
        }
        for (JsonNode item : node) {
            if (item.isNumber()) {
                ids.add(item.longValue());
            }
        }
        return ids;
    }

    private String writeIdSet(Set<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void ensureDefaultTasks(Long userId) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getStatus, 1));
        for (Tasks task : tasks) {
            String periodKey = periodKeyFor(task);
            UserTask existing = userTaskMapper.selectOne(Wrappers.<UserTask>lambdaQuery()
                    .eq(UserTask::getUserId, userId)
                    .eq(UserTask::getTaskId, task.getId())
                    .eq(UserTask::getPeriodKey, periodKey));
            if (existing == null) {
                insertUserTask(userId, task, periodKey);
            }
        }
    }

    private List<UserTaskResp> currentVisibleTasks(Long userId) {
        List<UserTask> userTasks = userTaskMapper.selectList(Wrappers.<UserTask>lambdaQuery()
                .eq(UserTask::getUserId, userId));
        List<UserTaskResp> result = new ArrayList<>();
        java.util.Map<Long, Integer> sortNos = new java.util.HashMap<>();
        for (UserTask userTask : userTasks) {
            Tasks task = taskMapper.selectById(userTask.getTaskId());
            if (task == null || task.getStatus() == null || task.getStatus() != 1) {
                continue;
            }
            if (!periodKeyFor(task).equals(userTask.getPeriodKey())) {
                continue;
            }
            sortNos.put(task.getId(), task.getSortNo() == null ? 0 : task.getSortNo());
            result.add(toUserTaskResp(userTask, task));
        }
        result.sort(Comparator
                .comparing((UserTaskResp item) -> typeOrder(item.taskType()))
                .thenComparing(item -> sortNos.getOrDefault(item.taskId(), 0)));
        return result;
    }

    private int typeOrder(String type) {
        if ("daily".equalsIgnoreCase(type)) return 0;
        if ("weekly".equalsIgnoreCase(type)) return 1;
        if ("growth".equalsIgnoreCase(type)) return 2;
        return 3;
    }

    private UserTask findOrCreateUserTask(Long userId, Tasks task, String periodKey) {
        UserTask userTask = userTaskMapper.selectOne(Wrappers.<UserTask>lambdaQuery()
                .eq(UserTask::getUserId, userId)
                .eq(UserTask::getTaskId, task.getId())
                .eq(UserTask::getPeriodKey, periodKey));
        if (userTask == null) {
            userTask = insertUserTask(userId, task, periodKey);
        }
        return userTask;
    }

    private UserTask insertUserTask(Long userId, Tasks task, String periodKey) {
        UserTask userTask = new UserTask();
        userTask.setUserId(userId);
        userTask.setTaskId(task.getId());
        userTask.setPeriodKey(periodKey);
        userTask.setProgressValue(0);
        userTask.setTargetValue(task.getTargetCount() == null ? 1 : task.getTargetCount());
        userTask.setStatus(0);
        try {
            userTaskMapper.insert(userTask);
        } catch (RuntimeException e) {
            if (!isIntegrityViolation(e)) {
                throw e;
            }
            log.warn("Skip user_tasks insert for user {} task {}: {}", userId, task.getId(), e.getMessage());
            return userTaskMapper.selectOne(Wrappers.<UserTask>lambdaQuery()
                    .eq(UserTask::getUserId, userId)
                    .eq(UserTask::getTaskId, task.getId())
                    .eq(UserTask::getPeriodKey, periodKey));
        }
        return userTask;
    }

    private boolean deptMatches(Tasks task, String deptType) {
        if (deptType == null) {
            return false;
        }
        String expected = jsonText(task.getConditionValue(), "dept");
        if (expected == null || expected.isBlank()) {
            return true;
        }
        String actual = normalizeDept(deptType);
        return expected.equalsIgnoreCase(actual) || expected.equalsIgnoreCase(deptType.trim());
    }

    private String normalizeDept(String deptType) {
        String value = deptType.trim().toLowerCase();
        if (value.contains("销售") || "sales".equals(value)) {
            return "sales";
        }
        if (value.contains("采购") || "purchase".equals(value)) {
            return "purchase";
        }
        if (value.contains("公共") || value.contains("老板") || "public".equals(value) || "boss".equals(value)) {
            return "public";
        }
        return value;
    }

    private String periodKeyFor(Tasks task) {
        String reset = task.getResetType() == null ? "" : task.getResetType();
        String scope = task.getPeriodScope() == null ? "" : task.getPeriodScope();
        LocalDate now = LocalDate.now(ZONE);
        if ("DAILY".equalsIgnoreCase(reset) || "DAY".equalsIgnoreCase(scope)) {
            return now.toString();
        }
        if ("WEEKLY".equalsIgnoreCase(reset) || "WEEK".equalsIgnoreCase(scope)) {
            return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
        }
        if ("MONTHLY".equalsIgnoreCase(reset) || "MONTH".equalsIgnoreCase(scope)) {
            return YearMonth.from(now).toString();
        }
        if ("ALL".equalsIgnoreCase(scope) || "NONE".equalsIgnoreCase(reset)) {
            return "ALL";
        }
        return now.toString();
    }

    private long secondsUntilDailyReset() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime next = now.toLocalDate().plusDays(1).atStartOfDay(ZONE);
        return Math.max(Duration.between(now, next).getSeconds(), 0L);
    }

    private int rewardAmount(String rewardValue) {
        JsonNode node = readJson(rewardValue);
        if (node == null) {
            return 0;
        }
        return node.path("amount").asInt(0);
    }

    private int jsonInt(String json, String field) {
        JsonNode node = readJson(json);
        if (node == null || node.path(field).isMissingNode()) {
            return 0;
        }
        return node.path(field).asInt(0);
    }

    private String jsonText(String json, String field) {
        JsonNode node = readJson(json);
        if (node == null || node.path(field).isMissingNode()) {
            return null;
        }
        return node.path(field).asText(null);
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isIntegrityViolation(Throwable error) {
        while (error != null) {
            if (error instanceof DataIntegrityViolationException
                    || error instanceof java.sql.SQLIntegrityConstraintViolationException) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    private boolean userExists(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null;
    }

    private TaskResp toTaskResp(Tasks task) {
        return new TaskResp(task.getId(), task.getTaskCode(), task.getTaskName(),
                task.getTaskType(), task.getResetType(), task.getPeriodScope(), task.getProgressType(),
                task.getDescription(), task.getConditionType(), task.getConditionValue(),
                task.getRewardType(), task.getRewardValue(), task.getTargetCount(), task.getSortNo(), task.getStatus());
    }

    private UserTaskResp toUserTaskResp(UserTask userTask, Tasks task) {
        return new UserTaskResp(userTask.getId(), userTask.getTaskId(),
                task.getTaskCode(), task.getTaskName(), task.getTaskType(),
                task.getResetType(), task.getPeriodScope(), task.getProgressType(),
                task.getDescription(), task.getConditionType(), task.getConditionValue(),
                task.getRewardType(), task.getRewardValue(), task.getTargetCount(),
                userTask.getProgressValue(), userTask.getStatus(), userTask.getPeriodKey(),
                userTask.getCompletedAt(), userTask.getClaimedAt());
    }
}
