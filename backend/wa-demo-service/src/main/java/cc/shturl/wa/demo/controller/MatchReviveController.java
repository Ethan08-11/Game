package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.req.MatchReviveReq;
import cc.shturl.wa.demo.dto.resp.MatchReviveResp;
import cc.shturl.wa.demo.dto.resp.MatchReviveStatusResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchReviveController {
    private final MatchService matchService;
    private final AuthTokenSupport authTokenSupport;

    @GetMapping("/{matchId}/revive/status")
    public Result<MatchReviveStatusResp> getReviveStatus(@RequestHeader("Authorization") String authorization,
                                                         @PathVariable("matchId") Long matchId,
                                                         @RequestParam("userId") Long userId) {
        authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.getReviveStatus(userId, matchId));
    }

    @PostMapping("/{matchId}/revive/request")
    public Result<MatchReviveResp> requestRevive(@RequestHeader("Authorization") String authorization,
                                                 @PathVariable("matchId") Long matchId,
                                                 @Valid @RequestBody MatchReviveReq request) {
        Long currentUserId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.requestRevive(currentUserId, matchId, request));
    }
}
