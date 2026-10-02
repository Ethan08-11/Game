-- 全员触发 50%；胆小怕事改回加血 +3；焦虑难安攻击 +2。可重复执行。
UPDATE `customer_types`
SET `trigger_chance` = 50
WHERE `status` = 1;

UPDATE `customer_types`
SET
  `effect_type` = 'bully_hp_up',
  `effect_value` = 3,
  `trigger_chance` = 50,
  `description` = '容易紧张，需要额外保护。效果触发时霸凌者血量 +3（上限同步提高）。'
WHERE `customer_code` = 'CUSTOMER_TIMID';

UPDATE `customer_types`
SET
  `effect_type` = 'bully_attack_up',
  `effect_value` = 2,
  `trigger_chance` = 50
WHERE `customer_code` = 'CUSTOMER_ANXIOUS';
