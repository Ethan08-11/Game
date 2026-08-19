-- IT 部样例卡：图鉴可见，便于对照改名/改效果。可重复执行。
INSERT INTO `cards` (`id`,`card_code`,`card_name`,`dept_id`,`dept_type`,`cost`,`card_type`,`description`,`image_url`,`combo_card_id`,`is_unique`,`require_unlock`,`status`,`created_at`,`updated_at`)
SELECT 147, 'T-01', '系统术士', 3, 'public', 2, 'attack', '立即造成 2 点伤害，并抽 1 张牌', '/images/cards/技术_机械工匠.webp', NULL, 0, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cards` WHERE `card_code` = 'T-01');

INSERT INTO `card_effects` (`card_id`,`effect_order`,`effect_scope`,`effect_type`,`trigger_timing`,`trigger_delay`,`remaining_triggers`,`stack_rule`,`duration_rounds`,`value`,`target_rule`,`extra_data`,`created_at`,`updated_at`)
SELECT c.id, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'BOSS', NULL, NOW(), NOW()
FROM `cards` c WHERE c.card_code = 'T-01'
AND NOT EXISTS (SELECT 1 FROM `card_effects` e WHERE e.card_id = c.id AND e.effect_order = 1 AND e.effect_type = 'DAMAGE_BOSS');

INSERT INTO `card_effects` (`card_id`,`effect_order`,`effect_scope`,`effect_type`,`trigger_timing`,`trigger_delay`,`remaining_triggers`,`stack_rule`,`duration_rounds`,`value`,`target_rule`,`extra_data`,`created_at`,`updated_at`)
SELECT c.id, 2, 'SELF', 'DRAW_CARDS', 'IMMEDIATE', 0, 1, 'STACK', 0, 1, 'SELF', NULL, NOW(), NOW()
FROM `cards` c WHERE c.card_code = 'T-01'
AND NOT EXISTS (SELECT 1 FROM `card_effects` e WHERE e.card_id = c.id AND e.effect_order = 2 AND e.effect_type = 'DRAW_CARDS');

ALTER TABLE `cards` AUTO_INCREMENT = 148;
