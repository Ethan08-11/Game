package cc.shturl.wa.demo.dto.resp;

public record MatchCardResp(
        Long instanceId,
        Long cardId,
        String cardCode,
        String cardName,
        String deptType,
        Integer cost,
        String cardType,
        String description,
        String imageUrl,
        String zone,
        Integer deckOrder,
        Integer drawnRound,
        Boolean requiresPlayerTarget
) {
}
