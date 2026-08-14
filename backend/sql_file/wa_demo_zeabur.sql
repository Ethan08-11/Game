/*
 Navicat Premium Dump SQL

 Source Server         : Hand_in_Hand_game
 Source Server Type    : MySQL
 Source Server Version : 80033 (8.0.33)
 Source Host           : localhost:3308
 Source Schema         : wa_demo

 Target Server Type    : MySQL
 Target Server Version : 80033 (8.0.33)
 File Encoding         : 65001

 Date: 06/08/2026 12:38:36
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for achievement_defs
-- ----------------------------
DROP TABLE IF EXISTS `achievement_defs`;
CREATE TABLE `achievement_defs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成就定义主键',
  `achievement_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就唯一编码',
  `achievement_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就名称',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就分类：battle/growth/social/hidden',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就达成条件描述',
  `condition_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '条件判定类型',
  `condition_value` json NOT NULL COMMENT '达成条件阈值(JSON)',
  `reward_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '奖励类型',
  `reward_value` json NULL COMMENT '奖励内容(JSON)',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '展示排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_achievement_code`(`achievement_code` ASC) USING BTREE COMMENT '成就编码唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '游戏成就定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of achievement_defs
-- ----------------------------
INSERT INTO `achievement_defs` VALUES (1, 'ACH-001', '首胜', 'battle', '完成第一场胜利', 'win_count', '{\"count\": 1}', 'title', '{\"name\": \"新手赢家\"}', 1, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `achievement_defs` VALUES (2, 'ACH-002', '百战老兵', 'battle', '累计取得 10 场胜利', 'win_count', '{\"count\": 10}', 'money', '{\"amount\": 500}', 2, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `achievement_defs` VALUES (3, 'ACH-003', '任务达人', 'growth', '累计完成 20 个任务', 'task_complete_count', '{\"count\": 20}', 'exp', '{\"amount\": 200}', 3, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `achievement_defs` VALUES (4, 'ACH-004', '社交先锋', 'social', '累计添加 5 位好友', 'friend_count', '{\"count\": 5}', 'item', '{\"count\": 1, \"itemCode\": \"FRIEND_BADGE\"}', 4, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `achievement_defs` VALUES (5, 'ACH-005', '隐藏彩蛋', 'hidden', '完成一次特殊条件即可解锁', 'special_event', '{\"code\": \"EASTER_EGG_001\"}', 'title', '{\"name\": \"彩蛋发现者\"}', 5, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');

-- ----------------------------
-- Table structure for bullies
-- ----------------------------
DROP TABLE IF EXISTS `bullies`;
CREATE TABLE `bullies`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '霸凌者主键',
  `bully_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '霸凌者编码',
  `bully_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '霸凌者名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '霸凌者背景/说明',
  `hp` int NOT NULL DEFAULT 50 COMMENT '初始血量',
  `attack_power` int NOT NULL DEFAULT 0 COMMENT '基础攻击',
  `defense_value` int NOT NULL DEFAULT 0 COMMENT '基础防御',
  `speed_value` int NOT NULL DEFAULT 0 COMMENT '速度/出手优先级',
  `rage_value` int NOT NULL DEFAULT 0 COMMENT '怒气初始值',
  `skill_data` json NULL COMMENT '特殊技能配置(JSON)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_bully_code`(`bully_code` ASC) USING BTREE COMMENT '霸凌者编码唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '霸凌者配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bullies
-- ----------------------------
INSERT INTO `bullies` VALUES (1, 'BULLY_BASIC_001', '普通霸凌者', '当前版本唯一的霸凌者模板，后续可扩展为多个难度与行为模板。', 100, 10, 0, 1, 0, '{\"targetRule\": \"both_players\", \"canCritical\": true, \"attackPattern\": \"single_target\"}', 1, '2026-07-22 13:04:31', '2026-08-04 16:55:23');

-- ----------------------------
-- Table structure for card_depts
-- ----------------------------
DROP TABLE IF EXISTS `card_depts`;
CREATE TABLE `card_depts`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门主键',
  `dept_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门唯一编码',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `dept_category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门大类：sales/purchase/public/neutral',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dept_code`(`dept_code` ASC) USING BTREE COMMENT '部门编码唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌部门字典表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of card_depts
-- ----------------------------
INSERT INTO `card_depts` VALUES (1, 'DEPT_SALES', '销售部', 'sales', 1, 1, '2026-07-21 16:28:11', '2026-07-21 16:28:24');
INSERT INTO `card_depts` VALUES (2, 'DEPT_PURCHASE', '采购部', 'purchase', 2, 1, '2026-07-21 16:28:11', '2026-07-21 16:28:26');
INSERT INTO `card_depts` VALUES (3, 'DEPT_PUBLIC', '公共部', 'public', 3, 1, '2026-07-21 16:28:11', '2026-07-21 16:28:28');
INSERT INTO `card_depts` VALUES (4, 'DEPT_PASSERBY', '路人部', 'neutral', 4, 1, '2026-07-21 16:28:11', '2026-07-21 16:28:31');

-- ----------------------------
-- Table structure for card_effects
-- ----------------------------
DROP TABLE IF EXISTS `card_effects`;
CREATE TABLE `card_effects`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '效果配置主键',
  `card_id` bigint NOT NULL COMMENT '归属卡牌ID',
  `effect_order` int NOT NULL DEFAULT 1 COMMENT '效果结算顺序',
  `effect_scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标范围：BOSS/SELF/ANY_PLAYER',
  `effect_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '效果类型：DAMAGE_BOSS/HEAL_PLAYER/ADD_SHIELD/ADD_ACTION_POINTS/MULTIPLY_NEXT_CARD',
  `trigger_timing` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'IMMEDIATE' COMMENT '触发时机：IMMEDIATE/ROUND_START/NEXT_CARD',
  `trigger_delay` int NOT NULL DEFAULT 0 COMMENT '延迟回合数；0表示当前时机，1表示下一回合',
  `remaining_triggers` int NOT NULL DEFAULT 1 COMMENT '创建本局待处理效果时的初始触发次数',
  `stack_rule` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STACK' COMMENT '叠加规则：STACK/REPLACE/REFRESH',
  `duration_rounds` int NOT NULL DEFAULT 0 COMMENT '持续回合数；一次性效果为0',
  `value` int NOT NULL DEFAULT 0 COMMENT '效果基础数值',
  `target_rule` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标规则：BOSS/SELF/ANY_PLAYER',
  `extra_data` json NULL COMMENT '其他效果机制JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_card_id`(`card_id` ASC) USING BTREE COMMENT '卡牌ID查询索引',
  CONSTRAINT `card_effects_ibfk_1` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌复杂效果配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of card_effects
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
INSERT INTO `card_effects` VALUES (46, 85, 1, 'SELF', 'MULTIPLY_NEXT_CARD', 'NEXT_CARD', 0, 1, 'REPLACE', 0, 2, 'SELF', '{\"consumeAfterUse\": true}', '2026-07-24 15:31:21', '2026-07-24 15:31:21');

-- ----------------------------
-- Table structure for cards
-- ----------------------------
DROP TABLE IF EXISTS `cards`;
CREATE TABLE `cards`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '卡牌主键',
  `card_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡牌唯一编号(如S-01)',
  `card_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡牌名称',
  `dept_id` bigint NOT NULL COMMENT '所属部门ID',
  `dept_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务分类冗余(如sales)',
  `cost` tinyint NOT NULL COMMENT '卡牌使用费用(资金)',
  `card_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡牌类型：attack/defend/draw/consume',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡牌效果文本描述',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '卡牌卡面图片URL',
  `combo_card_id` bigint NULL DEFAULT NULL COMMENT '强关联连携卡牌ID',
  `is_unique` tinyint NOT NULL DEFAULT 0 COMMENT '是否唯一卡：1是 0否',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_card_code`(`card_code` ASC) USING BTREE COMMENT '卡牌编号唯一索引',
  INDEX `idx_dept_type_cost`(`dept_id` ASC, `card_type` ASC, `cost` ASC) USING BTREE COMMENT '用于按部门、类型和费用快速筛选卡牌的外键复合索引',
  CONSTRAINT `cards_ibfk_1` FOREIGN KEY (`dept_id`) REFERENCES `card_depts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 86 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌基础信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cards
-- ----------------------------
INSERT INTO `cards` VALUES (59, 'S-01', 'Carl', 1, 'sales', 0, 'attack', '立即对霸凌者造成2点伤害', '/images/cards/Card_Carl.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (60, 'S-02', 'Colin', 1, 'sales', 1, 'attack', '立即对霸凌者造成3点伤害', '/images/cards/Card_Colin.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (61, 'S-03', 'Daniel', 1, 'sales', 2, 'attack', '本回合对霸凌者造成3点伤害，下回合再造成2点伤害', '/images/cards/Card_Daniel.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (62, 'S-04', 'Felicity', 1, 'sales', 3, 'attack', '立即对霸凌者造成6点伤害', '/images/cards/Card_Felicity.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (63, 'S-05', 'Kinyond', 1, 'sales', 1, 'attack', '本回合对霸凌者造成2点伤害，下回合再造成1点伤害', '/images/cards/Card_Kinyond.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (64, 'S-06', 'Kade', 1, 'sales', 2, 'attack', '立即对霸凌者造成4点伤害', '/images/cards/Card_Kade.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (65, 'P-01', 'Sandra', 2, 'purchase', 0, 'defend', '为一名玩家增加2点防御', '/images/cards/Card_Sandra.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (66, 'P-02', 'Harry', 2, 'purchase', 1, 'defend', '为一名玩家增加3点防御', '/images/cards/Card_Harry.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (67, 'P-03', 'Amy', 2, 'purchase', 2, 'defend', '为一名玩家增加4点防御', '/images/cards/Card_Amy.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (68, 'P-04', 'Duane', 2, 'purchase', 3, 'defend', '为一名玩家增加6点防御', '/images/cards/Card_Duane.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (69, 'O-07', 'Hermione', 3, 'public', 0, 'attack', '立即对霸凌者造成2点伤害', '/images/cards/Card_Hermione.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (70, 'O-09', 'Chrissy', 3, 'public', 1, 'attack', '立即对霸凌者造成3点伤害', '/images/cards/Card_Chrissy.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (71, 'O-11', 'Harrison', 3, 'public', 2, 'attack', '本回合对霸凌者造成2点伤害，下回合再造成2点伤害', '/images/cards/Card_Harrison.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (72, 'O-08', 'Iris', 3, 'public', 2, 'attack', '立即对霸凌者造成4点伤害', '/images/cards/Card_Iris.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (73, 'O-12', 'Leo', 3, 'public', 3, 'attack', '本回合对霸凌者造成3点伤害，下回合再造成2点伤害', '/images/cards/Card_Leo.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (74, 'O-15', 'Riley', 3, 'public', 0, 'attack', '本回合对霸凌者造成1点伤害，下回合再造成1点伤害', '/images/cards/Card_Riley.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (75, 'O-01', 'Charlene', 3, 'public', 1, 'defend', '为一名玩家增加3点防御', '/images/cards/Card_Charlene.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (76, 'O-02', 'Eden', 3, 'public', 2, 'defend', '本回合为一名玩家增加2点防御，下回合再增加2点防御', '/images/cards/Card_Eden.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (77, 'O-03', 'Rain', 3, 'public', 2, 'defend', '为一名玩家增加4点防御', '/images/cards/Card_Rain.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (78, 'O-10', 'Yuna', 3, 'public', 3, 'defend', '本回合为一名玩家增加3点防御，下回合再增加3点防御', '/images/cards/Card_Yuna.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 22:07:20');
INSERT INTO `cards` VALUES (79, 'O-13', 'Ethan', 3, 'public', 1, 'heal', '恢复一名玩家3点血量', '/images/cards/Card_Ethan.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (80, 'O-14', 'Nico', 3, 'public', 1, 'heal', '本回合恢复一名玩家2点血量，下回合再恢复1点血量', '/images/cards/Card_Nico.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (81, 'O-04', 'Tancy', 3, 'public', 2, 'heal', '恢复一名玩家4点血量', '/images/cards/Card_Tancy.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (82, 'O-05', 'Amber', 3, 'public', 3, 'heal', '本回合恢复一名玩家3点血量，下回合再恢复3点血量', '/images/cards/Card_Amber.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (83, 'O-06', 'Dylan', 3, 'public', 1, 'support', '本回合员工调用机会+1，下回合员工调用机会再+1', '/images/cards/Card_Dylan.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (84, 'L-01', 'Ancy', 4, 'neutral', 2, 'support', '本回合员工调用机会+2，下回合员工调用机会+1', '/images/cards/Card_Ancy.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 21:26:42');
INSERT INTO `cards` VALUES (85, 'L-02', '我的发', 4, 'neutral', 3, 'support', '使自己打出的下一张牌数值效果翻倍', '/images/cards/img.png', NULL, 0, 1, '2026-07-21 16:28:51', '2026-08-01 22:08:30');

-- ----------------------------
-- Table structure for customer_types
-- ----------------------------
DROP TABLE IF EXISTS `customer_types`;
CREATE TABLE `customer_types`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '顾客类型主键',
  `customer_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '顾客类型编码',
  `customer_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '顾客类型名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '顾客性格/背景描述',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '形象照片URL',
  `effect_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '效果类型，如 bully_attack_down/bully_hp_up/bully_attack_up',
  `effect_value` int NOT NULL DEFAULT 0 COMMENT '效果数值',
  `trigger_chance` int NOT NULL DEFAULT 0 COMMENT '触发概率(0-100)',
  `selection_weight` int NOT NULL DEFAULT 1 COMMENT '顾客类型随机出现权重',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_customer_code`(`customer_code` ASC) USING BTREE COMMENT '顾客类型编码唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '顾客类型配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of customer_types
-- ----------------------------
INSERT INTO `customer_types` VALUES (1, 'CUSTOMER_KIND', '善良稳重顾客', '情绪稳定、愿意配合，能削弱霸凌者的攻击性。', '/images/customer/p1.png', 'bully_attack_down', -2, 40, 40, 1, 1, '2026-07-22 13:04:31', '2026-07-31 18:27:41');
INSERT INTO `customer_types` VALUES (2, 'CUSTOMER_TIMID', '胆小怕事顾客', '容易紧张，需要额外保护，能提升霸凌者血量。', '/images/customer/p2.png', 'bully_hp_up', 1, 55, 35, 1, 2, '2026-07-22 13:04:31', '2026-07-31 18:27:41');
INSERT INTO `customer_types` VALUES (3, 'CUSTOMER_ANXIOUS', '焦虑难安顾客', '情绪波动较大，会加剧现场冲突，提升霸凌者攻击。', '/images/customer/p3.png', 'bully_attack_up', 1, 50, 25, 1, 3, '2026-07-22 13:04:31', '2026-07-31 18:27:41');

-- ----------------------------
-- Table structure for deck_card_configs
-- ----------------------------
DROP TABLE IF EXISTS `deck_card_configs`;
CREATE TABLE `deck_card_configs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '固定牌组配置主键',
  `dept_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门编码：sales/purchase',
  `card_id` bigint NOT NULL COMMENT '卡牌模板ID',
  `card_count` int NOT NULL DEFAULT 1 COMMENT '该卡牌在部门20张牌组中的数量',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '配置展示顺序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dept_card_config`(`dept_type` ASC, `card_id` ASC) USING BTREE,
  INDEX `idx_deck_config_card`(`card_id` ASC) USING BTREE,
  CONSTRAINT `fk_deck_card_configs_card` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_deck_card_count` CHECK (`card_count` > 0)
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门固定20张战斗牌组配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of deck_card_configs
-- ----------------------------
INSERT INTO `deck_card_configs` VALUES (1, 'sales', 59, 2, 1, 1, '2026-07-23 19:28:11', '2026-07-23 19:28:11');
INSERT INTO `deck_card_configs` VALUES (2, 'sales', 60, 2, 2, 1, '2026-07-23 19:28:11', '2026-07-23 19:28:11');
INSERT INTO `deck_card_configs` VALUES (3, 'sales', 61, 2, 3, 1, '2026-07-23 19:28:11', '2026-07-23 19:28:11');
INSERT INTO `deck_card_configs` VALUES (4, 'sales', 62, 2, 4, 1, '2026-07-23 19:28:11', '2026-07-23 19:28:11');
INSERT INTO `deck_card_configs` VALUES (5, 'sales', 63, 2, 5, 1, '2026-07-23 19:28:11', '2026-07-23 19:28:11');
INSERT INTO `deck_card_configs` VALUES (6, 'sales', 64, 2, 6, 1, '2026-07-23 19:28:11', '2026-07-23 19:28:11');
INSERT INTO `deck_card_configs` VALUES (7, 'sales', 69, 1, 7, 1, '2026-07-23 19:28:11', '2026-08-03 17:32:50');
INSERT INTO `deck_card_configs` VALUES (8, 'sales', 70, 1, 8, 1, '2026-07-23 19:28:11', '2026-08-03 17:32:52');
INSERT INTO `deck_card_configs` VALUES (9, 'sales', 71, 1, 9, 1, '2026-07-23 19:28:11', '2026-08-03 17:32:53');
INSERT INTO `deck_card_configs` VALUES (10, 'sales', 72, 1, 10, 1, '2026-07-23 19:28:11', '2026-08-03 17:32:54');
INSERT INTO `deck_card_configs` VALUES (11, 'sales', 73, 1, 11, 1, '2026-07-23 19:28:11', '2026-08-03 17:32:56');
INSERT INTO `deck_card_configs` VALUES (12, 'sales', 79, 1, 12, 1, '2026-07-23 19:28:11', '2026-08-03 17:32:57');
INSERT INTO `deck_card_configs` VALUES (13, 'sales', 80, 1, 13, 1, '2026-07-23 19:28:11', '2026-08-03 17:32:58');
INSERT INTO `deck_card_configs` VALUES (14, 'sales', 83, 1, 14, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:00');
INSERT INTO `deck_card_configs` VALUES (15, 'purchase', 65, 3, 1, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:17');
INSERT INTO `deck_card_configs` VALUES (16, 'purchase', 66, 3, 2, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:21');
INSERT INTO `deck_card_configs` VALUES (17, 'purchase', 67, 3, 3, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:22');
INSERT INTO `deck_card_configs` VALUES (18, 'purchase', 68, 3, 4, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:24');
INSERT INTO `deck_card_configs` VALUES (19, 'purchase', 74, 1, 5, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:38');
INSERT INTO `deck_card_configs` VALUES (20, 'purchase', 75, 1, 6, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:39');
INSERT INTO `deck_card_configs` VALUES (21, 'purchase', 76, 1, 7, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:40');
INSERT INTO `deck_card_configs` VALUES (22, 'purchase', 77, 1, 8, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:41');
INSERT INTO `deck_card_configs` VALUES (23, 'purchase', 78, 1, 9, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:42');
INSERT INTO `deck_card_configs` VALUES (24, 'purchase', 79, 1, 10, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:43');
INSERT INTO `deck_card_configs` VALUES (25, 'purchase', 80, 1, 11, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:45');
INSERT INTO `deck_card_configs` VALUES (26, 'purchase', 83, 1, 12, 1, '2026-07-23 19:28:11', '2026-08-03 17:33:46');

-- ----------------------------
-- Table structure for friend_remark
-- ----------------------------
DROP TABLE IF EXISTS `friend_remark`;
CREATE TABLE `friend_remark`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '当前登录用户ID',
  `friend_user_id` bigint NOT NULL COMMENT '被备注的好友用户ID',
  `remark_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '备注名',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_friend`(`user_id` ASC, `friend_user_id` ASC) USING BTREE,
  INDEX `idx_friend_user_id`(`friend_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_friend_remark_friend_user` FOREIGN KEY (`friend_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_friend_remark_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 651 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友备注表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of friend_remark
-- ----------------------------
INSERT INTO `friend_remark` VALUES (1, 45, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (2, 44, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (3, 35, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (4, 34, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (5, 33, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (6, 32, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (7, 31, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (8, 30, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (9, 29, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (10, 28, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (11, 27, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (12, 26, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (13, 25, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (14, 24, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (15, 23, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (16, 21, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (17, 20, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (18, 19, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (19, 18, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (20, 17, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (21, 15, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (22, 13, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (23, 12, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (24, 11, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (25, 10, 9, 'amber', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (26, 45, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (27, 44, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (28, 35, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (29, 34, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (30, 33, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (31, 32, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (32, 31, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (33, 30, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (34, 29, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (35, 28, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (36, 27, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (37, 26, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (38, 25, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (39, 23, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (40, 21, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (41, 20, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (42, 19, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (43, 18, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (44, 17, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (45, 15, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (46, 13, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (47, 12, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (48, 11, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (49, 10, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (50, 9, 24, 'amy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (51, 45, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (52, 44, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (53, 35, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (54, 34, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (55, 33, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (56, 32, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (57, 31, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (58, 30, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (59, 29, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (60, 28, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (61, 27, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (62, 26, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (63, 25, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (64, 24, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (65, 23, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (66, 21, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (67, 20, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (68, 19, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (69, 18, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (70, 17, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (71, 15, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (72, 13, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (73, 12, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (74, 11, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (75, 9, 10, 'ancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (76, 45, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (77, 44, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (78, 35, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (79, 34, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (80, 33, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (81, 32, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (82, 31, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (83, 30, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (84, 29, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (85, 28, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (86, 27, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (87, 26, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (88, 24, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (89, 23, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (90, 21, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (91, 20, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (92, 19, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (93, 18, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (94, 17, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (95, 15, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (96, 13, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (97, 12, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (98, 11, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (99, 10, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (100, 9, 25, 'carl', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (101, 45, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (102, 44, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (103, 35, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (104, 34, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (105, 33, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (106, 31, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (107, 30, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (108, 29, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (109, 28, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (110, 27, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (111, 26, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (112, 25, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (113, 24, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (114, 23, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (115, 21, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (116, 20, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (117, 19, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (118, 18, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (119, 17, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (120, 15, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (121, 13, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (122, 12, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (123, 11, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (124, 10, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (125, 9, 32, 'charlene', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (126, 45, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (127, 44, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (128, 35, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (129, 34, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (130, 33, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (131, 32, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (132, 31, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (133, 30, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (134, 29, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (135, 28, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (136, 27, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (137, 26, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (138, 25, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (139, 24, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (140, 23, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (141, 21, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (142, 20, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (143, 19, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (144, 18, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (145, 17, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (146, 15, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (147, 12, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (148, 11, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (149, 10, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (150, 9, 13, 'chrissy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (151, 45, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (152, 44, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (153, 35, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (154, 34, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (155, 33, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (156, 32, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (157, 31, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (158, 30, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (159, 29, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (160, 28, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (161, 27, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (162, 25, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (163, 24, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (164, 23, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (165, 21, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (166, 20, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (167, 19, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (168, 18, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (169, 17, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (170, 15, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (171, 13, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (172, 12, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (173, 11, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (174, 10, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (175, 9, 26, 'colin', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (176, 45, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (177, 44, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (178, 35, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (179, 34, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (180, 33, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (181, 32, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (182, 30, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (183, 29, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (184, 28, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (185, 27, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (186, 26, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (187, 25, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (188, 24, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (189, 23, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (190, 21, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (191, 20, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (192, 19, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (193, 18, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (194, 17, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (195, 15, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (196, 13, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (197, 12, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (198, 11, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (199, 10, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (200, 9, 31, 'daniel', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (201, 45, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (202, 44, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (203, 35, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (204, 34, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (205, 33, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (206, 32, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (207, 31, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (208, 30, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (209, 29, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (210, 28, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (211, 27, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (212, 26, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (213, 25, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (214, 24, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (215, 21, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (216, 20, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (217, 19, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (218, 18, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (219, 17, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (220, 15, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (221, 13, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (222, 12, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (223, 11, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (224, 10, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (225, 9, 23, 'duane', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (226, 45, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (227, 44, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (228, 35, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (229, 34, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (230, 33, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (231, 32, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (232, 31, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (233, 30, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (234, 29, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (235, 28, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (236, 27, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (237, 26, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (238, 25, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (239, 24, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (240, 23, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (241, 21, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (242, 20, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (243, 19, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (244, 18, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (245, 17, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (246, 15, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (247, 13, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (248, 12, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (249, 10, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (250, 9, 11, 'dylan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (251, 45, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (252, 44, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (253, 35, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (254, 34, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (255, 33, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (256, 32, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (257, 31, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (258, 30, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (259, 29, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (260, 28, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (261, 27, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (262, 26, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (263, 25, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (264, 24, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (265, 23, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (266, 21, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (267, 20, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (268, 19, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (269, 18, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (270, 17, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (271, 15, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (272, 13, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (273, 11, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (274, 10, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (275, 9, 12, 'eden', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (276, 45, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (277, 44, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (278, 35, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (279, 34, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (280, 32, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (281, 31, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (282, 30, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (283, 29, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (284, 28, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (285, 27, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (286, 26, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (287, 25, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (288, 24, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (289, 23, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (290, 21, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (291, 20, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (292, 19, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (293, 18, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (294, 17, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (295, 15, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (296, 13, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (297, 12, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (298, 11, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (299, 10, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (300, 9, 33, 'ethan', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (301, 45, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (302, 44, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (303, 35, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (304, 34, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (305, 33, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (306, 32, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (307, 31, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (308, 30, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (309, 28, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (310, 27, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (311, 26, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (312, 25, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (313, 24, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (314, 23, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (315, 21, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (316, 20, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (317, 19, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (318, 18, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (319, 17, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (320, 15, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (321, 13, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (322, 12, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (323, 11, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (324, 10, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (325, 9, 29, 'felicity', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (326, 45, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (327, 35, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (328, 34, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (329, 33, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (330, 32, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (331, 31, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (332, 30, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (333, 29, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (334, 28, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (335, 27, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (336, 26, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (337, 25, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (338, 24, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (339, 23, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (340, 21, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (341, 20, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (342, 19, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (343, 18, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (344, 17, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (345, 15, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (346, 13, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (347, 12, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (348, 11, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (349, 10, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (350, 9, 44, 'harrison', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (351, 45, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (352, 44, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (353, 35, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (354, 33, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (355, 32, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (356, 31, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (357, 30, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (358, 29, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (359, 28, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (360, 27, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (361, 26, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (362, 25, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (363, 24, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (364, 23, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (365, 21, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (366, 20, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (367, 19, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (368, 18, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (369, 17, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (370, 15, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (371, 13, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (372, 12, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (373, 11, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (374, 10, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (375, 9, 34, 'harry', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (376, 45, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (377, 44, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (378, 35, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (379, 34, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (380, 33, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (381, 32, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (382, 31, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (383, 30, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (384, 29, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (385, 28, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (386, 27, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (387, 26, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (388, 25, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (389, 24, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (390, 23, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (391, 21, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (392, 19, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (393, 18, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (394, 17, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (395, 15, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (396, 13, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (397, 12, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (398, 11, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (399, 10, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (400, 9, 20, 'hermione', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (401, 45, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (402, 44, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (403, 35, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (404, 34, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (405, 33, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (406, 32, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (407, 31, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (408, 30, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (409, 29, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (410, 28, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (411, 27, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (412, 26, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (413, 25, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (414, 24, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (415, 23, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (416, 21, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (417, 20, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (418, 19, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (419, 18, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (420, 17, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (421, 13, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (422, 12, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (423, 11, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (424, 10, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (425, 9, 15, 'iris', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (426, 45, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (427, 44, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (428, 35, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (429, 34, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (430, 33, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (431, 32, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (432, 31, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (433, 30, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (434, 29, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (435, 28, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (436, 26, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (437, 25, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (438, 24, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (439, 23, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (440, 21, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (441, 20, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (442, 19, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (443, 18, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (444, 17, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (445, 15, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (446, 13, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (447, 12, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (448, 11, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (449, 10, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (450, 9, 27, 'kade', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (451, 45, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (452, 44, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (453, 35, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (454, 34, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (455, 33, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (456, 32, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (457, 31, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (458, 30, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (459, 29, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (460, 28, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (461, 27, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (462, 26, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (463, 25, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (464, 24, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (465, 23, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (466, 20, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (467, 19, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (468, 18, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (469, 17, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (470, 15, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (471, 13, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (472, 12, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (473, 11, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (474, 10, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (475, 9, 21, 'kinyond', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (476, 44, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (477, 35, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (478, 34, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (479, 33, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (480, 32, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (481, 31, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (482, 30, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (483, 29, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (484, 28, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (485, 27, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (486, 26, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (487, 25, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (488, 24, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (489, 23, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (490, 21, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (491, 20, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (492, 19, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (493, 18, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (494, 17, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (495, 15, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (496, 13, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (497, 12, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (498, 11, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (499, 10, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (500, 9, 45, 'leo', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (501, 45, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (502, 44, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (503, 35, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (504, 34, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (505, 33, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (506, 32, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (507, 31, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (508, 30, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (509, 29, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (510, 27, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (511, 26, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (512, 25, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (513, 24, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (514, 23, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (515, 21, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (516, 20, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (517, 19, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (518, 18, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (519, 17, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (520, 15, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (521, 13, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (522, 12, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (523, 11, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (524, 10, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (525, 9, 28, 'nico', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (526, 45, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (527, 44, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (528, 35, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (529, 34, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (530, 33, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (531, 32, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (532, 31, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (533, 30, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (534, 29, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (535, 28, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (536, 27, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (537, 26, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (538, 25, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (539, 24, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (540, 23, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (541, 21, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (542, 20, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (543, 19, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (544, 18, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (545, 15, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (546, 13, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (547, 12, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (548, 11, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (549, 10, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (550, 9, 17, 'rain', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (551, 45, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (552, 44, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (553, 35, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (554, 34, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (555, 33, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (556, 32, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (557, 31, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (558, 29, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (559, 28, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (560, 27, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (561, 26, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (562, 25, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (563, 24, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (564, 23, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (565, 21, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (566, 20, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (567, 19, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (568, 18, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (569, 17, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (570, 15, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (571, 13, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (572, 12, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (573, 11, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (574, 10, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (575, 9, 30, 'riley', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (576, 45, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (577, 44, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (578, 34, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (579, 33, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (580, 32, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (581, 31, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (582, 30, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (583, 29, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (584, 28, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (585, 27, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (586, 26, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (587, 25, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (588, 24, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (589, 23, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (590, 21, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (591, 20, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (592, 19, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (593, 18, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (594, 17, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (595, 15, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (596, 13, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (597, 12, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (598, 11, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (599, 10, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (600, 9, 35, 'sandra', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (601, 45, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (602, 44, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (603, 35, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (604, 34, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (605, 33, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (606, 32, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (607, 31, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (608, 30, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (609, 29, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (610, 28, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (611, 27, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (612, 26, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (613, 25, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (614, 24, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (615, 23, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (616, 21, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (617, 20, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (618, 19, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (619, 17, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (620, 15, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (621, 13, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (622, 12, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (623, 11, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (624, 10, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (625, 9, 18, 'tancy', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (626, 45, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (627, 44, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (628, 35, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (629, 34, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (630, 33, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (631, 32, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (632, 31, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (633, 30, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (634, 29, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (635, 28, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (636, 27, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (637, 26, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (638, 25, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (639, 24, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (640, 23, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (641, 21, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (642, 20, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (643, 18, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (644, 17, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (645, 15, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (646, 13, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (647, 12, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (648, 11, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (649, 10, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');
INSERT INTO `friend_remark` VALUES (650, 9, 19, 'yuna', '2026-08-04 17:50:04', '2026-08-04 17:50:04');

-- ----------------------------
-- Table structure for friendships
-- ----------------------------
DROP TABLE IF EXISTS `friendships`;
CREATE TABLE `friendships`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系主键',
  `user_id` bigint NOT NULL COMMENT '发起方用户ID',
  `friend_id` bigint NOT NULL COMMENT '目标方用户ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '关系状态：1好友 0待同意 2拉黑',
  `remark_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单向备注名',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关系建立时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_friend`(`user_id` ASC, `friend_id` ASC) USING BTREE COMMENT '确保两名玩家关系唯一防重',
  INDEX `friend_id`(`friend_id` ASC) USING BTREE,
  CONSTRAINT `friendships_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `friendships_ibfk_2` FOREIGN KEY (`friend_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 326 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '玩家好友关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of friendships
-- ----------------------------
INSERT INTO `friendships` VALUES (1, 9, 10, 1, 'ancy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (2, 9, 11, 1, 'dylan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (3, 9, 12, 1, 'eden', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (4, 9, 13, 1, 'chrissy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (5, 9, 15, 1, 'iris', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (6, 9, 17, 1, 'rain', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (7, 9, 18, 1, 'tancy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (8, 9, 19, 1, 'yuna', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (9, 9, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (10, 9, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (11, 9, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (12, 9, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (13, 9, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (14, 9, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (15, 9, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (16, 9, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (17, 9, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (18, 9, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (19, 9, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (20, 9, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (21, 9, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (22, 9, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (23, 9, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (24, 9, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (25, 9, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (26, 10, 11, 1, 'dylan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (27, 10, 12, 1, 'eden', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (28, 10, 13, 1, 'chrissy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (29, 10, 15, 1, 'iris', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (30, 10, 17, 1, 'rain', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (31, 10, 18, 1, 'tancy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (32, 10, 19, 1, 'yuna', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (33, 10, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (34, 10, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (35, 10, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (36, 10, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (37, 10, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (38, 10, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (39, 10, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (40, 10, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (41, 10, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (42, 10, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (43, 10, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (44, 10, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (45, 10, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (46, 10, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (47, 10, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (48, 10, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (49, 10, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (50, 11, 12, 1, 'eden', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (51, 11, 13, 1, 'chrissy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (52, 11, 15, 1, 'iris', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (53, 11, 17, 1, 'rain', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (54, 11, 18, 1, 'tancy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (55, 11, 19, 1, 'yuna', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (56, 11, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (57, 11, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (58, 11, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (59, 11, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (60, 11, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (61, 11, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (62, 11, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (63, 11, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (64, 11, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (65, 11, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (66, 11, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (67, 11, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (68, 11, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (69, 11, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (70, 11, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (71, 11, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (72, 11, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (73, 12, 13, 1, 'chrissy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (74, 12, 15, 1, 'iris', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (75, 12, 17, 1, 'rain', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (76, 12, 18, 1, 'tancy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (77, 12, 19, 1, 'yuna', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (78, 12, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (79, 12, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (80, 12, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (81, 12, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (82, 12, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (83, 12, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (84, 12, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (85, 12, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (86, 12, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (87, 12, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (88, 12, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (89, 12, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (90, 12, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (91, 12, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (92, 12, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (93, 12, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (94, 12, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (95, 13, 15, 1, 'iris', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (96, 13, 17, 1, 'rain', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (97, 13, 18, 1, 'tancy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (98, 13, 19, 1, 'yuna', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (99, 13, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (100, 13, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (101, 13, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (102, 13, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (103, 13, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (104, 13, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (105, 13, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (106, 13, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (107, 13, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (108, 13, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (109, 13, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (110, 13, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (111, 13, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (112, 13, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (113, 13, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (114, 13, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (115, 13, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (116, 15, 17, 1, 'rain', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (117, 15, 18, 1, 'tancy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (118, 15, 19, 1, 'yuna', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (119, 15, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (120, 15, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (121, 15, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (122, 15, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (123, 15, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (124, 15, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (125, 15, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (126, 15, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (127, 15, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (128, 15, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (129, 15, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (130, 15, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (131, 15, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (132, 15, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (133, 15, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (134, 15, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (135, 15, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (136, 17, 18, 1, 'tancy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (137, 17, 19, 1, 'yuna', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (138, 17, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (139, 17, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (140, 17, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (141, 17, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (142, 17, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (143, 17, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (144, 17, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (145, 17, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (146, 17, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (147, 17, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (148, 17, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (149, 17, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (150, 17, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (151, 17, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (152, 17, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (153, 17, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (154, 17, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (155, 18, 19, 1, 'yuna', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (156, 18, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (157, 18, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (158, 18, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (159, 18, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (160, 18, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (161, 18, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (162, 18, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (163, 18, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (164, 18, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (165, 18, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (166, 18, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (167, 18, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (168, 18, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (169, 18, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (170, 18, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (171, 18, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (172, 18, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (173, 19, 20, 1, 'hermione', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (174, 19, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (175, 19, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (176, 19, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (177, 19, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (178, 19, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (179, 19, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (180, 19, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (181, 19, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (182, 19, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (183, 19, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (184, 19, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (185, 19, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (186, 19, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (187, 19, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (188, 19, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (189, 19, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (190, 20, 21, 1, 'kinyond', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (191, 20, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (192, 20, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (193, 20, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (194, 20, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (195, 20, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (196, 20, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (197, 20, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (198, 20, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (199, 20, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (200, 20, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (201, 20, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (202, 20, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (203, 20, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (204, 20, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (205, 20, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (206, 21, 23, 1, 'duane', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (207, 21, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (208, 21, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (209, 21, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (210, 21, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (211, 21, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (212, 21, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (213, 21, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (214, 21, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (215, 21, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (216, 21, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (217, 21, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (218, 21, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (219, 21, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (220, 21, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (221, 23, 24, 1, 'amy', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (222, 23, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (223, 23, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (224, 23, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (225, 23, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (226, 23, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (227, 23, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (228, 23, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (229, 23, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (230, 23, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (231, 23, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (232, 23, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (233, 23, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (234, 23, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (235, 24, 25, 1, 'carl', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (236, 24, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (237, 24, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (238, 24, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (239, 24, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (240, 24, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (241, 24, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (242, 24, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (243, 24, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (244, 24, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (245, 24, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (246, 24, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (247, 24, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (248, 25, 26, 1, 'colin', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (249, 25, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (250, 25, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (251, 25, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (252, 25, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (253, 25, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (254, 25, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (255, 25, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (256, 25, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (257, 25, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (258, 25, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (259, 25, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (260, 26, 27, 1, 'kade', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (261, 26, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (262, 26, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (263, 26, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (264, 26, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (265, 26, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (266, 26, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (267, 26, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (268, 26, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (269, 26, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (270, 26, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (271, 27, 28, 1, 'nico', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (272, 27, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (273, 27, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (274, 27, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (275, 27, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (276, 27, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (277, 27, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (278, 27, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (279, 27, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (280, 27, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (281, 28, 29, 1, 'felicity', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (282, 28, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (283, 28, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (284, 28, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (285, 28, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (286, 28, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (287, 28, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (288, 28, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (289, 28, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (290, 29, 30, 1, 'riley', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (291, 29, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (292, 29, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (293, 29, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (294, 29, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (295, 29, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (296, 29, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (297, 29, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (298, 30, 31, 1, 'daniel', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (299, 30, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (300, 30, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (301, 30, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (302, 30, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (303, 30, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (304, 30, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (305, 31, 32, 1, 'charlene', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (306, 31, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (307, 31, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (308, 31, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (309, 31, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (310, 31, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (311, 32, 33, 1, 'ethan', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (312, 32, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (313, 32, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (314, 32, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (315, 32, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (316, 33, 34, 1, 'harry', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (317, 33, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (318, 33, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (319, 33, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (320, 34, 35, 1, 'sandra', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (321, 34, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (322, 34, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (323, 35, 44, 1, 'harrison', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (324, 35, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');
INSERT INTO `friendships` VALUES (325, 44, 45, 1, 'leo', '2026-08-04 17:47:49', '2026-08-04 17:47:49');

-- ----------------------------
-- Table structure for game_rooms
-- ----------------------------
DROP TABLE IF EXISTS `game_rooms`;
CREATE TABLE `game_rooms`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房间主键',
  `room_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '6位房间邀请码',
  `host_user_id` bigint NOT NULL COMMENT '房主ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '房间状态：0等待中 1准备 2对战中 3结束',
  `player_count` int NOT NULL DEFAULT 0 COMMENT '当前房间内人数',
  `max_players` int NOT NULL DEFAULT 2 COMMENT '最大人数限制',
  `match_id` bigint NULL DEFAULT NULL COMMENT '关联的进行中/已结束对局ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态更新时间',
  `closed_at` datetime NULL DEFAULT NULL COMMENT '房间销毁关闭时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_room_code`(`room_code` ASC) USING BTREE COMMENT '房间码唯一，用于加房搜索',
  UNIQUE INDEX `uk_room_match_id`(`match_id` ASC) USING BTREE COMMENT '防止同一对局被多个房间绑定',
  INDEX `host_user_id`(`host_user_id` ASC) USING BTREE,
  CONSTRAINT `game_rooms_ibfk_1` FOREIGN KEY (`host_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `game_rooms_ibfk_2` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 375 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组队游戏房间表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of game_rooms
-- ----------------------------

-- ----------------------------
-- Table structure for match_actions
-- ----------------------------
DROP TABLE IF EXISTS `match_actions`;
CREATE TABLE `match_actions`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '行动主键',
  `match_id` bigint NOT NULL COMMENT '对局ID',
  `round_id` bigint NULL DEFAULT NULL COMMENT '回合ID(关联具体回合)',
  `actor_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行者身份：player / boss / system',
  `actor_user_id` bigint NULL DEFAULT NULL COMMENT '如果执行者是玩家，记录玩家ID',
  `action_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为指令：play_card/discard/boss_attack等',
  `card_id` bigint NULL DEFAULT NULL COMMENT '如是打牌行为，关联具体卡牌ID',
  `target_user_id` bigint NULL DEFAULT NULL COMMENT '如是针对性技能(恢复队友)，关联目标ID',
  `before_value` int NULL DEFAULT NULL COMMENT '变化前的属性值',
  `after_value` int NULL DEFAULT NULL COMMENT '变化后的属性值',
  `delta_value` int NULL DEFAULT NULL COMMENT '最终产生的变化量(加减值)',
  `extra_data` json NULL COMMENT '行为附加信息，如持续效果附着',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行动发生时间',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_match_round_actor`(`match_id` ASC, `round_id` ASC, `actor_user_id` ASC) USING BTREE COMMENT '按回合与发起人检索出牌时间轴',
  INDEX `round_id`(`round_id` ASC) USING BTREE,
  INDEX `actor_user_id`(`actor_user_id` ASC) USING BTREE,
  INDEX `card_id`(`card_id` ASC) USING BTREE,
  INDEX `target_user_id`(`target_user_id` ASC) USING BTREE,
  CONSTRAINT `match_actions_ibfk_1` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `match_actions_ibfk_2` FOREIGN KEY (`round_id`) REFERENCES `match_rounds` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `match_actions_ibfk_3` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `match_actions_ibfk_4` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `match_actions_ibfk_5` FOREIGN KEY (`target_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5995 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '游戏内每一手出牌与扣血流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of match_actions
-- ----------------------------

-- ----------------------------
-- Table structure for match_cards
-- ----------------------------
DROP TABLE IF EXISTS `match_cards`;
CREATE TABLE `match_cards`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '本局卡牌实例主键',
  `match_id` bigint NOT NULL COMMENT '所属对局ID',
  `match_player_id` bigint NOT NULL COMMENT '所属对局玩家记录ID',
  `user_id` bigint NOT NULL COMMENT '卡牌所属用户ID，便于鉴权和查询',
  `card_id` bigint NOT NULL COMMENT '对应的卡牌模板ID',
  `zone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DECK' COMMENT '卡牌区域：DECK/HAND/DISCARD/EXHAUST',
  `deck_order` int NULL DEFAULT NULL COMMENT '牌堆顺序，数值越小越先抽；离开牌堆后置空',
  `drawn_round` int NULL DEFAULT NULL COMMENT '最近一次抽到手牌的回合号',
  `discarded_round` int NULL DEFAULT NULL COMMENT '最近一次进入弃牌堆的回合号',
  `version` int NOT NULL DEFAULT 0 COMMENT '卡牌实例版本号，用于并发更新',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_match_card_owner_zone`(`match_id` ASC, `user_id` ASC, `zone` ASC, `deck_order` ASC) USING BTREE COMMENT '查询玩家在本局各区域的卡牌',
  INDEX `idx_match_card_player_zone`(`match_player_id` ASC, `zone` ASC) USING BTREE,
  INDEX `idx_match_card_template`(`card_id` ASC) USING BTREE,
  INDEX `fk_match_cards_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_match_cards_card` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_match_cards_match` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_match_cards_player` FOREIGN KEY (`match_player_id`) REFERENCES `match_players` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_match_cards_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_match_card_zone` CHECK (`zone` in (_utf8mb4'DECK',_utf8mb4'HAND',_utf8mb4'DISCARD',_utf8mb4'EXHAUST'))
) ENGINE = InnoDB AUTO_INCREMENT = 11481 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '本局卡牌实例及牌堆/手牌/弃牌状态表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of match_cards
-- ----------------------------

-- ----------------------------
-- Table structure for match_pending_effects
-- ----------------------------
DROP TABLE IF EXISTS `match_pending_effects`;
CREATE TABLE `match_pending_effects`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `match_id` bigint NOT NULL,
  `match_player_id` bigint NOT NULL,
  `source_user_id` bigint NOT NULL,
  `source_card_instance_id` bigint NOT NULL,
  `effect_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '待执行效果类型',
  `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标类型：BOSS/SELF/PLAYER',
  `target_user_id` bigint NULL DEFAULT NULL,
  `effect_value` int NOT NULL DEFAULT 0 COMMENT '实际待执行数值，已包含创建时的倍率',
  `trigger_round` int NOT NULL DEFAULT 0 COMMENT '应执行的对局回合号',
  `remaining_triggers` int NOT NULL DEFAULT 1 COMMENT '剩余触发次数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RESOLVED/CANCELLED',
  `extra_data` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mpe_match_round`(`match_id` ASC, `trigger_round` ASC) USING BTREE,
  INDEX `idx_mpe_match_player`(`match_player_id` ASC) USING BTREE,
  INDEX `idx_mpe_source_card`(`source_card_instance_id` ASC) USING BTREE,
  INDEX `idx_mpe_status_round`(`match_id` ASC, `status` ASC, `trigger_round` ASC) USING BTREE,
  CONSTRAINT `fk_mpe_match` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_mpe_player` FOREIGN KEY (`match_player_id`) REFERENCES `match_players` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 846 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of match_pending_effects
-- ----------------------------

-- ----------------------------
-- Table structure for match_players
-- ----------------------------
DROP TABLE IF EXISTS `match_players`;
CREATE TABLE `match_players`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对局玩家记录主键',
  `match_id` bigint NOT NULL COMMENT '归属对局ID',
  `user_id` bigint NOT NULL COMMENT '玩家用户ID',
  `seat_no` tinyint NOT NULL COMMENT '座位号(1/2)',
  `dept_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实际参赛职业(销售部/采购部)',
  `max_hp` int NOT NULL DEFAULT 0 COMMENT '玩家最大血量：销售部15，采购部20',
  `current_hp` int NOT NULL DEFAULT 0 COMMENT '玩家当前血量',
  `shield` int NOT NULL DEFAULT 0 COMMENT '本回合当前防御值',
  `base_action_points` int NOT NULL DEFAULT 3 COMMENT '每回合基础员工调用机会',
  `action_points` int NOT NULL DEFAULT 3 COMMENT '本回合剩余员工调用机会',
  `ended_turn` tinyint NOT NULL DEFAULT 0 COMMENT '本回合是否已点击结束：0否 1是',
  `player_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '玩家战斗状态：ACTIVE/RECONNECTING/DEAD/LEFT',
  `revive_count` int NOT NULL DEFAULT 0 COMMENT '本局已复活次数',
  `revive_limit` int NOT NULL DEFAULT 1 COMMENT '本局允许的最大复活次数',
  `last_revive_at` datetime NULL DEFAULT NULL COMMENT '最近一次复活时间',
  `revive_status` tinyint NOT NULL DEFAULT 0 COMMENT '复活状态：0不可复活 1可复活 2已复活',
  `initial_confidence` int NOT NULL DEFAULT 0 COMMENT '旧规则初始信心值，第一阶段兼容保留',
  `final_confidence` int NOT NULL DEFAULT 0 COMMENT '旧规则最终信心值，第一阶段兼容保留',
  `initial_funds` int NOT NULL DEFAULT 3 COMMENT '每回合初始发放资金(3点)',
  `total_funds_used` int NOT NULL DEFAULT 0 COMMENT '整局累计花掉的资金数',
  `cards_played_count` int NOT NULL DEFAULT 0 COMMENT '整局打出卡牌数',
  `damage_dealt` int NOT NULL DEFAULT 0 COMMENT '对BOSS做出的输出(提满意度或降怒气)',
  `damage_taken` int NOT NULL DEFAULT 0 COMMENT '承受了BOSS多少伤害',
  `healing_done` int NOT NULL DEFAULT 0 COMMENT '整局累计有效治疗量',
  `shield_granted` int NOT NULL DEFAULT 0 COMMENT '整局累计产生的防御值',
  `result_type` tinyint NOT NULL DEFAULT 0 COMMENT '个人结算：0未结算 1胜利 2失败 3逃跑中断',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_match_player_user`(`match_id` ASC, `user_id` ASC) USING BTREE COMMENT '同一玩家在同一对局中只能有一条状态记录',
  UNIQUE INDEX `uk_match_player_seat`(`match_id` ASC, `seat_no` ASC) USING BTREE COMMENT '同一对局座位不可重复',
  INDEX `idx_match_user`(`match_id` ASC, `user_id` ASC) USING BTREE COMMENT '按对局和用户联合查询个人战绩',
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `match_players_ibfk_1` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `match_players_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 643 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '玩家单局最终战绩表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of match_players
-- ----------------------------

-- ----------------------------
-- Table structure for match_replays
-- ----------------------------
DROP TABLE IF EXISTS `match_replays`;
CREATE TABLE `match_replays`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回放主键',
  `match_id` bigint NOT NULL COMMENT '对局ID',
  `replay_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '回放文件类型：local / cloud',
  `replay_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '回放数据存放地址/URL',
  `replay_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件完整性校验哈希',
  `data_size` int NOT NULL DEFAULT 0 COMMENT '文件大小(bytes)',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1.0' COMMENT '用于解析回放的客户端版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_match_replay`(`match_id` ASC) USING BTREE COMMENT '保证一局只有一个最终录像关联',
  CONSTRAINT `match_replays_ibfk_1` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对局录像回放索引表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of match_replays
-- ----------------------------

-- ----------------------------
-- Table structure for match_revive_logs
-- ----------------------------
DROP TABLE IF EXISTS `match_revive_logs`;
CREATE TABLE `match_revive_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `match_id` bigint NOT NULL COMMENT '对局ID',
  `round_no` int NOT NULL COMMENT '回合号',
  `user_id` bigint NOT NULL COMMENT '复活玩家ID',
  `before_hp` int NOT NULL COMMENT '复活前血量',
  `after_hp` int NOT NULL COMMENT '复活后血量',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1成功 0失败',
  `ad_platform` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'manual' COMMENT '广告平台/来源',
  `ad_request_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '广告请求ID',
  `ad_callback_raw` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '广告回调原文',
  `verify_status` tinyint NOT NULL DEFAULT 1 COMMENT '验证状态：1通过 0未通过',
  `revive_reason` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '复活原因或备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ad_request_id`(`ad_request_id` ASC) USING BTREE,
  INDEX `idx_match_user_round`(`match_id` ASC, `user_id` ASC, `round_no` ASC) USING BTREE,
  INDEX `idx_match_id`(`match_id` ASC) USING BTREE,
  INDEX `match_revive_logs_ibfk_2`(`user_id` ASC) USING BTREE,
  CONSTRAINT `match_revive_logs_ibfk_1` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `match_revive_logs_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '广告复活日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of match_revive_logs
-- ----------------------------

-- ----------------------------
-- Table structure for match_rounds
-- ----------------------------
DROP TABLE IF EXISTS `match_rounds`;
CREATE TABLE `match_rounds`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回合记录主键',
  `match_id` bigint NOT NULL COMMENT '对局ID',
  `round_no` int NOT NULL COMMENT '当前第几个回合',
  `first_player_user_id` bigint NULL DEFAULT NULL COMMENT '本回合先手玩家用户ID',
  `chosen_by_user_id` bigint NULL DEFAULT NULL COMMENT '本回合先手选择发起人用户ID',
  `round_status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0进行中 1已结束',
  `phase` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PLAYER_ACTION' COMMENT '回合阶段：PLAYER_ACTION/BOSS_ACTION/ROUND_SETTLEMENT/FINISHED',
  `boss_attack` int NOT NULL DEFAULT 0 COMMENT '本回合霸凌者最终攻击力',
  `customer_triggered` tinyint NOT NULL DEFAULT 0 COMMENT '本回合顾客效果是否触发：0否 1是',
  `customer_effect_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本回合实际判定的顾客效果类型',
  `customer_effect_value` int NOT NULL DEFAULT 0 COMMENT '本回合顾客效果数值',
  `boss_rage_value` int NOT NULL DEFAULT 0 COMMENT '本回合BOSS累计的怒气值',
  `satisfaction_delta` int NOT NULL DEFAULT 0 COMMENT '本回合满意度总体变化',
  `funds_per_player` int NOT NULL DEFAULT 3 COMMENT '本回合玩家拥有的可分配资金',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本回合开始时间',
  `ended_at` datetime NULL DEFAULT NULL COMMENT '本回合结束(含结算)时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录生成时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_match_round_no`(`match_id` ASC, `round_no` ASC) USING BTREE COMMENT '同一对局的回合号不可重复',
  INDEX `idx_match_round`(`match_id` ASC, `round_no` ASC) USING BTREE COMMENT '加速查找特定对局的特定回合进度',
  CONSTRAINT `match_rounds_ibfk_1` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 863 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对局内回合进度与BOSS阶段表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of match_rounds
-- ----------------------------

-- ----------------------------
-- Table structure for matches
-- ----------------------------
DROP TABLE IF EXISTS `matches`;
CREATE TABLE `matches`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对局主键',
  `match_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '全局唯一对局编号',
  `room_id` bigint NULL DEFAULT NULL COMMENT '从哪个房间发起的对战',
  `customer_type_id` bigint NULL DEFAULT NULL COMMENT '本局顾客类型ID',
  `bully_id` bigint NULL DEFAULT NULL COMMENT '本局霸凌者ID',
  `boss_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前对战BOSS名称',
  `boss_satisfaction_target` int NOT NULL DEFAULT 0 COMMENT '旧规则胜利阈值，第一阶段兼容保留',
  `boss_initial_satisfaction` int NOT NULL DEFAULT 0 COMMENT '旧规则初始满意度，第一阶段兼容保留',
  `boss_final_satisfaction` int NOT NULL DEFAULT 0 COMMENT '旧规则最终满意度，第一阶段兼容保留',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '对局状态：0初始化中 1进行中 2已结束 3已中断',
  `phase` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前阶段：INITIALIZING/SELECT_FIRST_PLAYER/PLAYER_ACTION/BOSS_ACTION/ROUND_SETTLEMENT/RECONNECT_WAIT/FINISHED',
  `current_round` int NOT NULL DEFAULT 1 COMMENT '当前回合数',
  `boss_max_hp` int NOT NULL DEFAULT 50 COMMENT '本局霸凌者最大血量快照',
  `boss_current_hp` int NOT NULL DEFAULT 50 COMMENT '霸凌者当前血量',
  `boss_base_attack` int NOT NULL DEFAULT 5 COMMENT '本局霸凌者基础攻击快照',
  `boss_current_attack` int NOT NULL DEFAULT 5 COMMENT '霸凌者本回合最终攻击力',
  `winner_type` tinyint NOT NULL DEFAULT 0 COMMENT '对局结果：0未结束 1玩家胜 2玩家负 3异常中断',
  `version` bigint NOT NULL DEFAULT 0 COMMENT '对局状态版本号，用于并发控制和前端状态同步',
  `duration_seconds` int NOT NULL DEFAULT 0 COMMENT '对局耗时(秒)',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '对局开始时间',
  `ended_at` datetime NULL DEFAULT NULL COMMENT '对局结束时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录生成时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_match_code`(`match_code` ASC) USING BTREE COMMENT '对战流水号唯一索引',
  UNIQUE INDEX `uk_match_room`(`room_id` ASC) USING BTREE COMMENT '第一阶段规定一个房间只能创建一局对局',
  INDEX `idx_room_time`(`room_id` ASC, `started_at` ASC) USING BTREE COMMENT '用于按房间和时间排查历史对局',
  INDEX `fk_matches_customer_type`(`customer_type_id` ASC) USING BTREE,
  INDEX `fk_matches_bully`(`bully_id` ASC) USING BTREE,
  INDEX `idx_match_status_updated`(`status` ASC, `updated_at` ASC) USING BTREE COMMENT '按状态扫描进行中或异常对局',
  CONSTRAINT `fk_matches_bully` FOREIGN KEY (`bully_id`) REFERENCES `bullies` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_matches_customer_type` FOREIGN KEY (`customer_type_id`) REFERENCES `customer_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_matches_room` FOREIGN KEY (`room_id`) REFERENCES `game_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 356 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '完整对局主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of matches
-- ----------------------------

-- ----------------------------
-- Table structure for room_invites
-- ----------------------------
DROP TABLE IF EXISTS `room_invites`;
CREATE TABLE `room_invites`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '邀请主键',
  `from_user_id` bigint NOT NULL COMMENT '邀请发起人ID',
  `to_user_id` bigint NOT NULL COMMENT '被邀请人ID',
  `room_id` bigint NULL DEFAULT NULL COMMENT '接受后创建的房间ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待处理 1已接受 2已拒绝 3已超时',
  `responded_at` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `expired_at` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_room_invite_from_user`(`from_user_id` ASC) USING BTREE,
  INDEX `idx_room_invite_to_user`(`to_user_id` ASC) USING BTREE,
  INDEX `idx_room_invite_status`(`status` ASC) USING BTREE,
  INDEX `room_id`(`room_id` ASC) USING BTREE,
  CONSTRAINT `room_invites_ibfk_1` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `room_invites_ibfk_2` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `room_invites_ibfk_3` FOREIGN KEY (`room_id`) REFERENCES `game_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 483 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房间邀请记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of room_invites
-- ----------------------------

-- ----------------------------
-- Table structure for room_members
-- ----------------------------
DROP TABLE IF EXISTS `room_members`;
CREATE TABLE `room_members`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员主键',
  `room_id` bigint NOT NULL COMMENT '所在房间ID',
  `user_id` bigint NOT NULL COMMENT '玩家用户ID',
  `seat_no` tinyint NOT NULL COMMENT '座位号：1或2',
  `dept_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '已选择的职业：销售部/采购部',
  `ready_status` tinyint NOT NULL DEFAULT 0 COMMENT '准备状态：1已准备 0未准备',
  `online_status` tinyint NOT NULL DEFAULT 1 COMMENT '在线/掉线状态：1在线 0掉线',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入房间时间',
  `left_at` datetime NULL DEFAULT NULL COMMENT '离开房间时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '信息更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_room_user`(`room_id` ASC, `user_id` ASC) USING BTREE COMMENT '确保同房间用户不可重复加入',
  UNIQUE INDEX `uk_room_seat`(`room_id` ASC, `seat_no` ASC) USING BTREE COMMENT '确保同房间座位号不被抢占',
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `room_members_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `game_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `room_members_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 749 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房间内部成员与准备状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of room_members
-- ----------------------------

-- ----------------------------
-- Table structure for system_configs
-- ----------------------------
DROP TABLE IF EXISTS `system_configs`;
CREATE TABLE `system_configs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '系统配置主键',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键名',
  `config_value` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
  `config_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置类型：string/number/json/boolean',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置详细说明',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE COMMENT '配置键唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统全局配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_configs
-- ----------------------------
INSERT INTO `system_configs` VALUES (1, 'boss.satisfaction.min', '80', 'number', '胜利满意度下限', 1, '2026-07-20 18:25:23', '2026-07-20 18:25:23');
INSERT INTO `system_configs` VALUES (2, 'boss.satisfaction.max', '150', 'number', '胜利满意度上限', 1, '2026-07-20 18:25:23', '2026-07-20 18:25:23');
INSERT INTO `system_configs` VALUES (3, 'player.fundsPerRound', '3', 'number', '每回合资金', 1, '2026-07-20 18:25:23', '2026-07-20 18:25:23');
INSERT INTO `system_configs` VALUES (4, 'combat.mode', 'customer_bully', 'string', '战斗模式：customer_bully 表示当前使用顾客+霸凌者结构', 1, '2026-07-22 13:04:31', '2026-07-22 13:04:31');
INSERT INTO `system_configs` VALUES (5, 'combat.customer.count', '3', 'number', '顾客固定类型数量', 1, '2026-07-22 13:04:31', '2026-07-22 13:04:31');
INSERT INTO `system_configs` VALUES (6, 'combat.bully.count', '1', 'number', '当前霸凌者模板数量', 1, '2026-07-22 13:04:31', '2026-07-22 13:04:31');
INSERT INTO `system_configs` VALUES (7, 'combat.bully.baseHp', '50', 'number', '霸凌者初始血量', 1, '2026-07-22 13:04:31', '2026-07-22 13:04:31');

-- ----------------------------
-- Table structure for tasks
-- ----------------------------
DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  `task_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务唯一编码',
  `task_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `task_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型：daily/growth/event/achievement',
  `reset_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NONE' COMMENT '任务重置类型：NONE=不重置/DAILY=每日重置/WEEKLY=每周重置/MONTHLY=每月重置',
  `period_scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ALL' COMMENT '任务统计周期范围：ALL=不限周期/DAY=按自然日/WEEK=按自然周/MONTH=按自然月',
  `progress_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'COUNT' COMMENT '任务进度类型：COUNT=次数累计/WIN_COUNT=胜场累计/LOSE_COUNT=败场累计/MATCH_COUNT=对局数累计/TEAM_COUNT=组队次数/DISTINCT_TEAMMATE_COUNT=不同队友数',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务描述',
  `condition_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '完成条件类型，如 win_count/play_card/login_days',
  `condition_value` json NOT NULL COMMENT '完成条件参数(JSON)',
  `reward_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖励类型，如 money/exp/card/item',
  `reward_value` json NOT NULL COMMENT '奖励内容(JSON)',
  `target_count` int NOT NULL DEFAULT 1 COMMENT '目标完成次数',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_code`(`task_code` ASC) USING BTREE COMMENT '任务编码唯一索引',
  INDEX `idx_task_type_status`(`task_type` ASC, `status` ASC) USING BTREE COMMENT '任务类型与状态查询索引'
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tasks
-- ----------------------------
INSERT INTO `tasks` VALUES (1, 'T-LOGIN-001', '每日登录', 'daily', 'NONE', 'ALL', 'COUNT', '每日登录一次即可完成', 'login_count', '{\"days\": 1}', 'money', '{\"amount\": 100}', 1, 1, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `tasks` VALUES (2, 'T-BATTLE-001', '完成一场对局', 'daily', 'NONE', 'ALL', 'COUNT', '完成任意一场对局', 'match_count', '{\"count\": 1}', 'exp', '{\"amount\": 50}', 1, 2, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `tasks` VALUES (3, 'T-CARD-001', '使用卡牌', 'growth', 'NONE', 'ALL', 'COUNT', '累计使用 10 张卡牌', 'card_play_count', '{\"count\": 10}', 'money', '{\"amount\": 200}', 10, 3, 1, '2026-07-22 11:43:29', '2026-07-22 11:43:29');
INSERT INTO `tasks` VALUES (4, 'T-FRIEND-001', '添加好友', 'event', 'NONE', 'ALL', 'COUNT', '成功添加 1 位好友', 'friend_count', '{\"count\": 1}', 'item', '{\"count\": 1, \"itemCode\": \"FRIEND_COIN\"}', 1, 4, 0, '2026-07-22 11:43:29', '2026-07-23 18:54:28');
INSERT INTO `tasks` VALUES (5, 'T-DAILY-WIN-001', '每日首3局赢1局', 'daily', 'DAILY', 'DAY', 'WIN_COUNT', '每日首3局内赢1局即可完成', 'WIN_COUNT', '{\"count\": 1, \"within_games\": 3}', 'money', '{\"amount\": 50}', 1, 1, 1, '2026-07-30 11:51:31', '2026-07-30 11:51:31');
INSERT INTO `tasks` VALUES (6, 'T-DAILY-LOSE-001', '每日首3局输1局', 'daily', 'DAILY', 'DAY', 'LOSE_COUNT', '每日首3局内输1局即可完成', 'LOSE_COUNT', '{\"count\": 1, \"within_games\": 3}', 'money', '{\"amount\": 25}', 1, 2, 1, '2026-07-30 11:51:31', '2026-07-30 11:51:31');
INSERT INTO `tasks` VALUES (7, 'T-WEEKLY-TEAM-005', '每周跟5位不同同事组队', 'weekly', 'WEEKLY', 'WEEK', 'DISTINCT_TEAMMATE_COUNT', '每周与5位不同同事组过队即可完成', 'DISTINCT_TEAMMATE_COUNT', '{\"count\": 5}', 'money', '{\"amount\": 250}', 5, 3, 1, '2026-07-30 11:51:31', '2026-07-30 11:51:31');
INSERT INTO `tasks` VALUES (8, 'T-WEEKLY-TEAM-010', '每周跟10位不同同事组队', 'weekly', 'WEEKLY', 'WEEK', 'DISTINCT_TEAMMATE_COUNT', '每周与10位不同同事组过队即可完成', 'DISTINCT_TEAMMATE_COUNT', '{\"count\": 10}', 'money', '{\"amount\": 250}', 10, 4, 1, '2026-07-30 11:51:31', '2026-07-30 11:51:31');
INSERT INTO `tasks` VALUES (9, 'T-WEEKLY-TEAM-015', '每周跟15位不同同事组队', 'weekly', 'WEEKLY', 'WEEK', 'DISTINCT_TEAMMATE_COUNT', '每周与15位不同同事组过队即可完成', 'DISTINCT_TEAMMATE_COUNT', '{\"count\": 15}', 'money', '{\"amount\": 250}', 15, 5, 1, '2026-07-30 11:51:31', '2026-07-30 11:51:31');
INSERT INTO `tasks` VALUES (10, 'T-MONTHLY-PLAY-015', '每月跟15位不同同事玩过', 'monthly', 'MONTHLY', 'MONTH', 'DISTINCT_TEAMMATE_COUNT', '每月与15位不同同事玩过即可完成', 'DISTINCT_TEAMMATE_COUNT', '{\"count\": 15}', 'money', '{\"amount\": 750}', 15, 6, 1, '2026-07-30 11:51:31', '2026-07-30 11:51:31');

-- ----------------------------
-- Table structure for user_achievements
-- ----------------------------
DROP TABLE IF EXISTS `user_achievements`;
CREATE TABLE `user_achievements`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成就记录主键',
  `user_id` bigint NOT NULL COMMENT '解锁玩家ID',
  `achievement_id` bigint NOT NULL COMMENT '对应成就定义ID',
  `progress_value` int NOT NULL DEFAULT 0 COMMENT '当前积累进度',
  `unlock_status` tinyint NOT NULL DEFAULT 0 COMMENT '解锁状态：1已解锁 0未解锁',
  `unlocked_at` datetime NULL DEFAULT NULL COMMENT '实际达成解锁的时间',
  `claimed_status` tinyint NOT NULL DEFAULT 0 COMMENT '奖励领取状态：1已领 0未领',
  `claimed_at` datetime NULL DEFAULT NULL COMMENT '领取奖励的时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '进度更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_achievement`(`user_id` ASC, `achievement_id` ASC) USING BTREE COMMENT '防止用户重复拥有同一成就',
  INDEX `achievement_id`(`achievement_id` ASC) USING BTREE,
  CONSTRAINT `user_achievements_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_achievements_ibfk_2` FOREIGN KEY (`achievement_id`) REFERENCES `achievement_defs` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户成就解锁明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_achievements
-- ----------------------------

-- ----------------------------
-- Table structure for user_profiles
-- ----------------------------
DROP TABLE IF EXISTS `user_profiles`;
CREATE TABLE `user_profiles`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '档案主键',
  `user_id` bigint NOT NULL COMMENT '归属用户ID',
  `display_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '游戏内展示昵称',
  `signature` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个性签名',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别：1男 2女 0未知',
  `level` int NOT NULL DEFAULT 1 COMMENT '玩家等级',
  `exp` int NOT NULL DEFAULT 0 COMMENT '玩家当前经验值',
  `win_count` int NOT NULL DEFAULT 0 COMMENT '历史胜场数',
  `lose_count` int NOT NULL DEFAULT 0 COMMENT '历史败场数',
  `draw_count` int NOT NULL DEFAULT 0 COMMENT '平局或未完成局数',
  `money` bigint NOT NULL DEFAULT 0 COMMENT '用户酬劳/余额(单位：元或游戏内积分，按业务定义)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE COMMENT '确保每个用户只有一份扩展资料',
  CONSTRAINT `user_profiles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户扩展资料库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_profiles
-- ----------------------------
INSERT INTO `user_profiles` VALUES (9, 9, 'amber', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (10, 10, 'ancy', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (11, 11, 'dylan', '风是抓不住的', 0, 1, 30, 0, 1, 0, 10, '2026-07-21 20:00:43', '2026-08-04 17:52:08');
INSERT INTO `user_profiles` VALUES (12, 12, 'eden', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (13, 13, 'chrissy', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (15, 15, 'iris', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (17, 17, 'rain', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (18, 18, 'tancy', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (19, 19, 'yuna', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (20, 20, 'hermione', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (21, 21, 'kinyond', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-07-21 20:00:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (24, 24, 'amy', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (25, 25, 'carl', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (26, 32, 'charlene', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (27, 26, 'colin', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (28, 31, 'daniel', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (29, 23, 'duane', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (30, 33, 'ethan', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (31, 29, 'felicity', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (32, 44, 'harrsion', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (33, 34, 'harry', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (34, 27, 'kade', NULL, 0, 1, 30, 0, 1, 0, 10, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (35, 45, 'leo', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (36, 28, 'nico', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (37, 30, 'riley', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');
INSERT INTO `user_profiles` VALUES (38, 35, 'sandra', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-08-04 17:36:43', '2026-08-04 17:36:43');

-- ----------------------------
-- Table structure for user_tasks
-- ----------------------------
DROP TABLE IF EXISTS `user_tasks`;
CREATE TABLE `user_tasks`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户任务记录主键',
  `user_id` bigint NOT NULL COMMENT '任务所属用户ID',
  `task_id` bigint NOT NULL COMMENT '对应任务ID',
  `period_key` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务周期标识，如 2026-07-29、2026-W30、2026-07',
  `progress_value` int NOT NULL DEFAULT 0 COMMENT '当前进度值',
  `target_value` int NOT NULL DEFAULT 0 COMMENT '任务目标值快照',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态：0未开始 1进行中 2已完成 3已领取',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '任务完成时间',
  `claimed_at` datetime NULL DEFAULT NULL COMMENT '奖励领取时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_task_period`(`user_id` ASC, `task_id` ASC, `period_key` ASC) USING BTREE COMMENT '同一用户同一任务同一周期只能有一条记录',
  INDEX `idx_user_task_user_status`(`user_id` ASC, `status` ASC) USING BTREE COMMENT '按用户和任务状态查询',
  INDEX `idx_user_task_task_id`(`task_id` ASC) USING BTREE COMMENT '按任务ID查询用户任务记录',
  CONSTRAINT `fk_user_tasks_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_tasks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 217 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户任务进度表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_tasks
-- ----------------------------

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码哈希值',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像地址',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态：1正常 0封禁',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最近一次登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE COMMENT '用户名防重唯一索引',
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE COMMENT '邮箱防重唯一索引',
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE COMMENT '手机号防重唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户基础账号表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (9, 'amber', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 18:29:55', '2026-07-21 20:00:23', '2026-07-22 12:30:56');
INSERT INTO `users` VALUES (10, 'ancy', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 18:52:47', '2026-07-21 20:00:23', '2026-07-22 12:31:05');
INSERT INTO `users` VALUES (11, 'dylan', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-06 11:29:57', '2026-07-21 20:00:23', '2026-07-24 11:40:48');
INSERT INTO `users` VALUES (12, 'eden', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 18:32:24', '2026-07-21 20:00:23', '2026-07-27 14:11:15');
INSERT INTO `users` VALUES (13, 'chrissy', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-04 16:50:13', '2026-07-21 20:00:23', '2026-07-27 14:11:13');
INSERT INTO `users` VALUES (15, 'iris', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-04 16:55:47', '2026-07-21 20:00:23', '2026-07-27 14:12:03');
INSERT INTO `users` VALUES (17, 'rain', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 19:06:29', '2026-07-21 20:00:23', '2026-07-26 12:17:29');
INSERT INTO `users` VALUES (18, 'tancy', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-04 16:53:13', '2026-07-21 20:00:23', '2026-07-27 14:12:07');
INSERT INTO `users` VALUES (19, 'yuna', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 19:25:17', '2026-07-21 20:00:23', '2026-07-27 14:12:08');
INSERT INTO `users` VALUES (20, 'hermione', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 19:08:45', '2026-07-21 20:00:23', '2026-07-27 14:12:10');
INSERT INTO `users` VALUES (21, 'kinyond', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 18:57:29', '2026-07-21 20:00:23', '2026-07-27 14:12:12');
INSERT INTO `users` VALUES (23, 'duane', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-06 11:30:19', '2026-07-27 00:00:00', '2026-07-27 14:12:14');
INSERT INTO `users` VALUES (24, 'amy', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-06 11:13:09', '2026-07-27 00:00:00', '2026-07-27 14:12:20');
INSERT INTO `users` VALUES (25, 'carl', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 18:48:53', '2026-07-27 00:00:00', '2026-07-27 14:12:18');
INSERT INTO `users` VALUES (26, 'colin', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-05 22:35:02', '2026-07-27 00:00:00', '2026-07-27 14:12:22');
INSERT INTO `users` VALUES (27, 'kade', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-06 11:15:23', '2026-07-27 00:00:00', '2026-07-27 14:12:23');
INSERT INTO `users` VALUES (28, 'nico', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 19:13:56', '2026-07-27 00:00:00', '2026-07-27 14:12:25');
INSERT INTO `users` VALUES (29, 'felicity', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 18:50:13', '2026-07-27 00:00:00', '2026-07-27 14:12:27');
INSERT INTO `users` VALUES (30, 'riley', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 19:26:00', '2026-07-27 00:00:00', '2026-07-27 14:12:29');
INSERT INTO `users` VALUES (31, 'daniel', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 18:54:52', '2026-07-27 00:00:00', '2026-07-27 14:12:31');
INSERT INTO `users` VALUES (32, 'charlene', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 19:10:05', '2026-07-27 00:00:00', '2026-07-27 14:12:33');
INSERT INTO `users` VALUES (33, 'ethan', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 18:52:12', '2026-07-27 00:00:00', '2026-07-27 14:12:35');
INSERT INTO `users` VALUES (34, 'harry', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 19:01:37', '2026-07-27 00:00:00', '2026-07-27 14:12:38');
INSERT INTO `users` VALUES (35, 'sandra', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, '2026-08-03 19:09:20', '2026-07-27 00:00:00', '2026-07-27 14:12:40');
INSERT INTO `users` VALUES (44, 'harrison', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, NULL, '2026-08-04 17:29:58', '2026-08-04 17:44:16');
INSERT INTO `users` VALUES (45, 'leo', '$2a$10$ZPo.RgnsqZlH2uWUEpjNAuRwMlGiQrQuCY3FJkd47Qj1cWoBoszqC', NULL, NULL, NULL, 1, NULL, '2026-08-04 17:30:15', '2026-08-04 17:30:15');

SET FOREIGN_KEY_CHECKS = 1;
