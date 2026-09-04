<template>
  <main class="customer-page" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
    <BackButton to="/game-hall" text="返回大厅" />

    <section class="customer-card">
      <p class="eyebrow">顾客图鉴</p>
      <h1>全部顾客</h1>

      <div v-if="catalogReady" class="customer-scroll" :style="{ '--card-bg': `url(${cardBg})` }">
        <div class="customer-grid">
          <article v-for="customer in customers" :key="customer.customerTypeId ?? customer.customerCode" class="customer-item">
            <div class="highlight-section">
              <img :src="getImageUrl(customer.imageUrl) || avatarImg" alt="顾客形象" class="customer-avatar-img" loading="lazy" decoding="async" />
            </div>

            <div class="traits-section">
              <p class="customer-desc">{{ customer.description }}</p>

              <div class="customer-meta">
                <span>效果触发概率</span>
                <strong>{{ formatRate(customer.triggerChance) }}</strong>
              </div>
              <div class="customer-meta">
                <span>效果类型</span>
                <strong>{{ effectLabel(customer.effectType) }}</strong>
              </div>
              <div class="customer-meta">
                <span>效果数值</span>
                <strong>{{ formatEffectValue(customer.effectType, customer.effectValue) }}</strong>
              </div>
              <div v-if="customer.bullyName" class="customer-meta">
                <span>对应霸凌者</span>
                <strong>{{ customer.bullyName }}</strong>
              </div>
              <div v-if="customer.bullySkillSummary" class="customer-meta customer-meta-skill">
                <span>霸凌者特效</span>
                <strong>{{ customer.bullySkillSummary }}</strong>
              </div>
              <div v-if="customer.bullyName" class="customer-meta">
                <span>霸凌者特效概率</span>
                <strong>{{ formatBullyChance(customer.bullySkillChance) }}</strong>
              </div>
            </div>

            <div class="name-section">
              <h2>{{ customer.customerName }}</h2>
            </div>
          </article>

          <article class="customer-item placeholder-card">
            <div class="name-section">
              <h2>敬请期待中。。。</h2>
            </div>
          </article>
        </div>
        <div class="customer-scroll-spacer" aria-hidden="true"></div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import BackButton from '@/components/BackButton.vue'
import { getCustomerCatalog, type CustomerApiItem } from '@/api'
import bg1 from '@/assets/hall-bg.webp'
import bg2 from '@/assets/hall-bg2.webp'
import cardBg from '@/assets/customer-card-bg.webp'
import avatarImg from '@/assets/customer-avatar-intro.webp'
import { getImageUrl } from '@/utils/imageUrl'

const bgDay = bg2
const bgNight = bg1
const bgImage = ref('')
const customers = ref<CustomerApiItem[]>([])
const catalogReady = ref(false)

function formatRate(value?: number) {
  if (value == null) return '后端未配置'
  return `${Math.round((value > 1 ? value / 100 : value) * 100)}%`
}

function formatBullyChance(value?: number | null) {
  if (value == null) return '常驻'
  return formatRate(value)
}

function effectLabel(effectType?: string) {
  const map: Record<string, string> = {
    bully_attack_down: '霸凌者攻击降低',
    bully_hp_up: '霸凌者血量提升',
    bully_attack_up: '霸凌者攻击提升',
    player_hp_up: '我方血值恢复',
    bully_defense_up: '霸凌者防御提升',
    hp: '血量变化',
    attack: '攻击变化',
    DAMAGE_BOSS: '对BOSS造成伤害',
    HEAL_PLAYER: '治疗',
    ADD_SHIELD: '增加护盾',
    REDUCE_SHIELD: '减少护盾',
    RESET_SHIELD: '护盾清零',
    MULTI_HIT: '多段攻击',
    POISON: '中毒',
    STUN: '眩晕',
    DAMAGE_SELF: '自损',
    DAMAGE_PLAYER: '对敌人伤害',
    HEAL_BOSS: '治疗BOSS',
    INTERCEPT_ATTACK: '承受攻击',
    DODGE_ATTACK: '躲避攻击',
    TRIGGER_DAMAGE: '触发伤害',
  }
  return map[effectType || ''] || effectType || '未知'
}

function formatEffectValue(effectType?: string, value?: number) {
  const num = value ?? 0
  const prefix = num >= 0 ? '+' : ''
  if (effectType === 'player_hp_up' || /heal_player/i.test(effectType || '')) {
    return `血值${prefix}${num}`
  }
  const unit = effectType?.includes('hp') ? 'HP' : '攻击'
  return `${unit}${prefix}${num}`
}

