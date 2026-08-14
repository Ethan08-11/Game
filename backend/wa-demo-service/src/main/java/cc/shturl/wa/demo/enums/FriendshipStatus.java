package cc.shturl.wa.demo.enums;

import java.util.Arrays;

/**
 * 好友关系状态枚举
 * <p>
 * 与数据库 friendships.status 字段对应：
 * <ul>
 *     <li>{@link #PENDING}   —— 待确认（对方尚未处理）</li>
 *     <li>{@link #ACCEPTED}  —— 已接受（正式好友）</li>
 *     <li>{@link #BLOCKED}   —— 已拉黑</li>
 * </ul>
 */
public enum FriendshipStatus {

    PENDING(0),
    ACCEPTED(1),
    BLOCKED(2);

    private final int code;

    FriendshipStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据状态码解析枚举，未知值返回 {@code null}。
     */
    public static FriendshipStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(null);
    }
}
