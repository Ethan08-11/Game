package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("matches")
public class Matches extends BaseEntity {
    private String matchCode;
    private Long roomId;
    private Long customerTypeId;
    private Long bullyId;
    private String bossName;
    private Integer bossSatisfactionTarget;
    private Integer bossInitialSatisfaction;
    private Integer bossFinalSatisfaction;
    private Integer status;
    private String phase;
    private Integer currentRound;
    private Integer bossMaxHp;
    private Integer bossCurrentHp;
    private Integer bossBaseAttack;
    private Integer bossCurrentAttack;
    private Integer bossCurrentShield;
    private String bullyRoundData;
    private Integer winnerType;
    private Long version;
    private Integer durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}

