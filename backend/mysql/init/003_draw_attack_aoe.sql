-- 抽牌 / 降低霸凌者攻击 / 全体护盾与治疗
-- 可在已有库上重复执行（按 card_code 去重）

INSERT INTO `cards` (`id`, `card_code`, `card_name`, `dept_id`, `dept_type`, `cost`, `card_type`, `description`, `image_url`, `combo_card_id`, `is_unique`, `status`, `created_at`, `updated_at`)
SELECT 86, 'O-16', '信使', 3, 'public', 1, 'draw', '抽 2 张牌', '/images/cards/物流_信使.webp', NULL, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cards` WHERE `card_code` = 'O-16');

INSERT INTO `cards` (`id`, `card_code`, `card_name`, `dept_id`, `dept_type`, `cost`, `card_type`, `description`, `image_url`, `combo_card_id`, `is_unique`, `status`, `created_at`, `updated_at`)
SELECT 87, 'O-17', '告示官', 3, 'public', 1, 'support', '本回合霸凌者攻击 -2', '/images/cards/宣传_告示官.webp', NULL, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cards` WHERE `card_code` = 'O-17');

INSERT INTO `cards` (`id`, `card_code`, `card_name`, `dept_id`, `dept_type`, `cost`, `card_type`, `description`, `image_url`, `combo_card_id`, `is_unique`, `status`, `created_at`, `updated_at`)
SELECT 88, 'O-18', '鼓舞手', 3, 'public', 2, 'defend', '双方各获得 2 点防御', '/images/cards/活动_鼓舞手.webp', NULL, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cards` WHERE `card_code` = 'O-18');

INSERT INTO `cards` (`id`, `card_code`, `card_name`, `dept_id`, `dept_type`, `cost`, `card_type`, `description`, `image_url`, `combo_card_id`, `is_unique`, `status`, `created_at`, `updated_at`)
SELECT 89, 'O-19', '宫廷乐师', 3, 'public', 2, 'heal', '双方各恢复 2 点血量', '/images/cards/演艺_宫廷乐师.webp', NULL, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cards` WHERE `card_code` = 'O-19');

INSERT INTO `card_effects` (`card_id`, `effect_order`, `effect_scope`, `effect_type`, `trigger_timing`, `trigger_delay`, `remaining_triggers`, `stack_rule`, `duration_rounds`, `value`, `target_rule`, `extra_data`, `created_at`, `updated_at`)
SELECT c.id, 1, 'SELF', 'DRAW_CARDS', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'SELF', NULL, NOW(), NOW()
FROM `cards` c
WHERE c.card_code = 'O-16'
  AND NOT EXISTS (SELECT 1 FROM `card_effects` e WHERE e.card_id = c.id AND e.effect_type = 'DRAW_CARDS');

INSERT INTO `card_effects` (`card_id`, `effect_order`, `effect_scope`, `effect_type`, `trigger_timing`, `trigger_delay`, `remaining_triggers`, `stack_rule`, `duration_rounds`, `value`, `target_rule`, `extra_data`, `created_at`, `updated_at`)
SELECT c.id, 1, 'BOSS', 'REDUCE_BOSS_ATTACK', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'BOSS', NULL, NOW(), NOW()
FROM `cards` c
WHERE c.card_code = 'O-17'
  AND NOT EXISTS (SELECT 1 FROM `card_effects` e WHERE e.card_id = c.id AND e.effect_type = 'REDUCE_BOSS_ATTACK');

INSERT INTO `card_effects` (`card_id`, `effect_order`, `effect_scope`, `effect_type`, `trigger_timing`, `trigger_delay`, `remaining_triggers`, `stack_rule`, `duration_rounds`, `value`, `target_rule`, `extra_data`, `created_at`, `updated_at`)
SELECT c.id, 1, 'ALL_PLAYERS', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'ALL_PLAYERS', NULL, NOW(), NOW()
FROM `cards` c
WHERE c.card_code = 'O-18'
  AND NOT EXISTS (SELECT 1 FROM `card_effects` e WHERE e.card_id = c.id AND e.effect_type = 'ADD_SHIELD');

INSERT INTO `card_effects` (`card_id`, `effect_order`, `effect_scope`, `effect_type`, `trigger_timing`, `trigger_delay`, `remaining_triggers`, `stack_rule`, `duration_rounds`, `value`, `target_rule`, `extra_data`, `created_at`, `updated_at`)
SELECT c.id, 1, 'ALL_PLAYERS', 'HEAL_PLAYER', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'ALL_PLAYERS', NULL, NOW(), NOW()
FROM `cards` c
WHERE c.card_code = 'O-19'
  AND NOT EXISTS (SELECT 1 FROM `card_effects` e WHERE e.card_id = c.id AND e.effect_type = 'HEAL_PLAYER');
