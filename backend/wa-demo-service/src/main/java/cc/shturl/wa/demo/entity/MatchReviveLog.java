package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("match_revive_logs")
public class MatchReviveLog extends BaseEntity {
    private Long matchId;
    private Integer roundNo;
    private Long userId;
    private Integer beforeHp;
    private Integer afterHp;
    private Integer status;
    private String adPlatform;
    private String adRequestId;
    private String adCallbackRaw;
    private Integer verifyStatus;
    private String reviveReason;
}
