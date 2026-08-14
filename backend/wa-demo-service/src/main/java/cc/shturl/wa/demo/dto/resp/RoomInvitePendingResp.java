package cc.shturl.wa.demo.dto.resp;

import java.time.LocalDateTime;

public record RoomInvitePendingResp(
        Long inviteId,
        Long fromUserId,
        String fromUsername,
        Long toUserId,
        LocalDateTime expiredAt,
        LocalDateTime createdAt
) {
}
