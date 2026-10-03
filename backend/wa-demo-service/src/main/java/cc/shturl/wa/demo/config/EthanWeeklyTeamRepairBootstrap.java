package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.QuestPeriod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 10 月 2 日 Ethan 已打有效局，20:00 仍误补了 3 个虚拟周常队友。
 * 从本周「跟 10 位不同同事组合」里去掉这 3 人（Eden / Chrissy / Iris）。
 */
@Component
@Order(22)
public class EthanWeeklyTeamRepairBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EthanWeeklyTeamRepairBootstrap.class);
    private static final String PATCH_ID = "repair_ethan_weekly_oct2_idle_20261003";
    private static final String WEEKLY_CODE = "T-WEEKLY-TEAM-10";
    private static final Set<Long> IDLE_FALSE_TEAMMATES = Set.of(12L, 13L, 15L);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public EthanWeeklyTeamRepairBootstrap(JdbcTemplate jdbcTemplate,
                                          PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("users") || !tableExists("user_tasks") || !tableExists("tasks")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                repair();
                markPatchApplied();
            });
        } catch (Exception e) {
            log.error("Ethan weekly team repair failed.", e);
        }
    }

    private void repair() {
        Long userId = findUserId("ethan");
        if (userId == null) {
            log.warn("Ethan weekly repair skipped; user not found.");
            return;
        }
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList("""
                SELECT id, target_count FROM tasks WHERE task_code = ? LIMIT 1
                """, WEEKLY_CODE);
        if (tasks.isEmpty()) {
            return;
        }
        long taskId = ((Number) tasks.get(0).get("id")).longValue();
        int target = Math.max(number(tasks.get(0).get("target_count"), 10), 1);
        String period = QuestPeriod.weeklyStartForDaily(QuestPeriod.currentDailyDate()).toString();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT progress_value, extra_data, status
                FROM user_tasks
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                LIMIT 1
                """, userId, taskId, period);
        if (rows.isEmpty()) {
            log.warn("Ethan weekly repair skipped; no user_task for period={}.", period);
            return;
        }
        Map<String, Object> row = rows.get(0);
        int status = number(row.get("status"), 0);
        if (status >= 3) {
            log.warn("Ethan weekly already claimed; leave period={} untouched.", period);
            return;
        }
        Set<Long> ids = parseIdSet(extraDataString(row.get("extra_data")));
        int before = ids.size();
        ids.removeAll(IDLE_FALSE_TEAMMATES);
        int progress = Math.min(ids.size(), target);
        int nextStatus = progress >= target ? 2 : (progress > 0 ? 1 : 0);
        jdbcTemplate.update("""
                UPDATE user_tasks
                SET progress_value = ?,
                    target_value = ?,
                    extra_data = CAST(? AS JSON),
                    status = ?,
                    completed_at = CASE WHEN ? >= 2 THEN IFNULL(completed_at, NOW()) ELSE NULL END,
                    updated_at = NOW()
                WHERE user_id = ? AND task_id = ? AND period_key = ?
                """, progress, target, writeIdSet(ids), nextStatus, nextStatus, userId, taskId, period);
        log.warn("Repaired Ethan weekly teammates period={}: {} -> {} extra={}",
                period, before, progress, writeIdSet(ids));
    }

    private String extraDataString(Object value) {
        if (value == null) {
            return "[]";
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? "[]" : text;
    }

    private Set<Long> parseIdSet(String json) {
        Set<Long> ids = new LinkedHashSet<>();
        if (json == null || json.isBlank() || "null".equalsIgnoreCase(json)) {
            return ids;
        }
        try {
            JsonNode node = JSON.readTree(json);
            if (node == null || !node.isArray()) {
                return ids;
            }
            for (JsonNode item : node) {
                if (item.isNumber()) {
                    ids.add(item.longValue());
                }
            }
        } catch (Exception ignored) {
            return ids;
        }
        return ids;
    }

    private String writeIdSet(Set<Long> ids) {
        ArrayNode array = JSON.createArrayNode();
        for (Long id : ids) {
            array.add(id);
        }
        return array.toString();
    }

    private Long findUserId(String username) {
        String key = username.toLowerCase();
        List<Long> ids = jdbcTemplate.query("""
                SELECT u.id
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE LOWER(u.username) = ?
                   OR LOWER(IFNULL(p.display_name, '')) = ?
                ORDER BY CASE WHEN LOWER(u.username) = ? THEN 0 ELSE 1 END, u.id
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), key, key, key);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
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
        jdbcTemplate.update("INSERT IGNORE INTO schema_patches(patch_id) VALUES (?)", PATCH_ID);
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
