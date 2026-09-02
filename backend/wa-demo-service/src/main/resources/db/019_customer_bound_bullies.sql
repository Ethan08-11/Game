-- 四名顾客各绑定一名霸凌者。可重复执行。
UPDATE `bullies`
SET `status` = 0,
    `description` = '旧通用模板，已停用。对局按顾客绑定专属霸凌者。'
WHERE `bully_code` = 'BULLY_BASIC_001';

INSERT INTO `bullies` (
  `bully_code`, `bully_name`, `description`, `hp`, `attack_power`, `defense_value`,
  `speed_value`, `rage_value`, `skill_data`, `status`
) SELECT
  'BULLY_FOCUS_001',
  '点名恶霸',
  '专挑更弱的护卫下手。血量持平时打销售。盾挡完仍会漏 6 点。',
  140, 19, 0, 1, 0,
  '{"customerCode":"CUSTOMER_KIND","pattern":"FOCUS_LOW_HP","chance":100,"shield":0,"bonusAttack":0,"catalogSummary":"专打更弱的护卫；盾挡完仍会漏 6 点。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_FOCUS_001');

INSERT INTO `bullies` (
  `bully_code`, `bully_name`, `description`, `hp`, `attack_power`, `defense_value`,
  `speed_value`, `rage_value`, `skill_data`, `status`
) SELECT
  'BULLY_SHIELD_001',
  '硬扛恶霸',
  '约两成回合胸口多 12 点盾，这回合出手也变轻。',
  140, 19, 0, 1, 0,
  '{"customerCode":"CUSTOMER_TIMID","pattern":"ROUND_SHIELD","chance":20,"shield":12,"bonusAttack":0,"catalogSummary":"约两成回合胸口多 12 点盾，这回合出手也变轻。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_SHIELD_001');

INSERT INTO `bullies` (
  `bully_code`, `bully_name`, `description`, `hp`, `attack_power`, `defense_value`,
  `speed_value`, `rage_value`, `skill_data`, `status`
) SELECT
  'BULLY_REVENGE_001',
  '针对恶霸',
  '仍打两人。约八成回合盯打得最疼的人多挨 5 点。',
  140, 19, 0, 1, 0,
  '{"customerCode":"CUSTOMER_ANXIOUS","pattern":"FOCUS_TOP_DAMAGE","chance":80,"shield":0,"bonusAttack":5,"catalogSummary":"约八成回合会盯打得最疼的人多挨 5 点。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_REVENGE_001');

INSERT INTO `bullies` (
  `bully_code`, `bully_name`, `description`, `hp`, `attack_power`, `defense_value`,
  `speed_value`, `rage_value`, `skill_data`, `status`
) SELECT
  'BULLY_PAIR_001',
  '不落单恶霸',
  '仍打两人。两人都几乎挡住时，下一拍再抽半刀。',
  140, 19, 0, 1, 0,
  '{"customerCode":"CUSTOMER_WINDOW","pattern":"BOTH_HALF_SWING","chance":0,"shield":0,"bonusAttack":0,"catalogSummary":"两人都几乎挡住时，下一拍会再抽半刀。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_PAIR_001');

UPDATE `bullies`
SET `bully_name` = '点名恶霸',
    `description` = '专挑更弱的护卫下手。血量持平时打销售。盾挡完仍会漏 6 点。',
    `hp` = 140, `attack_power` = 19, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_KIND","pattern":"FOCUS_LOW_HP","chance":100,"shield":0,"bonusAttack":0,"catalogSummary":"专打更弱的护卫；盾挡完仍会漏 6 点。"}'
WHERE `bully_code` = 'BULLY_FOCUS_001';

UPDATE `bullies`
SET `bully_name` = '硬扛恶霸',
    `description` = '约两成回合胸口多 12 点盾，这回合出手也变轻。',
    `hp` = 140, `attack_power` = 19, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_TIMID","pattern":"ROUND_SHIELD","chance":20,"shield":12,"bonusAttack":0,"catalogSummary":"约两成回合胸口多 12 点盾，这回合出手也变轻。"}'
WHERE `bully_code` = 'BULLY_SHIELD_001';

UPDATE `bullies`
SET `bully_name` = '针对恶霸',
    `description` = '仍打两人。约八成回合盯打得最疼的人多挨 5 点。',
    `hp` = 140, `attack_power` = 19, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_ANXIOUS","pattern":"FOCUS_TOP_DAMAGE","chance":80,"shield":0,"bonusAttack":5,"catalogSummary":"约八成回合会盯打得最疼的人多挨 5 点。"}'
WHERE `bully_code` = 'BULLY_REVENGE_001';

UPDATE `bullies`
SET `bully_name` = '不落单恶霸',
    `description` = '仍打两人。两人都几乎挡住时，下一拍再抽半刀。',
    `hp` = 140, `attack_power` = 19, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_WINDOW","pattern":"BOTH_HALF_SWING","chance":0,"shield":0,"bonusAttack":0,"catalogSummary":"两人都几乎挡住时，下一拍会再抽半刀。"}'
WHERE `bully_code` = 'BULLY_PAIR_001';
