package cc.shturl.wa.demo.support;

import java.util.Locale;
import java.util.Set;

/**
 * 测试账号：卡牌全解锁，且不受同网络组队限制。
 */
public final class TestAccounts {
    private static final Set<String> USERNAMES = Set.of("ethan", "duane", "amy");

    private TestAccounts() {
    }

    public static boolean isTester(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return USERNAMES.contains(username.trim().toLowerCase(Locale.ROOT));
    }
}
