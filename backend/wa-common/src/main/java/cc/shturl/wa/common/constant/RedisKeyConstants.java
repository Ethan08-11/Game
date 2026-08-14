package cc.shturl.wa.common.constant;

/**
 * Redis 缓存 Key 前缀常量
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /** 缓存 Key 前缀，格式: wa:demo: */
    public static final String CACHE_PREFIX = "wa:demo:";

    /** 分布式锁 Key 前缀 */
    public static final String LOCK_PREFIX = "wa:demo:lock:";

    /** 示例缓存 Key */
    public static final String EXAMPLE_CACHE = CACHE_PREFIX + "example:";

    /** WebSocket 在线连接 ZSet，member 为 userId:connectionId，score 为最后心跳毫秒时间戳 */
    public static final String ONLINE_CONNECTIONS = CACHE_PREFIX + "presence:connections";

    /** 用户 WebSocket 连接集合前缀，Key 格式为前缀 + userId */
    public static final String USER_CONNECTIONS_PREFIX = CACHE_PREFIX + "presence:user:";
}
