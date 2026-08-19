<template>
  <div class="battle-page" :style="{ '--battle-bg': bgImage ? `url(${bgImage})` : '', '--check-player-bg': `url(${checkPlayerBtnBg})` }">
    <BackButton to="" text="放弃对战" @click="handleLeave" />

    <div class="battle-main">
      <section class="battle-stage">
        <div class="stage-bg">
          <div class="turn-info">回合 {{ turnNumber }}</div>
          <div class="battle-status-row">
            <div class="status-col">
              <span>{{ customerStatusText }}</span>
              <span>{{ actionOrderText }}</span>
            </div>
            <span v-if="activePhase === 'REVIVE_WAIT'" class="revive-wait-tag">等待复活中…</span>
            <span v-if="bullyDefense > 0">霸凌者防御：{{ bullyDefense }}</span>
            <span v-if="bullyActionText">{{ bullyActionText }}</span>
            <span>房主识别：{{ room.isHost ? '正确' : '异常' }}</span>
            <span v-if="isSelectingFirstPlayer">先手状态：{{ firstPlayerStatusText }}</span>
          </div>
          <div class="entity-row">
            <div class="entity-outline entity-outline-customer" :style="customerBoxStyle">
              <EmployerCard />
            </div>
            <div class="guard-team" aria-hidden="true" />
            <div class="entity-outline entity-outline-bully" :style="bullyBoxStyle">
              <BullyCard />
            </div>
          </div>
          <div v-if="actionLog.length" class="action-log-panel">
            <div class="action-log-title">战斗记录</div>
            <div class="action-log-list">
              <div v-for="(msg, i) in actionLog" :key="i" class="action-log-item">{{ msg }}</div>
            </div>
          </div>

        </div>
      </section>
    </div>

    <div v-if="isSelectingFirstPlayer" class="first-player-overlay">
      <div class="first-player-card">
        <h2>{{ room.isHost ? '选择本局先手' : '等待房主选择先手' }}</h2>
        <p>双方查看手牌后，由房主决定谁先出牌。</p>
        <div class="first-player-actions">
          <el-button
            v-for="player in players"
            :key="player.userId"
            :disabled="!canChooseFirstPlayer"
            type="primary"
            @click="chooseFirst(player.userId)"
          >
            {{ player.dept || '玩家' }} 先手
          </el-button>
        </div>
      </div>
    </div>

    <footer class="battle-footer">
      <div class="footer-row">
        <div class="draw-pile card-area-highlight" @click="showDeckModal = true">
          <el-icon :size="20"><Box /></el-icon>
          <span class="pile-count">{{ activeDeckCount }}</span>
          <span class="pile-label">牌库</span>
        </div>

        <div class="center-stack">
          <div class="funds-indicator-wrap funds-indicator-highlight" :style="fundsIndicatorStyle">
            <div class="funds-indicator"><img class="funds-icon" :src="fundsIcon" alt="" />调用机会 {{ currentFunds }}/{{ fundsCap }}</div>
          </div>
          <div class="hand-actions-row">
            <button class="switch-player-btn" type="button" :disabled="game.isGameOver" @click="switchPlayer" :aria-label="`查看玩家 P${activePlayer + 1}`" />
            <button class="finish-btn" type="button" :style="finishBtnStyle" :disabled="!canActWithActivePlayer" @click="endTurn" aria-label="结束回合" />
          </div>

          <div class="hand-cards" :style="{ '--card-width': cardWidth + 'px', '--cost-top': cardCostTop + 'px', '--cost-left': cardCostLeft + 'px', '--cost-size': cardCostSize + 'px', '--dept-top': cardDeptTop + 'px', '--dept-left': cardDeptLeft + 'px', '--name-top': cardNameTop + 'px', '--name-left': cardNameLeft + 'px', '--desc-top': cardDescTop + 'px', '--desc-left': cardDescLeft + 'px', '--tag-top': cardTagTop + 'px', '--tag-left': cardTagLeft + 'px', '--effect-top': cardEffectTop + 'px', '--effect-left': cardEffectLeft + 'px', '--effect-size': cardEffectSize + 'px' }">
            <CardItem
              v-for="card in activeHand"
              :key="card.id"
              :name="card.name"
              :dept="card.dept"
              :cost="card.cost"
              :type="card.type"
              :description="card.description"
              :damage="card.damage || 0"
              :shield="card.shield || 0"
              :image-url="card.imageUrl"
              :disabled="!canActWithActivePlayer"
              @play="playCard(card)"
            />
            <div v-if="activeHandCount === 0" class="hand-empty">当前回合手牌已用完</div>
          </div>
        </div>

        <div class="discard-pile">
          <el-icon :size="20"><Delete /></el-icon>
          <span class="pile-count">{{ activeDiscardCount }}</span>
          <span class="pile-label">弃牌</span>
        </div>
      </div>
    </footer>

    <el-dialog
      v-model="showDeckModal"
      :title="`P${activePlayer + 1} 本局个人牌组`"
      width="960px"
      class="deck-dialog"
      append-to-body
      :z-index="200000"
    >
      <div v-if="activeFullDeck.length === 0" class="deck-empty">暂无牌组数据</div>
      <div v-else class="deck-grid" style="--card-width: 168px">
        <CardItem
          v-for="card in activeFullDeck"
          :key="card.id"
          class="deck-card-item"
          :name="card.name"
          :dept="card.dept"
          :cost="card.cost"
          :type="card.type"
          :description="card.description"
          :damage="card.damage || 0"
          :shield="card.shield || 0"
          :image-url="card.imageUrl"
          disabled
        />
      </div>
    </el-dialog>

    <Teleport to="body">
      <div v-if="showTargetDialog" class="target-overlay" @click.self="cancelTargetDialog">
        <div class="target-dialog target-dialog-highlight" :style="{ '--choose-player-bg': `url(${choosePlayerBg})`, '--p1-btn-bg': `url(${p1BtnBg})`, '--p2-btn-bg': `url(${p2BtnBg})`, '--frame-w': frameW + 'px', '--frame-h': frameH + 'px', '--bg-w': bgW + 'px', '--bg-h': bgH + 'px' }">
          <div class="target-dialog-title">
            <span class="target-title-box target-title-main" :style="{ position: 'relative', top: titleTop + 'px', left: titleLeft + 'px' }">{{ pendingTargetCard?.type === 'defend' ? '选择防御目标' : pendingTargetCard?.type === 'support' ? '选择辅助目标' : '选择加血目标' }}</span>
          </div>
          <div class="target-dialog-desc">
            <span class="target-title-box target-title-sub" :style="{ position: 'relative', top: descTop + 'px', left: descLeft + 'px' }">请选择本次{{ pendingTargetCard?.type === 'defend' ? '防御' : pendingTargetCard?.type === 'support' ? '辅助' : '加血' }}对象</span>
          </div>
          <div class="target-dialog-list">
            <div v-for="player in targetablePlayers" :key="player.userId" class="target-player-wrapper">
              <div class="target-player-label" :style="{ position: 'relative', top: (player.seatNo === 0 ? p1LabelTop : p2LabelTop) + 'px', left: (player.seatNo === 0 ? p1LabelLeft : p2LabelLeft) + 'px' }">
                {{ player.dept || '玩家' }}<span class="target-player-role">（{{ player.userId === user.userId ? '自己' : '队友' }}）</span>
              </div>
              <button
                class="target-player-btn"
                type="button"
                :class="player.seatNo === 0 ? 'target-player-btn-p1' : 'target-player-btn-p2'"
                :disabled="pendingTargetUserId === player.userId"
                :style="{ position: 'relative', top: (player.seatNo === 0 ? p1BtnTop : p2BtnTop) + 'px', left: (player.seatNo === 0 ? p1BtnLeft : p2BtnLeft) + 'px' }"
                @click="confirmTarget(player.userId)"
              />
            </div>
          </div>
          <button class="target-dialog-close" type="button" @click="cancelTargetDialog" aria-label="取消">×</button>
        </div>
      </div>
    </Teleport>


    <!-- 游戏结束结算弹窗 -->
    <Teleport to="body">
      <div v-if="game.isGameOver" class="result-overlay">
        <div class="result-panel">
          <div class="result-bg-layer" :style="{ backgroundImage: `url('${game.isVictory ? resultWinBg : resultLoseBg}')` }"></div>
          <div class="result-content">
            <div class="result-header" :style="{ position: 'relative', top: resultHeaderTop + 'px', left: resultHeaderLeft + 'px' }">
              <h1 :class="game.isVictory ? 'win' : 'lose'">{{ game.isVictory ? '胜利' : '失败' }}</h1>
            </div>
            <div class="stats-panel" :style="{ position: 'relative', top: statsPanelTop + 'px', left: statsPanelLeft + 'px' }">
              <p>对局回合：{{ resultRounds }}</p>
              <p>对局结果：{{ game.isVictory ? '胜利' : '失败' }}</p>
              <p>霸凌者剩余 HP：{{ game.bullyHP }}/{{ game.maxBullyHP }}</p>
              <p>P1 最终血量：{{ resultPlayer1Hp }}/{{ resultPlayer1MaxHp }} <span v-if="resultPlayer1Dead" class="dead-tag">（阵亡）</span></p>
              <p>P2 最终血量：{{ resultPlayer2Hp }}/{{ resultPlayer2MaxHp }} <span v-if="resultPlayer2Dead" class="dead-tag">（阵亡）</span></p>
              <p v-if="game.isVictory" class="points-reward">获得酬劳：+{{ resultRewardMoney }} 金币</p>
            </div>
            <div class="btn-group" :style="{ position: 'relative', top: hallBtnTop + 'px', left: hallBtnLeft + 'px' }">
              <el-button type="primary" size="large" @click="$router.push('/game-hall')">返回大厅</el-button>
              <p v-if="!game.isVictory" class="revive-unavailable">复活仅在队友存活时可用，对局已结束无法复活</p>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 好友掉线弹窗 -->
    <Teleport to="body">
      <div v-if="showDisconnectDialog" class="disconnect-overlay">
        <div class="disconnect-modal">
          <h2 class="disconnect-title">好友已掉线</h2>
          <p class="disconnect-desc">是否等待好友重新连接？</p>
          <p class="disconnect-countdown">{{ disconnectCountdown }} 秒后自动结束对局</p>
          <div class="disconnect-actions">
            <el-button size="large" @click="endMatchDueToDisconnect">结束对局</el-button>
            <el-button type="primary" size="large" @click="showDisconnectDialog = false">
              等待重连（后台计时）
            </el-button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showReviveDialog" class="revive-overlay" @click.self="handleReviveClose">
        <div class="revive-card">
          <h2>观看视频广告复活</h2>
          <p v-if="reviveStatusLoading">正在查询复活状态…</p>
          <template v-else>
            <p>当前可复活次数：{{ reviveStatus?.reviveCount ?? 0 }}/{{ reviveStatus?.reviveLimit ?? 1 }}</p>
            <p>当前血量：{{ reviveStatus?.currentHp ?? 0 }}/{{ reviveStatus?.maxHp ?? 0 }}</p>
            <p v-if="reviveStatus && !reviveStatus.canRevive" class="revive-hint">{{ reviveStatus.message || '当前无法复活' }}</p>
          </template>
          <video
            v-if="canShowRevive"
            ref="reviveVideoRef"
            class="revive-video"
            controls
            playsinline
            @play="onReviveVideoPlay"
            @ended="reviveVideoWatched = true"
            @error="reviveVideoError = '视频加载失败，请检查文件是否存在或文件名是否正确'"
          >
            <source :src="reviveAdVideo" type="video/mp4" />
          </video>
          <p v-if="reviveVideoError" class="revive-error">{{ reviveVideoError }}</p>
          <div class="revive-actions">
            <el-button @click="handleReviveClose">放弃复活</el-button>
            <el-button type="primary" :disabled="!canConfirmRevive" :loading="reviveSubmitting" @click="submitRevive">确认复活</el-button>
          </div>
        </div>
      </div>
    </Teleport>

    <div class="position-rects">
      <div class="pos-rect pos-rect-customer" :style="{ width: '188px', height: '289px', left: '50%', top: '58%' }">
        <img :src="customerImage" alt="顾客" />
      </div>
      <div class="pos-rect pos-rect-player1" :style="{ width: p1RectW + 'px', height: p1RectH + 'px', left: p1RectLeft + '%', top: p1RectTop + '%' }">
        <img class="player-img" :src="player1Img" alt="玩家1" />
        <div class="player-hp-hud">
          <PlayerInfo
            v-if="players[0]"
            :dept="players[0].dept"
            :username="resolvePlayerName(players[0].userId)"
            :stamina="players[0].hp"
            :max-stamina="players[0].maxHp"
            :is-self="players[0]?.userId === user.userId"
            :defense="players[0].defense"
          />
        </div>
        <div v-if="isPlayer1Turn" class="turn-fireflies">
          <span v-for="f in fireflies" :key="f.i" class="firefly" :style="f.style" />
        </div>
      </div>
      <div class="pos-rect pos-rect-player2" :style="{ width: p2RectW + 'px', height: p2RectH + 'px', left: p2RectLeft + '%', top: p2RectTop + '%' }">
        <img class="player-img" :src="player2Img" alt="玩家2" />
        <div class="player-hp-hud">
          <PlayerInfo
            v-if="players[1]"
            :dept="players[1].dept"
            :username="resolvePlayerName(players[1].userId)"
            :stamina="players[1].hp"
            :max-stamina="players[1].maxHp"
            :is-self="players[1]?.userId === user.userId"
            :defense="players[1].defense"
          />
        </div>
        <div v-if="isPlayer2Turn" class="turn-fireflies">
          <span v-for="f in fireflies" :key="f.i" class="firefly" :style="f.style" />
        </div>
      </div>
      <div class="pos-rect pos-rect-bully" :style="{ width: '188px', height: '289px', left: '51%', top: '16%' }">
        <img :src="bullyImg" alt="霸凌者" />
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box, Delete } from '@element-plus/icons-vue'
import { useGameStore } from '@/store/game'
import { useRoomStore } from '@/store/room'
import { useUserStore } from '@/store/user'
//import { abandonMatch, endMatchTurn, getMatchDeck, getMatchDetail, playMatchCard, reconnectMatch } from '@/api'
import { abandonMatch, chooseFirstPlayer, endMatchTurn, getMatchDeck, getMatchDetail, getMatchReviveStatus, getMatchSettlement, playMatchCard, reconnectMatch, requestMatchRevive } from '@/api'
import type { PlayCardPayload } from '@/api'
import { subscribeRoomEvent } from '@/utils/roomSocket'
import { getImageUrl } from '@/utils/imageUrl'
import { formatPlayerName } from '@/utils/playerName'
import bg1 from '@/assets/battle-background2.webp'
import bg2 from '@/assets/battle-background2.webp'
import bg3 from '@/assets/battle-background2.webp'
import bg4 from '@/assets/battle-background2.webp'
import bg5 from '@/assets/battle-background2.webp'
import bg6 from '@/assets/battle-background2.webp'
import choosePlayerBg from '@/assets/battle-playerchoose-bg.webp'
import fundsIcon from '@/assets/battle/funds-icon.webp'
import checkPlayerBtnBg from '@/assets/battle-check-player.webp'
import p1BtnBg from '@/assets/p1.webp'
import p2BtnBg from '@/assets/p2.webp'
import customerImg from '@/assets/battle/customer.webp'
import bullyImg from '@/assets/battle/bully.webp'
import playerImg from '@/assets/battle/player.webp'
import purchaseImg from '@/assets/battle/purchase.webp'
import salesImg from '@/assets/battle/sales.webp'
import reviveAdVideo from '@/assets/revive-ad.mp4'
import resultLoseBg from '@/assets/result-lose-bg.webp'
import resultWinBg from '@/assets/result-win-bg.webp'

