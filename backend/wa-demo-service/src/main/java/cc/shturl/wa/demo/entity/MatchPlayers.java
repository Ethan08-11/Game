package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("match_players")
public class MatchPlayers extends BaseEntity {
    private Long matchId;
    private Long userId;
    private Integer seatNo;
    private String deptType;
    private Integer maxHp;
    private Integer currentHp;
    private Integer shield;
    private Integer baseActionPoints;
    private Integer actionPoints;
    private Integer endedTurn;
    private String playerStatus;
    private Integer initialConfidence;
    private Integer finalConfidence;
    private Integer initialFunds;
    private Integer totalFundsUsed;
    private Integer cardsPlayedCount;
    private Integer damageDealt;
    private Integer damageTaken;
    private Integer healingDone;
    private Integer shieldGranted;
    private Integer resultType;
    private Integer reviveCount;
    private Integer reviveLimit;
    private LocalDateTime lastReviveAt;
    private Integer reviveStatus;
}

