package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("friend_remark")
public class FriendRemark extends BaseEntity {
    private Long userId;
    private Long friendUserId;
    private String remarkName;
}
