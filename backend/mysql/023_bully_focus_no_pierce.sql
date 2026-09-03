-- 点名不再破盾；按血量比例点名，持平随机。对战以 Java 为准。可重复执行。
UPDATE `bullies`
SET `description` = '专挑血更虚的护卫下手。血量比例持平时随机点名。护盾可以挡住。',
    `skill_data` = '{"customerCode":"CUSTOMER_KIND","pattern":"FOCUS_LOW_HP","chance":100,"shield":0,"bonusAttack":0,"catalogSummary":"专打血更虚的护卫；护盾可以挡住。"}'
WHERE `bully_code` = 'BULLY_FOCUS_001';
