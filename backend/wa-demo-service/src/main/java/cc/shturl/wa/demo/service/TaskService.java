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

    /**
     * 领取本月已经打完还没领的每日金币、本周周任务和补打。
     * 已下线任务和过期周任务不补发。由每天 23:59 的定时任务调用。
     */
    void claimEarnedGold(Long userId);

    void recordLogin(Long userId);
    void recordMatchResult(Long userId, String resultType, Long teammateId);
    /** 放弃 / 掉线超时：只占用当日第 N 局，不算完成、不给获胜任务。 */
    void consumeDailyMatchSlot(Long userId);
    /** 按本周每天最多 3 局重算不同队友。自己的前 3 局没满时补打占一个空位；作废不占槽，后面的局会滑进前三。 */
    void rebuildWeeklyTeammates(Long userId);
    /** 看广告复活：只记广告进度，不完成「赢第 N 局」。最终获胜才由 recordMatchResult 计入。 */
    void recordAdWatch(Long userId);
    void recordRoomFormation(Long userId, Long teammateId);
    void recordBattleAction(Long userId, String deptType, int cardsPlayed, int damageDealt);
}
