<template>
  <div class="app-shell">
    <router-view v-slot="{ Component }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
    <MagicTrail />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptRoomInvite, extractRoomId, getPendingRoomInvites, getRoomDetail, leaveRoom as leaveRoomApi, rejectRoomInvite } from '@/api'
import { useRoomStore } from '@/store/room'
import { useUserStore } from '@/store/user'
import { connectRoomSocket, disconnectRoomSocket, subscribeRoomEvent } from '@/utils/roomSocket'
import MagicTrail from '@/components/MagicTrail.vue'

const route = useRoute()
const router = useRouter()
const room = useRoomStore()
const user = useUserStore()
const unsubscribeFns: Array<() => void> = []
let friendsRefreshTimer: ReturnType<typeof setInterval> | null = null
let pendingInviteTimer: ReturnType<typeof setInterval> | null = null
const sessionReady = ref(false)
const shownInviteIds = new Set<string>()
let inviteDialogOpen = false

const isInBattlePage = computed(() => String(route.name || '') === 'BattlePage')

function canShowInvite() {
  // 对战中不弹；残留 players 不应拦截（需同时有有效 roomId）
  if (!user.isLoggedIn || isInBattlePage.value) return false
  if (room.roomId && room.players.length >= 2) return false
  return true
}

function getInviteId(data: any) {
  return String(
    data?.inviteId
    ?? data?.id
    ?? data?.data?.inviteId
    ?? data?.data?.id
    ?? ''
  )
}

function getInviterName(data: any) {
  return data?.fromUsername
    ?? data?.inviterUsername
    ?? data?.data?.fromUsername
    ?? data?.data?.inviterUsername
    ?? '好友'
}

