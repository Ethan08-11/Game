package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_tasks")
public class UserTask extends BaseEntity {
    private Long userId;
    private Long taskId;
    private String periodKey;
    private Integer progressValue;
    private Integer targetValue;
    private String extraData;
    private Integer status;
    private java.time.LocalDateTime completedAt;
    private java.time.LocalDateTime claimedAt;
}
