package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tasks")
public class Tasks extends BaseEntity {
    private String taskCode;
    private String taskName;
    private String taskType;
    private String resetType;
    private String periodScope;
    private String progressType;
    private String description;
    private String conditionType;
    private String conditionValue;
    private String rewardType;
    private String rewardValue;
    private Integer targetCount;
    private Integer sortNo;
    private Integer status;
}
