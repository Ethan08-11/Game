package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.req.FriendRemarkReq;
import cc.shturl.wa.demo.dto.req.FriendRequestReq;
import cc.shturl.wa.demo.dto.resp.FriendResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    private final UserService userService;
    private final AuthTokenSupport authTokenSupport;

    // ==================== 好友列表与申请 ====================

    @GetMapping
    public Result<List<FriendResp>> listMyFriends(@RequestHeader("Authorization") String authorization) {
        return Result.ok(userService.listMyFriends(authTokenSupport.requireUserIdFromAccessToken(authorization)));
    }

    @PostMapping("/request")
    public Result<FriendResp> requestFriend(@RequestHeader("Authorization") String authorization,
                                            @Valid @RequestBody FriendRequestReq request) {
        return Result.ok(userService.requestFriend(authTokenSupport.requireUserIdFromAccessToken(authorization), request));
    }

    @PutMapping("/{id}/accept")
    public Result<FriendResp> acceptFriend(@RequestHeader("Authorization") String authorization,
                                           @PathVariable("id") Long id,
                                           @Valid @RequestBody(required = false) FriendRemarkReq request) {
        return Result.ok(userService.acceptFriend(authTokenSupport.requireUserIdFromAccessToken(authorization), id, request));
    }

    /** 拒绝好友申请（仅申请接收方可调用）。 */
    @DeleteMapping("/requests/{id}/reject")
    public Result<Void> rejectFriend(@RequestHeader("Authorization") String authorization,
                                     @PathVariable("id") Long id) {
        userService.rejectFriend(authTokenSupport.requireUserIdFromAccessToken(authorization), id);
        return Result.ok();
    }

    /** 我收到的好友申请列表。 */
    @GetMapping("/requests/incoming")
    public Result<List<FriendResp>> listIncomingRequests(@RequestHeader("Authorization") String authorization) {
        return Result.ok(userService.listIncomingRequests(authTokenSupport.requireUserIdFromAccessToken(authorization)));
    }

    /** 我发出的好友申请列表。 */
    @GetMapping("/requests/outgoing")
    public Result<List<FriendResp>> listOutgoingRequests(@RequestHeader("Authorization") String authorization) {
        return Result.ok(userService.listOutgoingRequests(authTokenSupport.requireUserIdFromAccessToken(authorization)));
    }

    /** 修改好友备注。 */
    @PutMapping("/{id}/remark")
    public Result<FriendResp> updateFriendRemark(@RequestHeader("Authorization") String authorization,
                                                 @PathVariable("id") Long id,
                                                 @Valid @RequestBody(required = false) FriendRemarkReq request) {
        return Result.ok(userService.updateFriendRemark(authTokenSupport.requireUserIdFromAccessToken(authorization), id, request));
    }

    /** 删除好友 / 拒绝申请。 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFriend(@RequestHeader("Authorization") String authorization,
                                     @PathVariable("id") Long id) {
        userService.deleteFriend(authTokenSupport.requireUserIdFromAccessToken(authorization), id);
        return Result.ok();
    }

    // ==================== 拉黑 ====================

    /** 拉黑用户。 */
    @PostMapping("/block")
    public Result<FriendResp> blockUser(@RequestHeader("Authorization") String authorization,
                                        @Valid @RequestBody FriendRequestReq request) {
        return Result.ok(userService.blockUser(authTokenSupport.requireUserIdFromAccessToken(authorization), request));
    }

    /** 取消拉黑。 */
    @DeleteMapping("/block/{id}")
    public Result<Void> unblockUser(@RequestHeader("Authorization") String authorization,
                                    @PathVariable("id") Long id) {
        userService.unblockUser(authTokenSupport.requireUserIdFromAccessToken(authorization), id);
        return Result.ok();
    }

    /** 我的拉黑列表。 */
    @GetMapping("/blocked")
    public Result<List<FriendResp>> listBlockedUsers(@RequestHeader("Authorization") String authorization) {
        return Result.ok(userService.listBlockedUsers(authTokenSupport.requireUserIdFromAccessToken(authorization)));
    }
}
