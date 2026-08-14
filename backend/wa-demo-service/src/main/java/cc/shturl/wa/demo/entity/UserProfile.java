package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_profiles")
public class UserProfile extends BaseEntity {
    private Long userId;
    private String displayName;
    private String signature;
    private Integer gender;
    private Integer level;
    private Integer exp;
    private Integer winCount;
    private Integer loseCount;
    private Integer drawCount;
    private Long money;
}

