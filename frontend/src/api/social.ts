/**
 * 社交/商城相关 API：皮肤商店、任务系统
 */

import { apiCall } from './client'

// ---------- 任务（后端 API 格式）----------
export interface ApiTask {
  id: number
  taskCode: string
  taskName: string
  taskType: 'daily' | 'growth' | 'event'
  description: string
  conditionType: string
  conditionValue: string
  rewardType: 'money' | 'exp' | 'item'
  rewardValue: string
  targetCount: number
  sortNo: number
  status: number
}

export async function fetchTasks(): Promise<ApiTask[]> {
  return apiCall('/tasks')
}

// ---------- 皮肤 ----------
export interface Skin {
  id: string
  name: string
  price: number
  locked: boolean
  preview: string
}

export async function getSkins(): Promise<Skin[]> {
  return apiCall('/shop/skins')
}

export async function purchaseSkin(skinId: string): Promise<Skin | null> {
  return apiCall(`/shop/skins/${skinId}/purchase`, { method: 'POST' })
}

// ---------- 任务 ----------
export interface Quest {
  id: string
  icon: string
  name: string
  description: string
  progress: number
  target: number
  reward: number
  type: 'daily' | 'weekly'
}

export async function getQuests(): Promise<Quest[]> {
  return apiCall('/quests')
}

export async function claimQuestReward(questId: string): Promise<number> {
  return apiCall(`/quests/${questId}/claim`, { method: 'POST' })
}
