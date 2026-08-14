package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.resp.AchievementResp;
import cc.shturl.wa.demo.dto.resp.UserAchievementResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {
    private final AchievementService achievementService;
    private final AuthTokenSupport authTokenSupport;

    @GetMapping
    public Result<List<AchievementResp>> listAchievements(@RequestParam(value = "category", required = false) String category) {
        return Result.ok(achievementService.listAchievements(category));
    }

    @GetMapping("/me")
    public Result<List<UserAchievementResp>> listMyAchievements(@RequestHeader("Authorization") String authorization) {
        return Result.ok(achievementService.listMyAchievements(authTokenSupport.requireUserIdFromAccessToken(authorization)));
    }
}
