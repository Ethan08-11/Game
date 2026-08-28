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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RoomPresenceCleanupServiceImpl implements RoomPresenceCleanupService {
    private static final Logger log = LoggerFactory.getLogger(RoomPresenceCleanupServiceImpl.class);
    private static final long HEARTBEAT_TIMEOUT_MILLIS = 60_000L;
    private static final long DISCONNECT_GRACE_MILLIS = 8_000L;

    private final RoomMembersMapper roomMembersMapper;
    private final GameRoomsMapper gameRoomsMapper;
    private final MatchesMapper matchesMapper;
    private final RoomWebSocketSessionService sessionService;
    private final UserPresenceService userPresenceService;
    private final MatchService matchService;
    private final RoomNotificationService notificationService;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> pendingDisconnects = new ConcurrentHashMap<>();
    private final ScheduledExecutorService disconnectGraceExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ws-disconnect-grace");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void handleUserConnected(Long userId) {
        cancelPendingDisconnect(userId);
        try {
            matchService.recoverOnlinePlayer(userId);
        } catch (Exception e) {
            log.warn("Recover online player failed userId={}: {}", userId, e.getMessage());
        }
        userPresenceService.broadcastPresence(userId);
    }

    @Override
    public void handleUserDisconnected(Long userId, String reason) {
        // 刷新页面会先关旧连接再开新连接，给几秒宽限，避免被当成掉线判负
        if (sessionService.isOnline(userId)) {
            cancelPendingDisconnect(userId);
            userPresenceService.broadcastPresence(userId);
            return;
        }
        pendingDisconnects.compute(userId, (ignored, previous) -> {
            if (previous != null && !previous.isDone()) {
                return previous;
            }
            return disconnectGraceExecutor.schedule(() -> {
                pendingDisconnects.remove(userId);
                if (sessionService.isOnline(userId)) {
                    userPresenceService.broadcastPresence(userId);
                    return;
                }
                matchService.markPlayerDisconnected(userId);
                userPresenceService.broadcastPresence(userId);
            }, DISCONNECT_GRACE_MILLIS, TimeUnit.MILLISECONDS);
        });
    }

    private void cancelPendingDisconnect(Long userId) {
        ScheduledFuture<?> future = pendingDisconnects.remove(userId);
        if (future != null) {
            future.cancel(false);
        }
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
