package cc.shturl.wa.demo.service;

import java.util.Locale;

/** 对外版镜像账号名：HIH 加内部版名字，例如 HIH Ethan。 */
public final class PublicAccountNames {
    private PublicAccountNames() {
    }

    public static String mirrorName(String username, String displayName) {
        String source = username == null || username.isBlank() ? displayName : username;
        String pretty = titleCase(source);
        if (pretty.isEmpty()) {
            return "";
        }
        if (pretty.regionMatches(true, 0, "HIH ", 0, 4)) {
            return pretty;
        }
        return "HIH " + pretty;
    }

    public static String titleCase(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String part : trimmed.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            if (part.equalsIgnoreCase("hih")) {
                out.append("HIH");
                continue;
            }
            String lower = part.toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                out.append(lower.substring(1));
            }
        }
        return out.toString();
    }
}
