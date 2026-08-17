import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as api from '@/api'
import { disconnectRoomSocket } from '@/utils/roomSocket'
import { formatPlayerName } from '@/utils/playerName'

export interface Friend {
  id: string
  username: string
  displayName: string
  remarkName: string
  avatarUrl: string | null
  online: boolean
  presenceStatus: api.PresenceStatus
  invitable: boolean
}

export interface Achievement {
  id: string
  name: string
  description: string
  unlockedAt: string | null
  icon: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userId = ref<string>(localStorage.getItem('userId') || '')
  const username = ref<string>(formatPlayerName(localStorage.getItem('loginUsername') || ''))
  const avatar = ref<string>('')
  const friends = ref<Friend[]>([])
  const points = ref<number>(0)
  const money = ref<number>(0)
  const achievements = ref<Achievement[]>([])
  const profile = ref<api.UserProfile | null>(null)
  const stats = ref<api.UserStats | null>(null)
  const myLeaderboardRank = ref<api.LeaderboardEntry | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  // ---------- 好友 ----------
  async function loadFriends() {
    try {
      const fresh = await api.getFriends()
      if (friends.value.length === 0) {
        friends.value = fresh
        return
      }
      // Merge: keep WebSocket-set IN_MATCH/IN_ROOM when REST API has no explicit presence
      const oldMap = new Map(friends.value.map(f => [String(f.id), f]))
      friends.value = fresh.map(f => {
        const old = oldMap.get(String(f.id))
        // API didn't provide presence → defaulted to IDLE. Keep WebSocket-set status.
        if (old && !f.presenceFromApi && old.presenceStatus !== 'IDLE' && old.presenceStatus !== 'OFFLINE') {
          f.presenceStatus = old.presenceStatus
          f.invitable = old.invitable
          f.online = true
        }
        return f
      })
    } catch { /* keep existing */ }
  }

  async function addFriend(friend: Friend) {
    try {
      const result = await api.addFriend(friend)
      if (!friends.value.find(f => f.id === result.id)) {
        friends.value.push(result)
      }
    } catch {
      if (!friends.value.find(f => f.id === friend.id)) {
        friends.value.push(friend)
      }
    }
  }

  function updateFriendOnline(id: string, online: boolean) {
    const f = friends.value.find(x => String(x.id) === String(id))
    if (f) {
      f.online = online
      f.presenceStatus = online ? 'IDLE' : 'OFFLINE'
      f.invitable = online
    }
  }

  function updateFriendPresence(id: string, presenceStatus: Friend['presenceStatus'], invitable: boolean) {
    const f = friends.value.find(x => String(x.id) === String(id))
    if (f) {
      f.presenceStatus = presenceStatus
      f.invitable = invitable
      f.online = presenceStatus !== 'OFFLINE'
    }
  }

  async function sendFriendRequest(payload: api.FriendRequestPayload) {
    await api.sendFriendRequest(payload)
  }

  async function acceptFriendRequest(id: string | number) {
    await api.acceptFriendRequest(id)
    await loadFriends()
  }

  async function deleteFriend(id: string | number) {
    await api.deleteFriend(id)
    friends.value = friends.value.filter(friend => friend.id !== String(id))
  }

  // ---------- 积分 ----------
  async function addUserPoints(amount: number) {
    try {
      const updated = await api.addPoints(amount)
      points.value = updated
    } catch {
      points.value += amount
    }
  }

  async function loadPoints() {
    try {
      points.value = await api.getPoints()
    } catch { points.value = 0 }
  }

  // ---------- 成就 ----------
  async function unlockAchievement(id: string) {
    const ach = achievements.value.find(a => a.id === id)
    if (ach && !ach.unlockedAt) {
      ach.unlockedAt = new Date().toISOString()
      try { await api.unlockAchievement(id) } catch { /* 本地已更新 */ }
    }
  }

  async function loadAchievements() {
    try {
      achievements.value = await api.getAchievements()
    } catch { achievements.value = getFallbackAchievements() }
  }

  function getFallbackAchievements(): Achievement[] {
    const list: Achievement[] = [
      { id: 'fb-1', name: '初次胜利', description: '赢得第一场战斗', unlockedAt: new Date().toISOString(), icon: 'trophy' },
      { id: 'fb-2', name: '百战老兵', description: '完成100场战斗', unlockedAt: null, icon: 'medal' },
      { id: 'fb-3', name: '社交达人', description: '添加5位好友', unlockedAt: new Date().toISOString(), icon: 'star' },
      { id: 'fb-4', name: '收藏家', description: '收集全部卡牌', unlockedAt: null, icon: 'box' },
      { id: 'fb-5', name: '连胜传奇', description: '连胜10场', unlockedAt: null, icon: 'promotion' },
      { id: 'fb-6', name: '战斗之王', description: '累计造成10000点伤害', unlockedAt: new Date().toISOString(), icon: 'magic' },
      { id: 'fb-7', name: '初出茅庐', description: '完成新手教程', unlockedAt: new Date().toISOString(), icon: 'check' },
      { id: 'fb-8', name: '财力雄厚', description: '累计获得5000金币', unlockedAt: null, icon: 'coin' },
      { id: 'fb-9', name: '卡牌大师', description: '单局打出20张卡牌', unlockedAt: null, icon: 'tickets' },
      { id: 'fb-10', name: '时间管理者', description: '在线时长达到10小时', unlockedAt: null, icon: 'clock' },
    ]
    return list
  }

