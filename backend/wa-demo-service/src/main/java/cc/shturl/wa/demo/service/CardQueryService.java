package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.CardBasicResp;
import cc.shturl.wa.demo.dto.resp.CardEffectDetailResp;

import java.util.List;

public interface CardQueryService {
    List<CardBasicResp> listAllCards(Long userId);
    CardBasicResp getCardDetail(Long userId, Long cardId);
    List<CardEffectDetailResp> listCardEffects(Long userId, Long cardId);
}
