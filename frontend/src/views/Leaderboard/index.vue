<template>
  <div class="page" :style="{ '--page-bg': `url(${pageBg})`, '--hall-bg': `url(${hallBg})` }">
    <BackButton to="/game-hall" text="返回大厅" />
    <div class="title-bar" :style="{ '--title-bg': `url(${titleBg})` }">排行榜</div>
    <div class="tabs">
      <button :class="{ active: tab === 'total' }" @click="tab = 'total'">总榜</button>
      <button :class="{ active: tab === 'weekly' }" @click="tab = 'weekly'">周榜</button>
    </div>
    <p v-if="tab === 'weekly'" class="week-hint">本周金币 · 下周一 0:00 重置后重新累计</p>
    <div ref="listRef" class="list" :key="tab">
      <div v-for="(item, idx) in list" :key="item.userId" class="row" :style="{ backgroundImage: `url(${rowBg})`, animationDelay: `${Math.min(idx * 0.03, 0.4)}s` }">
        <span class="rank" :class="{ top: item.rank <= 3 }">{{ item.rank }}</span>
        <PlayerAvatar class="row-avatar" :src="item.avatarUrl" :alt="item.displayName || item.username" />
        <span class="name">{{ item.displayName || item.username }}</span>
        <div class="stats">
          <span class="pts">{{ item.money }} 金币</span>
          <span class="rate">胜率 {{ item.winRate }}%</span>
        </div>
      </div>
      <div v-if="list.length === 0" class="empty">暂无排行数据</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import { getLeaderboard } from '@/api'
import type { LeaderboardEntry } from '@/api'
import BackButton from '@/components/BackButton.vue'
import PlayerAvatar from '@/components/PlayerAvatar.vue'
import titleBg from '@/assets/title-bg-leaderboard.webp'
import rowBg from '@/assets/row-bg-leaderboard.webp'
import pageBg from '@/assets/beijing0.webp'
import hallDay from '@/assets/hall-bg2.webp'
import hallNight from '@/assets/hall-bg.webp'

const tab = ref<'total' | 'weekly'>('total')
const list = ref<LeaderboardEntry[]>([])
const listRef = ref<HTMLElement | null>(null)
const hour = new Date().getHours()
const hallBg = hour >= 6 && hour < 18 ? hallDay : hallNight

async function loadLeaderboard() {
  try {
    list.value = await getLeaderboard(tab.value, 1, 10000)
  } catch {
    list.value = []
  }
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = 0
}

onMounted(loadLeaderboard)
watch(tab, loadLeaderboard)
</script>

<style scoped>
.page {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: calc(var(--space-10) + 36px) var(--space-10) 0;
  color: var(--color-text-primary); text-align: center;
  isolation: isolate;
}
.page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -2;
  background: var(--hall-bg) center/cover no-repeat;
  filter: blur(6px);
  pointer-events: none;
}
.page::after {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  background: var(--page-bg) center/cover no-repeat;
  pointer-events: none;
}
.title-bar {
  position: relative;
  display: inline-block;
  margin: 0 auto var(--space-5);
  font-size: 32px;
  font-weight: var(--weight-semibold);
  padding: var(--space-3) var(--space-8);
  color: #4a3520;
  text-indent: -32px;
  isolation: isolate;
}
.title-bar::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background: var(--title-bg) center/contain no-repeat;
  transform: scale(3.5) translateY(-9px);
  transform-origin: center center;
  pointer-events: none;
}
.tabs { display: flex; gap: var(--space-2); justify-content: center; margin-bottom: var(--space-3); }
.week-hint {
  margin: 0 0 var(--space-4);
  color: #6a5338;
  font-size: var(--text-sm);
}
.tabs button {
  padding: var(--space-1) var(--space-5);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: var(--text-md);
  transition: all var(--transition-fast);
}
.tabs button.active {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: var(--color-bg-base);
}
.list { max-width: 760px; margin: 0 auto; flex: 1; overflow-y: auto; min-height: 0; padding-bottom: 120px; width: 100%; }
.row {
  display: grid;
  grid-template-columns: 48px 56px minmax(0, 1fr) 118px;
  align-items: center;
  column-gap: 12px;
  padding: 8px 96px 8px 20px;
  margin-bottom: var(--space-2);
  min-height: 64px;
  background-size: 100% 100%;
  background-position: center;
  background-repeat: no-repeat;
  transition: filter var(--transition-fast);
  animation: bounceIn 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
.row:hover { filter: brightness(1.1); }
.rank {
  font-size: 18px;
  color: #4a3520;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.rank.top {
  font-size: 24px;
  font-weight: var(--weight-bold);
  color: #4a3520;
}
.name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
  font-size: 18px;
  color: #4a3520;
}
.row-avatar {
  width: 52px;
  height: 52px;
  justify-self: center;
}
.stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  min-width: 0;
}
.pts {
  color: #4a3520;
  font-weight: var(--weight-medium);
  font-size: 16px;
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.rate {
  color: #6a5338;
  font-size: 13px;
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.empty { color: var(--color-text-tertiary); padding: var(--space-4); }

@keyframes bounceIn {
  0% { opacity: 0; transform: translateY(24px) scale(0.92); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}
</style>
