package cc.shturl.wa.common.dto;

public record RoomInviteRejectedEvent(String eventId, Long fromUserId, Long toUserId, Long inviteId,
                                     Long timestamp) {
}
