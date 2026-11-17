package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.AchievementService;
import cc.shturl.wa.demo.service.TaskService;
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
 * Riley 与 Yuna 的 2740 局把霸凌者打到 0，双方都勾了补打。
 * 当时补打胜负没有写入每日任务，任务页上这局胜利看起来像没赢。
 * 补上 2026-11-17 的获胜进度。补打金币已经发过，这里不再发一次。
 */
@Component
@Order(57)
public class RileyYunaWinQuestBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RileyYunaWinQuestBootstrap.class);
    static final String PATCH_ID = "credit_riley_yuna_match_2740_win_quest";
    private static final long MATCH_ID = 2740L;
    private static final long RILEY_ID = 30L;
    private static final long YUNA_ID = 19L;
    private static final String PERIOD = "2026-11-17";
    private static final String COUNTED_AT = "2026-11-17 09:37:45";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final TaskService taskService;
    private final AchievementService achievementService;

    public RileyYunaWinQuestBootstrap(JdbcTemplate jdbcTemplate,
                                      PlatformTransactionManager transactionManager,
                                      TaskService taskService,
                                      AchievementService achievementService) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.taskService = taskService;
        this.achievementService = achievementService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("matches") || !tableExists("user_tasks") || !tableExists("tasks")) {
                return;
            }
            ensurePatchTable();
            if (patchApplied()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (credit()) {
                    markPatchApplied();
                }
            });
        } catch (Exception e) {
            log.error("Riley/Yuna win quest credit failed.", e);
        }
    }

    private boolean credit() {
        Integer winner = jdbcTemplate.queryForObject(
                "SELECT winner_type FROM matches WHERE id = ? AND status = 2",
                Integer.class, MATCH_ID);
        if (winner == null || winner != 1) {
            log.warn("Skip Riley/Yuna win credit; match {} is not a settled win.", MATCH_ID);
            return false;
        }
        boolean riley = creditRileyThirdWin();
        boolean yuna = creditYunaSecondWin();
        taskService.rebuildWeeklyTeammates(RILEY_ID);
        taskService.rebuildWeeklyTeammates(YUNA_ID);
        achievementService.refreshProgress(RILEY_ID);
        achievementService.refreshProgress(YUNA_ID);
        log.info("Credited match {} win quests. riley={} yuna={}", MATCH_ID, riley, yuna);
        return true;
    }

    /** Riley 当天已有两场普通胜利。这场补打胜局补成第 3 局。 */
    private boolean creditRileyThirdWin() {
        Integer slot = progress(RILEY_ID, "T-DAILY-SLOT");
        Integer win = progress(RILEY_ID, "T-DAILY-WIN-3");
        if (slot == null || slot != 2 || win == null || win > 0) {
            log.info("Riley quest already moved. slot={} win3={}", slot, win);
            return false;
        }
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.progress_value = 3
                WHERE ut.user_id = ? AND ut.period_key = ? AND t.task_code = 'T-DAILY-SLOT'
                  AND ut.progress_value = 2
                """, RILEY_ID, PERIOD);
        complete(RILEY_ID, "T-DAILY-MATCH-3", 3);
        complete(RILEY_ID, "T-DAILY-WIN-3", 1);
        return true;
    }

    /**
     * Yuna 当天的失败把第 2 局进度也占上了，这场胜利没有写入任何获胜任务。
     * 第 2 局名额已经在，只补「赢第 2 局」，不再多占一局。
     */
    private boolean creditYunaSecondWin() {
        Integer slot = progress(YUNA_ID, "T-DAILY-SLOT");
        Integer win = progress(YUNA_ID, "T-DAILY-WIN-2");
        if (slot == null || slot != 2 || win == null || win > 0) {
            log.info("Yuna quest already moved. slot={} win2={}", slot, win);
            return false;
        }
        complete(YUNA_ID, "T-DAILY-WIN-2", 1);
        return true;
    }

    private Integer progress(long userId, String taskCode) {
        return jdbcTemplate.queryForObject("""
                SELECT ut.progress_value
                FROM user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                WHERE ut.user_id = ? AND ut.period_key = ? AND t.task_code = ?
                LIMIT 1
                """, Integer.class, userId, PERIOD, taskCode);
    }

    private void complete(long userId, String taskCode, int progress) {
        jdbcTemplate.update("""
                UPDATE user_tasks ut
                INNER JOIN tasks t ON t.id = ut.task_id
                SET ut.progress_value = ?,
                    ut.status = 2,
                    ut.completed_at = IFNULL(ut.completed_at, ?)
                WHERE ut.user_id = ? AND ut.period_key = ? AND t.task_code = ?
                  AND IFNULL(ut.status, 0) < 2
                """, progress, COUNTED_AT, userId, PERIOD, taskCode);
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
                Integer.class, PATCH_ID);
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }
}
