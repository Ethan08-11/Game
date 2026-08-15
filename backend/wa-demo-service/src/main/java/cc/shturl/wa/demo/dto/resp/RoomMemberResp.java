package cc.shturl.wa.demo.dto.resp;

public record RoomMemberResp(Long id, Long roomId, Long userId, Integer seatNo, String deptType,
                             Integer readyStatus, Integer onlineStatus, String username, String displayName) {
}
