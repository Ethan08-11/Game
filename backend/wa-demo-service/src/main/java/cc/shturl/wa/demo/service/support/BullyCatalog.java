package cc.shturl.wa.demo.service.support;

import cc.shturl.wa.demo.dto.resp.CustomerInfoResp;
import cc.shturl.wa.demo.entity.Bullies;
import cc.shturl.wa.demo.entity.CustomerTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 顾客与霸凌者一对一，以及 skill_data / 回合状态解析。
 */
public final class BullyCatalog {

    public static final String CUSTOMER_KIND = "CUSTOMER_KIND";
    public static final String CUSTOMER_TIMID = "CUSTOMER_TIMID";
    public static final String CUSTOMER_ANXIOUS = "CUSTOMER_ANXIOUS";
    public static final String CUSTOMER_WINDOW = "CUSTOMER_WINDOW";

    public static final String BULLY_FOCUS = "BULLY_FOCUS_001";
    public static final String BULLY_SHIELD = "BULLY_SHIELD_001";
    public static final String BULLY_REVENGE = "BULLY_REVENGE_001";
    public static final String BULLY_PAIR = "BULLY_PAIR_001";

    public static final String PATTERN_FOCUS_LOW_HP = "FOCUS_LOW_HP";
    public static final String PATTERN_ROUND_SHIELD = "ROUND_SHIELD";
    public static final String PATTERN_FOCUS_TOP_DAMAGE = "FOCUS_TOP_DAMAGE";
    public static final String PATTERN_BOTH_HALF_SWING = "BOTH_HALF_SWING";

    /** 销售+采购掷点；双销售由 pressureForSalesCount 再加一档。 */
    public static final int HP = 150;
    public static final int ATTACK_MIN = 23;
    public static final int ATTACK_MAX = 26;
    public static final int DEFENSE_STANCE_CHANCE = 15;
    public static final int DEFENSE_SHIELD = 14;
    public static final int PAIR_CHIP_THRESHOLD = 3;
    public static final int REVENGE_BONUS = 8;

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, String> CUSTOMER_TO_BULLY = Map.of(
            CUSTOMER_KIND, BULLY_FOCUS,
            CUSTOMER_TIMID, BULLY_SHIELD,
            CUSTOMER_ANXIOUS, BULLY_REVENGE,
            CUSTOMER_WINDOW, BULLY_PAIR
    );

    private BullyCatalog() {
    }

    public static String bullyCodeForCustomer(String customerCode) {
        if (customerCode == null) {
            return null;
        }
        return CUSTOMER_TO_BULLY.get(customerCode.trim());
    }

    public static BullySkill parse(Bullies bully) {
        if (bully == null) {
            return BullySkill.NONE;
        }
        String code = bully.getBullyCode() == null ? "" : bully.getBullyCode().trim();
        JsonNode root = readTree(bully.getSkillData());
        String pattern = text(root, "pattern", patternForCode(code));
        int chance = chanceForPattern(pattern);
        if (chance == 0) {
            chance = number(root, "chance", 0);
        }
        int shield = PATTERN_ROUND_SHIELD.equals(pattern) ? DEFENSE_SHIELD : number(root, "shield", 0);
        int bonus = PATTERN_FOCUS_TOP_DAMAGE.equals(pattern) ? REVENGE_BONUS : number(root, "bonusAttack", 0);
        String summary = summaryForPattern(pattern);
        if (summary.isBlank()) {
            summary = text(root, "catalogSummary", "");
        }
        return new BullySkill(pattern, chance, shield, bonus, summary);
    }

    public static CustomerInfoResp toCustomerResp(CustomerTypes customer, Bullies bully) {
        if (customer == null) {
            return new CustomerInfoResp(null, null, null, null, null, null, null, null, null, 0,
                    null, null, null, null, null);
        }
        BullySkill skill = parse(bully);
        Integer chance = displayChance(skill);
        return new CustomerInfoResp(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCustomerName(),
                customer.getDescription(),
                customer.getImageUrl(),
                customer.getEffectType(),
                customer.getEffectValue(),
                customer.getTriggerChance(),
                customer.getSelectionWeight(),
                customer.getStatus(),
                bully == null ? null : bully.getBullyCode(),
                bully == null ? null : bully.getBullyName(),
                bully == null ? null : bully.getDescription(),
                skill.summary().isBlank() ? null : skill.summary(),
                chance);
    }

    public record Pressure(int hpMin, int hpMax, int attackMin, int attackMax) {
        public int rollHp() {
            return ThreadLocalRandom.current().nextInt(hpMin, hpMax + 1);
        }

        public int rollAttack() {
            return ThreadLocalRandom.current().nextInt(attackMin, attackMax + 1);
        }

        public int baseAttack() {
            return (attackMin + attackMax) / 2;
        }
    }

    /**
     * 血量固定 150。攻击仍按组队：销售+采购 23～26，双销售/双采购 21～24。
     */
    public static Pressure pressureForSalesCount(int salesCount) {
        int n = Math.max(0, salesCount);
        if (n == 1) {
            return new Pressure(HP, HP, ATTACK_MIN, ATTACK_MAX);
        }
        return new Pressure(HP, HP, 21, 24);
    }

    public static int rollAttack() {
        return rollAttack(1);
    }

