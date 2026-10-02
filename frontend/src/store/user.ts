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
  difficulty?: number
  progressValue?: number
  targetCount?: number
  category?: string
  hidden?: boolean
  conditionType?: string
  sortNo?: number
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
    return [
      { id: 'ACH-002', name: '百战老兵', description: '累计取得 10 场胜利', unlockedAt: null, icon: 'trophy', difficulty: 2, progressValue: 0, targetCount: 10, category: 'battle', hidden: false, sortNo: 2 },
      { id: 'ACH-003', name: '任务达人', description: '累计领取 20 个任务', unlockedAt: null, icon: 'medal', difficulty: 2, progressValue: 0, targetCount: 20, category: 'growth', hidden: false, sortNo: 3 },
      { id: 'ACH-004', name: '社交先锋', description: '累计添加 5 位好友', unlockedAt: null, icon: 'star', difficulty: 2, progressValue: 0, targetCount: 5, category: 'social', hidden: false, sortNo: 4 },
      { id: 'ACH-012', name: '稳定输出', description: '累计打完 20 局（输赢都算，作废不计）', unlockedAt: null, icon: 'trophy', difficulty: 2, progressValue: 0, targetCount: 20, category: 'battle', hidden: false, sortNo: 201 },
      { id: 'ACH-014', name: '三日打卡', description: '累计 3 个工作日领过金币', unlockedAt: null, icon: 'medal', difficulty: 2, progressValue: 0, targetCount: 3, category: 'growth', hidden: false, sortNo: 202 },
      { id: 'ACH-016', name: '换着组', description: '本周在每日前 3 局里和 5 个不同的人组过队', unlockedAt: null, icon: 'star', difficulty: 2, progressValue: 0, targetCount: 5, category: 'social', hidden: false, sortNo: 203 },
      { id: 'ACH-017', name: '连胜三', description: '连续获胜 3 局（放弃或失败打断，作废不打断）', unlockedAt: null, icon: 'trophy', difficulty: 2, progressValue: 0, targetCount: 3, category: 'battle', hidden: false, sortNo: 204 },
      { id: 'ACH-018', name: '混组默契', description: '销售+采购组合获胜 5 局', unlockedAt: null, icon: 'trophy', difficulty: 2, progressValue: 0, targetCount: 5, category: 'battle', hidden: false, sortNo: 205 },
      { id: 'ACH-019', name: '收藏入门', description: '图鉴解锁 8 张收藏卡', unlockedAt: null, icon: 'medal', difficulty: 2, progressValue: 0, targetCount: 8, category: 'growth', hidden: false, sortNo: 206 },
      { id: 'ACH-020', name: '顶压过关', description: '与今日总榜前五组队并获胜 1 局', unlockedAt: null, icon: 'trophy', difficulty: 2, progressValue: 0, targetCount: 1, category: 'battle', hidden: false, sortNo: 207 },
      { id: 'ACH-021', name: '五十胜', description: '累计取得 50 场胜利', unlockedAt: null, icon: 'trophy', difficulty: 3, progressValue: 0, targetCount: 50, category: 'battle', hidden: false, sortNo: 301 },
      { id: 'ACH-022', name: '满勤一周', description: '同一自然周领满 5 个工作日金币', unlockedAt: null, icon: 'medal', difficulty: 3, progressValue: 0, targetCount: 5, category: 'growth', hidden: false, sortNo: 302 },
      { id: 'ACH-023', name: '十日十人', description: '本周在每日前 3 局里和 10 个不同的人组过队', unlockedAt: null, icon: 'star', difficulty: 3, progressValue: 0, targetCount: 10, category: 'social', hidden: false, sortNo: 303 },
      { id: 'ACH-024', name: '连胜五', description: '连续获胜 5 局', unlockedAt: null, icon: 'trophy', difficulty: 3, progressValue: 0, targetCount: 5, category: 'battle', hidden: false, sortNo: 304 },
      { id: 'ACH-025', name: '无复活十胜', description: '无人复活的胜利累计 10 局', unlockedAt: null, icon: 'trophy', difficulty: 3, progressValue: 0, targetCount: 10, category: 'battle', hidden: false, sortNo: 305 },
      { id: 'ACH-026', name: '双职精通', description: '销售获胜 20 局且采购获胜 20 局', unlockedAt: null, icon: 'trophy', difficulty: 3, progressValue: 0, targetCount: 20, category: 'battle', hidden: false, sortNo: 306 },
      { id: 'ACH-027', name: '图鉴过半', description: '可解锁收藏卡已解锁过半', unlockedAt: null, icon: 'medal', difficulty: 3, progressValue: 0, targetCount: 1, category: 'growth', hidden: false, sortNo: 307 },
      { id: 'ACH-028', name: '高压五胜', description: '与今日总榜前五组队并获胜 5 局', unlockedAt: null, icon: 'trophy', difficulty: 3, progressValue: 0, targetCount: 5, category: 'battle', hidden: false, sortNo: 308 },
      { id: 'ACH-029', name: '月度过半', description: '当月工作日领过不少于定额一半', unlockedAt: null, icon: 'medal', difficulty: 3, progressValue: 0, targetCount: 1, category: 'growth', hidden: false, sortNo: 309 },
      { id: 'ACH-042', name: '久经沙场', description: '累计打完 60 局（输赢都算，作废不计）', unlockedAt: null, icon: 'trophy', difficulty: 3, progressValue: 0, targetCount: 60, category: 'battle', hidden: false, sortNo: 310 },
      { id: 'ACH-043', name: '任务老手', description: '累计领取 60 个任务奖励', unlockedAt: null, icon: 'medal', difficulty: 3, progressValue: 0, targetCount: 60, category: 'growth', hidden: false, sortNo: 311 },
      { id: 'ACH-044', name: '十友相知', description: '累计添加 10 位好友', unlockedAt: null, icon: 'star', difficulty: 3, progressValue: 0, targetCount: 10, category: 'social', hidden: false, sortNo: 312 },
      { id: 'ACH-045', name: '完美三日', description: '同一个月里有 3 天都完成并赢得当日第 1、2、3 局', unlockedAt: null, icon: 'medal', difficulty: 3, progressValue: 0, targetCount: 3, category: 'growth', hidden: false, sortNo: 313 },
      { id: 'ACH-046', name: '五客会战', description: '在 5 种不同雇主的局里各获胜至少 1 局', unlockedAt: null, icon: 'trophy', difficulty: 3, progressValue: 0, targetCount: 5, category: 'battle', hidden: false, sortNo: 314 },
      { id: 'ACH-047', name: '险中取胜', description: '自己的血曾降到 10 及以下后仍获胜，累计 3 局', unlockedAt: null, icon: 'trophy', difficulty: 3, progressValue: 0, targetCount: 3, category: 'battle', hidden: false, sortNo: 315 },
      { id: 'ACH-030', name: '百胜传奇', description: '累计取得 100 场胜利', unlockedAt: null, icon: 'trophy', difficulty: 4, progressValue: 0, targetCount: 100, category: 'battle', hidden: false, sortNo: 401 },
      { id: 'ACH-031', name: '连胜十', description: '连续获胜 10 局', unlockedAt: null, icon: 'trophy', difficulty: 4, progressValue: 0, targetCount: 10, category: 'battle', hidden: false, sortNo: 402 },
      { id: 'ACH-032', name: '满勤十月', description: '2026 年 10 月领满 24 个工作日', unlockedAt: null, icon: 'medal', difficulty: 4, progressValue: 0, targetCount: 24, category: 'growth', hidden: false, sortNo: 403 },
      { id: 'ACH-033', name: '图鉴大师', description: '当前可解锁的收藏卡全部集齐', unlockedAt: null, icon: 'medal', difficulty: 4, progressValue: 0, targetCount: 1, category: 'growth', hidden: false, sortNo: 404 },
      { id: 'ACH-034', name: '隐藏 · 绝境翻盘', description: '双方都曾掉到危险血（≤5）后仍获胜', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 1, category: 'hidden', hidden: true, conditionType: 'both_low_hp_win', sortNo: 405 },
      { id: 'ACH-035', name: '隐藏 · 完美一日', description: '同一自然日完成并赢得当日第 1、2、3 局', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 1, category: 'hidden', hidden: true, conditionType: 'daily_three_wins', sortNo: 406 },
      { id: 'ACH-036', name: '隐藏 · 高压三连', description: '与当日潜在前五组队连续获胜 3 局。失败或放弃打断，作废不打断，普通胜局也打断', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 3, category: 'hidden', hidden: true, conditionType: 'hp_win_streak', sortNo: 408 },
      { id: 'ACH-037', name: '隐藏 · 三客碾压', description: '在胆小怕事、焦虑难安、刻薄尖客局里各无复活获胜 1 局', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 3, category: 'hidden', hidden: true, conditionType: 'three_harsh_no_revive', sortNo: 409 },
      { id: 'ACH-038', name: '隐藏 · 残血不复活', description: '双方最低血都曾 ≤5，全程无人复活，最后仍获胜', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 1, category: 'hidden', hidden: true, conditionType: 'low_hp_no_revive', sortNo: 410 },
      { id: 'ACH-039', name: '隐藏 · 双职无伤', description: '销售无复活获胜 10 局，且采购无复活获胜 10 局', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 10, category: 'hidden', hidden: true, conditionType: 'dept_no_revive', sortNo: 411 },
      { id: 'ACH-040', name: '隐藏 · 速战', description: '8 回合内无人复活获胜，累计 3 局', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 3, category: 'hidden', hidden: true, conditionType: 'fast_clear', sortNo: 412 },
      { id: 'ACH-041', name: '隐藏 · 完美一周', description: '同一自然周里有 5 天都完成并赢得当日第 1、2、3 局', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 5, category: 'hidden', hidden: true, conditionType: 'perfect_week', sortNo: 413 },
      { id: 'ACH-005', name: '隐藏彩蛋', description: '成功解锁全部成就', unlockedAt: null, icon: 'question', difficulty: 4, progressValue: 0, targetCount: 1, category: 'hidden', hidden: true, conditionType: 'all_unlocked', sortNo: 414 },
    ]
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
