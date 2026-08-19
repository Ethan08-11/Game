package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cards")
public class Cards extends BaseEntity {
    private String cardCode;
    private String cardName;
    private Long deptId;
    private String deptType;
    private Integer cost;
    private String cardType;
    private String description;
    private String imageUrl;
    private Long comboCardId;
    private Integer isUnique;
    private Integer requireUnlock;
    private Integer status;
}

