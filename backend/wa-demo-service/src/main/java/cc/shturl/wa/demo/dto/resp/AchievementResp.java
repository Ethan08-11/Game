package cc.shturl.wa.demo.dto.resp;

public record AchievementResp(Long id, String achievementCode, String achievementName, String category,
                               String description, String conditionType, String conditionValue, String rewardType,
                               String rewardValue, Integer sortNo, Integer status) {
}