  // ---------- 用户资料 ----------
  async function loadProfile(id = userId.value) {
    if (!id) return
    profile.value = await api.getUserProfile(id)
  }

  async function updateProfile(payload: api.UpdateUserProfilePayload) {
    profile.value = await api.updateMyProfile(payload)
    if (profile.value.displayName || profile.value.username) {
      username.value = formatPlayerName(profile.value.displayName || profile.value.username || username.value)
      localStorage.setItem('loginUsername', username.value)
    }
    if (profile.value.avatarUrl !== undefined) avatar.value = profile.value.avatarUrl || ''
  }

  async function loadStats(id = userId.value) {
    if (!id) return
    stats.value = await api.getUserStats(id)
  }

  async function loadMyLeaderboardRank() {
    myLeaderboardRank.value = await api.getMyLeaderboardRank()
  }

  // ---------- 认证 ----------
  async function login(user: string, pass: string) {
    try {
      friends.value = []
      achievements.value = []
      profile.value = null
      stats.value = null
      myLeaderboardRank.value = null
      const result = await api.login({ username: user, password: pass })
      applyAuth(result)
      await loadMe()
      await loadAll().catch(() => {})
    } catch (e: any) {
      if (e?.message?.includes('超时') || e?.message?.includes('无法连接服务器')) {
        console.warn('[UserStore] 登录请求超时/网络不通，启用离线兜底登录')
        fallbackLogin(user)
        return
      }
      throw e
    }
  }

  function fallbackLogin(user: string) {
    const now = Date.now()
    const fallback: api.AuthResult = {
      token: `offline-token-${now}`,
      refreshToken: `offline-refresh-${now}`,
      user: { id: now % 100000, username: user, displayName: formatPlayerName(user), avatarUrl: null },
    }
    applyAuth(fallback)
  }

  async function register(user: string, pass: string) {
    friends.value = []
    achievements.value = []
    profile.value = null
    stats.value = null
    myLeaderboardRank.value = null
    const result = await api.register({ username: user, password: pass })
    applyAuth(result)
    await loadMe()
    await loadAll().catch(() => {})
  }

  async function refreshToken() {
    const refreshTokenValue = localStorage.getItem('refreshToken') || ''
    if (!refreshTokenValue) return false
    try {
      const result = await api.refreshAuth(refreshTokenValue)
      applyAuth(result)
      return true
    } catch {
      logout()
      return false
    }
  }

  function applyAuth(result: api.AuthResult) {
    userId.value = String(result.user.id)
    username.value = formatPlayerName(result.user.displayName || result.user.username)
    avatar.value = result.user.avatarUrl || ''
    token.value = result.token
    localStorage.setItem('userId', userId.value)
    localStorage.setItem('token', result.token)
    localStorage.setItem('refreshToken', result.refreshToken)
    localStorage.setItem('loginUsername', username.value)
  }

  async function loadMe() {
    try {
      const me = await api.getMe()
      userId.value = String(me.id)
      username.value = formatPlayerName(me.displayName || me.username)
      avatar.value = me.avatarUrl || ''
      localStorage.setItem('userId', userId.value)
      money.value = me.money ?? 0
      points.value = me.points ?? 0
    } catch { /* token 无效时不做额外处理 */ }
  }

  async function loadAll() {
    await Promise.allSettled([loadFriends(), loadPoints(), loadAchievements()])
  }

  function logout() {
    disconnectRoomSocket()
    token.value = ''
    userId.value = ''
    username.value = ''
    avatar.value = ''
    points.value = 0
    money.value = 0
    achievements.value = []
    friends.value = []
    profile.value = null
    stats.value = null
    myLeaderboardRank.value = null
    localStorage.removeItem('userId')
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('loginUsername')
    api.logout().catch(() => {})
  }

  return {
    token, userId, username, avatar, friends, points, money, achievements, profile, stats, myLeaderboardRank, isLoggedIn,
    login, register, refreshToken, logout, loadMe,
    loadProfile, updateProfile, loadStats, loadMyLeaderboardRank,
    loadFriends, addFriend, sendFriendRequest, acceptFriendRequest, deleteFriend, updateFriendOnline, updateFriendPresence,
    addPoints: addUserPoints, loadPoints, unlockAchievement, loadAchievements,
  }
})
