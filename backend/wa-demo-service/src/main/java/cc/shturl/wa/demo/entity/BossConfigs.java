package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("boss_configs")
public class BossConfigs extends BaseEntity {
    private String bossCode;
    private String bossName;
    private String description;
    private Integer baseSatisfactionTargetMin;
    private Integer baseSatisfactionTargetMax;
    private Integer initialSatisfaction;
    private Integer initialRage;
    private String bossType;
    private String portraitUrl;
    private String effectData;
    private Integer status;
}

