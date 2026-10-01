-- 刻薄尖客：非常刻薄，偶尔扣一名存活护卫本回合 1 点调用机会并扣 3 点血。可重复执行。
INSERT INTO `customer_types` (
  `customer_code`, `customer_name`, `description`, `image_url`,
  `effect_type`, `effect_value`, `trigger_chance`, `selection_weight`,
  `status`, `sort_no`
) SELECT
  'CUSTOMER_HARSH',
  '刻薄尖客',
  '戴着眼镜、目光很尖的刻薄客。效果触发时随机让一名存活护卫本回合少 1 点调用机会，并刻薄到扣 3 点血。',
  '/images/customer/p6.webp',
  'player_action_hp_down',
  3, 40, 10, 1, 6
WHERE NOT EXISTS (SELECT 1 FROM `customer_types` WHERE `customer_code` = 'CUSTOMER_HARSH');

UPDATE `customer_types`
SET
  `customer_name` = '刻薄尖客',
  `description` = '戴着眼镜、目光很尖的刻薄客。效果触发时随机让一名存活护卫本回合少 1 点调用机会，并刻薄到扣 3 点血。',
  `image_url` = '/images/customer/p6.webp',
  `effect_type` = 'player_action_hp_down',
  `effect_value` = 3,
  `trigger_chance` = 40,
  `selection_weight` = 10,
  `status` = 1,
  `sort_no` = 6
WHERE `customer_code` = 'CUSTOMER_HARSH';
