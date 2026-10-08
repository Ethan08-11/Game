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

    /**
     * 同一公网 IPv4，或同一 IPv6 /64。临时 IPv6 每个窗口后 64 位不同，只比整段地址会把同一台电脑放过去。
     */
    public static boolean sameNetwork(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }
        String leftPrefix = ipv6Prefix64(left);
        String rightPrefix = ipv6Prefix64(right);
        return leftPrefix != null && leftPrefix.equals(rightPrefix);
    }

    static String ipv6Prefix64(String ip) {
        int[] groups = expandIpv6(ip);
        if (groups == null) {
            return null;
        }
        return groups[0] + ":" + groups[1] + ":" + groups[2] + ":" + groups[3];
    }

    private static int[] expandIpv6(String ip) {
        if (ip.indexOf(':') < 0 || ip.indexOf('.') >= 0) {
            return null;
        }
        int empty = ip.indexOf("::");
        String[] head;
        String[] tail;
        if (empty >= 0) {
            if (ip.indexOf("::", empty + 2) >= 0) {
                return null;
            }
            String left = ip.substring(0, empty);
            String right = ip.substring(empty + 2);
            head = left.isEmpty() ? new String[0] : left.split(":", -1);
            tail = right.isEmpty() ? new String[0] : right.split(":", -1);
        } else {
            head = ip.split(":", -1);
            tail = new String[0];
        }
        if (empty < 0 && head.length != 8) {
            return null;
        }
        if (head.length + tail.length > 8) {
            return null;
        }
        int[] groups = new int[8];
        for (int i = 0; i < head.length; i++) {
            Integer parsed = parseGroup(head[i]);
            if (parsed == null) {
                return null;
            }
            groups[i] = parsed;
        }
        for (int i = 0; i < tail.length; i++) {
            Integer parsed = parseGroup(tail[i]);
            if (parsed == null) {
                return null;
            }
            groups[8 - tail.length + i] = parsed;
        }
        return groups;
    }

    private static Integer parseGroup(String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > 4) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw, 16);
            return value >= 0 && value <= 0xFFFF ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
