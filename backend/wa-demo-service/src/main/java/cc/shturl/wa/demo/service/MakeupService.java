package cc.shturl.wa.demo.service;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.resp.MakeupOfferResp;
import cc.shturl.wa.demo.dto.resp.TaskClaimResp;
import cc.shturl.wa.demo.entity.RoomMembers;
import cc.shturl.wa.demo.mapper.RoomMembersMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 每人每天最多补 1 局，而且要等今天的 3 局打完才能勾选。
 * 补的是本月更早没打完的那一档。打完后到任务列表领取，金币按这一档是第 1、2、3 局变化。
 */
@Service
@RequiredArgsConstructor
public class MakeupService {
    private static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final WorkDayService workDayService;
    private final UserProfileMapper userProfileMapper;
    private final LeaderboardService leaderboardService;
    private final RoomMembersMapper roomMembersMapper;

    public record Reservation(LocalDate owedDay, int slot) {
    }

    private record Pending(long id, LocalDate day, int slot, int finishGold, int winGold,
                           int finishClaimed, int winClaimed) {
    }

    public MakeupOfferResp describe(Long userId) {
        if (userId == null || !ready()) {
            return MakeupOfferResp.none();
        }
        LocalDate today = LocalDate.now(WorkDayQuota.ZONE);
        LocalDate start = WorkDayQuota.countStart(YearMonth.from(today));
        MakeupQueue.Next next = MakeupQueue.next(today, start, usedSlots(userId, start, today), claimed(userId, start, today));
        Pending pending = findPending(userId);
        if (next == null && pending == null) {
            return MakeupOfferResp.none();
        }
        boolean usedToday = usedToday(userId, today);
        boolean dailyReady = playedToday(userId, today) >= 3;
        String owedDay = next == null ? null : next.day().toString();
        int slot = next == null ? 0 : next.slot();
        int finishGold = MakeupQueue.finishGold(slot);
        boolean pays = next != null && workDayService.couldPayGold(userId, next.day());
        boolean available = next != null && dailyReady && !usedToday;
        String questDay = pending == null ? owedDay : pending.day().toString();
        int questSlot = pending == null ? slot : pending.slot();
        int questFinish = pending == null ? finishGold : pending.finishGold();
        int questWin = pending == null ? MakeupQueue.WIN_GOLD : pending.winGold();
        boolean finishReady = pending != null && pending.finishClaimed() == 0 && pending.finishGold() > 0;
        boolean winReady = pending != null && pending.winClaimed() == 0 && pending.winGold() > 0;
        return new MakeupOfferResp(available, owedDay, slot, finishGold, MakeupQueue.WIN_GOLD,
                usedToday, pays, next == null ? 0 : next.owedCount(), dailyReady,
                questDay, questSlot, questFinish, questWin, finishReady, winReady);
    }

    public Reservation requireReservation(Long userId) {
        return reserve(userId, false);
    }

    public Reservation requireReservationForStart(Long userId) {
        return reserve(userId, true);
    }

    private Reservation reserve(Long userId, boolean starting) {
        MakeupOfferResp offer = describe(userId);
        if (!offer.available()) {
            String reason = !offer.dailyReady() && offer.slot() > 0
                    ? "今天打满 3 局后才能补打"
                    : offer.usedToday() ? "今天的补打已经用过" : "今天没有可补的局";
            throw new BusinessException(starting ? "这局不能按补打开局：" + reason : reason);
        }
        return new Reservation(LocalDate.parse(offer.owedDay()), offer.slot());
    }

    @Transactional
    public void setOptIn(Long userId, Long roomId, boolean enabled) {
        RoomMembers member = roomMembersMapper.selectOne(Wrappers.<RoomMembers>lambdaQuery()
                .eq(RoomMembers::getRoomId, roomId)
                .eq(RoomMembers::getUserId, userId)
                .isNull(RoomMembers::getLeftAt)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BusinessException("你不在这个房间");
        }
        if (enabled) {
            requireReservation(userId);
        }
        member.setMakeupOptIn(enabled ? 1 : 0);
        roomMembersMapper.updateById(member);
    }

