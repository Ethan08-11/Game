<template>
  <div class="match-fit" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
    <div class="match-page" :class="{ dimmed: showDeptModal }" :style="pageStyle">
    <!-- 部门选择遮罩层（独立于页面，不受 dim 影响） -->
    <Teleport to="body">
    <div v-if="showDeptModal" class="dept-modal-overlay" @click.self="">
      <div class="dept-modal">
        <h2 class="dept-modal-title">选择你的部门</h2>
        <p class="dept-modal-sub">选择后将决定你在本局游戏中的卡牌池</p>
        <div class="dept-cards">
          <div
            v-for="dept in room.depts"
            :key="dept"
            class="dept-card"
            :class="{ selected: pendingDept === dept }"
            :style="{ backgroundImage: `url(${deptCardBg})` }"
            @click="pendingDept = dept"
          >
            <div class="dept-card-name">{{ dept }}</div>
            <div class="dept-card-desc">{{ getDeptDesc(dept) }}</div>
            <div v-if="pendingDept === dept" class="dept-card-check">
              <el-icon :size="20"><Check /></el-icon>
            </div>
          </div>
        </div>
        <div class="dept-actions">
          <button class="dept-back-btn" @click="showDeptModal = false">
            返回
          </button>
          <button class="dept-confirm-btn" :disabled="!pendingDept" @click="confirmDept">
            确定
          </button>
        </div>
      </div>
    </div>
  </Teleport>

    <BackButton to="" text="返回大厅" @click="leaveCurrentRoom" />

    <div class="left-panel" :style="{ '--friend-list-bg': `url(${matchBg})` }">
      <h3 class="panel-title">好友列表</h3>
      <div class="friend-list">
        <div v-for="f in displayFriends" :key="f.id" class="friend-row">
          <PlayerAvatar class="friend-avatar" :src="f.avatarUrl" :alt="f.displayName || f.username" />
          <span class="fname">{{ f.displayName || f.username }}</span>
          <div class="friend-actions">
            <img :src="getStatusIcon(f)" class="status-icon" />
            <button
              class="invite-btn"
              :disabled="room.players.length >= 2 || f.invited || !f.invitable"
              @click="inviteFriend(f)"
            >
              <img :src="f.invitable && !f.invited ? inviteBrightIcon : inviteDimIcon" class="invite-icon" />
            </button>
          </div>
        </div>
      </div>
      <div v-if="displayFriends.length === 0" class="empty-tip">暂无好友</div>
    </div>

    <div class="right-panel" :style="{ '--team-bg': `url(${matchFriendListBg})` }">
      <h3 class="panel-title">队伍房间</h3>
      <div class="room-slots">
        <div v-for="i in 2" :key="i" class="slot" :class="{ filled: room.players[i-1], empty: !room.players[i-1] }">
          <template v-if="room.players[i-1]">
            <PlayerAvatar class="avatar" :src="slotAvatar(room.players[i-1])" :alt="room.players[i-1].username" />
            <span class="nickname">{{ room.players[i-1].username }}</span>
            <span v-if="isHostSlot(i - 1)" class="host-badge">房主</span>
            <span
              v-if="getSlotDept(i - 1)"
              class="dept-badge"
              :class="{ clickable: isSelfSlot(i - 1) && !isSlotReady(i - 1) }"
              :style="{ background: getDeptColor(getSlotDept(i - 1)) }"
              @click="isSelfSlot(i - 1) && !isSlotReady(i - 1) && openDeptModal()"
            >
              {{ getSlotDept(i - 1) }}
            </span>
            <span v-else-if="isSelfSlot(i - 1)" class="dept-pending clickable" @click="openDeptModal()">
              选择部门
            </span>
            <span v-else class="dept-pending">未选择</span>
            <span v-if="i === 1 ? room.player1Ready : room.player2Ready" class="ready-badge">已准备</span>
          </template>
          <template v-else>
            <div class="avatar empty-slot">
              <el-icon :size="18"><QuestionFilled /></el-icon>
            </div>
            <span class="nickname">{{ pendingInvite && i === 2 ? '等待对方接受...' : '等待加入...' }}</span>
          </template>
        </div>
      </div>

      <p v-if="pendingInvite && room.players.length < 2" class="invite-wait-hint">邀请已发送，双方进入房间后即可选部门</p>

      <button
        class="ready-btn"
        :class="{
          disabled: !room.canStart,
          active: room.canStart && !isSelfReady,
          readied: isSelfReady
        }"
        :disabled="!room.canStart"
        @click="toggleReady"
      >
        {{ isSelfReady ? '已准备' : '准备' }}
      </button>

      <p v-if="room.bothReady" class="battle-hint">双方已准备，等待开始对局...</p>
      <button v-if="room.roomId" class="leave-room-btn" @click="leaveCurrentRoom">退出房间</button>
    </div>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { QuestionFilled, Check } from '@element-plus/icons-vue'
