package cc.shturl.wa.demo.common;

import cc.shturl.wa.common.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具类（骨架示例）
 */
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /**
     * 尝试获取锁
     *
     * @param lockKey   锁 Key（不含前缀）
     * @param expireSec 过期时间（秒）
     * @return 锁标识（解锁时需传入），获取失败返回 null
     */
    public String tryLock(String lockKey, long expireSec) {
        String key = RedisKeyConstants.LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, lockValue, Duration.ofSeconds(expireSec));
        return Boolean.TRUE.equals(success) ? lockValue : null;
    }

    /**
     * 释放锁
     *
     * @param lockKey   锁 Key（不含前缀）
     * @param lockValue 加锁时返回的锁标识
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey, String lockValue) {
        String key = RedisKeyConstants.LOCK_PREFIX + lockKey;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), lockValue);
        return result != null && result > 0;
    }

    /**
     * 简易缓存读写示例
     */
    public void setCache(String cacheKey, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(RedisKeyConstants.EXAMPLE_CACHE + cacheKey, value, timeout, unit);
    }

    public Object getCache(String cacheKey) {
        return redisTemplate.opsForValue().get(RedisKeyConstants.EXAMPLE_CACHE + cacheKey);
    }
}
