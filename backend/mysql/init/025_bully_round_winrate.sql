-- 对局拉长：图鉴血量/基础攻击与混组一致。双销售血量由 Java 另取 250。可重复执行。
UPDATE `bullies`
SET `hp` = 140,
    `attack_power` = 18
WHERE `bully_code` = 'BULLY_SHIELD_001';
