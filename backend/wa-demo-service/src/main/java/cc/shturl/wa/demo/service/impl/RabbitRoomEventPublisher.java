package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.constant.MqConstants;
import cc.shturl.wa.common.dto.RoomCreatedEvent;
import cc.shturl.wa.common.dto.RoomInviteAcceptedEvent;
import cc.shturl.wa.common.dto.RoomInviteCreatedEvent;
import cc.shturl.wa.common.dto.RoomInviteRejectedEvent;
import cc.shturl.wa.common.dto.RoomMemberDepartmentChangedEvent;
import cc.shturl.wa.common.dto.RoomMemberReadyEvent;
import cc.shturl.wa.demo.service.RoomEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitRoomEventPublisher implements RoomEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishInviteCreated(RoomInviteCreatedEvent event) {
        rabbitTemplate.convertAndSend(MqConstants.ROOM_EVENT_EXCHANGE, MqConstants.ROOM_INVITE_CREATED_ROUTING_KEY, event);
    }

    @Override
    public void publishInviteAccepted(RoomInviteAcceptedEvent event) {
        rabbitTemplate.convertAndSend(MqConstants.ROOM_EVENT_EXCHANGE, MqConstants.ROOM_INVITE_ACCEPTED_ROUTING_KEY, event);
    }

    @Override
    public void publishInviteRejected(RoomInviteRejectedEvent event) {
        rabbitTemplate.convertAndSend(MqConstants.ROOM_EVENT_EXCHANGE, MqConstants.ROOM_INVITE_REJECTED_ROUTING_KEY, event);
    }

    @Override
    public void publishRoomCreated(RoomCreatedEvent event) {
        rabbitTemplate.convertAndSend(MqConstants.ROOM_EVENT_EXCHANGE, MqConstants.ROOM_CREATED_ROUTING_KEY, event);
    }

    @Override
    public void publishDepartmentChanged(RoomMemberDepartmentChangedEvent event) {
        rabbitTemplate.convertAndSend(MqConstants.ROOM_EVENT_EXCHANGE, MqConstants.ROOM_MEMBER_DEPARTMENT_CHANGED_ROUTING_KEY, event);
    }

    @Override
    public void publishReadyChanged(RoomMemberReadyEvent event) {
        rabbitTemplate.convertAndSend(MqConstants.ROOM_EVENT_EXCHANGE, MqConstants.ROOM_MEMBER_READY_ROUTING_KEY, event);
    }
}
