package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 补打用的勾选、对局标记和已补档位。已有库在启动时补列。
 */
@Component
@Order(4)
public class MakeupSchemaBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MakeupSchemaBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public MakeupSchemaBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tableExists("match_players") && !columnExists("match_players", "makeup_owed_day")) {
            jdbcTemplate.execute("""
                    ALTER TABLE `match_players`
                    ADD COLUMN `makeup_owed_day` date NULL DEFAULT NULL
                      COMMENT '补打对应的北京时间自然日' AFTER `result_type`
                    """);
            log.info("Added match_players.makeup_owed_day.");
        }
        if (tableExists("match_players") && !columnExists("match_players", "makeup_slot")) {
            jdbcTemplate.execute("""
                    ALTER TABLE `match_players`
                    ADD COLUMN `makeup_slot` tinyint NULL DEFAULT NULL
                      COMMENT '补打对应的第几局：1/2/3' AFTER `makeup_owed_day`
                    """);
            log.info("Added match_players.makeup_slot.");
        }
        if (tableExists("room_members") && !columnExists("room_members", "makeup_opt_in")) {
            jdbcTemplate.execute("""
                    ALTER TABLE `room_members`
                    ADD COLUMN `makeup_opt_in` tinyint NOT NULL DEFAULT 0
                      COMMENT '开局前勾选：这局算补打' AFTER `ready_status`
                    """);
            log.info("Added room_members.makeup_opt_in.");
        }
        if (!tableExists("user_makeup_claims")) {
            jdbcTemplate.execute("""
                    CREATE TABLE `user_makeup_claims` (
                      `id` bigint NOT NULL AUTO_INCREMENT,
                      `user_id` bigint NOT NULL,
                      `owed_day` date NOT NULL COMMENT '被补上的自然日',
                      `slot_no` tinyint NOT NULL COMMENT '第几局 1/2/3',
                      `match_id` bigint NOT NULL,
                      `gold_paid` int NOT NULL DEFAULT 0,
                      `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `uk_user_makeup_slot` (`user_id`, `owed_day`, `slot_no`),
                      KEY `idx_user_makeup_match` (`match_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已结算的补打档，含放弃'
                    """);
            log.info("Created user_makeup_claims.");
        }
        addClaimColumn("finish_gold", """
                ALTER TABLE `user_makeup_claims`
                ADD COLUMN `finish_gold` int NOT NULL DEFAULT 0
                  COMMENT '这一档打完可领的金币' AFTER `gold_paid`
                """);
        addClaimColumn("win_gold", """
                ALTER TABLE `user_makeup_claims`
                ADD COLUMN `win_gold` int NOT NULL DEFAULT 0
                  COMMENT '这一档赢了可领的额外金币' AFTER `finish_gold`
                """);
        addClaimColumn("finish_claimed", """
                ALTER TABLE `user_makeup_claims`
                ADD COLUMN `finish_claimed` tinyint NOT NULL DEFAULT 1
                  COMMENT '打完奖励已领。旧记录默认已领，避免再发一次' AFTER `win_gold`
                """);
        addClaimColumn("win_claimed", """
                ALTER TABLE `user_makeup_claims`
                ADD COLUMN `win_claimed` tinyint NOT NULL DEFAULT 1
                  COMMENT '获胜额外金币已领。旧记录默认已领' AFTER `finish_claimed`
                """);
        if (!tableExists("user_makeup_claim_snapshot")) {
            jdbcTemplate.execute("""
                    CREATE TABLE `user_makeup_claim_snapshot` (
                      `snapshot_id` varchar(64) NOT NULL,
                      `claim_id` bigint NOT NULL,
                      `user_id` bigint NOT NULL,
                      `owed_day` date NOT NULL,
                      `slot_no` tinyint NOT NULL,
                      `match_id` bigint NOT NULL,
                      `gold_paid` int NOT NULL,
                      `finish_gold` int NOT NULL,
                      `win_gold` int NOT NULL,
                      `finish_claimed` tinyint NOT NULL,
                      `win_claimed` tinyint NOT NULL,
                      `claim_created_at` datetime NULL,
                      `profile_money` bigint NOT NULL DEFAULT 0,
                      `profile_weekly_money` bigint NOT NULL DEFAULT 0,
                      `profile_win_count` int NOT NULL DEFAULT 0,
                      `profile_lose_count` int NOT NULL DEFAULT 0,
                      `snapshotted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (`snapshot_id`, `claim_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='改补打顺序前的场次和金币，只插入不改原账'
                    """);
            log.info("Created user_makeup_claim_snapshot.");
        }
    }

    private void addClaimColumn(String column, String ddl) {
        if (!tableExists("user_makeup_claims") || columnExists("user_makeup_claims", column)) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("Added user_makeup_claims.{}.", column);
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
