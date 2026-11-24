package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 现有账号标成内部版。对外版另有四位虚构顾客，不使用内部吐槽画像。
 */
@Component
@Order(70)
public class GameEditionBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(GameEditionBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public GameEditionBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("users") || !tableExists("customer_types") || !tableExists("game_rooms")) {
                return;
            }
            addColumn("users", "audience",
                    "ALTER TABLE users ADD COLUMN audience varchar(16) NOT NULL DEFAULT 'INTERNAL' COMMENT 'INTERNAL内部版 PUBLIC对外版'");
            addColumn("customer_types", "edition",
                    "ALTER TABLE customer_types ADD COLUMN edition varchar(16) NOT NULL DEFAULT 'INTERNAL' COMMENT 'INTERNAL内部版 PUBLIC对外版'");
            addColumn("game_rooms", "selected_customer_id",
                    "ALTER TABLE game_rooms ADD COLUMN selected_customer_id bigint NULL COMMENT '对外版本局选定顾客'");
            addColumn("game_rooms", "customer_confirmed",
                    "ALTER TABLE game_rooms ADD COLUMN customer_confirmed tinyint NOT NULL DEFAULT 0 COMMENT '队友已确认对外顾客'");
            seedPublicCustomer("PUBLIC_STEADY", "稳当客",
                    "话不多。半数回合霸凌者攻击 -1。",
                    "bully_attack_down", -1, 1);
            seedPublicCustomer("PUBLIC_PAIR", "结伴客",
                    "两人一起来。半数回合两名存活护卫各恢复 1 点血。",
                    "player_hp_up", 1, 2);
            seedPublicCustomer("PUBLIC_DRAW", "发牌客",
                    "喜欢看人动手。半数回合两名存活护卫各抽 1 张牌。",
                    "player_draw", 1, 3);
            seedPublicCustomer("PUBLIC_SHIELD", "护盾客",
                    "先把场面稳住。半数回合，下回合两名存活护卫各得 4 点防御。",
                    "player_shield_next", 4, 4);
        } catch (Exception e) {
            log.error("Game edition bootstrap failed.", e);
        }
    }

    private void seedPublicCustomer(String code, String name, String description,
                                    String effectType, int effectValue, int sortNo) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_types WHERE customer_code = ?",
                Integer.class,
                code);
        if (existing != null && existing > 0) {
            jdbcTemplate.update("""
                    UPDATE customer_types
                    SET customer_name = ?, description = ?, image_url = '',
                        effect_type = ?, effect_value = ?, trigger_chance = 50,
                        selection_weight = 0, status = 1, sort_no = ?, edition = 'PUBLIC'
                    WHERE customer_code = ?
                    """, name, description, effectType, effectValue, sortNo, code);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO customer_types(
                    customer_code, customer_name, description, image_url,
                    effect_type, effect_value, trigger_chance, selection_weight,
                    status, sort_no, edition)
                VALUES (?, ?, ?, '', ?, ?, 50, 0, 1, ?, 'PUBLIC')
                """, code, name, description, effectType, effectValue, sortNo);
        log.info("Added public customer {}.", code);
    }

    private void addColumn(String table, String column, String ddl) {
        if (columnExists(table, column)) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("Added {}.{}", table, column);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                table,
                column);
        return count != null && count > 0;
    }
}
