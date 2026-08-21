package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.constant.RedisKeyConstants;
import cc.shturl.wa.demo.service.RoomWebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryRoomWebSocketSessionService implements RoomWebSocketSessionService {
    private static final Logger log = LoggerFactory.getLogger(InMemoryRoomWebSocketSessionService.class);
    private static final long ONLINE_TIMEOUT_MILLIS = 60_000L;
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_LIMIT = 512 * 1024;
    private static final String ATTR_OUTBOUND = "outboundSession";

    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastHeartbeatAt = new ConcurrentHashMap<>();
    private final Map<String, Object> sendLocks = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;

    public InMemoryRoomWebSocketSessionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void bind(Long userId, WebSocketSession session) {
        String connectionId = UUID.randomUUID().toString();
        session.getAttributes().put("presenceConnectionId", connectionId);
        WebSocketSession outbound = wrapOutbound(session);
        sessions.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(connectionId, outbound);
        heartbeat(userId, session);
    }

    @Override
    public void unbind(Long userId, WebSocketSession session) {
        String connectionId = connectionId(session);
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(connectionId);
            if (userSessions.isEmpty()) {
                sessions.remove(userId, userSessions);
                lastHeartbeatAt.remove(userId);
            }
        }
        sendLocks.remove(session.getId());
        redisTemplate.opsForZSet().remove(RedisKeyConstants.ONLINE_CONNECTIONS, member(userId, connectionId));
        redisTemplate.delete(userConnectionKey(userId, connectionId));
    }

    @Override
    public void heartbeat(Long userId, WebSocketSession session) {
        String connectionId = connectionId(session);
        if (connectionId == null || "null".equals(connectionId)) {
            connectionId = UUID.randomUUID().toString();
            session.getAttributes().put("presenceConnectionId", connectionId);
            sessions.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(connectionId, wrapOutbound(session));
        }
        long now = System.currentTimeMillis();
        String member = member(userId, connectionId);
        redisTemplate.opsForZSet().add(RedisKeyConstants.ONLINE_CONNECTIONS, member, now);
        redisTemplate.opsForValue().set(userConnectionKey(userId, connectionId), now,
                Duration.ofMillis(ONLINE_TIMEOUT_MILLIS));
        lastHeartbeatAt.put(userId, now);
    }

    @Override
    public boolean isOnline(Long userId) {
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            for (WebSocketSession session : userSessions.values()) {
                if (session != null && session.isOpen()) {
                    return true;
                }
            }
        }
        Long last = lastHeartbeatAt.get(userId);
        return last != null && System.currentTimeMillis() - last < ONLINE_TIMEOUT_MILLIS;
    }

    @Override
    public boolean isStale(Long userId, long timeoutMillis) {
        Long last = lastHeartbeatAt.get(userId);
        return last == null || System.currentTimeMillis() - last > timeoutMillis;
    }

    @Override
    public void sendToUser(Long userId, String message) {
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        TextMessage payload = new TextMessage(message);
        for (WebSocketSession session : userSessions.values()) {
            sendSafely(session, payload);
        }
    }

    @Override
    public void sendText(WebSocketSession session, String message) {
        sendSafely(resolveOutbound(session), new TextMessage(message));
    }

    private WebSocketSession wrapOutbound(WebSocketSession session) {
        Object existing = session.getAttributes().get(ATTR_OUTBOUND);
        if (existing instanceof WebSocketSession outbound) {
            return outbound;
        }
        ConcurrentWebSocketSessionDecorator outbound = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT, OverflowStrategy.DROP);
        session.getAttributes().put(ATTR_OUTBOUND, outbound);
        return outbound;
    }

    private WebSocketSession resolveOutbound(WebSocketSession session) {
        if (session == null) {
            return null;
        }
        Object outbound = session.getAttributes().get(ATTR_OUTBOUND);
        if (outbound instanceof WebSocketSession wrapped) {
            return wrapped;
        }
        return wrapOutbound(session);
    }

    private void sendSafely(WebSocketSession session, TextMessage payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        Object lock = sendLocks.computeIfAbsent(session.getId(), id -> new Object());
        synchronized (lock) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendMessage(payload);
            } catch (IllegalStateException | IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Skip websocket send sessionId={}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    private String connectionId(WebSocketSession session) {
        Object value = session.getAttributes().get("presenceConnectionId");
        return value == null ? null : String.valueOf(value);
    }

    private String member(Long userId, String connectionId) {
        return userId + ":" + connectionId;
    }

    private String userConnectionKey(Long userId, String connectionId) {
        return RedisKeyConstants.USER_CONNECTIONS_PREFIX + userId + ":" + connectionId;
    }
}
