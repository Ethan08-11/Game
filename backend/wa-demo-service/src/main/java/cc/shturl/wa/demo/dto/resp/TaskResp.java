package cc.shturl.wa.demo.dto.resp;

public record TaskResp(Long id, String taskCode, String taskName, String taskType, String resetType,
                       String periodScope, String progressType, String description,
                       String conditionType, String conditionValue, String rewardType, String rewardValue,
                       Integer targetCount, Integer sortNo, Integer status) {
}
