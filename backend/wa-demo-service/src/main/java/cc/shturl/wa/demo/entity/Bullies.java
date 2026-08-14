package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bullies")
public class Bullies extends BaseEntity {
    private String bullyCode;
    private String bullyName;
    private String description;
    private Integer hp;
    private Integer attackPower;
    private Integer defenseValue;
    private Integer speedValue;
    private Integer rageValue;
    private String skillData;
    private Integer status;
}