    public static int rollAttack(int salesCount) {
        return pressureForSalesCount(salesCount).rollAttack();
    }

    public static boolean rollDefenseStance() {
        return roll(DEFENSE_STANCE_CHANCE);
    }

    public static int pierceFor(BullySkill skill, boolean defenseStance) {
        return 0;
    }

    public static boolean roll(int chance) {
        if (chance >= 100) {
            return true;
        }
        if (chance <= 0) {
            return false;
        }
        return ThreadLocalRandom.current().nextInt(100) < chance;
    }

    public static RoundState readRoundState(String raw) {
        JsonNode root = readTree(raw);
        if (root == null) {
            return RoundState.empty();
        }
        Map<Long, Integer> snapshots = new LinkedHashMap<>();
        JsonNode snapNode = root.get("snapshots");
        if (snapNode != null && snapNode.isObject()) {
            snapNode.fields().forEachRemaining(entry -> {
                try {
                    snapshots.put(Long.parseLong(entry.getKey()), entry.getValue().asInt(0));
                } catch (NumberFormatException ignored) {
                    // skip
                }
            });
        }
        boolean defense = root.path("defenseStance").asBoolean(root.path("shieldRolled").asBoolean(false));
        return new RoundState(
                defense,
                root.path("focusRolled").asBoolean(false),
                root.path("halfSwingThisRound").asBoolean(false),
                root.path("halfSwingNextRound").asBoolean(false),
                snapshots,
                root.path("fullAttack").asInt(0));
    }

    public static String writeRoundState(RoundState state) {
        ObjectNode root = JSON.createObjectNode();
        root.put("shieldRolled", state.shieldRolled());
        root.put("defenseStance", state.defenseStance());
        root.put("focusRolled", state.focusRolled());
        root.put("halfSwingThisRound", state.halfSwingThisRound());
        root.put("halfSwingNextRound", state.halfSwingNextRound());
        root.put("fullAttack", state.fullAttack());
        ObjectNode snapshots = root.putObject("snapshots");
        state.snapshots().forEach((userId, value) -> snapshots.put(String.valueOf(userId), value));
        return root.toString();
    }

    public record BullySkill(String pattern, int chance, int shield, int bonusAttack, String summary) {
        public static final BullySkill NONE = new BullySkill("", 0, 0, 0, "");

        public boolean alwaysOn() {
            return PATTERN_FOCUS_LOW_HP.equals(pattern) || chance >= 100;
        }

        public boolean is(String expected) {
            return expected != null && expected.equals(pattern);
        }
    }

    public record RoundState(boolean shieldRolled, boolean focusRolled, boolean halfSwingThisRound,
                             boolean halfSwingNextRound, Map<Long, Integer> snapshots, int fullAttack) {
        public static RoundState empty() {
            return new RoundState(false, false, false, false, Map.of(), 0);
        }

        public boolean defenseStance() {
            return shieldRolled;
        }

        public RoundState withHalfSwingNextRound(boolean value) {
            return new RoundState(shieldRolled, focusRolled, halfSwingThisRound, value, snapshots, fullAttack);
        }
    }

    private static Integer displayChance(BullySkill skill) {
        if (skill.alwaysOn() || skill.is(PATTERN_BOTH_HALF_SWING) || skill.chance() <= 0) {
            return null;
        }
        return skill.chance();
    }

    private static String patternForCode(String bullyCode) {
        return switch (bullyCode) {
            case BULLY_FOCUS -> PATTERN_FOCUS_LOW_HP;
            case BULLY_SHIELD -> PATTERN_ROUND_SHIELD;
            case BULLY_REVENGE -> PATTERN_FOCUS_TOP_DAMAGE;
            case BULLY_PAIR -> PATTERN_BOTH_HALF_SWING;
            default -> "";
        };
    }

    private static int chanceForPattern(String pattern) {
        return switch (pattern) {
            case PATTERN_FOCUS_LOW_HP -> 100;
            case PATTERN_ROUND_SHIELD -> DEFENSE_STANCE_CHANCE;
            case PATTERN_FOCUS_TOP_DAMAGE -> 100 - DEFENSE_STANCE_CHANCE;
            case PATTERN_BOTH_HALF_SWING -> 0;
            default -> 0;
        };
    }

    private static String summaryForPattern(String pattern) {
        return switch (pattern) {
            case PATTERN_FOCUS_LOW_HP -> "专打血更虚的护卫；护盾可以挡住。";
            case PATTERN_ROUND_SHIELD -> "约一成五回合胸口多 14 点盾，这回合出手也变轻。";
            case PATTERN_FOCUS_TOP_DAMAGE -> "约八成五回合会盯打得最疼的人多挨 8 点。";
            case PATTERN_BOTH_HALF_SWING -> "两人都几乎挡住时，下一拍会再抽半刀。";
            default -> "";
        };
    }

    private static JsonNode readTree(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return JSON.readTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String text(JsonNode root, String field, String fallback) {
        if (root == null || !root.has(field) || root.get(field).isNull()) {
            return fallback;
        }
        String value = root.get(field).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int number(JsonNode root, String field, int fallback) {
        if (root == null || !root.has(field) || root.get(field).isNull()) {
            return fallback;
        }
        return root.get(field).asInt(fallback);
    }
}
