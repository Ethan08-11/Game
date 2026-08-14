package cc.shturl.wa.demo.service;

import cc.shturl.wa.common.dto.RoomCreatedEvent;
import cc.shturl.wa.common.dto.RoomInviteAcceptedEvent;
import cc.shturl.wa.common.dto.RoomInviteCreatedEvent;
import cc.shturl.wa.common.dto.RoomInviteRejectedEvent;
import cc.shturl.wa.common.dto.RoomMemberDepartmentChangedEvent;
import cc.shturl.wa.common.dto.RoomMemberReadyEvent;

public interface RoomEventPublisher {
    void publishInviteCreated(RoomInviteCreatedEvent event);
    void publishInviteAccepted(RoomInviteAcceptedEvent event);
    void publishInviteRejected(RoomInviteRejectedEvent event);
    void publishRoomCreated(RoomCreatedEvent event);
    void publishDepartmentChanged(RoomMemberDepartmentChangedEvent event);
    void publishReadyChanged(RoomMemberReadyEvent event);
}
