package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("achievement_defs")
public class AchievementDefs extends BaseEntity {
    private String achievementCode;
    private String achievementName;
    private String category;
    private String description;
    private String conditionType;
    private String conditionValue;
    private String rewardType;
    private String rewardValue;
    private Integer sortNo;
    private Integer status;
}

