package cc.shturl.wa.demo.service;

public interface RoomNotificationService {
    void notifyUser(Long userId, Object payload);
    void notifyUsers(Long userId1, Long userId2, Object payload);
}
