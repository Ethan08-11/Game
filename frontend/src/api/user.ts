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
}

function getAchievementKey(item: BackendAchievement): string {
  return String(item.id ?? item.achievementId ?? item.code ?? item.achievementCode ?? '')
}

function transformAchievement(item: BackendAchievement): Achievement {
  const unlocked = item.unlocked === true || item.isUnlocked === true || item.isUnlocked === 1
  return {
    id: getAchievementKey(item),
    name: item.name ?? item.achievementName ?? item.title ?? '未命名成就',
    description: item.description ?? item.achievementDesc ?? '',
    icon: item.icon ?? item.iconUrl ?? 'trophy',
    unlockedAt: item.unlockedAt ?? item.unlockTime ?? (unlocked ? new Date().toISOString() : null),
  }
}

function mergeAchievements(all: BackendAchievement[], mine: BackendAchievement[]): Achievement[] {
  const unlockedKeys = new Set(mine.map(getAchievementKey))
  return all.map((item) => {
    const achievement = transformAchievement(item)
    if (unlockedKeys.has(getAchievementKey(item)) && !achievement.unlockedAt) {
      achievement.unlockedAt = new Date().toISOString()
    }
    return achievement
  })
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
  rank: number
}

export interface LeaderboardEntry {
  userId: number
  username: string
  displayName: string
  avatarUrl?: string | null
  money: number
  level: number
  rank: number
}

function transformLeaderboardEntry(be: BackendLeaderboardEntry): LeaderboardEntry {
  return {
    userId: be.userId,
    username: formatPlayerName(be.username),
    displayName: formatPlayerName(be.displayName || be.username),
    avatarUrl: be.avatarUrl,
    money: be.money,
    level: be.level ?? 1,
    rank: be.rank,
  }
}

export async function getLeaderboard(_type: 'total' | 'weekly', page = 1, size = 20): Promise<LeaderboardEntry[]> {
  const list = await apiCall<BackendLeaderboardEntry[]>(`/leaderboard?page=${page}&size=${size}`)
  return list.map(transformLeaderboardEntry)
}

export async function getMyLeaderboardRank(): Promise<LeaderboardEntry> {
  const entry = await apiCall<BackendLeaderboardEntry>('/leaderboard/me')
  return transformLeaderboardEntry(entry)
}

export { normalizePresence }
