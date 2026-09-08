-- 只保留成员专属卡（销售 S-35～S-43、采购 P-26～P-37）、公共部雇佣兵（M-01～M-06）和物流骑士（LG-01～LG-04）。
-- 其余公共部、中立（路人部）卡全部停用：图鉴不展示、对局不抽。卡面文件保留，可再打开。
-- 公共部部门（DEPT_PUBLIC）保留；中立/路人部部门停用。
-- 必须在 016、017 之后执行；雇佣兵由 028 写入，物流骑士由 031 写入。
UPDATE `cards`
SET `status` = 0
WHERE NOT (
    `card_code` IN (
        'S-35', 'S-36', 'S-37', 'S-38', 'S-39', 'S-40', 'S-41', 'S-42', 'S-43',
        'P-26', 'P-27', 'P-28', 'P-29', 'P-30', 'P-31', 'P-32', 'P-33', 'P-34', 'P-35', 'P-36', 'P-37',
        'M-01', 'M-02', 'M-03', 'M-04', 'M-05', 'M-06',
        'LG-01', 'LG-02', 'LG-03', 'LG-04'
    )
);

UPDATE `card_depts`
SET `status` = 1
WHERE `dept_code` = 'DEPT_PUBLIC';

UPDATE `card_depts`
SET `status` = 0
WHERE `dept_code` = 'DEPT_PASSERBY';
