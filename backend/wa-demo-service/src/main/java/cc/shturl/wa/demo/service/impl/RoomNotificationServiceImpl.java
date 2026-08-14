package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.service.RoomNotificationService;
import cc.shturl.wa.demo.service.RoomWebSocketSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomNotificationServiceImpl implements RoomNotificationService {
    private final RoomWebSocketSessionService sessionService;
    private final ObjectMapper objectMapper;

    @Override
    public void notifyUser(Long userId, Object payload) {
        try {
            sessionService.sendToUser(userId, objectMapper.writeValueAsString(payload));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void notifyUsers(Long userId1, Long userId2, Object payload) {
        notifyUser(userId1, payload);
        notifyUser(userId2, payload);
    }
}
