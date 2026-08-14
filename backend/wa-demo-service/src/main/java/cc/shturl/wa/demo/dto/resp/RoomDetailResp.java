package cc.shturl.wa.demo.dto.resp;

import java.time.LocalDateTime;
import java.util.List;

public record RoomDetailResp(Long id, String roomCode, Long hostUserId, Integer status, Integer playerCount,
                             Integer maxPlayers, Long matchId, LocalDateTime closedAt, List<RoomMemberResp> members) {
}
