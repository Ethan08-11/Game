package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.resp.LeaderboardResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {
    private final LeaderboardService leaderboardService;
    private final AuthTokenSupport authTokenSupport;

    @GetMapping
    public Result<List<LeaderboardResp>> list(@RequestHeader("Authorization") String authorization,
                                              @RequestParam(value = "page", defaultValue = "1") int page,
                                              @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.ok(leaderboardService.listLeaderboard(authTokenSupport.requireUserIdFromAccessToken(authorization), page, size));
    }

    @GetMapping("/me")
    public Result<LeaderboardResp> me(@RequestHeader("Authorization") String authorization) {
        return Result.ok(leaderboardService.getMyRank(authTokenSupport.requireUserIdFromAccessToken(authorization)));
    }
}
