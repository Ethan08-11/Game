<template>
  <div class="page">
    <BackButton to="/game-hall" text="返回大厅" />
    <h2>卡牌图鉴</h2>

    <div v-if="loading" class="state-msg">加载中...</div>
    <div v-else-if="error" class="state-msg state-error">{{ error }}</div>
    <div v-else-if="cardGroups.length === 0" class="state-msg">暂无卡牌数据</div>

    <template v-else>
      <div v-for="group in cardGroups" :key="group.dept" class="dept-section">
        <div class="dept-header">
          <span class="dept-dot" :style="{ background: getDeptColor(group.dept) }"></span>
          <span class="dept-name">{{ group.dept }}</span>
          <span class="dept-count">已解锁 {{ group.unlockedCount }} / {{ group.cards.length }}</span>
        </div>
        <div class="card-grid">
          <div
            v-for="card in group.cards"
            :key="card.id"
            class="card-item"
            :class="{ locked: !card.unlocked }"
          >
            <div class="card-image-box">
              <div v-if="cardThumb(card)" class="card-face">
                <img
                  :src="cardThumb(card)!"
                  :alt="card.unlocked ? card.cardName : '未解锁卡牌'"
                  class="card-img"
                  :class="{ 'card-img-locked': !card.unlocked }"
                  loading="lazy"
                  decoding="async"
                  @error="onCardImgError"
                />
                <template v-if="card.unlocked">
                  <span class="card-face-dept">{{ cardDeptLabel(card) }}</span>
                  <span class="card-face-name" :style="{ top: cardNameTop(card) + '%' }">{{ card.cardName }}</span>
                </template>
              </div>
              <span v-else class="card-placeholder">?</span>
            </div>
            <div class="card-name">{{ card.unlocked ? card.cardName : '???' }}</div>
            <div v-if="card.unlocked" class="card-meta">
              <span class="card-cost">{{ card.cost }}费</span>
              <span class="card-type-tag" :class="'type-' + card.cardType">{{ typeLabel(card.cardType) }}</span>
              <span class="card-dept-tag">{{ cardDeptLabel(card) }}</span>
            </div>
            <div v-else class="card-locked-hint">未解锁</div>
            <div class="card-desc">{{ card.unlocked ? card.description : '胜利后随机解锁' }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchCardList } from '@/api'
import type { ApiCard } from '@/api'
import BackButton from '@/components/BackButton.vue'
import { getImageUrl } from '@/utils/imageUrl'
import { displayCardDept } from '@/utils/cardDept'
import { cardNameTopPercent } from '@/utils/cardOverlay'
import lockedCardImg from '@/assets/cards/Card_Locked.webp'
import bg1 from '@/assets/hall-bg.webp'
import bg2 from '@/assets/hall-bg2.webp'

interface DeptGroup {
  dept: string
  cards: ApiCard[]
  unlockedCount: number
}

const cards = ref<ApiCard[]>([])
const loading = ref(true)
const error = ref('')
const bgImage = ref('')

const deptMap: Record<string, string> = {
  sales: '销售部',
  purchase: '采购部',
  public: '公共部',
  neutral: '中立',
  logistics: '物流部',
  marketing: '营销部',
  design: '设计部',
  tech: '公共部',
  it: '公共部',
  finance: '财务部',
  hr: '人事部',
  // 下次打开「你的0来了」时恢复下一行
  // zero: '你的0来了',
}

const deptOrder = ['销售部', '采购部', '物流部', '营销部', '设计部', '技术部', '财务部', '人事部', /* '你的0来了', */ '公共部']

const cardGroups = computed<DeptGroup[]>(() => {
  const map = new Map<string, ApiCard[]>()
  for (const card of cards.value) {
    const dept = deptMap[card.deptType] || card.deptType
    const list = map.get(dept) || []
    list.push(card)
    map.set(dept, list)
  }
  const ordered = [...deptOrder.filter(d => map.has(d)), ...[...map.keys()].filter(d => !deptOrder.includes(d)).sort()]
  return ordered.map(dept => {
      const list = [...map.get(dept)!].sort(compareGalleryCards)
      return {
        dept,
        cards: list,
        unlockedCount: list.filter(card => card.unlocked).length,
      }
    })
})

