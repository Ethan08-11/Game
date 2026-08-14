package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("card_combo_rules")
public class CardComboRules extends BaseEntity {
    private Long cardId;
    private Long comboCardId;
    private String comboName;
    private String effectDesc;
    private String bonusType;
    private Integer bonusValue;
    private Integer status;
}