import { useRoomStore } from '@/store/room'
import { useUserStore } from '@/store/user'
import { extractRoomId, getRoomDetail, getCurrentRoom, leaveRoom as leaveRoomApi, abandonMatch, sendRoomInvite, setRoomDepartment, setRoomReady } from '@/api'
import type { Friend } from '@/api'
import BackButton from '@/components/BackButton.vue'
import { connectRoomSocket, subscribeRoomEvent } from '@/utils/roomSocket'
import { clearMatchCache, isClosedRoom } from '@/utils/matchCache'
import { formatPlayerName } from '@/utils/playerName'
import bg1 from '@/assets/hall-bg.webp'
import bg2 from '@/assets/hall-bg2.webp'
import matchBg from '@/assets/match-bg.webp'
import matchFriendListBg from '@/assets/match-friend-list-bg.webp'
import statusOnlineIcon from '@/assets/status-online.webp'
import deptCardBg from '@/assets/dept-card-bg.webp'
import statusOfflineIcon from '@/assets/status-offline.webp'
import statusInGameIcon from '@/assets/status-in-game.webp'
import statusInTeamIcon from '@/assets/status-in-team.webp'
import inviteBrightIcon from '@/assets/invite-bright.webp'
import inviteDimIcon from '@/assets/invite-dim.webp'
import PlayerAvatar from '@/components/PlayerAvatar.vue'

const router = useRouter()
const route = useRoute()
const room = useRoomStore()
const user = useUserStore()

const DESIGN_W = 1280
const DESIGN_H = 800
const stageScale = ref(1)

const bgImage = ref('')
const bgDay = bg2
const bgNight = bg1

const pageStyle = computed(() => ({
  width: `${DESIGN_W}px`,
  height: `${DESIGN_H}px`,
  transform: `scale(${stageScale.value})`,
}))

function viewportSize() {
  const view = window.visualViewport
  return {
    width: view?.width ?? window.innerWidth,
    height: view?.height ?? window.innerHeight,
  }
}

function updateStageScale() {
  const { width, height } = viewportSize()
  stageScale.value = Math.min(width / DESIGN_W, height / DESIGN_H)
}

interface DisplayFriend extends Friend {
  invited: boolean
}

const invitedIds = ref<Record<string, boolean>>({})
const showDeptModal = ref(false)
const pendingDept = ref('')
const pendingInvite = ref(false)
const enteringBattle = ref(false)
const unsubscribeFns: Array<() => void> = []
let roomPollTimer: ReturnType<typeof setInterval> | null = null

const displayFriends = computed<DisplayFriend[]>(() =>
  user.friends.map(f => ({
    ...f,
    invited: !!invitedIds.value[String(f.id)],
  })),
)
const selfIndex = computed(() => room.players.findIndex(p => room.isSelfPlayer(p.id)))
const mySeatIndex = computed<0 | 1>(() => (selfIndex.value === 1 ? 1 : 0))
const isSelfReady = computed(() => (mySeatIndex.value === 0 ? room.player1Ready : room.player2Ready))

