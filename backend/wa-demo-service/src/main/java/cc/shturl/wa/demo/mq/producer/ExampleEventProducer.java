package cc.shturl.wa.demo.mq.producer;

import cc.shturl.wa.common.constant.MqConstants;
import cc.shturl.wa.demo.entity.ExampleEntity;
import cc.shturl.wa.demo.mq.event.MqEventType;
import cc.shturl.wa.demo.mq.message.ExampleCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 示例事件消息生产者
 * <p>
 * 场景：示例数据写入数据库后，发布 EXAMPLE_CREATED 事件到 RabbitMQ，
 * 由消费者异步完成缓存预热和审计记录，避免阻塞 HTTP 响应
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExampleEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布「示例数据创建」事件
     *
     * @param entity 已持久化的示例实体
     */
    public void publishExampleCreated(ExampleEntity entity) {
        ExampleCreatedMessage message = ExampleCreatedMessage.builder()
                .eventType(MqEventType.EXAMPLE_CREATED.getCode())
                .exampleId(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .occurredAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                MqConstants.EXAMPLE_EXCHANGE,
                MqConstants.EXAMPLE_CREATED_ROUTING_KEY,
                message);

        log.info("[MQ发送] eventType={}, exampleId={}, exchange={}, routingKey={}",
                message.getEventType(),
                message.getExampleId(),
                MqConstants.EXAMPLE_EXCHANGE,
                MqConstants.EXAMPLE_CREATED_ROUTING_KEY);
    }
}
