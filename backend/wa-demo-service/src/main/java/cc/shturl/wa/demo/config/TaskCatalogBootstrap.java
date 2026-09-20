package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 每日完成/获胜任务与每周不同队友任务目录。
 */
@Component
@Order(3)
public class TaskCatalogBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskCatalogBootstrap.class);
    static final List<String> ACTIVE_CODES = List.of(
            "T-DAILY-SLOT",
            "T-DAILY-MATCH-1",
            "T-DAILY-WIN-1",
            "T-DAILY-MATCH-2",
            "T-DAILY-WIN-2",
            "T-DAILY-MATCH-3",
            "T-DAILY-WIN-3",
            "T-WEEKLY-TEAM-10"
    );

    private final JdbcTemplate jdbcTemplate;

    public TaskCatalogBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("user_profiles") || !tableExists("tasks")) {
            log.info("Skip task catalog bootstrap: required tables missing.");
            return;
        }
        ensureLoginStreakColumns();
        ensureUserTaskExtraData();
        ensureWorkDayTable();
        backfillWorkDays();
        upsertCatalog();
        disableInactiveTasks();
        log.info("Task catalog ready.");
    }

    private void ensureWorkDayTable() {
        if (tableExists("user_month_work_days")) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE `user_month_work_days` (
                  `user_id` bigint NOT NULL COMMENT '用户ID',
                  `day_date` date NOT NULL COMMENT '已领金币的自然日 Asia/Shanghai',
                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`user_id`, `day_date`),
                  KEY `idx_work_day_month` (`user_id`, `day_date`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每月工作日：领过任务金币的自然日'
                """);
        log.info("Created user_month_work_days table.");
    }

    private void backfillWorkDays() {
        if (!tableExists("user_month_work_days") || !tableExists("user_tasks") || !tableExists("tasks")) {
            return;
        }
        int inserted = jdbcTemplate.update("""
                INSERT IGNORE INTO user_month_work_days (user_id, day_date)
                SELECT DISTINCT ut.user_id, CAST(ut.period_key AS DATE)
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.status = 3
                  AND LOWER(t.task_type) = 'daily'
                  AND LOWER(IFNULL(t.reward_type, '')) = 'money'
                  AND ut.period_key REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
                """);
        if (inserted > 0) {
            log.info("Backfilled {} monthly work-day rows from claimed daily tasks.", inserted);
        }
    }

    private void ensureLoginStreakColumns() {
        if (!columnExists("user_profiles", "login_streak")) {
            jdbcTemplate.execute("""
                    ALTER TABLE `user_profiles`
                    ADD COLUMN `login_streak` int NOT NULL DEFAULT 0
                    COMMENT '连续登录天数，断一天清零'
                    AFTER `weekly_money`
                    """);
            log.info("Added user_profiles.login_streak.");
        }
        if (!columnExists("user_profiles", "last_task_login_date")) {
            jdbcTemplate.execute("""
                    ALTER TABLE `user_profiles`
                    ADD COLUMN `last_task_login_date` date NULL
                    COMMENT '上次计入任务的登录自然日(Asia/Shanghai)'
                    AFTER `login_streak`
                    """);
            log.info("Added user_profiles.last_task_login_date.");
        }
    }

    private void ensureUserTaskExtraData() {
        if (!tableExists("user_tasks") || columnExists("user_tasks", "extra_data")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `user_tasks`
                ADD COLUMN `extra_data` json NULL
                COMMENT '任务附加进度，如本周已组队队友ID'
                AFTER `target_value`
                """);
        log.info("Added user_tasks.extra_data.");
    }

    private void upsertCatalog() {
        upsert("T-DAILY-SLOT", "当日局数槽", "system", "DAILY", "DAY", "MATCH_SLOT",
                "内部计数：正常结束、放弃、掉线超时都占用一局；卡死作废不占用",
                "match_slot", "{}", "none", "{}", 99, 0, 1);
        upsert("T-DAILY-MATCH-1", "完成第 1 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "今天打完第 1 局即可，输赢都算。中途放弃占用当日局数，但不算完成、不发这枚金币",
                "match_count", "{}", "money", "{\"amount\":30}", 1, 10, 1);
        upsert("T-DAILY-WIN-1", "赢第 1 局", "daily", "DAILY", "DAY", "MATCH_SLOT_WIN",
                "今天第 1 局最终获胜才算；看广告复活后赢了也算。放弃或掉线超时会占用这局，无法重打",
                "match_slot_win", "{\"slot\":1}", "money", "{\"amount\":10}", 1, 11, 1);
        upsert("T-DAILY-MATCH-2", "完成第 2 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "今天打完第 2 局即可，输赢都算。中途放弃占用当日局数，但不算完成、不发这枚金币",
                "match_count", "{}", "money", "{\"amount\":40}", 2, 20, 1);
        upsert("T-DAILY-WIN-2", "赢第 2 局", "daily", "DAILY", "DAY", "MATCH_SLOT_WIN",
                "今天第 2 局最终获胜才算；看广告复活后赢了也算。放弃或掉线超时会占用这局，无法重打",
                "match_slot_win", "{\"slot\":2}", "money", "{\"amount\":10}", 1, 21, 1);
        upsert("T-DAILY-MATCH-3", "完成第 3 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "今天打完第 3 局即可，输赢都算。中途放弃占用当日局数，但不算完成、不发这枚金币",
                "match_count", "{}", "money", "{\"amount\":50}", 3, 30, 1);
        upsert("T-DAILY-WIN-3", "赢第 3 局", "daily", "DAILY", "DAY", "MATCH_SLOT_WIN",
                "今天第 3 局最终获胜才算；看广告复活后赢了也算。放弃或掉线超时会占用这局，无法重打",
                "match_slot_win", "{\"slot\":3}", "money", "{\"amount\":10}", 1, 31, 1);
        upsert("T-WEEKLY-TEAM-10", "跟 10 位不同同事组合", "weekly", "WEEKLY", "WEEK", "DISTINCT_TEAMMATE_COUNT",
                "本周在每日前 3 局里，和 10 个不同的人组过队。放弃会占掉其中一局",
                "distinct_teammate", "{}", "money", "{\"amount\":500}", 10, 90, 1);
    }

    private void upsert(String code, String name, String type, String reset, String scope, String progress,
                        String description, String conditionType, String conditionValue,
                        String rewardType, String rewardValue, int target, int sort, int status) {
        jdbcTemplate.update("""
                INSERT INTO tasks (
                  task_code, task_name, task_type, reset_type, period_scope, progress_type,
                  description, condition_type, condition_value, reward_type, reward_value,
                  target_count, sort_no, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, CAST(? AS JSON), ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  task_name = VALUES(task_name),
                  task_type = VALUES(task_type),
                  reset_type = VALUES(reset_type),
                  period_scope = VALUES(period_scope),
                  progress_type = VALUES(progress_type),
                  description = VALUES(description),
                  condition_type = VALUES(condition_type),
                  condition_value = VALUES(condition_value),
                  reward_type = VALUES(reward_type),
                  reward_value = VALUES(reward_value),
                  target_count = VALUES(target_count),
                  sort_no = VALUES(sort_no),
                  status = VALUES(status)
                """,
                code, name, type, reset, scope, progress,
                description, conditionType, conditionValue, rewardType, rewardValue,
                target, sort, status);
    }

    private void disableInactiveTasks() {
        String placeholders = String.join(",", ACTIVE_CODES.stream().map(code -> "?").toList());
        jdbcTemplate.update(
                "UPDATE tasks SET status = 0 WHERE task_code NOT IN (" + placeholders + ")",
                ACTIVE_CODES.toArray());
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
}
