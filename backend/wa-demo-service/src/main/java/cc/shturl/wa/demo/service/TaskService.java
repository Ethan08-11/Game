package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.TaskClaimResp;
import cc.shturl.wa.demo.dto.resp.TaskResp;
import cc.shturl.wa.demo.dto.resp.UserTaskResp;

import java.util.List;

public interface TaskService {
    List<TaskResp> listTasks(String taskType);
    List<UserTaskResp> listMyTasks(Long userId);
    TaskClaimResp claimTask(Long userId, Long userTaskId);

    void recordMatchResult(Long userId, String resultType, Long teammateId);
    void recordRoomFormation(Long userId, Long teammateId);
}
