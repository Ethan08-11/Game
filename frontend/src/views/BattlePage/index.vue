<template>
  <div class="battle-page" :class="{ 'is-impact': bullyImpact }" :style="{ '--battle-bg': bgImage ? `url(${bgImage})` : '', '--check-player-bg': `url(${checkPlayerBtnBg})` }">
    <BackButton to="" text="放弃对战" @click="handleLeave" />

    <div class="battle-main">
      <section class="battle-stage">
        <div class="stage-bg">
          <div class="turn-info">回合 {{ turnNumber }}</div>
          <div class="battle-status-row">
            <div class="status-col">
              <span class="status-pill">{{ customerStatusText }}</span>
              <span class="status-pill">{{ actionOrderText }}</span>
            </div>
            <span v-if="activePhase === 'REVIVE_WAIT'" class="status-pill revive-wait-tag">等待复活中…</span>
            <span v-if="bullyDefense > 0" class="status-pill">霸凌者防御：{{ bullyDefense }}</span>
            <span v-if="bullyActionText" class="status-pill">{{ bullyActionText }}</span>
            <span v-if="isSelectingFirstPlayer" class="status-pill">先手状态：{{ firstPlayerStatusText }}</span>
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
          <div class="action-log-panel">
            <div class="action-log-title">战斗记录</div>
            <div ref="actionLogListRef" class="action-log-list">
              <div v-if="actionLog.length === 0" class="action-log-empty">等待行动…</div>
              <div v-for="entry in actionLog" :key="entry.key" class="action-log-item">{{ entry.text }}</div>
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
          <div class="pile-back" :style="cardBackStyle" aria-hidden="true" />
          <span class="pile-count">{{ activeDeckCount }}</span>
          <span class="pile-label">牌库</span>
        </div>

        <div class="center-stack">
          <div class="funds-indicator-wrap funds-indicator-highlight" :style="fundsIndicatorStyle">
            <div class="funds-indicator"><img class="funds-icon" :src="fundsIcon" alt="" />调用机会 {{ currentFunds }}/{{ fundsCap }}</div>
          </div>
          <div class="hand-actions-row">
            <button class="switch-player-btn" type="button" :disabled="game.isGameOver" @click="switchPlayer" :aria-label="`查看玩家 P${activePlayer + 1}`" />
            <button class="finish-btn" type="button" :style="finishBtnStyle" :disabled="!canActWithActivePlayer || bullyFxPlaying" @click="endTurn" aria-label="结束回合" />
          </div>

          <div class="hand-cards" :style="{ '--card-width': cardWidth + 'px', '--cost-top': cardCostTop + 'px', '--cost-left': cardCostLeft + 'px', '--cost-size': cardCostSize + 'px', '--dept-top': cardDeptTop + 'px', '--dept-left': cardDeptLeft + 'px', '--name-top': cardNameTop + 'px', '--name-left': cardNameLeft + 'px', '--desc-top': cardDescTop + 'px', '--desc-left': cardDescLeft + 'px', '--tag-top': cardTagTop + 'px', '--tag-left': cardTagLeft + 'px', '--effect-top': cardEffectTop + 'px', '--effect-left': cardEffectLeft + 'px', '--effect-size': cardEffectSize + 'px' }">
            <template v-if="canRevealHand">
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
            </template>
            <template v-else>
              <div
                v-for="n in hiddenHandCount"
                :key="`hand-back-${n}`"
                class="hand-card-back"
                :style="cardBackStyle"
                aria-hidden="true"
              />
              <div class="hand-wait-hint">等待自己的回合</div>
            </template>
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
      :title="`P${currentUserSeat + 1} 本局卡牌`"
      width="960px"
      class="deck-dialog"
      append-to-body
      :z-index="200000"
    >
      <div v-if="matchDeckCards.length === 0" class="deck-empty">暂无牌组数据</div>
      <template v-else>
        <p class="deck-privacy-hint">本局牌组已抽取完成，可查看全部 {{ matchDeckCards.length }} 张卡牌信息。</p>
        <div class="deck-grid" style="--card-width: 168px">
          <CardItem
            v-for="card in matchDeckCards"
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
      </template>
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
        <div class="result-panel" :class="game.isVictory ? 'is-win' : 'is-lose'">
          <img class="result-bg-img" :src="game.isVictory ? resultWinBg : resultLoseBg" alt="" />
          <div class="result-content" :class="{ 'has-unlock': game.isVictory && !!resultUnlockedCard }">
            <div class="result-header">
              <h1 :class="game.isVictory ? 'win' : 'lose'">{{ game.isVictory ? '胜利' : '失败' }}</h1>
            </div>
            <div class="result-mid">
              <div class="stats-panel">
                <p>对局回合：{{ resultRounds }}</p>
                <p>对局结果：{{ game.isVictory ? '胜利' : '失败' }}</p>
                <p>霸凌者剩余 HP：{{ game.bullyHP }}/{{ game.maxBullyHP }}</p>
                <p>P1 最终血量：{{ resultPlayer1Hp }}/{{ resultPlayer1MaxHp }} <span v-if="resultPlayer1Dead" class="dead-tag">（阵亡）</span></p>
                <p>P2 最终血量：{{ resultPlayer2Hp }}/{{ resultPlayer2MaxHp }} <span v-if="resultPlayer2Dead" class="dead-tag">（阵亡）</span></p>
                <p v-if="game.isVictory" class="points-reward">获得酬劳：+{{ resultRewardMoney }} 金币</p>
              </div>
              <div v-if="game.isVictory && resultUnlockedCard" class="unlock-panel">
                <p class="unlock-title">本局解锁</p>
                <div class="unlock-card-wrap">
                  <CardItem
                    :name="resultUnlockedCard.name"
                    :dept="resultUnlockedCard.deptType || ''"
                    :cost="resultUnlockedCard.cost ?? 0"
                    :type="resultUnlockedCard.cardType || 'support'"
                    :description="resultUnlockedCard.description || ''"
                    :damage="0"
                    :shield="0"
                    :image-url="resultUnlockedCard.imageUrl"
                  />
                </div>
                <p class="unlock-hint">已加入卡牌图鉴</p>
              </div>
              <div v-else-if="game.isVictory && settlementFetched && !resultUnlockedCard" class="unlock-panel unlock-complete">
                <p class="unlock-title">图鉴已集齐</p>
                <p class="unlock-hint">本局没有新的收藏卡</p>
              </div>
            </div>
            <div class="btn-group">
              <el-button class="btn-hall" type="primary" @click="$router.push('/game-hall')">返回大厅</el-button>
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
          <p class="disconnect-desc">是否等待好友重新连接？刷新页面不会判负。</p>
          <p class="disconnect-countdown">最多等待 {{ disconnectCountdown }} 秒</p>
          <div class="disconnect-actions">
            <el-button size="large" @click="endMatchDueToDisconnect">结束对局</el-button>
            <el-button type="primary" size="large" @click="dismissDisconnectDialog">
              继续等待
            </el-button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showReviveDialog" class="revive-overlay">
        <div class="revive-stage" @click.stop>
          <video
            v-if="canShowRevive"
            ref="reviveVideoRef"
            class="revive-video"
            controls
            playsinline
            autoplay
            preload="auto"
            controlslist="nodownload nofullscreen noremoteplayback noplaybackrate"
            disablepictureinpicture
            disableremoteplayback
            @timeupdate="onReviveTimeUpdate"
            @seeking="lockReviveSeek"
            @seeked="lockReviveSeek"
            @ratechange="lockRevivePlaybackRate"
            @contextmenu.prevent
            @ended="reviveVideoWatched = true"
            @error="reviveVideoError = '视频加载失败，请检查文件是否存在或文件名是否正确'"
          >
            <source :src="reviveAdVideo" type="video/mp4" />
          </video>
          <div v-else class="revive-video revive-video-placeholder" />
          <div class="revive-header">
            <h2>观看视频广告复活</h2>
            <p v-if="reviveStatusLoading">正在查询复活状态…</p>
            <template v-else>
              <p>当前可复活次数：{{ reviveStatus?.reviveCount ?? 0 }}/{{ reviveStatus?.reviveLimit ?? 1 }}</p>
              <p>当前血量：{{ reviveStatus?.currentHp ?? 0 }}/{{ reviveStatus?.maxHp ?? 0 }}</p>
              <p v-if="reviveStatus && !reviveStatus.canRevive" class="revive-hint">{{ reviveStatus.message || '当前无法复活' }}</p>
              <p v-else-if="reviveRemainingSeconds != null" class="revive-hint">请看完视频后确认复活，剩余 {{ reviveRemainingSeconds }} 秒</p>
            </template>
            <p v-if="reviveVideoError" class="revive-error">{{ reviveVideoError }}</p>
          </div>
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
      <div class="pos-rect pos-rect-player1" ref="player1RectRef" :class="{ 'is-struck': struckSeats.includes(0) }" :style="{ width: p1RectW + 'px', height: p1RectH + 'px', left: p1RectLeft + '%', top: p1RectTop + '%' }">
        <img class="player-img" :src="player1Img" alt="玩家1" />
        <div class="player-hp-hud" :class="{ 'is-flash': flashSeats.includes(0) }">
          <PlayerInfo
            v-if="players[0]"
            :dept="players[0].dept"
            :username="resolvePlayerName(players[0].userId)"
            :stamina="players[0].hp"
            :max-stamina="players[0].maxHp"
            :is-self="sameBattleUserId(players[0]?.userId, user.userId)"
            :defense="players[0].defense"
          />
        </div>
        <div v-if="isPlayer1Turn" class="turn-fireflies">
          <span v-for="f in fireflies" :key="f.i" class="firefly" :style="f.style" />
        </div>
      </div>
      <div class="pos-rect pos-rect-player2" ref="player2RectRef" :class="{ 'is-struck': struckSeats.includes(1) }" :style="{ width: p2RectW + 'px', height: p2RectH + 'px', left: p2RectLeft + '%', top: p2RectTop + '%' }">
        <img class="player-img" :src="player2Img" alt="玩家2" />
        <div class="player-hp-hud" :class="{ 'is-flash': flashSeats.includes(1) }">
          <PlayerInfo
            v-if="players[1]"
            :dept="players[1].dept"
            :username="resolvePlayerName(players[1].userId)"
            :stamina="players[1].hp"
            :max-stamina="players[1].maxHp"
            :is-self="sameBattleUserId(players[1]?.userId, user.userId)"
            :defense="players[1].defense"
          />
        </div>
        <div v-if="isPlayer2Turn" class="turn-fireflies">
          <span v-for="f in fireflies" :key="f.i" class="firefly" :style="f.style" />
        </div>
      </div>
      <div class="pos-rect pos-rect-bully" ref="bullyRectRef" :class="{ 'is-charging': bullyCharging }" :style="{ width: '188px', height: '289px', left: '51%', top: '16%' }">
        <img :src="bullyImg" alt="霸凌者" />
      </div>
    </div>

    <div class="bully-fx-layer" aria-hidden="true">
      <div v-if="bullySlash" class="bully-slash" :class="{ blocked: bullySlash.blocked }" :style="bullySlash.style" />
      <div
        v-for="hit in bullyFloats"
        :key="hit.id"
        class="bully-float"
        :class="{ blocked: hit.blocked }"
        :style="hit.style"
      >{{ hit.text }}</div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useGameStore } from '@/store/game'
