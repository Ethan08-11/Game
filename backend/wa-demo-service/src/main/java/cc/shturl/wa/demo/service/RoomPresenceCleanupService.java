package cc.shturl.wa.demo.service;

public interface RoomPresenceCleanupService {
    void handleUserConnected(Long userId);
    void handleUserDisconnected(Long userId, String reason);
    void cleanupExpiredRoomMembers();
    /** 只清已结束/已关闭房间残留 */
    void releaseStaleRooms(Long userId);
    /** 清掉非进行中对局的占位（含未开局的组队房），便于重新邀请 */
    void releaseIdleRooms(Long userId);
}
