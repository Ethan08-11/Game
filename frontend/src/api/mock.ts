/**
 * 集中式 Mock 数据存储
 * 后期对接后端时，删除本文件并将各 api 模块中的 USE_MOCK 改为 false 即可
 */

// ==================== Auth ====================
export function mockLogin(username: string) {
  return {
    token: `mock-token-${username}-${Date.now()}`,
    user: { username, avatar: '' },
  }
}

export function mockRegister(username: string) {
  return {
    token: `mock-token-${username}-${Date.now()}`,
    user: { username, avatar: '' },
  }
}

// ==================== Friends ====================
export const mockFriends = [
  { id: 'f1', username: 'wang', displayName: '外贸达人小王', remarkName: '小王', avatarUrl: null, online: true },
  { id: 'f2', username: 'li', displayName: '销冠小李', remarkName: '小李', avatarUrl: null, online: true },
  { id: 'f3', username: 'zhang', displayName: '物流老张', remarkName: '老张', avatarUrl: null, online: false },
  { id: 'f4', username: 'alin', displayName: '财务阿琳', remarkName: '阿琳', avatarUrl: null, online: true },
  { id: 'f5', username: 'qiang', displayName: 'IT阿强', remarkName: '阿强', avatarUrl: null, online: false },
]

// ==================== Leaderboard ====================
export const mockLeaderboard = {
  total: [
    { userId: 1, username: 'wang', displayName: '外贸达人小王', money: 520, level: 5, rank: 1 },
    { userId: 2, username: 'li', displayName: '销冠小李', money: 380, level: 4, rank: 2 },
    { userId: 3, username: 'player_c', displayName: '玩家C', money: 210, level: 3, rank: 3 },
    { userId: 4, username: 'zhang', displayName: '物流老张', money: 175, level: 3, rank: 4 },
    { userId: 5, username: 'qiang', displayName: 'IT阿强', money: 140, level: 2, rank: 5 },
  ],
  weekly: [
    { userId: 2, username: 'li', displayName: '销冠小李', money: 95, level: 4, rank: 1 },
    { userId: 1, username: 'wang', displayName: '外贸达人小王', money: 80, level: 5, rank: 2 },
    { userId: 3, username: 'player_c', displayName: '玩家C', money: 60, level: 3, rank: 3 },
  ],
}

// ==================== Skins ====================
export const mockSkins = [
  { id: 'skin_1', name: '经典卡背', price: 0, locked: false, preview: 'tickets' },
  { id: 'skin_2', name: '金色卡背', price: 100, locked: true, preview: 'tickets' },
  { id: 'skin_3', name: '暗夜卡背', price: 200, locked: true, preview: 'tickets' },
  { id: 'skin_4', name: '海洋卡背', price: 150, locked: true, preview: 'tickets' },
  { id: 'skin_5', name: '烈焰主题', price: 300, locked: true, preview: 'promotion' },
  { id: 'skin_6', name: '星空主题', price: 250, locked: true, preview: 'magic' },
]

// ==================== Quests ====================
export const mockQuests = [
  { id: 'q1', icon: 'attack', name: '每日对战', description: '完成2场对战', progress: 1, target: 2, reward: 30, type: 'daily' as const },
  { id: 'q2', icon: 'promotion', name: '组队达人', description: '与好友组队完成对战', progress: 0, target: 1, reward: 50, type: 'daily' as const },
  { id: 'q3', icon: 'trophy', name: '签约高手', description: '单次对战胜率满意度达到100', progress: 0, target: 1, reward: 100, type: 'weekly' as const },
  { id: 'q4', icon: 'box', name: '卡牌收藏家', description: '收集20张不同卡牌', progress: 10, target: 20, reward: 80, type: 'weekly' as const },
  { id: 'q5', icon: 'star', name: '社交达人', description: '添加5位好友', progress: 2, target: 5, reward: 60, type: 'weekly' as const },
]

// ==================== Cards（数据源: utils/cardData.ts） ====================
export { allCards as mockCards } from '@/utils/cardData'

// ==================== 图鉴卡牌（API 格式兜底数据） ====================
import type { ApiCard } from './game'

