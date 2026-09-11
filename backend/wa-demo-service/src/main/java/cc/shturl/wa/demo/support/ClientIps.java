package cc.shturl.wa.demo.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;

public final class ClientIps {
    private ClientIps() {
    }

    public static String fromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = firstForwarded(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            return forwarded;
        }
        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return normalize(request.getRemoteAddr());
    }

    public static String fromSession(WebSocketSession session) {
        if (session == null) {
            return null;
        }
        HttpHeaders headers = session.getHandshakeHeaders();
        String forwarded = firstForwarded(headers.getFirst("X-Forwarded-For"));
        if (forwarded != null) {
            return forwarded;
        }
        String realIp = normalize(headers.getFirst("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        InetSocketAddress remote = session.getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return null;
        }
        return normalize(remote.getAddress().getHostAddress());
    }

    static String firstForwarded(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return normalize(header.split(",")[0]);
    }

    static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || "unknown".equalsIgnoreCase(value)) {
            return null;
        }
        if (value.startsWith("[") && value.contains("]")) {
            value = value.substring(1, value.indexOf(']'));
        } else if (value.contains(".") && value.contains(":") && !value.contains("::")) {
            value = value.substring(0, value.lastIndexOf(':'));
        }
        if ("::1".equals(value)) {
            return "127.0.0.1";
        }
        if (value.startsWith("::ffff:")) {
            value = value.substring("::ffff:".length());
        }
        return value;
    }
}
