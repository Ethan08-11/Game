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
 * 还原 Harry / Chrissy 2026-10-08 的胜利局（match 1943）。
 * 该局打到第 16 回合，老板血量为 0。先前被误作废，这里改回胜利。
 * 补回双方胜利场次、100 经验和当时解锁的卡牌，并按剩余对局重算今日任务。
 * Harry 的本周组队加回 Chrissy，重新凑满 10 人；金币仍未领取，不改余额。
 */
@Component
@Order(40)
public class HarryChrissyMatchRestoreBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HarryChrissyMatchRestoreBootstrap.class);
    static final String PATCH_ID = "restore_harry_chrissy_match_1943_20261008";
    private static final long MATCH_ID = 1943L;
    private static final long HARRY_ID = 34L;
    private static final long CHRISSY_ID = 13L;
    private static final String PERIOD = "2026-10-08";
    private static final String WEEK = "2026-10-05";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public HarryChrissyMatchRestoreBootstrap(JdbcTemplate jdbcTemplate,
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
            log.error("Harry/Chrissy match restore failed.", e);
        }
    }

    private boolean reset() {
        int updated = jdbcTemplate.update("""
                UPDATE matches
                SET winner_type = 1,
                    phase = 'FINISHED',
                    status = 2
                WHERE id = ? AND status = 2 AND winner_type = 3
                """, MATCH_ID);
        if (updated != 1) {
            log.warn("Skip Harry/Chrissy restore; match {} is not a void.", MATCH_ID);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE match_players
                SET result_type = 1,
                    player_status = 'ACTIVE'
                WHERE match_id = ? AND result_type = 3
                """, MATCH_ID);
        jdbcTemplate.update("""
                UPDATE user_profiles p
                INNER JOIN match_players mp ON mp.user_id = p.user_id
                SET p.win_count = IFNULL(p.win_count, 0) + 1,
                    p.exp = IFNULL(p.exp, 0) + 100
                WHERE mp.match_id = ?
                """, MATCH_ID);
        jdbcTemplate.update("""
                INSERT INTO user_card_pools(user_id, card_id, owned_count, unlocked_status, level, created_at)
                SELECT a.actor_user_id, a.card_id, 1, 1, 1, a.created_at
                FROM match_actions a
                WHERE a.match_id = ?
                  AND a.action_type = 'unlock_card'
                  AND a.actor_user_id IS NOT NULL
                  AND a.card_id IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM user_card_pools pool
                      WHERE pool.user_id = a.actor_user_id AND pool.card_id = a.card_id
                  )
                """, MATCH_ID);
        List<Long> playerIds = jdbcTemplate.query(
                "SELECT user_id FROM match_players WHERE match_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                MATCH_ID);
        String start = IrisAncyAllMatchResetBootstrap.utcWallStart(LocalDate.parse(PERIOD));
        String end = IrisAncyAllMatchResetBootstrap.utcWallStart(LocalDate.parse(PERIOD).plusDays(1));
        for (Long playerId : playerIds) {
            rebuildDailyBoard(playerId, start, end);
        }
        addWeeklyTeammate(HARRY_ID, CHRISSY_ID);
        addWeeklyTeammate(CHRISSY_ID, HARRY_ID);
        log.warn("Restored win match {} and rebuilt boards for {}.", MATCH_ID, playerIds);
        return true;
    }

    private void addWeeklyTeammate(long userId, long teammateId) {
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
            log.warn("Skip weekly teammate restore for user {}.", userId);
            return;
        }
        if (ids.contains(teammateId)) {
            return;
        }
        ids.add(teammateId);
        long taskRowId = ((Number) rows.get(0).get("id")).longValue();
        int progress = ids.size();
        int nextStatus = progress >= 10 ? 2 : (progress > 0 ? 1 : 0);
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET extra_data = CAST(? AS JSON),
                    progress_value = ?,
                    status = ?,
                    completed_at = CASE WHEN ? >= 10 THEN IFNULL(completed_at, NOW()) ELSE completed_at END,
                    updated_at = NOW()
                WHERE id = ? AND status < 3
                """, JSON.valueToTree(ids).toString(), progress, nextStatus, progress, taskRowId);
    }

    private void rebuildDailyBoard(long userId, String start, String end) {
        List<Integer> remainingWinners = jdbcTemplate.query("""
                SELECT m.winner_type
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND m.status = 2
                  AND IFNULL(m.winner_type, 0) IN (1, 2)
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
