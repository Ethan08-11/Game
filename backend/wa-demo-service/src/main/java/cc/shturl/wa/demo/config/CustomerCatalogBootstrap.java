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
 * 顾客目录：难度微调，并幂等写入新顾客。
 */
@Component
@Order(4)
public class CustomerCatalogBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CustomerCatalogBootstrap.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public CustomerCatalogBootstrap(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("customer_types")) {
            log.info("Skip customer catalog bootstrap: customer_types missing.");
            return;
        }
        ensureBossCurrentShieldColumn();
        ensureBullyRoundDataColumn();
        runScript("db/015_window_couple_customer.sql");
        runScript("db/019_customer_bound_bullies.sql");
        runScript("db/023_bully_focus_no_pierce.sql");
        runScript("db/024_all_customers_hard_shield.sql");
        runScript("db/025_bully_round_winrate.sql");
        update("CUSTOMER_KIND", -1, 30, 32);
        update("CUSTOMER_TIMID", 2, 65, 38);
        update("CUSTOMER_ANXIOUS", 2, 60, 30);
        update("CUSTOMER_WINDOW", 2, 20, 10);
        log.info("Customer catalog difficulty tuned.");
    }

    private void ensureBossCurrentShieldColumn() {
        if (!tableExists("matches") || columnExists("matches", "boss_current_shield")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `matches`
                ADD COLUMN `boss_current_shield` int NOT NULL DEFAULT 0
                COMMENT '霸凌者本回合可打掉的护盾' AFTER `boss_current_attack`
                """);
        log.info("Added matches.boss_current_shield column.");
    }

    private void ensureBullyRoundDataColumn() {
        if (!tableExists("matches") || columnExists("matches", "bully_round_data")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `matches`
                ADD COLUMN `bully_round_data` json NULL
                COMMENT '本回合霸凌者特效状态' AFTER `boss_current_shield`
                """);
        log.info("Added matches.bully_round_data column.");
    }

    private void update(String code, int effectValue, int triggerChance, int selectionWeight) {
        jdbcTemplate.update("""
                UPDATE customer_types
                SET effect_value = ?, trigger_chance = ?, selection_weight = ?
                WHERE customer_code = ?
                """, effectValue, triggerChance, selectionWeight, code);
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

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                        """,
                Integer.class, table, column);
        return count != null && count > 0;
    }
}
