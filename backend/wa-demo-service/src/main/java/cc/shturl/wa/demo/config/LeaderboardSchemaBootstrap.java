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
 * 排行榜周期：总榜与胜率榜均按月计。每月 1 日 0 点（Asia/Shanghai）进入新月。
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
            log.info("user_profiles missing; skip leaderboard schema.");
            return;
        }
        ensureWeeklyMoneyColumn();
        ensureWeekStateTable();
        ensureAlignedFlagColumn();
        alignWeeklyToTotalOnce();
        log.info("Leaderboard schema ready, month starting {}.", LeaderboardServiceImpl.currentMonthStart());
    }

    private boolean ensureWeeklyMoneyColumn() {
        if (columnExists("user_profiles", "weekly_money")) {
            return false;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `user_profiles`
                ADD COLUMN `weekly_money` bigint NOT NULL DEFAULT 0
                COMMENT '历史周榜金币列，胜率榜已改按月、不再使用'
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜当前月起始日'
                """);
        LocalDate weekStart = LeaderboardServiceImpl.currentMonthStart();
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
     * 历史一次性对齐；胜率榜已不再读 weekly_money。
     */
    private void alignWeeklyToTotalOnce() {
        Integer aligned = jdbcTemplate.queryForObject(
                "SELECT aligned_to_total FROM leaderboard_week WHERE id = 1", Integer.class);
        if (aligned != null && aligned == 1) {
            return;
        }
        int updated = jdbcTemplate.update("UPDATE user_profiles SET weekly_money = IFNULL(money, 0)");
        LocalDate monthStart = LeaderboardServiceImpl.currentMonthStart();
        jdbcTemplate.update(
                "UPDATE leaderboard_week SET week_start = ?, aligned_to_total = 1 WHERE id = 1",
                Date.valueOf(monthStart));
        log.info("Aligned leftover weekly_money for {} profiles; leaderboard month starts {}.",
                updated, monthStart);
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
