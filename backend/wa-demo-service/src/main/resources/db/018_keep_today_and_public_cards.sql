-- 只保留今天新生成的成员专属卡，以及公共部（public/tech/it）卡。
-- 其余卡 status=0：图鉴不展示、对局不抽。卡面文件保留，可再打开。
-- 必须在 016、017 之后执行。
UPDATE `cards`
SET `status` = 0
WHERE NOT (
    LOWER(IFNULL(`dept_type`, '')) IN ('public', 'tech', 'it')
    OR `card_code` IN (
        'S-35', 'S-36', 'S-37', 'S-38', 'S-39', 'S-40', 'S-41', 'S-42', 'S-43',
        'P-26', 'P-27', 'P-28', 'P-29', 'P-30', 'P-31', 'P-32', 'P-33', 'P-34', 'P-35', 'P-36', 'P-37'
    )
);
