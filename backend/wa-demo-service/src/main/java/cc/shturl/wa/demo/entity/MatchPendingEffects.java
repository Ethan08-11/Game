package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("match_pending_effects")
public class MatchPendingEffects extends BaseEntity {
    private Long matchId;
    private Long matchPlayerId;
    private Long sourceUserId;
    private Long sourceCardInstanceId;
    private String effectType;
    private String targetType;
    private Long targetUserId;
    private Integer effectValue;
    private Integer triggerRound;
    private Integer remainingTriggers;
    private String status;
    private String extraData;
}
