package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.req.RoomDeptReq;
import cc.shturl.wa.demo.dto.req.RoomInviteReq;
import cc.shturl.wa.demo.dto.resp.RoomDetailResp;
import cc.shturl.wa.demo.entity.RoomInvites;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final AuthTokenSupport authTokenSupport;

    @PostMapping("/invites")
    public Result<RoomInvites> inviteFriend(@RequestHeader("Authorization") String authorization,
                                            @Valid @RequestBody RoomInviteReq request) {
        return Result.ok(roomService.inviteFriend(authTokenSupport.requireUserIdFromAccessToken(authorization), request));
    }

    @GetMapping("/invites/pending")
    public Result<java.util.List<cc.shturl.wa.demo.dto.resp.RoomInvitePendingResp>> listPendingInvites(
            @RequestHeader("Authorization") String authorization) {
        return Result.ok(roomService.listPendingInvites(authTokenSupport.requireUserIdFromAccessToken(authorization)));
    }

    @PostMapping("/invites/{id}/accept")
    public Result<RoomDetailResp> acceptInvite(@RequestHeader("Authorization") String authorization,
                                               @PathVariable("id") Long id) {
        return Result.ok(roomService.acceptInvite(authTokenSupport.requireUserIdFromAccessToken(authorization), id));
    }

    @PostMapping("/invites/{id}/reject")
    public Result<RoomInvites> rejectInvite(@RequestHeader("Authorization") String authorization,
                                            @PathVariable("id") Long id) {
        return Result.ok(roomService.rejectInvite(authTokenSupport.requireUserIdFromAccessToken(authorization), id));
    }

    @GetMapping("/{roomId}")
    public Result<RoomDetailResp> getRoomDetail(@RequestHeader("Authorization") String authorization,
                                                @PathVariable("roomId") Long roomId) {
        return Result.ok(roomService.getRoomDetail(authTokenSupport.requireUserIdFromAccessToken(authorization), roomId));
    }

    @PostMapping("/{roomId}/department")
    public Result<RoomDetailResp> setDepartment(@RequestHeader("Authorization") String authorization,
                                                @PathVariable("roomId") Long roomId,
                                                @Valid @RequestBody RoomDeptReq request) {
        return Result.ok(roomService.setDepartment(authTokenSupport.requireUserIdFromAccessToken(authorization), roomId, request));
    }

    @PostMapping("/{roomId}/ready")
    public Result<RoomDetailResp> setReady(@RequestHeader("Authorization") String authorization,
                                           @PathVariable("roomId") Long roomId) {
        return Result.ok(roomService.setReady(authTokenSupport.requireUserIdFromAccessToken(authorization), roomId));
    }

    @PostMapping("/{roomId}/leave")
    public Result<RoomDetailResp> leaveRoom(@RequestHeader("Authorization") String authorization,
                                            @PathVariable("roomId") Long roomId) {
        return Result.ok(roomService.leaveRoom(authTokenSupport.requireUserIdFromAccessToken(authorization), roomId));
    }
}
