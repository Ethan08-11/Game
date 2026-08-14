package cc.shturl.wa.common.dto;

public record RoomInviteAcceptedEvent(String eventId, Long fromUserId, Long toUserId, Long inviteId,
                                     Long roomId, String roomCode, Long timestamp) {
}