function slotAvatar(player: { id: string; avatarUrl?: string | null }): string | null {
  if (player.avatarUrl) return player.avatarUrl
  if (room.isSelfPlayer(player.id)) return user.avatar || null
  const friend = user.friends.find(f => String(f.id) === String(player.id))
  return friend?.avatarUrl || null
}



function getStatusIcon(f: Friend): string {
  switch (f.presenceStatus) {
    case 'IN_MATCH':
      return statusInGameIcon
    case 'IN_ROOM':
      return statusInTeamIcon
    case 'OFFLINE':
      return statusOfflineIcon
    default:
      return statusOnlineIcon
  }
}

function isSelfSlot(index: number): boolean {
  return selfIndex.value === index
}

function isHostSlot(index: number): boolean {
  const player = room.players[index]
  return !!player && String(room.hostUserId) === String(player.id)
}

function getSlotDept(index: number): string {
  if (index === 0) return room.player1Dept
  return room.player2Dept
}

function isSlotReady(index: number): boolean {
  if (index === 0) return room.player1Ready
  return room.player2Ready
}

function getDeptColor(dept: string): string {
  const colors: Record<string, string> = {
    '销售部': '#7da38a',
    '采购部': '#c8963e',
    '物流部': '#3498db',
    '营销部': '#e74c3c',
    '设计部': '#9b59b6',
    '技术部': '#1abc9c',
    '财务部': '#f39c12',
    '人事部': '#e67e22',
  }
  return colors[dept] || '#c4a962'
}

function getDeptDesc(dept: string): string {
  const descs: Record<string, string> = {
    '销售部': '擅长攻击型卡牌，直击霸凌者要害',
    '采购部': '擅长防御型卡牌，为团队提供护盾',
    '物流部': '擅长调配资源，保障团队行动点',
    '营销部': '擅长削弱敌人，降低霸凌者威胁',
    '设计部': '擅长多段攻击和创意战术',
    '技术部': '擅长控制和触发效果',
    '财务部': '擅长资金管理和经济压制',
    '人事部': '擅长增益和团队辅助',
  }
  return descs[dept] || ''
}

function getBackendDeptType(dept: string): string {
  const map: Record<string, string> = {
    '销售部': 'sales',
    '采购部': 'purchase',
    '物流部': 'logistics',
    '营销部': 'marketing',
    '设计部': 'design',
    '技术部': 'tech',
    '财务部': 'finance',
    '人事部': 'hr',
  }
  return map[dept] || dept
}

function getRoomIdFromEvent(data: any) {
  return extractRoomId(data)
}

function patchPlayerNames() {
  for (const player of room.players) {
    if (!player.username.startsWith('玩家')) continue
    if (String(player.id) === String(user.userId)) {
      player.username = formatPlayerName(user.username)
    } else {
      const friend = user.friends.find(f => String(f.id) === String(player.id))
      if (friend) {
        player.username = formatPlayerName(friend.displayName || friend.username)
      }
    }
  }
}

function friendNameMap() {
  return new Map(user.friends.map(f => [String(f.id), f.displayName || f.username]))
}

function stopRoomPoll() {
  if (roomPollTimer) {
    clearInterval(roomPollTimer)
    roomPollTimer = null
  }
}

async function syncCurrentRoom() {
  const detail = await getCurrentRoom().catch(() => null)
  if (!detail) return false
  const id = extractRoomId(detail) || String(detail.roomId ?? detail.id ?? '')
  if (!id) return false
  room.syncRoomDetail(detail, String(user.userId), user.username, friendNameMap())
  patchPlayerNames()
  if (room.players.length >= 2) {
    pendingInvite.value = false
    stopRoomPoll()
  }
  return room.players.length > 0
}