import BackButton from '@/components/BackButton.vue'
import PlayerInfo from '@/components/PlayerInfo.vue'
import CardItem from '@/components/CardItem.vue'
import EmployerCard from '@/components/EmployerCard.vue'
import BullyCard from '@/components/BullyCard.vue'
type BattleCard = {
  id: string
  instanceId: string
  name: string
  dept: string
  cost: number
  type: 'attack' | 'defend' | 'draw' | 'consume' | 'support' | 'attack_defend' | 'special' | 'trigger' | 'heal' | 'buff' | string
  description: string
  damage: number
  shield: number
  imageUrl?: string | null
  requiresPlayerTarget?: boolean
}

type BattlePlayer = {
  userId: string
  seatNo: 0 | 1
  dept: string
  maxHp: number
  hp: number
  defense: number
  currentFunds: number
  handCount: number
  deckCount: number
  discardCount: number
  fullDeck: BattleCard[]
  hand: BattleCard[]
  discardPile: BattleCard[]
}

const bgList = [bg1, bg2, bg3, bg4, bg5, bg6]
const bgImage = ref('')
const router = useRouter()
const route = useRoute()
const game = useGameStore()
const room = useRoomStore()
const user = useUserStore()
const px = (n: number) => `${n}px`

const fireflies = Array.from({ length: 8 }, (_, i) => {
  const r = 75 + i * 7 + (i % 3) * 4
  const d = 1.8 + (i % 4) * 0.5 + Math.sin(i * 1.7) * 0.25
  const delay = -(i * 0.35)
  const size = 5 + (i % 3) * 3
  const hue = 45 + (i % 5) * 6
  return {
    i,
    style: {
      '--orbit-r': `${r}px`,
      '--orbit-d': `${d.toFixed(1)}s`,
      '--orbit-delay': `${delay.toFixed(2)}s`,
      '--fly-size': `${size}px`,
      '--fly-hue': hue,
    },
  }
})

const turnNumber = ref(1)
const actionLog = ref<string[]>([])
function addAction(msg: string) {
  actionLog.value.push(msg)
  // 保留最近 20 条记录
  if (actionLog.value.length > 20) actionLog.value.shift()
}
const showDeckModal = ref(false)
const activePlayer = ref<0 | 1>(0)
const players = ref<BattlePlayer[]>([])
const matchDetail = ref<any>(null)
const turnEnded = ref<[boolean, boolean]>([false, false])
const activeVersion = ref(1)
const activePhase = ref('')
const activeMatchId = ref<string>('')
const choosingFirstPlayer = ref(false)
const refreshStateToken = ref(0)
const firstPlayerUserId = ref('')
const secondPlayerUserId = ref('')

const customerTriggered = ref<number | null>(null)
const customerEffectType = ref('')
const customerEffectValue = ref<number | null>(null)
const bullyDefense = ref(0)
const bullyActionText = ref('')
const unsubscribeFns: Array<() => void> = []
let disconnectTimer: ReturnType<typeof setInterval> | null = null
let disconnectTimeout: ReturnType<typeof setTimeout> | null = null
let firstPlayerPollTimer: ReturnType<typeof setInterval> | null = null
let actionPhasePollTimer: ReturnType<typeof setInterval> | null = null
const showDisconnectDialog = ref(false)
const disconnectCountdown = ref(30)
const showReviveDialog = ref(false)
const resultRounds = ref(0)
const settlementLoading = ref(false)
const resultRewardMoney = ref(0)
let pendingWinnerType: number | undefined
const resultPlayers = ref<Array<{ userId?: number | string; currentHp?: number; maxHp?: number; playerStatus?: string; deptType?: string }>>([])
const resultPlayer1Hp = computed(() => resultPlayers.value[0]?.currentHp ?? 0)
const resultPlayer1MaxHp = computed(() => resultPlayers.value[0]?.maxHp ?? 0)
const resultPlayer1Dead = computed(() => resultPlayers.value[0]?.playerStatus === 'DEAD')
const resultPlayer2Hp = computed(() => resultPlayers.value[1]?.currentHp ?? 0)
const resultPlayer2MaxHp = computed(() => resultPlayers.value[1]?.maxHp ?? 0)
const resultPlayer2Dead = computed(() => resultPlayers.value[1]?.playerStatus === 'DEAD')
// 找出倒下的部门名称
const fallenDeptLabel = computed(() => {
  const dead = resultPlayers.value.filter(p => p.playerStatus === 'DEAD')
  if (dead.length === 0) return ''
  return dead.map(p => normalizeDept(p.deptType) || '未知部门').join('、')
})
const reviveStatus = ref<any>(null)
const reviveStatusLoading = ref(false)
const reviveSubmitting = ref(false)
const reviveVideoWatched = ref(false)
const reviveVideoError = ref('')
const reviveVideoRef = ref<HTMLVideoElement | null>(null)
const justRevived = ref(false)

