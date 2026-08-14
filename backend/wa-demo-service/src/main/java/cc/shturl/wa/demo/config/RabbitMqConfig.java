package cc.shturl.wa.demo.config;

import cc.shturl.wa.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * <p>
 * 场景：示例创建事件
 * Exchange(wa.demo.exchange) --[routingKey: wa.demo.example.created]--> Queue(wa.demo.example.created.queue)
 * </p>
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange exampleExchange() {
        return new DirectExchange(MqConstants.EXAMPLE_EXCHANGE, true, false);
    }

    @Bean
    public Queue exampleCreatedQueue() {
        return new Queue(MqConstants.EXAMPLE_CREATED_QUEUE, true);
    }

    @Bean
    public Binding exampleCreatedBinding(@Qualifier("exampleCreatedQueue") Queue exampleCreatedQueue,
                                         @Qualifier("exampleExchange") DirectExchange exampleExchange) {
        return BindingBuilder.bind(exampleCreatedQueue)
                .to(exampleExchange)
                .with(MqConstants.EXAMPLE_CREATED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
