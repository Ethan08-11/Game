<template>
  <div class="hall-fit">
    <div class="hall-frame">
      <div class="hall-page" :style="pageStyle">
        <header class="hall-header">
      <span class="mode-tag">{{ modeText }}</span>
      <div class="top-nav">
        <button class="achievement-btn" @click="$router.push('/achievements')">
          <img :src="achievementBtnImg" alt="成就" />
        </button>
        <button class="leaderboard-btn" @click="$router.push('/leaderboard')">
          <img :src="leaderboardBtnImg" alt="排行榜" />
        </button>
        <span class="money-wrap" :style="{ '--money-bg': `url(${moneyBg})` }">
          <span class="money-text">金 币 : {{ user.money }}</span>
        </span>
      </div>
      <div class="header-right">
        <button class="user-profile-btn" type="button" title="点击更换头像" @click="avatarDialogVisible = true">
          <span class="user-avatar-wrap">
            <PlayerAvatar class="user-avatar" :src="user.avatar" :alt="user.username" />
            <span class="avatar-edit-dot">换</span>
          </span>
          <span class="user-name">{{ user.username }}</span>
        </button>
        <el-button @click="clearLocalCache">清除本地缓存</el-button>
        <el-button type="danger" text @click="handleLogout">退出</el-button>
      </div>
    </header>

    <div class="hall-body">
      <aside class="hall-sidebar">
        <FriendPanel />
      </aside>

      <main class="hall-main">
        <div class="center-area">
          <div class="right-actions">
          </div>
        </div>
      </main>

      <button class="float-btn" :style="cardsBtnStyle">
        <img :src="tujianBtnImg" alt="图鉴" class="tujian-img" />
        <span class="hit-target" @click="$router.push('/cards')"></span>
      </button>
      <button class="float-btn" :style="questBtnStyle">
        <img :src="questBtnImg" alt="任务" class="quest-img" />
        <span
          v-if="questBadgeText"
          class="quest-badge"
          :title="questBadgeTitle"
        >{{ questBadgeText }}</span>
        <span class="hit-target" @click="$router.push('/quests')"></span>
      </button>
      <button class="float-btn" :style="customerBtnStyle">
        <img :src="customerBtnImg" alt="顾客图鉴" />
        <span class="hit-target" @click="$router.push('/customer-intro')"></span>
      </button>
      <button class="start-btn" :style="startBtnStyle">
        <span class="start-btn-label">开始<br>游戏</span>
        <span class="hit-target" style="height:140px" @click="startGame"></span>
      </button>
    </div>

    <footer class="hall-footer">
      <AnnouncementBar />
    </footer>
      </div>
    </div>

    <!-- 重连弹窗 -->
    <Teleport to="body">
      <div v-if="reconnectDialogVisible" class="reconnect-overlay">
        <div class="reconnect-modal">
          <h2 class="reconnect-title">你有未完成的对局</h2>
          <p class="reconnect-desc">
            检测到上一次游戏异常退出，是否重新连接继续对战？
          </p>
          <p class="reconnect-countdown">
            {{ reconnectCountdown }} 秒后自动放弃
          </p>
          <div class="reconnect-actions">
            <el-button type="primary" size="large" @click="doReconnect">重新连接</el-button>
            <el-button size="large" @click="doAbandonMatch">放弃对局</el-button>
          </div>
        </div>
      </div>
    </Teleport>

    <AvatarPickerDialog v-model="avatarDialogVisible" />

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, onActivated, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useRoomStore } from '@/store/room'
import { leaveRoom, abandonMatch, getMatchDetail, getCurrentMatch, getCurrentRoom, releaseIdleRoom, fetchMyTaskBoard } from '@/api'
import { clearMatchCache } from '@/utils/matchCache'
import FriendPanel from '@/components/FriendPanel.vue'
import AnnouncementBar from '@/components/AnnouncementBar.vue'
import AvatarPickerDialog from '@/components/AvatarPickerDialog.vue'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import startBtnImg from '@/assets/start-btn-v2.webp'
import questBtnImg from '@/assets/quest-btn-v3.webp'
import customerBtnImg from '@/assets/customer-btn-v3.webp'