const canConfirmRevive = computed(() => {
  if (reviveStatusLoading.value) return false
  if (!reviveStatus.value?.canRevive) return false
  if (!reviveVideoWatched.value) return false
  if (reviveSubmitting.value) return false
  return true
})
const showTargetDialog = ref(false)
const pendingTargetCard = ref<BattleCard | null>(null)
const pendingTargetUserId = ref('')
const frameW = ref(570)
const frameH = ref(330)
const bgW = ref(780)
const bgH = ref(470)
const p1LabelTop = ref(97)
const p1LabelLeft = ref(32)
const p2LabelTop = ref(97)
const p2LabelLeft = ref(32)
const p1BtnTop = ref(-37)
const p1BtnLeft = ref(0)
const p2BtnTop = ref(-37)
const p2BtnLeft = ref(0)
const cardWidth = ref(230)
const cardCostTop = ref(0)
const cardCostLeft = ref(-1)
const cardCostSize = ref(24)
const cardDeptTop = ref(240)
const cardDeptLeft = ref(20)
const cardNameTop = ref(240)
const cardNameLeft = ref(20)
const cardDescTop = ref(240)
const cardDescLeft = ref(12)
const cardTagTop = ref(240)
const cardTagLeft = ref(20)
const cardEffectTop = ref(0)
const cardEffectLeft = ref(-1)
const cardEffectSize = ref(10)
const titleTop = ref(15)
const titleLeft = ref(0)
const descTop = ref(68)
const descLeft = ref(0)
const customerBoxPos = reactive({ top: 344, left: 118 })
const finishBtn = reactive({ w: 167, h: 83, bottom: -176, right: -170, imgW: 248, imgH: 64 })
const bullyBoxPos = reactive({ top: 8, left: -14 })
const p1RectW = ref(270)
const p1RectH = ref(234)
const p1RectLeft = ref(27)
const p1RectTop = ref(30)

const p2RectW = ref(270)
const p2RectH = ref(234)
const p2RectLeft = ref(71)
const p2RectTop = ref(31)

// 结算弹窗位置调节
const resultHeaderTop = ref(111)
const resultHeaderLeft = ref(42)
const statsPanelTop = ref(113)
const statsPanelLeft = ref(-5)
const hallBtnTop = ref(-11)
const hallBtnLeft = ref(360)

const teammateId = computed(() => {
  const selfId = user.userId
  const teammate = players.value.find(p => p.userId !== selfId)
  return teammate?.userId || ''
})
const customerBoxStyle = computed(() => ({
  position: 'relative' as const,
  zIndex: 20,
  top: px(customerBoxPos.top),
  left: px(customerBoxPos.left),
}))
const bullyBoxStyle = computed(() => ({
  position: 'relative' as const,
  zIndex: 20,
  top: px(bullyBoxPos.top),
  left: px(bullyBoxPos.left),
}))
const finishBtnStyle = computed(() => ({
  right: px(finishBtn.right),
  bottom: px(finishBtn.bottom),
  width: px(finishBtn.w),
  height: px(finishBtn.h),
  backgroundSize: `${px(finishBtn.imgW)} ${px(finishBtn.imgH)}`,
}))
const fundsPos = reactive({ top: 220, left: 25 })
const fundsIndicatorStyle = computed(() => ({
  width: '100%',
  transform: `translate(${px(fundsPos.left)}, ${px(fundsPos.top)})`,
}))
const activeFullDeck = computed(() => currentPlayerDetail.value ? players.value[currentUserSeat.value]?.fullDeck || [] : [])
const activeHand = computed(() => {
  const hand = matchDetail.value?.hand
  if (Array.isArray(hand)) return hand.map(mapCard)
  return players.value[currentUserSeat.value]?.hand || []
})
const activeHandCount = computed(() => matchDetail.value?.players?.[currentUserSeat.value]?.handCount ?? players.value[currentUserSeat.value]?.handCount ?? 0)
const currentPlayerDetail = computed(() => matchDetail.value?.players?.[currentUserSeat.value] ?? null)
const activeDeckCount = computed(() => currentPlayerDetail.value?.deckCount ?? players.value[currentUserSeat.value]?.deckCount ?? 0)
const activeDiscardCount = computed(() => currentPlayerDetail.value?.discardCount ?? players.value[currentUserSeat.value]?.discardCount ?? 0)
const currentFunds = computed(() => currentPlayerDetail.value?.actionPoints ?? 0)
const fundsCap = computed(() => Math.max(3, currentFunds.value))
const targetablePlayers = computed(() => players.value.filter((player) => player.userId))
const isSelectingFirstPlayer = computed(() => activePhase.value === 'SELECT_FIRST_PLAYER')
const localCurrentHp = computed(() => matchDetail.value?.players?.[currentUserSeat.value]?.currentHp ?? players.value[currentUserSeat.value]?.hp ?? 0)
const canShowRevive = computed(() => Boolean(reviveStatus.value?.reviveEnabled && reviveStatus.value?.canRevive && localCurrentHp.value <= 0 && !game.isGameOver))
const canChooseFirstPlayer = computed(() => activePhase.value === 'SELECT_FIRST_PLAYER' && room.isHost && !choosingFirstPlayer.value)
const firstPlayerStatusText = computed(() => {
  const first = matchDetail.value?.firstPlayerUserId ?? firstPlayerUserId.value
  const firstDept = players.value.find((player) => player.userId === String(first))?.dept || '玩家'
  if (activePhase.value === 'SELECT_FIRST_PLAYER') {
    return first ? `已选中：${firstDept}` : '等待房主选择先手'
  }
  if (activePhase.value === 'PLAYER_ACTION') {
    return first ? `已选中：${firstDept}` : '已进入出牌阶段'
  }
  return first ? `已选中：${firstDept}` : '未选择'
})
const currentUserSeat = computed<0 | 1>(() => {
  const normalizeId = (value: unknown) => {
    const id = String(value ?? '').trim()
    if (!id || id === 'undefined' || id === 'null') return ''
    return id
  }
  const currentUserId = normalizeId(room.currentUserId || user.userId)
  const seat = players.value.findIndex((player) => normalizeId(player.userId) === currentUserId)
  return seat === 1 ? 1 : 0
})
const currentUserPlayer = computed(() => players.value[currentUserSeat.value])
const currentTurnPlayer = computed(() => {
  const normalizeId = (value: unknown) => {
    const id = String(value ?? '').trim()
    if (!id || id === 'undefined' || id === 'null') return ''
    return id
  }
  // 根据服务器的 endedTurn 状态判断当前是谁的回合

  // 找到先手玩家
  let firstId = normalizeId(matchDetail.value?.firstPlayerUserId ?? firstPlayerUserId.value)
  let firstPlayer: (typeof players.value)[number] | undefined

  if (firstId) {
    firstPlayer = players.value.find((player) => normalizeId(player.userId) === firstId)
  }

  // 兜底：先手未设置时从后手玩家反推，或根据 endedTurn 推断
    if (!firstPlayer) {
      const secondId = normalizeId(matchDetail.value?.secondPlayerUserId ?? secondPlayerUserId.value)
      if (secondId) {
        firstPlayer = players.value.find((player) => normalizeId(player.userId) && normalizeId(player.userId) !== secondId)
      }
      if (!firstPlayer) {
        if (!turnEnded.value[0] && turnEnded.value[1]) return players.value[0] || null
        if (turnEnded.value[0] && !turnEnded.value[1]) return players.value[1] || null
        if (turnEnded.value[0] && turnEnded.value[1]) return null
        return players.value[0] || null
      }
    }

  // 先手玩家未结束回合 → 先手玩家的回合
  const firstEnded = Boolean(matchDetail.value?.players?.[firstPlayer.seatNo]?.endedTurn ?? turnEnded.value[firstPlayer.seatNo])
  if (!firstEnded) return firstPlayer

  // 先手已结束，找后手玩家
  const secondPlayer = players.value.find((player) => normalizeId(player.userId) && normalizeId(player.userId) !== normalizeId(firstPlayer.userId))
  if (!secondPlayer) return firstPlayer

  // 后手玩家未结束回合 → 后手玩家的回合
  const secondEnded = Boolean(matchDetail.value?.players?.[secondPlayer.seatNo]?.endedTurn ?? turnEnded.value[secondPlayer.seatNo])
  if (!secondEnded) return secondPlayer

  // 双方都结束 → 等待回合结算（boss 攻击阶段）
  return null
})
const isPlayer1Turn = computed(() => currentTurnPlayer.value?.userId === players.value[0]?.userId)
const isPlayer2Turn = computed(() => currentTurnPlayer.value?.userId === players.value[1]?.userId)
const player1Img = computed(() => {
  const dept = players.value[0]?.dept
  if (dept === '采购部') return purchaseImg
  if (dept === '销售部') return salesImg
  return playerImg
})
const player2Img = computed(() => {
  const dept = players.value[1]?.dept
  if (dept === '采购部') return purchaseImg
  if (dept === '销售部') return salesImg
  return playerImg
})
const customerImage = computed(() => {
  const url = getImageUrl(game.employerTrait?.imageUrl)
  console.log('[调试] customerImage employerTrait?.imageUrl:', game.employerTrait?.imageUrl, '→ 最终URL:', url, '→ 兜底:', url || customerImg)
  return url || customerImg
})
const isCurrentUserEnded = computed(() => Boolean(matchDetail.value?.players?.[currentUserSeat.value]?.endedTurn ?? turnEnded.value[currentUserSeat.value]))
const isCurrentUserActiveTurnPlayer = computed(() => {
  const normalizeId = (value: unknown) => {
    const id = String(value ?? '').trim()
    if (!id || id === 'undefined' || id === 'null') return ''
    return id
  }
  return !currentTurnPlayer.value || normalizeId(currentUserPlayer.value?.userId) === normalizeId(currentTurnPlayer.value.userId)
})
const canActWithActivePlayer = computed(() => {
  if (game.isGameOver || isSelectingFirstPlayer.value || isCurrentUserEnded.value) return false
  if (activePhase.value && activePhase.value !== 'PLAYER_ACTION') return false
  return isCurrentUserActiveTurnPlayer.value
})
const customerStatusText = computed(() => {
  if (customerTriggered.value === null) return '顾客机制：等待判定'
  if (!customerTriggered.value) return '顾客机制：本回合未触发'
  return `顾客机制：${effectLabel(customerEffectType.value)} ${formatSignedValue(customerEffectValue.value)}`
})
const actionOrderText = computed(() => {
  if (isSelectingFirstPlayer.value) return '阶段：选择先手'
  return currentTurnPlayer.value ? `当前行动：${currentTurnPlayer.value.dept || '玩家'}` : '当前行动：等待回合结算'
})

