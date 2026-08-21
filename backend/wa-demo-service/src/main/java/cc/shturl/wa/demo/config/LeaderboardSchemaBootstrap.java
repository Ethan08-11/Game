package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.impl.LeaderboardServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;

/**
 * 周榜：本周金币列 + 当前周起始日。每周一 0 点（Asia/Shanghai）刷新。
 */
@Component
@Order(2)
public class LeaderboardSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardSchemaBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardSchemaBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("user_profiles")) {
            log.info("user_profiles missing; skip weekly leaderboard schema.");
            return;
        }
        ensureWeeklyMoneyColumn();
        ensureWeekStateTable();
        ensureAlignedFlagColumn();
        alignWeeklyToTotalOnce();
        log.info("Weekly leaderboard schema ready, week starting {}.", LeaderboardServiceImpl.currentWeekStart());
    }

    private boolean ensureWeeklyMoneyColumn() {
        if (columnExists("user_profiles", "weekly_money")) {
            return false;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `user_profiles`
                ADD COLUMN `weekly_money` bigint NOT NULL DEFAULT 0
                COMMENT '本周获得金币，每周一0点清零'
                AFTER `money`
                """);
        log.info("Added user_profiles.weekly_money column.");
        return true;
    }

    private void ensureWeekStateTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS leaderboard_week (
                  id tinyint NOT NULL,
                  week_start date NOT NULL,
                  aligned_to_total tinyint NOT NULL DEFAULT 0,
                  PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周榜当前周起始日（周一）'
                """);
        LocalDate weekStart = LeaderboardServiceImpl.currentWeekStart();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM leaderboard_week WHERE id = 1", Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO leaderboard_week(id, week_start) VALUES (1, ?)", Date.valueOf(weekStart));
        }
    }

    private void ensureAlignedFlagColumn() {
        if (columnExists("leaderboard_week", "aligned_to_total")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `leaderboard_week`
                ADD COLUMN `aligned_to_total` tinyint NOT NULL DEFAULT 0
                COMMENT '是否已用总榜金币对齐周榜，只执行一次'
                """);
        log.info("Added leaderboard_week.aligned_to_total column.");
    }

    /**
     * 一次性把周榜金币对齐总榜，本周两边数字一致；下周一 0 点再按原逻辑清零周榜。
     */
    private void alignWeeklyToTotalOnce() {
        Integer aligned = jdbcTemplate.queryForObject(
                "SELECT aligned_to_total FROM leaderboard_week WHERE id = 1", Integer.class);
        if (aligned != null && aligned == 1) {
            return;
        }
        int updated = jdbcTemplate.update("UPDATE user_profiles SET weekly_money = IFNULL(money, 0)");
        LocalDate weekStart = LeaderboardServiceImpl.currentWeekStart();
        jdbcTemplate.update(
                "UPDATE leaderboard_week SET week_start = ?, aligned_to_total = 1 WHERE id = 1",
                Date.valueOf(weekStart));
        log.info("Aligned weekly_money to total money for {} profiles; next reset is Monday after {}.",
                updated, weekStart);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }
}
