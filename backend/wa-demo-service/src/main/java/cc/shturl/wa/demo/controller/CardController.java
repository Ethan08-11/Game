package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.resp.CardBasicResp;
import cc.shturl.wa.demo.dto.resp.CardEffectDetailResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.CardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardQueryService cardQueryService;
    private final AuthTokenSupport authTokenSupport;

    @GetMapping
    public Result<List<CardBasicResp>> listAllCards(@RequestHeader("Authorization") String authorization) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(cardQueryService.listAllCards(userId));
    }

    @GetMapping("/{cardId}")
    public Result<CardBasicResp> getCardDetail(@RequestHeader("Authorization") String authorization,
                                               @PathVariable("cardId") Long cardId) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(cardQueryService.getCardDetail(userId, cardId));
    }

    @GetMapping("/{cardId}/effects")
    public Result<List<CardEffectDetailResp>> listCardEffects(@RequestHeader("Authorization") String authorization,
                                                              @PathVariable("cardId") Long cardId) {
        Long userId = authTokenSupport.requireUserIdFromAccessToken(authorization);
        return Result.ok(cardQueryService.listCardEffects(userId, cardId));
    }
}
