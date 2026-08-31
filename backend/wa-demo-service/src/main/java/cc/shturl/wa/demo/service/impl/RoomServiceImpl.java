package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.common.dto.RoomCreatedEvent;
import cc.shturl.wa.demo.common.RedisDistributedLock;
import cc.shturl.wa.common.dto.RoomInviteAcceptedEvent;
import cc.shturl.wa.common.dto.RoomInviteCreatedEvent;
import cc.shturl.wa.common.dto.RoomInviteRejectedEvent;
import cc.shturl.wa.common.dto.RoomMemberDepartmentChangedEvent;
import cc.shturl.wa.common.dto.RoomMemberReadyEvent;
import cc.shturl.wa.demo.dto.req.RoomDeptReq;
import cc.shturl.wa.demo.dto.req.RoomInviteReq;
import cc.shturl.wa.demo.dto.resp.RoomDetailResp;
import cc.shturl.wa.demo.dto.resp.RoomMemberResp;
import cc.shturl.wa.demo.entity.Friendships;
import cc.shturl.wa.demo.entity.GameRooms;
import cc.shturl.wa.demo.entity.Matches;
import cc.shturl.wa.demo.entity.RoomInvites;
import cc.shturl.wa.demo.entity.RoomMembers;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.mapper.FriendshipsMapper;
import cc.shturl.wa.demo.mapper.GameRoomsMapper;
import cc.shturl.wa.demo.mapper.MatchesMapper;
import cc.shturl.wa.demo.mapper.RoomInvitesMapper;
import cc.shturl.wa.demo.mapper.RoomMembersMapper;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.service.MatchService;
import cc.shturl.wa.demo.service.RoomEventPublisher;
import cc.shturl.wa.demo.service.RoomNotificationService;
import cc.shturl.wa.demo.service.RoomPresenceCleanupService;
import cc.shturl.wa.demo.service.RoomService;
import cc.shturl.wa.demo.service.UserPresenceService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private static final String DEPT_SALES = "sales";
    private static final String DEPT_PURCHASE = "purchase";

    private final RoomInvitesMapper roomInvitesMapper;
    private final GameRoomsMapper gameRoomsMapper;
    private final RoomMembersMapper roomMembersMapper;
    private final MatchesMapper matchesMapper;
    private final FriendshipsMapper friendshipsMapper;
    private final UserMapper userMapper;
    private final UserPresenceService userPresenceService;
    private final RoomEventPublisher roomEventPublisher;
    private final RoomNotificationService notificationService;
    private final MatchService matchService;
    private final cc.shturl.wa.demo.service.TaskService taskService;
    private final RedisDistributedLock distributedLock;
    private final TransactionTemplate transactionTemplate;
    private final RoomPresenceCleanupService roomPresenceCleanupService;

    @Override
    @Transactional
    public RoomInvites inviteFriend(Long currentUserId, RoomInviteReq request) {
        Long friendId = request.friendId();
        User fromUser = requireUser(currentUserId);
        User toUser = requireUser(friendId);
        requireFriend(currentUserId, friendId);
        roomPresenceCleanupService.releaseIdleRooms(currentUserId);
        roomPresenceCleanupService.releaseIdleRooms(friendId);
        requireInvitableUser(currentUserId, "你当前不在线，请刷新页面后重试");
        requireInvitableUser(friendId, "对方不在线，无法邀请");
        RoomInvites invite = new RoomInvites();
        invite.setFromUserId(currentUserId);
        invite.setToUserId(friendId);
        invite.setStatus(0);
        invite.setExpiredAt(LocalDateTime.now().plusMinutes(3));
        roomInvitesMapper.insert(invite);
        RoomInviteCreatedEvent event = new RoomInviteCreatedEvent(
                UUID.randomUUID().toString().replace("-", ""),
                currentUserId,
                friendId,
                invite.getId(),
                fromUser.getUsername(),
                toUser.getUsername(),
                System.currentTimeMillis());
        roomEventPublisher.publishInviteCreated(event);
        // 同步直推 WS，避免仅依赖 MQ 消费导致邀请方成功但对方收不到弹窗
        notificationService.notifyUser(friendId, toWsPayload("room.invite.created", event));
        return invite;
    }

    @Override
    public List<cc.shturl.wa.demo.dto.resp.RoomInvitePendingResp> listPendingInvites(Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        List<RoomInvites> invites = roomInvitesMapper.selectList(Wrappers.<RoomInvites>lambdaQuery()
                .eq(RoomInvites::getToUserId, currentUserId)
                .eq(RoomInvites::getStatus, 0)
                .gt(RoomInvites::getExpiredAt, now)
                .orderByDesc(RoomInvites::getId));
        return invites.stream().map(invite -> {
            User from = userMapper.selectById(invite.getFromUserId());
            return new cc.shturl.wa.demo.dto.resp.RoomInvitePendingResp(
                    invite.getId(),
                    invite.getFromUserId(),
                    from == null ? "好友" : from.getUsername(),
                    invite.getToUserId(),
                    invite.getExpiredAt(),
                    invite.getCreatedAt());
        }).toList();
    }

    @Override
    public RoomDetailResp acceptInvite(Long currentUserId, Long inviteId) {
        RoomInvites invite = requireInvite(inviteId);
        if (!invite.getToUserId().equals(currentUserId)) {
            throw new BusinessException("无权处理该邀请");
        }
        if (invite.getStatus() != 0) {
            if (invite.getStatus() == 1 && invite.getRoomId() != null) {
                return getRoomDetail(currentUserId, invite.getRoomId());
            }
            throw new BusinessException("邀请已处理");
        }
        Long firstUserId = Math.min(invite.getFromUserId(), invite.getToUserId());
        Long secondUserId = Math.max(invite.getFromUserId(), invite.getToUserId());
        String firstLockKey = "room:user:" + firstUserId;
        String secondLockKey = "room:user:" + secondUserId;
        String firstLockValue = distributedLock.tryLock(firstLockKey, 10);
        if (firstLockValue == null) {
            throw new BusinessException("玩家房间状态正在变更，请稍后重试");
        }
        String secondLockValue = null;
        try {
            secondLockValue = distributedLock.tryLock(secondLockKey, 10);
            if (secondLockValue == null) {
                throw new BusinessException("玩家房间状态正在变更，请稍后重试");
            }
            // 先提交事务，再发 MQ/WS，避免邀请人 getRoomDetail 读到未提交数据
            RoomDetailResp detail = transactionTemplate.execute(status -> acceptInviteInTransaction(currentUserId, inviteId));
            publishRoomAcceptedEvents(inviteId);
            return detail;
        } finally {
            if (secondLockValue != null) {
                distributedLock.unlock(secondLockKey, secondLockValue);
            }
            distributedLock.unlock(firstLockKey, firstLockValue);
        }
    }

    private void publishRoomAcceptedEvents(Long inviteId) {
        RoomInvites invite = requireInvite(inviteId);
        if (invite.getStatus() != 1 || invite.getRoomId() == null) {
            return;
        }
        GameRooms room = gameRoomsMapper.selectById(invite.getRoomId());
        if (room == null) {
            return;
        }
        RoomInviteAcceptedEvent acceptedEvent = new RoomInviteAcceptedEvent(
                UUID.randomUUID().toString().replace("-", ""),
                invite.getFromUserId(), invite.getToUserId(), invite.getId(), room.getId(), room.getRoomCode(),
                System.currentTimeMillis());
        RoomCreatedEvent createdEvent = new RoomCreatedEvent(
                UUID.randomUUID().toString().replace("-", ""), room.getId(), room.getRoomCode(),
                room.getHostUserId(), invite.getToUserId(), System.currentTimeMillis());
        roomEventPublisher.publishInviteAccepted(acceptedEvent);
        roomEventPublisher.publishRoomCreated(createdEvent);
        // 直推双方 WS：线上 RabbitMQ listener 默认关闭时，邀请人否则收不到进房事件
        notificationService.notifyUsers(invite.getFromUserId(), invite.getToUserId(),
                toWsPayload("room.invite.accepted", acceptedEvent));
        notificationService.notifyUsers(room.getHostUserId(), invite.getToUserId(),
                toWsPayload("room.created", createdEvent));
        userPresenceService.broadcastPresence(invite.getFromUserId());
        userPresenceService.broadcastPresence(invite.getToUserId());
    }

    private RoomDetailResp acceptInviteInTransaction(Long currentUserId, Long inviteId) {
        RoomInvites invite = requireInvite(inviteId);
        if (!invite.getToUserId().equals(currentUserId)) {
            throw new BusinessException("无权处理该邀请");
        }
        if (invite.getStatus() != 0) {
            if (invite.getStatus() == 1 && invite.getRoomId() != null) {
                return getRoomDetail(currentUserId, invite.getRoomId());
            }
            throw new BusinessException("邀请已处理");
        }
        if (invite.getExpiredAt() != null && !invite.getExpiredAt().isAfter(LocalDateTime.now())) {
            invite.setStatus(3);
            invite.setRespondedAt(LocalDateTime.now());
            roomInvitesMapper.updateById(invite);
            throw new BusinessException("邀请已超时");
        }
        requireFriend(invite.getFromUserId(), invite.getToUserId());
        cleanupStaleMemberships(invite.getFromUserId());
        cleanupStaleMemberships(invite.getToUserId());
        requireOnlineAndIdle(invite.getFromUserId());
        requireOnlineAndIdle(invite.getToUserId());
        GameRooms room = createRoom(invite.getFromUserId());
        addRoomMember(room.getId(), invite.getFromUserId(), 1);
        addRoomMember(room.getId(), invite.getToUserId(), 2);
        room.setPlayerCount(2);
        room.setStatus(1);
        gameRoomsMapper.updateById(room);
        invite.setStatus(1);
        invite.setRespondedAt(LocalDateTime.now());
        invite.setRoomId(room.getId());
        roomInvitesMapper.updateById(invite);
        taskService.recordRoomFormation(invite.getFromUserId(), invite.getToUserId());
        taskService.recordRoomFormation(invite.getToUserId(), invite.getFromUserId());
        return getRoomDetail(currentUserId, room.getId());
    }

    @Override
    @Transactional
    public RoomInvites rejectInvite(Long currentUserId, Long inviteId) {
        RoomInvites invite = requireInvite(inviteId);
        if (!invite.getToUserId().equals(currentUserId)) {
            throw new BusinessException("无权处理该邀请");
        }
        if (invite.getStatus() != 0) {
            throw new BusinessException("邀请已处理");
        }
        invite.setStatus(2);
        invite.setRespondedAt(LocalDateTime.now());
        roomInvitesMapper.updateById(invite);
        roomEventPublisher.publishInviteRejected(new RoomInviteRejectedEvent(
                UUID.randomUUID().toString().replace("-", ""),
                invite.getFromUserId(),
                invite.getToUserId(),
                invite.getId(),
                System.currentTimeMillis()));
        return invite;
    }

    @Override
    @Transactional
    public RoomInvites timeoutInvite(Long inviteId) {
        RoomInvites invite = requireInvite(inviteId);
        if (invite.getStatus() != 0) {
            return invite;
        }
        if (invite.getExpiredAt() != null && invite.getExpiredAt().isAfter(LocalDateTime.now())) {
            return invite;
        }
        invite.setStatus(3);
        invite.setRespondedAt(LocalDateTime.now());
        roomInvitesMapper.updateById(invite);
        return invite;
    }

    @Override
    public RoomDetailResp getRoomDetail(Long currentUserId, Long roomId) {
        GameRooms room = gameRoomsMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }
        List<RoomMembers> members = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, roomId)
                .isNull(RoomMembers::getLeftAt)
                .orderByAsc(RoomMembers::getSeatNo, RoomMembers::getId));
        Map<Long, User> usersById = members.stream()
                .map(RoomMembers::getUserId)
                .filter(userId -> userId != null)
                .distinct()
                .map(userMapper::selectById)
                .filter(user -> user != null)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));
        List<RoomMemberResp> memberResps = members.stream()
                .map(member -> {
                    User user = usersById.get(member.getUserId());
                    String username = user != null && user.getUsername() != null
                            ? user.getUsername()
                            : "玩家" + member.getUserId();
                    return new RoomMemberResp(member.getId(), member.getRoomId(), member.getUserId(),
                            member.getSeatNo(), member.getDeptType(), member.getReadyStatus(), member.getOnlineStatus(),
                            username, username, user != null ? user.getAvatarUrl() : null);
                })
                .toList();
        return new RoomDetailResp(room.getId(), room.getRoomCode(), room.getHostUserId(), room.getStatus(),
                room.getPlayerCount(), room.getMaxPlayers(), room.getMatchId(), room.getClosedAt(), memberResps);
    }

    @Override
    public RoomDetailResp getCurrentRoom(Long currentUserId) {
        roomPresenceCleanupService.releaseStaleRooms(currentUserId);
        List<RoomMembers> memberships = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getUserId, currentUserId)
                .isNull(RoomMembers::getLeftAt)
                .orderByDesc(RoomMembers::getId));
        for (RoomMembers membership : memberships) {
            GameRooms room = gameRoomsMapper.selectById(membership.getRoomId());
            if (room == null || room.getClosedAt() != null || room.getStatus() == null || room.getStatus() >= 3) {
                continue;
            }
            return getRoomDetail(currentUserId, room.getId());
        }
        return null;
    }

    @Override
    public void releaseIdleRoom(Long currentUserId) {
        roomPresenceCleanupService.releaseIdleRooms(currentUserId);
    }

    @Override
    @Transactional
    public RoomDetailResp setDepartment(Long currentUserId, Long roomId, RoomDeptReq request) {
        requireOpenRoom(roomId);
        RoomMembers member = requireMember(roomId, currentUserId);
        String deptType = normalizeDeptType(request.deptType());
        member.setDeptType(deptType);
        roomMembersMapper.updateById(member);
        roomEventPublisher.publishDepartmentChanged(new RoomMemberDepartmentChangedEvent(
                UUID.randomUUID().toString().replace("-", ""),
                roomId,
                currentUserId,
                deptType,
                System.currentTimeMillis()));
        return getRoomDetail(currentUserId, roomId);
    }

    @Override
    @Transactional
    public RoomDetailResp setReady(Long currentUserId, Long roomId) {
        requireOpenRoom(roomId);
        clearFinishedMatchBinding(roomId);
        RoomMembers member = requireMember(roomId, currentUserId);
        if (member.getDeptType() == null || member.getDeptType().isBlank()) {
            throw new BusinessException("请先选择部门");
        }
        member.setReadyStatus(1);
        roomMembersMapper.updateById(member);
        RoomDetailResp detailResp = getRoomDetail(currentUserId, roomId);
        boolean allReady = detailResp.members().size() == 2
                && detailResp.members().stream().allMatch(item -> item.readyStatus() != null && item.readyStatus() == 1);
        roomEventPublisher.publishReadyChanged(new RoomMemberReadyEvent(
                UUID.randomUUID().toString().replace("-", ""),
                roomId,
                currentUserId,
                1,
                allReady,
                System.currentTimeMillis()));
        if (allReady) {
            matchService.initializeMatch(roomId);
            return getRoomDetail(currentUserId, roomId);
        }
        return detailResp;
    }

    @Override
    @Transactional
    public RoomDetailResp leaveRoom(Long currentUserId, Long roomId) {
        GameRooms room = gameRoomsMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }
        RoomMembers caller = roomMembersMapper.selectOne(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, roomId).eq(RoomMembers::getUserId, currentUserId));
        if (caller == null) {
            throw new BusinessException("无权退出该房间");
        }
        // 房间已关闭 / 对局已结束：允许清理退出，避免前端卡在“无法离开”
        if (Integer.valueOf(3).equals(room.getStatus()) || room.getClosedAt() != null || !isMatchActive(room.getMatchId())) {
            if (caller.getLeftAt() == null) {
                return closeRoom(roomId, currentUserId, "post_match_leave");
            }
            return getRoomDetail(currentUserId, roomId);
        }
        if (room.getMatchId() != null || Integer.valueOf(2).equals(room.getStatus())) {
            throw new BusinessException("对局已经开始，不能通过普通退出房间接口离开");
        }
        return closeRoom(roomId, currentUserId, "manual_leave");
    }

    private void requireOpenRoom(Long roomId) {
        GameRooms room = gameRoomsMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }
        if (Integer.valueOf(3).equals(room.getStatus()) || room.getClosedAt() != null) {
            throw new BusinessException("房间已关闭，请重新组队");
        }
        if (isMatchActive(room.getMatchId())) {
            throw new BusinessException("对局进行中，无法在房间内操作");
        }
    }

    private boolean isMatchActive(Long matchId) {
        if (matchId == null) {
            return false;
        }
        Matches match = matchesMapper.selectById(matchId);
        return match != null && Integer.valueOf(1).equals(match.getStatus());
    }

    /** 房间仍挂着已结束对局时，清掉绑定，避免再次准备复用旧 matchId */
    private void clearFinishedMatchBinding(Long roomId) {
        GameRooms room = gameRoomsMapper.selectById(roomId);
        if (room == null || room.getMatchId() == null) {
            return;
        }
        if (!isMatchActive(room.getMatchId())) {
            room.setMatchId(null);
            if (Integer.valueOf(2).equals(room.getStatus())) {
                room.setStatus(1);
            }
            gameRoomsMapper.updateById(room);
        }
    }

    private RoomMembers requireMember(Long roomId, Long userId) {
        RoomMembers member = roomMembersMapper.selectOne(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, roomId)
                .eq(RoomMembers::getUserId, userId)
                .isNull(RoomMembers::getLeftAt));
        if (member == null) {
            throw new BusinessException("房间成员不存在");
        }
        return member;
    }

    private String normalizeDeptType(String deptType) {
        if (deptType == null) {
            throw new BusinessException("部门类型不能为空");
        }
        String value = deptType.trim().toLowerCase();
        if (!DEPT_SALES.equals(value) && !DEPT_PURCHASE.equals(value)) {
            throw new BusinessException("部门类型不合法");
        }
        return value;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void requireFriend(Long currentUserId, Long friendId) {
        Friendships friendships = friendshipsMapper.selectOne(Wrappers.<Friendships>lambdaQuery()
                .eq(Friendships::getStatus, 1)
                .and(wrapper -> wrapper.eq(Friendships::getUserId, currentUserId)
                        .eq(Friendships::getFriendId, friendId)
                        .or()
                        .eq(Friendships::getUserId, friendId)
                        .eq(Friendships::getFriendId, currentUserId)));
        if (friendships == null) {
            throw new BusinessException("双方不是好友");
        }
    }

    private void requireOnlineAndIdle(Long userId) {
        userPresenceService.requireInvitable(userId);
    }

    private void requireInvitableUser(Long userId, String offlineMessage) {
        try {
            userPresenceService.requireInvitable(userId);
        } catch (BusinessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("不在线")) {
                throw new BusinessException(offlineMessage);
            }
            throw ex;
        }
    }

    private RoomInvites requireInvite(Long inviteId) {
        RoomInvites invite = roomInvitesMapper.selectById(inviteId);
        if (invite == null) {
            throw new BusinessException("邀请不存在");
        }
        return invite;
    }

    private RoomDetailResp closeRoom(Long roomId, Long closedByUserId, String reason) {
        LocalDateTime closedAt = LocalDateTime.now();
        GameRooms room = gameRoomsMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }
        List<RoomMembers> activeMembers = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, roomId).isNull(RoomMembers::getLeftAt));
        room.setStatus(3);
        room.setPlayerCount(0);
        room.setClosedAt(closedAt);
        gameRoomsMapper.updateById(room);
        for (RoomMembers member : activeMembers) {
            member.setLeftAt(closedAt);
            member.setReadyStatus(0);
            member.setOnlineStatus(0);
            roomMembersMapper.updateById(member);
        }
        List<RoomInvites> pendingInvites = roomInvitesMapper.selectList(Wrappers.<RoomInvites>lambdaQuery()
                .eq(RoomInvites::getRoomId, roomId).eq(RoomInvites::getStatus, 0));
        for (RoomInvites invite : pendingInvites) {
            invite.setStatus(3);
            invite.setRespondedAt(closedAt);
            roomInvitesMapper.updateById(invite);
        }
        if (!activeMembers.isEmpty()) {
            Long first = activeMembers.get(0).getUserId();
            Long second = activeMembers.size() > 1 ? activeMembers.get(1).getUserId() : first;
            notificationService.notifyUsers(first, second,
                    java.util.Map.of("type", "room.closed", "data", java.util.Map.of(
                            "roomId", roomId, "closedByUserId", closedByUserId, "reason", reason,
                            "closedAt", closedAt)));
            for (RoomMembers member : activeMembers) {
                userPresenceService.broadcastPresence(member.getUserId());
            }
        }
        return getRoomDetail(closedByUserId, roomId);
    }

    private void cleanupStaleMemberships(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<RoomMembers> memberships = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getUserId, userId).isNull(RoomMembers::getLeftAt));
        for (RoomMembers membership : memberships) {
            GameRooms room = gameRoomsMapper.selectById(membership.getRoomId());
            if (room == null || room.getClosedAt() != null || room.getStatus() == null || room.getStatus() >= 3) {
                membership.setLeftAt(now);
                membership.setOnlineStatus(0);
                roomMembersMapper.updateById(membership);
            }
        }
    }

    private GameRooms createRoom(Long hostUserId) {
        GameRooms room = new GameRooms();
        room.setRoomCode(generateRoomCode());
        room.setHostUserId(hostUserId);
        room.setStatus(0);
        room.setPlayerCount(0);
        room.setMaxPlayers(2);
        gameRoomsMapper.insert(room);
        return room;
    }

    private String generateRoomCode() {
        return String.valueOf(Math.abs((int) (System.currentTimeMillis() % 1_000_000)) + 100000);
    }

    private void addRoomMember(Long roomId, Long userId, Integer seatNo) {
        RoomMembers member = new RoomMembers();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setSeatNo(seatNo);
        member.setReadyStatus(0);
        member.setOnlineStatus(1);
        member.setJoinedAt(LocalDateTime.now());
        roomMembersMapper.insert(member);
    }

    private Map<String, Object> toWsPayload(String type, Object event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("data", event);
        return payload;
    }
}
