-- 所有顾客共用硬扛恶霸。点名 / 针对 / 不落单停用。可重复执行。
UPDATE `bullies`
SET `status` = 0,
    `description` = '已停用。当前对局所有顾客共用硬扛恶霸。'
WHERE `bully_code` IN ('BULLY_BASIC_001', 'BULLY_FOCUS_001', 'BULLY_REVENGE_001', 'BULLY_PAIR_001');

INSERT INTO `bullies` (
  `bully_code`, `bully_name`, `description`, `hp`, `attack_power`, `defense_value`,
  `speed_value`, `rage_value`, `skill_data`, `status`
) SELECT
  'BULLY_SHIELD_001',
  '硬扛恶霸',
  '所有顾客共用。约一成五回合胸口多 14 点盾，这回合出手也变轻。',
  150, 24, 0, 1, 0,
  '{"customerCode":"*","pattern":"ROUND_SHIELD","chance":15,"shield":14,"bonusAttack":0,"catalogSummary":"约一成五回合胸口多 14 点盾，这回合出手也变轻。"}',
  1
WHERE NOT EXISTS (SELECT 1 FROM `bullies` WHERE `bully_code` = 'BULLY_SHIELD_001');

UPDATE `bullies`
SET `bully_name` = '硬扛恶霸',
    `description` = '所有顾客共用。约一成五回合胸口多 14 点盾，这回合出手也变轻。',
    `hp` = 150,
    `attack_power` = 24,
    `defense_value` = 0,
    `status` = 1,
    `skill_data` = '{"customerCode":"*","pattern":"ROUND_SHIELD","chance":15,"shield":14,"bonusAttack":0,"catalogSummary":"约一成五回合胸口多 14 点盾，这回合出手也变轻。"}'
WHERE `bully_code` = 'BULLY_SHIELD_001';
