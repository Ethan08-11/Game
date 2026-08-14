package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.resp.CardBasicResp;
import cc.shturl.wa.demo.dto.resp.CardEffectDetailResp;
import cc.shturl.wa.demo.service.CardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardQueryService cardQueryService;

    @GetMapping
    public Result<List<CardBasicResp>> listAllCards() {
        return Result.ok(cardQueryService.listAllCards());
    }

    @GetMapping("/{cardId}")
    public Result<CardBasicResp> getCardDetail(@PathVariable("cardId") Long cardId) {
        return Result.ok(cardQueryService.getCardDetail(cardId));
    }

    @GetMapping("/{cardId}/effects")
    public Result<List<CardEffectDetailResp>> listCardEffects(@PathVariable("cardId") Long cardId) {
        return Result.ok(cardQueryService.listCardEffects(cardId));
    }
}