const deptLabelMap: Record<string, string> = {
  sales: '销售部',
  purchase: '采购部',
  public: '公共部',
  neutral: '路人部',
  passerby: '路人部',
}

function normalizeDept(dept?: string | null) {
  if (!dept) return ''
  return deptLabelMap[dept.toLowerCase()] || dept
}

function friendNameMap() {
  return new Map(
    user.friends.map((friend) => [String(friend.id), friend.displayName || friend.username || friend.remarkName || '']),
  )
}

function resolvePlayerName(userId: string | number | undefined | null): string {
  const id = String(userId ?? '').trim()
  if (!id || id === 'undefined' || id === 'null') return ''

  if (id === String(user.userId || '')) {
    return formatPlayerName(
      user.username
      || user.profile?.displayName
      || localStorage.getItem('loginUsername')
      || '',
    )
  }

  const friend = user.friends.find((item) => String(item.id) === id)
  if (friend) {
    return formatPlayerName(friend.displayName || friend.username || friend.remarkName || '')
  }

  const roomPlayer = room.players.find((item) => String(item.id) === id)
  if (roomPlayer?.username && !/^玩家\d+$/.test(roomPlayer.username)) {
    return formatPlayerName(roomPlayer.username)
  }

  return ''
}

function syncRoomPlayers(detail: any) {
  // 对局已结束时禁止把 match 详情回写进组队 store，否则会残留已关闭房间的组队 UI
  if (detail?.status === 2 || detail?.phase === 'FINISHED' || game.isGameOver) {
    return
  }
  // 仅在仍有有效房间时同步座位信息，避免用 matchId/roomId 污染 session
  if (!room.roomId && !sessionStorage.getItem('activeRoomId')) {
    return
  }
  room.syncRoomDetail(detail as any, String(user.userId || ''), user.username, friendNameMap())
}

function effectLabel(effectType?: string) {
  const map: Record<string, string> = {
    bully_attack_down: '霸凌者攻击降低',
    bully_attack_up: '霸凌者攻击提升',
    bully_hp_up: '霸凌者血量提升',
    bully_defense_up: '霸凌者防御提升',
    ADD_BOSS_SHIELD: '霸凌者防御提升',
    REDUCE_BOSS_ATTACK: '霸凌者攻击降低',
    DRAW_CARDS: '抽牌',
    ADD_SHIELD: '增加防御',
    HEAL_PLAYER: '恢复体力',
  }
  return map[effectType || ''] || effectType || '未知效果'
}

function formatSignedValue(value?: number | null) {
  if (value == null) return ''
  return value >= 0 ? `+${value}` : `${value}`
}

function mapCard(card: any): BattleCard {
  const id = String(card.instanceId ?? card.cardId ?? card.cardCode ?? '')
  const cardType = (card.cardType ?? card.type ?? 'attack') as BattleCard['type']
  return {
    id,
    instanceId: id,
    name: card.cardName ?? card.name ?? '未知卡牌',
    dept: normalizeDept(card.deptType ?? card.dept),
    cost: card.cost ?? 0,
    type: cardType,
    description: card.description ?? '',
    damage: card.damage ?? card.effectValue ?? card.satisfactionChange ?? 0,
    shield: card.shield ?? card.shieldChange ?? card.defense ?? 0,
    imageUrl: card.imageUrl ?? null,
    requiresPlayerTarget: typeof card.requiresPlayerTarget === 'boolean' ? card.requiresPlayerTarget : undefined,
  }
}

function applyBossHp(detail: any) {
  const current = detail?.bossCurrentHp ?? detail?.boss_current_hp ?? detail?.bossRemainingHp
  const max = detail?.bossMaxHp ?? detail?.boss_max_hp
  if (current != null && current !== '') {
    const hp = Number(current)
    if (Number.isFinite(hp)) game.bullyHP = hp
  }
  if (max != null && max !== '') {
    const hp = Number(max)
    if (Number.isFinite(hp) && hp > 0) game.maxBullyHP = hp
  }
}

function syncToStore(detail: any) {
  if (!detail || typeof detail !== 'object') return
  matchDetail.value = detail
  activeMatchId.value = String(detail.matchId ?? activeMatchId.value)
  turnNumber.value = detail.currentRound ?? detail.roundNo ?? turnNumber.value
  activeVersion.value = detail.version ?? activeVersion.value
  if (detail.phase) {
    activePhase.value = detail.phase
  }
  // 复活后服务器返回非结束状态，重置客户端 game-over 标记
  if (detail.phase !== 'FINISHED' && !detail.matchEnded) {
    game.isGameOver = false
    game.isVictory = false
  }
  firstPlayerUserId.value = detail.firstPlayerUserId != null ? String(detail.firstPlayerUserId) : firstPlayerUserId.value
  const derivedSecond = (detail.players || []).find((item: any) => String(item.userId ?? '') !== String(detail.firstPlayerUserId ?? firstPlayerUserId.value ?? ''))
  secondPlayerUserId.value = detail.secondPlayerUserId != null
    ? String(detail.secondPlayerUserId)
    : (derivedSecond?.userId != null ? String(derivedSecond.userId) : secondPlayerUserId.value)
  customerTriggered.value = detail.customerTriggered ?? customerTriggered.value
  customerEffectType.value = detail.customerEffectType ?? customerEffectType.value
  customerEffectValue.value = detail.customerEffectValue ?? customerEffectValue.value
  bullyDefense.value = detail.bossShield ?? detail.bossDefense ?? detail.bullyShield ?? detail.bullyDefense ?? bullyDefense.value
  bullyActionText.value = detail.bossActionText ?? detail.bullyActionText ?? detail.lastBossActionText ?? bullyActionText.value
  applyBossHp(detail)
  console.log(`[调试] syncToStore 服务端返回 bossCurrentHp: ${detail.bossCurrentHp}, bossMaxHp: ${detail.bossMaxHp}, 当前本地 bullyHP: ${game.bullyHP}`)
  game.bullyName = detail.bossName ?? game.bullyName
  game.bullyMinDamage = detail.bossCurrentAttack ?? detail.bossBaseAttack ?? game.bullyMinDamage
  game.bullyMaxDamage = detail.bossCurrentAttack ?? detail.bossBaseAttack ?? game.bullyMaxDamage
  console.log('[调试] syncToStore customer 原始数据:', JSON.stringify(detail.customer))
  game.employerName = detail.customer?.customerName ?? game.employerName
  game.employerTrait = detail.customer ? {
    id: String(detail.customer.id ?? detail.customer.customerCode ?? ''),
    name: detail.customer.customerName ?? '顾客',
    description: detail.customer.description ?? '',
    imageUrl: detail.customer.imageUrl ?? null,
    helpChance: 0.4,
    helpMin: 0,
    helpMax: 0,
    hinderMin: 0,
    hinderMax: 0,
    effectType: detail.customer.effectType?.includes('hp') ? 'hp' : 'attack',
    effectValue: detail.customer.effectValue ?? 0,
    effectTriggerRate: (detail.customer.triggerChance ?? 40) / 100,
  } : game.employerTrait

  const backendPlayers = (detail.players ?? []).slice(0, 2)
  players.value = backendPlayers.map((item: any, index: number) => ({
    userId: String(item.userId ?? ''),
    seatNo: index as 0 | 1,
    dept: normalizeDept(item.deptType),
    maxHp: item.maxHp ?? (index === 0 ? 15 : 20),
    hp: item.currentHp ?? (index === 0 ? 15 : 20),
    defense: item.shield ?? 0,
    currentFunds: item.actionPoints ?? 0,
    handCount: item.handCount ?? 0,
    deckCount: item.deckCount ?? 0,
    discardCount: item.discardCount ?? 0,
    fullDeck: players.value[index]?.fullDeck || [],
    hand: [],
    discardPile: [],
  }))

  turnEnded.value = backendPlayers.map((item: any) => Boolean(item.endedTurn)) as [boolean, boolean]
}

