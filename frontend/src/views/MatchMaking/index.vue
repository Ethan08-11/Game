<template>
  <div class="match-wrapper" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
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

  <div class="match-page" :class="{ dimmed: showDeptModal }">
    <BackButton to="" text="返回大厅" @click="leaveCurrentRoom" />

    <div class="left-panel" :style="{ '--friend-list-bg': `url(${matchBg})`, '--red-offset': '-40px' }">
      <h3>好友列表</h3>
      <div class="friend-list">
        <div v-for="f in displayFriends" :key="f.id" class="friend-row">
          <img :src="f.avatarUrl || defaultAvatar" class="friend-avatar" />
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

    <div class="right-panel" :style="{ '--team-bg': `url(${matchFriendListBg})`, '--red-offset': '40px' }">
      <h3>队伍房间</h3>
      <div class="room-slots">
        <div v-for="i in 2" :key="i" class="slot" :class="{ filled: room.players[i-1], empty: !room.players[i-1] }">
          <template v-if="room.players[i-1]">
            <div class="avatar">{{ room.players[i-1].username.charAt(0).toUpperCase() }}</div>
            <span class="nickname">{{ room.players[i-1].username }}</span>
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
            <span class="nickname">等待加入...</span>
          </template>
        </div>
      </div>

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
import { extractRoomId, getFriends, getRoomDetail, leaveRoom as leaveRoomApi, abandonMatch, sendRoomInvite, setRoomDepartment, setRoomReady } from '@/api'
import type { Friend } from '@/api'
import BackButton from '@/components/BackButton.vue'
import { connectRoomSocket, subscribeRoomEvent } from '@/utils/roomSocket'
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
import defaultAvatar from '@/assets/default-avatar.webp'

const router = useRouter()
const route = useRoute()
const room = useRoomStore()
const user = useUserStore()

const bgImage = ref('')
const bgDay = bg2
const bgNight = bg1

interface DisplayFriend extends Friend {
  invited: boolean
}

const simFriends = ref<DisplayFriend[]>([])
const showDeptModal = ref(false)
const pendingDept = ref('')
const enteringBattle = ref(false)
const unsubscribeFns: Array<() => void> = []

const displayFriends = computed(() => simFriends.value)
const selfIndex = computed(() => room.players.findIndex(p => room.isSelfPlayer(p.id)))
const mySeatIndex = computed<0 | 1>(() => (selfIndex.value === 1 ? 1 : 0))
const isSelfReady = computed(() => (mySeatIndex.value === 0 ? room.player1Ready : room.player2Ready))



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
    if (player.id === user.userId) {
      player.username = user.username
    } else {
      const friend = simFriends.value.find(f => f.id === player.id)
      if (friend) {
        player.username = friend.displayName || friend.username
      }
    }
  }
}

