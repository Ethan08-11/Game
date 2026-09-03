-- 初始化基础数据
-- 说明：本文件仅保留“启动游戏必须的静态配置数据”
-- 不包含对局记录、日志、临时缓存、测试过程中产生的脏数据

-- ----------------------------
-- Table data for users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'player_01', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-07-29 17:22:02', '2026-07-20 18:25:23', '2026-07-27 14:11:40');
INSERT INTO `users` VALUES (2, 'player_02', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, NULL, '2026-07-20 18:25:23', '2026-07-27 14:11:43');

-- ----------------------------
-- Table data for user_profiles
-- ----------------------------
INSERT INTO `user_profiles` VALUES (1, 1, 'Sales_King', NULL, NULL, 5, 0, 0, 0, 0, 0, '2026-07-20 18:25:23', '2026-07-20 18:25:23');
INSERT INTO `user_profiles` VALUES (2, 2, 'Purchasing_God', NULL, NULL, 4, 0, 0, 0, 0, 0, '2026-07-20 18:25:23', '2026-07-20 18:25:23');

-- ----------------------------
-- Table data for achievement_defs
-- ----------------------------
INSERT INTO `achievement_defs` VALUES (1, 'ACH-001', '首胜', 'battle', '完成第一场胜利', 'win_count', '{"count": 1}', 'title', '{"name": "新手赢家"}', 1, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `achievement_defs` VALUES (2, 'ACH-002', '百战老兵', 'battle', '累计取得 10 场胜利', 'win_count', '{"count": 10}', 'money', '{"amount": 500}', 2, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `achievement_defs` VALUES (3, 'ACH-003', '任务达人', 'growth', '累计完成 20 个任务', 'task_complete_count', '{"count": 20}', 'exp', '{"amount": 200}', 3, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `achievement_defs` VALUES (4, 'ACH-004', '社交先锋', 'social', '累计添加 5 位好友', 'friend_count', '{"count": 5}', 'item', '{"count": 1, "itemCode": "FRIEND_BADGE"}', 4, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `achievement_defs` VALUES (5, 'ACH-005', '隐藏彩蛋', 'hidden', '完成一次特殊条件即可解锁', 'special_event', '{"code": "EASTER_EGG_001"}', 'title', '{"name": "彩蛋发现者"}', 5, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');

-- ----------------------------
-- Table data for bullies
-- ----------------------------
INSERT INTO `bullies` VALUES (1, 'BULLY_BASIC_001', '普通霸凌者', '旧通用模板，已停用。对局按顾客绑定专属霸凌者。', 110, 5, 0, 1, 0, '{"targetRule": "both_players", "canCritical": true, "attackPattern": "single_target"}', 0, '2026-07-22 13:04:31', '2026-07-22 13:04:31');
INSERT INTO `bullies` VALUES (2, 'BULLY_FOCUS_001', '点名恶霸', '专挑血更虚的护卫下手。血量比例持平时随机点名。护盾可以挡住。', 180, 22, 0, 1, 0, '{"customerCode":"CUSTOMER_KIND","pattern":"FOCUS_LOW_HP","chance":100,"shield":0,"bonusAttack":0,"catalogSummary":"专打血更虚的护卫；护盾可以挡住。"}', 1, '2026-07-22 13:04:31', '2026-07-22 13:04:31');
INSERT INTO `bullies` VALUES (3, 'BULLY_SHIELD_001', '硬扛恶霸', '约一成五回合胸口多 14 点盾，这回合出手也变轻。', 180, 22, 0, 1, 0, '{"customerCode":"CUSTOMER_TIMID","pattern":"ROUND_SHIELD","chance":15,"shield":14,"bonusAttack":0,"catalogSummary":"约一成五回合胸口多 14 点盾，这回合出手也变轻。"}', 1, '2026-07-22 13:04:31', '2026-07-22 13:04:31');
INSERT INTO `bullies` VALUES (4, 'BULLY_REVENGE_001', '针对恶霸', '仍打两人。约八成五回合盯打得最疼的人多挨 6 点。', 180, 22, 0, 1, 0, '{"customerCode":"CUSTOMER_ANXIOUS","pattern":"FOCUS_TOP_DAMAGE","chance":85,"shield":0,"bonusAttack":6,"catalogSummary":"约八成五回合会盯打得最疼的人多挨 6 点。"}', 1, '2026-07-22 13:04:31', '2026-07-22 13:04:31');
INSERT INTO `bullies` VALUES (5, 'BULLY_PAIR_001', '不落单恶霸', '仍打两人。两人都几乎挡住时，下一拍再抽半刀。', 180, 22, 0, 1, 0, '{"customerCode":"CUSTOMER_WINDOW","pattern":"BOTH_HALF_SWING","chance":0,"shield":0,"bonusAttack":0,"catalogSummary":"两人都几乎挡住时，下一拍会再抽半刀。"}', 1, '2026-07-22 13:04:31', '2026-07-22 13:04:31');

-- ----------------------------
-- Table data for card_depts
-- ----------------------------
INSERT INTO `card_depts` VALUES (1, 'DEPT_SALES', '销售部', 'sales', 1, 1, '2026-07-21 16:28:11', '2026-07-21 16:28:24');
INSERT INTO `card_depts` VALUES (2, 'DEPT_PURCHASE', '采购部', 'purchase', 2, 1, '2026-07-21 16:28:11', '2026-07-21 16:28:26');
INSERT INTO `card_depts` VALUES (3, 'DEPT_PUBLIC', '公共部', 'public', 3, 1, '2026-07-21 16:28:11', '2026-07-21 16:28:28');
INSERT INTO `card_depts` VALUES (4, 'DEPT_PASSERBY', '路人部', 'neutral', 4, 1, '2026-07-21 16:28:11', '2026-07-21 16:28:31');

-- ----------------------------
-- Table data for cards
-- ----------------------------
INSERT INTO `cards` VALUES (59, 'S-01', '破冰突击', 1, 'sales', 0, 'attack', '立即对霸凌者造成2点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (60, 'S-02', '精准打击', 1, 'sales', 1, 'attack', '立即对霸凌者造成3点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (61, 'S-03', '连续追击', 1, 'sales', 2, 'attack', '本回合对霸凌者造成3点伤害，下回合再造成2点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (62, 'S-04', '强攻风暴', 1, 'sales', 3, 'attack', '立即对霸凌者造成6点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (63, 'S-05', '协同夹击', 1, 'sales', 1, 'attack', '本回合对霸凌者造成2点伤害，下回合再造成1点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (64, 'S-06', '巅峰冲刺', 1, 'sales', 2, 'attack', '立即对霸凌者造成4点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');
INSERT INTO `cards` VALUES (65, 'P-01', '危机斡旋', 2, 'purchase', 0, 'defend', '为一名玩家增加2点防御', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (66, 'P-02', '闪电溯源', 2, 'purchase', 1, 'defend', '为一名玩家增加3点防御', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (67, 'P-03', '溢价折冲', 2, 'purchase', 2, 'defend', '为一名玩家增加4点防御', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (68, 'P-04', '总裁绿灯', 2, 'purchase', 3, 'defend', '为一名玩家增加6点防御', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (69, 'O-07', '系统热修复', 3, 'public', 0, 'attack', '立即对霸凌者造成2点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (70, 'O-09', '算力超频', 3, 'public', 1, 'attack', '立即对霸凌者造成3点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (71, 'O-11', '权限提级', 3, 'public', 2, 'attack', '本回合对霸凌者造成2点伤害，下回合再造成2点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (72, 'O-08', '品牌共鸣', 3, 'public', 2, 'attack', '立即对霸凌者造成4点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (73, 'O-12', '破圈引流', 3, 'public', 3, 'attack', '本回合对霸凌者造成3点伤害，下回合再造成2点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (74, 'O-15', '全渠道赋能', 1, 'sales', 0, 'attack', '本回合对霸凌者造成1点伤害，下回合再造成1点伤害', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');
INSERT INTO `cards` VALUES (75, 'O-01', '紧急拨付', 2, 'purchase', 1, 'defend', '为一名玩家增加3点防御', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (76, 'O-02', '后勤保障', 3, 'public', 2, 'defend', '本回合为一名玩家增加2点防御，下回合再增加2点防御', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (77, 'O-03', '战略储备', 3, 'public', 2, 'defend', '为一名玩家增加4点防御', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (78, 'O-10', '全线空投', 3, 'public', 3, 'defend', '本回合为一名玩家增加3点防御，下回合再增加3点防御', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 14:52:58');
INSERT INTO `cards` VALUES (79, 'O-13', '极速调拨', 3, 'public', 1, 'heal', '恢复一名玩家3点血量', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');
INSERT INTO `cards` VALUES (80, 'O-14', '供应链润滑', 3, 'public', 1, 'heal', '本回合恢复一名玩家2点血量，下回合再恢复1点血量', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');
INSERT INTO `cards` VALUES (81, 'O-04', '核心猎聘', 3, 'public', 2, 'heal', '恢复一名玩家4点血量', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');
INSERT INTO `cards` VALUES (82, 'O-05', '绩效激励', 3, 'public', 3, 'heal', '本回合恢复一名玩家3点血量，下回合再恢复3点血量', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');
INSERT INTO `cards` VALUES (83, 'O-06', '潜能激发', 3, 'public', 1, 'support', '本回合员工调用机会+1，下回合员工调用机会再+1', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');
INSERT INTO `cards` VALUES (84, 'L-01', '自来水好评', 4, 'neutral', 2, 'support', '本回合员工调用机会+2，下回合员工调用机会+1', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');
INSERT INTO `cards` VALUES (85, 'L-02', '我的发', 4, 'neutral', 3, 'support', '使自己打出的下一张牌数值效果翻倍', NULL, NULL, 0, 1, '2026-07-21 16:28:51', '2026-07-24 15:31:21');

-- ----------------------------
-- Table data for card_effects
-- ----------------------------
INSERT INTO `card_effects` VALUES (9, 59, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (10, 60, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (11, 61, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (12, 61, 2, 'BOSS', 'DAMAGE_BOSS', 'ROUND_START', 1, 1, 'STACK', 0, 2, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (13, 62, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 6, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (14, 63, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (15, 63, 2, 'BOSS', 'DAMAGE_BOSS', 'ROUND_START', 1, 1, 'STACK', 0, 1, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (16, 64, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 4, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (17, 65, 1, 'ANY_PLAYER', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (18, 66, 1, 'ANY_PLAYER', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (19, 67, 1, 'ANY_PLAYER', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 4, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (20, 68, 1, 'ANY_PLAYER', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 6, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (21, 69, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (22, 70, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (23, 71, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (24, 71, 2, 'BOSS', 'DAMAGE_BOSS', 'ROUND_START', 1, 1, 'STACK', 0, 2, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (25, 72, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 4, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (26, 73, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (27, 73, 2, 'BOSS', 'DAMAGE_BOSS', 'ROUND_START', 1, 1, 'STACK', 0, 2, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (28, 74, 1, 'BOSS', 'DAMAGE_BOSS', 'IMMEDIATE', 0, 1, 'STACK', 0, 1, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (29, 74, 2, 'BOSS', 'DAMAGE_BOSS', 'ROUND_START', 1, 1, 'STACK', 0, 1, 'BOSS', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (30, 75, 1, 'ANY_PLAYER', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (31, 76, 1, 'ANY_PLAYER', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (32, 76, 2, 'ANY_PLAYER', 'ADD_SHIELD', 'ROUND_START', 1, 1, 'STACK', 0, 2, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (33, 77, 1, 'ANY_PLAYER', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 4, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (34, 78, 1, 'ANY_PLAYER', 'ADD_SHIELD', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (35, 78, 2, 'ANY_PLAYER', 'ADD_SHIELD', 'ROUND_START', 1, 1, 'STACK', 0, 3, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (36, 79, 1, 'ANY_PLAYER', 'HEAL_PLAYER', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (37, 80, 1, 'ANY_PLAYER', 'HEAL_PLAYER', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (38, 80, 2, 'ANY_PLAYER', 'HEAL_PLAYER', 'ROUND_START', 1, 1, 'STACK', 0, 1, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (39, 81, 1, 'ANY_PLAYER', 'HEAL_PLAYER', 'IMMEDIATE', 0, 1, 'STACK', 0, 4, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (40, 82, 1, 'ANY_PLAYER', 'HEAL_PLAYER', 'IMMEDIATE', 0, 1, 'STACK', 0, 3, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (41, 82, 2, 'ANY_PLAYER', 'HEAL_PLAYER', 'ROUND_START', 1, 1, 'STACK', 0, 3, 'ANY_PLAYER', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (42, 83, 1, 'SELF', 'ADD_ACTION_POINTS', 'IMMEDIATE', 0, 1, 'STACK', 0, 1, 'SELF', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (43, 83, 2, 'SELF', 'ADD_ACTION_POINTS', 'ROUND_START', 1, 1, 'STACK', 0, 1, 'SELF', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (44, 84, 1, 'SELF', 'ADD_ACTION_POINTS', 'IMMEDIATE', 0, 1, 'STACK', 0, 2, 'SELF', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (45, 84, 2, 'SELF', 'ADD_ACTION_POINTS', 'ROUND_START', 1, 1, 'STACK', 0, 1, 'SELF', NULL, '2026-07-24 15:31:21', '2026-07-24 15:31:21');
INSERT INTO `card_effects` VALUES (46, 85, 1, 'SELF', 'MULTIPLY_NEXT_CARD', 'NEXT_CARD', 0, 1, 'REPLACE', 0, 2, 'SELF', '{"consumeAfterUse": true}', '2026-07-24 15:31:21', '2026-07-24 15:31:21');

INSERT INTO `cards` VALUES (86,'O-16','信使',3,'public',1,'draw','抽 2 张牌','/images/cards/物流_信使.webp',NULL,0,1,'2026-08-19 11:30:00','2026-08-19 11:30:00');
INSERT INTO `cards` VALUES (87,'O-17','告示官',3,'public',1,'support','本回合霸凌者攻击 -2','/images/cards/宣传_告示官.webp',NULL,0,1,'2026-08-19 11:30:00','2026-08-19 11:30:00');
INSERT INTO `cards` VALUES (88,'O-18','鼓舞手',3,'public',2,'defend','双方各获得 2 点防御','/images/cards/活动_鼓舞手.webp',NULL,0,1,'2026-08-19 11:30:00','2026-08-19 11:30:00');
INSERT INTO `cards` VALUES (89,'O-19','宫廷乐师',3,'public',2,'heal','双方各恢复 2 点血量','/images/cards/演艺_宫廷乐师.webp',NULL,0,1,'2026-08-19 11:30:00','2026-08-19 11:30:00');
INSERT INTO `card_effects` VALUES (47,86,1,'SELF','DRAW_CARDS','IMMEDIATE',0,1,'STACK',0,2,'SELF',NULL,'2026-08-19 11:30:00','2026-08-19 11:30:00');
INSERT INTO `card_effects` VALUES (48,87,1,'BOSS','REDUCE_BOSS_ATTACK','IMMEDIATE',0,1,'STACK',0,2,'BOSS',NULL,'2026-08-19 11:30:00','2026-08-19 11:30:00');
INSERT INTO `card_effects` VALUES (49,88,1,'ALL_PLAYERS','ADD_SHIELD','IMMEDIATE',0,1,'STACK',0,2,'ALL_PLAYERS',NULL,'2026-08-19 11:30:00','2026-08-19 11:30:00');
INSERT INTO `card_effects` VALUES (50,89,1,'ALL_PLAYERS','HEAL_PLAYER','IMMEDIATE',0,1,'STACK',0,2,'ALL_PLAYERS',NULL,'2026-08-19 11:30:00','2026-08-19 11:30:00');

-- ----------------------------
-- Table data for user_achievements
-- ----------------------------
INSERT INTO `user_achievements` VALUES (1, 1, 1, 1, 1, '2026-07-22 11:54:21', 1, '2026-07-22 11:54:21', '2026-07-22 11:54:21', '2026-07-22 11:54:21');
INSERT INTO `user_achievements` VALUES (2, 2, 2, 10, 0, NULL, 0, NULL, '2026-07-22 11:54:21', '2026-07-22 11:54:21');

-- ----------------------------
-- Table data for user_card_pools
-- ----------------------------
INSERT INTO `user_card_pools` VALUES (1, 1, 59, 1, 1, 1, '2026-07-22 11:54:21', '2026-07-22 11:54:21');
INSERT INTO `user_card_pools` VALUES (2, 1, 60, 1, 1, 1, '2026-07-22 11:54:21', '2026-07-22 11:54:21');
INSERT INTO `user_card_pools` VALUES (3, 2, 65, 1, 1, 1, '2026-07-22 11:54:21', '2026-07-22 11:54:21');
INSERT INTO `user_card_pools` VALUES (4, 2, 66, 1, 1, 1, '2026-07-22 11:54:21', '2026-07-22 11:54:21');
