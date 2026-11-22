package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * 凌晨的正常局曾被算到前一天，补打又把当天已经领过的同一档发了一遍。
 * 只扣这种：该日正常局数已经盖住这一档，而且当天的完成金币和胜利金币都已领取。
 */
@Component
@Order(61)
public class DuplicateMakeupSlotGoldBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DuplicateMakeupSlotGoldBootstrap.class);
    static final String PATCH_ID = "makeup_duplicate_slot_gold_20261122";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public DuplicateMakeupSlotGoldBootstrap(JdbcTemplate jdbcTemplate,
                                            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("user_makeup_claims") || !tableExists("user_profiles")
                    || !tableExists("matches") || !tableExists("user_tasks")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (claw()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Duplicate makeup slot gold clawback failed.", e);
        }
    }

    private boolean claw() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT c.id, c.user_id, c.gold_paid, c.owed_day, c.slot_no, c.match_id
                FROM user_makeup_claims c
                WHERE c.gold_paid > 0
                  AND (
                    SELECT COUNT(*)
                    FROM matches m
                    INNER JOIN match_players me ON me.match_id = m.id AND me.user_id = c.user_id
                    WHERE m.status = 2
                      AND IFNULL(m.winner_type, 0) IN (1, 2)
                      AND me.makeup_slot IS NULL
                      AND DATE(DATE_ADD(COALESCE(m.ended_at, m.started_at, m.created_at), INTERVAL 8 HOUR)) = c.owed_day
                  ) >= c.slot_no
                  AND EXISTS (
                    SELECT 1
                    FROM user_tasks ut
                    INNER JOIN tasks t ON t.id = ut.task_id
                    WHERE ut.user_id = c.user_id
                      AND ut.period_key = DATE_FORMAT(c.owed_day, '%Y-%m-%d')
                      AND t.task_code = CONCAT('T-DAILY-MATCH-', c.slot_no)
                      AND IFNULL(ut.status, 0) >= 3
                  )
                  AND (
                    c.win_gold = 0
                    OR EXISTS (
                      SELECT 1
                      FROM user_tasks ut
                      INNER JOIN tasks t ON t.id = ut.task_id
                      WHERE ut.user_id = c.user_id
                        AND ut.period_key = DATE_FORMAT(c.owed_day, '%Y-%m-%d')
                        AND t.task_code = CONCAT('T-DAILY-WIN-', c.slot_no)
                        AND IFNULL(ut.status, 0) >= 3
                    )
                  )
                """);
        for (Map<String, Object> row : rows) {
            long claimId = ((Number) row.get("id")).longValue();
            long userId = ((Number) row.get("user_id")).longValue();
            int gold = ((Number) row.get("gold_paid")).intValue();
            int updated = jdbcTemplate.update("""
                    UPDATE user_makeup_claims
                    SET gold_paid = 0,
                        finish_gold = 0,
                        win_gold = 0,
                        finish_claimed = 1,
                        win_claimed = 1
                    WHERE id = ? AND user_id = ? AND gold_paid = ?
                    """, claimId, userId, gold);
            if (updated != 1) {
                log.warn("Skip duplicate makeup claw; claim {} changed.", claimId);
                return false;
            }
            jdbcTemplate.update("""
                    UPDATE user_profiles
                    SET money = GREATEST(0, IFNULL(money, 0) - ?),
                        weekly_money = GREATEST(0, IFNULL(weekly_money, 0) - ?)
                    WHERE user_id = ?
                    """, gold, gold, userId);
            log.warn("Clawed duplicate makeup gold userId={} day={} slot={} matchId={} gold={}.",
                    userId, row.get("owed_day"), row.get("slot_no"), row.get("match_id"), gold);
        }
        log.info("Duplicate makeup slot gold clawback finished, rows={}.", rows.size());
        return true;
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
