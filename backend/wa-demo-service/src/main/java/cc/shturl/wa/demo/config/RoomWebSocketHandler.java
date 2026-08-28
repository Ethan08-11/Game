package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.MatchChatService;
import cc.shturl.wa.demo.service.RoomPresenceCleanupService;
import cc.shturl.wa.demo.service.RoomWebSocketSessionService;
import cc.shturl.wa.demo.service.UserPresenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(RoomWebSocketHandler.class);

    private final RoomWebSocketSessionService sessionService;
    private final AuthTokenSupport authTokenSupport;
    private final UserPresenceService presenceService;
    private final RoomPresenceCleanupService cleanupService;
    private final MatchChatService matchChatService;
    private final ObjectMapper objectMapper;

    public RoomWebSocketHandler(RoomWebSocketSessionService sessionService, AuthTokenSupport authTokenSupport,
                                UserPresenceService presenceService, RoomPresenceCleanupService cleanupService,
                                MatchChatService matchChatService, ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.authTokenSupport = authTokenSupport;
        this.presenceService = presenceService;
        this.cleanupService = cleanupService;
        this.matchChatService = matchChatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveAuthenticatedUserId(session);
        if (userId == null) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("invalid access token"));
            return;
        }
        session.getAttributes().put("authenticatedUserId", userId);
        sessionService.bind(userId, session);
        cleanupService.handleUserConnected(userId);
        sessionService.sendText(session,
                "{\"type\":\"ws.connected\",\"message\":\"connected\",\"heartbeatIntervalSeconds\":20,\"onlineTimeoutSeconds\":60}");
        presenceService.broadcastPresence(userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = authenticatedUserId(session);
        if (userId == null) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText();
            if ("ws.heartbeat".equals(type)) {
                sessionService.heartbeat(userId, session);
                sessionService.sendText(session,
                        "{\"type\":\"ws.heartbeat.ack\",\"timestamp\":" + System.currentTimeMillis() + "}");
            } else if ("match.chat".equals(type)) {
                matchChatService.handleChat(userId, payload);
            }
        } catch (Exception e) {
            log.debug("Ignore websocket text from user {}: {}", userId, e.getMessage());
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
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        boolean alreadyClosed = session != null && !session.isOpen();
        boolean benign = alreadyClosed || isBenignTransportError(exception);
        if (benign) {
            log.debug("Websocket disconnected: {}", describeTransportError(exception));
        } else {
            log.warn("Websocket transport error: {}", describeTransportError(exception));
        }
        Long userId = authenticatedUserId(session);
        if (userId != null) {
            sessionService.unbind(userId, session);
            cleanupService.handleUserDisconnected(userId, benign ? "websocket_closed" : "websocket_error");
        }
        if (!benign) {
            closeQuietly(session, CloseStatus.SERVER_ERROR);
        } else {
            closeQuietly(session, CloseStatus.NORMAL);
        }
    }

    private static boolean isBenignTransportError(Throwable exception) {
        if (exception == null) {
            return true;
        }
        Throwable current = exception;
        while (current != null) {
            if (current instanceof EOFException
                    || current instanceof ClosedChannelException
                    || current instanceof java.io.UncheckedIOException) {
                return true;
            }
            String name = current.getClass().getName();
            String message = current.getMessage() == null ? "" : current.getMessage();
            if (name.contains("EOFException")
                    || name.contains("EofException")
                    || name.contains("ClosedChannelException")
                    || name.contains("ClientAbortException")
                    || name.contains("CloseNowException")
                    || message.contains("TEXT_PARTIAL_WRITING")
                    || message.contains("has been closed")
                    || message.contains("Broken pipe")
                    || message.contains("Connection reset")
                    || message.contains("Connection timed out")
                    || message.contains("An established connection was aborted")
                    || message.contains("远程主机强迫关闭")
                    || message.contains("你的主机中的软件中止")) {
                return true;
            }
            current = current.getCause();
        }
        // 客户端直接掐 TCP 时，Tomcat 常抛出 message 为空的 IOException
        return exception instanceof IOException
                && (exception.getMessage() == null || exception.getMessage().isBlank());
    }

    private static String describeTransportError(Throwable exception) {
        if (exception == null) {
            return "empty";
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(status);
        } catch (Exception ignored) {
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
