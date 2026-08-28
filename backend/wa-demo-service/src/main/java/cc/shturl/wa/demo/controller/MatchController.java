package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.req.ChooseFirstPlayerReq;
import cc.shturl.wa.demo.dto.req.EndTurnReq;
import cc.shturl.wa.demo.dto.req.PlayCardReq;
import cc.shturl.wa.demo.dto.resp.CurrentMatchResp;
import cc.shturl.wa.demo.dto.resp.EndTurnResp;
import cc.shturl.wa.demo.dto.resp.MatchActionResp;
import cc.shturl.wa.demo.dto.resp.MatchDeckResp;
import cc.shturl.wa.demo.dto.resp.MatchFirstPlayerResp;
import cc.shturl.wa.demo.dto.resp.MatchSettlementResp;
import cc.shturl.wa.demo.dto.resp.MatchStateResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;
    private final AuthTokenSupport authTokenSupport;

    @GetMapping("/current")
    public Result<CurrentMatchResp> getCurrentMatch(@RequestHeader("Authorization") String authorization) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(new CurrentMatchResp(matchService.findActiveMatchId(userId)));
    }

    @GetMapping("/{matchId:\\d+}")
    public Result<MatchStateResp> getMatchState(@RequestHeader("Authorization") String authorization,
                                                @PathVariable("matchId") Long matchId) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.getMatchState(userId, matchId));
    }

    @GetMapping("/{matchId}/deck")
    public Result<MatchDeckResp> getMatchDeck(@RequestHeader("Authorization") String authorization,
                                              @PathVariable("matchId") Long matchId) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.getMatchDeck(userId, matchId));
    }

    @GetMapping("/{matchId}/settlement")
    public Result<MatchSettlementResp> getMatchSettlement(@RequestHeader("Authorization") String authorization,
                                                           @PathVariable("matchId") Long matchId) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.getMatchSettlement(userId, matchId));
    }

    @PostMapping("/{matchId}/choose-first-player")
    public Result<MatchFirstPlayerResp> chooseFirstPlayer(@RequestHeader("Authorization") String authorization,
                                                          @PathVariable("matchId") Long matchId,
                                                          @Valid @RequestBody ChooseFirstPlayerReq request) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.chooseFirstPlayer(userId, matchId, request));
    }

    @PostMapping("/{matchId}/reconnect")
    public Result<MatchStateResp> reconnect(@RequestHeader("Authorization") String authorization,
                                            @PathVariable("matchId") Long matchId) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.reconnect(userId, matchId));
    }

    @PostMapping("/{matchId}/abandon")
    public Result<Void> abandon(@RequestHeader("Authorization") String authorization,
                                @PathVariable("matchId") Long matchId) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        matchService.abandon(userId, matchId);
        return Result.ok();
    }
    @PostMapping("/{matchId}/actions/play-card")
    public Result<MatchActionResp> playCard(@RequestHeader("Authorization") String authorization,
                                            @PathVariable("matchId") Long matchId,
                                            @Valid @RequestBody PlayCardReq request) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.playCard(userId, matchId, request));
    }

    @PostMapping("/{matchId}/actions/end-turn")
    public Result<EndTurnResp> endTurn(@RequestHeader("Authorization") String authorization,
                                      @PathVariable("matchId") Long matchId,
                                      @Valid @RequestBody EndTurnReq request) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(matchService.endTurn(userId, matchId, request));
    }
}
