package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.resp.MyTaskBoardResp;
import cc.shturl.wa.demo.dto.resp.TaskClaimResp;
import cc.shturl.wa.demo.dto.resp.TaskResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final AuthTokenSupport authTokenSupport;

    @GetMapping
    public Result<List<TaskResp>> listTasks(@RequestParam(value = "taskType", required = false) String taskType) {
        return Result.ok(taskService.listTasks(taskType));
    }

    @GetMapping("/me")
    public Result<MyTaskBoardResp> listMyTasks(@RequestHeader("Authorization") String authorization) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(taskService.listMyTaskBoard(userId));
    }

    @PostMapping("/{userTaskId}/claim")
    public Result<TaskClaimResp> claimTask(@RequestHeader("Authorization") String authorization,
                                           @PathVariable("userTaskId") Long userTaskId) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(taskService.claimTask(userId, userTaskId));
    }
}