async function refreshBattleState() {
  if (!activeMatchId.value) return
  const token = ++refreshStateToken.value
  console.log(`[调试] 调用 GET /api/matches/${activeMatchId.value}，refreshStateToken: ${token}`)
  const detail = await getMatchDetail(activeMatchId.value)
  console.log(`[调试] getMatchDetail 响应 phase: ${detail.phase}, matchEnded: ${detail.matchEnded}, bossCurrentHp: ${detail.bossCurrentHp}`)
  const deck = await getMatchDeck(activeMatchId.value)
  if (token !== refreshStateToken.value) {
    console.log(`[调试] refreshBattleState 跳过 (token ${token} != 当前 ${refreshStateToken.value})`)
    return
  }
  syncToStore(detail)

  // 对局已结束 → 不向 room store 同步 match 数据，避免覆盖 App.vue 已执行的房间清理
  // REVIVE_WAIT 阶段不要当成终局，否则会盖掉复活弹窗
  if ((detail.phase === 'FINISHED' || detail.matchEnded) && detail.phase !== 'REVIVE_WAIT' && !justRevived.value) {
    applyGameOver(detail)
    stopActionPhasePoll()
    return
  }

  syncRoomPlayers(detail)

  if (room.currentUserId) {
    room.isHost = String(room.hostUserId || '') === String(room.currentUserId)
  }

  const myUserId = String(room.currentUserId || user.userId || '')
  const normalizedMyUserId = myUserId && myUserId !== 'undefined' && myUserId !== 'null' ? myUserId : ''
  const mySeat = players.value.findIndex((item) => String(item.userId) === normalizedMyUserId)
  const activeSeat = mySeat === -1 ? 0 : mySeat as 0 | 1
  activePlayer.value = activeSeat

  // PLAYER_ACTION / 结算中启动轮询兜底，防止 WebSocket 丢包或双方结束回合后卡住
  if (!game.isGameOver && (detail.phase === 'PLAYER_ACTION' || detail.phase === 'BOSS_ACTION' || detail.phase === 'RECONNECT_WAIT')) {
    startActionPhasePoll()
  } else {
    stopActionPhasePoll()
  }

  const activePlayerState = players.value[activeSeat]
  if (!activePlayerState) return
  activePlayerState.fullDeck = (deck.cards ?? []).map(mapCard)
  activePlayerState.hand = (detail.hand ?? []).map(mapCard)
  activePlayerState.discardPile = []
  activePlayerState.currentFunds = detail.players?.[activeSeat]?.actionPoints ?? activePlayerState.currentFunds
  activePlayerState.handCount = detail.players?.[activeSeat]?.handCount ?? activePlayerState.hand.length
  activePlayerState.deckCount = detail.players?.[activeSeat]?.deckCount ?? activePlayerState.deckCount
  activePlayerState.discardCount = detail.players?.[activeSeat]?.discardCount ?? activePlayerState.discardCount

  if (localCurrentHp.value <= 0 && !game.isGameOver) {
    reviveVideoWatched.value = false
    reviveVideoError.value = ''
    reviveStatusLoading.value = true
    showReviveDialog.value = true
    loadReviveStatus().finally(() => { reviveStatusLoading.value = false })
  }
}

function notifyPlayCardEffects(res: any) {
  const effects = res?.effects ?? []
  const apEffects = effects.filter((e: any) => e.effectType === 'ADD_ACTION_POINTS')
  const immediateAp = apEffects.filter((e: any) => !e.scheduled).reduce((sum: number, e: any) => sum + (e.actualValue ?? e.baseValue ?? 0), 0)
  const scheduledAp = apEffects.filter((e: any) => e.scheduled).reduce((sum: number, e: any) => sum + (e.actualValue ?? e.baseValue ?? 0), 0)
  if (immediateAp > 0 || scheduledAp > 0) {
    const parts: string[] = []
    if (immediateAp > 0) parts.push(`本回合调用机会 +${immediateAp}`)
    if (scheduledAp > 0) parts.push(`下回合调用机会 +${scheduledAp}`)
    ElMessage.success(parts.join('，'))
  }
  if (effects.some((e: any) => e.effectType === 'MULTIPLY_NEXT_CARD')) {
    ElMessage.success('下一张牌的数值效果将翻倍')
  }
  if ((res.appliedMultiplier ?? 1) > 1) {
    ElMessage.success(`数值效果已翻倍（×${res.appliedMultiplier}）`)
  }
  const drawn = effects
    .filter((e: any) => e.effectType === 'DRAW_CARDS' && !e.scheduled)
    .reduce((sum: number, e: any) => sum + (e.actualValue ?? 0), 0)
  if (drawn > 0) {
    ElMessage.success(`抽到 ${drawn} 张牌`)
  }
  const attackDown = effects
    .filter((e: any) => e.effectType === 'REDUCE_BOSS_ATTACK' && !e.scheduled)
    .reduce((sum: number, e: any) => sum + (e.actualValue ?? e.baseValue ?? 0), 0)
  if (attackDown > 0) {
    ElMessage.success(`本回合霸凌者攻击 -${attackDown}`)
  }
  if (effects.some((e: any) => e.effectType === 'ADD_SHIELD' && e.targetType === 'ALL_PLAYERS')) {
    ElMessage.success('双方获得护盾')
  }
  if (effects.some((e: any) => e.effectType === 'HEAL_PLAYER' && e.targetType === 'ALL_PLAYERS')) {
    ElMessage.success('双方恢复体力')
  }
}

async function playCard(card: BattleCard) {
  if (!activeMatchId.value || !canActWithActivePlayer.value) return
  const skipTarget = card.requiresPlayerTarget === false
    || (card.requiresPlayerTarget !== true && (card.type === 'attack' || card.type === 'consume' || card.type === 'draw' || card.type === 'support'))
  // 攻击 / 抽牌 / 消耗 / 辅助 / 全体效果：无需选择玩家目标
  if (skipTarget) {
    try {
      const payload: PlayCardPayload = {
        cardInstanceId: card.instanceId,
        targetType: card.type === 'support' || card.type === 'draw' || card.type === 'defend' || card.type === 'heal'
          ? 'SELF'
          : 'BOSS',
        targetUserId: null,
        clientActionId: `${room.currentUserId}-${activeMatchId.value}-${Date.now()}`,
        expectedVersion: activeVersion.value,
      }
      console.log(`[调试] 调用 POST /api/matches/${activeMatchId.value}/actions/play-card，参数:`, JSON.stringify(payload, null, 2))
      const res = await playMatchCard(activeMatchId.value, payload)
      console.log(`[调试] play-card 响应:`, JSON.stringify(res, null, 2))
      console.log(`[调试] boss HP 变化 → beforeValue: ${res.beforeValue}, afterValue: ${res.afterValue}, effects:`, JSON.stringify(res.effects ?? []))
      notifyPlayCardEffects(res)
      if (res.matchEnded) {
        loadSettlement()
        return
      }
      const hpBefore = game.bullyHP
      await refreshBattleState()
      console.log(`[调试] refreshBattleState 后 boss HP: ${hpBefore} → ${game.bullyHP} (变化: ${game.bullyHP - hpBefore})`)
      await loadReviveStatus().catch(() => {})
      reviveVideoError.value = ''
    } catch (error: any) {
      const msg = error?.message || '出牌失败'
      if (msg.includes('已更新') || msg.includes('刷新')) {
        await refreshBattleState()
        ElMessage.warning('对局状态已刷新，请重新出牌')
      } else {
        ElMessage.closeAll()
        ElMessage.error(msg)
      }
    }
    return
  }

  pendingTargetCard.value = card
  pendingTargetUserId.value = ''
  showTargetDialog.value = true
}

async function endTurn() {
  if (!activeMatchId.value || !canActWithActivePlayer.value) return
  try {
    const payload = {
      clientActionId: `${room.currentUserId}-${activeMatchId.value}-end-${Date.now()}`,
      expectedVersion: activeVersion.value,
    }
    console.log(`[调试] 调用 POST /api/matches/${activeMatchId.value}/actions/end-turn，参数:`, JSON.stringify(payload, null, 2))
    const res = await endMatchTurn(activeMatchId.value, payload)
    console.log(`[调试] end-turn 响应:`, JSON.stringify(res, null, 2))
    if (res.matchEnded) {
      loadSettlement()
      return
    }
    const hpBefore = game.bullyHP
    await refreshBattleState()
    console.log(`[调试] end-turn 后 refreshBattleState，boss HP: ${hpBefore} → ${game.bullyHP} (变化: ${game.bullyHP - hpBefore})`)
  } catch (error: any) {
    const msg = error?.message || '结束回合失败'
    if (msg.includes('已更新') || msg.includes('刷新')) {
      await refreshBattleState()
      ElMessage.warning('对局状态已刷新，请重新操作')
    } else {
      ElMessage.error(msg)
    }
  }
}

async function confirmTarget(targetUserId: string) {
  if (!pendingTargetCard.value || !activeMatchId.value) return
  try {
    const res = await playMatchCard(activeMatchId.value, {
      cardInstanceId: pendingTargetCard.value.instanceId,
      targetType: 'PLAYER',
      targetUserId,
      clientActionId: `${room.currentUserId}-${activeMatchId.value}-${Date.now()}`,
      expectedVersion: activeVersion.value,
    })
    showTargetDialog.value = false
    pendingTargetUserId.value = targetUserId
    pendingTargetCard.value = null
    notifyPlayCardEffects(res)
    if (res.matchEnded) {
      loadSettlement()
      return
    }
    await refreshBattleState()
  } catch (error: any) {
    const msg = error?.message || '出牌失败'
    if (msg.includes('已更新') || msg.includes('刷新')) {
      await refreshBattleState()
      ElMessage.warning('对局状态已刷新，请重新出牌')
    } else {
      ElMessage.error(msg)
    }
  }
}

function cancelTargetDialog() {
  showTargetDialog.value = false
  pendingTargetUserId.value = ''
  pendingTargetCard.value = null
}

function switchPlayer() {
  if (players.value.length < 2) return
  activePlayer.value = activePlayer.value === 0 ? 1 : 0
}

async function loadReviveStatus() {
  if (!activeMatchId.value || !user.userId) return
  reviveStatus.value = await getMatchReviveStatus(activeMatchId.value, user.userId)
  return reviveStatus.value
}

async function submitRevive() {
  if (!activeMatchId.value || !user.userId || reviveSubmitting.value || !reviveVideoWatched.value) return
  reviveSubmitting.value = true
  try {
    const adRequestId = `revive-${activeMatchId.value}-${user.userId}-${Date.now()}`
    const res = await requestMatchRevive(activeMatchId.value, {
      userId: user.userId,
      adRequestId,
      adPlatform: 'manual',
      reviveReason: 'watch_ad',
      adCallbackRaw: JSON.stringify({ completed: true }),
    })
    const hpGained = (res.afterHp ?? 0) - (res.beforeHp ?? 0)
    addAction(`${playerLabel(user.userId)} 复活，恢复 ${hpGained} 点血量`)
    reviveVideoWatched.value = false
    reviveVideoError.value = ''
    showReviveDialog.value = false
    justRevived.value = true
    await refreshBattleState()
    justRevived.value = false
  } finally {
    reviveSubmitting.value = false
  }
}

