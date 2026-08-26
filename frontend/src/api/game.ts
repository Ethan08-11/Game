/**
 * 游戏相关 API：卡牌、游戏配置、结算
 */

import type { Card } from '@/utils/cardData'
import type { EmployerTrait } from '@/types/gameEntities'
import { apiCall } from './client'

// ---------- 图鉴卡牌（后端 API 格式）----------
export interface ApiCard {
  id: number
  cardId?: number
  cardCode: string
  cardName: string
  deptType: string
  cost: number | null
  cardType: string | null
  confidenceChange?: number
  satisfactionChange?: number
  rageChange?: number
  shieldChange?: number
  description: string
  imageUrl: string | null
  isUnique: number
  status: number
  requireUnlock?: number
  unlocked?: boolean
}

export async function fetchCardList(): Promise<ApiCard[]> {
  const list = await apiCall<any[]>('/cards')
  return (list ?? []).map((item) => {
    const unlocked = item.unlocked == null
      ? Number(item.requireUnlock ?? 0) !== 1
      : Boolean(item.unlocked)
    return {
      ...item,
      id: Number(item.id ?? item.cardId ?? 0),
      unlocked,
    }
  })
}

// ---------- 卡牌 ----------
export async function getAllCards(): Promise<Card[]> {
  return apiCall('/game/cards')
}

export async function getCardsByDept(dept: string): Promise<Card[]> {
  return apiCall(`/game/cards?dept=${encodeURIComponent(dept)}`)
}

// ---------- 配置 ----------
export interface GameConfig {
  turnFunds: number
  initialStamina: number
  maxBullyHP: number
  handSize: number
  bossMinDamage: number
  bossMaxDamage: number
  bullyMinDamage?: number
  bullyMaxDamage?: number
}

export async function getGameConfig(): Promise<GameConfig> {
  return apiCall('/game/config')
}

// ---------- 部门 ----------
export async function getDepartments(): Promise<string[]> {
  return apiCall('/game/departments')
}

export interface CustomerApiItem {
  customerTypeId?: number | string
  customerCode?: string
  customerName?: string
  description?: string
  effectType?: string
  effectValue?: number
  selectionWeight?: number
  triggerChance?: number
  status?: number
  imageUrl?: string | null
}

function normalizeRate(value?: number) {
  if (value == null) return undefined
  return value > 1 ? value / 100 : value
}

function normalizeEffectType(customer: CustomerApiItem): EmployerTrait['effectType'] {
  const raw = `${customer.effectType || ''}`
  if (/player_hp|heal_player/i.test(raw)) return 'player_hp'
  if (/hp|血/i.test(raw)) return 'hp'
  return 'attack'
}

export function transformCustomer(customer: CustomerApiItem): EmployerTrait {
  const typeTriggerRate = normalizeRate(customer.selectionWeight) ?? 0.4
  const effectTriggerRate = normalizeRate(customer.triggerChance) ?? typeTriggerRate
  return {
    id: String(customer.customerTypeId ?? customer.customerCode ?? ''),
    name: customer.customerName ?? '顾客',
    description: customer.description ?? '',
    helpChance: typeTriggerRate,
    helpMin: Math.abs(customer.effectValue ?? 0),
    helpMax: Math.abs(customer.effectValue ?? 0),
    hinderMin: Math.abs(customer.effectValue ?? 0),
    hinderMax: Math.abs(customer.effectValue ?? 0),
    effectType: normalizeEffectType(customer),
    effectValue: customer.effectValue ?? 0,
    typeTriggerRate,
    effectTriggerRate,
    imageUrl: customer.imageUrl ?? null,
  }
}

export async function getCurrentCustomer(): Promise<EmployerTrait> {
  return apiCall<CustomerApiItem>('/customers/current').then(transformCustomer)
}

export async function getCustomers(): Promise<EmployerTrait[]> {
  return apiCall<CustomerApiItem[]>('/customers').then((customers) => customers.map(transformCustomer))
}

export async function getCustomerCatalog(): Promise<CustomerApiItem[]> {
  return apiCall('/customers')
}

export async function getCustomerCatalogRaw(): Promise<{ code?: number; message?: string; data?: CustomerApiItem[]; success?: boolean }> {
  return apiCall('/customers')
}

// ---------- 游戏结算 ----------
export interface GameResultPayload {
  totalDamage: number
  player1Stamina: number
  player2Stamina: number
  rounds: number
  isVictory: boolean
}

export interface GameResultResponse {
  pointsEarned: number
}

export async function submitGameResult(payload: GameResultPayload): Promise<GameResultResponse> {
  return apiCall('/game/result', { method: 'POST', body: payload })
}
