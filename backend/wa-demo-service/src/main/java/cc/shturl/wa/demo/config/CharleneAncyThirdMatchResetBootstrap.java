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
 * 作废 Charlene / ANCY 2026-10-03 卡退的那一局（match 1788）。
 * 该局是放弃结算：记了失败并占用当日局数，没有发放金币。
 * 只回退失败场次和局数槽，不动已完成的前两局，也不扣已领金币。
 */
@Component
@Order(23)
public class CharleneAncyThirdMatchResetBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CharleneAncyThirdMatchResetBootstrap.class);
    static final String PATCH_ID = "reset_charlene_ancy_match_1788_20261003";
    private static final long MATCH_ID = 1788L;
    private static final String PERIOD = "2026-10-03";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public CharleneAncyThirdMatchResetBootstrap(JdbcTemplate jdbcTemplate,
                                                PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("matches") || !tableExists("match_players") || !tableExists("user_profiles")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (reset()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Charlene/ANCY third-match reset failed.", e);
        }
    }

    private boolean reset() {
        int updated = jdbcTemplate.update("""
                UPDATE matches
                SET winner_type = 3
                WHERE id = ? AND status = 2 AND winner_type = 2
                """, MATCH_ID);
        if (updated != 1) {
            log.warn("Skip Charlene/ANCY reset; match {} is not a settled loss.", MATCH_ID);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE match_players
                SET result_type = 3,
                    player_status = 'LEFT'
                WHERE match_id = ? AND result_type = 2
                """, MATCH_ID);
        jdbcTemplate.update("""
                UPDATE user_profiles p
                INNER JOIN match_players mp ON mp.user_id = p.user_id
                SET p.lose_count = GREATEST(0, IFNULL(p.lose_count, 0) - 1)
                WHERE mp.match_id = ?
                """, MATCH_ID);
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                INNER JOIN match_players mp ON mp.user_id = ut.user_id
                SET ut.progress_value = GREATEST(0, IFNULL(ut.progress_value, 0) - 1),
                    ut.status = 1,
                    ut.completed_at = NULL
                WHERE mp.match_id = ?
                  AND ut.period_key = ?
                  AND t.task_code = 'T-DAILY-SLOT'
                """, MATCH_ID, PERIOD);
        log.warn("Voided stuck match {} and rolled back the loss plus daily slot. No gold was paid.", MATCH_ID);
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
