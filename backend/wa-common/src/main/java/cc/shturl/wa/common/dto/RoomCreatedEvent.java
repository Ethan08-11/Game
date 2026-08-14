package cc.shturl.wa.common.dto;

public record RoomCreatedEvent(String eventId, Long roomId, String roomCode, Long hostUserId,
                               Long memberUserId, Long timestamp) {
}
