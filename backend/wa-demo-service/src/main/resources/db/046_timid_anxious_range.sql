-- 胆小怕事加血 +3～5；焦虑难安加攻 +2～4。可重复执行。
UPDATE `customer_types`
SET
  `effect_type` = 'bully_hp_up',
  `effect_value` = 3,
  `description` = '容易紧张。效果触发时，霸凌者血量随机 +3～5（上限同步提高）。'
WHERE `customer_code` = 'CUSTOMER_TIMID';

UPDATE `customer_types`
SET
  `effect_type` = 'bully_attack_up',
  `effect_value` = 2,
  `description` = '情绪波动较大。效果触发时，本回合霸凌者攻击随机 +2～4。'
WHERE `customer_code` = 'CUSTOMER_ANXIOUS';
