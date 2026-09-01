package cc.shturl.wa.demo.dto.resp;

public record CustomerInfoResp(Long customerTypeId, String customerCode, String customerName, String description,
                               String imageUrl, String effectType, Integer effectValue, Integer triggerChance,
                               Integer selectionWeight, Integer status,
                               String bullyCode, String bullyName, String bullyDescription,
                               String bullySkillSummary, Integer bullySkillChance) {
}
