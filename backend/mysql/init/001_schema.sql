-- 基础结构说明
-- 这里建议放完整表结构导出文件
-- 可以直接从当前数据库导出 schema 后整理到本目录
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

 Date: 30/07/2026 18:48:26
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
-- Table structure for boss_configs
-- ----------------------------
DROP TABLE IF EXISTS `boss_configs`;
CREATE TABLE `boss_configs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'BOSS配置主键',
  `boss_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BOSS唯一编码',
  `boss_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BOSS名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'BOSS背景或描述',
  `base_satisfaction_target_min` int NOT NULL DEFAULT 80 COMMENT '基础胜利满意度下限',
  `base_satisfaction_target_max` int NOT NULL DEFAULT 150 COMMENT '基础胜利满意度上限',
  `initial_satisfaction` int NOT NULL DEFAULT 0 COMMENT '初始满意度',
  `initial_rage` int NOT NULL DEFAULT 0 COMMENT '初始怒气值',
  `boss_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'normal' COMMENT 'BOSS类型：normal/elite/final',
  `portrait_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'BOSS立绘图片地址',
  `effect_data` json NULL COMMENT 'BOSS额外机制配置(JSON)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_boss_code`(`boss_code` ASC) USING BTREE COMMENT 'BOSS编码唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'BOSS模板配置表' ROW_FORMAT = Dynamic;

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
-- Table structure for card_combo_rules
-- ----------------------------
DROP TABLE IF EXISTS `card_combo_rules`;
CREATE TABLE `card_combo_rules`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联动规则主键',
  `card_id` bigint NOT NULL COMMENT '触发卡牌ID',
  `combo_card_id` bigint NOT NULL COMMENT '被搭配卡牌ID',
  `combo_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联动命名',
  `effect_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联动效果描述',
  `bonus_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加成类型(如double)',
  `bonus_value` int NOT NULL DEFAULT 0 COMMENT '加成数值',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_card_combo`(`card_id` ASC, `combo_card_id` ASC) USING BTREE COMMENT '卡牌联动组合查询索引',
  INDEX `combo_card_id`(`combo_card_id` ASC) USING BTREE,
  CONSTRAINT `card_combo_rules_ibfk_1` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `card_combo_rules_ibfk_2` FOREIGN KEY (`combo_card_id`) REFERENCES `cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌联动机制表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌部门字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for card_effects
-- ----------------------------
DROP TABLE IF EXISTS `card_effects`;
CREATE TABLE `card_effects`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '效果配置主键',
  `card_id` bigint NOT NULL COMMENT '归属卡牌ID',
  `effect_order` int NOT NULL DEFAULT 1 COMMENT '效果结算顺序',
  `effect_scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标范围：BOSS/SELF/ANY_PLAYER/ALL_PLAYERS',
  `effect_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '效果类型：DAMAGE_BOSS/HEAL_PLAYER/ADD_SHIELD/ADD_ACTION_POINTS/MULTIPLY_NEXT_CARD/DRAW_CARDS/REDUCE_BOSS_ATTACK',
  `trigger_timing` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'IMMEDIATE' COMMENT '触发时机：IMMEDIATE/ROUND_START/NEXT_CARD',
  `trigger_delay` int NOT NULL DEFAULT 0 COMMENT '延迟回合数；0表示当前时机，1表示下一回合',
  `remaining_triggers` int NOT NULL DEFAULT 1 COMMENT '创建本局待处理效果时的初始触发次数',
  `stack_rule` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STACK' COMMENT '叠加规则：STACK/REPLACE/REFRESH',
  `duration_rounds` int NOT NULL DEFAULT 0 COMMENT '持续回合数；一次性效果为0',
  `value` int NOT NULL DEFAULT 0 COMMENT '效果基础数值',
  `target_rule` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标规则：BOSS/SELF/ANY_PLAYER/ALL_PLAYERS',
  `extra_data` json NULL COMMENT '其他效果机制JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_card_id`(`card_id` ASC) USING BTREE COMMENT '卡牌ID查询索引',
  CONSTRAINT `card_effects_ibfk_1` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 51 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌复杂效果配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for card_effects_bak_20260724
