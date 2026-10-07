package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 给已经由玩家或系统申请作废的对局补上 stuck_void 标记。
 * 运营重置的作废没有这条记录，不占用每人每天 1 次卡死次数。
 * match 1884 是 Amy / Harry 在卡住约 10 分钟后由系统作废的。
 */
@Component
@Order(32)
public class StuckVoidQuotaMarkerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StuckVoidQuotaMarkerBootstrap.class);
    static final String PATCH_ID = "mark_stuck_void_match_1884_20261007";
    private static final long MATCH_ID = 1884L;

    private final JdbcTemplate jdbcTemplate;

    public StuckVoidQuotaMarkerBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("match_actions") || !tableExists("schema_patches")) {
                return;
            }
            Integer patched = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM schema_patches WHERE patch_id = ?",
                    Integer.class,
                    PATCH_ID);
            if (patched != null && patched > 0) {
                return;
            }
            Integer winnerType = jdbcTemplate.query(
                    "SELECT winner_type FROM matches WHERE id = ?",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    MATCH_ID);
            if (winnerType != null && winnerType == 3) {
                Integer marked = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM match_actions
                        WHERE match_id = ? AND action_type = 'stuck_void'
                        """, Integer.class, MATCH_ID);
                if (marked == null || marked == 0) {
                    jdbcTemplate.update("""
                            INSERT INTO match_actions(match_id, actor_type, action_type, extra_data, created_at)
                            VALUES (?, 'system', 'stuck_void', '{"reason":"auto"}', '2026-10-07 03:51:56')
                            """, MATCH_ID);
                    log.warn("Marked match {} as a real stuck void.", MATCH_ID);
                }
            }
            jdbcTemplate.update("INSERT INTO schema_patches(patch_id) VALUES (?)", PATCH_ID);
        } catch (Exception e) {
            log.error("Stuck-void quota marker failed.", e);
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
