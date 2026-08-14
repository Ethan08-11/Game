package cc.shturl.wa.demo.dto.resp;

import java.util.List;

public record CardBasicResp(
        Long cardId,
        String cardCode,
        String cardName,
        Long deptId,
        String deptType,
        Integer cost,
        String cardType,
        String description,
        String imageUrl,
        Long comboCardId,
        Integer isUnique,
        Integer status,
        List<CardEffectResp> effects
) {
}
