package cc.shturl.wa.demo.service;

import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public interface RoomWebSocketSessionService {
    void bind(Long userId, WebSocketSession session);
    void unbind(Long userId, WebSocketSession session);
    void heartbeat(Long userId, WebSocketSession session);
    boolean isOnline(Long userId);
    boolean isStale(Long userId, long timeoutMillis);
    void sendToUser(Long userId, String message);
    void sendText(WebSocketSession session, String message);
}
