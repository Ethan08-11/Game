/**
 * 用户相关 API：好友、积分、成就
 */

import { apiCall } from './client'
import { formatPlayerName } from '@/utils/playerName'

export type PresenceStatus = 'OFFLINE' | 'IDLE' | 'IN_ROOM' | 'IN_MATCH'

// ---------- types ----------
interface BackendFriend {
  id: number
  userId: number
  friendId: number
  status: number
  remarkName: string
  username: string
  displayName: string
  avatarUrl: string | null
  onlineStatus: number
  presenceStatus?: PresenceStatus | string
  invitable?: boolean
}

export interface UserProfile {
  id?: number | string
  userId?: number | string
  username?: string
  displayName?: string
  avatarUrl?: string | null
  email?: string | null
  phone?: string | null
  level?: number
  exp?: number
  money?: number
}

export interface UpdateUserProfilePayload {
  displayName?: string
  avatarUrl?: string | null
  email?: string | null
  phone?: string | null
}

export interface UserStats {
  userId?: number | string
  winCount?: number
  loseCount?: number
  drawCount?: number
  totalMatches?: number
  money?: number
  level?: number
  exp?: number
}

export interface FriendRequestPayload {
  friendId?: string | number
  targetUserId?: string | number
  username?: string
}

export interface Friend {
  id: string
  username: string
  displayName: string
  remarkName: string
  avatarUrl: string | null
  online: boolean
  presenceStatus: PresenceStatus
  invitable: boolean
  /** true if presenceStatus was from server, false if derived from onlineStatus fallback */
  presenceFromApi?: boolean
}

function normalizePresence(status?: string | null, _onlineStatus?: number): PresenceStatus {
  const value = String(status || '').toUpperCase()
  if (value === 'OFFLINE' || value === 'IDLE' || value === 'IN_ROOM' || value === 'IN_MATCH') {
    return value
  }
  // Server didn't provide explicit presence — default to IDLE.
  // onlineStatus is unreliable (often returns 0 even for connected users).
  // When the user is truly OFFLINE, the server must send presenceStatus: "OFFLINE".
  return 'IDLE'
}

function transformFriend(bf: BackendFriend): Friend {
  const hasExplicitPresence = typeof bf.presenceStatus === 'string' && bf.presenceStatus.length > 0
  const presenceStatus = normalizePresence(bf.presenceStatus, bf.onlineStatus)
  const invitable = typeof bf.invitable === 'boolean'
    ? bf.invitable
    : presenceStatus === 'IDLE'
  return {
    id: String(bf.friendId),
    username: formatPlayerName(bf.username),
    displayName: formatPlayerName(bf.displayName || bf.username),
    remarkName: bf.remarkName,
    avatarUrl: bf.avatarUrl,
    online: presenceStatus !== 'OFFLINE',
    presenceStatus,
    invitable,
    presenceFromApi: hasExplicitPresence,
  }
}

export interface Achievement {
  id: string
  name: string
  description: string
  unlockedAt: string | null
  icon: string
  difficulty: number
  progressValue: number
  targetCount: number
  category: string
  hidden: boolean
  conditionType?: string
  sortNo?: number
}

interface BackendAchievement {
  id?: number | string
  achievementId?: number | string
  code?: string
  achievementCode?: string
  name?: string
  title?: string
  achievementName?: string
  description?: string
  achievementDesc?: string
  icon?: string
  iconUrl?: string
  unlockedAt?: string | null
  unlockTime?: string | null
  isUnlocked?: boolean | number
  unlocked?: boolean
  status?: number
  unlockStatus?: number
  category?: string
  difficulty?: number
  conditionType?: string
  conditionValue?: string
  progressValue?: number
  sortNo?: number
  targetCount?: number
}

function getAchievementKey(item: BackendAchievement): string {
  return String(item.achievementCode ?? item.code ?? item.achievementId ?? item.id ?? '')
}

function parseConditionCount(raw: string | undefined): number {
  if (!raw) return 1
  try {
    const parsed = JSON.parse(raw) as { count?: number; sales?: number }
    if (typeof parsed.count === 'number' && parsed.count > 0) return parsed.count
    if (typeof parsed.sales === 'number' && parsed.sales > 0) return parsed.sales
  } catch {
    return 1
  }
  return 1
}

function iconFor(category: string | undefined, hidden: boolean): string {
  if (hidden) return 'question'
  if (category === 'social') return 'star'
  if (category === 'growth') return 'medal'
  return 'trophy'
}

function transformAchievement(item: BackendAchievement, mine?: BackendAchievement): Achievement {
  const unlockedFlag = mine?.unlockStatus === 1 || mine?.unlocked === true || mine?.isUnlocked === true || mine?.isUnlocked === 1
  const unlockedAt = mine?.unlockedAt ?? mine?.unlockTime ?? item.unlockedAt ?? item.unlockTime
    ?? (unlockedFlag ? new Date().toISOString() : null)
  const hidden = (item.category || '').toLowerCase() === 'hidden'
  const targetCount = Number(item.targetCount) > 0
    ? Number(item.targetCount)
    : parseConditionCount(item.conditionValue)
  return {
    id: getAchievementKey(item),
    name: item.name ?? item.achievementName ?? item.title ?? '未命名成就',
    description: item.description ?? item.achievementDesc ?? '',
    icon: item.icon ?? item.iconUrl ?? iconFor(item.category, hidden),
    unlockedAt: unlockedAt || null,
    difficulty: Number(item.difficulty) > 0 ? Number(item.difficulty) : 1,
    progressValue: Math.max(0, Number(mine?.progressValue) || 0),
    targetCount,
    category: item.category || 'battle',
    hidden,
    conditionType: item.conditionType || '',
    sortNo: Number(item.sortNo) || 0,
  }
}

