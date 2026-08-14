package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.mq.message.ExampleCreatedMessage;
import cc.shturl.wa.demo.service.ExampleAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 示例数据审计服务实现
 * <p>
 * MQ 消费者异步调用，记录操作审计日志（后续可扩展为写入 audit 表）
 * </p>
 */
@Slf4j
@Service
public class ExampleAuditServiceImpl implements ExampleAuditService {

    @Override
    public void recordCreated(ExampleCreatedMessage message) {
        // 骨架实现：输出结构化审计日志，生产环境可改为写入 t_audit_log 表
        log.info("[审计记录] eventType={}, exampleId={}, name={}, occurredAt={}",
                message.getEventType(),
                message.getExampleId(),
                message.getName(),
                message.getOccurredAt());
    }
}
