-- 阔绰金客：出手阔绰，偶尔给一名存活护卫增加本回合调用机会。可重复执行。
INSERT INTO `customer_types` (
  `customer_code`, `customer_name`, `description`, `image_url`,
  `effect_type`, `effect_value`, `trigger_chance`, `selection_weight`,
  `status`, `sort_no`
) SELECT
  'CUSTOMER_WEALTHY',
  '阔绰金客',
  '出手极阔的金主，口袋里总有余钱。效果触发时随机给一名存活护卫本回合调用机会 +1。',
  '/images/customer/p5.webp',
  'player_action_up',
  1, 40, 10, 1, 5
WHERE NOT EXISTS (SELECT 1 FROM `customer_types` WHERE `customer_code` = 'CUSTOMER_WEALTHY');

UPDATE `customer_types`
SET
  `customer_name` = '阔绰金客',
  `description` = '出手极阔的金主，口袋里总有余钱。效果触发时随机给一名存活护卫本回合调用机会 +1。',
  `image_url` = '/images/customer/p5.webp',
  `effect_type` = 'player_action_up',
  `effect_value` = 1,
  `trigger_chance` = 40,
  `selection_weight` = 10,
  `status` = 1,
  `sort_no` = 5
WHERE `customer_code` = 'CUSTOMER_WEALTHY';