async function sleep(ms: number) {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

async function syncRoom(roomId: string, options: { retries?: number; clearOnMissing?: boolean } = {}) {
  const retries = Math.max(1, options.retries ?? 1)
  const clearOnMissing = options.clearOnMissing ?? false
  if (!roomId) return

  let lastError: any = null
  for (let attempt = 0; attempt < retries; attempt++) {
    try {
      const detail = await getRoomDetail(roomId)
      console.log('[syncRoom] detail =', detail)
      const status = Number(detail.status)
      if (status === 3 || detail.closedAt) {
        sessionStorage.removeItem(ACTIVE_MATCH_KEY)
        sessionStorage.removeItem('activeRoomId')
        room.resetMatchMaking()
        return
      }
      // 仅确认房间仍开放后再落本地 roomId
      room.roomId = roomId
      sessionStorage.setItem('activeRoomId', roomId)
      const friendNames = new Map(user.friends.map(f => [f.id, f.displayName || f.username]))
      room.syncRoomDetail(detail, user.userId, user.username, friendNames)
      const matchId = String(detail.matchId ?? '')
      if (matchId && status === 2) {
        room.setMatchId(matchId)
        sessionStorage.setItem(ACTIVE_MATCH_KEY, matchId)
        if (String(route.name || '') !== 'BattlePage') {
          await router.push(`/battle/${matchId}`)
        }
      } else if (!detail.roomId && !detail.id) {
        sessionStorage.removeItem(ACTIVE_MATCH_KEY)
        room.resetMatchMaking()
      }
      return
    } catch (error: any) {
      lastError = error
      const message = String(error?.message || '')
      const missing = message.includes('房间不存在')
      if (missing && attempt < retries - 1) {
        await sleep(250 * (attempt + 1))
        continue
      }
      if (missing) {
        if (clearOnMissing) {
          sessionStorage.removeItem(ACTIVE_MATCH_KEY)
          sessionStorage.removeItem('activeRoomId')
          room.resetMatchMaking()
          if (String(route.name || '') !== 'GameHall') {
            await router.push('/game-hall')
          }
        }
        return
      }
      throw error
    }
  }
  if (lastError) throw lastError
}

async function handleInviteCreated(data: any) {
  if (!canShowInvite()) return
  const inviteId = getInviteId(data)
  if (!inviteId || shownInviteIds.has(inviteId) || inviteDialogOpen) return
  shownInviteIds.add(inviteId)
  inviteDialogOpen = true
  const inviterName = getInviterName(data)

  try {
    await ElMessageBox.confirm(`${inviterName} 邀请你组队，是否接受？`, '组队邀请', {
      confirmButtonText: '接受',
      cancelButtonText: '拒绝',
      distinguishCancelAndClose: true,
      closeOnClickModal: false,
      closeOnPressEscape: false,
      type: 'info',
    })
    const result = await acceptRoomInvite(inviteId)
    const roomId = extractRoomId(result)
    if (roomId) await syncRoom(roomId, { retries: 5, clearOnMissing: false })
    if (!room.matchId && !sessionStorage.getItem(ACTIVE_MATCH_KEY)) {
      await router.push('/matchmaking')
    }
  } catch (action) {
    if (action === 'cancel') await rejectRoomInvite(inviteId).catch(() => {})
  } finally {
    inviteDialogOpen = false
  }
}

async function pollPendingInvites() {
  if (!user.isLoggedIn || !canShowInvite() || inviteDialogOpen) return
  try {
    const pending = await getPendingRoomInvites()
    const next = pending.find((item) => {
      const id = String(item.inviteId ?? '')
      return id && !shownInviteIds.has(id)
    })
    if (!next) return
    await handleInviteCreated({
      inviteId: next.inviteId,
      fromUsername: next.fromUsername,
      fromUserId: next.fromUserId,
    })
  } catch {
    // ignore poll errors
  }
}

function startPendingInvitePoll() {
  if (pendingInviteTimer || !user.isLoggedIn) return
  void pollPendingInvites()
  pendingInviteTimer = setInterval(() => { void pollPendingInvites() }, 4000)
}

function stopPendingInvitePoll() {
  if (pendingInviteTimer) clearInterval(pendingInviteTimer)
  pendingInviteTimer = null
}

function getEventMatchId(data: any) {
  return String(data?.matchId ?? data?.data?.matchId ?? '')
}

function handlePresenceChanged(data: any) {
  const userId = String(data?.userId ?? data?.data?.userId ?? '')
  const presenceStatus = data?.presenceStatus ?? data?.data?.presenceStatus
  const invitable = Boolean(data?.invitable ?? data?.data?.invitable)
  if (!userId || !presenceStatus) {
    user.loadFriends()
    return
  }
  user.updateFriendPresence(userId, presenceStatus, invitable)
}

async function handleWsConnected() {
  room.isConnected = true
  refreshFriends()
  startPendingInvitePoll()
  const persistedRoomId = sessionStorage.getItem('activeRoomId') || room.roomId
  if (persistedRoomId) {
    await syncRoom(persistedRoomId, { retries: 5, clearOnMissing: false }).catch(() => {})
  }
}

function handleHeartbeatAck() {
  room.isConnected = true
}

async function refreshFriends() {
  if (user.isLoggedIn) await user.loadFriends().catch(() => {})
}

function startFriendsFallbackRefresh() {
  if (friendsRefreshTimer || !user.isLoggedIn) return
  friendsRefreshTimer = setInterval(refreshFriends, 30_000)
}

function stopFriendsFallbackRefresh() {
  if (friendsRefreshTimer) clearInterval(friendsRefreshTimer)
  friendsRefreshTimer = null
}

async function handleRoomAcceptedOrCreated(data: any) {
  const roomId = extractRoomId(data)
  if (!roomId) return
  room.setMatchId('')
  sessionStorage.removeItem(ACTIVE_MATCH_KEY)
  sessionStorage.setItem('activeRoomId', roomId)
  room.roomId = roomId
  await syncRoom(roomId, { retries: 8, clearOnMissing: false })
  await user.loadFriends().catch(() => {})
  if (room.matchId || sessionStorage.getItem(ACTIVE_MATCH_KEY)) return
  if (String(route.name || '') !== 'MatchMaking') {
    await router.push('/matchmaking')
  }
}

async function handleRoomClosed(_data: any) {
  // 房间关闭后一律清组队态；不要用残留 matchId 把人拉回已结束对局
  sessionStorage.removeItem(ACTIVE_MATCH_KEY)
  sessionStorage.removeItem('activeRoomId')
  room.resetMatchMaking()
  await refreshFriends()
  if (String(route.name || '') === 'MatchMaking') {
    ElMessage.warning('房间已关闭，请重新组队')
    await router.push('/game-hall')
  }
}

const ACTIVE_MATCH_KEY = 'activeMatchId'

async function handleMatchStarted(data: any) {
  const matchId = getEventMatchId(data) || room.matchId || sessionStorage.getItem(ACTIVE_MATCH_KEY) || ''
  console.log('[ws match.started]', data)
  console.log('[ws match.started] resolved matchId =', matchId)
  if (!matchId) return
  room.setMatchId(matchId)
  sessionStorage.setItem(ACTIVE_MATCH_KEY, matchId)
  await refreshFriends()
  if (String(route.name || '') !== 'BattlePage' || String(route.params.matchId || '') !== matchId) {
    await router.push(`/battle/${matchId}`)
  }
}

async function handleMatchEnded() {
  const currentRoomId = room.roomId || sessionStorage.getItem('activeRoomId') || ''
  sessionStorage.removeItem(ACTIVE_MATCH_KEY)
  sessionStorage.removeItem('activeRoomId')
  room.resetMatchMaking()
  if (currentRoomId) {
    await leaveRoomApi(currentRoomId).catch(() => {})
  }
  room.resetMatchMaking()
  await refreshFriends()
  // BattlePage shows its own result overlay; no navigation needed
}


function ensureRoomSocket() {
  if (!sessionReady.value) return
  const accessToken = user.token || localStorage.getItem('token')
  if (!accessToken) {
    disconnectRoomSocket()
    stopFriendsFallbackRefresh()
    return
  }
  if (user.userId) room.setCurrentUser(user.userId)
  connectRoomSocket(accessToken)
  startFriendsFallbackRefresh()
}

function handleTokenRefreshed(event: Event) {
  const detail = (event as CustomEvent<{ token?: string; user?: { id?: number | string } }>).detail
  if (!detail?.token) return
  user.token = detail.token
  if (detail.user?.id != null) {
    user.userId = String(detail.user.id)
    room.setCurrentUser(user.userId)
  }
  connectRoomSocket(detail.token)
}

function handleAuthExpired() {
  user.logout()
  disconnectRoomSocket()
  stopFriendsFallbackRefresh()
  sessionStorage.removeItem('activeMatchId')
  ElMessage.warning('登录已过期，请重新登录')
  router.push('/login')
}

function handleBeforeUnload() {
  const token = localStorage.getItem('token')
  if (!token) return

  // Don't leave room if user is in an active match — allow reconnection
  const isInMatch = !!sessionStorage.getItem('activeMatchId') || !!room.matchId
  if (!isInMatch && room.roomId) {
    const baseUrl = import.meta.env.VITE_API_BASE || '/api'
    const headers = { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
    fetch(`${baseUrl}/rooms/${room.roomId}/leave`, { method: 'POST', headers, keepalive: true }).catch(() => {})
  }
  disconnectRoomSocket()
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  window.addEventListener('auth:token-refreshed', handleTokenRefreshed)
  window.addEventListener('auth:expired', handleAuthExpired)
  unsubscribeFns.push(
    subscribeRoomEvent('room.invite.created', handleInviteCreated),
    subscribeRoomEvent('room.invite.accepted', handleRoomAcceptedOrCreated),
    subscribeRoomEvent('room.created', handleRoomAcceptedOrCreated),
    subscribeRoomEvent('friend.presence.changed', handlePresenceChanged),
    subscribeRoomEvent('ws.heartbeat.ack', handleHeartbeatAck),
    subscribeRoomEvent('ws.connected', handleWsConnected),
    subscribeRoomEvent('room.closed', handleRoomClosed),
    subscribeRoomEvent('match.started', handleMatchStarted),
    subscribeRoomEvent('match.ended', handleMatchEnded),
  )
})

// 有登录态就建立房间 WS（含刷新后从 localStorage 恢复 token）
watch(() => user.token, (newToken) => {
  sessionReady.value = !!newToken
  ensureRoomSocket()
  if (newToken) startPendingInvitePoll()
  else stopPendingInvitePoll()
}, { immediate: true })

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('auth:token-refreshed', handleTokenRefreshed)
  window.removeEventListener('auth:expired', handleAuthExpired)
  stopFriendsFallbackRefresh()
  stopPendingInvitePoll()
  unsubscribeFns.splice(0).forEach((unsubscribe) => unsubscribe())
})
</script>

<style lang="scss">
.app-shell {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--color-bg-base);
  color: var(--color-text-primary);
}
</style>
