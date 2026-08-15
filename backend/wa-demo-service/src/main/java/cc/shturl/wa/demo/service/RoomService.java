package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.req.RoomDeptReq;
import cc.shturl.wa.demo.dto.req.RoomInviteReq;
import cc.shturl.wa.demo.dto.resp.RoomDetailResp;
import cc.shturl.wa.demo.dto.resp.RoomInvitePendingResp;
import cc.shturl.wa.demo.entity.RoomInvites;

import java.util.List;

public interface RoomService {
    RoomInvites inviteFriend(Long currentUserId, RoomInviteReq request);
    RoomDetailResp acceptInvite(Long currentUserId, Long inviteId);
    RoomInvites rejectInvite(Long currentUserId, Long inviteId);
    RoomInvites timeoutInvite(Long inviteId);
    List<RoomInvitePendingResp> listPendingInvites(Long currentUserId);
    RoomDetailResp getRoomDetail(Long currentUserId, Long roomId);
    RoomDetailResp getCurrentRoom(Long currentUserId);
    void releaseIdleRoom(Long currentUserId);
    RoomDetailResp setDepartment(Long currentUserId, Long roomId, RoomDeptReq request);
    RoomDetailResp setReady(Long currentUserId, Long roomId);
    RoomDetailResp leaveRoom(Long currentUserId, Long roomId);
}