function isCollectibleCard(card: ApiCard): boolean {
  return Number(card.requireUnlock ?? 0) === 1
}

/** 基础卡在前、收藏卡在后；不用解锁状态参与排序，翻面不换位。 */
function compareGalleryCards(a: ApiCard, b: ApiCard): number {
  const byKind = Number(isCollectibleCard(a)) - Number(isCollectibleCard(b))
  if (byKind !== 0) return byKind
  const byCost = (a.cost ?? 0) - (b.cost ?? 0)
  if (byCost !== 0) return byCost
  const byCode = (a.cardCode || '').localeCompare(b.cardCode || '', 'zh')
  if (byCode !== 0) return byCode
  return (a.id || 0) - (b.id || 0)
}

onMounted(async () => {
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bg2 : bg1
  try {
    cards.value = await fetchCardList()
  } catch (e: any) {
    error.value = e.message || '加载卡牌数据失败'
  } finally {
    loading.value = false
  }
})

function cardThumb(card: ApiCard): string | null {
  if (!card.unlocked) return lockedCardImg
  return getImageUrl(card.imageUrl)
}

function deptLabel(deptType: string | null | undefined): string {
  if (!deptType) return ''
  return deptMap[deptType] || deptType
}

function cardDeptLabel(card: ApiCard): string {
  return displayCardDept(card.deptType, card.imageUrl, deptLabel(card.deptType))
}

function cardNameTop(card: ApiCard): number {
  return cardNameTopPercent(card.imageUrl, card.cardName)
}

function onCardImgError(event: Event) {
  const img = event.target as HTMLImageElement
  if (!img || img.dataset.fallback === '1') return
  img.dataset.fallback = '1'
  img.src = lockedCardImg
}

function getDeptColor(dept: string): string {
  const colors: Record<string, string> = {
    '销售部': '#7da38a',
    '采购部': '#c8963e',
    '公共部': '#6a8cbf',
    '中立': '#9b8ec4',
    '物流部': '#3498db',
    '营销部': '#e74c3c',
    '设计部': '#9b59b6',
    '技术部': '#1abc9c',
    '财务部': '#f39c12',
    '人事部': '#e67e22',
    // 下次打开「你的0来了」时恢复下一行
    // '你的0来了': '#6b3a4a',
  }
  return colors[dept] || '#c4a962'
}

function typeLabel(type: string | null | undefined): string {
  if (!type) return '未知'
  const labels: Record<string, string> = {
    attack: '攻击',
    defend: '防御',
    draw: '抽牌',
    consume: '消耗',
    attack_defend: '攻防',
    special: '特殊',
    trigger: '触发',
    heal: '治疗',
    buff: '增益',
    support: '辅助',
  }
  return labels[type] || type
}
</script>

<style scoped>
.page {
  position: relative;
  padding: var(--space-10);
  color: var(--color-text-primary);
  max-width: var(--content-max);
  margin: 0 auto;
  height: 100%;
  overflow-y: auto;
  isolation: isolate;
  --cards-bg: url('@/assets/hall-bg2.webp');
}
.page::before {
  content: '';
  position: fixed;
  inset: 0;
  background: center/cover no-repeat;
  background-image: var(--cards-bg);
  z-index: -2;
}
.page::after {
  content: '';
  position: fixed;
  inset: 0;
  background: linear-gradient(rgba(13, 20, 28, 0.42), rgba(13, 20, 28, 0.58));
  z-index: -1;
}
h2 {
  text-align: center;
  margin-bottom: var(--space-8);
  font-size: var(--text-3xl);
  font-weight: var(--weight-semibold);
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.35);
}

