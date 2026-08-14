package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room_members")
public class RoomMembers extends BaseEntity {
    private Long roomId;
    private Long userId;
    private Integer seatNo;
    private String deptType;
    private Integer readyStatus;
    private Integer onlineStatus;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}

