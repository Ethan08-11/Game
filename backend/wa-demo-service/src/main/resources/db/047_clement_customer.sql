-- 爽快应客：好顾客。随和爽快，触发时随机给一名存活护卫抽 1 张牌。可重复执行。
INSERT INTO `customer_types` (
  `customer_code`, `customer_name`, `description`, `image_url`,
  `effect_type`, `effect_value`, `trigger_chance`, `selection_weight`,
  `status`, `sort_no`
) SELECT
  'CUSTOMER_CLEMENT',
  '爽快应客',
  '随和爽快，笑起来就说 OK 的新加坡建筑业大亨。效果触发时，随机给一名存活护卫抽 1 张牌。',
  '/images/customer/p7.webp',
  'player_draw',
  1, 50, 10, 1, 7
WHERE NOT EXISTS (SELECT 1 FROM `customer_types` WHERE `customer_code` = 'CUSTOMER_CLEMENT');

UPDATE `customer_types`
SET
  `customer_name` = '爽快应客',
  `description` = '随和爽快，笑起来就说 OK 的新加坡建筑业大亨。效果触发时，随机给一名存活护卫抽 1 张牌。',
  `image_url` = '/images/customer/p7.webp',
  `effect_type` = 'player_draw',
  `effect_value` = 1,
  `trigger_chance` = 50,
  `selection_weight` = 10,
  `status` = 1,
  `sort_no` = 7
WHERE `customer_code` = 'CUSTOMER_CLEMENT';
