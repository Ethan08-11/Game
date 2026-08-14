package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.ExampleResp;
import cc.shturl.wa.demo.mq.message.ExampleCreatedMessage;

/**
 * 示例数据缓存服务
 */
public interface ExampleCacheService {

    /**
     * 缓存预热：将新创建的示例数据写入 Redis
     */
    void warmCache(ExampleCreatedMessage message);

    /**
     * 从缓存读取示例数据
     */
    ExampleResp getFromCache(Long exampleId);
}