import bg1 from '@/assets/hall-bg.webp'
import bg2 from '@/assets/hall-bg2.webp'
import moneyBg from '@/assets/points-btn-bg.webp'
import tujianBtnImg from '@/assets/tujian-btn.webp'
import leaderboardBtnImg from '@/assets/leaderboard-btn-icon.webp'
import achievementBtnImg from '@/assets/achievement-btn-icon.webp'


const router = useRouter()
const user = useUserStore()
const room = useRoomStore()

const DESIGN_W = 1280
const DESIGN_H = 800
const stageScaleX = ref(1)
const stageScaleY = ref(1)

const pageStyle = computed(() => ({
  '--hall-bg': bgImage.value ? `url(${bgImage.value})` : '',
  width: `${DESIGN_W}px`,
  height: `${DESIGN_H}px`,
  transform: `scale(${stageScaleX.value}, ${stageScaleY.value})`,
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
  stageScaleX.value = width / DESIGN_W
  stageScaleY.value = height / DESIGN_H
}

const bgImage = ref('')
const bgDay = bg2
const bgNight = bg1

const cardsBtnStyle = { left: '800px', bottom: '435px', width: '500px', height: '184px' }
const questBtnStyle = { left: '800px', bottom: '330px', width: '500px', height: '184px' }
const customerBtnStyle = { left: '800px', bottom: '239px', width: '500px', height: '184px' }
const startBtnStyle = { backgroundImage: `url(${startBtnImg})`, left: '703px', bottom: '-4px', width: '700px', height: '258px' }

const modeText = '本周模式：双人模式'

const avatarDialogVisible = ref(false)
const reconnectDialogVisible = ref(false)
const reconnectCountdown = ref(30)
const reconnectMatchId = ref('')
const questBadgeText = ref('')
const questBadgeTitle = ref('')
let reconnectTimer: ReturnType<typeof setInterval> | null = null
let reconnectTimeout: ReturnType<typeof setTimeout> | null = null

function stopReconnectTimers() {
  if (reconnectTimer) { clearInterval(reconnectTimer); reconnectTimer = null }
  if (reconnectTimeout) { clearTimeout(reconnectTimeout); reconnectTimeout = null }
}

async function doAbandonMatch() {
  stopReconnectTimers()
  reconnectDialogVisible.value = false
  if (reconnectMatchId.value) {
    await abandonMatch(reconnectMatchId.value).catch(() => {})
  }
  sessionStorage.removeItem('activeMatchId')
  reconnectMatchId.value = ''
  room.resetMatchMaking()
  clearMatchCache()
  ElMessage.warning('对局已结束')
  user.loadFriends().catch(() => {})
}

async function doReconnect() {
  stopReconnectTimers()
  reconnectDialogVisible.value = false
  const matchId = reconnectMatchId.value
  reconnectMatchId.value = ''
  if (matchId) {
    router.push(`/battle/${matchId}`)
  }
}

function persistActiveMatch(matchId: string) {
  if (!matchId) return
  room.setMatchId(matchId)
  sessionStorage.setItem('activeMatchId', matchId)
}

function matchIdFrom(value: unknown) {
  if (value == null || value === '') return ''
  return String(value)
}

async function resolveResumeMatchId(current: Awaited<ReturnType<typeof getCurrentRoom>> | null) {
  const fromRoom = matchIdFrom(current?.matchId)
  if (fromRoom) return fromRoom
  try {
    const live = await getCurrentMatch()
    const fromLive = matchIdFrom(live?.matchId)
    if (fromLive) return fromLive
  } catch {
    // 对局查询失败时退回本地缓存
  }
  if (current) return ''
  return sessionStorage.getItem('activeMatchId') || matchIdFrom(room.matchId)
}

async function checkActiveMatch(current: Awaited<ReturnType<typeof getCurrentRoom>> | null) {
  const savedMatchId = await resolveResumeMatchId(current)
  if (!savedMatchId) return

  try {
    const detail = await getMatchDetail(savedMatchId)
    if (detail.phase === 'FINISHED' || Number(detail.status) === 2) {
      sessionStorage.removeItem('activeMatchId')
      room.resetMatchMaking()
      clearMatchCache()
      return
    }
    persistActiveMatch(savedMatchId)
    router.replace(`/battle/${savedMatchId}`)
  } catch {
    if (matchIdFrom(current?.matchId) === savedMatchId) {
      persistActiveMatch(savedMatchId)
      router.replace(`/battle/${savedMatchId}`)
      return
    }
    sessionStorage.removeItem('activeMatchId')
  }
}

async function startGame() {
  ElMessage.closeAll()
  const current = await getCurrentRoom().catch(() => null)
  const liveMatchId = await resolveResumeMatchId(current)
  if (liveMatchId) {
    persistActiveMatch(liveMatchId)
    router.push(`/battle/${liveMatchId}`)
    return
  }
  await releaseIdleRoom().catch(() => {})
  room.resetMatchMaking()
  clearMatchCache()
  await user.loadFriends().catch(() => {})
  router.push('/customer-current')
}

async function clearLocalCache() {
  ElMessage.closeAll()
  await releaseIdleRoom().catch(() => {})
  room.resetMatchMaking()
  clearMatchCache()
  sessionStorage.removeItem('activeMatchId')
  sessionStorage.removeItem('activeRoomId')
  localStorage.removeItem('activeMatchId')
  localStorage.removeItem('activeRoomId')
  await user.loadFriends().catch(() => {})
  ElMessage.success('对局缓存已清除，可以重新组队')
}

async function loadQuestBadge() {
  try {
    const board = await fetchMyTaskBoard()
    const claimable = (board.tasks || []).filter((task) => Number(task.status) === 2).length
    if (claimable > 0) {
      questBadgeText.value = String(claimable)
      questBadgeTitle.value = `有 ${claimable} 个任务可领`
    } else {
      questBadgeText.value = ''
      questBadgeTitle.value = ''
    }
  } catch {
    questBadgeText.value = ''
    questBadgeTitle.value = ''
  }
}

onMounted(async () => {
  updateStageScale()
  window.addEventListener('resize', updateStageScale)
  window.visualViewport?.addEventListener('resize', updateStageScale)
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
  ElMessage.closeAll()
  user.loadMe()
  user.loadFriends()
  user.loadPoints()
  loadQuestBadge()
  let current: Awaited<ReturnType<typeof getCurrentRoom>> | null = null
  let roomLookupFailed = false
  try {
    current = await getCurrentRoom()
  } catch {
    roomLookupFailed = true
  }
  if (current) {
    room.syncRoomDetail(
      current,
      String(user.userId),
      user.username,
      new Map(user.friends.map(f => [String(f.id), f.displayName || f.username])),
    )
    const matchId = matchIdFrom(current.matchId)
    if (matchId) persistActiveMatch(matchId)
  }
  await checkActiveMatch(current)
  if (!current && !roomLookupFailed && !room.matchId && !sessionStorage.getItem('activeMatchId')) {
    room.resetMatchMaking()
    clearMatchCache()
  }
})

onActivated(() => {
  loadQuestBadge()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateStageScale)
  window.visualViewport?.removeEventListener('resize', updateStageScale)
  stopReconnectTimers()
})

async function handleLogout() {
  if (room.roomId) {
    await leaveRoom(room.roomId).catch(() => {})
    room.resetMatchMaking()
  }
  sessionStorage.removeItem('activeMatchId')
  clearMatchCache()
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.hall-fit {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: transparent;
}
.hall-frame {
  position: relative;
  overflow: hidden;
  width: 100%;
  height: 100%;
}
.hall-page {
  display: flex; flex-direction: column; height: 100%; color: var(--color-text-primary);
  position: relative; isolation: isolate;
  transform-origin: 0 0;
  min-height: 0;
}
.hall-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--hall-bg) center/cover no-repeat;
  z-index: 0;
}
.hall-page > * {
  position: relative;
  z-index: 1;
}