import { useRoomStore } from '@/store/room'
import { useUserStore } from '@/store/user'
//import { abandonMatch, endMatchTurn, getMatchDeck, getMatchDetail, playMatchCard, reconnectMatch } from '@/api'
import { abandonMatch, chooseFirstPlayer, declineMatchRevive, endMatchTurn, findSettlementPlayer, getMatchDeck, getMatchDetail, getMatchReviveStatus, getMatchSettlement, playMatchCard, reconnectMatch, requestMatchRevive, unlockedCardFromSettlement } from '@/api'
import type { PlayCardPayload, UnlockedCollectibleCard } from '@/api'
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
import bundledCardBack from '@/assets/cards/Card_Back.webp'

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
  zone?: string
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
const cardBackFallback = getImageUrl('/images/cards/Card_Back.webp') || '/images/cards/Card_Back.webp'
const cardBackImg = bundledCardBack || cardBackFallback
const cardBackStyle = {
  backgroundImage: [cardBackImg, cardBackFallback]
    .filter((url, index, list) => url && list.indexOf(url) === index)
    .map((url) => `url("${url}")`)
    .join(', '),
}
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
type ActionLogEntry = { key: string; text: string }
const actionLog = ref<ActionLogEntry[]>([])
const actionLogListRef = ref<HTMLElement | null>(null)
const loggedActionKeys = new Set<string>()
const bullyRectRef = ref<HTMLElement | null>(null)
const player1RectRef = ref<HTMLElement | null>(null)
const player2RectRef = ref<HTMLElement | null>(null)
const bullyCharging = ref(false)
const bullyImpact = ref(false)
const bullySlash = ref<{ style: Record<string, string>; blocked: boolean } | null>(null)
const bullyFloats = ref<Array<{ id: number; text: string; blocked: boolean; style: Record<string, string> }>>([])
const struckSeats = ref<number[]>([])
const flashSeats = ref<number[]>([])
const bullyFxPlaying = ref(false)
const pendingReviveAfterFx = ref(false)
let lastBullyFxRound: number | string | null = null
let bullyFloatSeq = 0

