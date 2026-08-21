package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 顾客效果数值：略微提高难度。
 */
@Component
@Order(4)
public class CustomerCatalogBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CustomerCatalogBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public CustomerCatalogBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("customer_types")) {
            log.info("Skip customer catalog bootstrap: customer_types missing.");
            return;
        }
        update("CUSTOMER_KIND", -1, 30, 32);
        update("CUSTOMER_TIMID", 2, 65, 38);
        update("CUSTOMER_ANXIOUS", 2, 60, 30);
        log.info("Customer catalog difficulty tuned.");
    }

    private void update(String code, int effectValue, int triggerChance, int selectionWeight) {
        jdbcTemplate.update("""
                UPDATE customer_types
                SET effect_value = ?, trigger_chance = ?, selection_weight = ?
                WHERE customer_code = ?
                """, effectValue, triggerChance, selectionWeight, code);
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return count != null && count > 0;
    }
}
