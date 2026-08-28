package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.entity.MatchPlayers;
import cc.shturl.wa.demo.entity.Matches;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.mapper.MatchPlayersMapper;
import cc.shturl.wa.demo.mapper.MatchesMapper;
import cc.shturl.wa.demo.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MatchChatService {
    private static final Logger log = LoggerFactory.getLogger(MatchChatService.class);
    private static final int MAX_TEXT_LENGTH = 40;
    private static final long MIN_INTERVAL_MILLIS = 400L;

    private final MatchPlayersMapper matchPlayersMapper;
    private final MatchesMapper matchesMapper;
    private final UserMapper userMapper;
    private final RoomNotificationService notificationService;
    private final Map<Long, Long> lastSentAt = new ConcurrentHashMap<>();

    public void handleChat(Long userId, JsonNode payload) {
        if (userId == null || payload == null) {
            return;
        }
        String text = normalizeText(payload.path("text").asText(""));
        if (text.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastSentAt.get(userId);
        if (previous != null && now - previous < MIN_INTERVAL_MILLIS) {
            return;
        }
        lastSentAt.put(userId, now);

        Matches match = resolveLiveMatch(userId, payload.path("matchId").asText(""));
        if (match == null) {
            return;
        }
        Long teammateId = teammateId(match.getId(), userId);
        if (teammateId == null) {
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("messageId", UUID.randomUUID().toString());
        data.put("matchId", match.getId());
        data.put("fromUserId", userId);
        data.put("fromName", displayName(userId));
        data.put("text", text);
        data.put("timestamp", now);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "match.chat");
        message.put("data", data);
        notificationService.notifyUsers(userId, teammateId, message);
    }

    private Matches resolveLiveMatch(Long userId, String requestedMatchId) {
        if (requestedMatchId != null && !requestedMatchId.isBlank()) {
            try {
                Matches match = matchesMapper.selectById(Long.parseLong(requestedMatchId.trim()));
                if (isLiveMatch(match) && isPlayer(match.getId(), userId)) {
                    return match;
                }
            } catch (NumberFormatException ignored) {
                log.debug("Ignore invalid match chat id from user {}: {}", userId, requestedMatchId);
            }
        }
        List<MatchPlayers> seats = matchPlayersMapper.selectList(
                Wrappers.<MatchPlayers>lambdaQuery()
                        .eq(MatchPlayers::getUserId, userId)
                        .orderByDesc(MatchPlayers::getId)
                        .last("LIMIT 8"));
        for (MatchPlayers seat : seats) {
            Matches match = matchesMapper.selectById(seat.getMatchId());
            if (isLiveMatch(match)) {
                return match;
            }
        }
        return null;
    }

    private boolean isLiveMatch(Matches match) {
        return match != null
                && Integer.valueOf(1).equals(match.getStatus())
                && !"FINISHED".equals(match.getPhase());
    }

    private boolean isPlayer(Long matchId, Long userId) {
        return matchPlayersMapper.selectCount(
                Wrappers.<MatchPlayers>lambdaQuery()
                        .eq(MatchPlayers::getMatchId, matchId)
                        .eq(MatchPlayers::getUserId, userId)) > 0;
    }

    private Long teammateId(Long matchId, Long userId) {
        List<MatchPlayers> players = matchPlayersMapper.selectList(
                Wrappers.<MatchPlayers>lambdaQuery().eq(MatchPlayers::getMatchId, matchId));
        for (MatchPlayers player : players) {
            if (player.getUserId() != null && !player.getUserId().equals(userId)) {
                return player.getUserId();
            }
        }
        return null;
    }

    private String displayName(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return "队友";
        }
        return user.getUsername();
    }

    private static String normalizeText(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length() && builder.length() < MAX_TEXT_LENGTH; i++) {
            char ch = raw.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                builder.append(' ');
                continue;
            }
            if (!Character.isISOControl(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString().trim();
    }
}
