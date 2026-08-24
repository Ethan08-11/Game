# -*- coding: utf-8 -*-
"""Generate 004_collectible_cards.sql from the approved effect table."""
from pathlib import Path

OUT = Path(__file__).with_name("004_collectible_cards.sql")

# (code, name, prefix, cost, card_type, dept_type, dept_id, desc, effects)
# effect: (order, scope, type, timing, delay, value, target)
IM, RS = "IMMEDIATE", "ROUND_START"


def dmg(v, order=1, timing=IM, delay=0):
    return (order, "BOSS", "DAMAGE_BOSS", timing, delay, v, "BOSS")


def sh(v, order=1, scope="ANY_PLAYER", timing=IM, delay=0):
    return (order, scope, "ADD_SHIELD", timing, delay, v, scope)


def heal(v, order=1, scope="ANY_PLAYER", timing=IM, delay=0):
    return (order, scope, "HEAL_PLAYER", timing, delay, v, scope)


def draw(v, order=1, timing=IM, delay=0):
    return (order, "SELF", "DRAW_CARDS", timing, delay, v, "SELF")


def reduce_atk(v, order=1):
    return (order, "BOSS", "REDUCE_BOSS_ATTACK", IM, 0, v, "BOSS")


def ap(v, order=1, scope="SELF", timing=IM, delay=0):
    return (order, scope, "ADD_ACTION_POINTS", timing, delay, v, scope)


def multiply(order=1):
    return (order, "SELF", "MULTIPLY_NEXT_CARD", "NEXT_CARD", 0, 2, "SELF")