onMounted(async () => {
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
  try {
    customers.value = await getCustomerCatalog()
    console.log('[调试] CustomerIntro getCustomerCatalog 原始返回:', JSON.stringify(customers.value))
    customers.value.forEach((c, i) => {
      console.log(`[调试] 顾客[${i}] ${c.customerName} imageUrl:`, c.imageUrl)
    })
  } catch (e) {
    console.log('[调试] API调用失败:', e)
    customers.value = []
  } finally {
    catalogReady.value = true
  }
})
</script>

<style scoped>
.customer-page {
  height: 100%;
  padding: var(--space-6) var(--space-10) var(--space-8);
  color: var(--color-text-primary);
  position: relative;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
.customer-page > :not(.back-btn) {
  position: relative;
  z-index: 1;
}
.customer-card {
  width: min(1100px, 95vw);
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin: 0 auto;
  padding: var(--space-5) var(--space-8) var(--space-6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-xl);
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-shadow: var(--shadow-lg);
}
.eyebrow {
  flex-shrink: 0;
  color: var(--color-accent);
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  letter-spacing: 0.2em;
}
h1 {
  flex-shrink: 0;
  margin: var(--space-1) 0 var(--space-2);
  font-size: var(--text-3xl);
  color: #f0e6d2;
}
.story {
  color: rgba(255, 255, 255, 0.7);
  font-size: var(--text-lg);
  line-height: var(--leading-relaxed);
}
.customer-scroll {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  margin-top: var(--space-2);
  padding: 148px var(--space-2) 16px;
}
.customer-scroll-spacer {
  height: 48px;
  flex-shrink: 0;
  pointer-events: none;
}
.customer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 200px var(--space-4);
}
.customer-item {
  position: relative;
  z-index: 1;
  padding: var(--space-5);
  padding-bottom: 48px;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  overflow: visible;
  min-height: 396px;
}
.customer-item:nth-child(3n+1) { z-index: 3; }
.customer-item:nth-child(3n+2) { z-index: 2; }
.customer-item:nth-child(3n+3) { z-index: 1; }
.customer-item::before {
  content: '';
  position: absolute;
  width: 210%;
  height: 546px;
  left: -55%;
  top: -143px;
  background: var(--card-bg, var(--color-surface-01)) center / contain no-repeat;
  z-index: -1;
  pointer-events: none;
}
.name-section {
  position: absolute;
  top: 356px;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 3px 18px 0;
  white-space: nowrap;
  z-index: 2;
  pointer-events: none;
}
.highlight-section {
  position: absolute;
  left: 50%;
  top: -78px;
  transform: translateX(-50%);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  width: 310px;
  height: 268px;
  overflow: visible;
}
.customer-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center bottom;
  transform: scale(1.36);
  transform-origin: center bottom;
}
.traits-section {
  position: absolute;
  left: 50%;
  top: 176px;
  transform: translateX(-50%);
  width: 268px;
  padding: 0 8px;
}
h2 {
  margin: 0;
  font-size: 14px;
  line-height: 1;
  font-weight: var(--weight-bold);
  color: #3a1f0d;
}
.status-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(139, 105, 20, 0.15);
  color: #8b6914;
  font-size: var(--text-xs);
  white-space: nowrap;
}
.customer-desc, .highlight-desc {
  margin: 2px 0 6px;
  color: #5c3d2e;
  font-size: 12px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.customer-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  padding: 1px 0;
  border-top: 1px solid rgba(139, 105, 20, 0.2);
  color: #5c3d2e;
  font-size: 12px;
  line-height: 1.3;
}
.customer-meta span {
  flex: 0 0 auto;
}
.customer-meta strong {
  color: #3a1f0d;
  text-align: right;
  min-width: 0;
  flex: 1 1 auto;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.customer-meta-skill {
  align-items: flex-start;
}
.customer-meta-skill strong {
  white-space: normal;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}
.placeholder-card .name-section {
  position: absolute;
  left: 50%;
  top: 50%;
  bottom: auto;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  height: auto;
  padding: var(--space-4) var(--space-8);
}
.placeholder-card h2 {
  font-size: var(--text-2xl);
  line-height: 1.2;
}

@media (max-width: 900px) {
  .customer-grid { grid-template-columns: 1fr; }
}
</style>

<style>
.back-btn {
  top: 28px !important;
  z-index: 10 !important;
}
</style>

