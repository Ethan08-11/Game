package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.resp.CardBasicResp;
import cc.shturl.wa.demo.dto.resp.CardEffectDetailResp;
import cc.shturl.wa.demo.entity.CardEffects;
import cc.shturl.wa.demo.entity.Cards;
import cc.shturl.wa.demo.mapper.CardEffectsMapper;
import cc.shturl.wa.demo.mapper.CardsMapper;
import cc.shturl.wa.demo.service.CardQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardQueryServiceImpl implements CardQueryService {
    private final CardsMapper cardsMapper;
    private final CardEffectsMapper cardEffectsMapper;

    @Override
    public List<CardBasicResp> listAllCards() {
        List<Cards> cards = cardsMapper.selectList(Wrappers.<Cards>lambdaQuery()
                .orderByAsc(Cards::getDeptType, Cards::getCost, Cards::getCardCode));
        return cards.stream().map(this::toCardBasicResp).toList();
    }

    @Override
    public CardBasicResp getCardDetail(Long cardId) {
        Cards card = cardsMapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException("卡牌不存在");
        }
        return toCardBasicResp(card);
    }

    @Override
    public List<CardEffectDetailResp> listCardEffects(Long cardId) {
        requireCard(cardId);
        return cardEffectsMapper.selectList(Wrappers.<CardEffects>lambdaQuery()
                        .eq(CardEffects::getCardId, cardId)
                        .orderByAsc(CardEffects::getEffectOrder, CardEffects::getId))
                .stream()
                .map(effect -> new CardEffectDetailResp(effect.getId(), effect.getCardId(), effect.getEffectOrder(),
                        effect.getEffectScope(), effect.getEffectType(), effect.getTriggerTiming(),
                        effect.getTriggerDelay(), effect.getRemainingTriggers(), effect.getStackRule(),
                        effect.getDurationRounds(), effect.getValue(), effect.getTargetRule(), effect.getExtraData()))
                .toList();
    }

    private CardBasicResp toCardBasicResp(Cards card) {
        List<CardEffectDetailResp> effects = listCardEffects(card.getId());
        return new CardBasicResp(card.getId(), card.getCardCode(), card.getCardName(), card.getDeptId(),
                card.getDeptType(), card.getCost(), card.getCardType(), card.getDescription(), card.getImageUrl(),
                card.getComboCardId(), card.getIsUnique(), card.getStatus(),
                effects.stream().map(effect -> new cc.shturl.wa.demo.dto.resp.CardEffectResp(
                        effect.effectType(), effect.triggerTiming(), effect.effectScope(), null,
                        effect.value(), effect.value(), null, null, false, effect.triggerDelay(), effect.effectId()))
                        .toList());
    }

    private void requireCard(Long cardId) {
        if (cardsMapper.selectById(cardId) == null) {
            throw new BusinessException("卡牌不存在");
        }
    }
}
