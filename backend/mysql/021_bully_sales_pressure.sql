-- 按销售人数缩放后的霸凌者文案与常规组中位攻击。掷点以 Java 为准。可重复执行。
UPDATE `bullies`
SET `bully_name` = '点名恶霸',
    `description` = '专挑更弱的护卫下手。血量持平时打销售。盾挡完仍会漏 8 点。',
    `hp` = 180, `attack_power` = 22, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_KIND","pattern":"FOCUS_LOW_HP","chance":100,"shield":0,"bonusAttack":0,"catalogSummary":"专打更弱的护卫；盾挡完仍会漏 8 点。"}'
WHERE `bully_code` = 'BULLY_FOCUS_001';

UPDATE `bullies`
SET `bully_name` = '硬扛恶霸',
    `description` = '约一成五回合胸口多 14 点盾，这回合出手也变轻。',
    `hp` = 180, `attack_power` = 22, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_TIMID","pattern":"ROUND_SHIELD","chance":15,"shield":14,"bonusAttack":0,"catalogSummary":"约一成五回合胸口多 14 点盾，这回合出手也变轻。"}'
WHERE `bully_code` = 'BULLY_SHIELD_001';

UPDATE `bullies`
SET `bully_name` = '针对恶霸',
    `description` = '仍打两人。约八成五回合盯打得最疼的人多挨 6 点。',
    `hp` = 180, `attack_power` = 22, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_ANXIOUS","pattern":"FOCUS_TOP_DAMAGE","chance":85,"shield":0,"bonusAttack":6,"catalogSummary":"约八成五回合会盯打得最疼的人多挨 6 点。"}'
WHERE `bully_code` = 'BULLY_REVENGE_001';

UPDATE `bullies`
SET `bully_name` = '不落单恶霸',
    `description` = '仍打两人。两人都几乎挡住时，下一拍再抽半刀。',
    `hp` = 180, `attack_power` = 22, `defense_value` = 0, `status` = 1,
    `skill_data` = '{"customerCode":"CUSTOMER_WINDOW","pattern":"BOTH_HALF_SWING","chance":0,"shield":0,"bonusAttack":0,"catalogSummary":"两人都几乎挡住时，下一拍会再抽半刀。"}'
WHERE `bully_code` = 'BULLY_PAIR_001';
