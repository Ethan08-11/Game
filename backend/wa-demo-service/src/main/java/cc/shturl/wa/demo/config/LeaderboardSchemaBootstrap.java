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
    private static final long VICTORY_MONEY = 50L;
    private static final long DEFEAT_MONEY = 10L;

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
        boolean addedColumn = ensureWeeklyMoneyColumn();
        ensureWeekStateTable();
        if (addedColumn) {
            backfillCurrentWeek();
        }
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
                  PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周榜当前周起始日（周一）'
                """);
        LocalDate weekStart = LeaderboardServiceImpl.currentWeekStart();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM leaderboard_week WHERE id = 1", Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO leaderboard_week(id, week_start) VALUES (1, ?)", Date.valueOf(weekStart));
        }
    }

    private void backfillCurrentWeek() {
        if (!tableExists("matches") || !tableExists("match_players")) {
            return;
        }
        LocalDate weekStart = LeaderboardServiceImpl.currentWeekStart();
        int updated = jdbcTemplate.update("""
                UPDATE user_profiles p
                LEFT JOIN (
                    SELECT mp.user_id,
                           SUM(CASE
                               WHEN m.winner_type = 1 THEN ?
                               WHEN m.winner_type = 2 THEN ?
                               ELSE 0
                           END) AS weekly_money
                    FROM match_players mp
                    INNER JOIN matches m ON m.id = mp.match_id
                    WHERE m.status = 2
                      AND m.winner_type IN (1, 2)
                      AND IFNULL(m.ended_at, m.updated_at) >= ?
                    GROUP BY mp.user_id
                ) s ON s.user_id = p.user_id
                SET p.weekly_money = IFNULL(s.weekly_money, 0)
                """,
                VICTORY_MONEY, DEFEAT_MONEY, Date.valueOf(weekStart));
        jdbcTemplate.update("UPDATE leaderboard_week SET week_start = ? WHERE id = 1", Date.valueOf(weekStart));
        log.info("Backfilled weekly_money for {} profiles from matches since {}.", updated, weekStart);
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
