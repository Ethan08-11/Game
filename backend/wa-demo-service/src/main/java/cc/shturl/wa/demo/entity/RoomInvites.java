package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room_invites")
public class RoomInvites extends BaseEntity {
    private Long fromUserId;
    private Long toUserId;
    private Long roomId;
    private Integer status;
    private LocalDateTime respondedAt;
    private LocalDateTime expiredAt;
}
