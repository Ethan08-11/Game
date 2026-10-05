package cc.shturl.wa.demo.service.support;

import cc.shturl.wa.demo.entity.CustomerTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BullyCatalogTriggerChanceTest {

    @Test
    @DisplayName("负面顾客对局内按 60% 触发，图鉴仍用库里的 50%")
    void negativeCombatTriggerIgnoresStoredFifty() {
        assertThat(BullyCatalog.combatTriggerChance(customer("CUSTOMER_TIMID", "bully_hp_up", 50)))
                .isEqualTo(60);
        assertThat(BullyCatalog.combatTriggerChance(customer("CUSTOMER_ANXIOUS", "bully_attack_up", 50)))
                .isEqualTo(60);
        assertThat(BullyCatalog.combatTriggerChance(customer("CUSTOMER_HARSH", "player_action_hp_down", 50)))
                .isEqualTo(60);
    }

    @Test
    @DisplayName("正面顾客仍按库里的触发概率")
    void positiveCustomersKeepStoredChance() {
        assertThat(BullyCatalog.combatTriggerChance(customer("CUSTOMER_KIND", "bully_attack_down", 50)))
                .isEqualTo(50);
        assertThat(BullyCatalog.combatTriggerChance(customer("CUSTOMER_WINDOW", "player_hp_up", 50)))
                .isEqualTo(50);
        assertThat(BullyCatalog.combatTriggerChance(customer("CUSTOMER_WEALTHY", "player_action_up", 50)))
                .isEqualTo(50);
    }

    private static CustomerTypes customer(String code, String effectType, int triggerChance) {
        CustomerTypes customer = new CustomerTypes();
        customer.setCustomerCode(code);
        customer.setEffectType(effectType);
        customer.setTriggerChance(triggerChance);
        return customer;
    }
}
