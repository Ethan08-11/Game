-- 混组/双采购霸凌者血量 180。双销售血量仍由 Java 取 250。可重复执行。
UPDATE `bullies`
SET `hp` = 180
WHERE `bully_code` = 'BULLY_SHIELD_001';