function scrollActionLogToLatest() {
  void nextTick(() => {
    const el = actionLogListRef.value
    if (!el) return
    el.scrollTop = el.scrollHeight
  })
}

function addAction(msg: string, key?: string) {
  const id = String(key || msg)
  if (!id || loggedActionKeys.has(id)) return
  loggedActionKeys.add(id)
  actionLog.value.push({ key: id, text: msg })
  if (actionLog.value.length > 30) {
    const removed = actionLog.value.shift()
    if (removed) loggedActionKeys.delete(removed.key)
  }
  scrollActionLogToLatest()
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
const settlementFetched = ref(false)
const resultRewardMoney = ref(0)
const resultUnlockedCard = ref<UnlockedCollectibleCard | null>(null)
let settlementPromise: Promise<void> | null = null
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
const reviveLastTime = ref(0)
let reviveSeekLocking = false
const reviveDialogDismissed = ref(false)
const justRevived = ref(false)
const reviveRemainingSeconds = ref<number | null>(null)
let reviveWatchHeartbeatTimer: ReturnType<typeof setInterval> | null = null
let reviveCountdownTimer: ReturnType<typeof setInterval> | null = null

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
const matchDeckCards = computed(() => activeFullDeck.value)
const activeHand = computed(() => {
  const source = Array.isArray(matchDetail.value?.hand)
    ? matchDetail.value.hand
    : (players.value[currentUserSeat.value]?.hand || [])
  return (source as unknown[]).map(mapCard).filter((card: BattleCard) => card.zone !== 'DECK')
})
const currentPlayerDetail = computed(() => findDetailPlayer(selfUserId()) ?? matchDetail.value?.players?.[currentUserSeat.value] ?? null)
const activeHandCount = computed(() => {
  const fromDetail = Number(currentPlayerDetail.value?.handCount)
  const fromSeat = Number(players.value[currentUserSeat.value]?.handCount)
  const fromDeck = (players.value[currentUserSeat.value]?.fullDeck || []).filter((card: BattleCard) => card.zone === 'HAND').length
  const fromHand = activeHand.value.length
  return Math.max(
    Number.isFinite(fromDetail) ? fromDetail : 0,
    Number.isFinite(fromSeat) ? fromSeat : 0,
    fromDeck,
    fromHand,
    0,
  )
})
const activeDeckCount = computed(() => currentPlayerDetail.value?.deckCount ?? players.value[currentUserSeat.value]?.deckCount ?? 0)
const activeDiscardCount = computed(() => currentPlayerDetail.value?.discardCount ?? players.value[currentUserSeat.value]?.discardCount ?? 0)
const currentFunds = computed(() => currentPlayerDetail.value?.actionPoints ?? 0)
const fundsCap = computed(() => Math.max(3, currentFunds.value))
const targetablePlayers = computed(() => players.value.filter((player) => player.userId))
const isSelectingFirstPlayer = computed(() => activePhase.value === 'SELECT_FIRST_PLAYER')
const localCurrentHp = computed(() => currentPlayerDetail.value?.currentHp ?? players.value[currentUserSeat.value]?.hp ?? 0)
const canShowRevive = computed(() => Boolean(reviveStatus.value?.reviveEnabled && reviveStatus.value?.canRevive && localCurrentHp.value <= 0 && !game.isGameOver))
const canChooseFirstPlayer = computed(() => activePhase.value === 'SELECT_FIRST_PLAYER' && room.isHost && !choosingFirstPlayer.value)
const firstPlayerStatusText = computed(() => {
  const first = matchDetail.value?.firstPlayerUserId ?? firstPlayerUserId.value
  const firstDept = players.value.find((player) => sameBattleUserId(player.userId, first))?.dept || '玩家'
  if (activePhase.value === 'SELECT_FIRST_PLAYER') {
    return first ? `已选中：${firstDept}` : '等待房主选择先手'
  }
  if (activePhase.value === 'PLAYER_ACTION') {
    return first ? `已选中：${firstDept}` : '已进入出牌阶段'
  }
  return first ? `已选中：${firstDept}` : '未选择'
})
function sameBattleUserId(a: unknown, b: unknown) {
  const left = String(a ?? '').trim()
  const right = String(b ?? '').trim()
  if (!left || !right || left === 'undefined' || left === 'null') return false
  return left === right
}

function selfUserId() {
  return String(user.userId || room.currentUserId || localStorage.getItem('userId') || '').trim()
}

function findDetailPlayer(userId: unknown) {
  const list = matchDetail.value?.players ?? []
  return list.find((item: any) => sameBattleUserId(item.userId, userId)) ?? null
}

function playerHasEndedTurn(player: { userId?: string; seatNo?: 0 | 1 } | null | undefined) {
  if (!player) return false
  const fromDetail = findDetailPlayer(player.userId)?.endedTurn
  const raw = fromDetail ?? matchDetail.value?.players?.[player.seatNo === 1 ? 1 : 0]?.endedTurn ?? turnEnded.value[player.seatNo === 1 ? 1 : 0]
  return Number(raw) === 1 || raw === true
}

const currentUserSeat = computed<0 | 1>(() => {
  const seat = players.value.findIndex((player) => sameBattleUserId(player.userId, selfUserId()))
  return seat === 1 ? 1 : 0
})
const currentUserPlayer = computed(() => players.value[currentUserSeat.value])
const currentTurnPlayer = computed(() => {
  let firstId = String(matchDetail.value?.firstPlayerUserId ?? firstPlayerUserId.value ?? '').trim()
  let firstPlayer: (typeof players.value)[number] | undefined

  if (firstId) {
    firstPlayer = players.value.find((player) => sameBattleUserId(player.userId, firstId))
  }

  if (!firstPlayer) {
    const secondId = String(matchDetail.value?.secondPlayerUserId ?? secondPlayerUserId.value ?? '').trim()
    if (secondId) {
      firstPlayer = players.value.find((player) => player.userId && !sameBattleUserId(player.userId, secondId))
    }
    if (!firstPlayer) {
      if (!turnEnded.value[0] && turnEnded.value[1]) return players.value[0] || null
      if (turnEnded.value[0] && !turnEnded.value[1]) return players.value[1] || null
      if (turnEnded.value[0] && turnEnded.value[1]) return null
      return players.value[0] || null
    }
  }

  if (!playerHasEndedTurn(firstPlayer)) return firstPlayer

  const secondPlayer = players.value.find((player) => player.userId && !sameBattleUserId(player.userId, firstPlayer.userId))
  if (!secondPlayer) return firstPlayer
  if (!playerHasEndedTurn(secondPlayer)) return secondPlayer
  return null
})
const isPlayer1Turn = computed(() => sameBattleUserId(currentTurnPlayer.value?.userId, players.value[0]?.userId))
const isPlayer2Turn = computed(() => sameBattleUserId(currentTurnPlayer.value?.userId, players.value[1]?.userId))
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
const isCurrentUserEnded = computed(() => playerHasEndedTurn(currentUserPlayer.value))
const isCurrentUserActiveTurnPlayer = computed(() => {
  if (!currentTurnPlayer.value) return false
  return sameBattleUserId(selfUserId(), currentTurnPlayer.value.userId)
    || sameBattleUserId(currentUserPlayer.value?.userId, currentTurnPlayer.value.userId)
})
const canActWithActivePlayer = computed(() => {
  if (game.isGameOver || isSelectingFirstPlayer.value || isCurrentUserEnded.value) return false
  if (activePhase.value && activePhase.value !== 'PLAYER_ACTION') return false
  return isCurrentUserActiveTurnPlayer.value
})
const canRevealHand = computed(() => {
  if (game.isGameOver) return false
  if (isSelectingFirstPlayer.value) return true
  if (activePhase.value !== 'PLAYER_ACTION') return false
  return isCurrentUserActiveTurnPlayer.value && !isCurrentUserEnded.value
})
const hiddenHandCount = computed(() => Math.max(activeHandCount.value, activeHand.value.length, 5))
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
    zone: card.zone,
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
    void loadSettlement()
    return
  }

  syncRoomPlayers(detail)

  if (room.currentUserId) {
    const host = (detail.players ?? []).find((item: any) => Number(item.seatNo) === 1)
    if (host?.userId != null) {
      room.hostUserId = String(host.userId)
    }
    room.isHost = String(room.hostUserId || '') === String(room.currentUserId)
  }

  const myUserId = selfUserId()
  const mySeat = players.value.findIndex((item) => sameBattleUserId(item.userId, myUserId))
  const activeSeat = mySeat === -1 ? 0 : mySeat as 0 | 1
  activePlayer.value = activeSeat

  // PLAYER_ACTION / 结算中启动轮询兜底，防止 WebSocket 丢包或双方结束回合后卡住
  if (!game.isGameOver && (detail.phase === 'PLAYER_ACTION' || detail.phase === 'BOSS_ACTION' || detail.phase === 'RECONNECT_WAIT' || detail.phase === 'REVIVE_WAIT')) {
    startActionPhasePoll()
  } else {
    stopActionPhasePoll()
  }

  const activePlayerState = players.value[activeSeat]
  if (!activePlayerState) return
  const myState = (detail.players ?? []).find((item: any) => sameBattleUserId(item.userId, myUserId))
  activePlayerState.fullDeck = (deck.cards ?? []).map(mapCard)
  activePlayerState.hand = (detail.hand ?? []).map(mapCard)
  activePlayerState.discardPile = []
  activePlayerState.currentFunds = myState?.actionPoints ?? detail.players?.[activeSeat]?.actionPoints ?? activePlayerState.currentFunds
  activePlayerState.handCount = myState?.handCount ?? detail.players?.[activeSeat]?.handCount ?? activePlayerState.hand.length
  activePlayerState.deckCount = myState?.deckCount ?? detail.players?.[activeSeat]?.deckCount ?? activePlayerState.deckCount
  activePlayerState.discardCount = myState?.discardCount ?? detail.players?.[activeSeat]?.discardCount ?? activePlayerState.discardCount

  if (localCurrentHp.value <= 0 && !game.isGameOver && !reviveDialogDismissed.value) {
    if (bullyFxPlaying.value) {
      pendingReviveAfterFx.value = true
    } else {
      openReviveDialog()
    }
  } else if (localCurrentHp.value > 0) {
    reviveDialogDismissed.value = false
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
      logMatchEvent('card.played', res)
      notifyPlayCardEffects(res)
      if (res.matchEnded) {
        await loadSettlement()
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

function waitFx(ms: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms)
  })
}

