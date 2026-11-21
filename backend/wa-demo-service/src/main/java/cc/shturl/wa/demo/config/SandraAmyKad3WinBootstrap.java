package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.WorkDayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sandra 与 Amy 打「硬扛恶霸」打到第 15 回合、霸凌者剩 3 血时，技术部 Kad3 被误判成需要防御，没能打出。
 * 这局改成胜利：胜率按胜利结算，并补上这一档还没领的完成金币和胜利金币。
 */
@Component
@Order(60)
public class SandraAmyKad3WinBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SandraAmyKad3WinBootstrap.class);
    static final String PATCH_ID = "sandra_amy_kad3_round15_win";
    private static final long SANDRA_ID = 35L;
    private static final long AMY_ID = 24L;
    private static final int VICTORY_EXP = 100;
    private static final int DEFEAT_EXP = 30;
    private static final int WIN_GOLD = 10;
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final WorkDayService workDayService;

    public SandraAmyKad3WinBootstrap(JdbcTemplate jdbcTemplate,
                                     PlatformTransactionManager transactionManager,
                                     WorkDayService workDayService) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.workDayService = workDayService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("matches") || !tableExists("match_players") || !tableExists("user_profiles")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (restore()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Sandra/Amy Kad3 win restore failed.", e);
        }
    }

    private boolean restore() {
        Map<String, Object> match = findMatch();
        if (match == null) {
            log.warn("Sandra/Amy Kad3 match is not in the database yet.");
            return false;
        }
        long matchId = ((Number) match.get("id")).longValue();
        int status = ((Number) match.get("status")).intValue();
        int winner = ((Number) match.get("winner_type")).intValue();
        if (status == 2 && winner == 1) {
            log.warn("Sandra/Amy match {} is already a win.", matchId);
            return true;
        }
        if (!(status == 1 || (status == 2 && winner == 2))) {
            log.warn("Skip Sandra/Amy match {} status {} winner {}.", matchId, status, winner);
            return false;
        }
        boolean wasLoss = status == 2 && winner == 2;
        int updated = jdbcTemplate.update("""
                UPDATE matches
                SET winner_type = 1,
                    status = 2,
                    phase = 'FINISHED',
                    boss_current_hp = 0,
                    ended_at = IFNULL(ended_at, UTC_TIMESTAMP()),
                    duration_seconds = IFNULL(duration_seconds,
                        TIMESTAMPDIFF(SECOND, started_at, IFNULL(ended_at, UTC_TIMESTAMP())))
                WHERE id = ?
                  AND (status = 1 OR (status = 2 AND winner_type = 2))
                """, matchId);
        if (updated != 1) {
            log.warn("Skip Sandra/Amy restore; match {} changed before the update.", matchId);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE match_players
                SET result_type = 1,
                    player_status = 'ACTIVE'
                WHERE match_id = ?
                """, matchId);
        if (!wasLoss) {
            closeRoom(match.get("room_id"), matchId);
        }
        String endedAt = jdbcTemplate.query("""
                SELECT DATE_FORMAT(ended_at, '%Y-%m-%d %H:%i:%s')
                FROM matches WHERE id = ?
                """, rs -> rs.next() ? rs.getString(1) : null, matchId);
        LocalDate day = shanghaiDay(string(match.get("started_at")));
        settlePlayer(SANDRA_ID, AMY_ID, matchId, wasLoss, day, endedAt);
        settlePlayer(AMY_ID, SANDRA_ID, matchId, wasLoss, day, endedAt);
        log.warn("Restored Sandra/Amy match {} as a win. wasLoss={}", matchId, wasLoss);
        return true;
    }

    private Map<String, Object> findMatch() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT m.id,
                       m.status,
                       IFNULL(m.winner_type, 0) AS winner_type,
                       IFNULL(m.current_round, 0) AS current_round,
                       IFNULL(m.boss_current_hp, -1) AS boss_current_hp,
                       m.room_id,
                       m.boss_name,
                       DATE_FORMAT(m.started_at, '%Y-%m-%d %H:%i:%s') AS started_at
                FROM matches m
                JOIN match_players sandra ON sandra.match_id = m.id AND sandra.user_id = ?
                JOIN match_players amy ON amy.match_id = m.id AND amy.user_id = ?
                WHERE m.started_at >= '2026-11-20 16:00:00'
                  AND m.started_at < '2026-11-21 16:00:00'
                  AND IFNULL(m.winner_type, 0) <> 3
                ORDER BY m.id DESC
                """, SANDRA_ID, AMY_ID);
        Map<String, Object> exact = null;
        Map<String, Object> open = null;
        Map<String, Object> loss = null;
        Map<String, Object> win = null;
        for (Map<String, Object> row : rows) {
            int round = ((Number) row.get("current_round")).intValue();
            int hp = ((Number) row.get("boss_current_hp")).intValue();
            int status = ((Number) row.get("status")).intValue();
            int winner = ((Number) row.get("winner_type")).intValue();
            String boss = row.get("boss_name") == null ? "" : String.valueOf(row.get("boss_name"));
            boolean thisFight = boss.contains("恶霸") && round >= 15;
            boolean stillOpen = status == 1 || (status == 2 && winner == 2);
            if (exact == null && thisFight && round == 15 && hp == 3 && stillOpen) {
                exact = row;
            }
            if (open == null && status == 1 && thisFight) {
                open = row;
            }
            if (loss == null && status == 2 && winner == 2 && thisFight) {
                loss = row;
            }
            if (win == null && status == 2 && winner == 1 && thisFight) {
                win = row;
            }
        }
        if (exact != null) {
            return exact;
        }
        if (open != null) {
            return open;
        }
        if (loss != null) {
            return loss;
        }
        if (win != null && !rows.isEmpty()
                && ((Number) rows.get(0).get("id")).longValue() == ((Number) win.get("id")).longValue()) {
            return win;
        }
        return null;
    }

    private void settlePlayer(long userId, long teammateId, long matchId, boolean wasLoss,
                              LocalDate day, String endedAt) {
        Makeup makeup = makeupOf(userId, matchId);
        int gold = 0;
        if (makeup != null) {
            if (workDayService.allowGold(userId, makeup.owedDay())) {
                gold = grantMakeup(userId, makeup, matchId);
            }
        } else {
            int slot = slotOf(userId, matchId, day, endedAt);
            if (slot >= 1 && slot <= 3) {
                if (workDayService.allowGold(userId, day)) {
                    gold = claimPaid(userId, "T-DAILY-MATCH-" + slot, day.toString(), finishGold(slot))
                            + claimPaid(userId, "T-DAILY-WIN-" + slot, day.toString(), WIN_GOLD);
                } else {
                    markReady(userId, "T-DAILY-MATCH-" + slot, day.toString());
                    markReady(userId, "T-DAILY-WIN-" + slot, day.toString());
                }
                if (!wasLoss) {
                    rememberTeammate(userId, teammateId, weekOf(day));
                }
            }
            if (!wasLoss) {
                bumpSlot(userId, day.toString(), Math.max(slot, 1));
            }
        }
        int exp = wasLoss ? VICTORY_EXP - DEFEAT_EXP : VICTORY_EXP;
        int loseDelta = wasLoss ? 1 : 0;
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = IFNULL(win_count, 0) + 1,
                    lose_count = GREATEST(IFNULL(lose_count, 0) - ?, 0),
                    exp = IFNULL(exp, 0) + ?,
                    money = IFNULL(money, 0) + ?,
                    weekly_money = IFNULL(weekly_money, 0) + ?,
                    money_reached_at = CASE WHEN ? > 0 THEN UTC_TIMESTAMP() ELSE money_reached_at END
                WHERE user_id = ?
                """, loseDelta, exp, gold, gold, gold, userId);
        log.warn("Sandra/Amy win user {} gold {} exp {} lossRollback {}", userId, gold, exp, wasLoss);
    }

    private int slotOf(long userId, long matchId, LocalDate day, String endedAt) {
        String start = utcWall(day);
        String end = endedAt == null || endedAt.isBlank() ? utcWall(day.plusDays(1)) : endedAt;
        Integer prior = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM matches m
                JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND m.id <> ?
                  AND m.status = 2
                  AND m.winner_type IN (1, 2)
                  AND mp.makeup_slot IS NULL
                  AND m.ended_at >= ?
                  AND (m.ended_at < ? OR (m.ended_at = ? AND m.id < ?))
                """, Integer.class, userId, matchId, start, end, end, matchId);
        return (prior == null ? 0 : prior) + 1;
    }

    private int claimPaid(long userId, String taskCode, String period, int gold) {
        Long taskId = taskId(taskCode);
        if (taskId == null || gold <= 0) {
            return 0;
        }
        Integer status = taskStatus(userId, taskId, period);
        if (status != null && status >= 3) {
            return 0;
        }
        if (status == null) {
            jdbcTemplate.update("""
                    INSERT INTO user_tasks (
                      user_id, task_id, period_key, progress_value, target_value,
                      status, completed_at, claimed_at, created_at, updated_at
                    )
                    SELECT ?, t.id, ?, 1, IFNULL(t.target_count, 1), 3, UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP()
                    FROM tasks t
                    WHERE t.id = ?
                    """, userId, period, taskId);
        } else {
            int changed = jdbcTemplate.update("""
                    UPDATE user_tasks
                    SET progress_value = GREATEST(IFNULL(progress_value, 0), 1),
                        status = 3,
                        completed_at = IFNULL(completed_at, UTC_TIMESTAMP()),
                        claimed_at = IFNULL(claimed_at, UTC_TIMESTAMP()),
                        updated_at = UTC_TIMESTAMP()
                    WHERE user_id = ? AND task_id = ? AND period_key = ? AND status < 3
                    """, userId, taskId, period);
            if (changed != 1) {
                return 0;
            }
        }
        return gold;
    }

    private void markReady(long userId, String taskCode, String period) {
        Long taskId = taskId(taskCode);
        if (taskId == null) {
            return;
        }
        Integer status = taskStatus(userId, taskId, period);
        if (status != null && status >= 2) {
            return;
        }
        if (status == null) {
            jdbcTemplate.update("""
                    INSERT INTO user_tasks (
                      user_id, task_id, period_key, progress_value, target_value,
                      status, completed_at, claimed_at, created_at, updated_at
                    )
                    SELECT ?, t.id, ?, 1, IFNULL(t.target_count, 1), 2, UTC_TIMESTAMP(), NULL, UTC_TIMESTAMP(), UTC_TIMESTAMP()
                    FROM tasks t
                    WHERE t.id = ?
                    """, userId, period, taskId);
            return;
        }
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET progress_value = GREATEST(IFNULL(progress_value, 0), 1),
                    status = 2,
                    completed_at = IFNULL(completed_at, UTC_TIMESTAMP()),
                    updated_at = UTC_TIMESTAMP()
                WHERE user_id = ? AND task_id = ? AND period_key = ? AND status < 2
                """, userId, taskId, period);
    }

    private void bumpSlot(long userId, String period, int slot) {
        Long taskId = taskId("T-DAILY-SLOT");
        if (taskId == null) {
            return;
        }
        Integer status = taskStatus(userId, taskId, period);
        if (status == null) {
            jdbcTemplate.update("""
                    INSERT INTO user_tasks (
                      user_id, task_id, period_key, progress_value, target_value,
                      status, completed_at, claimed_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, 3, 1, NULL, NULL, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                    """, userId, taskId, period, slot);
            return;
        }
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET progress_value = GREATEST(IFNULL(progress_value, 0), ?),
                    status = CASE WHEN status IS NULL OR status < 1 THEN 1 ELSE status END,
                    updated_at = UTC_TIMESTAMP()
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                """, slot, userId, taskId, period);
    }

    private void rememberTeammate(long userId, long teammateId, String week) {
        Long taskId = taskId("T-WEEKLY-TEAM-10");
        if (taskId == null) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, status, extra_data
                FROM user_tasks
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                LIMIT 1
                """, userId, taskId, week);
        if (rows.isEmpty()) {
            jdbcTemplate.update("""
                    INSERT INTO user_tasks (
                      user_id, task_id, period_key, progress_value, target_value, extra_data,
                      status, completed_at, claimed_at, created_at, updated_at
                    ) VALUES (?, ?, ?, 1, 10, ?, 1, NULL, NULL, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                    """, userId, taskId, week, "[" + teammateId + "]");
            return;
        }
        int status = ((Number) rows.get(0).get("status")).intValue();
        if (status >= 2) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        Object raw = rows.get(0).get("extra_data");
        try {
            JsonNode node = raw instanceof JsonNode json ? json : JSON.readTree(raw == null ? "[]" : String.valueOf(raw));
            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (item.isNumber()) {
                        ids.add(item.longValue());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Skip weekly teammate for user {}.", userId);
            return;
        }
        if (ids.contains(teammateId)) {
            return;
        }
        ids.add(teammateId);
        int nextStatus = ids.size() >= 10 ? 2 : 1;
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET extra_data = CAST(? AS JSON),
                    progress_value = ?,
                    status = ?,
                    completed_at = CASE WHEN ? >= 2 THEN IFNULL(completed_at, UTC_TIMESTAMP()) ELSE completed_at END,
                    updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, JSON.valueToTree(ids).toString(), ids.size(), nextStatus, nextStatus, rows.get(0).get("id"));
    }

    private int grantMakeup(long userId, Makeup makeup, long matchId) {
        int finish = finishGold(makeup.slot());
        int due = finish + WIN_GOLD;
        List<Integer> paidRows = jdbcTemplate.query("""
                SELECT gold_paid
                FROM user_makeup_claims
                WHERE user_id = ? AND owed_day = ? AND slot_no = ?
                """, (rs, row) -> rs.getInt("gold_paid"), userId, makeup.owedDay().toString(), makeup.slot());
        Integer paid = paidRows.isEmpty() ? null : paidRows.get(0);
        int already = paid == null ? 0 : paid;
        if (paid == null) {
            jdbcTemplate.update("""
                    INSERT INTO user_makeup_claims(
                        user_id, owed_day, slot_no, match_id, gold_paid,
                        finish_gold, win_gold, finish_claimed, win_claimed, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, UTC_TIMESTAMP())
                    """, userId, makeup.owedDay().toString(), makeup.slot(), matchId, due, finish, WIN_GOLD);
        } else {
            jdbcTemplate.update("""
                    UPDATE user_makeup_claims
                    SET match_id = ?,
                        gold_paid = ?,
                        finish_gold = ?,
                        win_gold = ?,
                        finish_claimed = 1,
                        win_claimed = 1
                    WHERE user_id = ? AND owed_day = ? AND slot_no = ?
                    """, matchId, Math.max(already, due), finish, WIN_GOLD,
                    userId, makeup.owedDay().toString(), makeup.slot());
        }
        return Math.max(0, due - already);
    }

    private Makeup makeupOf(long userId, long matchId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT makeup_slot, DATE_FORMAT(makeup_owed_day, '%Y-%m-%d') AS owed_day
                FROM match_players
                WHERE match_id = ? AND user_id = ? AND makeup_slot IS NOT NULL
                LIMIT 1
                """, matchId, userId);
        if (rows.isEmpty() || rows.get(0).get("makeup_slot") == null || rows.get(0).get("owed_day") == null) {
            return null;
        }
        int slot = ((Number) rows.get(0).get("makeup_slot")).intValue();
        LocalDate owed = LocalDate.parse(String.valueOf(rows.get(0).get("owed_day")));
        if (slot < 1 || slot > 3) {
            return null;
        }
        return new Makeup(owed, slot);
    }

    private void closeRoom(Object roomId, long matchId) {
        jdbcTemplate.update("""
                UPDATE match_rounds
                SET round_status = 1,
                    phase = 'FINISHED',
                    ended_at = IFNULL(ended_at, UTC_TIMESTAMP()),
                    updated_at = UTC_TIMESTAMP()
                WHERE match_id = ? AND round_status = 0
                """, matchId);
        if (!(roomId instanceof Number room)) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE game_rooms
                SET status = 3,
                    player_count = 0,
                    closed_at = UTC_TIMESTAMP(),
                    updated_at = UTC_TIMESTAMP()
                WHERE id = ? AND status <> 3
                """, room.longValue());
        jdbcTemplate.update("""
                UPDATE room_members
                SET left_at = IFNULL(left_at, UTC_TIMESTAMP()),
                    online_status = 0,
                    updated_at = UTC_TIMESTAMP()
                WHERE room_id = ? AND left_at IS NULL
                """, room.longValue());
    }

    private Long taskId(String taskCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM tasks WHERE task_code = ? LIMIT 1", taskCode);
        if (rows.isEmpty() || rows.get(0).get("id") == null) {
            return null;
        }
        return ((Number) rows.get(0).get("id")).longValue();
    }

    private Integer taskStatus(long userId, long taskId, String period) {
        return jdbcTemplate.query("""
                SELECT status FROM user_tasks
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                LIMIT 1
                """, rs -> rs.next() ? rs.getInt(1) : null, userId, taskId, period);
    }

    private static int finishGold(int slot) {
        return switch (slot) {
            case 1 -> 30;
            case 2 -> 40;
            case 3 -> 50;
            default -> 0;
        };
    }

    private static LocalDate shanghaiDay(String utcWall) {
        if (utcWall == null || utcWall.isBlank()) {
            return LocalDate.now(SHANGHAI);
        }
        LocalDateTime wall = LocalDateTime.parse(utcWall, SQL_TIME);
        return wall.atOffset(ZoneOffset.UTC).atZoneSameInstant(SHANGHAI).toLocalDate();
    }

    private static String utcWall(LocalDate day) {
        return day.atStartOfDay(SHANGHAI).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime().format(SQL_TIME);
    }

    private static String weekOf(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void ensurePatchTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS schema_patches (
                  patch_id varchar(64) NOT NULL,
                  applied_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (patch_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次性数据修复标记'
                """);
    }

    private void markPatchApplied() {
        jdbcTemplate.update("INSERT INTO schema_patches(patch_id) VALUES (?)", PATCH_ID);
    }

    private boolean patchApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_patches WHERE patch_id = ?",
                Integer.class,
                PATCH_ID);
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private record Makeup(LocalDate owedDay, int slot) {
    }
}