function onReviveVideoPlay() {
  const video = reviveVideoRef.value
  if (video && video.requestFullscreen) {
    video.requestFullscreen().catch(() => {})
  }
}

function handleReviveClose() {
  showReviveDialog.value = false
  reviveVideoWatched.value = false
  reviveVideoError.value = ''
  if (reviveVideoRef.value) {
    reviveVideoRef.value.pause()
    reviveVideoRef.value.currentTime = 0
  }
}

async function chooseFirst(userId: string) {
  if (!activeMatchId.value || !room.isHost || choosingFirstPlayer.value || !isSelectingFirstPlayer.value) return
  choosingFirstPlayer.value = true
  try {
    await chooseFirstPlayer(activeMatchId.value, userId)
    refreshStateToken.value++
    await refreshBattleState()
    syncRoomPlayers(await getMatchDetail(activeMatchId.value))
  } catch (error: any) {
    ElMessage.error(error?.message || '先手选择失败')
  } finally {
    choosingFirstPlayer.value = false
  }
}

async function handleLeave() {
  try {
    await ElMessageBox.confirm('确定要放弃当前对战吗？进度将不会保存。', '离开对战', {
      confirmButtonText: '确定离开',
      cancelButtonText: '继续对战',
      type: 'warning',
    })
    if (activeMatchId.value) {
      await abandonMatch(activeMatchId.value)
    }
    sessionStorage.removeItem('activeMatchId')
    sessionStorage.removeItem('activeRoomId')
    room.resetMatchMaking()
    game.resetGame()
    user.loadFriends().catch(() => {})
    router.push('/game-hall')
  } catch {
    // cancelled
  }
}

function resolveIsVictory(detail: any): boolean {
  const winnerType = detail?.winnerType ?? pendingWinnerType
  if (detail?.victory === true || Number(winnerType) === 1) return true
  if (detail?.victory === false || Number(winnerType) === 2) return false
  const bossHp = Number(detail?.bossCurrentHp ?? detail?.bossRemainingHp)
  const list = detail?.players ?? []
  if (!Number.isFinite(bossHp) || list.length === 0) return false
  const allAlive = list.every((p: any) => Number(p.currentHp ?? p.remainingHp) > 0)
  return bossHp <= 0 && allAlive
}

function normalizeResultPlayers(players: any[] = []) {
  return players.slice(0, 2).map((p) => {
    const currentHp = Number(p.currentHp ?? p.remainingHp ?? 0)
    return {
      ...p,
      currentHp,
      maxHp: Number(p.maxHp ?? 0),
      playerStatus: currentHp <= 0 ? 'DEAD' : (p.playerStatus || 'ACTIVE'),
    }
  })
}

function pickRewardMoney(detail: any): number {
  const list = detail?.players ?? []
  const mine = list.find((p: any) => String(p.userId) === String(user.userId)) || list[0]
  const awarded = Number(detail?.moneyAwarded ?? mine?.moneyAwarded)
  if (Number.isFinite(awarded) && awarded > 0) return awarded
  return resolveIsVictory(detail) ? 50 : 0
}

function applyGameOver(detail: any) {
  if (justRevived.value) return
  game.isGameOver = true
  game.isVictory = resolveIsVictory(detail)
  resultRounds.value = detail.currentRound ?? detail.roundNo ?? detail.totalRounds ?? 0
  applyBossHp(detail)
  resultPlayers.value = normalizeResultPlayers(detail.players)
  const reward = pickRewardMoney(detail)
  if (reward > 0 || game.isVictory) {
    resultRewardMoney.value = reward
    game.pointsEarned = reward
  }
  pendingWinnerType = undefined
  sessionStorage.removeItem('activeMatchId')
  room.resetMatchMaking()
  void user.loadMe().catch(() => {})
}

async function loadSettlement() {
  if (settlementLoading.value) return
  settlementLoading.value = true
  try {
    const matchId = activeMatchId.value
    if (!matchId) return
    try {
      const settlement = await getMatchSettlement(matchId)
      const mine = (settlement.players ?? []).find((p: any) => String(p.userId) === String(user.userId))
      applyGameOver({
        winnerType: settlement.winnerType,
        victory: settlement.victory,
        currentRound: settlement.totalRounds,
        bossCurrentHp: settlement.bossRemainingHp,
        bossMaxHp: settlement.bossMaxHp,
        players: settlement.players,
        moneyAwarded: mine?.moneyAwarded,
        expAwarded: mine?.expAwarded,
      })
    } catch {
      const detail = await getMatchDetail(matchId)
      applyGameOver(detail)
    }
  } finally {
    settlementLoading.value = false
  }
}

function stopDisconnectTimers() {
  if (disconnectTimer) { clearInterval(disconnectTimer); disconnectTimer = null }
  if (disconnectTimeout) { clearTimeout(disconnectTimeout); disconnectTimeout = null }
}

function stopFirstPlayerPoll() {
  if (firstPlayerPollTimer) { clearInterval(firstPlayerPollTimer); firstPlayerPollTimer = null }
}

function startFirstPlayerPoll() {
  stopFirstPlayerPoll()
  firstPlayerPollTimer = setInterval(() => {
    void refreshFirstPlayerState().then(() => {
      if (!isSelectingFirstPlayer.value) stopFirstPlayerPoll()
    })
  }, 1200)
}

function stopActionPhasePoll() {
  if (actionPhasePollTimer) { clearInterval(actionPhasePollTimer); actionPhasePollTimer = null }
}

function startActionPhasePoll() {
  stopActionPhasePoll()
  actionPhasePollTimer = setInterval(() => {
    if (game.isGameOver) {
      stopActionPhasePoll()
      return
    }
    void refreshBattleState().catch(() => {})
  }, 5000)
}

async function endMatchDueToDisconnect() {
  stopDisconnectTimers()
  showDisconnectDialog.value = false
  const matchId = activeMatchId.value || sessionStorage.getItem('activeMatchId') || ''
  activeMatchId.value = ''
  sessionStorage.removeItem('activeMatchId')
  if (matchId) {
    await abandonMatch(matchId).catch(() => {})
  }
  room.setMatchId('')
  room.resetMatchMaking()
  game.resetGame()
  ElMessage.warning('好友掉线，对局已结束')
  router.push('/game-hall')
}

function startWaitForReconnect() {
  showDisconnectDialog.value = true
  disconnectCountdown.value = 30
  stopDisconnectTimers()

  disconnectTimer = setInterval(() => {
    disconnectCountdown.value--
    if (disconnectCountdown.value <= 0) {
      endMatchDueToDisconnect()
    }
  }, 1000)

  disconnectTimeout = setTimeout(() => {
    endMatchDueToDisconnect()
  }, 30_000)
}

function handleTeammatePresence(data: any) {
  const userId = String(data?.userId ?? data?.data?.userId ?? '')
  if (userId !== teammateId.value) return

  const presenceStatus = data?.presenceStatus ?? data?.data?.presenceStatus
  if (presenceStatus === 'OFFLINE') {
    if (!showDisconnectDialog.value) {
      startWaitForReconnect()
    }
  } else if (presenceStatus && presenceStatus !== 'OFFLINE') {
    if (showDisconnectDialog.value) {
      stopDisconnectTimers()
      showDisconnectDialog.value = false
      ElMessage.success('好友已重新上线，继续对战')
      refreshBattleState()
    }
  }
}

function normalizeActorId(v: unknown) {
  const id = String(v ?? '').trim()
  if (!id || id === 'undefined' || id === 'null') return ''
  return id
}

function playerLabel(userId: unknown) {
  const uid = normalizeActorId(userId)
  const seat = players.value.findIndex(p => normalizeActorId(p.userId) === uid)
  return seat >= 0 ? `P${seat + 1}` : '玩家'
}

function logMatchEvent(type: string, data: any) {
  const d = data?.data ?? data
  switch (type) {
    case 'card.played':
      addAction(`${playerLabel(d?.actorUserId)} 打出「${d?.cardName ?? '未知卡牌'}」`)
      break
    case 'player.turn.ended':
      addAction(`${playerLabel(d?.userId)} 已结束本回合攻击`)
      break
    case 'round.started': {
      const r = d?.roundNo ?? d?.round ?? d?.currentRound ?? '?'
      addAction(`—— 第 ${r} 回合 ——`)
      break
    }
    case 'boss.attack.resolved':
      addAction('Boss 发动攻击')
      break
    case 'match.ended':
      addAction(game.isVictory ? '击败霸凌者！雇主安全了！' : `${fallenDeptLabel.value}倒下，保护失败`)
      break
  }
}

function makeMatchHandler(eventType: string) {
  return async (data: any) => {
    const eventMatchId = String(data?.matchId ?? data?.data?.matchId ?? '')
    if (eventMatchId && eventMatchId !== activeMatchId.value) return
    if (eventType === 'match.ended') {
      const d = data?.data ?? data
      applyGameOver({
        winnerType: d?.winnerType,
        currentRound: d?.currentRound ?? resultRounds.value,
        bossCurrentHp: d?.bossCurrentHp ?? game.bullyHP,
        bossMaxHp: d?.bossMaxHp ?? game.maxBullyHP,
        players: d?.players ?? players.value,
      })
    }
    logMatchEvent(eventType, data)
    await refreshBattleState()
  }
}

async function handleMatchEvent(data: any) {
  const eventMatchId = String(data?.matchId ?? data?.data?.matchId ?? '')
  if (eventMatchId && eventMatchId !== activeMatchId.value) return
  await refreshBattleState()
}

async function handleReviveAvailable(data: any) {
  const eventMatchId = String(data?.matchId ?? data?.data?.matchId ?? '')
  if (eventMatchId && eventMatchId !== activeMatchId.value) return
  await refreshBattleState()
  // 刷新后检查是否需要弹出复活弹窗
  if (localCurrentHp.value <= 0 && !game.isGameOver && !showReviveDialog.value) {
    reviveVideoWatched.value = false
    reviveVideoError.value = ''
    reviveStatusLoading.value = true
    showReviveDialog.value = true
    loadReviveStatus().finally(() => { reviveStatusLoading.value = false })
  }
}