function extractBossAttack(payload: any) {
  const data = unwrapMatchEvent(payload) ?? payload ?? {}
  const targets = data.bossAttackTargets ?? data.targets ?? []
  return {
    targets: Array.isArray(targets) ? targets : [],
    round: data.resolvedRound ?? data.currentRound ?? data.version ?? null,
  }
}

function playerRectBySeat(seat: number) {
  return seat === 1 ? player2RectRef.value : player1RectRef.value
}

function slashStyle(fromEl: HTMLElement, toEl: HTMLElement) {
  const from = fromEl.getBoundingClientRect()
  const to = toEl.getBoundingClientRect()
  const x1 = from.left + from.width * 0.5
  const y1 = from.top + from.height * 0.62
  const x2 = to.left + to.width * 0.5
  const y2 = to.top + to.height * 0.4
  const dx = x2 - x1
  const dy = y2 - y1
  const len = Math.max(90, Math.hypot(dx, dy))
  const angle = Math.atan2(dy, dx) * 180 / Math.PI
  return {
    left: `${x1}px`,
    top: `${y1}px`,
    width: `${len}px`,
    transform: `rotate(${angle}deg)`,
  }
}

function floatStyle(targetEl: HTMLElement) {
  const box = targetEl.getBoundingClientRect()
  return {
    left: `${box.left + box.width * 0.5}px`,
    top: `${box.top + box.height * 0.28}px`,
  }
}

