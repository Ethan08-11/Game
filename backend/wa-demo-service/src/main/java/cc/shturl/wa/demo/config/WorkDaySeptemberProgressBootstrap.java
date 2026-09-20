package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.WorkDayQuota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 2026 年 9 月工作日从 12 日起算。按每人 12 日及之后第一次领金币的日期，
 * 把本月进度对齐到「从当天到部署当日」的已过天数（不超过 16）。
 */
@Component
@Order(10)
public class WorkDaySeptemberProgressBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkDaySeptemberProgressBootstrap.class);
    private static final String PATCH_ID = "reset_sep_workdays_from_0912_first_claim_20260920";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public WorkDaySeptemberProgressBootstrap(JdbcTemplate jdbcTemplate,
                                             PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("user_month_work_days") || !tableExists("user_tasks") || !tableExists("tasks")) {
            return;
        }
        ensurePatchTable();
        if (patchApplied()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            realignSeptemberProgress();
            markPatchApplied();
        });
    }

    private void realignSeptemberProgress() {
        LocalDate monthStart = WorkDayQuota.LAUNCH_MONTH.atDay(1);
        LocalDate countStart = WorkDayQuota.LAUNCH_COUNT_START;
        LocalDate today = LocalDate.now(WorkDayQuota.ZONE);
        LocalDate monthEnd = WorkDayQuota.LAUNCH_MONTH.plusMonths(1).atDay(1);
        int quota = WorkDayQuota.days(YearMonth.from(countStart));
        jdbcTemplate.update(
                "DELETE FROM user_month_work_days WHERE day_date >= ? AND day_date < ?",
                Date.valueOf(monthStart), Date.valueOf(monthEnd));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT ut.user_id AS user_id,
                       DATE_FORMAT(MIN(DATE_ADD(ut.claimed_at, INTERVAL 8 HOUR)), '%Y-%m-%d') AS first_day
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.status = 3
                  AND ut.claimed_at IS NOT NULL
                  AND LOWER(IFNULL(t.reward_type, '')) = 'money'
                  AND DATE_ADD(ut.claimed_at, INTERVAL 8 HOUR) >= ?
                GROUP BY ut.user_id
                """, countStart.atStartOfDay());
        int users = 0;
        int days = 0;
        for (Map<String, Object> row : rows) {
            long userId = ((Number) row.get("user_id")).longValue();
            Object raw = row.get("first_day");
            if (raw == null) {
                continue;
            }
            LocalDate first = LocalDate.parse(raw.toString().substring(0, 10));
            if (first.isBefore(countStart)) {
                first = countStart;
            }
            int added = 0;
            for (LocalDate day = first; !day.isAfter(today) && added < quota; day = day.plusDays(1)) {
                days += jdbcTemplate.update(
                        "INSERT IGNORE INTO user_month_work_days(user_id, day_date) VALUES (?, ?)",
                        userId, Date.valueOf(day));
                added++;
            }
            users++;
        }
        log.warn("Realigned September work-day progress from {} for {} users, {} day rows, today={}.",
                countStart, users, days, today);
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

    private boolean patchApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_patches WHERE patch_id = ?",
                Integer.class,
                PATCH_ID);
        return count != null && count > 0;
    }

    private void markPatchApplied() {
        jdbcTemplate.update("INSERT INTO schema_patches(patch_id) VALUES (?)", PATCH_ID);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }
}