async function refreshFirstPlayerState() {
  if (!activeMatchId.value || !isSelectingFirstPlayer.value) return
  try {
    const detail = await getMatchDetail(activeMatchId.value)
    syncToStore(detail)
    syncRoomPlayers(detail)
  } catch {
    // ignore
  }
}

// 对局结束时自动关闭复活弹窗，防止与结算弹窗重叠
watch(() => game.isGameOver, (over) => {
  if (over && showReviveDialog.value) {
    handleReviveClose()
  }
})

onMounted(async () => {
  const hour = new Date().getHours()
  bgImage.value = bgList[hour % bgList.length]
  console.log('[BattlePage onMounted] route.matchId =', route.params.matchId)
  console.log('[BattlePage onMounted] room.matchId =', room.matchId)
  console.log('[BattlePage onMounted] localStorage =', sessionStorage.getItem('activeMatchId'))
  activeMatchId.value = String(route.params.matchId || room.matchId || sessionStorage.getItem('activeMatchId') || '')
  if (!activeMatchId.value && room.matchId) activeMatchId.value = room.matchId
  if (!activeMatchId.value) {
    ElMessage.error('缺少 matchId，无法进入对局')
    router.push('/matchmaking')
    return
  }
  room.setMatchId(activeMatchId.value)
  sessionStorage.setItem('activeMatchId', activeMatchId.value)
  if (!user.friends.length) {
    await user.loadFriends().catch(() => {})
  }
  try {
    const detail = await reconnectMatch(activeMatchId.value)
    if (detail && typeof detail === 'object' && (detail.matchId || detail.phase || detail.bossCurrentHp != null)) {
      syncToStore(detail)
      syncRoomPlayers(detail)
    }
  } catch {
    // 对局无需重连或后端拒绝重连时，继续以权威查询恢复页面。
  }
  await refreshBattleState()
  if (isSelectingFirstPlayer.value) startFirstPlayerPoll()
  unsubscribeFns.push(
    subscribeRoomEvent('friend.presence.changed', handleTeammatePresence),
    subscribeRoomEvent('match.started', handleMatchEvent),
    subscribeRoomEvent('match.first_player.chosen', handleMatchEvent),
    subscribeRoomEvent('card.played', makeMatchHandler('card.played')),
    subscribeRoomEvent('player.turn.ended', makeMatchHandler('player.turn.ended')),
    subscribeRoomEvent('boss.attack.resolved', makeMatchHandler('boss.attack.resolved')),
    subscribeRoomEvent('round.started', makeMatchHandler('round.started')),
    subscribeRoomEvent('match.reconnecting', handleMatchEvent),
    subscribeRoomEvent('match.recovered', handleMatchEvent),
    subscribeRoomEvent('match.revive.success', (data: any) => {
      const d = data?.data ?? data
      const revivedUserId = String(d?.userId ?? d?.revivedUserId ?? '')
      if (revivedUserId && revivedUserId !== String(user.userId)) {
        const hpGained = (d?.afterHp ?? 0) - (d?.beforeHp ?? 0)
        if (hpGained > 0) {
          addAction(`${playerLabel(revivedUserId)} 复活，恢复 ${hpGained} 点血量`)
        }
      }
      handleMatchEvent(data)
    }),
    subscribeRoomEvent('match.revive.available', handleReviveAvailable),
    subscribeRoomEvent('match.revive.failed', handleMatchEvent),
    subscribeRoomEvent('match.ended', async (data: any) => {
      // match.ended 不做 matchId 过滤，确保放弃/掉线等通知不会因 ID 不匹配被静默丢弃
      logMatchEvent('match.ended', data)
      const d = data?.data ?? data
      if (d?.winnerType != null) pendingWinnerType = Number(d.winnerType)
      await refreshBattleState()
    }),
  )
})

onUnmounted(() => {
  stopDisconnectTimers()
  stopFirstPlayerPoll()
  stopActionPhasePoll()
  unsubscribeFns.splice(0).forEach((unsubscribe) => unsubscribe())
  // 离开对局页时重置游戏状态，确保下一局不会残留旧数据
  if (game.isGameOver) {
    game.resetGame()
  }
})
</script>