function startRoomPoll() {
  if (roomPollTimer) return
  void syncCurrentRoom()
  roomPollTimer = setInterval(() => {
    void syncCurrentRoom()
  }, 2500)
}

async function syncRoom(roomId: string) {
  if (!roomId) return
  let lastError: any = null
  for (let attempt = 0; attempt < 6; attempt++) {
    try {
      const detail = await getRoomDetail(roomId)
      if (isClosedRoom(detail)) {
        pendingInvite.value = false
        stopRoomPoll()
        room.resetMatchMaking()
        clearMatchCache()
        return
      }
      room.roomId = roomId
      sessionStorage.setItem('activeRoomId', roomId)
      const friendNames = friendNameMap()
      room.syncRoomDetail(detail, String(user.userId), user.username, friendNames)
      patchPlayerNames()
      if (room.players.length >= 2) {
        pendingInvite.value = false
        stopRoomPoll()
      }
      if (detail.roomId || detail.id) {
        room.roomId = String(detail.roomId ?? detail.id ?? roomId)
        room.roomCode = String(detail.roomCode ?? detail.code ?? room.roomCode)
      }
      return
    } catch (error: any) {
      lastError = error
      const message = String(error?.message || '')
      if (message.includes('房间不存在') && attempt < 5) {
        await new Promise((resolve) => setTimeout(resolve, 250 * (attempt + 1)))
        continue
      }
      throw error
    }
  }
  if (lastError) throw lastError
}

async function clearClosedRoomAndLeave(_message?: string) {
  ElMessage.closeAll()
  pendingInvite.value = false
  stopRoomPoll()
  room.resetMatchMaking()
  clearMatchCache()
}

async function handleRoomCreated(data: any) {
  const roomId = getRoomIdFromEvent(data)
  if (!roomId) return
  ElMessage.success('组队成功')
  room.setCurrentUser(user.userId)
  const hostUserId = String(data?.hostUserId ?? data?.data?.hostUserId ?? '')
  room.isHost = !!user.userId && hostUserId === String(user.userId)
  if (route.name !== 'MatchMaking') await router.push('/matchmaking')
  await syncRoom(roomId)
}

async function handleRoomUpdated(data: any) {
  const roomId = getRoomIdFromEvent(data) || room.roomId
  if (!roomId) return
  await syncRoom(roomId)
  if (!room.roomId) return
  if (route.name !== 'MatchMaking') await router.push('/matchmaking')
}

watch(
  () => [route.query.openDept, room.roomId] as const,
  ([value, roomId]) => {
    if (value === '1' && roomId) {
      openDeptModal()
      if (route.query.openDept) {
        router.replace({ path: '/matchmaking' }).catch(() => {})
      }
    }
  },
  { immediate: true },
)

async function enterBattle(matchId: string) {
  console.log('[enterBattle] pushing to battle', matchId)
  if (!matchId || enteringBattle.value) return
  enteringBattle.value = true
  room.setMatchId(matchId)
  sessionStorage.setItem('activeMatchId', matchId)
  await router.push(`/battle/${matchId}`)
}

async function handleGameStart(data: any) {
  const matchId = String(data?.matchId ?? data?.data?.matchId ?? room.matchId ?? sessionStorage.getItem('activeMatchId') ?? '')
  if (route.name !== 'MatchMaking') await router.push('/matchmaking')
  if (matchId) await enterBattle(matchId)
  else if (room.matchId) await enterBattle(room.matchId)
  const roomId = getRoomIdFromEvent(data) || room.roomId
  if (roomId) {
    syncRoom(roomId).catch(() => {})
  }
}

