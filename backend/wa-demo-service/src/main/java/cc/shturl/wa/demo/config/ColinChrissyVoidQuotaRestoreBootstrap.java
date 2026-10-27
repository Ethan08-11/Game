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
 * 第 2454 局 Colin / Chrissy 于 2026-10-27 12:21（上海）被系统自动作废。
 * 对局保持作废，但删掉 stuck_void 标记。次数按整局计算，两人当天的一次作废机会都会回来。
 */
@Component
@Order(51)
public class ColinChrissyVoidQuotaRestoreBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ColinChrissyVoidQuotaRestoreBootstrap.class);
    static final String PATCH_ID = "restore_void_quota_match_2454_20261027";
    private static final long MATCH_ID = 2454L;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public ColinChrissyVoidQuotaRestoreBootstrap(JdbcTemplate jdbcTemplate,
                                                  PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("matches") || !tableExists("match_actions") || !tableExists("schema_patches")) {
                return;
            }
            Integer patched = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM schema_patches WHERE patch_id = ?",
                    Integer.class,
                    PATCH_ID);
            if (patched != null && patched > 0) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                Integer winnerType = jdbcTemplate.query(
                        "SELECT winner_type FROM matches WHERE id = ?",
                        rs -> rs.next() ? rs.getInt(1) : null,
                        MATCH_ID);
                if (winnerType == null || winnerType != 3) {
                    log.warn("Skip void-quota restore; match {} is not void.", MATCH_ID);
                    return;
                }
                int removed = jdbcTemplate.update("""
                        DELETE FROM match_actions
                        WHERE match_id = ? AND action_type = 'stuck_void'
                        """, MATCH_ID);
                jdbcTemplate.update("INSERT INTO schema_patches(patch_id) VALUES (?)", PATCH_ID);
                log.warn("Restored daily void quota for match {} by removing {} stuck_void mark.", MATCH_ID, removed);
            });
        } catch (Exception e) {
            log.error("Colin/Chrissy void quota restore failed.", e);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }
}
