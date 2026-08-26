-- 系统术士（T-01）与机械工匠共用同一张立绘，图鉴重复。
-- 停用 T-01：status=0，图鉴和对局都抽不到。保留 id 147 不复用。机械工匠卡面保留。
UPDATE `cards` SET `status` = 0 WHERE `card_code` = 'T-01';
