package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("card_effects")
public class CardEffects extends BaseEntity {
    private Long cardId;
    private Integer effectOrder;
    private String effectScope;
    private String effectType;
    private String triggerTiming;
    private Integer triggerDelay;
    private Integer remainingTriggers;
    private String stackRule;
    private Integer durationRounds;
    /** MySQL 保留字，必须显式加反引号，否则可能读不到数值导致辅助卡“无效果” */
    @TableField("`value`")
    private Integer value;
    private String targetRule;
    private String extraData;
}