    /**
     * 打完记下这一档，金币留到任务列表领取。放弃记 0 并消耗这一档。作废不要调用。
     */
    @Transactional
    public void settle(Long userId, Long matchId, LocalDate owedDay, int slot, boolean forfeit, boolean won) {
        if (userId == null || matchId == null || owedDay == null || slot < 1 || slot > 3 || !ready()) {
            return;
        }
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_makeup_claims WHERE user_id = ? AND owed_day = ? AND slot_no = ?",
                Integer.class, userId, Date.valueOf(owedDay), slot);
        if (existing != null && existing > 0) {
            return;
        }
        int finishGold = 0;
        int winGold = 0;
        int finishClaimed = 1;
        int winClaimed = 1;
        if (!forfeit && workDayService.couldPayGold(userId, owedDay)) {
            finishGold = MakeupQueue.finishGold(slot);
            winGold = won ? MakeupQueue.WIN_GOLD : 0;
            finishClaimed = 0;
            winClaimed = winGold > 0 ? 0 : 1;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO user_makeup_claims(
                        user_id, owed_day, slot_no, match_id, gold_paid,
                        finish_gold, win_gold, finish_claimed, win_claimed, created_at)
                    VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?)
                    """, userId, Date.valueOf(owedDay), slot, matchId,
                    finishGold, winGold, finishClaimed, winClaimed, Timestamp.valueOf(LocalDateTime.now()));
        } catch (DuplicateKeyException ignored) {
            // 这一档已经记过。
        }
    }

    @Transactional
    public TaskClaimResp claim(Long userId, String part) {
        if (userId == null || !ready()) {
            throw new BusinessException("没有可领的补打奖励");
        }
        boolean win = "win".equalsIgnoreCase(part);
        if (!win && !"finish".equalsIgnoreCase(part)) {
            throw new BusinessException("没有可领的补打奖励");
        }
        Pending pending = lockPending(userId);
        if (pending == null) {
            throw new BusinessException("没有可领的补打奖励");
        }
        int gold = win ? pending.winGold() : pending.finishGold();
        int claimed = win ? pending.winClaimed() : pending.finishClaimed();
        if (claimed != 0 || gold <= 0) {
            throw new BusinessException("补打奖励已领取");
        }
        if (!workDayService.allowGold(userId, pending.day())) {
            gold = 0;
        }
        String flag = win ? "win_claimed" : "finish_claimed";
        int updated = jdbcTemplate.update("""
                UPDATE user_makeup_claims
                SET %s = 1, gold_paid = gold_paid + ?
                WHERE id = ? AND user_id = ? AND %s = 0
                """.formatted(flag, flag), gold, pending.id(), userId);
        if (updated != 1) {
            throw new BusinessException("补打奖励已领取");
        }
        if (gold > 0) {
            leaderboardService.ensureCurrentMonth();
            userProfileMapper.applyMatchSettlement(userId, 0, 0, 0, 0, gold);
        }
        String message = gold > 0 ? "奖励领取成功" : "本月工作日已满，这局补打不发金币";
        return new TaskClaimResp(pending.id(), pending.id(), 1, 1, 3, message, (long) gold, 0);
    }

    private int playedToday(Long userId, LocalDate today) {
        return usedSlots(userId, today, today.plusDays(1)).getOrDefault(today, 0);
    }

    private Pending findPending(Long userId) {
        List<Pending> rows = jdbcTemplate.query("""
                SELECT id, owed_day, slot_no, finish_gold, win_gold, finish_claimed, win_claimed
                FROM user_makeup_claims
                WHERE user_id = ? AND (finish_claimed = 0 OR win_claimed = 0)
                ORDER BY owed_day, slot_no
                LIMIT 1
                """, (rs, row) -> pending(rs), userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Pending lockPending(Long userId) {
        List<Pending> rows = jdbcTemplate.query("""
                SELECT id, owed_day, slot_no, finish_gold, win_gold, finish_claimed, win_claimed
                FROM user_makeup_claims
                WHERE user_id = ? AND (finish_claimed = 0 OR win_claimed = 0)
                ORDER BY owed_day, slot_no
                LIMIT 1
                FOR UPDATE
                """, (rs, row) -> pending(rs), userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Pending pending(java.sql.ResultSet rs) throws java.sql.SQLException {
        Date day = rs.getDate("owed_day");
        return new Pending(rs.getLong("id"), day == null ? null : day.toLocalDate(),
                rs.getInt("slot_no"), rs.getInt("finish_gold"), rs.getInt("win_gold"),
                rs.getInt("finish_claimed"), rs.getInt("win_claimed"));
    }

    private Map<LocalDate, Integer> usedSlots(Long userId, LocalDate start, LocalDate today) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        jdbcTemplate.query("""
                SELECT COALESCE(m.ended_at, m.started_at, m.created_at) AS ended_at
                FROM matches m
                INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                WHERE m.status = 2
                  AND IFNULL(m.winner_type, 0) IN (1, 2)
                  AND me.makeup_slot IS NULL
                  AND (SELECT COUNT(*) FROM match_players mp WHERE mp.match_id = m.id) >= 2
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                """, rs -> {
            Timestamp ended = rs.getTimestamp("ended_at");
            if (ended == null) {
                return;
            }
            LocalDate day = shanghaiDay(ended.toLocalDateTime());
            counts.merge(day, 1, Integer::sum);
        }, userId, utcWall(start), utcWall(today));
        return counts;
    }

    private Set<MakeupQueue.DaySlot> claimed(Long userId, LocalDate start, LocalDate today) {
        Set<MakeupQueue.DaySlot> claimed = new HashSet<>();
        jdbcTemplate.query("""
                SELECT owed_day, slot_no
                FROM user_makeup_claims
                WHERE user_id = ? AND owed_day >= ? AND owed_day < ?
                """, rs -> {
            Date day = rs.getDate("owed_day");
            if (day == null) {
                return;
            }
            claimed.add(new MakeupQueue.DaySlot(day.toLocalDate(), rs.getInt("slot_no")));
        }, userId, Date.valueOf(start), Date.valueOf(today));
        return claimed;
    }

    private boolean usedToday(Long userId, LocalDate today) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM match_players me
                INNER JOIN matches m ON m.id = me.match_id
                WHERE me.user_id = ?
                  AND me.makeup_slot IS NOT NULL
                  AND (
                    m.status = 1
                    OR (
                      m.status = 2
                      AND IFNULL(m.winner_type, 0) IN (1, 2)
                      AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                      AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                    )
                  )
                """, Integer.class, userId, utcWall(today), utcWall(today.plusDays(1)));
        return count != null && count > 0;
    }

    private boolean ready() {
        return tableExists("user_makeup_claims")
                && columnExists("user_makeup_claims", "finish_claimed")
                && columnExists("match_players", "makeup_slot")
                && columnExists("room_members", "makeup_opt_in");
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        return count != null && count > 0;
    }

    private String utcWall(LocalDate shanghaiDay) {
        return shanghaiDay.atStartOfDay(WorkDayQuota.ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
                .format(SQL_TIME);
    }

    private LocalDate shanghaiDay(LocalDateTime utcWall) {
        return utcWall.atZone(ZoneOffset.UTC).withZoneSameInstant(WorkDayQuota.ZONE).toLocalDate();
    }
}
