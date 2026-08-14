package cc.shturl.wa.demo.service;

public interface RoomPresenceCleanupService {
    void handleUserDisconnected(Long userId, String reason);
    void cleanupExpiredRoomMembers();
}