function mergeAchievements(all: BackendAchievement[], mine: BackendAchievement[]): Achievement[] {
  const mineByKey = new Map<string, BackendAchievement>()
  for (const item of mine) {
    mineByKey.set(getAchievementKey(item), item)
    if (item.achievementId != null) mineByKey.set(String(item.achievementId), item)
  }
  return all
    .map((item) => {
      const mineRow = mineByKey.get(getAchievementKey(item)) ?? mineByKey.get(String(item.id ?? ''))
      return transformAchievement(item, mineRow)
    })
    .sort(compareAchievementSlots)
}

/** 按目录序号占位，不用解锁状态或显示名排序，翻面不换位。 */
function compareAchievementSlots(a: Achievement, b: Achievement): number {
  const bySort = (a.sortNo || 0) - (b.sortNo || 0)
  if (bySort !== 0) return bySort
  return a.id.localeCompare(b.id)
}

// ---------- 用户资料 ----------
export async function getUserProfile(id: string | number): Promise<UserProfile> {
  return apiCall(`/users/${id}/profile`)
}

export async function updateMyProfile(payload: UpdateUserProfilePayload): Promise<UserProfile> {
  return apiCall('/users/me/profile', { method: 'PUT', body: payload })
}

export async function getUserStats(id: string | number): Promise<UserStats> {
  return apiCall(`/users/${id}/stats`)
}

// ---------- 好友 ----------
export async function getFriends(): Promise<Friend[]> {
  const list = await apiCall<BackendFriend[]>('/friends')
  return list.map(transformFriend)
}

export async function sendFriendRequest(payload: FriendRequestPayload): Promise<void> {
  return apiCall('/friends/request', { method: 'POST', body: payload })
}

export async function acceptFriendRequest(id: string | number): Promise<void> {
  return apiCall(`/friends/${id}/accept`, { method: 'PUT' })
}

export async function deleteFriend(id: string | number): Promise<void> {
  return apiCall(`/friends/${id}`, { method: 'DELETE' })
}

export async function addFriend(friend: Friend): Promise<Friend> {
  await sendFriendRequest({ friendId: friend.id, targetUserId: friend.id })
  return friend
}

// ---------- 积分 ----------
export async function getPoints(): Promise<number> {
  return apiCall('/user/points')
}

export async function addPoints(amount: number): Promise<number> {
  return apiCall('/user/points', { method: 'POST', body: { amount } })
}

// ---------- 成就 ----------
export async function getAchievements(): Promise<Achievement[]> {
  const [all, mine] = await Promise.all([
    apiCall<BackendAchievement[]>('/achievements'),
    apiCall<BackendAchievement[]>('/achievements/me'),
  ])
  return mergeAchievements(all, mine)
}

export async function unlockAchievement(id: string): Promise<Achievement | null> {
  return apiCall(`/user/achievements/${id}/unlock`, { method: 'POST' })
}

// ---------- 排行榜 ----------
interface BackendLeaderboardEntry {
  userId: number
  username: string
  displayName: string
  avatarUrl?: string | null
  money: number
  level?: number
  exp?: number
  winCount?: number
  loseCount?: number
  drawCount?: number
  winRate?: number
  rank: number
}

export type LeaderboardType = 'total' | 'winrate' | 'weekly'

export interface LeaderboardEntry {
  userId: number
  username: string
  displayName: string
  avatarUrl?: string | null
  money: number
  winRate: number
  winCount: number
  loseCount: number
  level: number
  rank: number
}

function transformLeaderboardEntry(be: BackendLeaderboardEntry): LeaderboardEntry {
  const winCount = Math.max(0, Number(be.winCount) || 0)
  const loseCount = Math.max(0, Number(be.loseCount) || 0)
  const games = winCount + loseCount + (be.drawCount ?? 0)
  const winRate = Number.isFinite(Number(be.winRate))
    ? Math.max(0, Number(be.winRate))
    : (games <= 0 ? 0 : Math.round((winCount * 10000) / games) / 100)
  return {
    userId: be.userId,
    username: formatPlayerName(be.username),
    displayName: formatPlayerName(be.displayName || be.username),
    avatarUrl: be.avatarUrl,
    money: be.money,
    winRate,
    winCount,
    loseCount,
    level: be.level ?? 1,
    rank: be.rank,
  }
}

export async function getLeaderboard(type: LeaderboardType = 'total', page = 1, size = 10000): Promise<LeaderboardEntry[]> {
  const list = await apiCall<BackendLeaderboardEntry[]>(`/leaderboard?type=${type}&page=${page}&size=${size}`)
  return (Array.isArray(list) ? list : []).map(transformLeaderboardEntry)
}

export async function getMyLeaderboardRank(type: LeaderboardType = 'total'): Promise<LeaderboardEntry> {
  const entry = await apiCall<BackendLeaderboardEntry>(`/leaderboard/me?type=${type}`)
  return transformLeaderboardEntry(entry)
}

export { normalizePresence }
