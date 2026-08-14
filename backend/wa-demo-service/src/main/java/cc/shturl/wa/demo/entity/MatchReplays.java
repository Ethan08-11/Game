package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("match_replays")
public class MatchReplays extends BaseEntity {
    private Long matchId;
    private String replayType;
    private String replayUrl;
    private String replayHash;
    private Integer dataSize;
    private String version;
}

