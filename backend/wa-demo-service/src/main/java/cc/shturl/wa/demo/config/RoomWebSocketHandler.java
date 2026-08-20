package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.RoomPresenceCleanupService;
import cc.shturl.wa.demo.service.RoomWebSocketSessionService;
import cc.shturl.wa.demo.service.UserPresenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {
    private final RoomWebSocketSessionService sessionService;
    private final AuthTokenSupport authTokenSupport;
    private final UserPresenceService presenceService;
    private final RoomPresenceCleanupService cleanupService;
    private final ObjectMapper objectMapper;

    public RoomWebSocketHandler(RoomWebSocketSessionService sessionService, AuthTokenSupport authTokenSupport,
                                UserPresenceService presenceService, RoomPresenceCleanupService cleanupService,
                                ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.authTokenSupport = authTokenSupport;
        this.presenceService = presenceService;
        this.cleanupService = cleanupService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveAuthenticatedUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("invalid access token"));
            return;
        }
        session.getAttributes().put("authenticatedUserId", userId);
        sessionService.bind(userId, session);
        cleanupService.handleUserConnected(userId);
        session.sendMessage(new TextMessage("{\"type\":\"ws.connected\",\"message\":\"connected\",\"heartbeatIntervalSeconds\":20,\"onlineTimeoutSeconds\":60}"));
        presenceService.broadcastPresence(userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = authenticatedUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        JsonNode payload = objectMapper.readTree(message.getPayload());
        if ("ws.heartbeat".equals(payload.path("type").asText())) {
            sessionService.heartbeat(userId, session);
            session.sendMessage(new TextMessage("{\"type\":\"ws.heartbeat.ack\",\"timestamp\":"
                    + System.currentTimeMillis() + "}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = authenticatedUserId(session);
        if (userId != null) {
            sessionService.unbind(userId, session);
            cleanupService.handleUserDisconnected(userId, "websocket_closed");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = authenticatedUserId(session);
        if (userId != null) {
            sessionService.unbind(userId, session);
            cleanupService.handleUserDisconnected(userId, "websocket_error");
        }
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Long resolveAuthenticatedUserId(WebSocketSession session) {
        String token = queryParameter(session, "accessToken");
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return authTokenSupport.requireUserIdFromAccessToken("Bearer " + token);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long authenticatedUserId(WebSocketSession session) {
        Object value = session.getAttributes().get("authenticatedUserId");
        return value instanceof Long userId ? userId : null;
    }

    private String queryParameter(WebSocketSession session, String name) {
        return Optional.ofNullable(session.getUri()).map(uri -> uri.getRawQuery()).map(query -> {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && name.equals(URLDecoder.decode(kv[0], StandardCharsets.UTF_8))) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
            return null;
        }).orElse(null);
    }
}
