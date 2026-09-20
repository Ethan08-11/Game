package cc.shturl.wa.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class WorkDayService {
    public record Snapshot(int used, int quota, boolean restDay, boolean todayWorkDay) {
    }

    private final JdbcTemplate jdbcTemplate;

    public Snapshot snapshot(Long userId) {
        LocalDate today = LocalDate.now(WorkDayQuota.ZONE);
        YearMonth month = YearMonth.from(today);
        int quota = WorkDayQuota.days(month);
        if (userId == null || !tableReady()) {
            return new Snapshot(0, quota, false, false);
        }
        boolean todayWorkDay = exists(userId, today);
        int used = countUsed(userId, month);
        boolean restDay = !todayWorkDay && used >= quota;
        return new Snapshot(used, quota, restDay, todayWorkDay);
    }

    /** 今天已是工作日，或本月还能新开一个工作日时，允许发金币。 */
    public boolean allowGold(Long userId) {
        if (userId == null) {
            return false;
        }
        if (!tableReady()) {
            return true;
        }
        LocalDate today = LocalDate.now(WorkDayQuota.ZONE);
        if (exists(userId, today)) {
            return true;
        }
        YearMonth month = YearMonth.from(today);
        if (countUsed(userId, month) >= WorkDayQuota.days(month)) {
            return false;
        }
        jdbcTemplate.update(
                "INSERT IGNORE INTO user_month_work_days(user_id, day_date) VALUES (?, ?)",
                userId, Date.valueOf(today));
        return exists(userId, today);
    }

    private int countUsed(Long userId, YearMonth month) {
        Date start = Date.valueOf(month.atDay(1));
        Date end = Date.valueOf(month.plusMonths(1).atDay(1));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_month_work_days WHERE user_id = ? AND day_date >= ? AND day_date < ?",
                Integer.class, userId, start, end);
        return count == null ? 0 : count;
    }

    private boolean exists(Long userId, LocalDate day) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_month_work_days WHERE user_id = ? AND day_date = ?",
                Integer.class, userId, Date.valueOf(day));
        return count != null && count > 0;
    }

    private boolean tableReady() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, "user_month_work_days");
        return count != null && count > 0;
    }
}
