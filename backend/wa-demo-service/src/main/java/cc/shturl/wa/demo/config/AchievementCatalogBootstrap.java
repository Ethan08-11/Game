package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 成就目录：入门保留原 5 条；补齐进阶 / 高难 / 极难。
 */
@Component
@Order(4)
public class AchievementCatalogBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AchievementCatalogBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public AchievementCatalogBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("achievement_defs")) {
            return;
        }
        ensureDifficultyColumn();
        ensureMinHpColumn();
        upsertCatalog();
        log.info("Achievement catalog ready.");
    }

    private void ensureDifficultyColumn() {
        if (columnExists("achievement_defs", "difficulty")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE achievement_defs
                  ADD COLUMN difficulty tinyint NOT NULL DEFAULT 1 COMMENT '1入门 2进阶 3高难 4极难' AFTER sort_no
                """);
        log.info("Added achievement_defs.difficulty.");
    }

    private void ensureMinHpColumn() {
        if (!tableExists("match_players") || columnExists("match_players", "min_hp")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE match_players
                  ADD COLUMN min_hp int NULL DEFAULT NULL COMMENT '本局出现过的最低血量' AFTER current_hp
                """);
        log.info("Added match_players.min_hp.");
    }

    private void upsertCatalog() {
        upsert("ACH-001", "首胜", "battle", "完成第一场胜利", "win_count", "{\"count\":1}",
                "title", "{\"name\":\"新手赢家\"}", 1, 1, 1);
        upsert("ACH-002", "百战老兵", "battle", "累计取得 10 场胜利", "win_count", "{\"count\":10}",
                "title", "{\"name\":\"百战老兵\"}", 2, 2, 1);
        upsert("ACH-003", "任务达人", "growth", "累计领取 20 个任务", "task_complete_count", "{\"count\":20}",
                "title", "{\"name\":\"任务达人\"}", 3, 2, 1);
        upsert("ACH-004", "社交先锋", "social", "累计添加 5 位好友", "friend_count", "{\"count\":5}",
                "title", "{\"name\":\"社交先锋\"}", 4, 2, 1);
        upsert("ACH-005", "隐藏彩蛋", "hidden", "成功解锁全部成就", "all_unlocked", "{\"count\":0}",
                "title", "{\"name\":\"彩蛋发现者\"}", 414, 4, 1);

        upsert("ACH-012", "稳定输出", "battle", "累计打完 20 局（输赢都算，作废不计）", "match_count", "{\"count\":20}",
                "title", "{\"name\":\"稳定输出\"}", 201, 2, 1);
        upsert("ACH-014", "三日打卡", "growth", "累计 3 个工作日领过金币", "work_day_count", "{\"count\":3}",
                "title", "{\"name\":\"三日打卡\"}", 202, 2, 1);
        upsert("ACH-016", "换着组", "social", "本周在每日前 3 局里和 5 个不同的人组过队", "unique_teammate", "{\"count\":5}",
                "title", "{\"name\":\"换着组\"}", 203, 2, 1);
        upsert("ACH-017", "连胜三", "battle", "连续获胜 3 局（放弃或失败打断，作废不打断）", "win_streak", "{\"count\":3}",
                "title", "{\"name\":\"连胜三\"}", 204, 2, 1);
        upsert("ACH-018", "混组默契", "battle", "销售+采购组合获胜 5 局", "mixed_win", "{\"count\":5}",
                "title", "{\"name\":\"混组默契\"}", 205, 2, 1);
        upsert("ACH-019", "收藏入门", "growth", "图鉴解锁 8 张收藏卡", "card_count", "{\"count\":8}",
                "title", "{\"name\":\"收藏入门\"}", 206, 2, 1);
        upsert("ACH-020", "顶压过关", "battle", "与今日总榜前五组队并获胜 1 局", "top5_win", "{\"count\":1}",
                "title", "{\"name\":\"顶压过关\"}", 207, 2, 1);

        upsert("ACH-021", "五十胜", "battle", "累计取得 50 场胜利", "win_count", "{\"count\":50}",
                "title", "{\"name\":\"五十胜\"}", 301, 3, 1);
        upsert("ACH-022", "满勤一周", "growth", "同一自然周领满 5 个工作日金币", "work_day_week", "{\"count\":5}",
                "title", "{\"name\":\"满勤一周\"}", 302, 3, 1);
        upsert("ACH-023", "十日十人", "social", "本周在每日前 3 局里和 10 个不同的人组过队", "unique_teammate", "{\"count\":10}",
                "title", "{\"name\":\"十日十人\"}", 303, 3, 1);
        upsert("ACH-024", "连胜五", "battle", "连续获胜 5 局", "win_streak", "{\"count\":5}",
                "title", "{\"name\":\"连胜五\"}", 304, 3, 1);
        upsert("ACH-025", "无复活十胜", "battle", "无人复活的胜利累计 10 局", "no_revive_win", "{\"count\":10}",
                "title", "{\"name\":\"无复活十胜\"}", 305, 3, 1);
        upsert("ACH-026", "双职精通", "battle", "销售获胜 20 局且采购获胜 20 局", "dept_win", "{\"sales\":20,\"purchase\":20}",
                "title", "{\"name\":\"双职精通\"}", 306, 3, 1);
        upsert("ACH-027", "图鉴过半", "growth", "可解锁收藏卡已解锁过半", "card_half", "{\"count\":0}",
                "title", "{\"name\":\"图鉴过半\"}", 307, 3, 1);
        upsert("ACH-028", "高压五胜", "battle", "与今日总榜前五组队并获胜 5 局", "top5_win", "{\"count\":5}",
                "title", "{\"name\":\"高压五胜\"}", 308, 3, 1);
        upsert("ACH-029", "月度过半", "growth", "当月工作日领过不少于定额一半", "work_day_half", "{\"count\":0}",
                "title", "{\"name\":\"月度过半\"}", 309, 3, 1);

        upsert("ACH-030", "百胜传奇", "battle", "累计取得 100 场胜利", "win_count", "{\"count\":100}",
                "title", "{\"name\":\"百胜传奇\"}", 401, 4, 1);
        upsert("ACH-031", "连胜十", "battle", "连续获胜 10 局", "win_streak", "{\"count\":10}",
                "title", "{\"name\":\"连胜十\"}", 402, 4, 1);
        upsert("ACH-032", "满勤十月", "growth", "2026 年 10 月领满 24 个工作日", "work_day_october", "{\"count\":24}",
                "title", "{\"name\":\"满勤十月\"}", 403, 4, 1);
        upsert("ACH-033", "图鉴大师", "growth", "当前可解锁的收藏卡全部集齐", "card_all", "{\"count\":0}",
                "title", "{\"name\":\"图鉴大师\"}", 404, 4, 1);
        upsert("ACH-034", "隐藏 · 绝境翻盘", "hidden", "双方都曾掉到危险血（≤5）后仍获胜", "both_low_hp_win", "{\"count\":1,\"hp\":5}",
                "title", "{\"name\":\"绝境翻盘\"}", 405, 4, 1);
        upsert("ACH-035", "隐藏 · 完美一日", "hidden", "同一自然日完成并赢得当日第 1、2、3 局", "daily_three_wins", "{\"count\":1}",
                "title", "{\"name\":\"完美一日\"}", 406, 4, 1);
        upsert("ACH-036", "隐藏 · 高压三连", "hidden", "与当日潜在前五组队连续获胜 3 局。失败或放弃打断，作废不打断，普通胜局也打断", "hp_win_streak", "{\"count\":3}",
                "title", "{\"name\":\"高压三连\"}", 408, 4, 1);
        upsert("ACH-037", "隐藏 · 三客碾压", "hidden", "在胆小怕事、焦虑难安、刻薄尖客局里各无复活获胜 1 局", "three_harsh_no_revive", "{\"count\":3}",
                "title", "{\"name\":\"三客碾压\"}", 409, 4, 1);
        upsert("ACH-038", "隐藏 · 残血不复活", "hidden", "双方最低血都曾 ≤5，全程无人复活，最后仍获胜", "low_hp_no_revive", "{\"count\":1,\"hp\":5}",
                "title", "{\"name\":\"残血不复活\"}", 410, 4, 1);
        upsert("ACH-039", "隐藏 · 双职无伤", "hidden", "销售无复活获胜 10 局，且采购无复活获胜 10 局", "dept_no_revive", "{\"sales\":10,\"purchase\":10}",
                "title", "{\"name\":\"双职无伤\"}", 411, 4, 1);
        upsert("ACH-040", "隐藏 · 速战", "hidden", "8 回合内无人复活获胜，累计 3 局", "fast_clear", "{\"count\":3,\"rounds\":8}",
                "title", "{\"name\":\"速战\"}", 412, 4, 1);
        upsert("ACH-041", "隐藏 · 完美一周", "hidden", "同一自然周里有 5 天都完成并赢得当日第 1、2、3 局", "perfect_week", "{\"count\":5}",
                "title", "{\"name\":\"完美一周\"}", 413, 4, 1);
    }

    private void upsert(String code, String name, String category, String description, String conditionType,
                        String conditionValue, String rewardType, String rewardValue, int sort, int difficulty,
                        int status) {
        jdbcTemplate.update("""
                INSERT INTO achievement_defs (
                  achievement_code, achievement_name, category, description,
                  condition_type, condition_value, reward_type, reward_value,
                  sort_no, difficulty, status
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), ?, CAST(? AS JSON), ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  achievement_name = VALUES(achievement_name),
                  category = VALUES(category),
                  description = VALUES(description),
                  condition_type = VALUES(condition_type),
                  condition_value = VALUES(condition_value),
                  reward_type = VALUES(reward_type),
                  reward_value = VALUES(reward_value),
                  sort_no = VALUES(sort_no),
                  difficulty = VALUES(difficulty),
                  status = VALUES(status)
                """,
                code, name, category, description, conditionType, conditionValue,
                rewardType, rewardValue, sort, difficulty, status);
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        return count != null && count > 0;
    }
}
