package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.req.FriendRemarkReq;
import cc.shturl.wa.demo.dto.req.FriendRequestReq;
import cc.shturl.wa.demo.dto.req.UpdateProfileReq;
import cc.shturl.wa.demo.dto.resp.FriendResp;
import cc.shturl.wa.demo.dto.resp.UserProfileResp;
import cc.shturl.wa.demo.dto.resp.UserSearchResp;
import cc.shturl.wa.demo.dto.resp.UserStatsResp;

import java.util.List;

public interface UserService {
    UserProfileResp getProfile(Long currentUserId, Long targetUserId);
    UserProfileResp updateMyProfile(Long currentUserId, UpdateProfileReq request);
    UserStatsResp getStats(Long currentUserId, Long targetUserId);

    // ===== 好友列表与申请 =====
    List<FriendResp> listMyFriends(Long currentUserId);
    FriendResp requestFriend(Long currentUserId, FriendRequestReq request);
    FriendResp acceptFriend(Long currentUserId, Long friendshipId, FriendRemarkReq request);
    void rejectFriend(Long currentUserId, Long friendshipId);
    void deleteFriend(Long currentUserId, Long friendshipId);
    FriendResp updateFriendRemark(Long currentUserId, Long friendshipId, FriendRemarkReq request);

    /** 我收到的好友申请列表（status=PENDING 且 friend_id=当前用户） */
    List<FriendResp> listIncomingRequests(Long currentUserId);

    /** 我发出的好友申请列表（status=PENDING 且 user_id=当前用户） */
    List<FriendResp> listOutgoingRequests(Long currentUserId);

    // ===== 拉黑 =====
    FriendResp blockUser(Long currentUserId, FriendRequestReq request);
    void unblockUser(Long currentUserId, Long friendshipId);
    List<FriendResp> listBlockedUsers(Long currentUserId);

    // ===== 用户搜索 =====
    List<UserSearchResp> searchUsers(Long currentUserId, String keyword);
}
