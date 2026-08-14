package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.entity.GameRooms;
import cc.shturl.wa.demo.entity.RoomMembers;
import cc.shturl.wa.demo.mapper.GameRoomsMapper;
import cc.shturl.wa.demo.mapper.RoomMembersMapper;
import cc.shturl.wa.demo.service.MatchService;
import cc.shturl.wa.demo.service.RoomPresenceCleanupService;
import cc.shturl.wa.demo.service.RoomWebSocketSessionService;
import cc.shturl.wa.demo.service.UserPresenceService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomPresenceCleanupServiceImpl implements RoomPresenceCleanupService {
    private static final long HEARTBEAT_TIMEOUT_MILLIS = 60_000L;

    private final RoomMembersMapper roomMembersMapper;
    private final GameRoomsMapper gameRoomsMapper;
    private final RoomWebSocketSessionService sessionService;
    private final UserPresenceService userPresenceService;
    private final MatchService matchService;

    @Override
    @Transactional
    public void handleUserDisconnected(Long userId, String reason) {
        cleanupUserMemberships(userId, reason);
        matchService.markPlayerDisconnected(userId);
        userPresenceService.broadcastPresence(userId);
    }

    @Override
    @Transactional
    public void cleanupExpiredRoomMembers() {
        List<RoomMembers> activeMembers = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .isNull(RoomMembers::getLeftAt));
        for (RoomMembers member : activeMembers) {
            if (sessionService.isStale(member.getUserId(), HEARTBEAT_TIMEOUT_MILLIS)) {
                cleanupUserMemberships(member.getUserId(), "heartbeat_timeout");
            }
        }
    }

    private void cleanupUserMemberships(Long userId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        List<RoomMembers> memberships = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getUserId, userId).isNull(RoomMembers::getLeftAt));
        for (RoomMembers membership : memberships) {
            GameRooms room = gameRoomsMapper.selectById(membership.getRoomId());
            if (room == null) {
                membership.setLeftAt(now);
                membership.setReadyStatus(0);
                membership.setOnlineStatus(0);
                roomMembersMapper.updateById(membership);
                continue;
            }
            if (room.getMatchId() != null || Integer.valueOf(2).equals(room.getStatus())) {
                continue;
            }
            membership.setLeftAt(now);
            membership.setReadyStatus(0);
            membership.setOnlineStatus(0);
            roomMembersMapper.updateById(membership);
            List<RoomMembers> remaining = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                    .eq(RoomMembers::getRoomId, room.getId()).isNull(RoomMembers::getLeftAt));
            if (remaining.isEmpty()) {
                room.setStatus(3);
                room.setPlayerCount(0);
                room.setClosedAt(now);
                gameRoomsMapper.updateById(room);
            } else {
                room.setPlayerCount(remaining.size());
                room.setStatus(0);
                gameRoomsMapper.updateById(room);
            }
        }
    }
}
