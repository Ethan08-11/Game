-- 胆小怕事加血 2→4；焦虑难安加攻 2→3。可重复执行。
UPDATE `customer_types`
SET `effect_value` = 4
WHERE `customer_code` = 'CUSTOMER_TIMID';

UPDATE `customer_types`
SET `effect_value` = 3
WHERE `customer_code` = 'CUSTOMER_ANXIOUS';
