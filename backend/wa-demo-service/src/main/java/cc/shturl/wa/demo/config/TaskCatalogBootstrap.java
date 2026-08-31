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
        upsertCatalog();
        disableInactiveTasks();
        log.info("Task catalog ready.");
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
        upsert("T-DAILY-MATCH-1", "完成第 1 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "今天打完第 1 局即可，输赢都算", "match_count", "{}", "money", "{\"amount\":30}", 1, 10, 1);
        upsert("T-DAILY-WIN-1", "赢第 1 局", "daily", "DAILY", "DAY", "MATCH_SLOT_WIN",
                "今天第 1 局获胜，看广告复活也算", "match_slot_win", "{\"slot\":1}", "money", "{\"amount\":10}", 1, 11, 1);
        upsert("T-DAILY-MATCH-2", "完成第 2 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "今天打完第 2 局即可，输赢都算", "match_count", "{}", "money", "{\"amount\":40}", 2, 20, 1);
        upsert("T-DAILY-WIN-2", "赢第 2 局", "daily", "DAILY", "DAY", "MATCH_SLOT_WIN",
                "今天第 2 局获胜，看广告复活也算", "match_slot_win", "{\"slot\":2}", "money", "{\"amount\":10}", 1, 21, 1);
        upsert("T-DAILY-MATCH-3", "完成第 3 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "今天打完第 3 局即可，输赢都算", "match_count", "{}", "money", "{\"amount\":50}", 3, 30, 1);
        upsert("T-DAILY-WIN-3", "赢第 3 局", "daily", "DAILY", "DAY", "MATCH_SLOT_WIN",
                "今天第 3 局获胜，看广告复活也算", "match_slot_win", "{\"slot\":3}", "money", "{\"amount\":10}", 1, 31, 1);
        upsert("T-WEEKLY-TEAM-10", "跟 10 位不同同事组合", "weekly", "WEEKLY", "WEEK", "DISTINCT_TEAMMATE_COUNT",
                "本周在每日前 3 局里，和 10 个不同的人组过队", "distinct_teammate", "{}", "money", "{\"amount\":500}", 10, 90, 1);
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
