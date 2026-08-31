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
    { userId: 2, username: 'li', displayName: '销冠小李', money: 380, winRate: 80, winCount: 8, loseCount: 2, level: 4, rank: 1 },
    { userId: 1, username: 'wang', displayName: '外贸达人小王', money: 520, winRate: 70, winCount: 7, loseCount: 3, level: 5, rank: 2 },
    { userId: 3, username: 'player_c', displayName: '玩家C', money: 210, winRate: 60, winCount: 3, loseCount: 2, level: 3, rank: 3 },
  ],
  winrate: [
    { userId: 2, username: 'li', displayName: '销冠小李', money: 380, winRate: 80, winCount: 8, loseCount: 2, level: 4, rank: 1 },
    { userId: 1, username: 'wang', displayName: '外贸达人小王', money: 520, winRate: 70, winCount: 7, loseCount: 3, level: 5, rank: 2 },
    { userId: 3, username: 'player_c', displayName: '玩家C', money: 210, winRate: 60, winCount: 3, loseCount: 2, level: 3, rank: 3 },
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
  { id: 'q1', icon: 'attack', name: '完成第 1 局', description: '今天打完第 1 局即可，输赢都算', progress: 0, target: 1, reward: 30, type: 'daily' as const },
  { id: 'q2', icon: 'trophy', name: '赢第 1 局', description: '今天第 1 局获胜，看广告复活也算', progress: 0, target: 1, reward: 10, type: 'daily' as const },
  { id: 'q3', icon: 'attack', name: '完成第 2 局', description: '今天打完第 2 局即可，输赢都算', progress: 0, target: 1, reward: 40, type: 'daily' as const },
  { id: 'q4', icon: 'trophy', name: '赢第 2 局', description: '今天第 2 局获胜，看广告复活也算', progress: 0, target: 1, reward: 10, type: 'daily' as const },
  { id: 'q5', icon: 'attack', name: '完成第 3 局', description: '今天打完第 3 局即可，输赢都算', progress: 0, target: 1, reward: 50, type: 'daily' as const },
  { id: 'q6', icon: 'trophy', name: '赢第 3 局', description: '今天第 3 局获胜，看广告复活也算', progress: 0, target: 1, reward: 10, type: 'daily' as const },
  { id: 'q7', icon: 'promotion', name: '跟 10 位不同同事组合', description: '本周在每日前 3 局里，和 10 个不同的人组过队', progress: 0, target: 10, reward: 500, type: 'weekly' as const },
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
  { id: 1, taskCode: 'T-DAILY-MATCH-1', taskName: '完成第 1 局', taskType: 'daily', description: '今天打完第 1 局即可，输赢都算', conditionType: 'match_count', conditionValue: '{}', rewardType: 'money', rewardValue: '{"amount":30}', targetCount: 1, sortNo: 10, status: 1 },
  { id: 2, taskCode: 'T-DAILY-WIN-1', taskName: '赢第 1 局', taskType: 'daily', description: '今天第 1 局获胜，看广告复活也算', conditionType: 'match_slot_win', conditionValue: '{"slot":1}', rewardType: 'money', rewardValue: '{"amount":10}', targetCount: 1, sortNo: 11, status: 1 },
  { id: 3, taskCode: 'T-DAILY-MATCH-2', taskName: '完成第 2 局', taskType: 'daily', description: '今天打完第 2 局即可，输赢都算', conditionType: 'match_count', conditionValue: '{}', rewardType: 'money', rewardValue: '{"amount":40}', targetCount: 2, sortNo: 20, status: 1 },
  { id: 4, taskCode: 'T-DAILY-WIN-2', taskName: '赢第 2 局', taskType: 'daily', description: '今天第 2 局获胜，看广告复活也算', conditionType: 'match_slot_win', conditionValue: '{"slot":2}', rewardType: 'money', rewardValue: '{"amount":10}', targetCount: 1, sortNo: 21, status: 1 },
  { id: 5, taskCode: 'T-DAILY-MATCH-3', taskName: '完成第 3 局', taskType: 'daily', description: '今天打完第 3 局即可，输赢都算', conditionType: 'match_count', conditionValue: '{}', rewardType: 'money', rewardValue: '{"amount":50}', targetCount: 3, sortNo: 30, status: 1 },
  { id: 6, taskCode: 'T-DAILY-WIN-3', taskName: '赢第 3 局', taskType: 'daily', description: '今天第 3 局获胜，看广告复活也算', conditionType: 'match_slot_win', conditionValue: '{"slot":3}', rewardType: 'money', rewardValue: '{"amount":10}', targetCount: 1, sortNo: 31, status: 1 },
  { id: 7, taskCode: 'T-WEEKLY-TEAM-10', taskName: '跟 10 位不同同事组合', taskType: 'weekly', description: '本周在每日前 3 局里，和 10 个不同的人组过队', conditionType: 'distinct_teammate', conditionValue: '{}', rewardType: 'money', rewardValue: '{"amount":500}', targetCount: 10, sortNo: 90, status: 1 },
]

// ==================== Game Config ====================
export const mockGameConfig = {
  turnFunds: 3,
  initialStamina: 100,
  maxBullyHP: 110,
  handSize: 5,
  bossMinDamage: 5,
  bossMaxDamage: 14,
  bullyMinDamage: 5,
  bullyMaxDamage: 14,
}

// ==================== Departments ====================
export const mockDepts = ['财务部', '人事部', '采购部', '销售部', 'IT部', '设计部', '行政部', '物流部']
