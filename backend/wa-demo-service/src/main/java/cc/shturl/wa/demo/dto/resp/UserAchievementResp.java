package cc.shturl.wa.demo.dto.resp;

import java.time.LocalDateTime;

public record UserAchievementResp(Long id, Long userId, Long achievementId, String achievementCode,
                                  String achievementName, Integer progressValue, Integer unlockStatus,
                                  LocalDateTime unlockedAt, Integer claimedStatus, LocalDateTime claimedAt) {
}