<style scoped>
.battle-page {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  isolation: isolate;
}
.battle-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--battle-bg);
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  filter: blur(0.5px) brightness(0.82) saturate(1.12) contrast(1.04);
  transform: scale(1.01);
  z-index: 0;
  pointer-events: none;
}
.battle-main { position: relative; z-index: 1; flex: 1; min-height: 0; display: flex; overflow: visible; }
.battle-footer { position: relative; z-index: 4; }
.battle-stage { flex: 1; display: flex; align-items: center; justify-content: center; position: relative; }
.stage-bg { text-align: center; color: #fff; width: 100%; }
.turn-info {
  position: absolute;
  top: 56px;
  left: var(--space-4);
  font-size: var(--text-3xl);
  font-weight: var(--weight-bold);
  text-align: left;
}
.battle-status-row {
  position: absolute;
  top: 92px;
  left: var(--space-4);
  display: flex;
  gap: var(--space-4);
  flex-wrap: wrap;
  color: rgba(255,255,255,0.82);
  font-size: var(--text-sm);
}
.status-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.battle-status-row span {
  padding: 4px 10px;
  border: 1px solid rgba(255,255,255,0.16);
  border-radius: var(--radius-full);
  background: rgba(0,0,0,0.22);
}
.revive-wait-tag {
  color: #f0c040;
  border-color: rgba(240, 192, 64, 0.5) !important;
  animation: revivePulse 1.5s ease-in-out infinite;
}
.entity-row { display: flex; align-items: center; justify-content: center; gap: var(--space-6); overflow: visible; }
.guard-team {
  width: 280px;
  height: 72px;
  flex-shrink: 0;
  pointer-events: none;
}
.entity-outline-bully { overflow: visible; }
.vs-divider { font-size: var(--text-3xl); font-weight: var(--weight-bold); }
.action-log-panel {
  position: absolute;
  top: var(--space-4);
  right: var(--space-6);
  z-index: 5;
  width: 260px;
  background: url('@/assets/action-log-bg.webp') center/cover no-repeat;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  pointer-events: none;
}
.action-log-title {
  font-size: var(--text-sm);
  color: #4a3020;
  margin-bottom: var(--space-2);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid #8b6914;
  flex-shrink: 0;
}
.action-log-list {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  max-height: 144px;
}
.action-log-item {
  font-size: var(--text-sm);
  color: #3e2723;
  line-height: 1.5;
  padding: var(--space-1) 0;
  border-bottom: 1px dashed rgba(139, 105, 20, 0.3);
}
.action-log-item:last-child {
  border-bottom: none;
  color: #5d3a1a;
  font-weight: var(--weight-bold);
}
.result-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.result-panel {
  position: relative;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-xl);
  text-align: center;
  max-width: 576px;
  width: calc(100vw - 48px);
  overflow: hidden;
}
.result-bg-layer {
  position: absolute;
  inset: -10px;
  background: center/cover no-repeat;
  z-index: 0;
}
.result-content {
  position: relative;
  z-index: 1;
  padding: var(--space-10) var(--space-12);
  transform: scale(0.7);
  transform-origin: top left;
}
.result-header {
  margin-bottom: 0;
  text-align: left;
}
.result-header h1 {
  font-size: calc(var(--text-2xl) * 2);
  margin: 0 0 var(--space-1) 0;
  font-weight: var(--weight-bold);
}
.result-header h1 { color: #4a3020; }
.stats-panel {
  background: var(--color-surface-02);
  border: 1px solid var(--color-border-subtle);
  padding: var(--space-5) var(--space-10) var(--space-5) var(--space-6);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-6);
  text-align: left;
  width: fit-content;
  white-space: nowrap;
}
.stats-panel p { margin: var(--space-2) 0; color: #4a3020; font-size: calc(var(--text-base) * 2); }
.points-reward { color: #5d3a1a; font-weight: var(--weight-bold); font-size: calc(var(--text-xl) * 2); }
.dead-tag { color: #8b3a3a; font-size: var(--text-sm); margin-left: var(--space-1); }
.btn-group { display: flex; gap: var(--space-4); justify-content: center; flex-wrap: wrap; }
.btn-group .el-button { font-size: calc(var(--text-base) * 2); color: #4a3020; }

.revive-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  animation: fadeIn 0.3s ease;
}
.revive-card {
  background: var(--color-surface-02);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-xl);
  padding: var(--space-8) var(--space-10);
  text-align: center;
  max-width: 480px;
  width: calc(100vw - 48px);
  color: var(--color-text-primary);
}
.revive-card h2 {
  font-size: var(--text-2xl);
  margin: 0 0 var(--space-4) 0;
  color: var(--color-accent);
}
.revive-card p {
  margin: var(--space-2) 0;
  font-size: var(--text-md);
}
.revive-video {
  width: 100%;
  max-width: 360px;
  border-radius: var(--radius-md);
  margin: var(--space-4) 0;
  background: #000;
}
.revive-error {
  color: #f56c6c;
  font-size: var(--text-sm);
  margin-top: var(--space-2);
}
.revive-hint {
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}
.revive-actions {
  display: flex;
  gap: var(--space-4);
  justify-content: center;
  margin-top: var(--space-4);
}
.revive-actions .el-button {
  font-size: var(--text-lg);
  padding: var(--space-3) var(--space-8);
}

.revive-unavailable {
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
  margin-top: var(--space-2);
}

.pos-adjuster {
  margin-top: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border: 1px dashed rgba(255,255,255,0.3);
  border-radius: var(--radius-md);
}
.pos-adjuster-title { font-size: var(--text-xs); color: #8b6914; margin-bottom: var(--space-2); }
.pos-adjuster-row { display: flex; gap: var(--space-3); justify-content: center; margin-bottom: var(--space-1); }
.pos-adjuster-row label { font-size: var(--text-xs); color: #4a3020; }
.pos-adjuster-row input { width: 52px; margin-left: var(--space-1); text-align: center; background: rgba(255,255,255,0.6); border: 1px solid #8b6914; border-radius: 3px; color: #4a3020; }
.first-player-overlay {
  position: absolute;
  inset: 0;
  z-index: 100000;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.first-player-overlay::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(4, 8, 12, 0.42);
}
.first-player-card {
  position: relative;
  pointer-events: auto;
  width: min(520px, 88vw);
  padding: var(--space-8);
  border: 1px solid rgba(196, 169, 98, 0.45);
  border-radius: var(--radius-xl);
  background: rgba(11, 19, 27, 0.92);
  color: #fff;
  text-align: center;
  box-shadow: var(--shadow-lg);
}
.first-player-card h2 { margin: 0 0 var(--space-3); font-size: var(--text-3xl); }
.first-player-card p { color: rgba(255,255,255,0.72); }
.first-player-actions { display: flex; gap: var(--space-3); justify-content: center; margin-top: var(--space-6); flex-wrap: wrap; }
.battle-footer {
  padding: 0 var(--space-6) var(--space-2);
  background: transparent;
  border-top: none;
  overflow: visible;
}
.funds-indicator-wrap {
  position: relative;
  z-index: 6;
}
.footer-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: var(--space-4);
}
.draw-pile, .discard-pile {
  width: 92px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #fff;
  cursor: pointer;
  margin-bottom: var(--space-2);
}
.pile-count { font-size: var(--text-2xl); font-weight: var(--weight-bold); }
.pile-label { font-size: var(--text-xs); color: rgba(255,255,255,0.7); }
.center-stack {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
  overflow: visible;
}
.hand-actions-row {
  position: relative;
  width: 100%;
  height: 176px;
  margin-bottom: 4px;
}
.switch-player-btn {
  position: absolute;
  left: -94px;
  bottom: -235px;
  width: 60px;
  height: 152px;
  background: var(--check-player-bg) center/500% auto no-repeat;
  padding: 0;
  cursor: pointer;
  flex: 0 0 auto;
  font-size: 0;
  color: transparent;
  overflow: hidden;
  z-index: 2;
  transition: transform 0.2s ease;
  border: none;
  outline: none;
}
.switch-player-btn:hover:not(:disabled) {
  transform: translateY(-6px);
}
.switch-player-btn:disabled {
  cursor: not-allowed;
  filter: grayscale(0.35) brightness(0.8);
  opacity: 0.72;
}
.finish-btn {
  position: absolute;
  background-color: transparent;
  background-image: url('@/assets/battle-finish.webp');
  background-position: center;
  background-repeat: no-repeat;
  padding: 0;
  cursor: pointer;
  flex: 0 0 auto;
  font-size: 0;
  color: transparent;
  overflow: visible;
  transition: transform 0.2s ease;
  border: none;
  outline: none;
}
.finish-btn:hover:not(:disabled) {
  transform: translateY(-6px);
}
.finish-btn:disabled {
  cursor: not-allowed;
  filter: grayscale(0.35) brightness(0.8);
  opacity: 0.72;
}
.hand-cards {
  width: 100%;
  display: flex;
  gap: 4px;
  justify-content: center;
  align-items: flex-end;
  min-height: 280px;
  padding-top: 0;
  margin-top: 0;
  overflow: visible;
  position: relative;
  z-index: 3;
}
.hand-cards :deep(.card-item) {
  transform: scale(0.88);
  transform-origin: bottom center;
}
.hand-empty {
  color: rgba(255,255,255,0.7);
  padding: var(--space-4);
  white-space: nowrap;
}
.finish-btn:disabled {
  cursor: not-allowed;
  filter: grayscale(0.35) brightness(0.8);
  opacity: 0.72;
}
.deck-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: var(--space-4);
  max-height: min(68vh, 720px);
  overflow-y: auto;
  padding: var(--space-2);
  justify-items: center;
}
.deck-grid :deep(.card-item) {
  pointer-events: none;
  opacity: 1;
  cursor: default;
}
.deck-empty {
  text-align: center;
  color: var(--color-text-secondary);
  padding: var(--space-8);
}

/* —— 位置矩形 —— */
.position-rects {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.pos-rect {
  position: absolute;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.pos-rect-player1,
.pos-rect-player2 {
  overflow: visible;
}
.player-hp-hud {
  position: absolute;
  left: 50%;
  bottom: 14px;
  transform: translateX(-50%);
  z-index: 3;
  pointer-events: none;
}
.me-tag {
  position: absolute;
  left: -36px;
  top: 50%;
  transform: translateY(-50%);
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--color-accent);
  color: var(--color-bg-base);
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  white-space: nowrap;
  pointer-events: none;
}
.turn-fireflies {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  pointer-events: none;
  z-index: 2;
}
.firefly {
  position: absolute;
  width: var(--fly-size);
  height: var(--fly-size);
  border-radius: 50%;
  background: radial-gradient(circle at center,
    rgba(255, 250, 220, 1) 0%,
    rgba(255, 235, 140, 1) 15%,
    rgba(255, 200, 60, 0.7) 45%,
    transparent 100%
  );
  box-shadow:
    0 0 6px 3px rgba(255, 235, 140, 0.9),
    0 0 14px 7px rgba(255, 200, 60, 0.5),
    0 0 28px 14px rgba(255, 170, 30, 0.25);
  pointer-events: none;
  animation: firefly-orbit var(--orbit-d) linear infinite;
  animation-delay: var(--orbit-delay);
}
@keyframes firefly-orbit {
  0% {
    transform: translate(-50%, -50%) rotate(0deg) translateX(var(--orbit-r)) translateY(0px);
    opacity: 0.55;
  }
  12% { opacity: 1; }
  25% {
    transform: translate(-50%, -50%) rotate(90deg) translateX(var(--orbit-r)) translateY(-8px);
    opacity: 0.6;
  }
  37% { opacity: 0.95; }
  50% {
    transform: translate(-50%, -50%) rotate(180deg) translateX(var(--orbit-r)) translateY(6px);
    opacity: 0.5;
  }
  62% { opacity: 1; }
  75% {
    transform: translate(-50%, -50%) rotate(270deg) translateX(var(--orbit-r)) translateY(-10px);
    opacity: 0.55;
  }
  87% { opacity: 0.9; }
  100% {
    transform: translate(-50%, -50%) rotate(360deg) translateX(var(--orbit-r)) translateY(0px);
    opacity: 0.55;
  }
}
.pos-rect img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pos-rect .player-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.funds-icon {
  width: 48px;
  height: 48px;
  vertical-align: middle;
  margin-right: 4px;
}
.funds-indicator {
  font-size: calc(var(--text-base) * 2);
}
/* —— 组件轮廓 —— */
.entity-outline {
  position: relative;
}

</style>

<style>
.target-overlay {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 320px;
  display: flex;
  justify-content: center;
  pointer-events: none;
  z-index: 3000;
}
.target-dialog {
  width: var(--frame-w, min(760px, calc(100vw - 32px)));
  height: var(--frame-h, auto);
  background: var(--choose-player-bg) center / var(--bg-w, 760px) var(--bg-h, auto) no-repeat;
  border: none;
  border-radius: 18px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4);
  color: #fff;
  padding: 92px 40px 42px;
  pointer-events: auto;
  position: relative;
  overflow: visible;
}
.target-dialog::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(11, 19, 27, 0.18);
  pointer-events: none;
}
.target-dialog > * {
  position: relative;
  z-index: 1;
}
.target-dialog-title {
  margin-top: -46px;
  margin-bottom: 6px;
  text-align: center;
}
.target-dialog-desc {
  margin-bottom: 22px;
  text-align: center;
}
.target-title-main {
  font-size: 26px;
  font-weight: 700;
  color: #3d2b1f;
}
.target-title-sub {
  font-size: 18px;
  color: #3d2b1f;
}
.target-player-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.target-player-label {
  font-size: 16px;
  font-weight: 700;
  color: #3d2b1f;
  text-align: center;
  white-space: nowrap;
  z-index: 2;
}
.target-player-role {
  font-size: 13px;
  font-weight: 400;
  color: #5b4a3a;
}
.target-dialog-list {
  position: relative;
  display: flex;
  justify-content: center;
  gap: 88px;
  flex-wrap: nowrap;
  padding-top: 10px;
  min-height: 200px;
}
.target-player-btn {
  position: relative;
  border: none;
  background-color: transparent;
  background-position: center;
  background-repeat: no-repeat;
  background-size: contain;
  width: 190px;
  height: 240px;
  padding: 0;
  cursor: pointer;
  flex: 0 0 auto;
  font-size: 0;
  color: transparent;
  overflow: hidden;
}
.target-player-btn-p1 {
  top: 4px;
}
.target-player-btn-p2 {
  top: 10px;
}
.target-player-btn-p1 {
  background-image: var(--p1-btn-bg);
}
.target-player-btn-p2 {
  background-image: var(--p2-btn-bg);
}
.target-player-btn:disabled {
  cursor: not-allowed;
  filter: grayscale(0.35) brightness(0.8);
  opacity: 0.72;
}
.target-dialog-close {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.5);
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}
.target-dialog-close:hover {
  background: rgba(200,60,60,0.7);
  border-color: #fff;
}
.disconnect-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.disconnect-modal {
  background: #2a2520;
  border: 1px solid var(--color-accent, #c4a962);
  border-radius: 12px;
  padding: 40px 48px;
  text-align: center;
  max-width: 420px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.5);
}
.disconnect-title {
  color: var(--color-danger, #e06060);
  font-size: 22px;
  margin: 0 0 12px;
}
.disconnect-desc {
  color: var(--color-text-secondary, #b0a89a);
  font-size: 15px;
  margin: 0 0 10px;
  line-height: 1.6;
}
.disconnect-countdown {
  color: var(--color-accent, #c4a962);
  font-size: 14px;
  margin: 0 0 24px;
}
.disconnect-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

</style>
