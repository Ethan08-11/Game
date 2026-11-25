package cc.shturl.wa.demo.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 下一张伤害翻倍和下一张防御翻倍互不覆盖，只替换同一种。 */
public final class NextCardMultiplierRules {

    private static final Pattern TYPE_BLOCK = Pattern.compile("\"multiplyEffectTypes\"\\s*:\\s*\\[([^\\]]*)]");
    private static final Pattern TYPE_NAME = Pattern.compile("\"([A-Z_]+)\"");

    private NextCardMultiplierRules() {
    }

    public static Set<String> effectTypes(String extraData) {
        Set<String> types = new LinkedHashSet<>();
        if (extraData == null || extraData.isBlank()) {
            return types;
        }
        Matcher block = TYPE_BLOCK.matcher(extraData);
        if (!block.find()) {
            return types;
        }
        Matcher names = TYPE_NAME.matcher(block.group(1));
        while (names.find()) {
            types.add(names.group(1));
        }
        return types;
    }

    /** 没有列出效果类型时，这一次翻倍作用于整张牌，因此会替换已有的下一张翻倍。 */
    public static boolean replaces(Set<String> incoming, Set<String> existing) {
        if (incoming == null || incoming.isEmpty() || existing == null || existing.isEmpty()) {
            return true;
        }
        for (String type : incoming) {
            if (existing.contains(type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean covers(Set<String> types, String effectType) {
        return types == null || types.isEmpty() || types.contains(effectType);
    }
}