-- ----------------------------
DROP TABLE IF EXISTS `card_effects_bak_20260724`;
CREATE TABLE `card_effects_bak_20260724`  (
  `id` bigint NOT NULL DEFAULT 0 COMMENT '效果配置主键',
  `card_id` bigint NOT NULL COMMENT '归属卡牌ID',
  `effect_order` int NOT NULL DEFAULT 1 COMMENT '效果结算顺序',
  `effect_scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '生效范围：self/ally/enemy/all',
  `effect_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '效果类型：immediate/next_round/duration/passive',
  `duration_rounds` int NOT NULL DEFAULT 0 COMMENT '持续回合数',
  `value` int NOT NULL DEFAULT 0 COMMENT '效果作用数值',
  `target_rule` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标筛选规则',
  `extra_data` json NULL COMMENT '其他效果机制JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for card_effects_before_structured_20260724
-- ----------------------------
DROP TABLE IF EXISTS `card_effects_before_structured_20260724`;
CREATE TABLE `card_effects_before_structured_20260724`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '效果配置主键',
  `card_id` bigint NOT NULL COMMENT '归属卡牌ID',
  `effect_order` int NOT NULL DEFAULT 1 COMMENT '效果结算顺序',
  `effect_scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '生效范围：self/ally/enemy/all',
  `effect_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '效果类型：immediate/next_round/duration/passive',
  `trigger_timing` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'IMMEDIATE',
  `trigger_delay` int NOT NULL DEFAULT 0,
  `remaining_triggers` int NOT NULL DEFAULT 1,
  `stack_rule` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STACK',
  `duration_rounds` int NOT NULL DEFAULT 0 COMMENT '持续回合数',
  `value` int NOT NULL DEFAULT 0 COMMENT '效果作用数值',
  `target_rule` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标筛选规则',
  `extra_data` json NULL COMMENT '其他效果机制JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_card_id`(`card_id` ASC) USING BTREE COMMENT '卡牌ID查询索引'
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌复杂效果配置表' ROW_FORMAT = Dynamic;

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
  `require_unlock` tinyint NOT NULL DEFAULT 0 COMMENT '1需胜利解锁才可进入对局',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_card_code`(`card_code` ASC) USING BTREE COMMENT '卡牌编号唯一索引',
  INDEX `idx_dept_type_cost`(`dept_id` ASC, `card_type` ASC, `cost` ASC) USING BTREE COMMENT '用于按部门、类型和费用快速筛选卡牌的外键复合索引',
  CONSTRAINT `cards_ibfk_1` FOREIGN KEY (`dept_id`) REFERENCES `card_depts` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 90 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌基础信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cards_bak_20260724
-- ----------------------------
DROP TABLE IF EXISTS `cards_bak_20260724`;
CREATE TABLE `cards_bak_20260724`  (
  `id` bigint NOT NULL DEFAULT 0 COMMENT '卡牌主键',
  `card_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡牌唯一编号(如S-01)',
  `card_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡牌名称',
  `dept_id` bigint NOT NULL COMMENT '所属部门ID',
  `dept_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务分类冗余(如sales)',
  `cost` tinyint NOT NULL COMMENT '卡牌使用费用(资金)',
  `card_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡牌类型：attack/defend/draw/consume',
  `confidence_change` int NOT NULL DEFAULT 0 COMMENT '导致信心值变化的数值',
  `satisfaction_change` int NOT NULL DEFAULT 0 COMMENT '导致BOSS满意度变化的数值',
  `rage_change` int NOT NULL DEFAULT 0 COMMENT '导致BOSS怒气值变化的数值',
  `shield_change` int NOT NULL DEFAULT 0 COMMENT '产生的护盾值(备用)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡牌效果文本描述',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '卡牌卡面图片URL',
  `combo_card_id` bigint NULL DEFAULT NULL COMMENT '强关联连携卡牌ID',
  `is_unique` tinyint NOT NULL DEFAULT 0 COMMENT '是否唯一卡：1是 0否',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cards_before_structured_effects_20260724
-- ----------------------------
DROP TABLE IF EXISTS `cards_before_structured_effects_20260724`;
CREATE TABLE `cards_before_structured_effects_20260724`  (
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
  INDEX `idx_dept_type_cost`(`dept_id` ASC, `card_type` ASC, `cost` ASC) USING BTREE COMMENT '用于按部门、类型和费用快速筛选卡牌的外键复合索引'
) ENGINE = InnoDB AUTO_INCREMENT = 86 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '卡牌基础信息表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1702 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友备注表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 340 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '玩家好友关系表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 206 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组队游戏房间表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 939 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '游戏内每一手出牌与扣血流水表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 6361 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '本局卡牌实例及牌堆/手牌/弃牌状态表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 148 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 319 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '玩家单局最终战绩表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对局录像回放索引表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '广告复活日志表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 239 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对局内回合进度与BOSS阶段表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 160 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '完整对局主表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 270 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房间邀请记录表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 411 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房间内部成员与准备状态表' ROW_FORMAT = Dynamic;

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
-- Table structure for t_example
-- ----------------------------
DROP TABLE IF EXISTS `t_example`;
CREATE TABLE `t_example`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '示例名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '示例描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0-未删除，1-已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '示例表' ROW_FORMAT = Dynamic;

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
-- Table structure for user_card_pools
-- ----------------------------
DROP TABLE IF EXISTS `user_card_pools`;
CREATE TABLE `user_card_pools`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '卡池记录主键',
  `user_id` bigint NOT NULL COMMENT '拥有者用户ID',
  `card_id` bigint NOT NULL COMMENT '拥有的卡牌ID',
  `owned_count` int NOT NULL DEFAULT 0 COMMENT '拥有数量',
  `unlocked_status` tinyint NOT NULL DEFAULT 1 COMMENT '解锁状态：1已解锁',
  `level` int NOT NULL DEFAULT 1 COMMENT '个人卡牌养成等级(备用)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获取时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_card`(`user_id` ASC, `card_id` ASC) USING BTREE COMMENT '用户与卡牌防重唯一索引',
  INDEX `card_id`(`card_id` ASC) USING BTREE,
  CONSTRAINT `user_card_pools_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_card_pools_ibfk_2` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户已获取/解锁的卡牌表' ROW_FORMAT = Dynamic;

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
  `weekly_money` bigint NOT NULL DEFAULT 0 COMMENT '本周获得金币，每周一0点清零',
  `login_streak` int NOT NULL DEFAULT 0 COMMENT '连续登录天数，断一天清零',
  `last_task_login_date` date NULL DEFAULT NULL COMMENT '上次计入任务的登录自然日(Asia/Shanghai)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE COMMENT '确保每个用户只有一份扩展资料',
  CONSTRAINT `user_profiles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户扩展资料库表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户任务进度表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户基础账号表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
