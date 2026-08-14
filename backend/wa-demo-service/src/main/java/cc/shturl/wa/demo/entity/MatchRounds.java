package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("match_rounds")
public class MatchRounds extends BaseEntity {
    private Long matchId;
    private Integer roundNo;
    private Long firstPlayerUserId;
    private Long chosenByUserId;
    private Integer roundStatus;
    private String phase;
    private Integer bossAttack;
    private Integer customerTriggered;
    private String customerEffectType;
    private Integer customerEffectValue;
    private Integer bossRageValue;
    private Integer satisfactionDelta;
    private Integer fundsPerPlayer;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}

