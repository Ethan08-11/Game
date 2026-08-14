package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_types")
public class CustomerTypes extends BaseEntity {
    private String customerCode;
    private String customerName;
    private String description;
    private String imageUrl;
    private String effectType;
    private Integer effectValue;
    private Integer triggerChance;
    private Integer selectionWeight;
    private Integer status;
    private Integer sortNo;
}
