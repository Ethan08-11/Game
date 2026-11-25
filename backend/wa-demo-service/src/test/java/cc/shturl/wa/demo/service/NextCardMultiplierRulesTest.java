package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NextCardMultiplierRulesTest {

    private static final String SHIELD = "{\"consumeAfterUse\":true,\"multiplyEffectTypes\":[\"ADD_SHIELD\"]}";
    private static final String DAMAGE = "{\"consumeAfterUse\":true,\"multiplyEffectTypes\":[\"DAMAGE_BOSS\"]}";

    @Test
    void damageBuffDoesNotReplaceShieldBuff() {
        Set<String> shield = NextCardMultiplierRules.effectTypes(SHIELD);
        Set<String> damage = NextCardMultiplierRules.effectTypes(DAMAGE);
        assertEquals(Set.of("ADD_SHIELD"), shield);
        assertEquals(Set.of("DAMAGE_BOSS"), damage);
        assertFalse(NextCardMultiplierRules.replaces(damage, shield));
        assertFalse(NextCardMultiplierRules.replaces(shield, damage));
    }

    @Test
    void sameKindReplacesItself() {
        Set<String> shield = NextCardMultiplierRules.effectTypes(SHIELD);
        assertTrue(NextCardMultiplierRules.replaces(shield, shield));
    }

    @Test
    void untypedBuffReplacesEveryPendingBuff() {
        assertTrue(NextCardMultiplierRules.replaces(Set.of(), NextCardMultiplierRules.effectTypes(SHIELD)));
        assertTrue(NextCardMultiplierRules.replaces(NextCardMultiplierRules.effectTypes(DAMAGE), Set.of()));
    }

    @Test
    void eachBuffCoversOnlyItsOwnEffect() {
        Set<String> shield = NextCardMultiplierRules.effectTypes(SHIELD);
        Set<String> damage = NextCardMultiplierRules.effectTypes(DAMAGE);
        assertTrue(NextCardMultiplierRules.covers(shield, "ADD_SHIELD"));
        assertFalse(NextCardMultiplierRules.covers(shield, "DAMAGE_BOSS"));
        assertTrue(NextCardMultiplierRules.covers(damage, "DAMAGE_BOSS"));
        assertFalse(NextCardMultiplierRules.covers(damage, "ADD_SHIELD"));
        assertTrue(NextCardMultiplierRules.covers(Set.of(), "ADD_SHIELD"));
    }
}