async function strikeSeat(seat: number, target: any) {
  const toEl = playerRectBySeat(seat)
  const fromEl = bullyRectRef.value
  if (!fromEl || !toEl) return
  const hpDamage = Math.max(0, Number(target?.hpDamage ?? 0))
  const absorbed = Math.max(0, Number(target?.absorbedDamage ?? 0))
  const blocked = hpDamage <= 0 && absorbed > 0
  bullySlash.value = { style: slashStyle(fromEl, toEl), blocked }
  bullyImpact.value = true
  struckSeats.value = [...new Set([...struckSeats.value, seat])]
  flashSeats.value = [...new Set([...flashSeats.value, seat])]
  bullyFloatSeq += 1
  bullyFloats.value = [
    ...bullyFloats.value,
    {
      id: bullyFloatSeq,
      text: blocked ? '格挡' : `-${hpDamage || Number(target?.attack ?? 0)}`,
      blocked,
      style: floatStyle(toEl),
    },
  ]
  const floatId = bullyFloatSeq
  window.setTimeout(() => {
    bullyFloats.value = bullyFloats.value.filter((item) => item.id !== floatId)
  }, 900)
  await waitFx(240)
  bullySlash.value = null
  bullyImpact.value = false
  window.setTimeout(() => {
    struckSeats.value = struckSeats.value.filter((item) => item !== seat)
    flashSeats.value = flashSeats.value.filter((item) => item !== seat)
  }, 420)
}

