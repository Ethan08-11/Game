package cc.shturl.wa.demo.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * 复活超时曾在未把对局标成已结束的情况下反复发失败奖，把金币刷高。
 * 在定时任务启动前：收掉卡住的复活等待局（不发奖），再扣回多发的金币。
 */
@Component
public class ProfileMoneyRepairBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ProfileMoneyRepairBootstrap.class);
    private static final String PATCH_ID = "restore_money_revive_timeout_inflation_20260820";
    private static final long VICTORY_MONEY = 50L;
    private static final long DEFEAT_MONEY = 10L;
    private static final int VICTORY_EXP = 100;
    private static final int DEFEAT_EXP = 30;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public ProfileMoneyRepairBootstrap(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    public void restoreOnce() {
        try {
            if (!tableExists("user_profiles") || !tableExists("matches") || !tableExists("match_players")) {
                log.info("Skip profile money repair; match tables are missing.");
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                log.info("Profile money repair {} already applied.", PATCH_ID);
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                int closed = closeStuckReviveMatches();
                List<Map<String, Object>> before = snapshotProfiles();
                int updated = restoreInflatedRewards();
                markPatchApplied();
                logRepair(closed, updated, before);
            });
        } catch (Exception e) {
            log.error("Failed to restore profile money from revive-timeout inflation.", e);
        }
    }

    private int closeStuckReviveMatches() {
        jdbcTemplate.update("""
                UPDATE room_members rm
                INNER JOIN matches m ON m.room_id = rm.room_id
                SET rm.left_at = IFNULL(rm.left_at, NOW()),
                    rm.online_status = 0,
                    rm.ready_status = 0
                WHERE m.status = 1 AND m.phase = 'REVIVE_WAIT' AND rm.left_at IS NULL
                """);
        jdbcTemplate.update("""
                UPDATE game_rooms r
                INNER JOIN matches m ON m.room_id = r.id
                SET r.status = 3,
                    r.closed_at = IFNULL(r.closed_at, NOW()),
                    r.player_count = 0
                WHERE m.status = 1 AND m.phase = 'REVIVE_WAIT'
                """);
        jdbcTemplate.update("""
                UPDATE match_players mp
                INNER JOIN matches m ON m.id = mp.match_id
                SET mp.result_type = 3,
                    mp.player_status = 'LEFT'
                WHERE m.status = 1 AND m.phase = 'REVIVE_WAIT'
                """);
        return jdbcTemplate.update("""
                UPDATE matches
                SET status = 2,
                    phase = 'FINISHED',
                    winner_type = 3,
                    ended_at = IFNULL(ended_at, NOW())
                WHERE status = 1 AND phase = 'REVIVE_WAIT'
                """);
    }

    private int restoreInflatedRewards() {
        return jdbcTemplate.update("""
                UPDATE user_profiles p
                LEFT JOIN (
                    SELECT mp.user_id,
                           SUM(m.winner_type = 1) AS wins,
                           SUM(m.winner_type = 2) AS losses,
                           SUM(m.winner_type = 3) AS draws
                    FROM match_players mp
                    INNER JOIN matches m ON m.id = mp.match_id
                    WHERE m.status = 2 AND m.winner_type IN (1, 2, 3)
                    GROUP BY mp.user_id
                ) s ON s.user_id = p.user_id
                SET p.money = GREATEST(0,
                        IFNULL(p.money, 0)
                        - GREATEST(0, IFNULL(p.win_count, 0) - IFNULL(s.wins, 0)) * ?
                        - GREATEST(0, IFNULL(p.lose_count, 0) - IFNULL(s.losses, 0)) * ?),
                    p.exp = GREATEST(0,
                        IFNULL(p.exp, 0)
                        - GREATEST(0, IFNULL(p.win_count, 0) - IFNULL(s.wins, 0)) * ?
                        - GREATEST(0, IFNULL(p.lose_count, 0) - IFNULL(s.losses, 0)) * ?),
                    p.win_count = IFNULL(s.wins, 0),
                    p.lose_count = IFNULL(s.losses, 0),
                    p.draw_count = IFNULL(s.draws, 0)
                """,
                VICTORY_MONEY, DEFEAT_MONEY, VICTORY_EXP, DEFEAT_EXP);
    }

    private List<Map<String, Object>> snapshotProfiles() {
        return jdbcTemplate.queryForList("""
                SELECT u.username, p.user_id, p.money, p.win_count, p.lose_count, p.exp
                FROM user_profiles p
                LEFT JOIN users u ON u.id = p.user_id
                ORDER BY p.user_id
                """);
    }

    private void logRepair(int closed, int updated, List<Map<String, Object>> before) {
        List<Map<String, Object>> after = snapshotProfiles();
        log.warn("Restored profile money after revive-timeout inflation: closedStuckMatches={}, profilesTouched={}.",
                closed, updated);
        for (Map<String, Object> row : before) {
            Object userId = row.get("user_id");
            Map<String, Object> next = after.stream()
                    .filter(item -> userId != null && userId.equals(item.get("user_id")))
                    .findFirst()
                    .orElse(null);
            if (next == null) {
                continue;
            }
            if (sameNumber(row.get("money"), next.get("money"))
                    && sameNumber(row.get("win_count"), next.get("win_count"))
                    && sameNumber(row.get("lose_count"), next.get("lose_count"))) {
                continue;
            }
            log.warn("Profile money restored user={} id={}: money {} -> {}, win {} -> {}, lose {} -> {}, exp {} -> {}",
                    row.get("username"), userId,
                    row.get("money"), next.get("money"),
                    row.get("win_count"), next.get("win_count"),
                    row.get("lose_count"), next.get("lose_count"),
                    row.get("exp"), next.get("exp"));
        }
    }

    private boolean sameNumber(Object left, Object right) {
        return String.valueOf(left).equals(String.valueOf(right));
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

    private boolean patchApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_patches WHERE patch_id = ?",
                Integer.class,
                PATCH_ID);
        return count != null && count > 0;
    }

    private void markPatchApplied() {
        jdbcTemplate.update("INSERT INTO schema_patches(patch_id) VALUES (?)", PATCH_ID);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }
}
