<template>
  <main class="customer-page" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
    <BackButton to="/game-hall" text="返回大厅" />

    <section class="customer-card" :style="{ '--panel-bg': `url(${panelBg})` }">
      <div class="card-title" :style="{ backgroundImage: `url(${titleBanner})` }">顾客来访</div>
      <div class="card-layout">
        <div class="card-body">
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
            <div v-if="trait?.bullyName" class="info-item">
              <span>对应霸凌者</span>
              <strong>{{ trait.bullyName }}</strong>
            </div>
            <div v-if="trait?.bullySkillSummary" class="info-item info-item-skill">
              <span>霸凌者特效</span>
              <strong>{{ trait.bullySkillSummary }}</strong>
            </div>
            <div v-if="trait?.bullyName" class="info-item">
              <span>霸凌者特效概率</span>
              <strong>{{ bullyChanceText }}</strong>
            </div>
          </div>
        </div>
        <img :src="getImageUrl(game.employerTrait?.imageUrl) || characterImg" alt="顾客形象" class="customer-avatar" />
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
const bullyChanceText = computed(() => {
  const chance = trait.value?.bullySkillChance
  if (chance == null) return '常驻'
  return formatRate(chance)
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
  height: 100%;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: var(--space-6) var(--space-8) var(--space-4);
  box-sizing: border-box;
  color: var(--color-text-primary);
  position: relative;
  isolation: isolate;
}
.customer-page::before {
  content: '';
  position: fixed;
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
  width: min(82%, 1000px);
  min-height: 0;
  margin: 36px auto 12px;
  padding: 44px 48px 36px 96px;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  isolation: isolate;
  overflow: visible;
}
.customer-card::before {
  content: '';
  position: absolute;
  inset: -18px -3% -48px;
  background: var(--panel-bg, var(--color-surface-02)) center/100% 100% no-repeat;
  z-index: -1;
  pointer-events: none;
}
.card-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(260px, 0.95fr);
  gap: 8px 28px;
  align-items: center;
}
.customer-avatar {
  position: relative;
  z-index: 0;
  width: 100%;
  max-width: 440px;
  max-height: 460px;
  height: auto;
  object-fit: contain;
  justify-self: center;
  transform: translateX(-18px);
  pointer-events: none;
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
.card-body {
  position: relative;
  z-index: 1;
  min-width: 0;
}
.eyebrow {
  color: #8b6914;
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  letter-spacing: 0.2em;
}
h1 {
  margin: 4px 0 8px;
  font-size: var(--text-3xl);
  line-height: var(--leading-tight);
  color: #3a1f0d;
}
.story {
  margin: 0 0 16px;
  color: #5c3d2e;
  font-size: var(--text-sm);
  line-height: 1.55;
}
.trait-panel {
  margin: 0 0 8px;
  padding: 0;
  background: transparent;
}
.trait-label {
  display: block;
  margin-bottom: 4px;
  color: #5c3d2e;
  font-size: var(--text-xs);
}
.trait-panel strong {
  display: block;
  color: #4a2c1a;
  font-size: var(--text-xl);
  line-height: 1.3;
}
.trait-panel p {
  margin: 6px 0 0;
  color: #5c3d2e;
  font-size: var(--text-sm);
  line-height: 1.5;
}
.info-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 0 0 16px;
}
.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 6px 0;
  border-top: 1px solid rgba(92, 61, 46, 0.14);
  background: transparent;
}
.info-item span {
  flex-shrink: 0;
  color: #5c3d2e;
  font-size: var(--text-sm);
}
.info-item strong {
  color: #4a2c1a;
  font-size: var(--text-md);
  text-align: right;
  max-width: 62%;
  white-space: normal;
  line-height: 1.35;
}
.info-item-skill {
  align-items: flex-start;
}
.primary-btn {
  display: block;
  position: relative;
  z-index: 2;
  width: min(420px, 52%);
  margin: 0 auto;
  padding: 10px 16px;
  border: none;
  border-radius: 0;
  background-color: transparent;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  color: #4a3520;
  cursor: pointer;
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  transition: all var(--transition-fast);
}
.primary-btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.1);
}
.slogan {
  position: relative;
  z-index: 2;
  text-align: center;
  font-size: 20px;
  font-weight: var(--weight-bold);
  color: #4a2c1a;
  margin: 8px 0 8px;
}

@media (max-width: 767px) {
  .customer-page { padding: var(--space-4); }
  .customer-card { padding: var(--space-5) var(--space-5) var(--space-4); }
  .card-layout { grid-template-columns: 1fr; }
  .customer-avatar { max-width: 280px; max-height: 280px; }
}

@media (max-height: 780px) {
  .customer-card { margin-top: 28px; padding: 32px 40px 24px 88px; }
  h1 { font-size: var(--text-2xl); }
  .slogan { font-size: 18px; }
}
</style>
