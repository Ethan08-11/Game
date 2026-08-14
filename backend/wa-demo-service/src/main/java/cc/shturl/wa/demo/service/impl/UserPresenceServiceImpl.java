package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.resp.UserPresenceResp;
import cc.shturl.wa.demo.entity.Friendships;
import cc.shturl.wa.demo.entity.GameRooms;
import cc.shturl.wa.demo.entity.Matches;
import cc.shturl.wa.demo.entity.RoomMembers;
import cc.shturl.wa.demo.enums.FriendshipStatus;
import cc.shturl.wa.demo.mapper.FriendshipsMapper;
import cc.shturl.wa.demo.mapper.GameRoomsMapper;
import cc.shturl.wa.demo.mapper.MatchesMapper;
import cc.shturl.wa.demo.mapper.RoomMembersMapper;
import cc.shturl.wa.demo.service.RoomNotificationService;
import cc.shturl.wa.demo.service.RoomWebSocketSessionService;
import cc.shturl.wa.demo.service.UserPresenceService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPresenceServiceImpl implements UserPresenceService {
    private static final String OFFLINE = "OFFLINE";
    private static final String IDLE = "IDLE";
    private static final String IN_ROOM = "IN_ROOM";
    private static final String IN_MATCH = "IN_MATCH";

    private final RoomWebSocketSessionService sessionService;
    private final RoomMembersMapper roomMembersMapper;
    private final GameRoomsMapper gameRoomsMapper;
    private final MatchesMapper matchesMapper;
    private final FriendshipsMapper friendshipsMapper;
    private final RoomNotificationService notificationService;

    @Override
    public UserPresenceResp getPresence(Long userId) {
        if (!sessionService.isOnline(userId)) {
            return new UserPresenceResp(userId, 0, OFFLINE, false);
        }
        String status = resolveBusinessStatus(userId);
        return new UserPresenceResp(userId, 1, status, IDLE.equals(status));
    }

    @Override
    public Map<Long, UserPresenceResp> getPresences(Collection<Long> userIds) {
        Map<Long, UserPresenceResp> result = new LinkedHashMap<>();
        if (userIds == null) {
            return result;
        }
        for (Long userId : new LinkedHashSet<>(userIds)) {
            if (userId != null) {
                result.put(userId, getPresence(userId));
            }
        }
        return result;
    }

    @Override
    public void requireInvitable(Long userId) {
        UserPresenceResp presence = getPresence(userId);
        switch (presence.presenceStatus()) {
            case OFFLINE -> throw new BusinessException("用户不在线，请确认双方均已登录并保持页面打开");
            case IN_ROOM -> throw new BusinessException("用户已在房间中");
            case IN_MATCH -> throw new BusinessException("用户正在对局中");
            default -> {
                if (!presence.invitable()) {
                    throw new BusinessException("用户当前不可邀请");
                }
            }
        }
    }

    @Override
    public void broadcastPresence(Long userId) {
        UserPresenceResp presence = getPresence(userId);
        Map<String, Object> payload = Map.of("type", "friend.presence.changed", "data", presence);
        for (Long friendId : findFriendIds(userId)) {
            notificationService.notifyUser(friendId, payload);
        }
    }

    private String resolveBusinessStatus(Long userId) {
        List<RoomMembers> memberships = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getUserId, userId)
                .isNull(RoomMembers::getLeftAt));
        boolean inRoom = false;
        for (RoomMembers membership : memberships) {
            GameRooms room = gameRoomsMapper.selectById(membership.getRoomId());
            if (room == null || room.getClosedAt() != null || room.getStatus() == null || room.getStatus() >= 3) {
                continue;
            }
            if (room.getMatchId() != null) {
                Matches match = matchesMapper.selectById(room.getMatchId());
                if (match != null && Integer.valueOf(1).equals(match.getStatus())
                        && match.getEndedAt() == null && !"FINISHED".equals(match.getPhase())) {
                    return IN_MATCH;
                }
            }
            if (room.getStatus() == 0 || room.getStatus() == 1) {
                inRoom = true;
            }
        }
        return inRoom ? IN_ROOM : IDLE;
    }

    private Set<Long> findFriendIds(Long userId) {
        List<Friendships> relations = friendshipsMapper.selectList(Wrappers.<Friendships>lambdaQuery()
                .eq(Friendships::getStatus, FriendshipStatus.ACCEPTED.getCode())
                .and(wrapper -> wrapper.eq(Friendships::getUserId, userId)
                        .or()
                        .eq(Friendships::getFriendId, userId)));
        Set<Long> result = new LinkedHashSet<>();
        for (Friendships relation : relations) {
            result.add(userId.equals(relation.getUserId()) ? relation.getFriendId() : relation.getUserId());
        }
        return result;
    }
}
