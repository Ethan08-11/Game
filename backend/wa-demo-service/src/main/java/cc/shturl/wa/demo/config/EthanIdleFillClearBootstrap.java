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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 取消 Ethan 自动补打后，清掉 AE 补胜对局。
 * 11 月的胜场、经验和已领金币按剩余真实对局收回；10 月金币已随月初清零，只收回经验。
 * 补打打开的 11 月工作日，在当天不再有已领金币任务时删掉。
 */
@Component
@Order(52)
public class EthanIdleFillClearBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EthanIdleFillClearBootstrap.class);
    static final String PATCH_ID = "clear_ethan_idle_fill_20261107";
    private static final long ETHAN_ID = 33L;
    private static final int WIN_EXP = 100;
    private static final int WEEKLY_TARGET = 10;
    private static final DateTimeFormatter CODE_DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public EthanIdleFillClearBootstrap(JdbcTemplate jdbcTemplate,
                                        PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("matches") || !tableExists("user_profiles") || !tableExists("user_tasks")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (clear()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Ethan idle fill clear failed.", e);
        }
    }

    private boolean clear() {
        List<String> codes = jdbcTemplate.query("""
                SELECT m.match_code
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id AND mp.user_id = ?
                WHERE m.match_code REGEXP '^AE2026[0-9]{4}[1-3]$'
                  AND m.winner_type = 1
                ORDER BY m.match_code
                """, (rs, row) -> rs.getString(1), ETHAN_ID);
        if (codes.isEmpty()) {
            log.warn("Ethan idle fill clear found no auto wins.");
            return true;
        }
        Set<LocalDate> novemberDays = new TreeSet<>();
        Set<LocalDate> weeks = new TreeSet<>();
        int novemberWins = 0;
        for (String code : codes) {
            LocalDate day = LocalDate.parse(code.substring(2, 10), CODE_DAY);
            if (day.getYear() == 2026 && day.getMonthValue() == 11) {
                novemberDays.add(day);
                novemberWins++;
                weeks.add(day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            }
        }
        jdbcTemplate.update("""
                UPDATE match_players mp
                INNER JOIN matches m ON m.id = mp.match_id
                SET mp.result_type = 3
                WHERE mp.user_id = ?
                  AND m.match_code REGEXP '^AE2026[0-9]{4}[1-3]$'
                  AND m.winner_type = 1
                """, ETHAN_ID);
        int voided = jdbcTemplate.update("""
                UPDATE matches m
                INNER JOIN match_players mp ON mp.match_id = m.id AND mp.user_id = ?
                SET m.winner_type = 3,
                    m.phase = 'FINISHED',
                    m.status = 2
                WHERE m.match_code REGEXP '^AE2026[0-9]{4}[1-3]$'
                  AND m.winner_type = 1
                """, ETHAN_ID);
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = GREATEST(0, IFNULL(win_count, 0) - ?),
                    exp = GREATEST(0, IFNULL(exp, 0) - ?)
                WHERE user_id = ?
                """, novemberWins, voided * WIN_EXP, ETHAN_ID);
        long clawed = 0L;
        for (LocalDate day : novemberDays) {
            clawed += rebuildDailyBoard(day);
            dropWorkDayIfUnpaid(day);
        }
        for (LocalDate week : weeks) {
            clawed += rebuildWeek(week);
        }
        log.warn("Cleared Ethan idle fills voided={} novemberWins={} clawedGold={} days={}",
                voided, novemberWins, clawed, novemberDays);
        return voided > 0;
    }

    private long rebuildDailyBoard(LocalDate day) {
        String start = IrisAncyAllMatchResetBootstrap.utcWallStart(day);
        String end = IrisAncyAllMatchResetBootstrap.utcWallStart(day.plusDays(1));
        List<Integer> remainingWinners = jdbcTemplate.query("""
                SELECT m.winner_type
                FROM matches m
                INNER JOIN match_players mp ON mp.match_id = m.id
                WHERE mp.user_id = ?
                  AND m.status = 2
                  AND IFNULL(m.winner_type, 0) IN (1, 2)
                  AND IFNULL(m.match_code, '') NOT REGEXP '^AE2026[0-9]{4}[1-3]$'
                  AND (SELECT COUNT(*) FROM match_players seat WHERE seat.match_id = m.id) >= 2
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                  AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                ORDER BY COALESCE(m.started_at, m.created_at, m.id), m.id
                """, (rs, rowNum) -> rs.getInt(1), ETHAN_ID, start, end);
        int remaining = remainingWinners.size();
        String period = day.toString();
        long clawed = 0L;
        clawed += syncTask(period, "T-DAILY-SLOT", remaining, Math.max(remaining, 1), false);
        for (int slot = 1; slot <= 3; slot++) {
            clawed += syncTask(period, "T-DAILY-MATCH-" + slot, remaining, slot, remaining >= slot);
            boolean wonSlot = slot <= remaining && remainingWinners.get(slot - 1) == 1;
            clawed += syncTask(period, "T-DAILY-WIN-" + slot, wonSlot ? 1 : 0, 1, wonSlot);
        }
        return clawed;
    }

    private long rebuildWeek(LocalDate weekStart) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT ut.id, ut.status, ut.extra_data
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.user_id = ?
                  AND t.task_code = 'T-WEEKLY-TEAM-10'
                  AND ut.period_key = ?
                LIMIT 1
                """, ETHAN_ID, weekStart.toString());
        if (rows.isEmpty()) {
            return 0L;
        }
        int status = ((Number) rows.get(0).get("status")).intValue();
        Set<Long> ids = realTeammates(weekStart);
        int progress = Math.min(ids.size(), WEEKLY_TARGET);
        boolean stillClaimed = status >= 3 && progress >= WEEKLY_TARGET;
        int nextStatus = stillClaimed ? 3 : (progress >= WEEKLY_TARGET ? 2 : (progress > 0 ? 1 : 0));
        long clawed = 0L;
        if (status >= 3 && !stillClaimed) {
            long reward = rewardAmount(queryReward("T-WEEKLY-TEAM-10"));
            if (reward > 0) {
                jdbcTemplate.update("""
                        UPDATE user_profiles
                        SET money = GREATEST(0, IFNULL(money, 0) - ?),
                            weekly_money = GREATEST(0, IFNULL(weekly_money, 0) - ?)
                        WHERE user_id = ?
                        """, reward, reward, ETHAN_ID);
                clawed = reward;
            }
        }
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET extra_data = CAST(? AS JSON),
                    progress_value = ?,
                    status = ?,
                    completed_at = CASE WHEN ? >= 2 THEN completed_at ELSE NULL END,
                    claimed_at = CASE WHEN ? >= 3 THEN claimed_at ELSE NULL END,
                    updated_at = NOW()
                WHERE id = ?
                """, writeIds(ids), progress, nextStatus, nextStatus, nextStatus,
                ((Number) rows.get(0).get("id")).longValue());
        return clawed;
    }

    private Set<Long> realTeammates(LocalDate weekStart) {
        Set<Long> ids = new LinkedHashSet<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            String start = IrisAncyAllMatchResetBootstrap.utcWallStart(day);
            String end = IrisAncyAllMatchResetBootstrap.utcWallStart(day.plusDays(1));
            List<Long> found = jdbcTemplate.query("""
                    SELECT first3.id AS match_id, other.user_id AS user_id
                    FROM (
                      SELECT m.id
                      FROM matches m
                      INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = ?
                      WHERE m.status = 2
                        AND IFNULL(m.winner_type, 0) IN (1, 2)
                        AND IFNULL(m.match_code, '') NOT REGEXP '^AE2026[0-9]{4}[1-3]$'
                        AND COALESCE(m.ended_at, m.started_at, m.created_at) >= ?
                        AND COALESCE(m.ended_at, m.started_at, m.created_at) < ?
                      ORDER BY COALESCE(m.started_at, m.created_at, m.id), m.id
                      LIMIT 3
                    ) first3
                    LEFT JOIN match_players other
                      ON other.match_id = first3.id AND other.user_id <> ?
                    """, (rs, rowNum) -> {
                        long teammateId = rs.getLong("user_id");
                        if (rs.wasNull()) {
                            return -rs.getLong("match_id");
                        }
                        return teammateId;
                    }, ETHAN_ID, start, end, ETHAN_ID);
            ids.addAll(found);
        }
        return ids;
    }

    private void dropWorkDayIfUnpaid(LocalDate day) {
        jdbcTemplate.update("""
                DELETE FROM user_month_work_days
                WHERE user_id = ? AND day_date = ?
                  AND NOT EXISTS (
                    SELECT 1
                    FROM user_tasks ut
                    INNER JOIN tasks t ON t.id = ut.task_id
                    WHERE ut.user_id = ?
                      AND ut.period_key = ?
                      AND ut.status >= 3
                      AND t.reward_type = 'money'
                      AND t.task_code LIKE 'T-DAILY-%'
                  )
                """, ETHAN_ID, java.sql.Date.valueOf(day), ETHAN_ID, day.toString());
    }

    private long syncTask(String period, String taskCode, int progress, int target, boolean complete) {
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
                """, rs -> rs.next() ? rs.getInt(1) : null, ETHAN_ID, taskId, period);
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
                        """, reward, reward, ETHAN_ID);
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
                """, progress, target, nextStatus, complete, nextStatus >= 3, ETHAN_ID, taskId, period);
        return clawed;
    }

    private Object queryReward(String taskCode) {
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                "SELECT reward_value FROM tasks WHERE task_code = ? LIMIT 1", taskCode);
        return tasks.isEmpty() ? null : tasks.get(0).get("reward_value");
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

    private String writeIds(Set<Long> ids) {
        try {
            return JSON.writeValueAsString(new ArrayList<>(ids));
        } catch (Exception e) {
            return "[]";
        }
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
