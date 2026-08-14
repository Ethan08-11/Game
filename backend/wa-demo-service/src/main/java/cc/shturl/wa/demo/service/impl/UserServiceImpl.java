package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.req.FriendRemarkReq;
import cc.shturl.wa.demo.dto.req.FriendRequestReq;
import cc.shturl.wa.demo.dto.req.UpdateProfileReq;
import cc.shturl.wa.demo.dto.resp.FriendResp;
import cc.shturl.wa.demo.dto.resp.UserPresenceResp;
import cc.shturl.wa.demo.dto.resp.UserProfileResp;
import cc.shturl.wa.demo.dto.resp.UserSearchResp;
import cc.shturl.wa.demo.dto.resp.UserStatsResp;
import cc.shturl.wa.demo.entity.FriendRemark;
import cc.shturl.wa.demo.entity.Friendships;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.enums.FriendshipStatus;
import cc.shturl.wa.demo.mapper.FriendRemarkMapper;
import cc.shturl.wa.demo.mapper.FriendshipsMapper;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.RoomNotificationService;
import cc.shturl.wa.demo.service.UserPresenceService;
import cc.shturl.wa.demo.service.UserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    /** 用户搜索单次最大返回数量 */
    private static final int SEARCH_LIMIT = 20;

    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final FriendshipsMapper friendshipsMapper;
    private final FriendRemarkMapper friendRemarkMapper;
    private final UserPresenceService userPresenceService;
    private final RoomNotificationService notificationService;

    // ==================== 用户资料与统计 ====================

    @Override
    public UserProfileResp getProfile(Long currentUserId, Long targetUserId) {
        User user = requireUser(targetUserId);
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, targetUserId));
        return toProfileResp(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResp updateMyProfile(Long currentUserId, UpdateProfileReq request) {
        User user = requireUser(currentUserId);
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, currentUserId));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(currentUserId);
            profile.setLevel(1);
            profile.setExp(0);
            profile.setWinCount(0);
            profile.setLoseCount(0);
            profile.setDrawCount(0);
            profile.setMoney(0L);
            profileMapper.insert(profile);
        }
        if (request.displayName() != null) profile.setDisplayName(request.displayName());
        if (request.signature() != null) profile.setSignature(request.signature());
        if (request.gender() != null) profile.setGender(request.gender());
        profileMapper.updateById(profile);
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        if (request.email() != null) {
            checkEmailUnique(request.email(), currentUserId);
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            checkPhoneUnique(request.phone(), currentUserId);
            user.setPhone(request.phone());
        }
        userMapper.updateById(user);
        return toProfileResp(user, profile);
    }

    private void checkEmailUnique(String email, Long currentUserId) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email)
                .ne(User::getId, currentUserId));
        if (count != null && count > 0) {
            throw new BusinessException("邮箱已被占用");
        }
    }

    private void checkPhoneUnique(String phone, Long currentUserId) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone)
                .ne(User::getId, currentUserId));
        if (count != null && count > 0) {
            throw new BusinessException("手机号已被占用");
        }
    }

    @Override
    public UserStatsResp getStats(Long currentUserId, Long targetUserId) {
        User user = requireUser(targetUserId);
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, targetUserId));
        int win = profile == null || profile.getWinCount() == null ? 0 : profile.getWinCount();
        int lose = profile == null || profile.getLoseCount() == null ? 0 : profile.getLoseCount();
        int draw = profile == null || profile.getDrawCount() == null ? 0 : profile.getDrawCount();
        int total = win + lose + draw;
        int winRate = total == 0 ? 0 : (int) Math.round(win * 100.0 / total);
        return new UserStatsResp(user.getId(), user.getUsername(), profile == null ? 1 : profile.getLevel(),
                profile == null ? 0 : profile.getExp(), win, lose, draw, total, winRate);
    }

    // ==================== 好友列表与申请 ====================

    @Override
    public List<FriendResp> listMyFriends(Long currentUserId) {
        List<Friendships> list = friendshipsMapper.selectList(Wrappers.<Friendships>lambdaQuery()
                .eq(Friendships::getStatus, FriendshipStatus.ACCEPTED.getCode())
                .and(wrapper -> wrapper.eq(Friendships::getUserId, currentUserId)
                        .or()
                        .eq(Friendships::getFriendId, currentUserId)));
        return list.stream().map(friendships -> toFriendResp(friendships, currentUserId)).toList();
    }

    @Override
    @Transactional
    public FriendResp requestFriend(Long currentUserId, FriendRequestReq request) {
        Long targetId = request.friendId();
        if (currentUserId.equals(targetId)) {
            throw new BusinessException("不能添加自己为好友");
        }
        User target = requireUser(targetId);

        // 双向查重（任意方向存在记录都视为已有关系）
        Friendships existing = findAnyRelation(currentUserId, targetId);
        if (existing != null) {
            FriendshipStatus status = FriendshipStatus.of(existing.getStatus());
            // 对方拉黑了我（记录方向：user_id=对方, friend_id=我, status=BLOCKED）
            if (status == FriendshipStatus.BLOCKED
                    && Objects.equals(existing.getUserId(), targetId)
                    && Objects.equals(existing.getFriendId(), currentUserId)) {
                // 不暴露被拉黑事实，统一提示无法添加
                throw new BusinessException("无法添加该用户");
            }
            // 已是好友 / 我已发过申请 → 幂等返回现有记录
            return toFriendResp(existing, currentUserId);
        }

        Friendships friendships = new Friendships();
        friendships.setUserId(currentUserId);
        friendships.setFriendId(targetId);
        friendships.setStatus(FriendshipStatus.PENDING.getCode());
        friendshipsMapper.insert(friendships);

        // WebSocket 实时通知被申请方收到好友申请
        User fromUser = requireUser(currentUserId);
        notifyFriendRequestReceived(fromUser, target, friendships.getId());

        return toFriendResp(friendships, currentUserId);
    }

    @Override
    @Transactional
    public FriendResp acceptFriend(Long currentUserId, Long friendshipId, FriendRemarkReq request) {
        Friendships friendships = friendshipsMapper.selectById(friendshipId);
        if (friendships == null) {
            throw new BusinessException("好友申请不存在");
        }
        if (!Objects.equals(friendships.getFriendId(), currentUserId)) {
            throw new BusinessException("无权处理该好友申请");
        }
        FriendshipStatus current = FriendshipStatus.of(friendships.getStatus());
        if (current == FriendshipStatus.ACCEPTED) {
            throw new BusinessException("已经是好友，无需重复接受");
        }
        if (current != FriendshipStatus.PENDING) {
            throw new BusinessException("该申请当前不可接受");
        }
        friendships.setStatus(FriendshipStatus.ACCEPTED.getCode());
        friendships.setRemarkName(request == null ? null : request.remarkName());
        friendshipsMapper.updateById(friendships);

        String defaultRemark = requireUser(friendships.getUserId()).getUsername();
        upsertFriendRemark(currentUserId, friendships.getUserId(), request == null ? null : request.remarkName(), defaultRemark);
        upsertFriendRemark(friendships.getUserId(), currentUserId, null, requireUser(currentUserId).getUsername());

        // 通知申请发起方：申请已被接受
        notifyFriendRequestAccepted(currentUserId, friendships.getUserId(), friendshipId);

        return toFriendResp(friendships, currentUserId);
    }

    @Override
    @Transactional
    public void rejectFriend(Long currentUserId, Long friendshipId) {
        Friendships friendships = friendshipsMapper.selectById(friendshipId);
        if (friendships == null) {
            throw new BusinessException("好友申请不存在");
        }
        // 仅申请的接收方（friend_id=当前用户）有权拒绝，且记录需为待处理
        if (!Objects.equals(friendships.getFriendId(), currentUserId)) {
            throw new BusinessException("无权处理该好友申请");
        }
        if (FriendshipStatus.of(friendships.getStatus()) != FriendshipStatus.PENDING) {
            throw new BusinessException("该申请当前不可拒绝");
        }
        friendshipsMapper.deleteById(friendshipId);
    }

    @Override
    @Transactional
    public void deleteFriend(Long currentUserId, Long friendshipId) {
        Friendships friendships = friendshipsMapper.selectById(friendshipId);
        if (friendships == null) {
            throw new BusinessException("好友关系不存在");
        }
        if (!Objects.equals(friendships.getUserId(), currentUserId)
                && !Objects.equals(friendships.getFriendId(), currentUserId)) {
            throw new BusinessException("无权删除该关系");
        }
        friendshipsMapper.deleteById(friendshipId);
    }

    @Override
    @Transactional
    public FriendResp updateFriendRemark(Long currentUserId, Long friendshipId, FriendRemarkReq request) {
        Friendships friendships = friendshipsMapper.selectById(friendshipId);
        if (friendships == null) {
            throw new BusinessException("好友关系不存在");
        }
        if (!Objects.equals(friendships.getUserId(), currentUserId)
                && !Objects.equals(friendships.getFriendId(), currentUserId)) {
            throw new BusinessException("无权修改该好友备注");
        }
        if (FriendshipStatus.of(friendships.getStatus()) != FriendshipStatus.ACCEPTED) {
            throw new BusinessException("仅好友关系可修改备注");
        }
        String remarkName = request == null || request.remarkName() == null || request.remarkName().isBlank()
                ? requireUser(currentUserId.equals(friendships.getUserId()) ? friendships.getFriendId() : friendships.getUserId()).getUsername()
                : request.remarkName();
        friendships.setRemarkName(remarkName);
        friendshipsMapper.updateById(friendships);
        upsertFriendRemark(currentUserId, currentUserId.equals(friendships.getUserId()) ? friendships.getFriendId() : friendships.getUserId(),
                remarkName, remarkName);
        return toFriendResp(friendships, currentUserId);
    }

    @Override
    public List<FriendResp> listIncomingRequests(Long currentUserId) {
        List<Friendships> list = friendshipsMapper.selectList(Wrappers.<Friendships>lambdaQuery()
                .eq(Friendships::getFriendId, currentUserId)
                .eq(Friendships::getStatus, FriendshipStatus.PENDING.getCode()));
        return list.stream().map(friendships -> toFriendResp(friendships, currentUserId)).toList();
    }

    @Override
    public List<FriendResp> listOutgoingRequests(Long currentUserId) {
        List<Friendships> list = friendshipsMapper.selectList(Wrappers.<Friendships>lambdaQuery()
                .eq(Friendships::getUserId, currentUserId)
                .eq(Friendships::getStatus, FriendshipStatus.PENDING.getCode()));
        return list.stream().map(friendships -> toFriendResp(friendships, currentUserId)).toList();
    }

    // ==================== 拉黑 ====================

    @Override
    @Transactional
    public FriendResp blockUser(Long currentUserId, FriendRequestReq request) {
        Long targetId = request.friendId();
        if (currentUserId.equals(targetId)) {
            throw new BusinessException("不能拉黑自己");
        }
        User target = requireUser(targetId);

        // 查任意方向的现有记录
        Friendships existing = findAnyRelation(currentUserId, targetId);
        if (existing != null) {
            FriendshipStatus status = FriendshipStatus.of(existing.getStatus());
            if (status == FriendshipStatus.BLOCKED
                    && Objects.equals(existing.getUserId(), currentUserId)
                    && Objects.equals(existing.getFriendId(), targetId)) {
                // 已经拉黑过，幂等返回
                return toBlockResp(existing, currentUserId);
            }
            // 反向记录或其它状态：删除后以"我(user_id)→对方(friend_id)"方向重建为 BLOCKED
            friendshipsMapper.deleteById(existing.getId());
        }

        Friendships blocked = new Friendships();
        blocked.setUserId(currentUserId);
        blocked.setFriendId(targetId);
        blocked.setStatus(FriendshipStatus.BLOCKED.getCode());
        blocked.setRemarkName(null);
        friendshipsMapper.insert(blocked);
        return toBlockResp(blocked, currentUserId);
    }

    @Override
    @Transactional
    public void unblockUser(Long currentUserId, Long friendshipId) {
        Friendships friendships = friendshipsMapper.selectById(friendshipId);
        if (friendships == null) {
            throw new BusinessException("拉黑记录不存在");
        }
        if (!Objects.equals(friendships.getUserId(), currentUserId)) {
            throw new BusinessException("无权取消该拉黑");
        }
        if (FriendshipStatus.of(friendships.getStatus()) != FriendshipStatus.BLOCKED) {
            throw new BusinessException("该记录不是拉黑关系");
        }
        friendshipsMapper.deleteById(friendshipId);
    }

    @Override
    public List<FriendResp> listBlockedUsers(Long currentUserId) {
        List<Friendships> list = friendshipsMapper.selectList(Wrappers.<Friendships>lambdaQuery()
                .eq(Friendships::getUserId, currentUserId)
                .eq(Friendships::getStatus, FriendshipStatus.BLOCKED.getCode()));
        // 拉黑列表无需在线状态，使用轻量转换
        return list.stream().map(friendships -> toBlockResp(friendships, currentUserId)).toList();
    }

    // ==================== 用户搜索 ====================

    @Override
    public List<UserSearchResp> searchUsers(Long currentUserId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String trimmed = keyword.trim();
        List<User> users;
        // 纯数字时额外支持按 ID 精确匹配
        if (trimmed.matches("\\d+")) {
            Long id = Long.valueOf(trimmed);
            User byId = userMapper.selectById(id);
            users = byId == null ? List.of() : List.of(byId);
        } else {
            users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                    .like(User::getUsername, trimmed)
                    .last("LIMIT " + SEARCH_LIMIT));
        }

        // 批量预查关系状态，避免 N+1
        Map<Long, Friendships> relationMap = loadRelationsFor(currentUserId,
                users.stream().map(User::getId).toList());

        return users.stream()
                .map(u -> toSearchResp(u, currentUserId, relationMap.get(u.getId())))
                .toList();
    }

    /** 批量查询当前用户与一组用户的任意方向关系，按对方 ID 索引。 */
    private Map<Long, Friendships> loadRelationsFor(Long currentUserId, List<Long> targetIds) {
        Map<Long, Friendships> result = new LinkedHashMap<>();
        if (targetIds == null || targetIds.isEmpty()) {
            return result;
        }
        List<Friendships> relations = friendshipsMapper.selectList(Wrappers.<Friendships>lambdaQuery()
                .and(wrapper -> wrapper.eq(Friendships::getUserId, currentUserId)
                        .or().eq(Friendships::getFriendId, currentUserId)));
        for (Friendships relation : relations) {
            Long otherId = Objects.equals(relation.getUserId(), currentUserId)
                    ? relation.getFriendId() : relation.getUserId();
            // 仅保留搜索结果中出现的目标
            if (targetIds.contains(otherId)) {
                result.put(otherId, relation);
            }
        }
        return result;
    }

    private UserSearchResp toSearchResp(User user, Long currentUserId, Friendships relation) {
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, user.getId()));
        Integer level = profile == null || profile.getLevel() == null ? 1 : profile.getLevel();
        boolean isFriend = relation != null
                && FriendshipStatus.of(relation.getStatus()) == FriendshipStatus.ACCEPTED;
        Integer status = relation == null ? null : relation.getStatus();
        String displayName = profile == null || profile.getDisplayName() == null
                ? user.getUsername() : profile.getDisplayName();
        return new UserSearchResp(user.getId(), user.getUsername(), displayName,
                user.getAvatarUrl(), level, isFriend, status);
    }

    // ==================== 通知 ====================

    /**
     * 推送好友申请通知给被申请方（target）。
     *
     * @param fromUser     申请发起方用户（信息来源）
     * @param target       被申请方用户（通知接收者）
     * @param friendshipId 关系记录 ID
     */
    private void notifyFriendRequestReceived(User fromUser, User target, Long friendshipId) {
        if (fromUser == null || fromUser.getId() == null || target == null || target.getId() == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("friendshipId", friendshipId);
        data.put("fromUserId", fromUser.getId());
        data.put("username", fromUser.getUsername());
        data.put("avatarUrl", fromUser.getAvatarUrl());
        Map<String, Object> payload = Map.of("type", "friend.request.received", "data", data);
        notificationService.notifyUser(target.getId(), payload);
    }

    private void notifyFriendRequestAccepted(Long acceptorId, Long requesterId, Long friendshipId) {
        User acceptor = userMapper.selectById(acceptorId);
        Map<String, Object> data = new HashMap<>();
        data.put("friendshipId", friendshipId);
        data.put("acceptorId", acceptorId);
        data.put("username", acceptor == null ? null : acceptor.getUsername());
        data.put("displayName", acceptor == null ? null : acceptor.getUsername());
        data.put("avatarUrl", acceptor == null ? null : acceptor.getAvatarUrl());
        Map<String, Object> payload = Map.of("type", "friend.request.accepted", "data", data);
        notificationService.notifyUser(requesterId, payload);
    }

    // ==================== 私有辅助 ====================

    /** 查询任意方向的好友关系（user_id=A&friend_id=B 或 user_id=B&friend_id=A）。 */
    private Friendships findAnyRelation(Long a, Long b) {
        return friendshipsMapper.selectOne(Wrappers.<Friendships>lambdaQuery()
                .and(wrapper -> wrapper
                        .nested(n -> n.eq(Friendships::getUserId, a).eq(Friendships::getFriendId, b))
                        .or()
                        .nested(n -> n.eq(Friendships::getUserId, b).eq(Friendships::getFriendId, a))));
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private UserProfileResp toProfileResp(User user, UserProfile profile) {
        return new UserProfileResp(user.getId(), user.getUsername(),
                profile == null || profile.getDisplayName() == null ? user.getUsername() : profile.getDisplayName(),
                profile == null ? null : profile.getSignature(),
                profile == null ? null : profile.getGender(),
                user.getAvatarUrl(),
                profile == null || profile.getLevel() == null ? 1 : profile.getLevel(),
                profile == null || profile.getExp() == null ? 0 : profile.getExp(),
                profile == null || profile.getWinCount() == null ? 0 : profile.getWinCount(),
                profile == null || profile.getLoseCount() == null ? 0 : profile.getLoseCount(),
                profile == null || profile.getDrawCount() == null ? 0 : profile.getDrawCount(),
                profile == null || profile.getMoney() == null ? 0L : profile.getMoney());
    }

    private FriendResp toFriendResp(Friendships friendships, Long currentUserId) {
        Long friendId = currentUserId.equals(friendships.getUserId()) ? friendships.getFriendId() : friendships.getUserId();
        Long ownerId = currentUserId.equals(friendships.getUserId()) ? friendships.getUserId() : friendships.getFriendId();
        User friend = requireUser(friendId);
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, friend.getId()));
        UserPresenceResp presence = userPresenceService.getPresence(friend.getId());
        String remarkName = resolveFriendRemark(currentUserId, friendId, friendships.getRemarkName(), friend.getUsername());
        return new FriendResp(friendships.getId(), ownerId, friendId, friendships.getStatus(),
                remarkName, friend.getUsername(),
                profile == null || profile.getDisplayName() == null ? friend.getUsername() : profile.getDisplayName(),
                friend.getAvatarUrl(), presence.onlineStatus(), presence.presenceStatus(), presence.invitable());
    }

    /** 拉黑列表使用的轻量转换（不查在线状态）。 */
    private FriendResp toBlockResp(Friendships friendships, Long currentUserId) {
        Long friendId = currentUserId.equals(friendships.getUserId()) ? friendships.getFriendId() : friendships.getUserId();
        Long ownerId = currentUserId.equals(friendships.getUserId()) ? friendships.getUserId() : friendships.getFriendId();
        User friend = requireUser(friendId);
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, friend.getId()));
        String remarkName = resolveFriendRemark(currentUserId, friendId, friendships.getRemarkName(), friend.getUsername());
        return new FriendResp(friendships.getId(), ownerId, friendId, friendships.getStatus(),
                remarkName, friend.getUsername(),
                profile == null || profile.getDisplayName() == null ? friend.getUsername() : profile.getDisplayName(),
                friend.getAvatarUrl(), 0, "OFFLINE", false);
    }

    private String resolveFriendRemark(Long currentUserId, Long friendId, String relationRemarkName, String fallbackUsername) {
        FriendRemark friendRemark = friendRemarkMapper.selectOne(Wrappers.<FriendRemark>lambdaQuery()
                .eq(FriendRemark::getUserId, currentUserId)
                .eq(FriendRemark::getFriendUserId, friendId)
                .last("LIMIT 1"));
        if (friendRemark != null && friendRemark.getRemarkName() != null && !friendRemark.getRemarkName().isBlank()) {
            return friendRemark.getRemarkName();
        }
        if (relationRemarkName != null && !relationRemarkName.isBlank()) {
            return relationRemarkName;
        }
        return fallbackUsername;
    }

    private void upsertFriendRemark(Long userId, Long friendUserId, String remarkName, String fallbackUsername) {
        String finalRemark = remarkName == null || remarkName.isBlank() ? fallbackUsername : remarkName;
        FriendRemark friendRemark = friendRemarkMapper.selectOne(Wrappers.<FriendRemark>lambdaQuery()
                .eq(FriendRemark::getUserId, userId)
                .eq(FriendRemark::getFriendUserId, friendUserId)
                .last("LIMIT 1"));
        if (friendRemark == null) {
            friendRemark = new FriendRemark();
            friendRemark.setUserId(userId);
            friendRemark.setFriendUserId(friendUserId);
            friendRemark.setRemarkName(finalRemark);
            friendRemarkMapper.insert(friendRemark);
        } else {
            friendRemark.setRemarkName(finalRemark);
            friendRemarkMapper.updateById(friendRemark);
        }
    }
}
