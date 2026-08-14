package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_achievements")
public class UserAchievements extends BaseEntity {
    private Long userId;
    private Long achievementId;
    private Integer progressValue;
    private Integer unlockStatus;
    private LocalDateTime unlockedAt;
    private Integer claimedStatus;
    private LocalDateTime claimedAt;
}