cards = [
    # sales
    ("S-07", "摊贩", "销售", 0, "attack", "sales", 1, "立即造成 1 点伤害，并为一名玩家增加 1 点防御", [dmg(1), sh(1, 2)]),
    ("S-08", "货郎", "销售", 0, "draw", "sales", 1, "抽 1 张牌", [draw(1)]),
    ("S-09", "卖艺货郎", "销售", 0, "attack", "sales", 1, "立即造成 1 点伤害，并抽 1 张牌", [dmg(1), draw(1, 2)]),
    ("S-10", "酒娘", "销售", 1, "attack", "sales", 1, "立即造成 2 点伤害，并恢复自己 1 点血量", [dmg(2), heal(1, 2, "SELF")]),
    ("S-11", "面包师", "销售", 1, "attack", "sales", 1, "本回合造成 1 点伤害，下回合再造成 2 点", [dmg(1), dmg(2, 2, RS, 1)]),
    ("S-12", "珠串商", "销售", 1, "attack", "sales", 1, "立即造成 2 点伤害，并为一名玩家增加 1 点防御", [dmg(2), sh(1, 2)]),
    ("S-13", "香囊商", "销售", 1, "attack", "sales", 1, "立即造成 2 点伤害，并恢复一名玩家 1 点血量", [dmg(2), heal(1, 2)]),
    ("S-14", "绒帽商", "销售", 1, "support", "sales", 1, "立即造成 1 点伤害，本回合霸凌者攻击 -1", [dmg(1), reduce_atk(1, 2)]),
    ("S-15", "酒商", "销售", 2, "attack", "sales", 1, "立即造成 3 点伤害，并抽 1 张牌", [dmg(3), draw(1, 2)]),
    ("S-16", "衡器商", "销售", 2, "support", "sales", 1, "立即造成 2 点伤害，本回合霸凌者攻击 -2", [dmg(2), reduce_atk(2, 2)]),
    ("S-17", "布商", "销售", 2, "attack", "sales", 1, "本回合造成 2 点伤害，下回合再造成 3 点", [dmg(2), dmg(3, 2, RS, 1)]),
    ("S-18", "行商", "销售", 2, "attack", "sales", 1, "立即造成 2 点伤害，并抽 1 张牌", [dmg(2), draw(1, 2)]),
    ("S-19", "杂货商", "销售", 2, "attack", "sales", 1, "立即造成 2 点伤害，双方各获得 1 点防御", [dmg(2), sh(1, 2, "ALL_PLAYERS")]),
    ("S-20", "花边商", "销售", 2, "attack", "sales", 1, "立即造成 3 点伤害，下回合再造成 1 点", [dmg(3), dmg(1, 2, RS, 1)]),
    ("S-21", "公会会长", "销售", 3, "attack", "sales", 1, "立即造成 4 点伤害，双方各获得 1 点防御", [dmg(4), sh(1, 2, "ALL_PLAYERS")]),
    ("S-22", "香料商", "销售", 3, "attack", "sales", 1, "本回合造成 3 点伤害，下回合再造成 3 点", [dmg(3), dmg(3, 2, RS, 1)]),
    ("S-23", "旅店掌柜", "销售", 3, "attack", "sales", 1, "立即造成 2 点伤害，双方各获得 2 点防御", [dmg(2), sh(2, 2, "ALL_PLAYERS")]),
    ("S-24", "绸缎商", "销售", 3, "attack", "sales", 1, "立即造成 4 点伤害，并抽 1 张牌", [dmg(4), draw(1, 2)]),
    # purchase
    ("P-05", "掮客", "采购", 0, "defend", "purchase", 2, "为自己增加 2 点防御", [sh(2, 1, "SELF")]),
    ("P-06", "采办官", "采购", 0, "defend", "purchase", 2, "本回合增加 1 点防御，下回合再增加 1 点", [sh(1), sh(1, 2, "ANY_PLAYER", RS, 1)]),
    ("P-07", "仓储管事", "采购", 1, "defend", "purchase", 2, "本回合增加 2 点防御，下回合再增加 1 点", [sh(2), sh(1, 2, "ANY_PLAYER", RS, 1)]),
    ("P-08", "园圃办货", "采购", 1, "defend", "purchase", 2, "为一名玩家增加 2 点防御，并恢复 1 点血量", [sh(2), heal(1, 2)]),
    ("P-09", "丝线采买", "采购", 1, "draw", "purchase", 2, "抽 1 张牌，并为一名玩家增加 1 点防御", [draw(1), sh(1, 2)]),
    ("P-10", "账册核验", "采购", 1, "support", "purchase", 2, "为一名玩家增加 2 点防御，本回合霸凌者攻击 -1", [sh(2), reduce_atk(1, 2)]),
    ("P-11", "粮秣官", "采购", 2, "defend", "purchase", 2, "为一名玩家增加 2 点防御，并恢复 2 点血量", [sh(2), heal(2, 2)]),
    ("P-12", "暗市办货", "采购", 2, "defend", "purchase", 2, "为自己增加 4 点防御", [sh(4, 1, "SELF")]),
    ("P-13", "书坊采买", "采购", 2, "defend", "purchase", 2, "本回合增加 3 点防御，下回合再增加 1 点", [sh(3), sh(1, 2, "ANY_PLAYER", RS, 1)]),
    ("P-14", "学府采办", "采购", 2, "draw", "purchase", 2, "为一名玩家增加 2 点防御，并抽 1 张牌", [sh(2), draw(1, 2)]),
    ("P-15", "珍宝估价", "采购", 2, "defend", "purchase", 2, "为一名玩家增加 3 点防御，并对霸凌者造成 1 点伤害", [sh(3), dmg(1, 2)]),
    ("P-16", "军需官", "采购", 3, "defend", "purchase", 2, "双方各获得 3 点防御", [sh(3, 1, "ALL_PLAYERS")]),
    ("P-17", "王室采办", "采购", 3, "defend", "purchase", 2, "本回合增加 4 点防御，下回合再增加 2 点", [sh(4), sh(2, 2, "ANY_PLAYER", RS, 1)]),
    ("P-18", "贡银采办", "采购", 3, "defend", "purchase", 2, "双方各获得 2 点防御，并抽 1 张牌", [sh(2, 1, "ALL_PLAYERS"), draw(1, 2)]),
    # design / others as public
    ("O-20", "绣娘", "设计", 1, "attack", "public", 3, "立即造成 2 点伤害，下回合为一名玩家增加 1 点防御", [dmg(2), sh(1, 2, "ANY_PLAYER", RS, 1)]),
    ("O-21", "纹章师", "设计", 1, "defend", "public", 3, "为一名玩家增加 2 点防御，并对霸凌者造成 1 点伤害", [sh(2), dmg(1, 2)]),
    ("O-22", "泥金师", "设计", 2, "attack", "public", 3, "立即造成 3 点伤害，并恢复一名玩家 1 点血量", [dmg(3), heal(1, 2)]),
    ("O-23", "营造师", "设计", 2, "defend", "public", 3, "本回合增加 1 点防御，下回合再增加 3 点", [sh(1), sh(3, 2, "ANY_PLAYER", RS, 1)]),
    ("O-24", "舆图师", "设计", 2, "draw", "public", 3, "抽 1 张牌，下回合再抽 1 张", [draw(1), draw(1, 2, RS, 1)]),
    ("O-25", "果贩", "市场", 0, "attack", "public", 3, "立即造成 1 点伤害，并恢复一名玩家 1 点血量", [dmg(1), heal(1, 2)]),
    ("O-26", "宫廷抄写员", "文员", 0, "attack", "public", 3, "立即造成 1 点伤害，下回合抽 1 张牌", [dmg(1), draw(1, 2, RS, 1)]),
    ("O-27", "守门人", "安保", 0, "defend", "public", 3, "双方各获得 1 点防御", [sh(1, 1, "ALL_PLAYERS")]),
    ("O-28", "学徒", "行政", 0, "draw", "public", 3, "下回合抽 1 张牌", [draw(1, 1, RS, 1)]),
    ("O-29", "花使", "礼仪", 1, "attack", "public", 3, "立即造成 1 点伤害，双方各恢复 1 点血量", [dmg(1), heal(1, 2, "ALL_PLAYERS")]),
    ("O-30", "草药师", "医疗", 1, "heal", "public", 3, "恢复一名玩家 2 点血量，并为一名玩家增加 1 点防御", [heal(2), sh(1, 2)]),
    ("O-31", "司膳", "餐饮", 1, "heal", "public", 3, "本回合恢复 1 点，下回合再恢复 2 点", [heal(1), heal(2, 2, "ANY_PLAYER", RS, 1)]),
    ("O-32", "牧羊女", "农业", 1, "defend", "public", 3, "双方各获得 1 点防御，并恢复一名玩家 1 点血量", [sh(1, 1, "ALL_PLAYERS"), heal(1, 2)]),
    ("O-33", "捕鼠师", "仓储", 1, "attack", "public", 3, "立即造成 2 点伤害，并为自己增加 1 点防御", [dmg(2), sh(1, 2, "SELF")]),
    ("O-34", "藏书吏", "行政", 1, "draw", "public", 3, "抽 1 张牌，下回合对霸凌者造成 1 点伤害", [draw(1), dmg(1, 2, RS, 1)]),
    ("O-35", "弓箭手", "军事", 2, "attack", "public", 3, "本回合造成 1 点伤害，下回合再造成 4 点", [dmg(1), dmg(4, 2, RS, 1)]),
    ("O-36", "厨子", "餐饮", 2, "heal", "public", 3, "恢复一名玩家 3 点血量，并抽 1 张牌", [heal(3), draw(1, 2)]),
    ("O-37", "渔妇", "渔业", 2, "heal", "public", 3, "本回合恢复 2 点，下回合再恢复 2 点", [heal(2), heal(2, 2, "ANY_PLAYER", RS, 1)]),
    ("O-38", "织工", "生产", 2, "defend", "public", 3, "双方各获得 1 点防御，下回合再各获得 1 点", [sh(1, 1, "ALL_PLAYERS"), sh(1, 2, "ALL_PLAYERS", RS, 1)]),
    ("O-39", "染匠", "生产", 2, "attack", "public", 3, "下回合造成 4 点伤害", [dmg(4, 1, RS, 1)]),
    ("O-40", "机械工匠", "技术", 2, "attack", "public", 3, "立即造成 2 点伤害，并为一名玩家增加 2 点防御", [dmg(2), sh(2, 2)]),
    ("O-41", "学者", "教育", 2, "heal", "public", 3, "恢复一名玩家 2 点血量，并抽 1 张牌", [heal(2), draw(1, 2)]),
    ("O-42", "珠宝商", "零售", 3, "attack", "public", 3, "立即造成 5 点伤害，并为自己增加 1 点防御", [dmg(5), sh(1, 2, "SELF")]),
    ("O-43", "骑士", "安保", 3, "defend", "public", 3, "立即造成 3 点伤害，并为自己增加 3 点防御", [dmg(3), sh(3, 2, "SELF")]),
    ("O-44", "占卜师", "玄学", 3, "support", "public", 3, "本回合员工调用机会 +1，并使自己打出的下一张牌数值效果翻倍", [ap(1), multiply(2)]),
]