.hall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) var(--space-5);
  background: transparent;
  border-bottom: none;
  box-shadow: none;
  height: 64px;
}
.mode-tag {
  font-family: 'HuiWen MingChao', 'Songti SC', 'STSong', serif;
  font-size: 22px;
  font-weight: 400;
  line-height: 1;
  letter-spacing: 0.14em;
  color: #ffe9a6;
  text-shadow:
    0 0 1px #6a4a12,
    0 1px 0 #3a2408,
    0 2px 6px rgba(0, 0, 0, 0.75);
  padding: 6px 14px 5px;
  border: 1px solid rgba(255, 214, 120, 0.55);
  border-radius: 2px;
  background: linear-gradient(180deg, rgba(90, 58, 18, 0.72), rgba(42, 26, 8, 0.78));
  box-shadow: inset 0 1px 0 rgba(255, 232, 180, 0.25), 0 2px 8px rgba(0, 0, 0, 0.35);
}
.top-nav { display: flex; gap: var(--space-1); }
.top-btn {
  padding: var(--space-1) var(--space-4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: var(--text-base);
  transition: all var(--transition-fast);
}
.top-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  border-color: var(--color-accent);
  color: var(--color-text-primary);
}
.leaderboard-btn {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  transform: translate(198px, 13px);
  transition: none;
}
.leaderboard-btn:hover {
  transform: translate(198px, 13px) scale(1.08);
}
.leaderboard-btn img {
  height: 20px;
  width: auto;
  display: block;
  transform: scale(4.2);
  transform-origin: center center;
}
.achievement-btn {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  transform: translate(117px, 11px);
  transition: none;
}
.achievement-btn:hover {
  transform: translate(117px, 11px) scale(1.08);
}
.achievement-btn img {
  height: 20px;
  width: auto;
  display: block;
  transform: scale(4);
  transform-origin: center center;
}
.money-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px var(--space-4);
  margin-left: 288px;
  isolation: isolate;
}
.money-wrap::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background: var(--money-bg) center/contain no-repeat;
  transform: scale(9);
  transform-origin: center center;
  pointer-events: none;
}
.money-text {
  color: #fff;
  font-weight: var(--weight-bold);
  font-size: var(--text-lg);
  margin-left: 16px;
}
.header-right { display: flex; align-items: center; gap: var(--space-3); }
.user-profile-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: inherit;
}
.user-avatar-wrap {
  position: relative;
  width: 56px;
  height: 56px;
  flex-shrink: 0;
}
.user-avatar {
  width: 56px;
  height: 56px;
}
.avatar-edit-dot {
  position: absolute;
  right: -4px;
  bottom: -2px;
  min-width: 22px;
  height: 20px;
  padding: 0 5px;
  border-radius: 999px;
  background: #8b6914;
  color: #fff8e6;
  font-size: 12px;
  line-height: 20px;
  font-weight: 700;
  box-shadow: 0 0 0 2px rgba(40, 24, 8, 0.35);
}
.user-profile-btn:hover .user-avatar {
  filter: brightness(1.08);
}
.user-name {
  color: #fff8e6;
  font-weight: var(--weight-semibold);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.9), 0 0 8px rgba(0, 0, 0, 0.5);
}
.header-right :deep(.el-button) {
  --el-button-text-color: #fff8e6;
  --el-button-bg-color: rgba(40, 26, 12, 0.55);
  --el-button-border-color: rgba(255, 232, 196, 0.35);
  --el-button-hover-text-color: #fffdf4;
  --el-button-hover-bg-color: rgba(62, 40, 18, 0.72);
  --el-button-hover-border-color: rgba(255, 232, 196, 0.55);
  color: #fff8e6;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.85);
}
.header-right :deep(.el-button--danger.is-text) {
  --el-button-text-color: #ff9a8a;
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-text-color: #ffc2b6;
  --el-button-hover-bg-color: rgba(80, 20, 16, 0.35);
  --el-button-hover-border-color: transparent;
  color: #ff9a8a;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.85);
}

