package cc.shturl.wa.demo.service;

import java.util.Set;

/** 内部版沿用现在的顾客和随机规则。对外版用另一套顾客，开局由玩家选定。 */
public final class GameEdition {
    public static final String INTERNAL = "INTERNAL";
    public static final String PUBLIC = "PUBLIC";

    public static final Set<String> PUBLIC_CUSTOMER_CODES = Set.of(
            "PUBLIC_STEADY",
            "PUBLIC_PAIR",
            "PUBLIC_DRAW",
            "PUBLIC_SHIELD");

    private GameEdition() {
    }

    public static String normalize(String audience) {
        if (audience != null && PUBLIC.equalsIgnoreCase(audience.trim())) {
            return PUBLIC;
        }
        return INTERNAL;
    }

    public static boolean isPublic(String audience) {
        return PUBLIC.equals(normalize(audience));
    }

    public static boolean isPublicCustomer(String code) {
        return code != null && PUBLIC_CUSTOMER_CODES.contains(code.trim());
    }
}
