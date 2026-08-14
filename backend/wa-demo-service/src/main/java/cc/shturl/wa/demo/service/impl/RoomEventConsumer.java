package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.constant.MqConstants;
import cc.shturl.wa.common.dto.RoomCreatedEvent;
import cc.shturl.wa.common.dto.RoomInviteAcceptedEvent;
import cc.shturl.wa.common.dto.RoomInviteCreatedEvent;
import cc.shturl.wa.common.dto.RoomInviteRejectedEvent;
import cc.shturl.wa.common.dto.RoomMemberDepartmentChangedEvent;
import cc.shturl.wa.common.dto.RoomMemberReadyEvent;
import cc.shturl.wa.demo.entity.RoomMembers;
import cc.shturl.wa.demo.mapper.RoomMembersMapper;
import cc.shturl.wa.demo.service.RoomNotificationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomEventConsumer {
    private final RoomNotificationService notificationService;
    private final RoomMembersMapper roomMembersMapper;

    @RabbitListener(queues = MqConstants.ROOM_INVITE_CREATED_QUEUE)
    public void onInviteCreated(RoomInviteCreatedEvent event) {
        notificationService.notifyUser(event.toUserId(), toPayload("room.invite.created", event));
    }

    @RabbitListener(queues = MqConstants.ROOM_INVITE_ACCEPTED_QUEUE)
    public void onInviteAccepted(RoomInviteAcceptedEvent event) {
        notificationService.notifyUsers(event.fromUserId(), event.toUserId(), toPayload("room.invite.accepted", event));
    }

    @RabbitListener(queues = MqConstants.ROOM_INVITE_REJECTED_QUEUE)
    public void onInviteRejected(RoomInviteRejectedEvent event) {
        notificationService.notifyUser(event.fromUserId(), toPayload("room.invite.rejected", event));
    }

    @RabbitListener(queues = MqConstants.ROOM_CREATED_QUEUE)
    public void onRoomCreated(RoomCreatedEvent event) {
        notificationService.notifyUsers(event.hostUserId(), event.memberUserId(), toPayload("room.created", event));
    }

    @RabbitListener(queues = MqConstants.ROOM_MEMBER_DEPARTMENT_CHANGED_QUEUE)
    public void onDepartmentChanged(RoomMemberDepartmentChangedEvent event) {
        notifyRoomMembers(event.roomId(), toPayload("room.member.department.changed", event));
    }

    @RabbitListener(queues = MqConstants.ROOM_MEMBER_READY_QUEUE)
    public void onReadyChanged(RoomMemberReadyEvent event) {
        notifyRoomMembers(event.roomId(), toPayload("room.member.ready", event));
    }

    private void notifyRoomMembers(Long roomId, Object payload) {
        List<RoomMembers> members = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, roomId));
        for (RoomMembers member : members) {
            if (member.getUserId() != null) {
                notificationService.notifyUser(member.getUserId(), payload);
            }
        }
    }

    private Map<String, Object> toPayload(String type, Object event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("data", event);
        return payload;
    }
}
