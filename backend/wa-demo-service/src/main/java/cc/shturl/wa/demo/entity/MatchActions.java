package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("match_actions")
public class MatchActions extends BaseEntity {
    private Long matchId;
    private Long roundId;
    private String actorType;
    private Long actorUserId;
    private String actionType;
    private Long cardId;
    private Long targetUserId;
    private Integer beforeValue;
    private Integer afterValue;
    private Integer deltaValue;
    private String extraData;
}

