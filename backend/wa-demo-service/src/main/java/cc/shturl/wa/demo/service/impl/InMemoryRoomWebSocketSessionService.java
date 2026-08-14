package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.constant.RedisKeyConstants;
import cc.shturl.wa.demo.service.RoomWebSocketSessionService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryRoomWebSocketSessionService implements RoomWebSocketSessionService {
    private static final long ONLINE_TIMEOUT_MILLIS = 60_000L;
    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastHeartbeatAt = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;

    public InMemoryRoomWebSocketSessionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void bind(Long userId, WebSocketSession session) {
        String connectionId = UUID.randomUUID().toString();
        session.getAttributes().put("presenceConnectionId", connectionId);
        sessions.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(connectionId, session);
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
        redisTemplate.opsForZSet().remove(RedisKeyConstants.ONLINE_CONNECTIONS, member(userId, connectionId));
        redisTemplate.delete(userConnectionKey(userId, connectionId));
    }

    @Override
    public void heartbeat(Long userId, WebSocketSession session) {
        String connectionId = connectionId(session);
        if (connectionId == null || "null".equals(connectionId)) {
            connectionId = UUID.randomUUID().toString();
            session.getAttributes().put("presenceConnectionId", connectionId);
            sessions.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(connectionId, session);
        }
        long now = System.currentTimeMillis();
        String member = member(userId, connectionId);
        redisTemplate.opsForZSet().add(RedisKeyConstants.ONLINE_CONNECTIONS, member, now);
        redisTemplate.opsForValue().set(userConnectionKey(userId, connectionId), now,
                Duration.ofMillis(ONLINE_TIMEOUT_MILLIS));
        lastHeartbeatAt.put(userId, now);
        cleanupExpired();
    }

    @Override
    public boolean isOnline(Long userId) {
        cleanupExpired();
        // 本机仍有存活 WebSocket 时直接视为在线，避免 Redis KEYS/TTL 短暂不一致导致误判离线
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            for (WebSocketSession session : userSessions.values()) {
                if (session != null && session.isOpen()) {
                    return true;
                }
            }
        }
        if (hasFreshRedisConnection(userId)) {
            return true;
        }
        Set<String> members = redisTemplate.keys(RedisKeyConstants.USER_CONNECTIONS_PREFIX + userId + ":*");
        return members != null && !members.isEmpty();
    }

    @Override
    public boolean isStale(Long userId, long timeoutMillis) {
        Long last = lastHeartbeatAt.get(userId);
        return last == null || System.currentTimeMillis() - last > timeoutMillis;
    }

    @Override
    public void sendToUser(Long userId, String message) throws IOException {
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    private boolean hasFreshRedisConnection(Long userId) {
        long minScore = System.currentTimeMillis() - ONLINE_TIMEOUT_MILLIS;
        Set<Object> members = redisTemplate.opsForZSet()
                .rangeByScore(RedisKeyConstants.ONLINE_CONNECTIONS, minScore, Double.POSITIVE_INFINITY);
        if (members == null || members.isEmpty()) {
            return false;
        }
        String prefix = userId + ":";
        for (Object member : members) {
            if (member != null && String.valueOf(member).startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void cleanupExpired() {
        redisTemplate.opsForZSet().removeRangeByScore(RedisKeyConstants.ONLINE_CONNECTIONS, 0,
                System.currentTimeMillis() - ONLINE_TIMEOUT_MILLIS);
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
