package cc.shturl.wa.demo.mq.consumer;

import cc.shturl.wa.common.constant.MqConstants;
import cc.shturl.wa.demo.mq.handler.ExampleCreatedMqHandler;
import cc.shturl.wa.demo.mq.message.ExampleCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 示例创建事件消费者
 * <p>
 * 监听 wa.demo.example.created.queue，收到消息后委托 Handler 处理
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExampleCreatedConsumer {

    private final ExampleCreatedMqHandler exampleCreatedMqHandler;

    @RabbitListener(queues = MqConstants.EXAMPLE_CREATED_QUEUE)
    public void onExampleCreated(ExampleCreatedMessage message) {
        log.info("[MQ接收] eventType={}, exampleId={}",
                message.getEventType(), message.getExampleId());
        exampleCreatedMqHandler.handle(message);
    }
}
