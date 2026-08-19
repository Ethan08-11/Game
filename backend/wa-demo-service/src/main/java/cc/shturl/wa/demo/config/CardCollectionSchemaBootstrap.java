package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 线上库不会自动执行 mysql/004、005。启动时补齐图鉴所需列/表，并幂等写入收藏卡。
 */
@Component
@Order(0)
public class CardCollectionSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CardCollectionSchemaBootstrap.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public CardCollectionSchemaBootstrap(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("cards")) {
            log.info("cards table missing; skip card collection schema bootstrap.");
            return;
        }
        ensureRequireUnlockColumn();
        ensureUserCardPoolsTable();
        realignLegacyCardDepartments();
        refreshEthanCardArt();
        runScript("db/004_collectible_cards.sql");
        runScript("db/005_it_sample_card.sql");
        log.info("Card collection schema bootstrap finished.");
    }

    private void ensureRequireUnlockColumn() {
        if (columnExists("cards", "require_unlock")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `cards`
                ADD COLUMN `require_unlock` tinyint NOT NULL DEFAULT 0
                COMMENT '1需胜利解锁才可进入对局' AFTER `is_unique`
                """);
        log.warn("Added cards.require_unlock column.");
    }

    private void ensureUserCardPoolsTable() {
        if (tableExists("user_card_pools")) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE `user_card_pools` (
                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '卡池记录主键',
                  `user_id` bigint NOT NULL COMMENT '拥有者用户ID',
                  `card_id` bigint NOT NULL COMMENT '拥有的卡牌ID',
                  `owned_count` int NOT NULL DEFAULT 0 COMMENT '拥有数量',
                  `unlocked_status` tinyint NOT NULL DEFAULT 1 COMMENT '解锁状态：1已解锁',
                  `level` int NOT NULL DEFAULT 1 COMMENT '个人卡牌养成等级(备用)',
                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获取时间',
                  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_user_card` (`user_id`, `card_id`),
                  KEY `card_id` (`card_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户已获取/解锁的卡牌表'
                """);
        log.warn("Created user_card_pools table.");
    }

    private void realignLegacyCardDepartments() {
        int riley = jdbcTemplate.update(
                "UPDATE `cards` SET `dept_id` = 1, `dept_type` = 'sales' WHERE `card_code` = 'O-15' OR `card_name` = 'Riley'");
        int charlene = jdbcTemplate.update(
                "UPDATE `cards` SET `dept_id` = 2, `dept_type` = 'purchase' WHERE `card_code` = 'O-01' OR `card_name` IN ('Charlene', 'Cherlene')");
        if (riley > 0 || charlene > 0) {
            log.warn("Realigned card departments: Riley/O-15 -> sales ({}), Charlene/O-01 -> purchase ({}).", riley, charlene);
        }
    }

    private void refreshEthanCardArt() {
        int n = jdbcTemplate.update(
                "UPDATE `cards` SET `image_url` = '/images/cards/技术_Ethan.webp' WHERE `card_code` = 'O-13' OR `card_name` = 'Ethan'");
        if (n > 0) {
            log.warn("Updated Ethan/O-13 card art to 技术_Ethan.webp ({} rows).", n);
        }
    }

    private void runScript(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            log.warn("SQL script missing on classpath: {}", classpathLocation);
            return;
        }
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource);
            populator.setSqlScriptEncoding("UTF-8");
            populator.setContinueOnError(true);
            populator.setIgnoreFailedDrops(true);
            populator.setSeparator(";");
            populator.execute(dataSource);
        } catch (Exception e) {
            log.error("Failed to run {}", classpathLocation, e);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }
}