async function playBullyAttackFx(rawTargets: any[], round: number | string | null) {
  if (!rawTargets.length) return
  if (round != null && String(round) === String(lastBullyFxRound)) return
  if (bullyFxPlaying.value) return
  lastBullyFxRound = round
  bullyFxPlaying.value = true
  try {
    await nextTick()
    const ordered = [...rawTargets].sort((a, b) => {
      const seatA = players.value.findIndex((item) => sameBattleUserId(item.userId, a?.userId))
      const seatB = players.value.findIndex((item) => sameBattleUserId(item.userId, b?.userId))
      return seatA - seatB
    })
    bullyCharging.value = true
    await waitFx(280)
    for (const target of ordered) {
      const seat = players.value.findIndex((item) => sameBattleUserId(item.userId, target?.userId))
      if (seat < 0) continue
      await strikeSeat(seat, target)
      await waitFx(80)
    }
    await waitFx(360)
  } finally {
    bullyCharging.value = false
    bullySlash.value = null
    bullyImpact.value = false
    bullyFxPlaying.value = false
    if (pendingReviveAfterFx.value && localCurrentHp.value <= 0 && !game.isGameOver) {
      pendingReviveAfterFx.value = false
      openReviveDialog()
    }
  }
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
    logMatchEvent('player.turn.ended', res)
    const attack = extractBossAttack(res)
    if (attack.targets.length) {
      await playBullyAttackFx(attack.targets, attack.round)
    }
    if (res.matchEnded) {
      await loadSettlement()
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
    logMatchEvent('card.played', res)
    if (res.matchEnded) {
      await loadSettlement()
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
  const remaining = Number(reviveStatus.value?.remainingSeconds)
  reviveRemainingSeconds.value = Number.isFinite(remaining) ? remaining : null
  return reviveStatus.value
}

function stopReviveWatchHeartbeat() {
  if (reviveWatchHeartbeatTimer) {
    clearInterval(reviveWatchHeartbeatTimer)
    reviveWatchHeartbeatTimer = null
  }
  if (reviveCountdownTimer) {
    clearInterval(reviveCountdownTimer)
    reviveCountdownTimer = null
  }
}

function startReviveWatchHeartbeat() {
  stopReviveWatchHeartbeat()
  reviveWatchHeartbeatTimer = setInterval(() => {
    if (!showReviveDialog.value || game.isGameOver) {
      stopReviveWatchHeartbeat()
      return
    }
    void loadReviveStatus().catch(() => {})
  }, 8000)
  reviveCountdownTimer = setInterval(() => {
    if (reviveRemainingSeconds.value == null) return
    reviveRemainingSeconds.value = Math.max(0, reviveRemainingSeconds.value - 1)
  }, 1000)
}

function onReviveTimeUpdate() {
  const video = reviveVideoRef.value
  if (!video || video.seeking || reviveSeekLocking) return
  reviveLastTime.value = video.currentTime
}

function lockReviveSeek() {
  const video = reviveVideoRef.value
  if (!video || reviveSeekLocking) return
  if (Math.abs(video.currentTime - reviveLastTime.value) <= 0.35) return
  reviveSeekLocking = true
  video.currentTime = reviveLastTime.value
  window.setTimeout(() => { reviveSeekLocking = false }, 0)
}

function lockRevivePlaybackRate() {
  const video = reviveVideoRef.value
  if (video && video.playbackRate !== 1) {
    video.playbackRate = 1
  }
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
    reviveDialogDismissed.value = false
    justRevived.value = true
    stopReviveWatchHeartbeat()
    await refreshBattleState()
    justRevived.value = false
  } finally {
    reviveSubmitting.value = false
  }
}

function openReviveDialog() {
  if (showReviveDialog.value || game.isGameOver || reviveDialogDismissed.value) return
  reviveVideoWatched.value = false
  reviveVideoError.value = ''
  reviveLastTime.value = 0
  reviveSeekLocking = false
  reviveRemainingSeconds.value = null
  reviveStatusLoading.value = true
  showReviveDialog.value = true
  startReviveWatchHeartbeat()
  loadReviveStatus().finally(() => { reviveStatusLoading.value = false })
}

function handleReviveClose() {
  const alreadyOver = game.isGameOver
  reviveDialogDismissed.value = true
  showReviveDialog.value = false
  reviveVideoWatched.value = false
  reviveVideoError.value = ''
  reviveLastTime.value = 0
  reviveSeekLocking = false
  reviveRemainingSeconds.value = null
  stopReviveWatchHeartbeat()
  if (reviveVideoRef.value) {
    reviveVideoRef.value.pause()
    reviveVideoRef.value.currentTime = 0
  }
  if (!alreadyOver && activeMatchId.value) {
    void declineMatchRevive(activeMatchId.value).catch(() => {})
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

function currentBattleUserId() {
  return String(user.userId || room.currentUserId || localStorage.getItem('userId') || '')
}

function captureUnlockFromPlayers(list: any[] | undefined) {
  const unlocked = unlockedCardFromSettlement(findSettlementPlayer(list, currentBattleUserId()))
  if (unlocked) resultUnlockedCard.value = unlocked
}

function pickRewardMoney(detail: any): number {
  const list = detail?.players ?? []
  const mine = findSettlementPlayer(list, currentBattleUserId()) || list[0]
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
  if (game.isVictory) {
    captureUnlockFromPlayers(detail.players)
  } else {
    resultUnlockedCard.value = null
  }
  pendingWinnerType = undefined
  sessionStorage.removeItem('activeMatchId')
  room.resetMatchMaking()
  void user.loadMe().catch(() => {})
}

async function loadSettlement() {
  if (settlementPromise) return settlementPromise
  settlementPromise = (async () => {
    const matchId = activeMatchId.value
    if (!matchId) return
    for (let attempt = 0; attempt < 4; attempt++) {
      try {
        const settlement = await getMatchSettlement(matchId)
        const mine = findSettlementPlayer(settlement.players, currentBattleUserId())
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
        const unlocked = unlockedCardFromSettlement(mine)
        if (unlocked) resultUnlockedCard.value = unlocked
        settlementFetched.value = true
        return
      } catch {
        await new Promise((resolve) => setTimeout(resolve, 350 * (attempt + 1)))
      }
    }
    try {
      const detail = await getMatchDetail(matchId)
      applyGameOver(detail)
    } catch {
      // ignore
    }
    settlementFetched.value = true
  })()
  try {
    await settlementPromise
  } finally {
    settlementPromise = null
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
  disconnectCountdown.value = 60
  stopDisconnectTimers()

  disconnectTimer = setInterval(() => {
    if (disconnectCountdown.value > 0) {
      disconnectCountdown.value--
    }
    if (disconnectCountdown.value <= 0) {
      stopDisconnectTimers()
    }
  }, 1000)
}

function dismissDisconnectDialog() {
  showDisconnectDialog.value = false
}

function handleTeammatePresence(data: any) {
  const userId = String(data?.userId ?? data?.data?.userId ?? '')
  if (userId !== teammateId.value) return

  const presenceStatus = data?.presenceStatus ?? data?.data?.presenceStatus
  if (presenceStatus && presenceStatus !== 'OFFLINE') {
    if (showDisconnectDialog.value) {
      stopDisconnectTimers()
      showDisconnectDialog.value = false
      ElMessage.success('好友已重新上线，继续对战')
      refreshBattleState()
    }
  }
}

function handleMatchReconnecting(data: any) {
  const payload = unwrapMatchEvent(data)
  const uid = String(payload?.userId ?? data?.userId ?? '')
  if (uid && sameBattleUserId(uid, selfUserId())) {
    void handleMatchEvent(data)
    return
  }
  if (uid && sameBattleUserId(uid, teammateId.value) && !showDisconnectDialog.value) {
    startWaitForReconnect()
  }
  void handleMatchEvent(data)
}

function handleMatchRecovered(data: any) {
  if (showDisconnectDialog.value) {
    stopDisconnectTimers()
    showDisconnectDialog.value = false
    ElMessage.success('好友已重新连接，继续对战')
  }
  void handleMatchEvent(data)
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

function unwrapMatchEvent(data: any, message?: any) {
  if (data && typeof data === 'object' && (data.actorUserId != null || data.cardName != null || data.userId != null || data.currentRound != null || data.actionId != null)) {
    return data
  }
  if (data?.data && typeof data.data === 'object') return data.data
  if (message?.data?.data && typeof message.data.data === 'object') return message.data.data
  return data?.data ?? data
}

function eventBelongsToCurrentMatch(data: any, message?: any) {
  const payload = unwrapMatchEvent(data, message)
  const eventMatchId = String(payload?.matchId ?? data?.matchId ?? message?.data?.matchId ?? '')
  if (!eventMatchId || eventMatchId === 'undefined' || eventMatchId === 'null') return true
  return eventMatchId === String(activeMatchId.value)
}

function logMatchEvent(type: string, data: any, message?: any) {
  const d = unwrapMatchEvent(data, message)
  switch (type) {
    case 'card.played': {
      const actor = d?.actorUserId ?? d?.actor_user_id ?? d?.userId
      const name = d?.cardName ?? d?.card_name ?? '未知卡牌'
      const key = `play:${d?.actionId ?? d?.clientActionId ?? `${actor}:${name}:${d?.version ?? Date.now()}`}`
      addAction(`${playerLabel(actor)} 打出「${name}」`, key)
      break
    }
    case 'player.turn.ended': {
      const actor = d?.userId ?? d?.actorUserId
      addAction(`${playerLabel(actor)} 已结束本回合攻击`, `end:${d?.version ?? ''}:${actor}:${d?.resolvedRound ?? d?.currentRound ?? ''}`)
      break
    }
    case 'round.started': {
      const r = d?.roundNo ?? d?.round ?? d?.currentRound ?? '?'
      addAction(`—— 第 ${r} 回合 ——`, `round:${r}`)
      break
    }
    case 'boss.attack.resolved': {
      const hits = extractBossAttack(d).targets
      if (hits.length) {
        const parts = hits.map((item: any) => {
          const dmg = Number(item.hpDamage ?? 0)
          const absorbed = Number(item.absorbedDamage ?? 0)
          if (dmg > 0) return `${playerLabel(item.userId)} -${dmg}`
          if (absorbed > 0) return `${playerLabel(item.userId)} 格挡`
          return `${playerLabel(item.userId)} 未受伤`
        })
        addAction(`霸凌者发动攻击：${parts.join('，')}`, `boss:${d?.resolvedRound ?? d?.currentRound ?? d?.version ?? Date.now()}`)
      } else {
        addAction('霸凌者发动攻击', `boss:${d?.resolvedRound ?? d?.currentRound ?? d?.version ?? Date.now()}`)
      }
      break
    }
    case 'match.ended': {
      const won = resolveIsVictory(d) || game.isVictory
      addAction(
        won ? '霸凌者倒下，保护成功' : `${fallenDeptLabel.value || '护卫'}倒下，保护失败`,
        `ended:${d?.winnerType ?? ''}:${d?.version ?? ''}`,
      )
      break
    }
  }
}

function makeMatchHandler(eventType: string) {
  return (data: any, message?: any) => {
    if (!eventBelongsToCurrentMatch(data, message)) return
    if (eventType === 'match.ended') {
      const d = unwrapMatchEvent(data, message)
      applyGameOver({
        winnerType: d?.winnerType,
        currentRound: d?.currentRound ?? resultRounds.value,
        bossCurrentHp: d?.bossCurrentHp ?? game.bullyHP,
        bossMaxHp: d?.bossMaxHp ?? game.maxBullyHP,
        players: d?.players ?? players.value,
      })
    }
    logMatchEvent(eventType, data, message)
    void refreshBattleState().catch(() => {})
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
  if (localCurrentHp.value <= 0 && !game.isGameOver) {
    if (bullyFxPlaying.value) {
      pendingReviveAfterFx.value = true
    } else {
      openReviveDialog()
    }
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

watch(canShowRevive, async (ok) => {
  if (!ok) return
  await nextTick()
  try {
    await reviveVideoRef.value?.play()
  } catch {
    // 浏览器拦截自动播放时，玩家手动点播放即可
  }
})

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
    subscribeRoomEvent('player.turn.ended', async (data: any, message?: any) => {
      if (!eventBelongsToCurrentMatch(data, message)) return
      logMatchEvent('player.turn.ended', data, message)
      const attack = extractBossAttack(data)
      if (attack.targets.length) {
        await playBullyAttackFx(attack.targets, attack.round)
      }
      await refreshBattleState().catch(() => {})
    }),
    subscribeRoomEvent('boss.attack.resolved', async (data: any, message?: any) => {
      if (!eventBelongsToCurrentMatch(data, message)) return
      logMatchEvent('boss.attack.resolved', data, message)
      const attack = extractBossAttack(data)
      if (attack.targets.length) {
        await playBullyAttackFx(attack.targets, attack.round)
      }
      await refreshBattleState().catch(() => {})
    }),
    subscribeRoomEvent('round.started', makeMatchHandler('round.started')),
    subscribeRoomEvent('match.reconnecting', handleMatchReconnecting),
    subscribeRoomEvent('match.recovered', handleMatchRecovered),
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
      const d = data?.data ?? data
      if (d?.winnerType != null) pendingWinnerType = Number(d.winnerType)
      logMatchEvent('match.ended', data)
      await refreshBattleState()
    }),
  )
})

onUnmounted(() => {
  stopDisconnectTimers()
  stopFirstPlayerPoll()
  stopActionPhasePoll()
  stopReviveWatchHeartbeat()
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
  z-index: 6;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: flex-start;
  pointer-events: none;
}
.status-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.status-pill {
  display: inline-flex;
  align-items: center;
  max-width: 360px;
  padding: 7px 14px 6px;
  border: 1px solid rgba(255, 224, 160, 0.42);
  border-radius: 999px;
  background: rgba(16, 10, 5, 0.82);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: #fff4dc;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 0.04em;
  text-align: left;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.9);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 232, 180, 0.16);
  white-space: nowrap;
}
.revive-wait-tag {
  color: #ffe08a;
  border-color: rgba(240, 192, 64, 0.7);
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
  max-height: 176px;
  pointer-events: auto;
  scrollbar-width: thin;
}
.action-log-empty {
  font-size: var(--text-sm);
  color: rgba(62, 39, 35, 0.55);
  line-height: 1.5;
  padding: var(--space-1) 0;
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
  border: none;
  border-radius: 10px;
  text-align: center;
  width: min(920px, 92vw, calc(86vh * 1.55));
  aspect-ratio: 1026 / 643;
  height: auto;
  overflow: hidden;
  background: #e6d4a8;
}
.result-panel.is-lose {
  aspect-ratio: 1560 / 1031;
}
.result-bg-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  z-index: 0;
  pointer-events: none;
}
.result-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 10.5% 8% 8.5%;
  font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, sans-serif;
}
.result-content.has-unlock {
  padding-right: 5.5%;
}
.result-content.has-unlock .stats-panel {
  max-width: 40%;
}
.result-header {
  flex-shrink: 0;
  margin: 0;
  text-align: left;
  padding-left: 2%;
}
.result-header h1 {
  font-size: 40px;
  line-height: 1.1;
  margin: 0;
  font-weight: 800;
  letter-spacing: 0.12em;
  color: #1a0e06;
  text-shadow:
    0 1px 0 rgba(255, 248, 230, 0.95),
    0 0 10px rgba(255, 236, 200, 0.45);
}
.result-mid {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  flex: 1;
  min-height: 0;
  padding: 10px 0 8px;
}
.stats-panel {
  flex: 0 1 44%;
  max-width: 46%;
  background: rgba(247, 236, 214, 0.42);
  border: 1px solid rgba(90, 58, 28, 0.28);
  padding: 12px 16px 10px;
  border-radius: var(--radius-lg);
  text-align: left;
}
.stats-panel p {
  margin: 6px 0;
  color: #1a0e06;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.65;
  letter-spacing: 0.02em;
  text-shadow: 0 1px 0 rgba(255, 248, 230, 0.9);
}
.points-reward {
  color: #6b3a0a;
  font-weight: 800;
  font-size: 17px;
}
.unlock-panel {
  flex: 0 0 auto;
  width: 168px;
  max-height: 100%;
  margin-left: auto;
  margin-right: 0;
  padding: 8px 8px 10px;
  background: rgba(247, 236, 214, 0.72);
  border: 1px solid rgba(107, 74, 40, 0.45);
  border-radius: var(--radius-lg);
  text-align: center;
  transform: translateX(6px);
}
.unlock-complete {
  align-self: flex-start;
  width: auto;
  min-width: 140px;
  padding: 12px 14px;
}
.unlock-title {
  margin: 0 0 6px;
  color: #1a0e06;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-shadow: 0 1px 0 rgba(255, 248, 230, 0.9);
}
.unlock-card-wrap {
  width: 148px;
  --card-width: 148px;
  pointer-events: none;
  margin: 0 auto;
}
.unlock-hint {
  margin: 6px 0 0;
  color: #2a1810;
  font-size: 13px;
  font-weight: 700;
}
.dead-tag {
  color: #8b3a3a;
  font-size: 13px;
  font-weight: 800;
  margin-left: var(--space-1);
}
.btn-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
  width: 100%;
  margin-top: auto;
  padding: 4px 0 2px;
}
.btn-hall {
  min-width: 280px;
  height: 52px;
  padding: 0 40px;
  font-size: 22px !important;
  font-weight: 800 !important;
  letter-spacing: 0.16em;
  color: #1a0e06 !important;
  background: #f0c84a !important;
  border: 2px solid #8b5a12 !important;
  border-radius: 10px;
  box-shadow: 0 4px 0 #8b5a12, 0 8px 16px rgba(0, 0, 0, 0.28);
}
.btn-hall:hover,
.btn-hall:focus {
  background: #f6d45c !important;
  color: #1a0e06 !important;
  border-color: #8b5a12 !important;
}
@media (max-height: 700px) {
  .result-header h1 { font-size: 34px; }
  .unlock-card-wrap { width: 128px; --card-width: 128px; }
  .unlock-panel { width: 148px; }
  .btn-hall { min-width: 240px; height: 46px; font-size: 20px !important; }
}

