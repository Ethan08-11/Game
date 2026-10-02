-- 胆小怕事：加血改为本回合霸凌者攻击 +2。可重复执行。
UPDATE `customer_types`
SET
  `effect_type` = 'bully_attack_up',
  `effect_value` = 2,
  `description` = '容易紧张，需要额外保护。效果触发时本回合霸凌者攻击 +2。'
WHERE `customer_code` = 'CUSTOMER_TIMID';
