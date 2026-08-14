package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.constant.RedisKeyConstants;
import cc.shturl.wa.demo.dto.resp.ExampleResp;
import cc.shturl.wa.demo.mq.message.ExampleCreatedMessage;
import cc.shturl.wa.demo.service.ExampleCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 示例数据缓存服务实现
 * <p>
 * MQ 消费者异步调用，将新创建的数据预热到 Redis，加速后续查询
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleCacheServiceImpl implements ExampleCacheService {

    private static final long CACHE_TTL_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void warmCache(ExampleCreatedMessage message) {
        ExampleResp resp = new ExampleResp();
        resp.setId(message.getExampleId());
        resp.setName(message.getName());
        resp.setDescription(message.getDescription());
        resp.setCreateTime(message.getOccurredAt());

        String cacheKey = RedisKeyConstants.EXAMPLE_CACHE + message.getExampleId();
        redisTemplate.opsForValue().set(cacheKey, resp, CACHE_TTL_HOURS, TimeUnit.HOURS);

        log.info("[缓存预热] exampleId={}, cacheKey={}, ttl={}h",
                message.getExampleId(), cacheKey, CACHE_TTL_HOURS);
    }

    @Override
    public ExampleResp getFromCache(Long exampleId) {
        String cacheKey = RedisKeyConstants.EXAMPLE_CACHE + exampleId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ExampleResp resp) {
            log.debug("[缓存命中] exampleId={}", exampleId);
            return resp;
        }
        return null;
    }
}
