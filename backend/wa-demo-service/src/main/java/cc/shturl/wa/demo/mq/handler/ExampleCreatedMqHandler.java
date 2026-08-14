package cc.shturl.wa.demo.mq.handler;

import cc.shturl.wa.demo.mq.message.ExampleCreatedMessage;
import cc.shturl.wa.demo.service.ExampleAuditService;
import cc.shturl.wa.demo.service.ExampleCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 示例创建事件 MQ 处理器
 * <p>
 * 编排消费者侧的异步任务：缓存预热 → 审计记录
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExampleCreatedMqHandler {

    private final ExampleCacheService exampleCacheService;
    private final ExampleAuditService exampleAuditService;

    /**
     * 处理示例创建事件
     */
    public void handle(ExampleCreatedMessage message) {
        log.info("[MQ处理] 开始处理示例创建事件, exampleId={}", message.getExampleId());

        // 1. 异步缓存预热
        exampleCacheService.warmCache(message);

        // 2. 记录审计日志
        exampleAuditService.recordCreated(message);

        log.info("[MQ处理] 示例创建事件处理完成, exampleId={}", message.getExampleId());
    }
}