.hall-body { display: flex; flex: 1; overflow-x: hidden; overflow-y: auto; position: relative; min-height: 0; }

.hall-sidebar {
  width: var(--sidebar-width);
  padding: 0;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 1px 0 8px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
  position: relative;
  isolation: isolate;
}
.hall-sidebar::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background: url('@/assets/friend-list-bg.webp') 53% 20%/200% no-repeat;
  pointer-events: none;
}

.hall-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.center-area {
  display: flex;
  align-items: center;
  gap: var(--space-10);
}
.start-btn {
  position: absolute;
  border: none;
  background-color: transparent;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  padding: 0;
  cursor: default;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform var(--transition-base), filter var(--transition-base);
  z-index: 2;
  pointer-events: none;
}
.start-btn-label {
  position: relative;
  z-index: 1;
  font-size: 48px;
  font-weight: var(--weight-bold);
  color: #4a3520;
  line-height: 1.3;
  text-align: center;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.2);
}
.start-btn:hover {
  transform: scale(1.05);
  filter: brightness(1.1);
}

.right-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.action-btn {
  width: 80px; height: 80px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
  color: var(--color-text-secondary);
  font-size: var(--text-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.action-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: var(--color-accent-muted);
}
.float-btn {
  position: absolute;
  z-index: 2;
  border: none;
  background: transparent;
  padding: 0;
  cursor: default;
  color: #4a3520;
  font-size: 48px;
  font-weight: var(--weight-bold);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform var(--transition-base), filter var(--transition-base);
  pointer-events: none;
}
.float-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.quest-img {
  transform: scale(0.80) translateX(12px);
}
.quest-badge {
  position: absolute;
  right: 148px;
  top: 42px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 99px;
  background: #c44536;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 22px;
  text-align: center;
  z-index: 4;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.35);
}
.tujian-img {
  transform: scale(1.08) translateX(-6px);
}
.float-btn:hover {
  filter: brightness(1.1);
  transform: scale(1.05);
}
.hit-target {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 240px;
  height: 70px;
  pointer-events: auto;
  cursor: pointer;
  z-index: 3;
}
.hall-footer {
  position: relative;
  z-index: 6;
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .hall-header {
    padding: var(--space-1) var(--space-3);
  }
  .mode-tag { display: none; }
  .top-btn { padding: var(--space-1) var(--space-2); font-size: var(--text-sm); }
  .user-name { display: none; }
  .hall-sidebar { display: none; }
  .center-area {
    flex-direction: column;
    gap: var(--space-5);
  }
  .right-actions { flex-direction: row; }
}
</style>

<style>
</style>

<style>
.reconnect-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.reconnect-modal {
  background: #2a2520;
  border: 1px solid var(--color-accent, #c4a962);
  border-radius: var(--radius-lg, 12px);
  padding: 40px 48px;
  text-align: center;
  max-width: 420px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.5);
}
.reconnect-title {
  color: var(--color-accent, #c4a962);
  font-size: 22px;
  margin: 0 0 12px;
}
.reconnect-desc {
  color: var(--color-text-secondary, #b0a89a);
  font-size: 15px;
  margin: 0 0 10px;
  line-height: 1.6;
}
.reconnect-countdown {
  color: var(--color-danger, #e06060);
  font-size: 14px;
  margin: 0 0 24px;
}
.reconnect-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}
</style>
