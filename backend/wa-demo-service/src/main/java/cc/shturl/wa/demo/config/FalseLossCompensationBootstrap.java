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
import java.util.Locale;
import java.util.Map;

/**
 * Ethan / Riley 上一局双方仍有血却被记失败。一次性补偿：胜率分别调到 98% / 100%，排行榜金币各 +50。
 */
@Component
@Order(6)
public class FalseLossCompensationBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FalseLossCompensationBootstrap.class);
    private static final String PATCH_ID = "compensate_ethan_riley_false_loss_20260920";
    private static final long MONEY_BONUS = 50L;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public FalseLossCompensationBootstrap(JdbcTemplate jdbcTemplate,
                                          PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("users") || !tableExists("user_profiles")) {
            return;
        }
        ensurePatchTable();
        if (patchApplied()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            compensate("ethan", 98);
            compensate("riley", 100);
            markPatchApplied();
        });
    }

    private void compensate(String username, int targetWinRate) {
        String key = username.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT u.id AS user_id, u.username,
                       IFNULL(p.win_count, 0) AS win_count,
                       IFNULL(p.lose_count, 0) AS lose_count,
                       IFNULL(p.draw_count, 0) AS draw_count,
                       IFNULL(p.money, 0) AS money
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE LOWER(u.username) = ?
                   OR LOWER(IFNULL(p.display_name, '')) = ?
                ORDER BY CASE WHEN LOWER(u.username) = ? THEN 0 ELSE 1 END, u.id
                LIMIT 1
                """, key, key, key);
        if (rows.isEmpty()) {
            log.warn("False-loss compensation skipped; user {} not found.", username);
            return;
        }
        Map<String, Object> row = rows.get(0);
        long userId = ((Number) row.get("user_id")).longValue();
        int wins = ((Number) row.get("win_count")).intValue();
        int losses = ((Number) row.get("lose_count")).intValue();
        int draws = ((Number) row.get("draw_count")).intValue();
        long money = ((Number) row.get("money")).longValue();
        Record next = countsForWinRate(wins + losses + draws, targetWinRate);
        jdbcTemplate.update("""
                INSERT INTO user_profiles (user_id, display_name, win_count, lose_count, draw_count, money, created_at, updated_at)
                SELECT u.id, u.username, 0, 0, 0, 0, NOW(), NOW()
                FROM users u
                WHERE u.id = ?
                  AND NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id)
                """,
                userId);
        int updated = jdbcTemplate.update("""
                UPDATE user_profiles
                SET win_count = ?,
                    lose_count = ?,
                    draw_count = ?,
                    money = IFNULL(money, 0) + ?
                WHERE user_id = ?
                """,
                next.wins(), next.losses(), next.draws(), MONEY_BONUS, userId);
        log.warn("False-loss compensation user={} id={}: win/lose/draw {}/{}/{} -> {}/{}/{}, money {} -> {}, rows={}",
                row.get("username"), userId, wins, losses, draws, next.wins(), next.losses(), next.draws(),
                money, money + MONEY_BONUS, updated);
    }

    static Record countsForWinRate(int currentTotal, int targetPercent) {
        int total = Math.max(currentTotal, 1);
        if (targetPercent >= 100) {
            return new Record(total, 0, 0);
        }
        Integer wins = winsForDisplayedRate(total, targetPercent);
        if (wins == null) {
            for (int candidate = total + 1; candidate <= Math.max(80, total + 200); candidate++) {
                wins = winsForDisplayedRate(candidate, targetPercent);
                if (wins != null) {
                    total = candidate;
                    break;
                }
            }
        }
        if (wins == null) {
            return new Record(49, 1, 0);
        }
        return new Record(wins, total - wins, 0);
    }

    static Integer winsForDisplayedRate(int total, int targetPercent) {
        if (total <= 0) {
            return null;
        }
        Integer best = null;
        double bestDiff = Double.MAX_VALUE;
        for (int wins = 0; wins <= total; wins++) {
            int shown = (int) Math.round(wins * 100.0 / total);
            if (shown != targetPercent) {
                continue;
            }
            double diff = Math.abs(wins * 100.0 / total - targetPercent);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = wins;
            }
        }
        return best;
    }

    record Record(int wins, int losses, int draws) {
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
