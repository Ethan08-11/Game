package cc.shturl.wa.demo.config;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作废 Felicity / Sean 2026-11-25 最近一把（match 2916）。
 * 还在打时直接作废，不记胜负、不占任务、不占用每天一次的作废次数。
 * 若启动前已经分出胜负，则回退对应胜负、经验和当日任务，并撤掉本周尚未领取的这对队友。
 */
@Component
@Order(75)
public class FelicitySeanMatch2916ResetBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FelicitySeanMatch2916ResetBootstrap.class);
    static final String PATCH_ID = "reset_felicity_sean_match_2916_20261125";
    private static final long MATCH_ID = 2916L;
    private static final long FELICITY_ID = 29L;
    private static final long SEAN_ID = 46L;
    private static final int DEFEAT_EXP = 30;
    private static final int VICTORY_EXP = 100;
    private static final String PERIOD = "2026-11-25";
    private static final String WEEK = "2026-11-23";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public FelicitySeanMatch2916ResetBootstrap(JdbcTemplate jdbcTemplate,
                                             PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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
                if (reset()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Felicity/Sean match reset failed.", e);
        }
    }

    private boolean reset() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, winner_type, room_id FROM matches WHERE id = ?", MATCH_ID);
        if (rows.isEmpty()) {
            log.warn("Skip Felicity/Sean reset; match {} is missing.", MATCH_ID);
            return false;
        }
        int status = ((Number) rows.get(0).get("status")).intValue();
        int winner = rows.get(0).get("winner_type") == null ? 0 : ((Number) rows.get(0).get("winner_type")).intValue();
        if (status == 2 && winner == 3) {
            int removed = jdbcTemplate.update("""
                    DELETE FROM match_actions
                    WHERE match_id = ? AND action_type = 'stuck_void'
                    """, MATCH_ID);
            log.warn("Match {} is already void. Removed {} stuck_void marks.", MATCH_ID, removed);
            return true;
        }
        if (status == 1) {
            voidOpen(rows.get(0).get("room_id"));
            log.warn("Voided open match {}.", MATCH_ID);
            return true;
        }
        if (winner != 1 && winner != 2) {
            log.warn("Skip Felicity/Sean reset; match {} status {} winner {}.", MATCH_ID, status, winner);
            return false;
        }
        return voidSettled(winner);
    }

    private void voidOpen(Object roomId) {
        jdbcTemplate.update("""
                UPDATE matches
                SET winner_type = 3,
                    phase = 'FINISHED',
                    status = 2,
                    ended_at = UTC_TIMESTAMP(),
                    duration_seconds = GREATEST(0, TIMESTAMPDIFF(SECOND, started_at, UTC_TIMESTAMP()))
                WHERE id = ? AND status = 1
                """, MATCH_ID);
        jdbcTemplate.update("""
                UPDATE match_rounds
                SET round_status = 1,
                    phase = 'FINISHED',
                    ended_at = UTC_TIMESTAMP()
                WHERE match_id = ? AND round_status = 0
                """, MATCH_ID);
        jdbcTemplate.update("""
                UPDATE match_players
                SET result_type = 3,
                    player_status = 'LEFT'
                WHERE match_id = ?
                """, MATCH_ID);
        if (roomId == null) {
            return;
        }
        long id = ((Number) roomId).longValue();
        jdbcTemplate.update("""
                UPDATE game_rooms
                SET status = 3,
                    player_count = 0,
                    closed_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, id);
        jdbcTemplate.update("""
                UPDATE room_members
                SET left_at = UTC_TIMESTAMP(),
                    online_status = 0,
                    ready_status = 0
                WHERE room_id = ? AND left_at IS NULL
                """, id);
    }

    private boolean voidSettled(int winner) {
        int updated = jdbcTemplate.update("""
                UPDATE matches
                SET winner_type = 3,
                    phase = 'FINISHED',
                    status = 2
                WHERE id = ? AND status = 2 AND winner_type = ?
                """, MATCH_ID, winner);
        if (updated != 1) {
            log.warn("Skip Felicity/Sean reset; match {} changed before settle.", MATCH_ID);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE match_players
                SET result_type = 3,
                    player_status = 'LEFT'
                WHERE match_id = ? AND result_type = ?
                """, MATCH_ID, winner);
        int exp = winner == 1 ? VICTORY_EXP : DEFEAT_EXP;
        String countColumn = winner == 1 ? "win_count" : "lose_count";
        jdbcTemplate.update("""
                UPDATE user_profiles p
                INNER JOIN match_players mp ON mp.user_id = p.user_id
                SET p.%s = GREATEST(0, IFNULL(p.%s, 0) - 1),
                    p.exp = GREATEST(0, IFNULL(p.exp, 0) - ?)
                WHERE mp.match_id = ?
                """.formatted(countColumn, countColumn), exp, MATCH_ID);
        if (winner == 1) {
            jdbcTemplate.update("""
                    DELETE pool FROM user_card_pools pool
                    INNER JOIN match_actions a
                        ON a.match_id = ?
                       AND a.action_type = 'unlock_card'
                       AND a.actor_user_id = pool.user_id
                       AND a.card_id = pool.card_id
                    WHERE pool.owned_count <= 1
                    """, MATCH_ID);
        }
        List<Long> playerIds = jdbcTemplate.query(
                "SELECT user_id FROM match_players WHERE match_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                MATCH_ID);
        String start = IrisAncyAllMatchResetBootstrap.utcWallStart(LocalDate.parse(PERIOD));
        String end = IrisAncyAllMatchResetBootstrap.utcWallStart(LocalDate.parse(PERIOD).plusDays(1));
        for (Long playerId : playerIds) {
            rebuildDailyBoard(playerId, start, end);
        }
        dropWeeklyTeammate(SEAN_ID, FELICITY_ID);
        dropWeeklyTeammate(FELICITY_ID, SEAN_ID);
        log.warn("Voided settled match {} winner {} and rebuilt boards for {}.", MATCH_ID, winner, playerIds);
        return true;
    }

    private void rebuildDailyBoard(long userId, String start, String end) {
        List<Integer> remainingWinners = jdbcTemplate.query("""
                SELECT m.winner_type
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND m.status = 2
                  AND IFNULL(m.winner_type, 0) IN (1, 2)
                  AND mp.makeup_slot IS NULL
                  AND (SELECT COUNT(*) FROM match_players seat WHERE seat.match_id = m.id) >= 2
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                ORDER BY COALESCE(m.started_at, m.created_at, m.id), m.id
                """, (rs, rowNum) -> rs.getInt(1),
                userId, start, end);
        int remaining = remainingWinners.size();
        long clawed = 0L;
        clawed += syncTask(userId, "T-DAILY-SLOT", remaining, Math.max(remaining, 1), false);
        for (int slot = 1; slot <= 3; slot++) {
            clawed += syncTask(userId, "T-DAILY-MATCH-" + slot, remaining, slot, remaining >= slot);
            boolean wonSlot = slot <= remaining && remainingWinners.get(slot - 1) == 1;
            clawed += syncTask(userId, "T-DAILY-WIN-" + slot, wonSlot ? 1 : 0, 1, wonSlot);
        }
        log.warn("Rebuilt daily board userId={} remaining={} clawedGold={}", userId, remaining, clawed);
    }

    private void dropWeeklyTeammate(long userId, long teammateId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT ut.id, ut.status, ut.extra_data
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.user_id = ?
                  AND t.task_code = 'T-WEEKLY-TEAM-10'
                  AND ut.period_key = ?
                LIMIT 1
                """, userId, WEEK);
        if (rows.isEmpty()) {
            return;
        }
        int status = ((Number) rows.get(0).get("status")).intValue();
        if (status >= 3) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        boolean removed = false;
        Object raw = rows.get(0).get("extra_data");
        try {
            JsonNode node = raw instanceof JsonNode json ? json : JSON.readTree(raw == null ? "[]" : String.valueOf(raw));
            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (!item.isNumber()) {
                        continue;
                    }
                    if (item.longValue() == teammateId) {
                        removed = true;
                        continue;
                    }
                    ids.add(item.longValue());
                }
            }
        } catch (Exception e) {
            log.warn("Skip weekly teammate rollback for user {}.", userId);
            return;
        }
        if (!removed) {
            return;
        }
        long taskRowId = ((Number) rows.get(0).get("id")).longValue();
        int progress = ids.size();
        int nextStatus = progress >= 10 ? 2 : (progress > 0 ? 1 : 0);
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET extra_data = CAST(? AS JSON),
                    progress_value = ?,
                    status = ?,
                    completed_at = CASE WHEN ? >= 10 THEN completed_at ELSE NULL END,
                    updated_at = NOW()
                WHERE id = ? AND status < 3
                """, JSON.valueToTree(ids).toString(), progress, nextStatus, progress, taskRowId);
    }

    private long syncTask(long userId, String taskCode, int progress, int target, boolean complete) {
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList("""
                SELECT id, reward_value FROM tasks WHERE task_code = ? LIMIT 1
                """, taskCode);
        if (tasks.isEmpty()) {
            return 0L;
        }
        long taskId = ((Number) tasks.get(0).get("id")).longValue();
        long reward = "T-DAILY-SLOT".equals(taskCode) ? 0L : rewardAmount(tasks.get(0).get("reward_value"));
        Integer status = jdbcTemplate.query("""
                SELECT status FROM user_tasks
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                LIMIT 1
                """, rs -> rs.next() ? rs.getInt(1) : null, userId, taskId, PERIOD);
        if (status == null) {
            return 0L;
        }
        long clawed = 0L;
        int nextStatus;
        if (complete && status >= 3) {
            nextStatus = 3;
        } else if (complete) {
            nextStatus = 2;
        } else {
            nextStatus = progress > 0 ? 1 : 0;
            if (status >= 3 && reward > 0) {
                jdbcTemplate.update("""
                        UPDATE user_profiles
                        SET money = GREATEST(0, IFNULL(money, 0) - ?),
                            weekly_money = GREATEST(0, IFNULL(weekly_money, 0) - ?)
                        WHERE user_id = ?
                        """, reward, reward, userId);
                clawed = reward;
            }
        }
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET progress_value = ?,
                    target_value = GREATEST(IFNULL(target_value, 0), ?),
                    status = ?,
                    completed_at = CASE WHEN ? THEN IFNULL(completed_at, NOW()) ELSE NULL END,
                    claimed_at = CASE WHEN ? THEN claimed_at ELSE NULL END,
                    updated_at = NOW()
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                """, progress, target, nextStatus, complete, nextStatus >= 3, userId, taskId, PERIOD);
        return clawed;
    }

    private long rewardAmount(Object raw) {
        if (raw == null) {
            return 0L;
        }
        try {
            JsonNode node = raw instanceof JsonNode json ? json : JSON.readTree(String.valueOf(raw));
            if (node.has("amount")) {
                return node.get("amount").asLong(0L);
            }
        } catch (Exception ignored) {
            return 0L;
        }
        return 0L;
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
}
