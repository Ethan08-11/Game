package cc.shturl.wa.demo.service.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 顾客挂的待生效记录。库里的 JSON 读出来会在冒号后带空格，不能按紧凑字符串比对。
 */
public final class CustomerHookRevert {

    private static final Pattern FROM_CUSTOMER = Pattern.compile("\"fromCustomer\"\\s*:\\s*true");
    private static final Pattern GRANTED_ROUND = Pattern.compile("\"grantedRound\"\\s*:\\s*(\\d+)");

    private CustomerHookRevert() {
    }

    public static boolean fromRound(String extra, int grantedRound) {
        if (extra == null || extra.isBlank() || grantedRound < 0) {
            return false;
        }
        if (!FROM_CUSTOMER.matcher(extra).find()) {
            return false;
        }
        Matcher round = GRANTED_ROUND.matcher(extra);
        if (!round.find()) {
            return false;
        }
        try {
            return Integer.parseInt(round.group(1)) == grantedRound;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