onMounted(async () => {
  updateStageScale()
  window.addEventListener('resize', updateStageScale)
  window.visualViewport?.addEventListener('resize', updateStageScale)
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
  try {
    await user.loadFriends()
  } catch (error: any) {
    ElMessage.error(error?.message || '在线好友加载失败')
  }
  if (user.userId) {
    room.setCurrentUser(user.userId)
  }
  if (user.token) {
    connectRoomSocket(user.token)
  }
  const restored = await syncCurrentRoom().catch(() => false)
  if (!restored) {
    const persistedRoomId = sessionStorage.getItem('activeRoomId') || room.roomId
    if (persistedRoomId) {
      await syncRoom(persistedRoomId).catch(() => {})
    }
  }
  unsubscribeFns.push(
    subscribeRoomEvent('ws.connected', () => {
      void user.loadFriends()
    }),
    subscribeRoomEvent('friend.presence.changed', (data: any) => {
      const userId = String(data?.userId ?? data?.data?.userId ?? '')
      const presenceStatus = data?.presenceStatus ?? data?.data?.presenceStatus
      const invitable = Boolean(data?.invitable ?? data?.data?.invitable)
      if (userId && presenceStatus) {
        user.updateFriendPresence(userId, presenceStatus, invitable)
      }
      if (presenceStatus === 'IN_ROOM' || presenceStatus === 'IN_MATCH') {
        void syncCurrentRoom()
      }
    }),
    subscribeRoomEvent('room.invite.accepted', async (data: any) => {
      const roomId = getRoomIdFromEvent(data)
      if (roomId) await syncRoom(roomId).catch(() => {})
      else if (room.roomId) await syncRoom(room.roomId).catch(() => {})
      ElMessage.success('对方已接受邀请')
    }),
    subscribeRoomEvent('room.created', handleRoomCreated),
    subscribeRoomEvent('room.updated', handleRoomUpdated),
    subscribeRoomEvent('room.member.updated', handleRoomUpdated),
    subscribeRoomEvent('room.member.department.changed', handleRoomUpdated),
    subscribeRoomEvent('room.member.ready', handleRoomUpdated),
    subscribeRoomEvent('room.ready.changed', handleRoomUpdated),
    subscribeRoomEvent('room.first_player.changed', handleRoomUpdated),
    subscribeRoomEvent('match.started', handleGameStart),
    subscribeRoomEvent('room.game.started', handleGameStart),
    subscribeRoomEvent('game.started', handleGameStart),
    subscribeRoomEvent('room.invite.rejected', () => {
      invitedIds.value = {}
      ElMessage.warning('对方已拒绝，可以继续邀请其他在线好友')
    }),
  )
})

onUnmounted(() => {
  window.removeEventListener('resize', updateStageScale)
  window.visualViewport?.removeEventListener('resize', updateStageScale)
  stopRoomPoll()
  unsubscribeFns.splice(0).forEach((unsubscribe) => unsubscribe())
})

function openDeptModal() {
  const seatIndex = mySeatIndex.value
  pendingDept.value = (seatIndex === 1 ? room.player2Dept : room.player1Dept) || ''
  showDeptModal.value = true
}

async function confirmDept() {
  if (!pendingDept.value) return
  if (!room.roomId) {
    const persistedRoomId = sessionStorage.getItem('activeRoomId') || ''
    if (persistedRoomId) {
      await syncRoom(persistedRoomId).catch(() => {})
    }
  }
  if (!room.roomId) {
    ElMessage.warning('请先成功组队后再选择部门')
    showDeptModal.value = false
    return
  }
  try {
    const detail = await setRoomDepartment(room.roomId, getBackendDeptType(pendingDept.value))
    room.syncRoomDetail(detail, String(user.userId), user.username, friendNameMap())
    patchPlayerNames()
    showDeptModal.value = false
    await syncRoom(String(detail.roomId ?? detail.id ?? room.roomId))
    await router.replace('/matchmaking')
    ElMessage.success('部门选择成功')
  } catch (error: any) {
    const msg = String(error?.message || '')
    if (msg.includes('房间已关闭') || msg.includes('请重新组队') || msg.includes('对局进行中')) {
      showDeptModal.value = false
      await clearClosedRoomAndLeave('房间已关闭，请重新组队')
      return
    }
    ElMessage.error(msg || '部门选择失败')
  }
}

