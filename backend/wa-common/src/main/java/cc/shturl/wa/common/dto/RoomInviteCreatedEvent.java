package cc.shturl.wa.common.dto;

public record RoomInviteCreatedEvent(String eventId, Long fromUserId, Long toUserId, Long inviteId,
                                     String fromUsername, String toUsername, Long timestamp) {
}
