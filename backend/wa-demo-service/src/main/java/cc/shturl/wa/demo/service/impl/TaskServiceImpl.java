package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.resp.TaskClaimResp;
import cc.shturl.wa.demo.dto.resp.TaskResp;
import cc.shturl.wa.demo.dto.resp.UserTaskResp;
import cc.shturl.wa.demo.entity.Tasks;
import cc.shturl.wa.demo.entity.UserTask;
import cc.shturl.wa.demo.mapper.TaskMapper;
import cc.shturl.wa.demo.mapper.UserTaskMapper;
import cc.shturl.wa.demo.service.TaskService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private static final String STATUS_NOT_STARTED = "0";
    private static final String STATUS_IN_PROGRESS = "1";
    private static final String STATUS_COMPLETED = "2";
    private static final String STATUS_CLAIMED = "3";

    private final TaskMapper taskMapper;
    private final UserTaskMapper userTaskMapper;

    @Override
    public List<TaskResp> listTasks(String taskType) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(taskType != null && !taskType.isBlank(), Tasks::getTaskType, taskType)
                .orderByAsc(Tasks::getSortNo, Tasks::getId));
        return tasks.stream().map(task -> new TaskResp(task.getId(), task.getTaskCode(), task.getTaskName(),
                task.getTaskType(), task.getResetType(), task.getPeriodScope(), task.getProgressType(),
                task.getDescription(), task.getConditionType(), task.getConditionValue(),
                task.getRewardType(), task.getRewardValue(), task.getTargetCount(), task.getSortNo(), task.getStatus()))
                .toList();
    }

    @Override
    public List<UserTaskResp> listMyTasks(Long userId) {
        ensureDefaultTasks(userId);
        List<UserTask> userTasks = userTaskMapper.selectList(Wrappers.<UserTask>lambdaQuery()
                .eq(UserTask::getUserId, userId)
                .orderByDesc(UserTask::getCreatedAt));
        return userTasks.stream().map(this::toUserTaskResp).toList();
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
        userTask.setStatus(3);
        userTask.setClaimedAt(java.time.LocalDateTime.now());
        userTaskMapper.updateById(userTask);
        return new TaskClaimResp(userTask.getId(), userTask.getTaskId(), userTask.getProgressValue(),
                userTask.getTargetValue(), userTask.getStatus(), "奖励领取成功");
    }

    @Override
    @Transactional
    public void recordMatchResult(Long userId, String resultType, Long teammateId) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getStatus, 1)
                .like(Tasks::getTaskCode, "T-DAILY-")
                .or()
                .like(Tasks::getTaskCode, "T-WEEKLY-")
                .or()
                .like(Tasks::getTaskCode, "T-MONTHLY-")
                .orderByAsc(Tasks::getSortNo, Tasks::getId));
        for (Tasks task : tasks) {
            if (task.getProgressType() == null) {
                continue;
            }
            String progressType = task.getProgressType();
            if (("WIN_COUNT".equals(progressType) && !"WIN".equals(resultType))
                    || ("LOSE_COUNT".equals(progressType) && !"LOSE".equals(resultType))
                    || (!"WIN_COUNT".equals(progressType) && !"LOSE_COUNT".equals(progressType))) {
                continue;
            }
            String periodKey = buildPeriodKey(task.getPeriodScope());
            UserTask userTask = findOrCreateUserTask(userId, task, periodKey);
            int next = (userTask.getProgressValue() == null ? 0 : userTask.getProgressValue()) + 1;
            userTask.setProgressValue(next);
            if (userTask.getTargetValue() == null || userTask.getTargetValue() <= 0) {
                userTask.setTargetValue(task.getTargetCount() == null ? 1 : task.getTargetCount());
            }
            if (next >= userTask.getTargetValue()) {
                userTask.setStatus(2);
                if (userTask.getCompletedAt() == null) {
                    userTask.setCompletedAt(java.time.LocalDateTime.now());
                }
            } else {
                userTask.setStatus(1);
            }
            userTaskMapper.updateById(userTask);
        }
    }

    @Override
    @Transactional
    public void recordRoomFormation(Long userId, Long teammateId) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getStatus, 1)
                .like(Tasks::getProgressType, "DISTINCT_TEAMMATE_COUNT")
                .orderByAsc(Tasks::getSortNo, Tasks::getId));
        for (Tasks task : tasks) {
            String periodKey = buildPeriodKey(task.getPeriodScope());
            UserTask userTask = findOrCreateUserTask(userId, task, periodKey);
            int next = (userTask.getProgressValue() == null ? 0 : userTask.getProgressValue()) + 1;
            userTask.setProgressValue(next);
            if (userTask.getTargetValue() == null || userTask.getTargetValue() <= 0) {
                userTask.setTargetValue(task.getTargetCount() == null ? 1 : task.getTargetCount());
            }
            if (next >= userTask.getTargetValue()) {
                userTask.setStatus(2);
                if (userTask.getCompletedAt() == null) {
                    userTask.setCompletedAt(java.time.LocalDateTime.now());
                }
            } else {
                userTask.setStatus(1);
            }
            userTaskMapper.updateById(userTask);
        }
    }

    private void ensureDefaultTasks(Long userId) {
        List<Tasks> tasks = taskMapper.selectList(Wrappers.<Tasks>lambdaQuery()
                .eq(Tasks::getStatus, 1));
        for (Tasks task : tasks) {
            String periodKey = buildPeriodKey(task.getResetType());
            UserTask existing = userTaskMapper.selectOne(Wrappers.<UserTask>lambdaQuery()
                    .eq(UserTask::getUserId, userId)
                    .eq(UserTask::getTaskId, task.getId())
                    .eq(UserTask::getPeriodKey, periodKey));
            if (existing == null) {
                UserTask userTask = new UserTask();
                userTask.setUserId(userId);
                userTask.setTaskId(task.getId());
                userTask.setPeriodKey(periodKey);
                userTask.setProgressValue(0);
                userTask.setTargetValue(task.getTargetCount() == null ? 1 : task.getTargetCount());
                userTask.setStatus(0);
                userTaskMapper.insert(userTask);
            }
        }
    }

    private UserTask findOrCreateUserTask(Long userId, Tasks task, String periodKey) {
        UserTask userTask = userTaskMapper.selectOne(Wrappers.<UserTask>lambdaQuery()
                .eq(UserTask::getUserId, userId)
                .eq(UserTask::getTaskId, task.getId())
                .eq(UserTask::getPeriodKey, periodKey));
        if (userTask == null) {
            userTask = new UserTask();
            userTask.setUserId(userId);
            userTask.setTaskId(task.getId());
            userTask.setPeriodKey(periodKey);
            userTask.setProgressValue(0);
            userTask.setTargetValue(task.getTargetCount() == null ? 1 : task.getTargetCount());
            userTask.setStatus(0);
            userTaskMapper.insert(userTask);
        }
        return userTask;
    }

    private String buildPeriodKey(String scope) {
        if (scope == null || scope.isBlank() || "ALL".equalsIgnoreCase(scope)) {
            return "ALL";
        }
        LocalDate now = LocalDate.now();
        if ("DAY".equalsIgnoreCase(scope)) {
            return now.toString();
        }
        if ("WEEK".equalsIgnoreCase(scope)) {
            WeekFields wf = WeekFields.of(Locale.CHINA);
            return now.getYear() + "-W" + now.get(wf.weekOfWeekBasedYear());
        }
        if ("MONTH".equalsIgnoreCase(scope)) {
            YearMonth ym = YearMonth.from(now);
            return ym.toString();
        }
        return now.toString();
    }

    private UserTaskResp toUserTaskResp(UserTask userTask) {
        Tasks task = taskMapper.selectById(userTask.getTaskId());
        return new UserTaskResp(userTask.getId(), userTask.getTaskId(),
                task == null ? null : task.getTaskCode(),
                task == null ? null : task.getTaskName(),
                task == null ? null : task.getTaskType(),
                task == null ? null : task.getResetType(),
                task == null ? null : task.getPeriodScope(),
                task == null ? null : task.getProgressType(),
                task == null ? null : task.getDescription(),
                task == null ? null : task.getConditionType(),
                task == null ? null : task.getConditionValue(),
                task == null ? null : task.getRewardType(),
                task == null ? null : task.getRewardValue(),
                task == null ? null : task.getTargetCount(),
                userTask.getProgressValue(), userTask.getStatus(), userTask.getPeriodKey(),
                userTask.getCompletedAt(), userTask.getClaimedAt());
    }
}