async function inviteFriend(f: DisplayFriend) {
  if (!f.online) {
    ElMessage.warning('只能邀请在线好友')
    return
  }

  invitedIds.value = { ...invitedIds.value, [String(f.id)]: true }
  try {
    const invite = await sendRoomInvite(f.id)
    room.sendInvite(String(invite.inviteId ?? invite.id ?? ''))
    pendingInvite.value = true
    if (invite.roomId) await syncRoom(String(invite.roomId))
    startRoomPoll()
    ElMessage.success('组队邀请已发送')
  } catch (error: any) {
    invitedIds.value = { ...invitedIds.value, [String(f.id)]: false }
    pendingInvite.value = false
    ElMessage.error(error?.message || '组队邀请发送失败')
  }
}

async function toggleReady() {
  if (!room.roomId) return
  try {
    const nextReady = !isSelfReady.value
    const detail = await setRoomReady(room.roomId, nextReady)
    console.log('[toggleReady] ready detail =', detail)
    room.syncRoomDetail(detail, String(user.userId), user.username, friendNameMap())
    patchPlayerNames()
    const matchId = String(detail?.matchId ?? room.matchId ?? '')
    console.log('[toggleReady] matchId =', matchId)
    if (matchId) {
      room.setMatchId(matchId)
      sessionStorage.setItem('activeMatchId', matchId)
      await enterBattle(matchId)
      return
    }
    await syncRoom(String(detail.roomId ?? detail.id ?? room.roomId))
  } catch (error: any) {
    const msg = String(error?.message || '')
    if (msg.includes('房间已关闭') || msg.includes('请重新组队') || msg.includes('对局进行中')) {
      await clearClosedRoomAndLeave('房间已关闭，请重新组队')
      return
    }
    ElMessage.error(msg || '准备状态更新失败')
  }
}

async function leaveCurrentRoom() {
  stopRoomPoll()
  pendingInvite.value = false
  if (!room.roomId) {
    room.resetMatchMaking()
    await router.push('/game-hall')
    return
  }
  try {
    await leaveRoomApi(room.roomId)
    room.resetMatchMaking()
    sessionStorage.removeItem('activeMatchId')
    await user.loadFriends().catch(() => {})
    ElMessage.success('已退出房间')
    await router.push('/game-hall')
  } catch (error: any) {
    const msg = String(error?.message || '')
    // 对局已结束但仍卡在房间：强制放弃后清本地并回大厅
    if (msg.includes('对局已经开始') || msg.includes('不能通过普通退出')) {
      const mid = room.matchId || sessionStorage.getItem('activeMatchId') || ''
      if (mid) await abandonMatch(mid).catch(() => {})
      await leaveRoomApi(room.roomId).catch(() => {})
      room.resetMatchMaking()
      sessionStorage.removeItem('activeMatchId')
      await user.loadFriends().catch(() => {})
      ElMessage.success('已退出房间')
      await router.push('/game-hall')
      return
    }
    ElMessage.error(msg || '退出房间失败')
  }
}
</script>

<style scoped>
.match-fit {
  width: 100%;
  height: 100%;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  isolation: isolate;
}
.match-fit::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--hall-bg) center/cover no-repeat;
  filter: blur(12px);
  transform: scale(1.1);
  z-index: 0;
  pointer-events: none;
}

.match-page {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: stretch;
  gap: 48px;
  padding: 96px 48px 80px;
  box-sizing: border-box;
  color: #4a3520;
  transition: filter var(--transition-base);
  overflow: hidden;
  transform-origin: center center;
}
.match-page.dimmed {
  filter: brightness(0.35);
  pointer-events: none;
  user-select: none;
}