.state-msg {
  text-align: center;
  padding: var(--space-16) 0;
  font-size: var(--text-lg);
  color: var(--color-text-secondary);
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.35);
}
.state-error { color: #ff9b94; }

.dept-section {
  margin-bottom: var(--space-8);
  background: rgba(13, 20, 28, 0.28);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.18);
}
.dept-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.dept-dot {
  width: 10px; height: 10px;
  border-radius: var(--radius-full);
}
.dept-name {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
}
.dept-count {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin-left: auto;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--space-3);
}

.card-item {
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-lg);
  padding: var(--space-3);
  transition: all var(--transition-fast);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}
.card-item:hover {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.24);
  transform: translateY(-2px);
}
.card-item.locked {
  background: rgba(8, 10, 14, 0.45);
  border-color: rgba(196, 169, 98, 0.22);
}
.card-item.locked:hover {
  transform: none;
  background: rgba(8, 10, 14, 0.52);
}

.card-image-box {
  width: 100%;
  aspect-ratio: 3 / 4;
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}
.card-face {
  position: relative;
  height: 100%;
  aspect-ratio: 640 / 1023;
  max-width: 100%;
}
.card-img {
  width: 100%;
  height: 100%;
  object-fit: fill;
  display: block;
}
.card-face-dept {
  position: absolute;
  top: 3.2%;
  left: 50%;
  transform: translateX(-50%);
  max-width: 48%;
  color: #3E2723;
  font-size: 11px;
  font-weight: var(--weight-semibold);
  line-height: 1.1;
  text-align: center;
  white-space: nowrap;
  pointer-events: none;
  z-index: 2;
  text-shadow: 0 0 4px rgba(255, 248, 230, 0.9);
}
.card-face-name {
  position: absolute;
  top: var(--card-name-top, 72.2%);
  left: 50%;
  transform: translate(-50%, -50%);
  width: 62%;
  color: #3E2723;
  font-size: 12px;
  font-weight: var(--weight-bold);
  line-height: 1;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  pointer-events: none;
  z-index: 2;
}
.card-img-locked {
  object-fit: contain;
  background: #0a0c10;
}
.card-placeholder {
  font-size: var(--text-4xl);
  color: rgba(255, 255, 255, 0.55);
  font-weight: var(--weight-light);
}

.card-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  text-align: center;
}
.card-locked-hint {
  text-align: center;
  font-size: var(--text-xs);
  color: #c4a962;
  letter-spacing: 0.12em;
}

.card-meta {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-2);
}
.card-cost {
  font-size: var(--text-xs);
  color: var(--color-accent);
  font-weight: var(--weight-bold);
}
.card-type-tag {
  display: inline-block;
  padding: 0 var(--space-1);
  border-radius: var(--radius-sm);
  font-size: var(--text-2xs);
  font-weight: var(--weight-semibold);
}
.type-attack { background: rgba(231, 76, 60, 0.2); color: #ffb4ae; }
.type-defend { background: rgba(52, 152, 219, 0.2); color: #9dd4ff; }
.type-draw { background: rgba(46, 204, 113, 0.2); color: #abf0c5; }
.type-consume { background: rgba(155, 89, 182, 0.2); color: #d8b5e8; }
.type-attack_defend { background: rgba(201, 107, 43, 0.2); color: #f0c8a0; }
.type-special { background: rgba(155, 89, 182, 0.2); color: #d8b5e8; }
.type-trigger { background: rgba(230, 126, 34, 0.2); color: #f5c8a0; }
.type-heal { background: rgba(39, 174, 96, 0.2); color: #9ff0c0; }
.type-buff { background: rgba(41, 128, 185, 0.2); color: #9dd4ff; }
.type-support { background: rgba(22, 160, 133, 0.2); color: #7ee0cc; }
.card-dept-tag {
  font-size: var(--text-2xs);
  color: rgba(255, 255, 255, 0.78);
}

.card-desc {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  text-align: center;
}
</style>