.revive-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  align-items: stretch;
  justify-content: center;
  z-index: 10000;
  animation: fadeIn 0.3s ease;
  padding: 0;
}
.revive-stage {
  position: relative;
  width: min(42vw, 640px);
  height: 100%;
  background: #000;
  color: var(--color-text-primary);
  overflow: hidden;
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.55);
}
.revive-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 2;
  padding: 18px 20px 28px;
  text-align: center;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.82), rgba(0, 0, 0, 0.35), transparent);
  pointer-events: none;
}
.revive-header h2 {
  font-size: 22px;
  margin: 0 0 8px;
  color: var(--color-accent);
}
.revive-header p {
  margin: 4px 0;
  font-size: 14px;
}
.revive-video {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  max-width: none;
  max-height: none;
  object-fit: contain;
  background: #000;
  border-radius: 0;
  margin: 0;
}
.revive-video::-webkit-media-controls-timeline,
.revive-video::-webkit-media-controls-current-time-display,
.revive-video::-webkit-media-controls-time-remaining-display,
.revive-video::-webkit-media-controls-seek-back-button,
.revive-video::-webkit-media-controls-seek-forward-button {
  display: none !important;
  pointer-events: none !important;
}
.revive-video-placeholder {
  background: #000;
}
.revive-error {
  color: #f56c6c;
  font-size: 13px;
  margin-top: 8px;
}
.revive-hint {
  color: var(--color-text-tertiary);
  font-size: 13px;
}
.revive-actions {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 64px;
  z-index: 2;
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 0 16px;
}
.revive-actions .el-button {
  font-size: 15px;
  padding: 10px 22px;
  min-width: 120px;
}

