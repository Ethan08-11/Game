package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("match_cards")
public class MatchCards extends BaseEntity {
    private Long matchId;
    private Long matchPlayerId;
    private Long userId;
    private Long cardId;
    private String zone;
    private Integer deckOrder;
    private Integer drawnRound;
    private Integer discardedRound;
    private Integer version;
}
