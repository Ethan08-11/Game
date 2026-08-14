package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_rooms")
public class GameRooms extends BaseEntity {
    private String roomCode;
    private Long hostUserId;
    private Integer status;
    private Integer playerCount;
    private Integer maxPlayers;
    private Long matchId;
    private LocalDateTime closedAt;
}

