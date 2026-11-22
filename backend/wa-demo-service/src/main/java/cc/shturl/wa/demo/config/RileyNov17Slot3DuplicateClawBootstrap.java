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

/**
 * Riley 11 月 17 日第 3 局的完成金币当天已经领过，补打 2840 又领了 50。扣回这 50。
 */
@Component
@Order(63)
public class RileyNov17Slot3DuplicateClawBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RileyNov17Slot3DuplicateClawBootstrap.class);
    static final String PATCH_ID = "riley_nov17_slot3_makeup_duplicate_claw";
    private static final long RILEY_ID = 30L;
    private static final long MATCH_ID = 2840L;
    private static final int DUPLICATE_GOLD = 50;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RileyNov17Slot3DuplicateClawBootstrap(JdbcTemplate jdbcTemplate,
                                                 PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("user_makeup_claims") || !tableExists("user_profiles")
                    || !tableExists("user_tasks") || !tableExists("tasks")) {
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
            log.error("Riley Nov 17 slot 3 duplicate claw failed.", e);
        }
    }

    private boolean claw() {
        if (alreadyCleared()) {
            log.info("Riley Nov 17 slot 3 duplicate gold is already cleared.");
            return true;
        }
        Integer ready = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM user_makeup_claims c
                WHERE c.user_id = ?
                  AND c.match_id = ?
                  AND c.owed_day = '2026-11-17'
                  AND c.slot_no = 3
                  AND c.gold_paid = ?
                  AND c.finish_gold = ?
                  AND c.win_gold = 0
                  AND c.finish_claimed = 1
                  AND EXISTS (
                    SELECT 1
                    FROM user_tasks ut
                    INNER JOIN tasks t ON t.id = ut.task_id
                    WHERE ut.user_id = c.user_id
                      AND ut.period_key = '2026-11-17'
                      AND t.task_code = 'T-DAILY-MATCH-3'
                      AND IFNULL(ut.status, 0) >= 3
                  )
                """, Integer.class, RILEY_ID, MATCH_ID, DUPLICATE_GOLD, DUPLICATE_GOLD);
        if (ready == null || ready != 1) {
            log.warn("Skip Riley duplicate claw; match {} is not the claimed Nov 17 slot 3 double pay.", MATCH_ID);
            return false;
        }
        int updated = jdbcTemplate.update("""
                UPDATE user_makeup_claims
                SET gold_paid = 0,
                    finish_gold = 0,
                    win_gold = 0,
                    finish_claimed = 1,
                    win_claimed = 1
                WHERE user_id = ?
                  AND match_id = ?
                  AND owed_day = '2026-11-17'
                  AND slot_no = 3
                  AND gold_paid = ?
                """, RILEY_ID, MATCH_ID, DUPLICATE_GOLD);
        if (updated != 1) {
            log.warn("Skip Riley duplicate claw; claim for match {} changed.", MATCH_ID);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET money = GREATEST(0, IFNULL(money, 0) - ?),
                    weekly_money = GREATEST(0, IFNULL(weekly_money, 0) - ?)
                WHERE user_id = ?
                """, DUPLICATE_GOLD, DUPLICATE_GOLD, RILEY_ID);
        log.warn("Clawed Riley duplicate makeup gold userId={} matchId={} gold={}.",
                RILEY_ID, MATCH_ID, DUPLICATE_GOLD);
        return true;
    }

    private boolean alreadyCleared() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM user_makeup_claims
                WHERE user_id = ?
                  AND match_id = ?
                  AND owed_day = '2026-11-17'
                  AND slot_no = 3
                  AND gold_paid = 0
                  AND finish_gold = 0
                  AND finish_claimed = 1
                """, Integer.class, RILEY_ID, MATCH_ID);
        return count != null && count == 1;
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
