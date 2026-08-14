package cc.shturl.wa.common.dto;

public record RoomMemberDepartmentChangedEvent(String eventId, Long roomId, Long userId,
                                               String deptType, Long timestamp) {
}
