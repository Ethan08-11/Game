-- 闲逛双客：一对和气但不落单的顾客，登场权重低于其余顾客。可重复执行。
-- 效果为给我方加血（player_hp_up），不与其他顾客的攻/血效果重复。
INSERT INTO `customer_types` (
  `customer_code`, `customer_name`, `description`, `image_url`,
  `effect_type`, `effect_value`, `trigger_chance`, `selection_weight`,
  `status`, `sort_no`
) SELECT
  'CUSTOMER_WINDOW',
  '闲逛双客',
  '结伴闲逛却从不落单，偶尔会给两名护卫恢复血值。',
  '/images/customer/p4.webp',
  'player_hp_up',
  2, 20, 10, 1, 4
WHERE NOT EXISTS (SELECT 1 FROM `customer_types` WHERE `customer_code` = 'CUSTOMER_WINDOW');

UPDATE `customer_types`
SET
  `customer_name` = '闲逛双客',
  `description` = '结伴闲逛却从不落单，偶尔会给两名护卫恢复血值。',
  `image_url` = '/images/customer/p4.webp',
  `effect_type` = 'player_hp_up',
  `effect_value` = 2,
  `trigger_chance` = 20,
  `selection_weight` = 10,
  `status` = 1,
  `sort_no` = 4
WHERE `customer_code` = 'CUSTOMER_WINDOW';
