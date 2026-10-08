package cc.shturl.wa.demo.support;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ClientDevices {
    private static final Pattern DEVICE_ID = Pattern.compile("^[A-Za-z0-9:._-]{8,128}$");

    private ClientDevices() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (!DEVICE_ID.matcher(value).matches()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