async function syncRoom(roomId: string) {
  if (!roomId) return
  let lastError: any = null
  for (let attempt = 0; attempt < 6; attempt++) {
    try {
      const detail = await getRoomDetail(roomId)
      // 房间已关闭或对局已结束：清本地状态，避免卡在无法退出
      const status = Number(detail.status)
      if (status === 3 || detail.closedAt) {
        room.resetMatchMaking()
        sessionStorage.removeItem('activeMatchId')
        sessionStorage.removeItem('activeRoomId')
        return
      }
      room.roomId = roomId
      sessionStorage.setItem('activeRoomId', roomId)
      const friendNames = new Map(simFriends.value.map(f => [f.id, f.displayName || f.username]))
      room.syncRoomDetail(detail, user.userId, user.username, friendNames)
      patchPlayerNames()
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

async function clearClosedRoomAndLeave(message?: string) {
  ElMessage.closeAll()
  room.resetMatchMaking()
  sessionStorage.removeItem('activeMatchId')
  sessionStorage.removeItem('activeRoomId')
  if (message) ElMessage.warning(message)
  if (route.name === 'MatchMaking') {
    await router.push('/game-hall')
  }
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

watch(() => route.query.openDept, (value) => {
  if (value === '1' && room.roomId) openDeptModal()
}, { immediate: true })

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
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
  try {
    const list = await getFriends()
    if (list.length > 0) {
      simFriends.value = list.map(f => ({ ...f, invited: false }))
    }
  } catch (error: any) {
    simFriends.value = []
    ElMessage.error(error?.message || '在线好友加载失败')
  }
  if (user.userId) {
    room.setCurrentUser(user.userId)
  }
  if (user.token) {
    connectRoomSocket(user.token)
  }
  // 未真正进房时保持空槽，避免误显示“已在房间”
  const persistedRoomId = sessionStorage.getItem('activeRoomId') || room.roomId
  if (persistedRoomId) {
    await syncRoom(persistedRoomId).catch(() => {})
  }
  unsubscribeFns.push(
    subscribeRoomEvent('friend.presence.changed', (data: any) => {
      const userId = String(data?.userId ?? data?.data?.userId ?? '')
      const presenceStatus = data?.presenceStatus ?? data?.data?.presenceStatus
      const invitable = Boolean(data?.invitable ?? data?.data?.invitable)
      const f = simFriends.value.find(x => x.id === userId)
      if (f && presenceStatus) {
        f.presenceStatus = presenceStatus as any
        f.invitable = invitable
        f.online = presenceStatus !== 'OFFLINE'
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
      simFriends.value.forEach((friend) => { friend.invited = false })
      ElMessage.warning('对方已拒绝，可以继续邀请其他在线好友')
    }),
  )
})

onUnmounted(() => {
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
    room.syncRoomDetail(detail, user.userId, user.username, new Map(simFriends.value.map(f => [f.id, f.displayName || f.username])))
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

  f.invited = true
  try {
    const invite = await sendRoomInvite(f.id)
    room.sendInvite(String(invite.inviteId ?? invite.id ?? ''))
    if (invite.roomId) await syncRoom(String(invite.roomId))
    ElMessage.success('组队邀请已发送')
  } catch (error: any) {
    f.invited = false
    ElMessage.error(error?.message || '组队邀请发送失败')
  }
}

async function toggleReady() {
  if (!room.roomId) return
  try {
    const nextReady = !isSelfReady.value
    const detail = await setRoomReady(room.roomId, nextReady)
    console.log('[toggleReady] ready detail =', detail)
    room.syncRoomDetail(detail, user.userId, user.username, new Map(simFriends.value.map(f => [f.id, f.displayName || f.username])))
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
.match-wrapper {
  height: 100%;
  position: relative;
  isolation: isolate;
  overflow-x: hidden;
  overflow-y: auto;
}
.match-wrapper::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--hall-bg) center/cover no-repeat;
  filter: blur(12px);
  transform: scale(1.1);
  z-index: 0;
}

.match-page {
  position: relative;
  z-index: 1;
  display: flex;
  gap: var(--space-5);
  padding: 20vh 3% 20vh 3%;
  height: 100%;
  max-width: none;
  margin: 0 auto;
  color: #4a3520;
  transition: filter var(--transition-base);
  overflow-x: hidden;
  overflow-y: auto;
}
.match-page.dimmed {
  filter: brightness(0.35);
  pointer-events: none;
  user-select: none;
}

/* ========== 面板布局 ========== */
.left-panel, .right-panel {
  flex: 1 1 0;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
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
  position: fixed;
  left: calc(28vw + var(--red-offset, 0px));
  top: 50vh;
  transform: translate(-50%, -50%);
  height: 100vh;
  aspect-ratio: 1;
  background: var(--friend-list-bg) center/contain no-repeat;
  z-index: -1;
  pointer-events: none;
}
.right-panel {
  position: relative;
  isolation: isolate;
  padding-left: calc(var(--space-3) + 76px);
}
.right-panel::before {
  content: '';
  position: fixed;
  left: calc(72vw + var(--red-offset, 0px));
  top: 50vh;
  transform: translate(-50%, -50%);
  height: 100vh;
  aspect-ratio: 1;
  background: var(--team-bg) center/contain no-repeat;
  z-index: -1;
  pointer-events: none;
}
.left-panel h3, .right-panel h3 {
  margin-bottom: var(--space-5);
  font-size: var(--text-xl);
  color: #4a3520;
}
.friend-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #c4a962 transparent;
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
  margin-top: -130px;
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

.empty-slot {
  background: rgba(0, 0, 0, 0.08);
  color: #8b7a65;
}
.nickname { flex: 1; font-size: var(--text-md); min-width: 80px; color: #4a3520; }
.slot.empty .nickname { color: #8b7a65; }

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
    flex-direction: column;
    padding: var(--space-4);
    gap: var(--space-4);
    height: auto;
    overflow: visible;
  }
  .left-panel, .right-panel { flex: none; min-height: 300px; }
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