.revive-unavailable {
  color: #3a2414;
  font-size: 14px;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 1px 0 rgba(255, 248, 230, 0.85);
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
.pile-back {
  width: 52px;
  aspect-ratio: 441 / 800;
  border-radius: 6px;
  border: 1px solid rgba(196, 169, 98, 0.55);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.4);
  background-color: #e6d4a8;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
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
.hand-card-back {
  width: calc(var(--card-width) * 0.88);
  aspect-ratio: 441 / 800;
  flex: 0 0 auto;
  border-radius: var(--radius-md);
  border: 1px solid rgba(196, 169, 98, 0.45);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.35);
  pointer-events: none;
  background-color: #e6d4a8;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
}
.hand-wait-hint {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 12px;
  text-align: center;
  color: rgba(255, 248, 230, 0.92);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.7);
  pointer-events: none;
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
.deck-privacy-hint {
  margin: 0 0 12px;
  color: var(--color-text-secondary);
  font-size: 13px;
  text-align: center;
}
.deck-backs {
  margin-top: 16px;
}
.deck-backs-title {
  margin: 0 0 10px;
  color: var(--color-text-secondary);
  font-size: 14px;
  text-align: center;
}
.deck-back-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-height: 240px;
  overflow-y: auto;
}
.deck-card-back {
  width: 56px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid rgba(196, 169, 98, 0.45);
  background: #e6d4a8 url('@/assets/cards/Card_Back.webp') center / cover no-repeat;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.28);
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
  overflow: hidden;
  clip-path: inset(0 0 240px 0);
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
.pos-rect-player2,
.pos-rect-bully {
  overflow: visible;
}
.pos-rect-bully.is-charging img {
  transform-origin: center bottom;
  animation: bully-charge 0.32s ease-in forwards;
  filter: drop-shadow(0 0 18px rgba(255, 64, 32, 0.85));
}
@keyframes bully-charge {
  0% { transform: scale(1) translateY(0) rotate(0deg); }
  55% { transform: scale(1.1) translateY(6px) rotate(-2deg); }
  100% { transform: scale(1.06) translateY(10px) rotate(-3deg); }
}
.pos-rect-player1.is-struck .player-img,
.pos-rect-player2.is-struck .player-img {
  animation: owl-hit 0.4s ease-out;
}
@keyframes owl-hit {
  0% { filter: brightness(2) saturate(0.35); transform: translate(0, 0); }
  30% { filter: brightness(1.5) sepia(0.45) hue-rotate(-25deg); transform: translate(8px, 5px); }
  100% { filter: none; transform: translate(0, 0); }
}
.player-hp-hud.is-flash {
  animation: hp-flash 0.45s ease;
}
.player-hp-hud.is-flash :deep(.stamina-bar) {
  filter: brightness(1.8) saturate(1.4);
}
@keyframes hp-flash {
  0%, 100% { filter: none; }
  40% { filter: drop-shadow(0 0 14px #ff3b2f); }
}
.bully-fx-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 40;
  overflow: hidden;
}
.bully-slash {
  position: absolute;
  height: 12px;
  transform-origin: 0 50%;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent 0%, #fff4c8 12%, #ff4a2a 48%, #ffb24a 82%, transparent 100%);
  box-shadow: 0 0 18px 5px rgba(255, 72, 32, 0.55);
  animation: slash-strike 0.24s ease-out forwards;
}
.bully-slash.blocked {
  background: linear-gradient(90deg, transparent 0%, #d7f2ff 12%, #6cb8ff 50%, #b8e0ff 82%, transparent 100%);
  box-shadow: 0 0 16px 4px rgba(90, 170, 255, 0.5);
}
@keyframes slash-strike {
  0% { opacity: 0; clip-path: inset(0 100% 0 0); }
  30% { opacity: 1; clip-path: inset(0 35% 0 0); }
  100% { opacity: 0; clip-path: inset(0 0 0 0); }
}
.bully-float {
  position: absolute;
  transform: translate(-50%, -50%);
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #ff5b4a;
  text-shadow: 0 2px 0 #4a1208, 0 0 12px rgba(255, 72, 40, 0.75);
  animation: damage-float 0.9s ease-out forwards;
}
.bully-float.blocked {
  font-size: 22px;
  color: #9ad4ff;
  text-shadow: 0 2px 0 #16324a, 0 0 10px rgba(90, 170, 255, 0.7);
}
@keyframes damage-float {
  0% { opacity: 0; transform: translate(-50%, -30%) scale(0.7); }
  18% { opacity: 1; transform: translate(-50%, -70%) scale(1.12); }
  100% { opacity: 0; transform: translate(-50%, -150%) scale(1); }
}
.battle-page.is-impact {
  animation: bully-impact 0.26s linear;
}
@keyframes bully-impact {
  0%, 100% { transform: translate(0, 0); }
  25% { transform: translate(-3px, 2px); }
  50% { transform: translate(3px, -2px); }
  75% { transform: translate(-2px, 1px); }
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
