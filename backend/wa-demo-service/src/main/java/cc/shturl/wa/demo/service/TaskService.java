package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.MyTaskBoardResp;
import cc.shturl.wa.demo.dto.resp.TaskClaimResp;
import cc.shturl.wa.demo.dto.resp.TaskResp;
import cc.shturl.wa.demo.dto.resp.UserTaskResp;

import java.util.List;

public interface TaskService {
    List<TaskResp> listTasks(String taskType);
    List<UserTaskResp> listMyTasks(Long userId);
    MyTaskBoardResp listMyTaskBoard(Long userId);
    TaskClaimResp claimTask(Long userId, Long userTaskId);

    void recordLogin(Long userId);
    void recordMatchResult(Long userId, String resultType, Long teammateId);
    /** 放弃 / 掉线超时：只占用当日第 N 局，不算完成、不给获胜任务。 */
    void consumeDailyMatchSlot(Long userId);
    void recordAdWatch(Long userId);
    void recordRoomFormation(Long userId, Long teammateId);
    void recordBattleAction(Long userId, String deptType, int cardsPlayed, int damageDealt);
}