export const mockCardList: ApiCard[] = [
  { id: 59, cardCode: 'S-01', cardName: '破冰礼遇', deptType: 'sales', cost: 0, cardType: 'attack', confidenceChange: 0, satisfactionChange: 2, rageChange: 0, shieldChange: 0, description: '立即+2满意度', imageUrl: null, isUnique: 0, status: 1 },
  { id: 60, cardCode: 'S-02', cardName: '尊享矩阵', deptType: 'sales', cost: 1, cardType: 'attack', confidenceChange: 0, satisfactionChange: 3, rageChange: 0, shieldChange: 0, description: '立即+3满意度', imageUrl: null, isUnique: 0, status: 1 },
  { id: 61, cardCode: 'S-03', cardName: '羁绊重塑', deptType: 'sales', cost: 2, cardType: 'attack', confidenceChange: 0, satisfactionChange: 5, rageChange: 0, shieldChange: 0, description: '本回合+3，下回合+2满意度', imageUrl: null, isUnique: 0, status: 1 },
  { id: 65, cardCode: 'P-01', cardName: '危机斡旋', deptType: 'purchase', cost: 0, cardType: 'defend', confidenceChange: 0, satisfactionChange: 0, rageChange: -2, shieldChange: 0, description: '立即-2怒气值', imageUrl: null, isUnique: 0, status: 1 },
  { id: 66, cardCode: 'P-02', cardName: '闪电溯源', deptType: 'purchase', cost: 1, cardType: 'defend', confidenceChange: 0, satisfactionChange: 0, rageChange: -3, shieldChange: 0, description: '立即-3怒气值', imageUrl: null, isUnique: 0, status: 1 },
  { id: 75, cardCode: 'O-01', cardName: '紧急拨付', deptType: 'purchase', cost: 1, cardType: 'defend', confidenceChange: 0, satisfactionChange: 0, rageChange: -3, shieldChange: 0, description: '怒气值-3', imageUrl: null, isUnique: 0, status: 1 },
  { id: 69, cardCode: 'O-07', cardName: '系统热修复', deptType: 'public', cost: 0, cardType: 'attack', confidenceChange: 0, satisfactionChange: 2, rageChange: 0, shieldChange: 0, description: '+2满意度', imageUrl: null, isUnique: 0, status: 1 },
  { id: 84, cardCode: 'L-01', cardName: '自来水好评', deptType: 'neutral', cost: 2, cardType: 'attack', confidenceChange: 0, satisfactionChange: 0, rageChange: 0, shieldChange: 0, description: '本回合资金+2，下回合资金+1', imageUrl: null, isUnique: 0, status: 1 },
]

// ==================== 任务（API 格式兜底数据） ====================
import type { ApiTask } from './social'

export const mockApiTasks: ApiTask[] = [
  { id: 1, taskCode: 'T-LOGIN-001', taskName: '每日登录', taskType: 'daily', description: '每日登录一次即可完成', conditionType: 'login_count', conditionValue: '{"days": 1}', rewardType: 'money', rewardValue: '{"amount": 100}', targetCount: 1, sortNo: 1, status: 1 },
  { id: 2, taskCode: 'T-BATTLE-001', taskName: '完成一场对局', taskType: 'daily', description: '完成任意一场对局', conditionType: 'match_count', conditionValue: '{"count": 1}', rewardType: 'exp', rewardValue: '{"amount": 50}', targetCount: 1, sortNo: 2, status: 1 },
  { id: 3, taskCode: 'T-CARD-001', taskName: '使用卡牌', taskType: 'growth', description: '累计使用 10 张卡牌', conditionType: 'card_play_count', conditionValue: '{"count": 10}', rewardType: 'money', rewardValue: '{"amount": 200}', targetCount: 10, sortNo: 3, status: 1 },
  { id: 4, taskCode: 'T-FRIEND-001', taskName: '添加好友', taskType: 'event', description: '成功添加 1 位好友', conditionType: 'friend_count', conditionValue: '{"count": 1}', rewardType: 'item', rewardValue: '{"count": 1, "itemCode": "FRIEND_COIN"}', targetCount: 1, sortNo: 4, status: 1 },
]

// ==================== Game Config ====================
export const mockGameConfig = {
  turnFunds: 3,
  initialStamina: 100,
  maxBullyHP: 200,
  handSize: 5,
  bossMinDamage: 5,
  bossMaxDamage: 14,
  bullyMinDamage: 5,
  bullyMaxDamage: 14,
}

// ==================== Departments ====================
export const mockDepts = ['财务部', '人事部', '采购部', '销售部', 'IT部', '设计部', '行政部', '物流部']
