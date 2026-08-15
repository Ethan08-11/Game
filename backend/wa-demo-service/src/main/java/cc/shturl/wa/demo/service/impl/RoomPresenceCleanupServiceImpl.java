package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.entity.GameRooms;
import cc.shturl.wa.demo.entity.Matches;
import cc.shturl.wa.demo.entity.RoomMembers;
import cc.shturl.wa.demo.mapper.GameRoomsMapper;
import cc.shturl.wa.demo.mapper.MatchesMapper;
import cc.shturl.wa.demo.mapper.RoomMembersMapper;
import cc.shturl.wa.demo.service.MatchService;
import cc.shturl.wa.demo.service.RoomNotificationService;
import cc.shturl.wa.demo.service.RoomPresenceCleanupService;
import cc.shturl.wa.demo.service.RoomWebSocketSessionService;
import cc.shturl.wa.demo.service.UserPresenceService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomPresenceCleanupServiceImpl implements RoomPresenceCleanupService {
    private static final long HEARTBEAT_TIMEOUT_MILLIS = 60_000L;

    private final RoomMembersMapper roomMembersMapper;
    private final GameRoomsMapper gameRoomsMapper;
    private final MatchesMapper matchesMapper;
    private final RoomWebSocketSessionService sessionService;
    private final UserPresenceService userPresenceService;
    private final MatchService matchService;
    private final RoomNotificationService notificationService;

    @Override
    @Transactional
    public void handleUserDisconnected(Long userId, String reason) {
        // 刷新/重连会先建新连接再关旧连接，不能立刻踢出组队房间
        if (sessionService.isOnline(userId)) {
            userPresenceService.broadcastPresence(userId);
            return;
        }
        matchService.markPlayerDisconnected(userId);
        userPresenceService.broadcastPresence(userId);
    }

    @Override
    @Transactional
    public void cleanupExpiredRoomMembers() {
        List<RoomMembers> activeMembers = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .isNull(RoomMembers::getLeftAt));
        for (RoomMembers member : activeMembers) {
            if (member.getUserId() == null) {
                continue;
            }
            GameRooms room = gameRoomsMapper.selectById(member.getRoomId());
            if (isFinishedOrClosed(room)) {
                leaveMembership(member, room, "stale_room");
                continue;
            }
            if (sessionService.isOnline(member.getUserId())) {
                continue;
            }
            if (sessionService.isStale(member.getUserId(), HEARTBEAT_TIMEOUT_MILLIS)) {
                cleanupUserMemberships(member.getUserId(), "heartbeat_timeout");
            }
        }
    }

    @Override
    @Transactional
    public void releaseStaleRooms(Long userId) {
        List<RoomMembers> memberships = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getUserId, userId).isNull(RoomMembers::getLeftAt));
        for (RoomMembers membership : memberships) {
            GameRooms room = gameRoomsMapper.selectById(membership.getRoomId());
            if (isFinishedOrClosed(room)) {
                leaveMembership(membership, room, "stale_room");
            }
        }
        userPresenceService.broadcastPresence(userId);
    }

    @Override
    @Transactional
    public void releaseIdleRooms(Long userId) {
        cleanupUserMemberships(userId, "release_idle");
        userPresenceService.broadcastPresence(userId);
    }

    private void cleanupUserMemberships(Long userId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        List<RoomMembers> memberships = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getUserId, userId).isNull(RoomMembers::getLeftAt));
        for (RoomMembers membership : memberships) {
            GameRooms room = gameRoomsMapper.selectById(membership.getRoomId());
            if (isMatchStillActive(room)) {
                continue;
            }
            leaveMembership(membership, room, reason);
        }
    }

    private void leaveMembership(RoomMembers membership, GameRooms room, String reason) {
        LocalDateTime now = LocalDateTime.now();
        membership.setLeftAt(now);
        membership.setReadyStatus(0);
        membership.setOnlineStatus(0);
        roomMembersMapper.updateById(membership);
        if (room == null) {
            return;
        }
        List<RoomMembers> remaining = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, room.getId()).isNull(RoomMembers::getLeftAt));
        if (remaining.isEmpty() || !isMatchStillActive(room)) {
            room.setStatus(3);
            room.setPlayerCount(0);
            room.setClosedAt(now);
            gameRoomsMapper.updateById(room);
            for (RoomMembers member : remaining) {
                member.setLeftAt(now);
                member.setReadyStatus(0);
                member.setOnlineStatus(0);
                roomMembersMapper.updateById(member);
            }
            notifyRoomClosed(room.getId(), membership.getUserId(), reason, remaining);
        } else {
            room.setPlayerCount(remaining.size());
            room.setStatus(0);
            gameRoomsMapper.updateById(room);
        }
        userPresenceService.broadcastPresence(membership.getUserId());
        for (RoomMembers member : remaining) {
            userPresenceService.broadcastPresence(member.getUserId());
        }
    }

    private void notifyRoomClosed(Long roomId, Long closedByUserId, String reason, List<RoomMembers> remaining) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roomId", roomId);
        data.put("closedByUserId", closedByUserId);
        data.put("reason", reason);
        Map<String, Object> payload = Map.of("type", "room.closed", "data", data);
        notificationService.notifyUser(closedByUserId, payload);
        for (RoomMembers member : remaining) {
            if (member.getUserId() != null && !member.getUserId().equals(closedByUserId)) {
                notificationService.notifyUser(member.getUserId(), payload);
            }
        }
    }

    private boolean isFinishedOrClosed(GameRooms room) {
        if (room == null || room.getClosedAt() != null || room.getStatus() == null || room.getStatus() >= 3) {
            return true;
        }
        return room.getMatchId() != null && !isMatchStillActive(room);
    }

    private boolean isMatchStillActive(GameRooms room) {
        if (room == null || room.getMatchId() == null) {
            return false;
        }
        Matches match = matchesMapper.selectById(room.getMatchId());
        return match != null && Integer.valueOf(1).equals(match.getStatus())
                && match.getEndedAt() == null && !"FINISHED".equals(match.getPhase());
    }
}
