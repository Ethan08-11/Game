package cc.shturl.wa.demo.task;

import cc.shturl.wa.demo.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 每天北京时间 23:59，把已经打完还没领的金币领走。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestGoldClaimTask {
    private final JdbcTemplate jdbcTemplate;
    private final TaskService taskService;

    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Shanghai")
    public void claimUnclaimedGold() {
        List<Long> userIds;
        try {
            userIds = jdbcTemplate.queryForList("SELECT id FROM users", Long.class);
        } catch (Exception ex) {
            log.warn("Skip end-of-day gold claim: {}", ex.getMessage());
            return;
        }
        int players = 0;
        for (Long userId : userIds) {
            try {
                taskService.claimEarnedGold(userId);
                players++;
            } catch (Exception ex) {
                log.warn("Skip end-of-day gold claim userId={}: {}", userId, ex.getMessage());
            }
        }
        log.info("End-of-day gold claim finished, players={}", players);
    }
}
