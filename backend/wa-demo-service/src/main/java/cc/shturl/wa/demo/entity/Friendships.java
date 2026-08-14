package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("friendships")
public class Friendships extends BaseEntity {
    private Long userId;
    private Long friendId;
    private Integer status;
    private String remarkName;
}

