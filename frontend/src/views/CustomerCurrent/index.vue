<template>
  <main class="customer-page" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
    <BackButton to="/game-hall" text="返回大厅" />

    <section class="customer-card" :style="{ '--panel-bg': `url(${panelBg})` }">
      <img :src="getImageUrl(game.employerTrait?.imageUrl) || characterImg" alt="顾客形象" class="customer-avatar" />
      <div class="card-title" :style="{ backgroundImage: `url(${titleBanner})` }">顾客来访</div>
      <h1>{{ game.employerName || '顾客加载中' }}</h1>
      <p class="story">顾客长期遭受霸凌者欺凌，已向 HIH 发起求助。请先了解本局顾客属性，再进入部门选择。</p>

      <div class="trait-panel">
        <span class="trait-label">顾客属性</span>
        <strong>{{ trait?.name || '加载中' }}</strong>
        <p>{{ trait?.description || '顾客状态加载中...' }}</p>
      </div>

      <div class="info-grid">
        <div class="info-item">
          <span>顾客类型触发概率</span>
          <strong>{{ formatRate(trait?.typeTriggerRate) }}</strong>
        </div>
        <div class="info-item">
          <span>效果触发概率</span>
          <strong>{{ formatRate(trait?.effectTriggerRate) }}</strong>
        </div>
        <div class="info-item">
          <span>顾客效果</span>
          <strong>{{ effectText }}</strong>
        </div>
      </div>

      <button type="button" class="primary-btn" :disabled="jumping" :style="{ backgroundImage: `url(${titleBanner})` }" @click.stop.prevent="goMatchMaking">
        {{ jumping ? '跳转中...' : '保护我们的顾客' }}
      </button>
      <p class="slogan">顾客需要您们的帮助！！！</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '@/store/game'
import BackButton from '@/components/BackButton.vue'
import bg1 from '@/assets/hall-bg.webp'
import bg2 from '@/assets/hall-bg2.webp'
import panelBg from '@/assets/customer-panel-bg.webp'
import characterImg from '@/assets/customer-character.webp'
import titleBanner from '@/assets/title-banner.webp'
import { getImageUrl } from '@/utils/imageUrl'

const bgDay = bg2
const bgNight = bg1
const bgImage = ref('')

const router = useRouter()
const game = useGameStore()
const jumping = ref(false)
const trait = computed(() => game.employerTrait)
const effectText = computed(() => {
  if (!trait.value) return '加载中'
  const target = trait.value.effectType === 'player_hp'
    ? '我方血值'
    : trait.value.effectType === 'hp'
      ? '霸凌者血量'
      : '霸凌者基础攻击'
  const value = trait.value.effectValue ?? 0
  return `${target}${value >= 0 ? '+' : ''}${value}`
})

function formatRate(value?: number) {
  if (value == null) return '后端未配置'
  return `${Math.round(value * 100)}%`
}

onMounted(() => {
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
  game.loadCurrentCustomer()
})

async function goMatchMaking() {
  if (jumping.value) return
  jumping.value = true
  try {
    await router.push({ path: '/matchmaking', query: { openDept: '1' } })
  } finally {
    jumping.value = false
  }
}
</script>

<style scoped>
.customer-page {
  min-height: 100%;
  padding: var(--space-10);
  color: var(--color-text-primary);
  position: relative;
  isolation: isolate;
}
.customer-page::before {
  content: '';
  position: absolute;
  inset: -20px;
  background: var(--hall-bg, var(--color-bg-base)) center/cover no-repeat;
  filter: blur(6px);
  z-index: 0;
  pointer-events: none;
}
.customer-page > * {
  position: relative;
  z-index: 1;
}
.customer-card {
  position: relative;
  width: 75%;
  min-height: 520px;
  margin: var(--space-6) auto 0;
  padding: var(--space-10) var(--space-12);
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  isolation: isolate;
}
.customer-card::before {
  content: '';
  position: absolute;
  width: 105%;
  height: 105%;
  left: -2.5%;
  top: -2.5%;
  background: var(--panel-bg, var(--color-surface-02)) center/100% 100% no-repeat;
  z-index: -1;
  pointer-events: none;
}
.customer-avatar {
  position: absolute;
  right: -180px;
  top: -62px;
  width: 480px;
  height: auto;
  z-index: 1;
}
.card-title {
  position: absolute;
  left: 50%;
  top: -24px;
  transform: translateX(-50%);
  width: 260px;
  height: 60px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: var(--weight-bold);
  color: #4a3520;
  z-index: 2;
}
.eyebrow {
  color: #8b6914;
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  letter-spacing: 0.2em;
}
h1 {
  margin: var(--space-2) 0 var(--space-4);
  font-size: var(--text-4xl);
  color: #3a1f0d;
}
.story {
  color: #5c3d2e;
  font-size: var(--text-md);
  line-height: var(--leading-relaxed);
  max-width: 55%;
}
.trait-panel {
  margin-top: var(--space-4);
  padding: var(--space-3) 0;
  border-radius: var(--radius-lg);
  background: transparent;
  max-width: 50%;
}
.trait-label {
  display: block;
  margin-bottom: var(--space-2);
  color: #5c3d2e;
  font-size: var(--text-xs);
}
.trait-panel strong {
  color: #4a2c1a;
  font-size: var(--text-2xl);
}
.trait-panel p {
  margin-top: var(--space-2);
  color: #5c3d2e;
}
.info-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: var(--space-5) 0;
  max-width: 50%;
}
.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: transparent;
}
.info-item span {
  color: #5c3d2e;
  font-size: var(--text-sm);
}
.info-item strong {
  color: #4a2c1a;
  font-size: var(--text-md);
}
.primary-btn {
  display: block;
  width: 50%;
  margin: 0 auto;
  padding: var(--space-4);
  border: none;
  border-radius: 0;
  background-color: transparent;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  color: #4a3520;
  cursor: pointer;
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  transition: all var(--transition-fast);
}
.primary-btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.1);
}
.slogan {
  text-align: center;
  font-size: 28px;
  font-weight: var(--weight-bold);
  color: #4a2c1a;
  margin-top: var(--space-3);
}

@media (max-width: 767px) {
  .customer-page { padding: var(--space-4); }
  .customer-card { padding: var(--space-5); }
}
</style>
