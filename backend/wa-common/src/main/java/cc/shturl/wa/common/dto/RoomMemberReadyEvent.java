package cc.shturl.wa.common.dto;

public record RoomMemberReadyEvent(String eventId, Long roomId, Long userId,
                                   Integer readyStatus, Boolean allReady, Long timestamp) {
}
