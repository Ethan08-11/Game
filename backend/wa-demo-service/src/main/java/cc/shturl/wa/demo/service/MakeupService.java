package cc.shturl.wa.demo.service;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.resp.MakeupOfferResp;
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
import java.util.Map;
import java.util.Set;

/**
 * 每人每天最多补 1 局。补的是本月更早没打完的那一档，同时仍计入今天的胜负。
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

    public MakeupOfferResp describe(Long userId) {
        if (userId == null || !ready()) {
            return MakeupOfferResp.none();
        }
        LocalDate today = LocalDate.now(WorkDayQuota.ZONE);
        LocalDate start = WorkDayQuota.countStart(YearMonth.from(today));
        MakeupQueue.Next next = MakeupQueue.next(today, start, usedSlots(userId, start, today), claimed(userId, start, today));
        if (next == null) {
            return MakeupOfferResp.none();
        }
        boolean usedToday = usedToday(userId, today);
        boolean pays = workDayService.couldPayGold(userId, next.day());
        return new MakeupOfferResp(!usedToday, next.day().toString(), next.slot(),
                MakeupQueue.finishGold(next.slot()), MakeupQueue.WIN_GOLD,
                usedToday, pays, next.owedCount());
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
            String reason = offer.usedToday() ? "今天的补打已经用过" : "今天没有可补的局";
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
     * 打完发这一档的金币；放弃记 0 金币并消耗这一档。作废不要调用。
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
        int gold = 0;
        if (!forfeit) {
            gold = MakeupQueue.finishGold(slot) + (won ? MakeupQueue.WIN_GOLD : 0);
            if (!workDayService.couldPayGold(userId, owedDay)) {
                gold = 0;
            } else {
                workDayService.allowGold(userId, owedDay);
            }
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO user_makeup_claims(user_id, owed_day, slot_no, match_id, gold_paid, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, userId, Date.valueOf(owedDay), slot, matchId, gold, Timestamp.valueOf(LocalDateTime.now()));
        } catch (DuplicateKeyException ignored) {
            return;
        }
        if (gold > 0) {
            leaderboardService.ensureCurrentMonth();
            userProfileMapper.applyMatchSettlement(userId, 0, 0, 0, 0, gold);
        }
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