lines = [
    "-- 收藏卡：需胜利解锁；含效果配置。可重复执行。",
    "SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cards' AND COLUMN_NAME = 'require_unlock');",
    "SET @sql := IF(@exist = 0, 'ALTER TABLE `cards` ADD COLUMN `require_unlock` tinyint NOT NULL DEFAULT 0 COMMENT ''1需胜利解锁才可进入对局'' AFTER `is_unique`', 'SELECT 1');",
    "PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;",
    "",
    "UPDATE `cards` SET `require_unlock` = 1 WHERE `card_code` IN ('O-16','O-17','O-18','O-19');",
    "",
]

start_id = 90
for i, row in enumerate(cards):
    code, name, prefix, cost, ctype, dept, dept_id, desc, effects = row
    cid = start_id + i
    img = f"/images/cards/{prefix}_{name}.webp"
    desc_sql = desc.replace("'", "''")
    lines.append(
        "INSERT INTO `cards` (`id`,`card_code`,`card_name`,`dept_id`,`dept_type`,`cost`,`card_type`,`description`,`image_url`,`combo_card_id`,`is_unique`,`require_unlock`,`status`,`created_at`,`updated_at`) "
        f"SELECT {cid}, '{code}', '{name}', {dept_id}, '{dept}', {cost}, '{ctype}', '{desc_sql}', '{img}', NULL, 0, 1, 1, NOW(), NOW() "
        f"WHERE NOT EXISTS (SELECT 1 FROM `cards` WHERE `card_code` = '{code}');"
    )
    lines.append(
        f"UPDATE `cards` SET `description` = '{desc_sql}', `card_type` = '{ctype}', `cost` = {cost} WHERE `card_code` = '{code}';"
    )
    lines.append(
        f"DELETE e FROM `card_effects` e INNER JOIN `cards` c ON c.id = e.card_id WHERE c.card_code = '{code}';"
    )
    for eff in effects:
        order, scope, etype, timing, delay, value, target = eff
        extra = "NULL"
        stack = "STACK"
        remain = 1
        if etype == "MULTIPLY_NEXT_CARD":
            extra = """'{"consumeAfterUse": true}'"""
            stack = "REPLACE"
        lines.append(
            "INSERT INTO `card_effects` (`card_id`,`effect_order`,`effect_scope`,`effect_type`,`trigger_timing`,`trigger_delay`,`remaining_triggers`,`stack_rule`,`duration_rounds`,`value`,`target_rule`,`extra_data`,`created_at`,`updated_at`) "
            f"SELECT c.id, {order}, '{scope}', '{etype}', '{timing}', {delay}, {remain}, '{stack}', 0, {value}, '{target}', {extra}, NOW(), NOW() "
            f"FROM `cards` c WHERE c.card_code = '{code}';"
        )
    lines.append("")

lines.append(f"ALTER TABLE `cards` AUTO_INCREMENT = {start_id + len(cards)};")
text = "\n".join(lines) + "\n"
paths = [
    OUT,
    Path(__file__).parent / "init" / "004_collectible_cards.sql",
    Path(__file__).parent.parent / "wa-demo-service" / "src" / "main" / "resources" / "db" / "004_collectible_cards.sql",
]
for p in paths:
    p.write_text(text, encoding="utf-8")
print(f"wrote {len(paths)} files, cards={len(cards)}")
