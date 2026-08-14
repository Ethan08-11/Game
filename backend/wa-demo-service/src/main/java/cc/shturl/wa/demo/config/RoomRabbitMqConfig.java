package cc.shturl.wa.demo.config;

import cc.shturl.wa.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomRabbitMqConfig {

    @Bean
    public DirectExchange roomEventExchange() {
        return new DirectExchange(MqConstants.ROOM_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue roomInviteCreatedQueue() {
        return new Queue(MqConstants.ROOM_INVITE_CREATED_QUEUE, true);
    }

    @Bean
    public Queue roomInviteAcceptedQueue() {
        return new Queue(MqConstants.ROOM_INVITE_ACCEPTED_QUEUE, true);
    }

    @Bean
    public Queue roomInviteRejectedQueue() {
        return new Queue(MqConstants.ROOM_INVITE_REJECTED_QUEUE, true);
    }

    @Bean
    public Queue roomCreatedQueue() {
        return new Queue(MqConstants.ROOM_CREATED_QUEUE, true);
    }

    @Bean
    public Queue roomMemberDepartmentChangedQueue() {
        return new Queue(MqConstants.ROOM_MEMBER_DEPARTMENT_CHANGED_QUEUE, true);
    }

    @Bean
    public Queue roomMemberReadyQueue() {
        return new Queue(MqConstants.ROOM_MEMBER_READY_QUEUE, true);
    }

    @Bean
    public Binding roomInviteCreatedBinding(@Qualifier("roomInviteCreatedQueue") Queue roomInviteCreatedQueue,
                                             @Qualifier("roomEventExchange") DirectExchange roomEventExchange) {
        return BindingBuilder.bind(roomInviteCreatedQueue).to(roomEventExchange)
                .with(MqConstants.ROOM_INVITE_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding roomInviteAcceptedBinding(@Qualifier("roomInviteAcceptedQueue") Queue roomInviteAcceptedQueue,
                                             @Qualifier("roomEventExchange") DirectExchange roomEventExchange) {
        return BindingBuilder.bind(roomInviteAcceptedQueue).to(roomEventExchange)
                .with(MqConstants.ROOM_INVITE_ACCEPTED_ROUTING_KEY);
    }

    @Bean
    public Binding roomInviteRejectedBinding(@Qualifier("roomInviteRejectedQueue") Queue roomInviteRejectedQueue,
                                             @Qualifier("roomEventExchange") DirectExchange roomEventExchange) {
        return BindingBuilder.bind(roomInviteRejectedQueue).to(roomEventExchange)
                .with(MqConstants.ROOM_INVITE_REJECTED_ROUTING_KEY);
    }

    @Bean
    public Binding roomCreatedBinding(@Qualifier("roomCreatedQueue") Queue roomCreatedQueue,
                                      @Qualifier("roomEventExchange") DirectExchange roomEventExchange) {
        return BindingBuilder.bind(roomCreatedQueue).to(roomEventExchange)
                .with(MqConstants.ROOM_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding roomMemberDepartmentChangedBinding(@Qualifier("roomMemberDepartmentChangedQueue") Queue roomMemberDepartmentChangedQueue,
                                                      @Qualifier("roomEventExchange") DirectExchange roomEventExchange) {
        return BindingBuilder.bind(roomMemberDepartmentChangedQueue).to(roomEventExchange)
                .with(MqConstants.ROOM_MEMBER_DEPARTMENT_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding roomMemberReadyBinding(@Qualifier("roomMemberReadyQueue") Queue roomMemberReadyQueue,
                                          @Qualifier("roomEventExchange") DirectExchange roomEventExchange) {
        return BindingBuilder.bind(roomMemberReadyQueue).to(roomEventExchange)
                .with(MqConstants.ROOM_MEMBER_READY_ROUTING_KEY);
    }
}
