package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 每日/连续登录任务目录，以及登录连续天数列。
 */
@Component
@Order(3)
public class TaskCatalogBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskCatalogBootstrap.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<String> ACTIVE_CODES = List.of(
            "T-DAILY-LOGIN",
            "T-DAILY-FIRST-WIN",
            "T-DAILY-MATCH-1",
            "T-DAILY-MATCH-2",
            "T-DAILY-MATCH-3",
            "T-DAILY-DUO",
            "T-DAILY-ROTATE-0",
            "T-DAILY-ROTATE-1",
            "T-DAILY-ROTATE-2",
            "T-DAILY-ROTATE-3",
            "T-DAILY-ROTATE-4",
            "T-DAILY-ROTATE-5",
            "T-DAILY-ROTATE-6",
            "T-CARD-001",
            "T-STREAK-3",
            "T-STREAK-7"
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
        upsertDailyCatalog();
        disableLegacyDailyTasks();
        migrateLegacyCardPlayTask();
        syncGrowthCardPlayProgress();
        activateTodayRotate();
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

    private void upsertDailyCatalog() {
        upsert("T-DAILY-LOGIN", "每日登录", "daily", "DAILY", "DAY", "LOGIN_COUNT",
                "登录一次即可，开局才有更大奖励", "login_count", "{}", "money", "{\"amount\":20}", 1, 10, 1);
        upsert("T-DAILY-FIRST-WIN", "今天赢一把", "daily", "DAILY", "DAY", "WIN_COUNT",
                "今日首胜，打出这一把的最大金币", "win_count", "{}", "money", "{\"amount\":100}", 1, 20, 1);
        upsert("T-DAILY-MATCH-1", "完成 1 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "打完一局就能领，输赢都算", "match_count", "{}", "money", "{\"amount\":30}", 1, 30, 1);
        upsert("T-DAILY-MATCH-2", "完成 2 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "今天再打一局，多一份金币", "match_count", "{}", "money", "{\"amount\":40}", 2, 31, 1);
        upsert("T-DAILY-MATCH-3", "完成 3 局", "daily", "DAILY", "DAY", "MATCH_COUNT",
                "今日第三局，当日最大包", "match_count", "{}", "money", "{\"amount\":80}", 3, 32, 1);
        upsert("T-DAILY-DUO", "双人开局", "daily", "DAILY", "DAY", "COOP_MATCH_COUNT",
                "和好友组队打完一局", "coop_match", "{}", "money", "{\"amount\":40}", 1, 40, 1);

        upsert("T-DAILY-ROTATE-0", "销售部出牌", "daily", "DAILY", "DAY", "PLAY_DEPT",
                "今天销售部打出 3 张牌", "play_dept", "{\"dept\":\"sales\"}", "money", "{\"amount\":35}", 3, 50, 0);
        upsert("T-DAILY-ROTATE-1", "采购部出牌", "daily", "DAILY", "DAY", "PLAY_DEPT",
                "今天采购部打出 3 张牌", "play_dept", "{\"dept\":\"purchase\"}", "money", "{\"amount\":35}", 3, 50, 0);
        upsert("T-DAILY-ROTATE-2", "造成伤害", "daily", "DAILY", "DAY", "DAMAGE_DEALT",
                "今天对霸凌者造成 30 点伤害", "damage_dealt", "{}", "money", "{\"amount\":40}", 30, 50, 0);
        upsert("T-DAILY-ROTATE-3", "多出几张牌", "daily", "DAILY", "DAY", "CARD_PLAY_COUNT",
                "今天打出 8 张牌", "card_play_count", "{}", "money", "{\"amount\":35}", 8, 50, 0);
        upsert("T-DAILY-ROTATE-4", "公共部出牌", "daily", "DAILY", "DAY", "PLAY_DEPT",
                "今天公共部打出 3 张牌", "play_dept", "{\"dept\":\"public\"}", "money", "{\"amount\":35}", 3, 50, 0);
        upsert("T-DAILY-ROTATE-5", "再出几张牌", "daily", "DAILY", "DAY", "CARD_PLAY_COUNT",
                "今天打出 5 张牌", "card_play_count", "{}", "money", "{\"amount\":30}", 5, 50, 0);
        upsert("T-DAILY-ROTATE-6", "持续输出", "daily", "DAILY", "DAY", "DAMAGE_DEALT",
                "今天对霸凌者造成 20 点伤害", "damage_dealt", "{}", "money", "{\"amount\":30}", 20, 50, 0);

        upsert("T-CARD-001", "使用卡牌", "growth", "NONE", "ALL", "CARD_PLAY_COUNT",
                "累计使用 200 张卡牌", "card_play_count", "{\"count\":200}", "money", "{\"amount\":200}", 200, 70, 1);

        upsert("T-STREAK-3", "连续登录 3 天", "growth", "NONE", "ALL", "LOGIN_STREAK",
                "连续登录 3 天，断一天重新算", "login_streak", "{}", "money", "{\"amount\":80}", 3, 80, 1);
        upsert("T-STREAK-7", "连续登录 7 天", "growth", "NONE", "ALL", "LOGIN_STREAK",
                "连续登录 7 天，断一天重新算", "login_streak", "{}", "money", "{\"amount\":200}", 7, 81, 1);
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

    private void disableLegacyDailyTasks() {
        String placeholders = String.join(",", ACTIVE_CODES.stream().map(code -> "?").toList());
        List<Object> args = new java.util.ArrayList<>(ACTIVE_CODES);
        jdbcTemplate.update(
                "UPDATE tasks SET status = 0 WHERE task_type = 'daily' AND task_code NOT IN (" + placeholders + ")",
                args.toArray());
        jdbcTemplate.update("""
                UPDATE tasks SET status = 0
                WHERE task_name LIKE '%输1局%'
                   OR task_name LIKE '%首3局%'
                   OR description LIKE '%故意%'
                """);
    }

    private void migrateLegacyCardPlayTask() {
        jdbcTemplate.update("""
                UPDATE tasks
                SET task_name = '使用卡牌',
                    task_type = 'growth',
                    reset_type = 'NONE',
                    period_scope = 'ALL',
                    progress_type = 'CARD_PLAY_COUNT',
                    description = '累计使用 200 张卡牌',
                    condition_type = 'card_play_count',
                    condition_value = CAST('{"count":200}' AS JSON),
                    reward_type = 'money',
                    reward_value = CAST('{"amount":200}' AS JSON),
                    target_count = 200,
                    status = 1
                WHERE status = 1
                  AND (
                    task_code = 'T-CARD-001'
                    OR task_name = '使用卡牌'
                    OR (
                      task_type = 'growth'
                      AND condition_type IN ('card_play_count', 'play_card', 'CARD_PLAY_COUNT')
                    )
                  )
                """);
    }

    private void syncGrowthCardPlayProgress() {
        if (!tableExists("user_tasks")) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.target_value = t.target_count,
                    ut.status = CASE
                        WHEN ut.status = 3 THEN 3
                        WHEN ut.progress_value >= t.target_count THEN 2
                        WHEN ut.progress_value > 0 THEN 1
                        ELSE 0
                    END,
                    ut.completed_at = CASE
                        WHEN ut.status = 3 THEN ut.completed_at
                        WHEN ut.progress_value >= t.target_count THEN COALESCE(ut.completed_at, NOW())
                        ELSE NULL
                    END
                WHERE t.task_type = 'growth'
                  AND t.progress_type = 'CARD_PLAY_COUNT'
                  AND ut.period_key = 'ALL'
                """);
    }

    private void activateTodayRotate() {
        int todayIndex = LocalDate.now(ZONE).getDayOfWeek().getValue() - 1;
        jdbcTemplate.update("UPDATE tasks SET status = 0 WHERE task_code LIKE 'T-DAILY-ROTATE-%'");
        jdbcTemplate.update("UPDATE tasks SET status = 1 WHERE task_code = ?", "T-DAILY-ROTATE-" + todayIndex);
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
