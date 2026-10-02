<template>
  <div class="page" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
    <BackButton to="/game-hall" text="返回大厅" />
    <div class="title-wrapper">
      <img :src="cardBg" class="title-bg" :style="{ transform: `translate(${bgX}px, ${bgY}px)` }" />
      <h2 class="title-text" :style="{ transform: `translate(${textX}px, ${textY}px)` }">您的成就</h2>
    </div>
    <div class="tabs">
      <button
        v-for="item in tabs"
        :key="item.id"
        :class="{ active: tab === item.id }"
        type="button"
        @click="tab = item.id"
      >{{ item.label }}</button>
    </div>
    <p class="summary">{{ tabLabel }} · 已解锁 {{ unlockedCount }}/{{ visibleList.length }}</p>
    <div class="grid" :style="{ '--card-bg': `url(${achieveBg})` }">
      <div
        v-for="a in visibleList"
        :key="a.id"
        class="card"
        :class="{ locked: !a.unlockedAt, hidden: isHiddenLocked(a) }"
      >
        <div class="card-icon">
          <el-icon :size="28"><component :is="getIcon(cardIcon(a))" /></el-icon>
        </div>
        <div class="name">{{ cardName(a) }}</div>
        <div class="desc">{{ cardDesc(a) }}</div>
        <div class="progress-text">{{ progressText(a) }}</div>
        <div class="time">{{ a.unlockedAt ? '已解锁' : '未解锁' }}</div>
      </div>
      <div v-if="visibleList.length === 0" class="empty">该难度暂无成就</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUserStore, type Achievement } from '@/store/user'
import bg1 from '@/assets/hall-bg.webp'
import bg2 from '@/assets/hall-bg2.webp'
import cardBg from '@/assets/achievement-card-bg.webp'
import achieveBg from '@/assets/achievement-bg.webp'
import BackButton from '@/components/BackButton.vue'
import { getIcon } from '@/utils/iconMap'

const tabs = [
  { id: 2, label: '进阶' },
  { id: 3, label: '高难' },
  { id: 4, label: '极难' },
] as const

const bgDay = bg2
const bgNight = bg1
const bgImage = ref('')
const bgX = ref(0)
const bgY = ref(-46)
const textX = ref(0)
const textY = ref(0)
const tab = ref<2 | 3 | 4>(2)
const user = useUserStore()

const tabLabel = computed(() => tabs.find(item => item.id === tab.value)?.label || '进阶')
const visibleList = computed(() =>
  user.achievements
    .filter(item => (item.difficulty || 1) === tab.value)
    .slice()
    .sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0) || a.id.localeCompare(b.id)),
)
const unlockedCount = computed(() => visibleList.value.filter(item => item.unlockedAt).length)

function isHiddenLocked(item: Achievement) {
  return Boolean(item.hidden) && !item.unlockedAt
}

function isMysteryLocked(item: Achievement) {
  return isHiddenLocked(item) && item.id !== 'ACH-005' && item.conditionType !== 'all_unlocked'
}

function cardName(item: Achievement) {
  return isMysteryLocked(item) ? '???' : item.name
}

function cardDesc(item: Achievement) {
  if (isHiddenLocked(item) && (item.id === 'ACH-005' || item.conditionType === 'all_unlocked')) {
    return '完成一次特殊条件即可解锁'
  }
  return isMysteryLocked(item) ? '???' : item.description
}

function cardIcon(item: Achievement) {
  return isHiddenLocked(item) ? 'question' : (item.icon || 'trophy')
}

function progressText(item: Achievement) {
  if (isHiddenLocked(item)) return '???'
  const target = Math.max(item.targetCount || 1, 1)
  const current = Math.min(item.progressValue || 0, target)
  return `${current}/${target}`
}

onMounted(() => {
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
  user.loadAchievements()
})
</script>

<style scoped>
.page {
  position: relative;
  height: 100%;
  padding: var(--space-10);
  color: var(--color-text-primary);
  text-align: center;
  isolation: isolate;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}
.page::before {
  content: '';
  position: fixed;
  inset: -20px;
  background: var(--hall-bg, var(--color-bg-base)) center/cover no-repeat;
  filter: blur(6px);
  z-index: 0;
  pointer-events: none;
}
.page > * {
  position: relative;
  z-index: 1;
}
.tabs {
  display: flex;
  gap: var(--space-2);
  justify-content: center;
  margin: 0 auto var(--space-3);
}
.tabs button {
  padding: var(--space-1) var(--space-5);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: var(--text-md);
}
.tabs button.active {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: var(--color-bg-base);
}
.summary {
  margin: 0 0 var(--space-4);
  color: #6a5338;
  font-size: var(--text-sm);
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 180px);
  justify-content: center;
  align-content: start;
  align-items: stretch;
  gap: var(--space-4);
  width: 100%;
  max-width: calc(180px * 5 + var(--space-4) * 4);
  margin: 0 auto;
  padding-bottom: 64px;
}
.card {
  width: 180px;
  min-height: 176px;
  padding: var(--space-2) var(--space-5) var(--space-5);
  background: var(--card-bg, rgba(0, 0, 0, 0.35)) center/100% 100% no-repeat;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: opacity var(--transition-base), transform var(--transition-base);
  position: relative;
  display: flex;
  flex-direction: column;
}
.card > * { position: relative; z-index: 1; }
.card:hover { transform: translateY(-2px); }
.card.locked { opacity: 0.5; }
.card.locked:hover { transform: none; }
.card.hidden .name,
.card.hidden .desc { letter-spacing: 0.08em; }
.card-icon {
  width: 48px; height: 48px;
  margin: 0 auto var(--space-2);
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.08);
}
.card.locked .card-icon { color: rgba(62, 42, 20, 0.3); }
.card:not(.locked) .card-icon { color: #3e2a14; }
.name {
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-1);
  color: #3e2a14;
  min-height: 1.4em;
  line-height: 1.4;
}
.desc {
  font-size: var(--text-sm);
  color: rgba(62, 42, 20, 0.65);
  margin-bottom: var(--space-1);
  min-height: 3.2em;
  line-height: 1.4;
  flex: 1;
}
.progress-text { font-size: var(--text-xs); color: #6a5338; margin-bottom: 2px; font-variant-numeric: tabular-nums; }
.time { font-size: var(--text-2xs); color: #3e2a14; margin-top: auto; }
.empty { color: rgba(255, 255, 255, 0.5); }
.title-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 180px;
  margin-bottom: var(--space-4);
}
.title-bg {
  position: absolute;
  width: 500px;
  height: auto;
  z-index: 0;
  pointer-events: none;
}
.title-text {
  position: relative;
  z-index: 1;
  font-size: var(--text-3xl);
  font-weight: var(--weight-semibold);
  color: #4a3520;
  margin: 0;
  white-space: nowrap;
}
</style>

<style>
.back-btn {
  position: absolute !important;
  z-index: 100 !important;
}
</style>
