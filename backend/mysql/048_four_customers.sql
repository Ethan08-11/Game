-- 白问刺客、迟来金客、红枣双客、笑后苛客。可重复执行。
INSERT INTO `customer_types` (
  `customer_code`, `customer_name`, `description`, `image_url`,
  `effect_type`, `effect_value`, `trigger_chance`, `selection_weight`,
  `status`, `sort_no`
) SELECT
  'CUSTOMER_HOTPOT',
  '白问刺客',
  '开火锅店，问得很细却不下单，还拿报价去别处成交。效果触发时，随机一名存活护卫弃掉手里 1 张牌。',
  '/images/customer/p8.webp',
  'player_discard',
  1, 50, 10, 1, 8
WHERE NOT EXISTS (SELECT 1 FROM `customer_types` WHERE `customer_code` = 'CUSTOMER_HOTPOT');

UPDATE `customer_types`
SET
  `customer_name` = '白问刺客',
  `description` = '开火锅店，问得很细却不下单，还拿报价去别处成交。效果触发时，随机一名存活护卫弃掉手里 1 张牌。',
  `image_url` = '/images/customer/p8.webp',
  `effect_type` = 'player_discard',
  `effect_value` = 1,
  `trigger_chance` = 50,
  `selection_weight` = 10,
  `status` = 1,
  `sort_no` = 8
WHERE `customer_code` = 'CUSTOMER_HOTPOT';

INSERT INTO `customer_types` (
  `customer_code`, `customer_name`, `description`, `image_url`,
  `effect_type`, `effect_value`, `trigger_chance`, `selection_weight`,
  `status`, `sort_no`
) SELECT
  'CUSTOMER_DELAYED',
  '迟来金客',
  '问过报价，过一阵突然下单转账。效果触发时，下回合开始给两名存活护卫各 4 点护盾。',
  '/images/customer/p9.webp',
  'player_shield_next',
  4, 50, 10, 1, 9
WHERE NOT EXISTS (SELECT 1 FROM `customer_types` WHERE `customer_code` = 'CUSTOMER_DELAYED');

UPDATE `customer_types`
SET
  `customer_name` = '迟来金客',
  `description` = '问过报价，过一阵突然下单转账。效果触发时，下回合开始给两名存活护卫各 4 点护盾。',
  `image_url` = '/images/customer/p9.webp',
  `effect_type` = 'player_shield_next',
  `effect_value` = 4,
  `trigger_chance` = 50,
  `selection_weight` = 10,
  `status` = 1,
  `sort_no` = 9
WHERE `customer_code` = 'CUSTOMER_DELAYED';

INSERT INTO `customer_types` (
  `customer_code`, `customer_name`, `description`, `image_url`,
  `effect_type`, `effect_value`, `trigger_chance`, `selection_weight`,
  `status`, `sort_no`
) SELECT
  'CUSTOMER_JUJUBE',
  '红枣双客',
  '新加坡红枣业大亨。正常局每回合一半走好面、一半走坏面。好面清掉霸凌者当前护盾。坏面让销售本回合的下一张牌没有效果；场上没有销售时落空。针对局只有坏面。',
  '/images/customer/p10.webp',
  'dual_jujube',
  0, 50, 10, 1, 10
WHERE NOT EXISTS (SELECT 1 FROM `customer_types` WHERE `customer_code` = 'CUSTOMER_JUJUBE');

UPDATE `customer_types`
SET
  `customer_name` = '红枣双客',
  `description` = '新加坡红枣业大亨。正常局每回合一半走好面、一半走坏面。好面清掉霸凌者当前护盾。坏面让销售本回合的下一张牌没有效果；场上没有销售时落空。针对局只有坏面。',
  `image_url` = '/images/customer/p10.webp',
  `effect_type` = 'dual_jujube',
  `effect_value` = 0,
  `trigger_chance` = 50,
  `selection_weight` = 10,
  `status` = 1,
  `sort_no` = 10
WHERE `customer_code` = 'CUSTOMER_JUJUBE';

INSERT INTO `customer_types` (
  `customer_code`, `customer_name`, `description`, `image_url`,
  `effect_type`, `effect_value`, `trigger_chance`, `selection_weight`,
  `status`, `sort_no`
) SELECT
  'CUSTOMER_PHONE',
  '笑后苛客',
  '电话里笑嘻嘻，产品出问题后变刻薄。正常局每回合一半走好面、一半走坏面。好面让每名存活护卫的下一张防御牌护盾 ×2。坏面让采购本回合打出的防御牌护盾为 0；场上没有采购时落空。针对局只有坏面。',
  '/images/customer/p11.webp',
  'dual_phone',
  0, 50, 10, 1, 11
WHERE NOT EXISTS (SELECT 1 FROM `customer_types` WHERE `customer_code` = 'CUSTOMER_PHONE');

UPDATE `customer_types`
SET
  `customer_name` = '笑后苛客',
  `description` = '电话里笑嘻嘻，产品出问题后变刻薄。正常局每回合一半走好面、一半走坏面。好面让每名存活护卫的下一张防御牌护盾 ×2。坏面让采购本回合打出的防御牌护盾为 0；场上没有采购时落空。针对局只有坏面。',
  `image_url` = '/images/customer/p11.webp',
  `effect_type` = 'dual_phone',
  `effect_value` = 0,
  `trigger_chance` = 50,
  `selection_weight` = 10,
  `status` = 1,
  `sort_no` = 11
WHERE `customer_code` = 'CUSTOMER_PHONE';