/* ========== 面板布局 ========== */
.left-panel, .right-panel {
  flex: 1 1 0;
  max-width: 520px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-xl);
  padding: 56px 20px 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
}
.left-panel {
  position: relative;
  isolation: isolate;
  background: rgba(0, 0, 0, 0.15);
  overflow: hidden;
}
.left-panel::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 140%;
  height: 130%;
  background: var(--friend-list-bg) center/contain no-repeat;
  z-index: -1;
  pointer-events: none;
}
.right-panel {
  position: relative;
  isolation: isolate;
  padding-left: calc(var(--space-3) + 24px);
  overflow: hidden;
}
.right-panel::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 140%;
  height: 130%;
  background: var(--team-bg) center/contain no-repeat;
  z-index: -1;
  pointer-events: none;
}
.panel-title {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
.friend-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #c4a962 transparent;
  padding-top: 16px;
}

.friend-row {
  display: flex;
  align-items: center;
  height: 320px;
  flex-shrink: 0;
  padding: 12px var(--space-5) 0 var(--space-3);
  background: url('@/assets/friend-row-bg.webp') center/100% 100% no-repeat;
  position: relative;
  margin-top: -260px;
}
.friend-row:first-child {
  margin-top: -10px;
}

.friend-avatar {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}
.fname {
  flex: 1;
  color: #4a3520;
  font-size: 24px;
  margin-left: calc(var(--space-3) - 1em);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  position: relative;
  z-index: 1;
}
.friend-actions {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  margin-left: -80px;
}

.status-icon {
  height: 54px;
  object-fit: contain;
  flex-shrink: 0;
}

