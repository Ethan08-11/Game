package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.req.EndTurnReq;
import cc.shturl.wa.demo.dto.req.MatchReviveReq;
import cc.shturl.wa.demo.dto.req.PlayCardReq;
import cc.shturl.wa.demo.dto.resp.BossAttackTargetResp;
import cc.shturl.wa.demo.dto.resp.CardEffectResp;
import cc.shturl.wa.demo.dto.resp.CustomerInfoResp;
import cc.shturl.wa.demo.dto.resp.EndTurnResp;
import cc.shturl.wa.demo.dto.resp.MatchActionResp;
import cc.shturl.wa.demo.dto.resp.MatchCardResp;
import cc.shturl.wa.demo.dto.resp.MatchDeckResp;
import cc.shturl.wa.demo.dto.resp.MatchFirstPlayerResp;
import cc.shturl.wa.demo.dto.resp.MatchPlayerStateResp;
import cc.shturl.wa.demo.dto.resp.MatchReviveResp;
import cc.shturl.wa.demo.dto.resp.MatchReviveStatusResp;
import cc.shturl.wa.demo.dto.resp.MatchSettlementResp;
import cc.shturl.wa.demo.dto.resp.MatchStateResp;
import cc.shturl.wa.demo.entity.Bullies;
import cc.shturl.wa.demo.entity.CardEffects;
import cc.shturl.wa.demo.entity.Cards;
import cc.shturl.wa.demo.entity.CustomerTypes;
import cc.shturl.wa.demo.entity.DeckCardConfigs;
import cc.shturl.wa.demo.entity.GameRooms;
import cc.shturl.wa.demo.entity.MatchActions;
import cc.shturl.wa.demo.entity.MatchCards;
import cc.shturl.wa.demo.entity.MatchPlayers;
import cc.shturl.wa.demo.entity.MatchReviveLog;
import cc.shturl.wa.demo.entity.MatchPendingEffects;
import cc.shturl.wa.demo.entity.MatchRounds;
import cc.shturl.wa.demo.entity.Matches;
import cc.shturl.wa.demo.entity.RoomMembers;
import cc.shturl.wa.demo.entity.UserCardPools;
import cc.shturl.wa.demo.mapper.BulliesMapper;
import cc.shturl.wa.demo.mapper.CardEffectsMapper;
import cc.shturl.wa.demo.mapper.CardsMapper;
import cc.shturl.wa.demo.mapper.CustomerTypesMapper;
import cc.shturl.wa.demo.mapper.DeckCardConfigsMapper;
import cc.shturl.wa.demo.mapper.GameRoomsMapper;
import cc.shturl.wa.demo.mapper.MatchActionsMapper;
import cc.shturl.wa.demo.mapper.MatchCardsMapper;
import cc.shturl.wa.demo.mapper.MatchPlayersMapper;
import cc.shturl.wa.demo.mapper.MatchReviveLogMapper;
import cc.shturl.wa.demo.mapper.MatchPendingEffectsMapper;
import cc.shturl.wa.demo.mapper.MatchRoundsMapper;
import cc.shturl.wa.demo.mapper.MatchesMapper;
import cc.shturl.wa.demo.mapper.RoomMembersMapper;
import cc.shturl.wa.demo.mapper.UserCardPoolsMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.CardCollectionService;
import cc.shturl.wa.demo.service.LeaderboardService;
import cc.shturl.wa.demo.service.MatchService;
import cc.shturl.wa.demo.service.RoomNotificationService;
import cc.shturl.wa.demo.service.UserPresenceService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
    private static final Logger logger = LoggerFactory.getLogger(MatchServiceImpl.class);
    private static final int PLAYER_COUNT = 2;
    private static final int DECK_SIZE = 30;
    private static final int DEPT_STARTER_UNIQUE_CAP = 10;
    private static final int DEPT_STARTER_COPIES = 2;
    private static final int DEPT_COLLECTIBLE_SLOTS = 8;
    private static final int SHARED_COLLECTIBLE_SLOTS = 6;
    private static final int SHARED_STARTER_SLOTS = 2;
    private static final int INITIAL_HAND_SIZE = 5;
    private static final String SALES = "sales";
    private static final String PURCHASE = "purchase";
    private static final String PLAYER_ACTION = "PLAYER_ACTION";
    private static final int VICTORY_EXP = 100;
    private static final int DEFEAT_EXP = 30;
    private static final long VICTORY_MONEY = 50L;
    private static final long DEFEAT_MONEY = 40L;
    private static final int BOSS_HP_MIN = 100;
    private static final int BOSS_HP_MAX = 110;
    private static final long RECONNECT_TIMEOUT_MILLIS = 60_000L;
    private static final long REVIVE_TIMEOUT_MILLIS = 90_000L;
    private static final long REVIVE_MAX_WAIT_MILLIS = 180_000L;
    private static final Set<String> KEEP_PHASE_ON_DISCONNECT = Set.of(
            "SELECT_FIRST_PLAYER", "BOSS_ACTION", "REVIVE_WAIT", "FINISHED");

    private final GameRoomsMapper gameRoomsMapper;
    private final RoomMembersMapper roomMembersMapper;
    private final MatchesMapper matchesMapper;
    private final MatchPlayersMapper matchPlayersMapper;
    private final MatchRoundsMapper matchRoundsMapper;
    private final MatchCardsMapper matchCardsMapper;
    private final MatchActionsMapper matchActionsMapper;
    private final CardEffectsMapper cardEffectsMapper;
    private final MatchPendingEffectsMapper matchPendingEffectsMapper;
    private final MatchReviveLogMapper matchReviveLogMapper;
    private final DeckCardConfigsMapper deckCardConfigsMapper;
    private final CardsMapper cardsMapper;
    private final CustomerTypesMapper customerTypesMapper;
    private final BulliesMapper bulliesMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserCardPoolsMapper userCardPoolsMapper;
    private final RoomNotificationService notificationService;
    private final UserPresenceService userPresenceService;
    private final cc.shturl.wa.demo.service.TaskService taskService;
    private final CardCollectionService cardCollectionService;
    private final PlatformTransactionManager transactionManager;
    private final LeaderboardService leaderboardService;

    @Override
    @Transactional
    public Long initializeMatch(Long roomId) {
        GameRooms room = requireRoom(roomId);
        if (room.getMatchId() != null) {
            Matches bound = matchesMapper.selectById(room.getMatchId());
            if (bound != null && value(bound.getStatus()) == 1) {
                return room.getMatchId();
            }
            // 绑定的是已结束对局，清掉后允许重新开局
            room.setMatchId(null);
            if (value(room.getStatus()) == 2) {
                room.setStatus(1);
            }
            gameRoomsMapper.updateById(room);
        }
        Matches existing = matchesMapper.selectOne(Wrappers.<Matches>lambdaQuery()
                .eq(Matches::getRoomId, roomId)
                .eq(Matches::getStatus, 1)
                .orderByDesc(Matches::getId)
                .last("LIMIT 1"));
        if (existing != null) {
            bindRoomToMatch(room, existing.getId());
            return existing.getId();
        }

        List<RoomMembers> members = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, roomId)
                .orderByAsc(RoomMembers::getSeatNo));
        validateReadyMembers(members);
        CustomerTypes customer = pickCustomer();
        Bullies bully = requireEnabledBully();

        Matches match = buildMatch(roomId, customer, bully);
        try {
            matchesMapper.insert(match);
        } catch (DuplicateKeyException exception) {
            Matches concurrentMatch = matchesMapper.selectOne(Wrappers.<Matches>lambdaQuery()
                    .eq(Matches::getRoomId, roomId));
            if (concurrentMatch == null) {
                throw exception;
            }
            bindRoomToMatch(room, concurrentMatch.getId());
            return concurrentMatch.getId();
        }

        for (RoomMembers member : members) {
            MatchPlayers player = createPlayer(match.getId(), member);
            createDeck(match.getId(), player);
        }
        createFirstRound(match, customer);
        bindRoomToMatch(room, match.getId());
        notifyMatchStarted(match, members);
        for (RoomMembers member : members) {
            userPresenceService.broadcastPresence(member.getUserId());
        }
        return match.getId();
    }

    @Override
    @Transactional
    public MatchStateResp getMatchState(Long currentUserId, Long matchId) {
        Matches match = requireMatch(matchId);
        requirePlayer(currentUserId, matchId);
        resumeStuckRoundIfNeeded(match);
        match = requireMatch(matchId);
        List<MatchPlayers> players = listPlayers(matchId);
        MatchRounds round = matchRoundsMapper.selectOne(Wrappers.<MatchRounds>lambdaQuery()
                .eq(MatchRounds::getMatchId, matchId)
                .eq(MatchRounds::getRoundNo, match.getCurrentRound()));
        CustomerTypes customer = customerTypesMapper.selectById(match.getCustomerTypeId());
        List<MatchCards> allCards = matchCardsMapper.selectList(Wrappers.<MatchCards>lambdaQuery()
                .eq(MatchCards::getMatchId, matchId));
        Map<Long, List<MatchCards>> cardsByUser = allCards.stream()
                .collect(Collectors.groupingBy(MatchCards::getUserId));
        List<MatchPlayerStateResp> playerStates = players.stream()
                .sorted(Comparator.comparing(MatchPlayers::getSeatNo))
                .map(player -> toPlayerState(player, cardsByUser.getOrDefault(player.getUserId(), List.of())))
                .toList();
        List<MatchCardResp> hand = toCardResponses(allCards.stream()
                .filter(card -> Objects.equals(currentUserId, card.getUserId()) && "HAND".equals(card.getZone()))
                .toList());
        MatchPlayers firstPlayer = players.stream().filter(player -> value(player.getEndedTurn()) == 0)
                .sorted(Comparator.comparing(MatchPlayers::getSeatNo)).findFirst().orElse(null);
        MatchPlayers reconnectingPlayer = players.stream().filter(player -> "RECONNECTING".equals(player.getPlayerStatus()))
                .findFirst().orElse(null);
        boolean waitingReconnect = "RECONNECT_WAIT".equals(match.getPhase()) || reconnectingPlayer != null;
        Integer reconnectRemainingSeconds = null;
        if (waitingReconnect && reconnectingPlayer != null && reconnectingPlayer.getUpdatedAt() != null) {
            long elapsedMillis = java.time.Duration.between(reconnectingPlayer.getUpdatedAt(), LocalDateTime.now()).toMillis();
            long remainMillis = Math.max(RECONNECT_TIMEOUT_MILLIS - elapsedMillis, 0L);
            reconnectRemainingSeconds = (int) Math.ceil(remainMillis / 1000.0);
        }
        return new MatchStateResp(match.getId(), match.getMatchCode(), match.getRoomId(), match.getStatus(),
                match.getPhase(), match.getCurrentRound(), match.getVersion(), toCustomerResp(customer),
                match.getBullyId(), match.getBossName(), match.getBossMaxHp(), match.getBossCurrentHp(),
                match.getBossBaseAttack(), match.getBossCurrentAttack(), round == null ? 0 : round.getCustomerTriggered(),
                round == null ? null : round.getCustomerEffectType(), round == null ? 0 : round.getCustomerEffectValue(),
                round == null ? null : round.getFirstPlayerUserId(), round == null ? null : round.getChosenByUserId(),
                waitingReconnect, reconnectRemainingSeconds, playerStates, hand, match.getWinnerType());
    }

    @Override
    @Transactional
    public MatchReviveStatusResp getReviveStatus(Long currentUserId, Long matchId) {
        Matches match = requireMatch(matchId);
        MatchPlayers player = requirePlayer(currentUserId, matchId);
        boolean reviveEnabled = true;
        boolean canRevive = reviveEnabled && value(match.getStatus()) == 1 && value(player.getCurrentHp()) <= 0
                && value(player.getReviveCount()) < value(player.getReviveLimit());
        String message;
        if (!reviveEnabled) {
            message = "当前对局未开启广告复活";
        } else if (value(match.getStatus()) != 1 || (!PLAYER_ACTION.equals(match.getPhase()) && !"REVIVE_WAIT".equals(match.getPhase()))) {
            message = "当前对局不在可复活阶段";
        } else if (value(player.getCurrentHp()) > 0) {
            message = "当前玩家未死亡";
        } else if (value(player.getReviveCount()) >= value(player.getReviveLimit())) {
            message = "本局复活次数已用尽";
        } else {
            message = "可以观看广告复活";
        }
        Integer remainingSeconds = null;
        if (canRevive && "REVIVE_WAIT".equals(match.getPhase())) {
            LocalDateTime heartbeatAt = LocalDateTime.now();
            matchPlayersMapper.update(null, Wrappers.<MatchPlayers>lambdaUpdate()
                    .eq(MatchPlayers::getId, player.getId())
                    .eq(MatchPlayers::getMatchId, matchId)
                    .set(MatchPlayers::getReviveStatus, 1)
                    .set(MatchPlayers::getUpdatedAt, heartbeatAt));
            player.setUpdatedAt(heartbeatAt);
            remainingSeconds = remainingReviveSeconds(match, player, heartbeatAt);
        }
        return new MatchReviveStatusResp(matchId, currentUserId, reviveEnabled, canRevive, value(player.getReviveCount()),
                value(player.getReviveLimit()), value(player.getCurrentHp()), value(player.getMaxHp()), remainingSeconds, player.getLastReviveAt(),
                player.getReviveStatus(), message);
    }

    @Override
    @Scheduled(
            fixedDelayString = "${app.match.reconnect-timeout-check-ms:5000}",
            initialDelayString = "${app.match.reconnect-timeout-initial-delay-ms:15000}")
    public void timeoutReconnectMatches() {
        List<MatchPlayers> reconnecting = matchPlayersMapper.selectList(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getPlayerStatus, "RECONNECTING"));
        LocalDateTime now = LocalDateTime.now();
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        for (MatchPlayers player : reconnecting) {
            if (player.getMatchId() == null || player.getUserId() == null) {
                continue;
            }
            if (player.getUpdatedAt() != null
                    && java.time.Duration.between(player.getUpdatedAt(), now).toMillis() < RECONNECT_TIMEOUT_MILLIS) {
                continue;
            }
            try {
                template.executeWithoutResult(status -> settleExpiredReconnect(player.getMatchId(), player.getUserId()));
            } catch (Exception e) {
                logger.warn("Reconnect timeout settlement failed matchId={} userId={}: {}",
                        player.getMatchId(), player.getUserId(), e.getMessage());
            }
        }
    }

    private void settleExpiredReconnect(Long matchId, Long userId) {
        Matches match = matchesMapper.selectById(matchId);
        if (match == null || value(match.getStatus()) != 1 || "FINISHED".equals(match.getPhase()) || "REVIVE_WAIT".equals(match.getPhase())) {
            return;
        }
        MatchPlayers player = matchPlayersMapper.selectOne(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getMatchId, matchId)
                .eq(MatchPlayers::getUserId, userId)
                .eq(MatchPlayers::getPlayerStatus, "RECONNECTING")
                .last("LIMIT 1"));
        if (player == null) {
            return;
        }
        if (player.getUpdatedAt() != null
                && java.time.Duration.between(player.getUpdatedAt(), LocalDateTime.now()).toMillis() < RECONNECT_TIMEOUT_MILLIS) {
            return;
        }
        player.setPlayerStatus("LEFT");
        player.setResultType(2);
        matchPlayersMapper.updateById(player);
        finishMatch(match, 2, false);
        notifyPlayers(match.getId(), "match.ended", Map.of(
                "matchId", match.getId(),
                "winnerType", 2,
                "reason", "reconnect_timeout"
        ));
        userPresenceService.broadcastPresence(userId);
    }

    @Override
    @Scheduled(
            fixedDelayString = "${app.match.revive-timeout-check-ms:5000}",
            initialDelayString = "${app.match.revive-timeout-initial-delay-ms:15000}")
    public void timeoutReviveMatches() {
        List<Matches> waitingMatches = matchesMapper.selectList(Wrappers.<Matches>lambdaQuery()
                .eq(Matches::getStatus, 1).eq(Matches::getPhase, "REVIVE_WAIT"));
        LocalDateTime now = LocalDateTime.now();
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        for (Matches waiting : waitingMatches) {
            try {
                template.executeWithoutResult(status -> settleExpiredReviveMatch(waiting.getId(), now));
            } catch (Exception e) {
                logger.warn("Revive timeout settlement failed matchId={}: {}", waiting.getId(), e.getMessage());
            }
        }
    }

    private void settleExpiredReviveMatch(Long matchId, LocalDateTime now) {
        Matches match = matchesMapper.selectById(matchId);
        if (match == null || value(match.getStatus()) != 1 || !"REVIVE_WAIT".equals(match.getPhase())) {
            return;
        }
        List<MatchPlayers> players = listPlayers(match.getId());
        if (players.stream().noneMatch(player -> value(player.getCurrentHp()) <= 0)) {
            return;
        }
        List<MatchPlayers> waiting = players.stream()
                .filter(player -> value(player.getCurrentHp()) <= 0)
                .filter(player -> value(player.getReviveCount()) < value(player.getReviveLimit()))
                .toList();
        boolean timedOut = revivePhaseMaxExceeded(match, now)
                || waiting.isEmpty()
                || waiting.stream().noneMatch(player -> !reviveWaitExpired(player, now));
        if (!timedOut) {
            return;
        }
        finishMatch(match, 2);
        notifyPlayers(match.getId(), "match.ended", Map.of(
                "matchId", match.getId(), "winnerType", 2, "reason", "revive_timeout"));
    }

    private boolean reviveWaitExpired(MatchPlayers player, LocalDateTime now) {
        if (player.getUpdatedAt() == null) {
            return false;
        }
        return java.time.Duration.between(player.getUpdatedAt(), now).toMillis() >= REVIVE_TIMEOUT_MILLIS;
    }

    private boolean revivePhaseMaxExceeded(Matches match, LocalDateTime now) {
        if (match.getUpdatedAt() == null) {
            return false;
        }
        return java.time.Duration.between(match.getUpdatedAt(), now).toMillis() >= REVIVE_MAX_WAIT_MILLIS;
    }

    private int remainingReviveSeconds(Matches match, MatchPlayers player, LocalDateTime now) {
        long idleElapsed = player.getUpdatedAt() == null
                ? 0L
                : Math.max(java.time.Duration.between(player.getUpdatedAt(), now).toMillis(), 0L);
        long maxElapsed = match.getUpdatedAt() == null
                ? 0L
                : Math.max(java.time.Duration.between(match.getUpdatedAt(), now).toMillis(), 0L);
        long idleLeft = Math.max(REVIVE_TIMEOUT_MILLIS - idleElapsed, 0L);
        long maxLeft = Math.max(REVIVE_MAX_WAIT_MILLIS - maxElapsed, 0L);
        return (int) (Math.min(idleLeft, maxLeft) / 1000L);
    }

    @Override
    @Transactional
    public void declineRevive(Long currentUserId, Long matchId) {
        Matches match = requireMatch(matchId);
        if (value(match.getStatus()) != 1) {
            return;
        }
        MatchPlayers player = requirePlayer(currentUserId, matchId);
        if (value(player.getCurrentHp()) > 0) {
            return;
        }
        player.setReviveStatus(0);
        matchPlayersMapper.updateById(player);
        if (listPlayers(matchId).stream().anyMatch(item -> value(item.getCurrentHp()) <= 0)) {
            finishMatch(match, 2);
            notifyPlayers(matchId, "match.ended", Map.of(
                    "matchId", matchId, "winnerType", 2, "reason", "revive_declined"));
        }
    }

    @Override
    @Transactional
    public MatchReviveResp requestRevive(Long currentUserId, Long matchId, MatchReviveReq request) {
        if (!currentUserId.equals(request.userId())) {
            throw new BusinessException("无权为其他玩家申请复活");
        }
        Matches match = requireMatch(matchId);
        if (value(match.getStatus()) != 1 || (!PLAYER_ACTION.equals(match.getPhase()) && !"REVIVE_WAIT".equals(match.getPhase()))) {
            throw new BusinessException("当前对局不在可复活阶段");
        }
        MatchPlayers player = requirePlayer(currentUserId, matchId);
        if (value(player.getCurrentHp()) > 0) {
            throw new BusinessException("当前玩家未死亡，无需复活");
        }
        if (value(player.getReviveCount()) >= value(player.getReviveLimit())) {
            throw new BusinessException("本局复活次数已用尽");
        }
        int beforeHp = value(player.getCurrentHp());
        int reviveHp = ThreadLocalRandom.current().nextInt(10, 21);
        reviveHp = Math.min(reviveHp, value(player.getMaxHp()));
        player.setCurrentHp(reviveHp);
        player.setPlayerStatus("ACTIVE");
        player.setReviveCount(value(player.getReviveCount()) + 1);
        player.setLastReviveAt(LocalDateTime.now());
        player.setReviveStatus(2);
        matchPlayersMapper.updateById(player);

        List<MatchPlayers> players = listPlayers(matchId);
        for (MatchPlayers participant : players) {
            int handCount = countCards(matchId, participant.getUserId(), "HAND");
            if (handCount < INITIAL_HAND_SIZE) {
                drawCards(matchId, participant.getUserId(), match.getCurrentRound(), INITIAL_HAND_SIZE - handCount);
            }
            participant.setEndedTurn(0);
            if (value(participant.getCurrentHp()) > 0) {
                participant.setPlayerStatus("ACTIVE");
                participant.setActionPoints(value(participant.getBaseActionPoints()));
            }
            matchPlayersMapper.updateById(participant);
        }
        if ("REVIVE_WAIT".equals(match.getPhase())) {
            match.setPhase(PLAYER_ACTION);
        }

        MatchReviveLog log = new MatchReviveLog();
        log.setMatchId(matchId);
        MatchRounds round = currentRound(match);
        log.setRoundNo(round == null ? match.getCurrentRound() : round.getRoundNo());
        log.setUserId(currentUserId);
        log.setBeforeHp(beforeHp);
        log.setAfterHp(player.getCurrentHp());
        log.setStatus(1);
        log.setAdPlatform(request.adPlatform() == null || request.adPlatform().isBlank() ? "manual" : request.adPlatform());
        log.setAdRequestId(request.adRequestId());
        log.setAdCallbackRaw(request.adCallbackRaw());
        log.setVerifyStatus(1);
        log.setReviveReason(request.reviveReason());
        matchReviveLogMapper.insert(log);

        match.setVersion(match.getVersion() + 1);
        matchesMapper.updateById(match);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("matchId", matchId);
        data.put("userId", currentUserId);
        data.put("beforeHp", beforeHp);
        data.put("afterHp", player.getCurrentHp());
        data.put("reviveCount", player.getReviveCount());
        data.put("reviveStatus", player.getReviveStatus());
        data.put("currentRound", match.getCurrentRound());
        data.put("version", match.getVersion());
        data.put("phase", match.getPhase());
        notifyPlayers(matchId, "match.revive.success", data);
        userPresenceService.broadcastPresence(currentUserId);
        return new MatchReviveResp(matchId, currentUserId, beforeHp, player.getCurrentHp(), player.getReviveCount(),
                player.getReviveStatus(), match.getCurrentRound(), match.getVersion(), player.getLastReviveAt(), "复活成功");
    }

    @Override
    public MatchDeckResp getMatchDeck(Long currentUserId, Long matchId) {
        requirePlayerAndList(currentUserId, matchId);
        List<MatchCards> cards = matchCardsMapper.selectList(Wrappers.<MatchCards>lambdaQuery()
                .eq(MatchCards::getMatchId, matchId)
                .eq(MatchCards::getUserId, currentUserId)
                .orderByAsc(MatchCards::getId));
        return new MatchDeckResp(matchId, currentUserId, cards.size(), toCardResponses(cards));
    }

    @Override
    public MatchSettlementResp getMatchSettlement(Long currentUserId, Long matchId) {
        Matches match = requireMatch(matchId);
        List<MatchPlayers> players = requirePlayerAndList(currentUserId, matchId);
        if (value(match.getStatus()) != 2 || value(match.getWinnerType()) == 0) {
            throw new BusinessException("对局尚未结束，暂时无法查看结算");
        }
        List<MatchActions> unlockActions = matchActionsMapper.selectList(Wrappers.<MatchActions>lambdaQuery()
                .eq(MatchActions::getMatchId, matchId)
                .eq(MatchActions::getActionType, "unlock_card"));
        Map<Long, Cards> unlockedByUser = new LinkedHashMap<>();
        for (MatchActions action : unlockActions) {
            if (action.getActorUserId() == null || action.getCardId() == null) {
                continue;
            }
            Cards unlocked = cardsMapper.selectById(action.getCardId());
            if (unlocked != null) {
                unlockedByUser.put(action.getActorUserId(), unlocked);
            }
        }
        List<MatchSettlementResp.PlayerSettlement> settlements = players.stream()
                .sorted(Comparator.comparing(MatchPlayers::getSeatNo))
                .map(player -> toPlayerSettlement(player, resolveMatchUnlock(player.getUserId(), unlockedByUser, match), match.getWinnerType()))
                .toList();
        return new MatchSettlementResp(match.getId(), match.getMatchCode(), match.getWinnerType(),
                value(match.getWinnerType()) == 1, match.getCurrentRound(), match.getDurationSeconds(),
                match.getBossMaxHp(), match.getBossCurrentHp(), settlements);
    }

    private Cards resolveMatchUnlock(Long userId, Map<Long, Cards> unlockedByUser, Matches match) {
        Cards unlocked = userId == null ? null : unlockedByUser.get(userId);
        if (unlocked != null) {
            return unlocked;
        }
        if (userId == null || value(match.getWinnerType()) != 1 || match.getEndedAt() == null) {
            return null;
        }
        List<UserCardPools> rows = userCardPoolsMapper.selectList(Wrappers.<UserCardPools>lambdaQuery()
                .eq(UserCardPools::getUserId, userId)
                .eq(UserCardPools::getUnlockedStatus, 1)
                .ge(UserCardPools::getCreatedAt, match.getEndedAt().minusSeconds(15))
                .le(UserCardPools::getCreatedAt, match.getEndedAt().plusSeconds(90))
                .orderByDesc(UserCardPools::getId)
                .last("LIMIT 1"));
        if (rows == null || rows.isEmpty() || rows.get(0).getCardId() == null) {
            return null;
        }
        return cardsMapper.selectById(rows.get(0).getCardId());
    }

    private MatchSettlementResp.PlayerSettlement toPlayerSettlement(MatchPlayers player, Cards unlocked, Integer winnerType) {
        int winner = value(winnerType);
        return new MatchSettlementResp.PlayerSettlement(
                player.getUserId(), player.getSeatNo(), player.getDeptType(), player.getResultType(),
                player.getMaxHp(), player.getCurrentHp(), player.getDamageDealt(), player.getDamageTaken(),
                player.getHealingDone(), player.getShieldGranted(), player.getCardsPlayedCount(),
                player.getTotalFundsUsed(), rewardExp(winner), rewardMoney(winner),
                unlocked == null ? null : unlocked.getId(),
                unlocked == null ? null : unlocked.getCardName(),
                unlocked == null ? null : unlocked.getImageUrl(),
                unlocked == null ? null : unlocked.getDeptType(),
                unlocked == null ? null : unlocked.getCost(),
                unlocked == null ? null : unlocked.getCardType(),
                unlocked == null ? null : unlocked.getDescription());
    }

    @Override
    @Transactional
    public MatchActionResp playCard(Long currentUserId, Long matchId, PlayCardReq request) {
        Matches match = requireMatch(matchId);
        if (match.getStatus() == null || match.getStatus() != 1 || !PLAYER_ACTION.equals(match.getPhase())) {
            throw new BusinessException("当前对局阶段不允许出牌");
        }
        MatchActions duplicateAction = matchActionsMapper.selectOne(Wrappers.<MatchActions>lambdaQuery()
                .eq(MatchActions::getMatchId, matchId)
                .eq(MatchActions::getActorUserId, currentUserId)
                .like(MatchActions::getExtraData, "\"clientActionId\":\"" + request.clientActionId() + "\"")
                .last("LIMIT 1"));
        if (duplicateAction != null) {
            throw new BusinessException("该出牌请求已处理，请刷新对局状态");
        }
        if (!request.expectedVersion().equals(match.getVersion())) {
            throw new BusinessException("对局状态已更新，请刷新后重试");
        }
        MatchPlayers actor = matchPlayersMapper.selectOne(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getMatchId, matchId)
                .eq(MatchPlayers::getUserId, currentUserId));
        if (actor == null) {
            throw new BusinessException("无权操作该对局");
        }
        if (actor.getCurrentHp() == null || actor.getCurrentHp() <= 0 || !"ACTIVE".equals(actor.getPlayerStatus())) {
            throw new BusinessException("当前玩家无法出牌");
        }
        if (actor.getEndedTurn() != null && actor.getEndedTurn() == 1) {
            throw new BusinessException("本回合已经结束，不能继续出牌");
        }
        MatchCards instance = matchCardsMapper.selectById(request.cardInstanceId());
        if (instance == null || !matchId.equals(instance.getMatchId()) || !currentUserId.equals(instance.getUserId())
                || !"HAND".equals(instance.getZone())) {
            throw new BusinessException("卡牌不在当前玩家手牌中");
        }
        Cards card = cardsMapper.selectById(instance.getCardId());
        if (card == null || card.getStatus() == null || card.getStatus() != 1) {
            throw new BusinessException("卡牌不存在或已停用");
        }
        if (!canUseCardInCurrentDept(actor.getDeptType(), card.getDeptType())) {
            throw new BusinessException("当前部门不能使用该卡牌");
        }
        int cost = value(card.getCost());
        if (value(actor.getActionPoints()) < cost) {
            throw new BusinessException("员工调用机会不足");
        }

        List<CardEffects> configuredEffects = cardEffectsMapper.selectList(Wrappers.<CardEffects>lambdaQuery()
                .eq(CardEffects::getCardId, card.getId()).orderByAsc(CardEffects::getEffectOrder, CardEffects::getId));
        if (configuredEffects.isEmpty()) {
            throw new BusinessException("该卡牌没有配置结构化效果");
        }
        boolean requiresPlayerTarget = configuredEffects.stream()
                .anyMatch(effect -> "ANY_PLAYER".equals(effect.getEffectScope()));
        MatchPlayers target = null;
        if (requiresPlayerTarget) {
            if (request.targetUserId() == null) {
                throw new BusinessException("该卡牌必须选择玩家目标");
            }
            target = matchPlayersMapper.selectOne(Wrappers.<MatchPlayers>lambdaQuery()
                    .eq(MatchPlayers::getMatchId, matchId).eq(MatchPlayers::getUserId, request.targetUserId()));
            if (target == null || value(target.getCurrentHp()) <= 0) {
                throw new BusinessException("目标玩家不合法");
            }
        }
        Bullies bully = requireBully(match.getBullyId());
        boolean appliesNumericEffects = configuredEffects.stream()
                .anyMatch(effect -> !"MULTIPLY_NEXT_CARD".equals(effect.getEffectType()));
        MatchPendingEffects multiplierEffect = appliesNumericEffects
                ? findNextCardMultiplier(matchId, currentUserId) : null;
        int multiplier = multiplierEffect == null ? 1 : Math.max(value(multiplierEffect.getEffectValue()), 1);
        List<CardEffectResp> effectResults = new ArrayList<>();
        int actionBeforeEffect = value(actor.getActionPoints());
        // 先扣费用，再结算立即效果，确保 ADD_ACTION_POINTS（如 Dylan）能真正增加剩余调用机会
        actor.setActionPoints(actionBeforeEffect - cost);
        actor.setTotalFundsUsed(value(actor.getTotalFundsUsed()) + cost);
        actor.setCardsPlayedCount(value(actor.getCardsPlayedCount()) + 1);
        for (CardEffects effect : configuredEffects) {
            if ("IMMEDIATE".equals(effect.getTriggerTiming())) {
                applyImmediateEffect(match, bully, actor, target, effect, multiplier, effectResults);
            }
        }
        schedulePendingEffects(match, actor, instance, target, configuredEffects, multiplier, effectResults);
        if (multiplierEffect != null) {
            multiplierEffect.setRemainingTriggers(0);
            multiplierEffect.setStatus("RESOLVED");
            matchPendingEffectsMapper.updateById(multiplierEffect);
        }
        matchPlayersMapper.updateById(actor);
        int damageDealt = 0;
        for (CardEffectResp effect : effectResults) {
            if ("DAMAGE_BOSS".equals(effect.effectType()) && !effect.scheduled() && effect.actualValue() != null) {
                damageDealt += Math.max(effect.actualValue(), 0);
            }
        }
        try {
            taskService.recordBattleAction(currentUserId, card.getDeptType(), 1, damageDealt);
        } catch (Exception e) {
            logger.warn("Skip battle task progress userId={} matchId={}: {}", currentUserId, matchId, e.getMessage());
        }
        instance.setZone("DISCARD");
        instance.setDeckOrder(null);
        instance.setDiscardedRound(match.getCurrentRound());
        instance.setVersion(value(instance.getVersion()) + 1);
        matchCardsMapper.updateById(instance);

        boolean bossDefeated = value(match.getBossCurrentHp()) <= 0;
        boolean playersDown = !bossDefeated && allPlayersDown(match.getId());
        if (bossDefeated) {
            finishMatch(match, 1);
        } else if (playersDown) {
            finishMatch(match, 2);
        }
        final boolean matchEnded = bossDefeated || playersDown;
        match.setVersion(match.getVersion() + 1);
        matchesMapper.updateById(match);

        MatchRounds round = matchRoundsMapper.selectOne(Wrappers.<MatchRounds>lambdaQuery()
                .eq(MatchRounds::getMatchId, matchId)
                .eq(MatchRounds::getRoundNo, match.getCurrentRound()));
        MatchActions action = new MatchActions();
        action.setMatchId(matchId);
        action.setRoundId(round == null ? null : round.getId());
        action.setActorType("player");
        action.setActorUserId(currentUserId);
        action.setActionType("play_card");
        action.setCardId(card.getId());
        action.setTargetUserId(target == null ? null : target.getUserId());
        action.setBeforeValue(actionBeforeEffect);
        action.setAfterValue(actor.getActionPoints());
        action.setDeltaValue(actor.getActionPoints() - actionBeforeEffect);
        action.setExtraData("{\"clientActionId\":\"" + request.clientActionId() + "\",\"cardInstanceId\":"
                + instance.getId() + ",\"effectCount\":" + effectResults.size()
                + ",\"appliedMultiplier\":" + multiplier + "}");
        matchActionsMapper.insert(action);

        MatchActionResp response = new MatchActionResp(matchId, action.getId(), request.clientActionId(), "PLAY_CARD",
                currentUserId, instance.getId(), card.getId(), card.getCardName(),
                resolveResponseTargetType(configuredEffects, requiresPlayerTarget),
                target == null ? null : target.getUserId(),
                actor.getActionPoints(), multiplier, effectResults,
                match.getVersion(), matchEnded, match.getWinnerType());
        notifyAfterCommit(() -> {
            notifyCardPlayed(matchId, response);
            if (matchEnded) {
                notifyMatchEnded(match, response);
            }
        });
        return response;
    }

    private Matches buildMatch(Long roomId, CustomerTypes customer, Bullies bully) {
        Matches match = new Matches();
        match.setMatchCode(UUID.randomUUID().toString().replace("-", ""));
        match.setRoomId(roomId);
        match.setCustomerTypeId(customer.getId());
        match.setBullyId(bully.getId());
        match.setBossName(bully.getBullyName());
        match.setBossSatisfactionTarget(0);
        match.setBossInitialSatisfaction(0);
        match.setBossFinalSatisfaction(0);
        match.setStatus(1);
        match.setPhase(PLAYER_ACTION);
        match.setCurrentRound(1);
        int bossHp = ThreadLocalRandom.current().nextInt(BOSS_HP_MIN, BOSS_HP_MAX + 1);
        match.setBossMaxHp(bossHp);
        match.setBossCurrentHp(bossHp);
        match.setBossBaseAttack(bully.getAttackPower());
        match.setBossCurrentAttack(bully.getAttackPower());
        match.setWinnerType(0);
        match.setVersion(1L);
        match.setDurationSeconds(0);
        match.setStartedAt(LocalDateTime.now());
        return match;
    }

    private MatchPlayers createPlayer(Long matchId, RoomMembers member) {
        int maxHp = SALES.equals(member.getDeptType()) ? 50 : 75;
        MatchPlayers player = new MatchPlayers();
        player.setMatchId(matchId);
        player.setUserId(member.getUserId());
        player.setSeatNo(member.getSeatNo());
        player.setDeptType(member.getDeptType());
        player.setMaxHp(maxHp);
        player.setCurrentHp(maxHp);
        player.setShield(0);
        player.setBaseActionPoints(3);
        player.setActionPoints(3);
        player.setEndedTurn(0);
        player.setPlayerStatus("ACTIVE");
        player.setInitialConfidence(0);
        player.setFinalConfidence(0);
        player.setInitialFunds(3);
        player.setTotalFundsUsed(0);
        player.setCardsPlayedCount(0);
        player.setDamageDealt(0);
        player.setDamageTaken(0);
        player.setHealingDone(0);
        player.setShieldGranted(0);
        player.setResultType(0);
        player.setReviveCount(0);
        player.setReviveLimit(1);
        player.setReviveStatus(0);
        matchPlayersMapper.insert(player);
        return player;
    }

    private void createDeck(Long matchId, MatchPlayers player) {
        List<Long> cardIds = buildUnlockedDeckCardIds(player.getUserId(), player.getDeptType());
        if (cardIds.isEmpty() || cardIds.size() > DECK_SIZE) {
            throw new BusinessException("牌组数量异常");
        }
        Collections.shuffle(cardIds);
        int handSize = Math.min(INITIAL_HAND_SIZE, cardIds.size());
        List<Long> handCardIds = new ArrayList<>(cardIds.subList(0, handSize));
        List<Long> deckCardIds = new ArrayList<>(cardIds.subList(handSize, cardIds.size()));
        Collections.shuffle(handCardIds);
        Collections.shuffle(deckCardIds);
        int insertIndex = 0;
        for (Long cardId : handCardIds) {
            MatchCards card = new MatchCards();
            card.setMatchId(matchId);
            card.setMatchPlayerId(player.getId());
            card.setUserId(player.getUserId());
            card.setCardId(cardId);
            card.setZone("HAND");
            card.setDeckOrder(null);
            card.setDrawnRound(1);
            card.setVersion(0);
            matchCardsMapper.insert(card);
            insertIndex++;
        }
        for (int index = 0; index < deckCardIds.size(); index++) {
            MatchCards card = new MatchCards();
            card.setMatchId(matchId);
            card.setMatchPlayerId(player.getId());
            card.setUserId(player.getUserId());
            card.setCardId(deckCardIds.get(index));
            card.setZone("DECK");
            card.setDeckOrder(index + 1);
            card.setDrawnRound(null);
            card.setVersion(0);
            matchCardsMapper.insert(card);
            insertIndex++;
        }
        if (insertIndex != cardIds.size()) {
            throw new BusinessException("牌组初始化数量异常");
        }
    }

    /**
     * 销售满编 30 张：本部门基础 14（7 种 × 2）+ 本部门收藏 8 种各 1 张 + 公共收藏 6 + 公共基础 2。
     * 采购基础卡更少时，多出的空位优先加本部门收藏。收藏名额优先给最近没上场过的已解锁卡。
     * 解锁不足时可以少于 30 张；够填满时必须正好 30 张。
     */
    private List<Long> buildUnlockedDeckCardIds(Long userId, String deptType) {
        Set<Long> playable = cardCollectionService.listPlayableCardIds(userId);
        if (playable.isEmpty()) {
            throw new BusinessException("没有已解锁的可用卡牌");
        }
        String ownDept = deptType == null ? "" : deptType.trim().toLowerCase();
        String opponent = SALES.equals(ownDept) ? PURCHASE : PURCHASE.equals(ownDept) ? SALES : null;
        List<Cards> enabled = cardsMapper.selectList(Wrappers.<Cards>lambdaQuery()
                .eq(Cards::getStatus, 1)
                .orderByAsc(Cards::getId));
        List<Cards> coreStarters = new ArrayList<>();
        List<Cards> coreCollectibles = new ArrayList<>();
        List<Cards> sharedStarters = new ArrayList<>();
        List<Cards> sharedCollectibles = new ArrayList<>();
        for (Cards card : enabled) {
            if (!playable.contains(card.getId())) {
                continue;
            }
            String cardDept = card.getDeptType() == null ? "" : card.getDeptType().trim().toLowerCase();
            if (!ownDept.isEmpty() && ownDept.equals(cardDept)) {
                if (isStarterCard(card)) {
                    coreStarters.add(card);
                } else {
                    coreCollectibles.add(card);
                }
            } else if (isSharedDept(cardDept) && (opponent == null || !opponent.equals(cardDept))) {
                if (isStarterCard(card)) {
                    sharedStarters.add(card);
                } else {
                    sharedCollectibles.add(card);
                }
            }
        }
        if (coreStarters.isEmpty() && coreCollectibles.isEmpty() && sharedStarters.isEmpty() && sharedCollectibles.isEmpty()) {
            throw new BusinessException("没有可用于组牌的已解锁卡牌");
        }
        Map<Long, Long> lastSeen = lastSeenCardSeq(userId);
        List<Long> cardIds = new ArrayList<>(DECK_SIZE);
        Set<Long> used = new HashSet<>();
        int starterUnique = Math.min(DEPT_STARTER_UNIQUE_CAP, coreStarters.size());
        if (starterUnique > 0) {
            List<Long> starters = pickUniqueWithCopies(coreStarters, starterUnique, DEPT_STARTER_COPIES);
            cardIds.addAll(starters);
            used.addAll(starters);
        }
        int remaining = Math.max(DECK_SIZE - cardIds.size(), 0);
        int bandTotal = DEPT_COLLECTIBLE_SLOTS + SHARED_COLLECTIBLE_SLOTS + SHARED_STARTER_SLOTS;
        int wantDeptColl;
        int wantSharedColl;
        int wantSharedStart;
        if (remaining >= bandTotal) {
            wantDeptColl = DEPT_COLLECTIBLE_SLOTS + (remaining - bandTotal);
            wantSharedColl = SHARED_COLLECTIBLE_SLOTS;
            wantSharedStart = SHARED_STARTER_SLOTS;
        } else {
            wantDeptColl = Math.round(remaining * (DEPT_COLLECTIBLE_SLOTS / (float) bandTotal));
            wantSharedColl = Math.round(remaining * (SHARED_COLLECTIBLE_SLOTS / (float) bandTotal));
            wantSharedStart = Math.max(0, remaining - wantDeptColl - wantSharedColl);
        }
        cardIds.addAll(takeDistinct(preferUnseen(coreCollectibles, lastSeen), wantDeptColl, used, false));
        cardIds.addAll(takeDistinct(preferUnseen(sharedCollectibles, lastSeen), wantSharedColl, used, false));
        cardIds.addAll(takeDistinct(preferUnseen(sharedStarters, lastSeen), wantSharedStart, used, true));
        if (cardIds.size() < DECK_SIZE) {
            List<Cards> leftover = new ArrayList<>();
            leftover.addAll(preferUnseen(coreCollectibles, lastSeen));
            leftover.addAll(preferUnseen(sharedCollectibles, lastSeen));
            leftover.addAll(sharedStarters);
            cardIds.addAll(takeDistinct(leftover, DECK_SIZE - cardIds.size(), used, false));
        }
        if (cardIds.isEmpty() || cardIds.size() > DECK_SIZE) {
            throw new BusinessException("牌组生成数量异常");
        }
        int available = starterUnique * DEPT_STARTER_COPIES
                + coreCollectibles.size() + sharedCollectibles.size() + sharedStarters.size();
        if (available >= DECK_SIZE && cardIds.size() != DECK_SIZE) {
            throw new BusinessException("已解锁卡牌足够时牌组必须凑满 " + DECK_SIZE + " 张");
        }
        return cardIds;
    }

    private boolean isStarterCard(Cards card) {
        return card.getRequireUnlock() == null || card.getRequireUnlock() == 0;
    }

    private Map<Long, Long> lastSeenCardSeq(Long userId) {
        Map<Long, Long> lastSeen = new HashMap<>();
        if (userId == null) {
            return lastSeen;
        }
        List<MatchCards> recent = matchCardsMapper.selectList(Wrappers.<MatchCards>lambdaQuery()
                .eq(MatchCards::getUserId, userId)
                .orderByDesc(MatchCards::getId)
                .last("LIMIT 400"));
        if (recent == null) {
            return lastSeen;
        }
        for (MatchCards row : recent) {
            if (row.getCardId() == null) {
                continue;
            }
            lastSeen.putIfAbsent(row.getCardId(), row.getId());
        }
        return lastSeen;
    }

    private List<Cards> preferUnseen(List<Cards> pool, Map<Long, Long> lastSeen) {
        List<Cards> copy = new ArrayList<>(pool);
        Collections.shuffle(copy);
        copy.sort(Comparator.comparingLong(card -> {
            Long cardId = card.getId();
            return cardId == null ? 0L : lastSeen.getOrDefault(cardId, 0L);
        }));
        return copy;
    }

    private List<Long> takeDistinct(List<Cards> pool, int count, Set<Long> used, boolean shuffle) {
        if (count <= 0 || pool == null || pool.isEmpty()) {
            return List.of();
        }
        List<Cards> ordered = new ArrayList<>(pool);
        if (shuffle) {
            Collections.shuffle(ordered);
        }
        List<Long> ids = new ArrayList<>(count);
        for (Cards card : ordered) {
            if (ids.size() >= count) {
                break;
            }
            Long cardId = card.getId();
            if (cardId == null || used.contains(cardId)) {
                continue;
            }
            used.add(cardId);
            ids.add(cardId);
        }
        return ids;
    }

    private List<Long> pickUniqueWithCopies(List<Cards> pool, int uniqueCount, int copies) {
        Collections.shuffle(pool);
        List<Long> ids = new ArrayList<>(uniqueCount * copies);
        for (int i = 0; i < uniqueCount; i++) {
            Long cardId = pool.get(i).getId();
            for (int copy = 0; copy < copies; copy++) {
                ids.add(cardId);
            }
        }
        return ids;
    }

    private List<Long> pickUpToCount(List<Cards> pool, int count) {
        Collections.shuffle(pool);
        List<Long> ids = new ArrayList<>(count);
        for (Cards card : pool) {
            if (ids.size() >= count) {
                break;
            }
            ids.add(card.getId());
        }
        int index = 0;
        while (ids.size() < count) {
            ids.add(pool.get(index % pool.size()).getId());
            index++;
        }
        return ids;
    }

    /** 兜底：按 deck_card_configs 固定 20 张 */
    private List<Long> buildConfiguredDeckCardIds(String deptType) {
        List<DeckCardConfigs> configs = deckCardConfigsMapper.selectList(Wrappers.<DeckCardConfigs>lambdaQuery()
                .eq(DeckCardConfigs::getDeptType, deptType)
                .eq(DeckCardConfigs::getStatus, 1)
                .orderByAsc(DeckCardConfigs::getSortNo, DeckCardConfigs::getId));
        int configuredCount = configs.stream().mapToInt(config -> config.getCardCount() == null ? 0 : config.getCardCount()).sum();
        if (configuredCount != DECK_SIZE) {
            throw new BusinessException(deptType + " 部门启用牌组必须正好配置20张牌");
        }
        List<Long> cardIds = new ArrayList<>(DECK_SIZE);
        for (DeckCardConfigs config : configs) {
            for (int i = 0; i < config.getCardCount(); i++) {
                cardIds.add(config.getCardId());
            }
        }
        return cardIds;
    }

    /**
     * 主部门全部启用卡各 2 张，再用其他部门卡牌随机补齐至 20 张。
     * 补齐池排除：本部门、对立部门（销售↔采购）。
     */
    private List<Long> buildCoreDeptDeckCardIds(String coreDept, String excludedOpponentDept, String deptLabel) {
        List<Cards> coreCards = cardsMapper.selectList(Wrappers.<Cards>lambdaQuery()
                .eq(Cards::getDeptType, coreDept)
                .eq(Cards::getStatus, 1)
                .orderByAsc(Cards::getId));
        if (coreCards.isEmpty()) {
            throw new BusinessException(deptLabel + "没有可用卡牌");
        }
        List<Long> cardIds = new ArrayList<>(DECK_SIZE);
        for (Cards card : coreCards) {
            cardIds.add(card.getId());
            cardIds.add(card.getId());
        }
        int remaining = DECK_SIZE - cardIds.size();
        if (remaining < 0) {
            throw new BusinessException(deptLabel + "卡牌数量超过牌组上限20张");
        }
        if (remaining > 0) {
            List<Cards> fillers = cardsMapper.selectList(Wrappers.<Cards>lambdaQuery()
                    .ne(Cards::getDeptType, coreDept)
                    .ne(Cards::getDeptType, excludedOpponentDept)
                    .eq(Cards::getStatus, 1)
                    .orderByAsc(Cards::getId));
            if (fillers.isEmpty()) {
                throw new BusinessException("没有可用于补齐" + deptLabel + "牌组的其他部门卡牌");
            }
            List<Long> fillerPool = fillers.stream().map(Cards::getId).collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(fillerPool);
            int index = 0;
            while (remaining > 0) {
                if (index >= fillerPool.size()) {
                    Collections.shuffle(fillerPool);
                    index = 0;
                }
                cardIds.add(fillerPool.get(index++));
                remaining--;
            }
        }
        if (cardIds.size() != DECK_SIZE) {
            throw new BusinessException(deptLabel + "牌组生成数量异常");
        }
        return cardIds;
    }

    private void createFirstRound(Matches match, CustomerTypes customer) {
        MatchRounds round = new MatchRounds();
        round.setMatchId(match.getId());
        round.setRoundNo(1);
        round.setRoundStatus(0);
        round.setPhase("SELECT_FIRST_PLAYER");
        round.setBossAttack(match.getBossCurrentAttack());
        round.setCustomerTriggered(0);
        round.setCustomerEffectType(customer.getEffectType());
        round.setCustomerEffectValue(customer.getEffectValue());
        round.setBossRageValue(0);
        round.setSatisfactionDelta(0);
        round.setFundsPerPlayer(3);
        round.setStartedAt(LocalDateTime.now());
        matchRoundsMapper.insert(round);
        match.setStatus(1);
        match.setPhase("SELECT_FIRST_PLAYER");
        matchesMapper.updateById(match);
    }

    private void validateReadyMembers(List<RoomMembers> members) {
        if (members.size() != PLAYER_COUNT) {
            throw new BusinessException("房间必须正好有两名玩家");
        }
        if (members.stream().anyMatch(member -> member.getReadyStatus() == null || member.getReadyStatus() != 1)) {
            throw new BusinessException("双方尚未全部准备");
        }
        long deptCount = members.stream().map(RoomMembers::getDeptType).filter(dept -> dept != null && !dept.isBlank()).distinct().count();
        boolean legal = deptCount == PLAYER_COUNT && members.stream()
                .allMatch(member -> SALES.equals(member.getDeptType()) || PURCHASE.equals(member.getDeptType()));
        if (!legal) {
            throw new BusinessException("双方必须分别选择销售部和采购部");
        }
    }

    private CustomerTypes pickCustomer() {
        List<CustomerTypes> customers = customerTypesMapper.selectList(Wrappers.<CustomerTypes>lambdaQuery()
                .eq(CustomerTypes::getStatus, 1)
                .orderByAsc(CustomerTypes::getSortNo));
        if (customers.isEmpty()) {
            throw new BusinessException("没有可用的顾客配置");
        }
        int totalWeight = customers.stream().mapToInt(customer -> Math.max(value(customer.getSelectionWeight()), 0)).sum();
        if (totalWeight <= 0) {
            return customers.get(0);
        }
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        for (CustomerTypes customer : customers) {
            random -= Math.max(value(customer.getSelectionWeight()), 0);
            if (random < 0) {
                return customer;
            }
        }
        return customers.get(0);
    }

    private Bullies requireEnabledBully() {
        Bullies bully = bulliesMapper.selectOne(Wrappers.<Bullies>lambdaQuery()
                .eq(Bullies::getStatus, 1)
                .orderByAsc(Bullies::getId)
                .last("LIMIT 1"));
        if (bully == null) {
            throw new BusinessException("没有可用的霸凌者配置");
        }
        return bully;
    }

    private void bindRoomToMatch(GameRooms room, Long matchId) {
        room.setMatchId(matchId);
        room.setStatus(2);
        gameRoomsMapper.updateById(room);
    }

    private void notifyMatchStarted(Matches match, List<RoomMembers> members) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("matchId", match.getId());
        data.put("matchCode", match.getMatchCode());
        data.put("roomId", match.getRoomId());
        data.put("roundNo", match.getCurrentRound());
        data.put("phase", match.getPhase());
        Map<String, Object> payload = Map.of("type", "match.started", "data", data);
        for (RoomMembers member : members) {
            notificationService.notifyUser(member.getUserId(), payload);
        }
    }

    private void updateFirstPlayerPhase(Matches match, Long firstPlayerUserId, Long chosenByUserId) {
        MatchRounds round = currentRound(match);
        if (round == null) {
            throw new BusinessException("当前回合不存在");
        }
        List<MatchPlayers> players = listPlayers(match.getId());
        Long secondPlayerUserId = players.stream().map(MatchPlayers::getUserId)
                .filter(userId -> !userId.equals(firstPlayerUserId)).findFirst().orElse(null);
        round.setFirstPlayerUserId(firstPlayerUserId);
        round.setChosenByUserId(chosenByUserId);
        round.setPhase("PLAYER_ACTION");
        round.setCustomerTriggered(0);
        round.setCustomerEffectType(null);
        round.setCustomerEffectValue(0);
        round.setStartedAt(LocalDateTime.now());
        matchRoundsMapper.updateById(round);
        match.setPhase("PLAYER_ACTION");
        match.setVersion((match.getVersion() == null ? 0L : match.getVersion()) + 1L);
        matchesMapper.updateById(match);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("matchId", match.getId());
        data.put("roundNo", round.getRoundNo());
        data.put("chosenByUserId", chosenByUserId);
        data.put("firstPlayerUserId", firstPlayerUserId);
        data.put("secondPlayerUserId", secondPlayerUserId);
        data.put("version", match.getVersion());
        data.put("phase", match.getPhase());
        notifyPlayers(match.getId(), "match.first_player.chosen", data);
    }

    private MatchPlayers requirePlayer(Long userId, Long matchId) {
        MatchPlayers player = matchPlayersMapper.selectOne(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getMatchId, matchId)
                .eq(MatchPlayers::getUserId, userId));
        if (player == null) {
            throw new BusinessException("无权操作该对局");
        }
        return player;
    }

    private void markReconnecting(Matches match, Long userId) {
        MatchPlayers player = matchPlayersMapper.selectOne(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getMatchId, match.getId())
                .eq(MatchPlayers::getUserId, userId));
        if (player == null || "DEAD".equals(player.getPlayerStatus()) || "LEFT".equals(player.getPlayerStatus())) {
            return;
        }
        if ("RECONNECTING".equals(player.getPlayerStatus())) {
            return;
        }
        LocalDateTime disconnectedAt = LocalDateTime.now();
        matchPlayersMapper.update(null, Wrappers.<MatchPlayers>lambdaUpdate()
                .eq(MatchPlayers::getId, player.getId())
                .set(MatchPlayers::getPlayerStatus, "RECONNECTING")
                .set(MatchPlayers::getUpdatedAt, disconnectedAt));
        if (!KEEP_PHASE_ON_DISCONNECT.contains(match.getPhase())) {
            match.setPhase("RECONNECT_WAIT");
            matchesMapper.updateById(match);
        }
        notifyPlayers(match.getId(), "match.reconnecting", Map.of(
                "matchId", match.getId(),
                "userId", userId,
                "timeoutSeconds", RECONNECT_TIMEOUT_MILLIS / 1000
        ));
    }

    @Override
    @Transactional
    public MatchStateResp reconnect(Long currentUserId, Long matchId) {
        applyReconnect(currentUserId, matchId);
        return getMatchState(currentUserId, matchId);
    }

    @Override
    @Transactional
    public void recoverOnlinePlayer(Long userId) {
        if (userId == null) {
            return;
        }
        List<MatchPlayers> rows = matchPlayersMapper.selectList(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getUserId, userId)
                .eq(MatchPlayers::getPlayerStatus, "RECONNECTING"));
        for (MatchPlayers player : rows) {
            if (player.getMatchId() == null) {
                continue;
            }
            try {
                applyReconnect(userId, player.getMatchId());
            } catch (Exception e) {
                logger.warn("Auto-recover reconnect failed userId={} matchId={}: {}",
                        userId, player.getMatchId(), e.getMessage());
            }
        }
    }

    @Override
    public Long findActiveMatchId(Long userId) {
        if (userId == null) {
            return null;
        }
        List<MatchPlayers> rows = matchPlayersMapper.selectList(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getUserId, userId)
                .in(MatchPlayers::getPlayerStatus, List.of("ACTIVE", "RECONNECTING", "DEAD"))
                .orderByDesc(MatchPlayers::getUpdatedAt)
                .orderByDesc(MatchPlayers::getId));
        for (MatchPlayers player : rows) {
            if (player.getMatchId() == null) {
                continue;
            }
            Matches match = matchesMapper.selectById(player.getMatchId());
            if (match != null && value(match.getStatus()) == 1
                    && match.getEndedAt() == null && !"FINISHED".equals(match.getPhase())) {
                return match.getId();
            }
        }
        return null;
    }

    private void applyReconnect(Long currentUserId, Long matchId) {
        Matches match = requireMatch(matchId);
        if (value(match.getStatus()) == 2 || "FINISHED".equals(match.getPhase())) {
            return;
        }
        MatchPlayers player = requirePlayer(currentUserId, matchId);
        if ("LEFT".equals(player.getPlayerStatus())) {
            return;
        }
        if (!"RECONNECTING".equals(player.getPlayerStatus())) {
            return;
        }
        if ("DEAD".equals(player.getPlayerStatus()) || value(player.getCurrentHp()) <= 0) {
            player.setPlayerStatus("DEAD");
        } else {
            player.setPlayerStatus("ACTIVE");
        }
        matchPlayersMapper.updateById(player);
        restoreActionPhaseIfReconnectCleared(match);
        notifyPlayers(matchId, "match.recovered", Map.of(
                "matchId", matchId,
                "userId", currentUserId,
                "phase", match.getPhase()));
        userPresenceService.broadcastPresence(currentUserId);
    }

    private void restoreActionPhaseIfReconnectCleared(Matches match) {
        if (!"RECONNECT_WAIT".equals(match.getPhase())) {
            return;
        }
        boolean stillWaiting = listPlayers(match.getId()).stream().anyMatch(item ->
                "RECONNECTING".equals(item.getPlayerStatus()) && value(item.getCurrentHp()) > 0);
        if (!stillWaiting) {
            match.setPhase(PLAYER_ACTION);
            matchesMapper.updateById(match);
        }
    }

    @Override
    @Transactional
    public MatchFirstPlayerResp chooseFirstPlayer(Long currentUserId, Long matchId, cc.shturl.wa.demo.dto.req.ChooseFirstPlayerReq request) {
        Matches match = requireMatch(matchId);
        if (!Integer.valueOf(1).equals(match.getStatus()) || match.getPhase() == null || !"SELECT_FIRST_PLAYER".equals(match.getPhase())) {
            throw new BusinessException("当前不是选择先手阶段");
        }
        List<MatchPlayers> players = requirePlayerAndList(currentUserId, matchId);
        MatchPlayers host = players.stream().filter(player -> player.getSeatNo() != null && player.getSeatNo() == 1).findFirst()
                .orElseThrow(() -> new BusinessException("房主信息不存在"));
        if (!currentUserId.equals(host.getUserId())) {
            throw new BusinessException("只有房主可以选择先手");
        }
        MatchPlayers firstPlayer = players.stream().filter(player -> request.firstPlayerUserId().equals(player.getUserId()))
                .findFirst().orElseThrow(() -> new BusinessException("先手玩家必须是房间成员"));
        updateFirstPlayerPhase(match, firstPlayer.getUserId(), currentUserId);
        MatchRounds round = currentRound(match);
        Long secondPlayerUserId = players.stream().map(MatchPlayers::getUserId)
                .filter(userId -> !userId.equals(firstPlayer.getUserId())).findFirst().orElse(null);
        return new MatchFirstPlayerResp(matchId, round.getRoundNo(), currentUserId, firstPlayer.getUserId(),
                secondPlayerUserId, match.getVersion(), match.getPhase());
    }

    @Override
    @Transactional
    public void abandon(Long currentUserId, Long matchId) {
        Matches match = requireMatch(matchId);
        if (value(match.getStatus()) == 2) {
            return;
        }
        MatchPlayers player = requirePlayer(currentUserId, matchId);
        player.setPlayerStatus("LEFT");
        player.setResultType(2);
        matchPlayersMapper.updateById(player);
        // 放弃对局：计失败，但不发放金币/经验/任务进度
        finishMatch(match, 2, false);
        notifyPlayers(matchId, "match.ended", Map.of("matchId", matchId, "winnerType", 2, "reason", "abandon"));
        userPresenceService.broadcastPresence(currentUserId);
    }

    @Override
    @Transactional
    public void markPlayerDisconnected(Long userId) {
        List<MatchPlayers> activePlayers = matchPlayersMapper.selectList(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getUserId, userId)
                .eq(MatchPlayers::getPlayerStatus, "ACTIVE"));
        for (MatchPlayers player : activePlayers) {
            Matches match = requireMatch(player.getMatchId());
            if (value(match.getStatus()) != 1 || "FINISHED".equals(match.getPhase())) {
                continue;
            }
            markReconnecting(match, userId);
        }
    }

    private List<MatchPlayers> requirePlayerAndList(Long userId, Long matchId) {
        requireMatch(matchId);
        List<MatchPlayers> players = matchPlayersMapper.selectList(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getMatchId, matchId));
        if (players.stream().noneMatch(player -> userId.equals(player.getUserId()))) {
            throw new BusinessException("无权查看该对局");
        }
        return players;
    }

    private GameRooms requireRoom(Long roomId) {
        GameRooms room = gameRoomsMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }
        return room;
    }

    private Matches requireMatch(Long matchId) {
        Matches match = matchesMapper.selectById(matchId);
        if (match == null) {
            throw new BusinessException("对局不存在");
        }
        return match;
    }

    private MatchPlayerStateResp toPlayerState(MatchPlayers player, List<MatchCards> cards) {
        return new MatchPlayerStateResp(player.getUserId(), player.getSeatNo(), player.getDeptType(), player.getMaxHp(),
                player.getCurrentHp(), player.getShield(), player.getActionPoints(), player.getEndedTurn(),
                player.getPlayerStatus(), countZone(cards, "HAND"), countZone(cards, "DECK"), countZone(cards, "DISCARD"));
    }

    private int countZone(List<MatchCards> cards, String zone) {
        return (int) cards.stream().filter(card -> zone.equals(card.getZone())).count();
    }

    private List<MatchCardResp> toCardResponses(List<MatchCards> instances) {
        return toCardResponses(instances, false);
    }

    private List<MatchCardResp> toCardResponses(List<MatchCards> instances, boolean hideDeckContents) {
        if (instances.isEmpty()) {
            return List.of();
        }
        List<MatchCards> visible = hideDeckContents
                ? instances.stream().filter(card -> !"DECK".equals(card.getZone())).toList()
                : instances;
        List<MatchCards> hiddenDeck = hideDeckContents
                ? instances.stream().filter(card -> "DECK".equals(card.getZone())).toList()
                : List.of();
        Map<Long, Cards> templates = visible.isEmpty() ? Map.of()
                : cardsMapper.selectBatchIds(visible.stream().map(MatchCards::getCardId).distinct().toList())
                .stream().collect(Collectors.toMap(Cards::getId, Function.identity()));
        Map<Long, Boolean> requiresTargetByCard = Map.of();
        if (!templates.isEmpty()) {
            List<CardEffects> effectRows = cardEffectsMapper.selectList(Wrappers.<CardEffects>lambdaQuery()
                    .in(CardEffects::getCardId, templates.keySet()));
            requiresTargetByCard = effectRows.stream().collect(Collectors.groupingBy(CardEffects::getCardId,
                    Collectors.collectingAndThen(Collectors.toList(),
                            list -> list.stream().anyMatch(effect -> "ANY_PLAYER".equals(effect.getEffectScope())))));
        }
        Map<Long, Boolean> targetFlags = requiresTargetByCard;
        List<MatchCardResp> result = new ArrayList<>(instances.size());
        for (MatchCards instance : visible) {
            Cards card = templates.get(instance.getCardId());
            result.add(new MatchCardResp(instance.getId(), instance.getCardId(), card == null ? null : card.getCardCode(),
                    card == null ? null : card.getCardName(), card == null ? null : card.getDeptType(),
                    card == null ? null : card.getCost(), card == null ? null : card.getCardType(),
                    card == null ? null : card.getDescription(), card == null ? null : card.getImageUrl(),
                    instance.getZone(), null, instance.getDrawnRound(),
                    card == null ? Boolean.FALSE : targetFlags.getOrDefault(card.getId(), Boolean.FALSE)));
        }
        for (MatchCards instance : hiddenDeck) {
            result.add(new MatchCardResp(instance.getId(), null, null, null, null, null, null, null, null,
                    "DECK", null, instance.getDrawnRound(), Boolean.FALSE));
        }
        return result;
    }

    private CustomerInfoResp toCustomerResp(CustomerTypes customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerInfoResp(customer.getId(), customer.getCustomerCode(), customer.getCustomerName(),
                customer.getDescription(), customer.getImageUrl(), customer.getEffectType(), customer.getEffectValue(),
                customer.getTriggerChance(), customer.getSelectionWeight(), customer.getStatus());
    }

    private boolean canUseCardInCurrentDept(String actorDept, String cardDept) {
        String actor = actorDept == null ? "" : actorDept.trim().toLowerCase();
        String card = cardDept == null ? "" : cardDept.trim().toLowerCase();
        if (card.isEmpty()) {
            return false;
        }
        if (isSharedDept(card)) {
            return true;
        }
        return card.equals(actor);
    }

    private boolean isSharedDept(String cardDept) {
        String card = cardDept == null ? "" : cardDept.trim().toLowerCase();
        return !card.isEmpty() && !SALES.equals(card) && !PURCHASE.equals(card);
    }

    private MatchPendingEffects findNextCardMultiplier(Long matchId, Long userId) {
        return matchPendingEffectsMapper.selectOne(Wrappers.<MatchPendingEffects>lambdaQuery()
                .eq(MatchPendingEffects::getMatchId, matchId).eq(MatchPendingEffects::getSourceUserId, userId)
                .eq(MatchPendingEffects::getEffectType, "MULTIPLY_NEXT_CARD")
                .eq(MatchPendingEffects::getStatus, "PENDING").orderByAsc(MatchPendingEffects::getId).last("LIMIT 1"));
    }

    private void replacePendingNextCardMultipliers(Long matchId, Long userId) {
        List<MatchPendingEffects> existing = matchPendingEffectsMapper.selectList(
                Wrappers.<MatchPendingEffects>lambdaQuery()
                        .eq(MatchPendingEffects::getMatchId, matchId)
                        .eq(MatchPendingEffects::getSourceUserId, userId)
                        .eq(MatchPendingEffects::getEffectType, "MULTIPLY_NEXT_CARD")
                        .eq(MatchPendingEffects::getStatus, "PENDING"));
        for (MatchPendingEffects pending : existing) {
            pending.setRemainingTriggers(0);
            pending.setStatus("RESOLVED");
            matchPendingEffectsMapper.updateById(pending);
        }
    }

    private void applyImmediateEffect(Matches match, Bullies bully, MatchPlayers actor, MatchPlayers target, CardEffects effect,
                                      int multiplier, List<CardEffectResp> results) {
        int baseValue = Math.max(value(effect.getValue()), 0);
        int actualValue = baseValue * multiplier;
        switch (effect.getEffectType()) {
            case "DAMAGE_BOSS" -> {
                int beforeValue = value(match.getBossCurrentHp());
                int defense = bully == null ? 0 : value(bully.getDefenseValue());
                int finalDamage = Math.max(0, actualValue - defense);
                int afterValue = Math.max(0, beforeValue - finalDamage);
                match.setBossCurrentHp(afterValue);
                actor.setDamageDealt(value(actor.getDamageDealt()) + beforeValue - afterValue);
                results.add(effectResult(effect, "BOSS", null, baseValue, beforeValue - afterValue,
                        beforeValue, afterValue, match.getCurrentRound()));
            }
            case "REDUCE_BOSS_ATTACK" -> {
                int beforeValue = value(match.getBossCurrentAttack());
                int afterValue = Math.max(0, beforeValue - actualValue);
                match.setBossCurrentAttack(afterValue);
                results.add(effectResult(effect, "BOSS", null, baseValue, beforeValue - afterValue,
                        beforeValue, afterValue, match.getCurrentRound()));
            }
            case "DRAW_CARDS" -> {
                for (MatchPlayers recipient : resolveEffectTargets(match.getId(), actor, target, effect)) {
                    int beforeValue = countCards(match.getId(), recipient.getUserId(), "HAND");
                    int drawn = drawCards(match.getId(), recipient.getUserId(), match.getCurrentRound(), actualValue);
                    int afterValue = beforeValue + drawn;
                    results.add(effectResult(effect, resultTargetType(effect), recipient.getUserId(), baseValue, drawn,
                            beforeValue, afterValue, match.getCurrentRound()));
                }
            }
            case "HEAL_PLAYER" -> {
                for (MatchPlayers recipient : resolveEffectTargets(match.getId(), actor, target, effect)) {
                    int beforeValue = value(recipient.getCurrentHp());
                    int afterValue = Math.min(value(recipient.getMaxHp()), beforeValue + actualValue);
                    recipient.setCurrentHp(afterValue);
                    actor.setHealingDone(value(actor.getHealingDone()) + afterValue - beforeValue);
                    persistPlayerIfNotActor(actor, recipient);
                    results.add(effectResult(effect, resultTargetType(effect), recipient.getUserId(), baseValue,
                            afterValue - beforeValue, beforeValue, afterValue, match.getCurrentRound()));
                }
            }
            case "ADD_SHIELD" -> {
                for (MatchPlayers recipient : resolveEffectTargets(match.getId(), actor, target, effect)) {
                    int beforeValue = value(recipient.getShield());
                    int afterValue = beforeValue + actualValue;
                    recipient.setShield(afterValue);
                    actor.setShieldGranted(value(actor.getShieldGranted()) + actualValue);
                    persistPlayerIfNotActor(actor, recipient);
                    results.add(effectResult(effect, resultTargetType(effect), recipient.getUserId(), baseValue,
                            actualValue, beforeValue, afterValue, match.getCurrentRound()));
                }
            }
            case "ADD_ACTION_POINTS" -> {
                for (MatchPlayers recipient : resolveEffectTargets(match.getId(), actor, target, effect)) {
                    int beforeValue = value(recipient.getActionPoints());
                    int afterValue = beforeValue + actualValue;
                    recipient.setActionPoints(afterValue);
                    persistPlayerIfNotActor(actor, recipient);
                    results.add(effectResult(effect, resultTargetType(effect), recipient.getUserId(), baseValue,
                            actualValue, beforeValue, afterValue, match.getCurrentRound()));
                }
            }
            case "DAMAGE_PLAYER" -> {
                for (MatchPlayers recipient : resolveEffectTargets(match.getId(), actor, target, effect)) {
                    int beforeValue = value(recipient.getCurrentHp());
                    int afterValue = applyPlayerHpLoss(recipient, actualValue);
                    persistPlayerIfNotActor(actor, recipient);
                    results.add(effectResult(effect, resultTargetType(effect), recipient.getUserId(), baseValue,
                            beforeValue - afterValue, beforeValue, afterValue, match.getCurrentRound()));
                }
            }
            case "INCREASE_BOSS_ATTACK" -> {
                int beforeValue = value(match.getBossCurrentAttack());
                int afterValue = beforeValue + actualValue;
                match.setBossCurrentAttack(afterValue);
                results.add(effectResult(effect, "BOSS", null, baseValue, actualValue,
                        beforeValue, afterValue, match.getCurrentRound()));
            }
            case "HEAL_BOSS" -> {
                int beforeValue = value(match.getBossCurrentHp());
                int afterValue = beforeValue + actualValue;
                match.setBossMaxHp(value(match.getBossMaxHp()) + actualValue);
                match.setBossCurrentHp(afterValue);
                results.add(effectResult(effect, "BOSS", null, baseValue, actualValue,
                        beforeValue, afterValue, match.getCurrentRound()));
            }
            default -> throw new BusinessException("不支持的立即效果类型：" + effect.getEffectType());
        }
    }

    private void requireEffectTarget(MatchPlayers target) {
        if (target == null) {
            throw new BusinessException("卡牌效果缺少合法玩家目标");
        }
    }

    private String resolveResponseTargetType(List<CardEffects> effects, boolean requiresPlayerTarget) {
        if (requiresPlayerTarget) {
            return "PLAYER";
        }
        if (effects.stream().anyMatch(effect -> "ALL_PLAYERS".equals(effect.getEffectScope()))) {
            return "ALL_PLAYERS";
        }
        boolean hasSelf = effects.stream().anyMatch(effect -> "SELF".equals(effect.getEffectScope()));
        boolean hasBoss = effects.stream().anyMatch(effect -> "BOSS".equals(effect.getEffectScope())
                || "DAMAGE_BOSS".equals(effect.getEffectType())
                || "REDUCE_BOSS_ATTACK".equals(effect.getEffectType())
                || "INCREASE_BOSS_ATTACK".equals(effect.getEffectType())
                || "HEAL_BOSS".equals(effect.getEffectType()));
        if (hasSelf && !hasBoss) {
            return "SELF";
        }
        return "BOSS";
    }

    private String resultTargetType(CardEffects effect) {
        return "ALL_PLAYERS".equals(effect.getEffectScope()) ? "ALL_PLAYERS" : "PLAYER";
    }

    private CardEffectResp effectResult(CardEffects effect, String targetType, Long targetUserId, int baseValue,
                                        int actualValue, int beforeValue, int afterValue, Integer triggerRound) {
        return new CardEffectResp(effect.getEffectType(), effect.getTriggerTiming(), targetType, targetUserId,
                baseValue, actualValue, beforeValue, afterValue, false, triggerRound, null);
    }

    private List<MatchPlayers> resolveEffectTargets(Long matchId, MatchPlayers actor, MatchPlayers selected, CardEffects effect) {
        String scope = effect.getEffectScope();
        if ("ALL_PLAYERS".equals(scope)) {
            List<MatchPlayers> targets = new ArrayList<>();
            for (MatchPlayers player : listPlayers(matchId)) {
                if (value(player.getCurrentHp()) <= 0) {
                    continue;
                }
                targets.add(player.getId().equals(actor.getId()) ? actor : player);
            }
            return targets;
        }
        if ("SELF".equals(scope) || selected == null) {
            if ("ANY_PLAYER".equals(scope)) {
                requireEffectTarget(null);
            }
            return List.of(actor);
        }
        requireEffectTarget(selected);
        return List.of(selected.getId().equals(actor.getId()) ? actor : selected);
    }

    private void persistPlayerIfNotActor(MatchPlayers actor, MatchPlayers player) {
        if (player != null && !actor.getId().equals(player.getId())) {
            matchPlayersMapper.updateById(player);
        }
    }

    private int applyPlayerHpLoss(MatchPlayers player, int amount) {
        int before = value(player.getCurrentHp());
        int after = Math.max(0, before - Math.max(amount, 0));
        player.setCurrentHp(after);
        player.setDamageTaken(value(player.getDamageTaken()) + before - after);
        if (after <= 0) {
            player.setPlayerStatus("DEAD");
            player.setReviveStatus(1);
        }
        return after;
    }

    private boolean allPlayersDown(Long matchId) {
        List<MatchPlayers> players = listPlayers(matchId);
        if (players.isEmpty()) {
            return false;
        }
        for (MatchPlayers player : players) {
            if (value(player.getCurrentHp()) > 0) {
                return false;
            }
        }
        return true;
    }

    private void schedulePendingEffects(Matches match, MatchPlayers actor, MatchCards instance, MatchPlayers target,
                                        List<CardEffects> effects, int multiplier, List<CardEffectResp> results) {
        for (CardEffects effect : effects) {
            if ("IMMEDIATE".equals(effect.getTriggerTiming())) {
                continue;
            }
            int triggerRound;
            if ("NEXT_CARD".equals(effect.getTriggerTiming())) {
                // 下一张牌翻倍应跨回合保留，不能按“下回合开始”结算掉
                triggerRound = match.getCurrentRound();
            } else {
                triggerRound = match.getCurrentRound() + Math.max(value(effect.getTriggerDelay()), 1);
            }
            if ("MULTIPLY_NEXT_CARD".equals(effect.getEffectType())) {
                replacePendingNextCardMultipliers(match.getId(), actor.getUserId());
            }
            List<Long> targetUserIds = pendingTargetUserIds(match.getId(), actor, target, effect);
            String pendingTargetType = pendingTargetType(effect);
            for (Long targetUserId : targetUserIds) {
                MatchPendingEffects pending = new MatchPendingEffects();
                pending.setMatchId(match.getId());
                pending.setMatchPlayerId(actor.getId());
                pending.setSourceUserId(actor.getUserId());
                pending.setSourceCardInstanceId(instance.getId());
                pending.setEffectType(effect.getEffectType());
                pending.setTargetType(pendingTargetType);
                pending.setTargetUserId(targetUserId);
                pending.setEffectValue(value(effect.getValue()) * multiplier);
                pending.setTriggerRound(triggerRound);
                pending.setRemainingTriggers(Math.max(value(effect.getRemainingTriggers()),
                        Math.max(value(effect.getDurationRounds()), 1)));
                pending.setStatus("PENDING");
                pending.setExtraData(effect.getExtraData());
                matchPendingEffectsMapper.insert(pending);
                results.add(new CardEffectResp(effect.getEffectType(), effect.getTriggerTiming(),
                        "ALL_PLAYERS".equals(effect.getEffectScope()) ? "ALL_PLAYERS" : pending.getTargetType(),
                        pending.getTargetUserId(), value(effect.getValue()), pending.getEffectValue(), null, null, true,
                        triggerRound, pending.getId()));
            }
        }
    }

    private String pendingTargetType(CardEffects effect) {
        if ("DAMAGE_BOSS".equals(effect.getEffectType()) || "REDUCE_BOSS_ATTACK".equals(effect.getEffectType())
                || "INCREASE_BOSS_ATTACK".equals(effect.getEffectType()) || "HEAL_BOSS".equals(effect.getEffectType())) {
            return "BOSS";
        }
        return "PLAYER";
    }

    private List<Long> pendingTargetUserIds(Long matchId, MatchPlayers actor, MatchPlayers selected, CardEffects effect) {
        if ("DAMAGE_BOSS".equals(effect.getEffectType()) || "REDUCE_BOSS_ATTACK".equals(effect.getEffectType())
                || "INCREASE_BOSS_ATTACK".equals(effect.getEffectType()) || "HEAL_BOSS".equals(effect.getEffectType())
                || "MULTIPLY_NEXT_CARD".equals(effect.getEffectType())) {
            if ("MULTIPLY_NEXT_CARD".equals(effect.getEffectType())) {
                return List.of(actor.getUserId());
            }
            return Collections.singletonList(null);
        }
        return resolveEffectTargets(matchId, actor, selected, effect).stream().map(MatchPlayers::getUserId).toList();
    }

    @Override
    @Transactional
    public EndTurnResp endTurn(Long currentUserId, Long matchId, EndTurnReq request) {
        Matches match = requireMatch(matchId);
        if (match.getStatus() == null || match.getStatus() != 1 || !PLAYER_ACTION.equals(match.getPhase())) {
            throw new BusinessException("当前对局阶段不允许结束回合");
        }
        if (!request.expectedVersion().equals(match.getVersion())) {
            throw new BusinessException("对局状态已更新，请刷新后重试");
        }
        MatchActions duplicate = findClientAction(matchId, currentUserId, request.clientActionId());
        if (duplicate != null) {
            throw new BusinessException("该结束回合请求已处理，请刷新对局状态");
        }
        MatchPlayers actor = requirePlayer(currentUserId, matchId);
        if (value(actor.getCurrentHp()) <= 0 || !"ACTIVE".equals(actor.getPlayerStatus())) {
            throw new BusinessException("当前玩家无法结束回合");
        }
        if (value(actor.getEndedTurn()) == 1) {
            throw new BusinessException("本回合已经结束");
        }
        int resolvedRound = match.getCurrentRound();
        int discarded = discardHand(matchId, currentUserId, resolvedRound);
        actor.setEndedTurn(1);
        matchPlayersMapper.updateById(actor);
        MatchRounds round = currentRound(match);
        insertEndTurnAction(matchId, round, actor, request.clientActionId(), discarded);
        List<MatchPlayers> players = listPlayers(matchId);
        boolean allEnded = allLivingPlayersEnded(players);
        List<BossAttackTargetResp> targets = List.of();
        boolean attackResolved = false;
        boolean matchEnded = false;
        if (allEnded) {
            RoundSettleResult settled = settleEndedRound(match, round, players);
            targets = settled.targets();
            attackResolved = settled.attackResolved();
            matchEnded = settled.matchEnded();
        }
        match.setVersion(match.getVersion() + 1);
        matchesMapper.updateById(match);
        MatchPlayers refreshedActor = matchPlayersMapper.selectById(actor.getId());
        List<MatchCards> actorCards = matchCardsMapper.selectList(Wrappers.<MatchCards>lambdaQuery()
                .eq(MatchCards::getMatchId, matchId).eq(MatchCards::getUserId, currentUserId));
        EndTurnResp response = new EndTurnResp(matchId, currentUserId, refreshedActor.getEndedTurn(), discarded,
                allEnded, attackResolved, resolvedRound, targets, matchEnded, match.getWinnerType(),
                match.getCurrentRound(), match.getPhase(), match.getVersion(), countZone(actorCards, "HAND"),
                countZone(actorCards, "DECK"), countZone(actorCards, "DISCARD"));
        notifyAfterCommit(() -> {
            notifyPlayers(matchId, "player.turn.ended", response);
            if (response.bossAttackResolved()) {
                Map<String, Object> attackPayload = new LinkedHashMap<>();
                attackPayload.put("matchId", matchId);
                attackPayload.put("userId", currentUserId);
                attackPayload.put("resolvedRound", response.resolvedRound());
                attackPayload.put("currentRound", response.currentRound());
                attackPayload.put("bossAttackResolved", true);
                attackPayload.put("bossAttackTargets", response.bossAttackTargets());
                notifyPlayers(matchId, "boss.attack.resolved", attackPayload);
            }
            if (response.matchEnded()) {
                notifyPlayers(matchId, "match.ended", response);
                for (MatchPlayers player : players) {
                    userPresenceService.broadcastPresence(player.getUserId());
                }
            } else if (response.allPlayersEnded() && PLAYER_ACTION.equals(match.getPhase())) {
                notifyPlayers(matchId, "round.started", response);
            }
        });
        return response;
    }

    private int discardHand(Long matchId, Long userId, int roundNo) {
        List<MatchCards> hand = matchCardsMapper.selectList(Wrappers.<MatchCards>lambdaQuery()
                .eq(MatchCards::getMatchId, matchId).eq(MatchCards::getUserId, userId)
                .eq(MatchCards::getZone, "HAND"));
        for (MatchCards card : hand) {
            card.setZone("DISCARD");
            card.setDiscardedRound(roundNo);
            card.setDeckOrder(null);
            card.setVersion(value(card.getVersion()) + 1);
            matchCardsMapper.updateById(card);
        }
        return hand.size();
    }

    private List<BossAttackTargetResp> resolveBossAttack(Matches match, MatchRounds round, List<MatchPlayers> players) {
        int attack = Math.max(value(match.getBossCurrentAttack()), 0);
        List<BossAttackTargetResp> results = new ArrayList<>();
        for (MatchPlayers player : players) {
            int shieldBefore = value(player.getShield());
            int absorbed = Math.min(shieldBefore, attack);
            int hpDamage = Math.max(attack - shieldBefore, 0);
            int hpBefore = value(player.getCurrentHp());
            int hpAfter = Math.max(hpBefore - hpDamage, 0);
            player.setShield(0);
            player.setCurrentHp(hpAfter);
            player.setDamageTaken(value(player.getDamageTaken()) + hpBefore - hpAfter);
            if (hpAfter <= 0) {
                player.setPlayerStatus("DEAD");
                player.setReviveStatus(1);
            }
            matchPlayersMapper.updateById(player);
            MatchActions action = new MatchActions();
            action.setMatchId(match.getId());
            action.setRoundId(round == null ? null : round.getId());
            action.setActorType("boss");
            action.setActionType("boss_attack");
            action.setTargetUserId(player.getUserId());
            action.setBeforeValue(hpBefore);
            action.setAfterValue(hpAfter);
            action.setDeltaValue(hpAfter - hpBefore);
            action.setExtraData("{\"attack\":" + attack + ",\"shieldBefore\":" + shieldBefore
                    + ",\"absorbedDamage\":" + absorbed + "}");
            matchActionsMapper.insert(action);
            results.add(new BossAttackTargetResp(player.getUserId(), attack, shieldBefore, absorbed, hpBefore,
                    hpBefore - hpAfter, hpAfter, hpAfter <= 0));
        }
        return results;
    }

    private void finishRoundAndStartNext(Matches match, MatchRounds currentRound, List<MatchPlayers> players) {
        if (currentRound != null) {
            currentRound.setRoundStatus(1);
            currentRound.setPhase("FINISHED");
            currentRound.setEndedAt(LocalDateTime.now());
            matchRoundsMapper.updateById(currentRound);
        }
        int nextRoundNo = match.getCurrentRound() + 1;
        CustomerTypes customer = customerTypesMapper.selectById(match.getCustomerTypeId());
        boolean triggered = customer != null && ThreadLocalRandom.current().nextInt(100) < value(customer.getTriggerChance());
        int attack = value(match.getBossBaseAttack());
        if (triggered && customer != null) {
            if ("bully_attack_down".equals(customer.getEffectType()) || "bully_attack_up".equals(customer.getEffectType())) {
                attack = Math.max(0, attack + value(customer.getEffectValue()));
            } else if ("bully_hp_up".equals(customer.getEffectType())) {
                int hpGain = Math.max(value(customer.getEffectValue()), 0);
                match.setBossMaxHp(value(match.getBossMaxHp()) + hpGain);
                match.setBossCurrentHp(value(match.getBossCurrentHp()) + hpGain);
            } else if ("player_hp_up".equals(customer.getEffectType())) {
                int heal = Math.max(value(customer.getEffectValue()), 0);
                for (MatchPlayers player : players) {
                    if (value(player.getCurrentHp()) <= 0) {
                        continue;
                    }
                    int before = value(player.getCurrentHp());
                    player.setCurrentHp(Math.min(value(player.getMaxHp()), before + heal));
                }
            }
        }
        match.setCurrentRound(nextRoundNo);
        match.setPhase(PLAYER_ACTION);
        match.setBossCurrentAttack(attack);
        for (MatchPlayers player : players) {
            player.setActionPoints(value(player.getBaseActionPoints()));
            player.setEndedTurn(0);
            player.setShield(0);
            if (value(player.getCurrentHp()) > 0) {
                player.setReviveStatus(0);
            }
            matchPlayersMapper.updateById(player);
        }
        resolvePendingEffects(match, nextRoundNo);
        if (value(match.getBossCurrentHp()) <= 0) {
            finishMatch(match, 1);
            return;
        }
        for (MatchPlayers player : players) {
            drawCards(match.getId(), player.getUserId(), nextRoundNo, INITIAL_HAND_SIZE);
        }
        MatchRounds nextRound = new MatchRounds();
        nextRound.setMatchId(match.getId());
        nextRound.setRoundNo(nextRoundNo);
        nextRound.setRoundStatus(0);
        nextRound.setPhase(PLAYER_ACTION);
        nextRound.setBossAttack(attack);
        nextRound.setCustomerTriggered(triggered ? 1 : 0);
        nextRound.setCustomerEffectType(customer == null ? null : customer.getEffectType());
        nextRound.setCustomerEffectValue(customer == null ? 0 : value(customer.getEffectValue()));
        nextRound.setBossRageValue(0);
        nextRound.setSatisfactionDelta(0);
        nextRound.setFundsPerPlayer(3);
        nextRound.setStartedAt(LocalDateTime.now());
        if (currentRound != null) {
            nextRound.setFirstPlayerUserId(currentRound.getFirstPlayerUserId());
            nextRound.setChosenByUserId(currentRound.getChosenByUserId());
        }
        matchRoundsMapper.insert(nextRound);
    }

    private void resolvePendingEffects(Matches match, int roundNo) {
        List<MatchPendingEffects> pendingEffects = matchPendingEffectsMapper.selectList(
                Wrappers.<MatchPendingEffects>lambdaQuery().eq(MatchPendingEffects::getMatchId, match.getId())
                        .eq(MatchPendingEffects::getTriggerRound, roundNo).eq(MatchPendingEffects::getStatus, "PENDING"));
        int defense = resolveBossDefense(match.getBullyId());
        for (MatchPendingEffects pending : pendingEffects) {
            if ("MULTIPLY_NEXT_CARD".equals(pending.getEffectType())) {
                continue;
            }
            if ("DAMAGE_BOSS".equals(pending.getEffectType())) {
                int finalDamage = Math.max(0, value(pending.getEffectValue()) - defense);
                match.setBossCurrentHp(Math.max(0, value(match.getBossCurrentHp()) - finalDamage));
                if (finalDamage > 0 && pending.getSourceUserId() != null) {
                    try {
                        taskService.recordBattleAction(pending.getSourceUserId(), null, 0, finalDamage);
                    } catch (Exception e) {
                        logger.warn("Skip delayed damage task progress userId={} matchId={}: {}",
                                pending.getSourceUserId(), match.getId(), e.getMessage());
                    }
                }
            } else if ("REDUCE_BOSS_ATTACK".equals(pending.getEffectType())) {
                match.setBossCurrentAttack(Math.max(0, value(match.getBossCurrentAttack()) - value(pending.getEffectValue())));
            } else if ("INCREASE_BOSS_ATTACK".equals(pending.getEffectType())) {
                match.setBossCurrentAttack(value(match.getBossCurrentAttack()) + Math.max(value(pending.getEffectValue()), 0));
            } else if ("HEAL_BOSS".equals(pending.getEffectType())) {
                int hpGain = Math.max(value(pending.getEffectValue()), 0);
                match.setBossMaxHp(value(match.getBossMaxHp()) + hpGain);
                match.setBossCurrentHp(value(match.getBossCurrentHp()) + hpGain);
            } else if ("DRAW_CARDS".equals(pending.getEffectType()) && pending.getTargetUserId() != null) {
                drawCards(match.getId(), pending.getTargetUserId(), roundNo, value(pending.getEffectValue()));
            } else {
                MatchPlayers target = pending.getTargetUserId() == null ? null : matchPlayersMapper.selectOne(
                        Wrappers.<MatchPlayers>lambdaQuery().eq(MatchPlayers::getMatchId, match.getId())
                                .eq(MatchPlayers::getUserId, pending.getTargetUserId()));
                if (target != null && "ADD_SHIELD".equals(pending.getEffectType())) {
                    target.setShield(value(target.getShield()) + value(pending.getEffectValue()));
                    matchPlayersMapper.updateById(target);
                } else if (target != null && "HEAL_PLAYER".equals(pending.getEffectType())) {
                    target.setCurrentHp(Math.min(target.getMaxHp(), value(target.getCurrentHp()) + value(pending.getEffectValue())));
                    matchPlayersMapper.updateById(target);
                } else if (target != null && "ADD_ACTION_POINTS".equals(pending.getEffectType())) {
                    // 下回合加调用机会：加在本回合已刷新的 actionPoints 上，不要永久改 base
                    target.setActionPoints(value(target.getActionPoints()) + value(pending.getEffectValue()));
                    matchPlayersMapper.updateById(target);
                } else if (target != null && "DAMAGE_PLAYER".equals(pending.getEffectType())) {
                    applyPlayerHpLoss(target, Math.max(value(pending.getEffectValue()), 0));
                    matchPlayersMapper.updateById(target);
                }
            }
            int remaining = value(pending.getRemainingTriggers()) - 1;
            pending.setRemainingTriggers(Math.max(remaining, 0));
            if (remaining <= 0) {
                pending.setStatus("RESOLVED");
            } else {
                pending.setTriggerRound(roundNo + 1);
            }
            matchPendingEffectsMapper.updateById(pending);
        }
    }

    private int countCards(Long matchId, Long userId, String zone) {
        return Math.toIntExact(matchCardsMapper.selectCount(Wrappers.<MatchCards>lambdaQuery()
                .eq(MatchCards::getMatchId, matchId)
                .eq(MatchCards::getUserId, userId)
                .eq(MatchCards::getZone, zone)));
    }

    /**
     * 从牌库随机抽牌。
     * 规则：只有当 DECK 抽空（本轮牌库中的牌全部抽完）后，才允许把 DISCARD 洗回 DECK；
     * 牌库仍有牌时绝不触碰弃牌堆。
     */
    private int drawCards(Long matchId, Long userId, int roundNo, int count) {
        int drawn = 0;
        while (drawn < count) {
            int deckCount = countCards(matchId, userId, "DECK");
            if (deckCount <= 0) {
                // 牌库已空 = 当前牌库中的牌都抽完了，才洗弃牌堆
                if (!reshuffleDiscardIntoDeck(matchId, userId)) {
                    break;
                }
                continue;
            }

            List<MatchCards> deck = matchCardsMapper.selectList(Wrappers.<MatchCards>lambdaQuery()
                    .eq(MatchCards::getMatchId, matchId)
                    .eq(MatchCards::getUserId, userId)
                    .eq(MatchCards::getZone, "DECK"));
            Collections.shuffle(deck);
            // 本轮最多抽完当前牌库，不够的等牌库空了再洗弃牌
            int need = Math.min(count - drawn, deck.size());
            for (int i = 0; i < need; i++) {
                MatchCards card = deck.get(i);
                card.setZone("HAND");
                card.setDeckOrder(null);
                card.setDrawnRound(roundNo);
                card.setVersion(value(card.getVersion()) + 1);
                matchCardsMapper.updateById(card);
                drawn++;
            }
            reindexDeckOrder(matchId, userId);
        }
        return drawn;
    }

    /** @return true 若成功将弃牌洗入牌库；false 若弃牌也为空 */
    private boolean reshuffleDiscardIntoDeck(Long matchId, Long userId) {
        if (countCards(matchId, userId, "DECK") > 0) {
            // 牌库未抽完，禁止洗弃牌
            return false;
        }
        List<MatchCards> discard = matchCardsMapper.selectList(Wrappers.<MatchCards>lambdaQuery()
                .eq(MatchCards::getMatchId, matchId)
                .eq(MatchCards::getUserId, userId)
                .eq(MatchCards::getZone, "DISCARD"));
        if (discard.isEmpty()) {
            return false;
        }
        Collections.shuffle(discard);
        for (int index = 0; index < discard.size(); index++) {
            MatchCards card = discard.get(index);
            card.setZone("DECK");
            card.setDeckOrder(index + 1);
            card.setVersion(value(card.getVersion()) + 1);
            matchCardsMapper.updateById(card);
        }
        return true;
    }

    private void reindexDeckOrder(Long matchId, Long userId) {
        List<MatchCards> remaining = matchCardsMapper.selectList(Wrappers.<MatchCards>lambdaQuery()
                .eq(MatchCards::getMatchId, matchId)
                .eq(MatchCards::getUserId, userId)
                .eq(MatchCards::getZone, "DECK"));
        Collections.shuffle(remaining);
        for (int index = 0; index < remaining.size(); index++) {
            MatchCards card = remaining.get(index);
            card.setDeckOrder(index + 1);
            card.setVersion(value(card.getVersion()) + 1);
            matchCardsMapper.updateById(card);
        }
    }

    private int resolveBossDefense(Long bullyId) {
        if (bullyId == null) {
            return 0;
        }
        Bullies bully = bulliesMapper.selectById(bullyId);
        return bully == null ? 0 : value(bully.getDefenseValue());
    }

    private void finishMatch(Matches match, int winnerType) {
        finishMatch(match, winnerType, true);
    }

    /**
     * @param grantRewards false 表示放弃/掉线强退等：只结束对局并记胜负，不发金币、经验、任务进度
     */
    private void finishMatch(Matches match, int winnerType, boolean grantRewards) {
        if (value(match.getStatus()) == 2 && value(match.getWinnerType()) != 0) {
            return;
        }
        LocalDateTime endedAt = LocalDateTime.now();
        Integer duration = match.getStartedAt() == null
                ? null
                : (int) Math.max(java.time.Duration.between(match.getStartedAt(), endedAt).getSeconds(), 0);
        int claimed = matchesMapper.update(null, Wrappers.<Matches>lambdaUpdate()
                .eq(Matches::getId, match.getId())
                .eq(Matches::getStatus, 1)
                .set(Matches::getStatus, 2)
                .set(Matches::getPhase, "FINISHED")
                .set(Matches::getWinnerType, winnerType)
                .set(Matches::getEndedAt, endedAt)
                .set(duration != null, Matches::getDurationSeconds, duration)
                .set(Matches::getVersion, nextVersion(match.getVersion())));
        if (claimed == 0) {
            Matches latest = matchesMapper.selectById(match.getId());
            if (latest != null) {
                match.setStatus(latest.getStatus());
                match.setPhase(latest.getPhase());
                match.setWinnerType(latest.getWinnerType());
                match.setEndedAt(latest.getEndedAt());
                match.setDurationSeconds(latest.getDurationSeconds());
                match.setVersion(latest.getVersion());
            }
            return;
        }
        match.setStatus(2);
        match.setPhase("FINISHED");
        match.setWinnerType(winnerType);
        match.setEndedAt(endedAt);
        match.setDurationSeconds(duration);
        match.setVersion(nextVersion(match.getVersion()));
        MatchRounds round = currentRound(match);
        if (round != null && value(round.getRoundStatus()) == 0) {
            round.setRoundStatus(1);
            round.setPhase("FINISHED");
            round.setEndedAt(endedAt);
            matchRoundsMapper.updateById(round);
        }
        GameRooms room = gameRoomsMapper.selectById(match.getRoomId());
        if (room != null) {
            room.setStatus(3);
            room.setClosedAt(endedAt);
            room.setPlayerCount(0);
            gameRoomsMapper.updateById(room);
        }
        List<RoomMembers> roomMembers = roomMembersMapper.selectList(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, match.getRoomId()).isNull(RoomMembers::getLeftAt));
        for (RoomMembers member : roomMembers) {
            member.setLeftAt(endedAt);
            member.setOnlineStatus(0);
            member.setReadyStatus(0);
            roomMembersMapper.updateById(member);
        }
        for (MatchPlayers player : listPlayers(match.getId())) {
            player.setResultType(winnerType == 1 ? 1 : winnerType == 2 ? 2 : 3);
            player.setFinalConfidence(value(player.getCurrentHp()));
            player.setPlayerStatus(winnerType == 1 ? "ACTIVE" : winnerType == 2 ? "LEFT" : "LEFT");
            matchPlayersMapper.updateById(player);
            applyProfileSettlement(player.getUserId(), winnerType, grantRewards);
        }
        if (room != null) {
            Map<String, Object> closed = Map.of(
                    "type", "room.closed",
                    "data", Map.of("roomId", room.getId(), "reason", "match_finished"));
            for (RoomMembers member : roomMembers) {
                notificationService.notifyUser(member.getUserId(), closed);
                userPresenceService.broadcastPresence(member.getUserId());
            }
        }
        if (grantRewards && (winnerType == 1 || winnerType == 2)) {
            List<MatchPlayers> settledPlayers = listPlayers(match.getId());
            for (MatchPlayers player : settledPlayers) {
                try {
                    String resultType = winnerType == 1 ? "WIN" : "LOSE";
                    Long teammateId = settledPlayers.stream()
                            .filter(p -> !p.getUserId().equals(player.getUserId()))
                            .map(MatchPlayers::getUserId)
                            .findFirst().orElse(null);
                    taskService.recordMatchResult(player.getUserId(), resultType, teammateId);
                } catch (Exception e) {
                    logger.warn("Skip match task progress userId={} matchId={}: {}",
                            player.getUserId(), match.getId(), e.getMessage());
                }
            }
            if (winnerType == 1) {
                for (MatchPlayers player : settledPlayers) {
                    try {
                        Cards unlocked = cardCollectionService.unlockRandomCollectible(player.getUserId());
                        if (unlocked == null) {
                            continue;
                        }
                        MatchActions unlockAction = new MatchActions();
                        unlockAction.setMatchId(match.getId());
                        unlockAction.setRoundId(round == null ? null : round.getId());
                        unlockAction.setActorType("system");
                        unlockAction.setActorUserId(player.getUserId());
                        unlockAction.setActionType("unlock_card");
                        unlockAction.setCardId(unlocked.getId());
                        matchActionsMapper.insert(unlockAction);
                    } catch (Exception e) {
                        logger.warn("Skip card unlock userId={} matchId={}: {}",
                                player.getUserId(), match.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    private void applyProfileSettlement(Long userId, int winnerType, boolean grantRewards) {
        leaderboardService.ensureCurrentWeek();
        int winDelta = winnerType == 1 ? 1 : 0;
        int loseDelta = winnerType == 2 ? 1 : 0;
        int drawDelta = winnerType == 1 || winnerType == 2 ? 0 : 1;
        int expDelta = grantRewards ? rewardExp(winnerType) : 0;
        long moneyDelta = grantRewards ? rewardMoney(winnerType) : 0L;
        userProfileMapper.applyMatchSettlement(userId, winDelta, loseDelta, drawDelta, expDelta, moneyDelta);
    }

    private int rewardExp(int winnerType) {
        return winnerType == 1 ? VICTORY_EXP : winnerType == 2 ? DEFEAT_EXP : 0;
    }

    private long rewardMoney(int winnerType) {
        return winnerType == 1 ? VICTORY_MONEY : winnerType == 2 ? DEFEAT_MONEY : 0L;
    }

    private void insertEndTurnAction(Long matchId, MatchRounds round, MatchPlayers actor, String clientActionId,
                                     int discardedCount) {
        MatchActions action = new MatchActions();
        action.setMatchId(matchId);
        action.setRoundId(round == null ? null : round.getId());
        action.setActorType("player");
        action.setActorUserId(actor.getUserId());
        action.setActionType("end_turn");
        action.setBeforeValue(0);
        action.setAfterValue(1);
        action.setDeltaValue(1);
        action.setExtraData("{\"clientActionId\":\"" + clientActionId + "\",\"discardedCount\":"
                + discardedCount + "}");
        matchActionsMapper.insert(action);
    }

    private MatchActions findClientAction(Long matchId, Long userId, String clientActionId) {
        return matchActionsMapper.selectOne(Wrappers.<MatchActions>lambdaQuery()
                .eq(MatchActions::getMatchId, matchId).eq(MatchActions::getActorUserId, userId)
                .like(MatchActions::getExtraData, "\"clientActionId\":\"" + clientActionId + "\"")
                .last("LIMIT 1"));
    }

    private Bullies requireBully(Long bullyId) {
        Bullies bully = bulliesMapper.selectById(bullyId);
        if (bully == null) {
            throw new BusinessException("霸凌者不存在");
        }
        return bully;
    }

    private record RoundSettleResult(boolean attackResolved, boolean matchEnded, List<BossAttackTargetResp> targets) {
    }

    private void enterReviveWait(Matches match, List<MatchPlayers> players) {
        LocalDateTime now = LocalDateTime.now();
        match.setPhase("REVIVE_WAIT");
        match.setVersion(nextVersion(match.getVersion()));
        match.setUpdatedAt(now);
        matchesMapper.updateById(match);
        for (MatchPlayers player : players) {
            if (value(player.getCurrentHp()) <= 0 && value(player.getReviveCount()) < value(player.getReviveLimit())) {
                player.setReviveStatus(1);
                player.setUpdatedAt(now);
                matchPlayersMapper.updateById(player);
            }
        }
    }

    private boolean isLivingPlayer(MatchPlayers player) {
        return value(player.getCurrentHp()) > 0
                && !"DEAD".equals(player.getPlayerStatus())
                && !"LEFT".equals(player.getPlayerStatus());
    }

    private boolean allLivingPlayersEnded(List<MatchPlayers> players) {
        List<MatchPlayers> living = players.stream().filter(this::isLivingPlayer).toList();
        return !living.isEmpty() && living.stream().allMatch(player -> value(player.getEndedTurn()) == 1);
    }

    private boolean hasBossAttackThisRound(Matches match) {
        MatchRounds round = currentRound(match);
        if (round == null) {
            return false;
        }
        Long count = matchActionsMapper.selectCount(Wrappers.<MatchActions>lambdaQuery()
                .eq(MatchActions::getMatchId, match.getId())
                .eq(MatchActions::getRoundId, round.getId())
                .eq(MatchActions::getActionType, "boss_attack"));
        return count != null && count > 0;
    }

    private void resumeStuckRoundIfNeeded(Matches match) {
        if (value(match.getStatus()) != 1) {
            return;
        }
        if ("SELECT_FIRST_PLAYER".equals(match.getPhase()) || "FINISHED".equals(match.getPhase()) || "REVIVE_WAIT".equals(match.getPhase())) {
            return;
        }
        List<MatchPlayers> players = listPlayers(match.getId());
        if (!allLivingPlayersEnded(players)) {
            return;
        }
        String phase = match.getPhase();
        if (!PLAYER_ACTION.equals(phase) && !"RECONNECT_WAIT".equals(phase) && !"BOSS_ACTION".equals(phase)) {
            return;
        }
        MatchRounds round = currentRound(match);
        if (hasBossAttackThisRound(match)) {
            completeRoundAfterBossAttack(match, round, players);
            return;
        }
        int claimed = matchesMapper.update(null, Wrappers.<Matches>lambdaUpdate()
                .eq(Matches::getId, match.getId())
                .eq(Matches::getVersion, match.getVersion())
                .in(Matches::getPhase, List.of(PLAYER_ACTION, "RECONNECT_WAIT", "BOSS_ACTION"))
                .set(Matches::getPhase, "BOSS_ACTION")
                .set(Matches::getVersion, nextVersion(match.getVersion())));
        if (claimed == 0) {
            return;
        }
        match.setPhase("BOSS_ACTION");
        match.setVersion(nextVersion(match.getVersion()));
        RoundSettleResult settled = settleEndedRound(match, round, players);
        if (settled.matchEnded()) {
            notifyPlayers(match.getId(), "match.ended", Map.of(
                    "matchId", match.getId(),
                    "winnerType", match.getWinnerType(),
                    "reason", "round_settled"));
        } else if (PLAYER_ACTION.equals(match.getPhase())) {
            notifyPlayers(match.getId(), "round.started", Map.of(
                    "matchId", match.getId(),
                    "currentRound", match.getCurrentRound(),
                    "phase", match.getPhase(),
                    "version", match.getVersion()));
        }
    }

    private void completeRoundAfterBossAttack(Matches match, MatchRounds round, List<MatchPlayers> players) {
        boolean allDead = players.stream().allMatch(player -> value(player.getCurrentHp()) <= 0);
        boolean anyDead = players.stream().anyMatch(player -> value(player.getCurrentHp()) <= 0);
        if (allDead) {
            finishMatch(match, 2);
            notifyPlayers(match.getId(), "match.ended", Map.of(
                    "matchId", match.getId(), "winnerType", 2, "reason", "round_settled"));
            return;
        }
        if (anyDead) {
            enterReviveWait(match, players);
            notifyPlayers(match.getId(), "match.revive.required", Map.of(
                    "matchId", match.getId(), "phase", "REVIVE_WAIT", "timeoutSeconds", REVIVE_TIMEOUT_MILLIS / 1000));
            return;
        }
        finishRoundAndStartNext(match, round, players);
        match.setVersion(nextVersion(match.getVersion()));
        matchesMapper.updateById(match);
        notifyPlayers(match.getId(), "round.started", Map.of(
                "matchId", match.getId(),
                "currentRound", match.getCurrentRound(),
                "phase", match.getPhase(),
                "version", match.getVersion()));
    }

    private RoundSettleResult settleEndedRound(Matches match, MatchRounds round, List<MatchPlayers> players) {
        if (hasBossAttackThisRound(match)) {
            completeRoundAfterBossAttack(match, round, players);
            boolean matchEnded = value(match.getStatus()) == 2;
            return new RoundSettleResult(true, matchEnded, List.of());
        }
        if (value(match.getBossCurrentHp()) <= 0) {
            finishMatch(match, 1);
            return new RoundSettleResult(false, true, List.of());
        }
        match.setPhase("BOSS_ACTION");
        if (round != null) {
            round.setPhase("BOSS_ACTION");
            matchRoundsMapper.updateById(round);
        }
        List<BossAttackTargetResp> targets = resolveBossAttack(match, round, players);
        boolean anyDead = targets.stream().anyMatch(BossAttackTargetResp::dead);
        boolean allDead = players.stream().allMatch(player -> value(player.getCurrentHp()) <= 0);
        if (allDead) {
            finishMatch(match, 2);
            return new RoundSettleResult(true, true, targets);
        }
        if (anyDead) {
            enterReviveWait(match, players);
            notifyPlayers(match.getId(), "match.revive.required", Map.of(
                    "matchId", match.getId(), "phase", "REVIVE_WAIT", "timeoutSeconds", REVIVE_TIMEOUT_MILLIS / 1000));
            return new RoundSettleResult(true, false, targets);
        }
        finishRoundAndStartNext(match, round, players);
        boolean matchEnded = value(match.getStatus()) == 2 && value(match.getWinnerType()) != 0;
        return new RoundSettleResult(true, matchEnded, targets);
    }

    private MatchRounds currentRound(Matches match) {
        return matchRoundsMapper.selectOne(Wrappers.<MatchRounds>lambdaQuery()
                .eq(MatchRounds::getMatchId, match.getId()).eq(MatchRounds::getRoundNo, match.getCurrentRound()));
    }

    private List<MatchPlayers> listPlayers(Long matchId) {
        return matchPlayersMapper.selectList(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getMatchId, matchId).orderByAsc(MatchPlayers::getSeatNo));
    }

    private void notifyAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private void notifyPlayers(Long matchId, String type, Object data) {
        Map<String, Object> payload = Map.of("type", type, "data", data);
        for (MatchPlayers player : listPlayers(matchId)) {
            notificationService.notifyUser(player.getUserId(), payload);
        }
    }

    private void notifyCardPlayed(Long matchId, MatchActionResp response) {
        Map<String, Object> payload = Map.of("type", "card.played", "data", response);
        for (MatchPlayers player : matchPlayersMapper.selectList(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getMatchId, matchId))) {
            notificationService.notifyUser(player.getUserId(), payload);
        }
    }

    private void notifyMatchEnded(Matches match, MatchActionResp response) {
        Map<String, Object> payload = Map.of("type", "match.ended", "data", response);
        for (MatchPlayers player : matchPlayersMapper.selectList(Wrappers.<MatchPlayers>lambdaQuery()
                .eq(MatchPlayers::getMatchId, match.getId()))) {
            notificationService.notifyUser(player.getUserId(), payload);
            userPresenceService.broadcastPresence(player.getUserId());
        }
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private long nextVersion(Long version) {
        return version == null ? 1L : version + 1L;
    }
}
