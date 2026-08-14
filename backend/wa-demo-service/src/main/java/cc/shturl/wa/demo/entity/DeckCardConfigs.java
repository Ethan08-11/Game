package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("deck_card_configs")
public class DeckCardConfigs extends BaseEntity {
    private String deptType;
    private Long cardId;
    private Integer cardCount;
    private Integer sortNo;
    private Integer status;
}
