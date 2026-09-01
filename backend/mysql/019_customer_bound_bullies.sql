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
  '专挑更弱的护卫下手。血量持平时打销售。',
  110, 5, 0, 1, 0,
  '{"customerCode":"CUSTOMER_KIND","pattern":"FOCUS_LOW_HP","chance":100,"shield":0,"bonusAttack":0,"catalogSummary":"每回合专打更弱的护卫。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_FOCUS_001');

INSERT INTO `bullies` (
  `bully_code`, `bully_name`, `description`, `hp`, `attack_power`, `defense_value`,
  `speed_value`, `rage_value`, `skill_data`, `status`
) SELECT
  'BULLY_SHIELD_001',
  '硬扛恶霸',
  '偶发在胸口撑起一层薄盾，打掉就没。',
  110, 5, 0, 1, 0,
  '{"customerCode":"CUSTOMER_TIMID","pattern":"ROUND_SHIELD","chance":40,"shield":4,"bonusAttack":0,"catalogSummary":"约四成回合胸口会多一层 4 点盾。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_SHIELD_001');

INSERT INTO `bullies` (
  `bully_code`, `bully_name`, `description`, `hp`, `attack_power`, `defense_value`,
  `speed_value`, `rage_value`, `skill_data`, `status`
) SELECT
  'BULLY_REVENGE_001',
  '针对恶霸',
  '仍打两人。约一半回合会盯本回合打得最疼的人加一刀。',
  110, 5, 0, 1, 0,
  '{"customerCode":"CUSTOMER_ANXIOUS","pattern":"FOCUS_TOP_DAMAGE","chance":50,"shield":0,"bonusAttack":2,"catalogSummary":"约一半回合会盯打得最疼的人加一刀。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_REVENGE_001');

INSERT INTO `bullies` (
  `bully_code`, `bully_name`, `description`, `hp`, `attack_power`, `defense_value`,
  `speed_value`, `rage_value`, `skill_data`, `status`
) SELECT
  'BULLY_PAIR_001',
  '不落单恶霸',
  '仍打两人。若两人都没掉血，约三成会在下一拍再抽一次半伤。',
  110, 5, 0, 1, 0,
  '{"customerCode":"CUSTOMER_WINDOW","pattern":"BOTH_HALF_SWING","chance":35,"shield":0,"bonusAttack":0,"catalogSummary":"两人都挡住时，约三成会在下一拍再抽一次。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_PAIR_001');

UPDATE `bullies`
SET `bully_name` = '点名恶霸',
    `description` = '专挑更弱的护卫下手。血量持平时打销售。',
    `hp` = 110, `attack_power` = 5, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_KIND","pattern":"FOCUS_LOW_HP","chance":100,"shield":0,"bonusAttack":0,"catalogSummary":"每回合专打更弱的护卫。"}'
WHERE `bully_code` = 'BULLY_FOCUS_001';

UPDATE `bullies`
SET `bully_name` = '硬扛恶霸',
    `description` = '偶发在胸口撑起一层薄盾，打掉就没。',
    `hp` = 110, `attack_power` = 5, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_TIMID","pattern":"ROUND_SHIELD","chance":40,"shield":4,"bonusAttack":0,"catalogSummary":"约四成回合胸口会多一层 4 点盾。"}'
WHERE `bully_code` = 'BULLY_SHIELD_001';

UPDATE `bullies`
SET `bully_name` = '针对恶霸',
    `description` = '仍打两人。约一半回合会盯本回合打得最疼的人加一刀。',
    `hp` = 110, `attack_power` = 5, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_ANXIOUS","pattern":"FOCUS_TOP_DAMAGE","chance":50,"shield":0,"bonusAttack":2,"catalogSummary":"约一半回合会盯打得最疼的人加一刀。"}'
WHERE `bully_code` = 'BULLY_REVENGE_001';

UPDATE `bullies`
SET `bully_name` = '不落单恶霸',
    `description` = '仍打两人。若两人都没掉血，约三成会在下一拍再抽一次半伤。',
    `hp` = 110, `attack_power` = 5, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_WINDOW","pattern":"BOTH_HALF_SWING","chance":35,"shield":0,"bonusAttack":0,"catalogSummary":"两人都挡住时，约三成会在下一拍再抽一次。"}'
WHERE `bully_code` = 'BULLY_PAIR_001';