.invite-btn {
  width: 54px; height: 54px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
  transition: transform var(--transition-fast);
}
.invite-btn:hover:not(:disabled) {
  animation: inviteBounce 0.5s ease;
}
.invite-btn:disabled {
  cursor: not-allowed;
}
.invite-icon {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

@keyframes inviteBounce {
  0%   { transform: scale(1); }
  30%  { transform: scale(1.25); }
  50%  { transform: scale(0.9); }
  70%  { transform: scale(1.1); }
  100% { transform: scale(1); }
}
.empty-tip {
  color: #8b7a65;
  padding: var(--space-5);
  text-align: center;
}

/* ========== 房间槽位 ========== */
.room-slots {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  min-height: 0;
  padding-top: 72px;
}

.slot {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  flex-wrap: wrap;
}
.slot.filled {
  background: rgba(196, 169, 98, 0.2);
  border: 1px solid #c4a962;
}
.slot.empty {
  background: rgba(0, 0, 0, 0.08);
  border: 1px dashed #8b7a65;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-slot {
  background: rgba(0, 0, 0, 0.08);
  color: #8b7a65;
}
.nickname { flex: 1; font-size: var(--text-md); min-width: 80px; color: #4a3520; }
.slot.empty .nickname { color: #8b7a65; }
.host-badge {
  padding: 2px 10px;
  border-radius: var(--radius-full);
  background: #5a8a6a;
  color: #fff;
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  white-space: nowrap;
}
.invite-wait-hint {
  margin: var(--space-3) 0 0;
  text-align: center;
  color: #8b6914;
  font-size: var(--text-sm);
}

.dept-badge {
  padding: 2px var(--space-3);
  border-radius: var(--radius-full);
  color: #fff;
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  white-space: nowrap;
}
.dept-badge.clickable {
  cursor: pointer;
  transition: opacity var(--transition-fast);
}
.dept-badge.clickable:hover {
  opacity: 0.8;
}
.dept-pending {
  color: #8b7a65;
  font-size: var(--text-xs);
}
.dept-pending.clickable {
  cursor: pointer;
  color: var(--color-accent);
  text-decoration: underline;
  text-underline-offset: 2px;
}
.dept-pending.clickable:hover {
  color: var(--color-accent-hover);
}

.ready-badge {
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  background: rgba(125, 163, 138, 0.2);
  color: #5a8a6a;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  flex-shrink: 0;
}

.first-player-panel {
  margin-top: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: rgba(0, 0, 0, 0.08);
  border: 1px solid #c4a962;
}
.first-player-title {
  margin-bottom: var(--space-3);
  color: #4a3520;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
}
.first-player-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-2);
}
.first-player-btn {
  padding: var(--space-2) var(--space-3);
  border: 1px solid #c4a962;
  border-radius: var(--radius-md);
  background: rgba(0, 0, 0, 0.1);
  color: #4a3520;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.first-player-btn.selected {
  border-color: #4a3520;
  color: #4a3520;
  box-shadow: 0 0 0 1px #4a3520;
  background: rgba(0, 0, 0, 0.2);
}
.first-player-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.first-player-tip {
  margin: var(--space-2) 0 0;
  color: #8b7a65;
  font-size: var(--text-xs);
}
.ready-btn {
  width: 100%;
  padding: var(--space-4);
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  cursor: pointer;
  transition: all var(--transition-base);
  margin-top: var(--space-5);
}
.ready-btn.disabled {
  background: rgba(0, 0, 0, 0.1);
  color: #8b7a65;
  cursor: not-allowed;
}
.ready-btn.active {
  background: #c4a962;
  color: #fff;
  box-shadow: 0 0 15px rgba(196, 169, 98, 0.4);
}
.ready-btn.active:hover {
  background: #b3984e;
  transform: translateY(-1px);
}
.ready-btn.readied {
  background: #5a8a6a;
  color: #fff;
  cursor: default;
}

.battle-hint {
  margin-top: var(--space-3);
  text-align: center;
  color: #c4a962;
  font-size: var(--text-md);
  animation: pulse 1s infinite;
}
.leave-room-btn {
  width: 100%;
  margin-top: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}
.leave-room-btn:hover {
  border-color: #e74c3c;
  color: #e74c3c;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (max-width: 767px) {
  .match-page {
    gap: 32px;
    padding: 80px 32px 56px;
  }
}
</style>

<!-- 模态框样式（非 scoped，因为 Teleport 到 body） -->
<style>
.dept-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
}
.dept-modal {
  background: #1a1a24;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 48px;
  text-align: center;
  max-width: 620px;
  width: 90%;
  animation: deptSlideUp 0.4s ease;
}
.dept-modal-title {
  font-size: var(--text-3xl);
  color: var(--color-text-primary);
  margin-bottom: 8px;
  font-weight: var(--weight-bold);
}
.dept-modal-sub {
  font-size: var(--text-md);
  color: var(--color-text-secondary);
  margin-bottom: 36px;
}
.dept-cards {
  display: flex;
  gap: 24px;
  justify-content: center;
  margin-bottom: 36px;
}
.dept-card {
  width: 220px;
  height: 280px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.dept-card:hover {
  transform: translateY(-6px);
  filter: brightness(1.1);
}
.dept-card.selected {
  filter: brightness(1.15) drop-shadow(0 0 12px rgba(196, 169, 98, 0.6));
}
.dept-card-name {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: #3e2b1c;
  margin-bottom: 8px;
  margin-top: calc(60px + 3em);
}
.dept-card-desc {
  font-size: var(--text-sm);
  color: #5a3d28;
}
.dept-card-check {
  position: absolute;
  top: 10px;
  right: 10px;
  color: #c4a962;
}
.dept-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  align-items: center;
}
.dept-back-btn,
.dept-confirm-btn {
  padding: 12px 56px;
  border: none;
  border-radius: 8px;
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  cursor: pointer;
  transition: all 0.2s ease;
  background-image: url('@/assets/dept-btn-bg.webp');
  background-size: 100% 100%;
  background-repeat: no-repeat;
  color: var(--color-bg-base);
}
.dept-confirm-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.1);
}
.dept-confirm-btn:disabled {
  filter: grayscale(100%) brightness(0.7);
  color: #666;
  cursor: not-allowed;
}

@keyframes deptSlideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
