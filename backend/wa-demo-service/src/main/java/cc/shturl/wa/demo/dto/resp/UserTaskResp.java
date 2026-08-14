package cc.shturl.wa.demo.dto.resp;

public record UserTaskResp(Long id, Long taskId, String taskCode, String taskName, String taskType,
                           String resetType, String periodScope, String progressType,
                           String description, String conditionType, String conditionValue,
                           String rewardType, String rewardValue, Integer targetCount,
                           Integer progressValue, Integer status, String periodKey,
                           java.time.LocalDateTime completedAt, java.time.LocalDateTime claimedAt) {
}
