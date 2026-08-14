<template>
  <div class="page" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
    <BackButton to="/game-hall" text="返回大厅" />
    <div class="title-wrapper">
      <img :src="cardBg" class="title-bg" :style="{ transform: `translate(${bgX}px, ${bgY}px)` }" />
      <h2 class="title-text" :style="{ transform: `translate(${textX}px, ${textY}px)` }">您的成就</h2>
    </div>
	    <div class="grid" :style="{ '--card-bg': `url(${achieveBg})` }">
      <div v-for="a in user.achievements" :key="a.id" class="card" :class="{ locked: !a.unlockedAt }">
        <div class="card-icon">
          <el-icon :size="28"><component :is="getIcon(a.icon || 'trophy')" /></el-icon>
        </div>
        <div class="name">{{ a.name }}</div>
        <div class="desc">{{ a.description }}</div>
        <div class="time">{{ a.unlockedAt ? '已解锁' : '未解锁' }}</div>
      </div>
      <div v-if="user.achievements.length === 0" class="empty">暂无成就数据</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useUserStore } from '@/store/user'
import bg1 from '@/assets/hall-bg.png'
import bg2 from '@/assets/hall-bg2.png'
import cardBg from '@/assets/achievement-card-bg.png'
import achieveBg from '@/assets/achievement-bg.png'

const bgDay = bg2
const bgNight = bg1
const bgImage = ref('')
const bgX = ref(0)
const bgY = ref(-46)
const textX = ref(0)
const textY = ref(0)
import BackButton from '@/components/BackButton.vue'
import { getIcon } from '@/utils/iconMap'

const user = useUserStore()
onMounted(() => {
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
  user.loadAchievements()
})
</script>

<style scoped>
.page {
  position: relative;
  padding: var(--space-10);
  color: var(--color-text-primary);
  text-align: center;
  min-height: 100vh;
  isolation: isolate;
}
.page::before {
  content: '';
  position: absolute;
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
h2 { margin-bottom: var(--space-6); font-size: var(--text-3xl); font-weight: var(--weight-semibold); }
.grid { display: flex; flex-wrap: wrap; gap: var(--space-4); justify-content: center; margin-bottom: var(--space-8); }
.card {
  width: 180px; padding: var(--space-2) var(--space-5) var(--space-5);
  background: var(--card-bg, rgba(0, 0, 0, 0.35)) center/100% 100% no-repeat;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all var(--transition-base);
  position: relative;
}
.card > * { position: relative; z-index: 1; }
.card:hover { transform: translateY(-2px); }
.card.locked { opacity: 0.5; }
.card-icon {
  width: 48px; height: 48px;
  margin: 0 auto var(--space-2);
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.08);
}
.card.locked .card-icon { color: rgba(62, 42, 20, 0.3); }
.card:not(.locked) .card-icon { color: #3e2a14; }
.name { font-weight: var(--weight-semibold); margin-bottom: var(--space-1); color: #3e2a14; }
.desc { font-size: var(--text-sm); color: rgba(62, 42, 20, 0.65); margin-bottom: var(--space-1); }
.time { font-size: var(--text-2xs); color: #3e2a14; }
.empty { color: rgba(255, 255, 255, 0.5); }
.title-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 180px;
  margin-bottom: var(--space-6);
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
