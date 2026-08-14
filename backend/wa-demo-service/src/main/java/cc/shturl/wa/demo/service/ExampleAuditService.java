package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.mq.message.ExampleCreatedMessage;

/**
 * 示例数据审计服务
 */
public interface ExampleAuditService {

    /**
     * 记录示例创建审计日志
     */
    void recordCreated(ExampleCreatedMessage message);
}
