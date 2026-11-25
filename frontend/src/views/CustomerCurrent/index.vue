<template>
  <main class="customer-page" :class="{ 'is-en': lang === 'en' }" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
    <BackButton to="/game-hall" :text="t('返回大厅')" />

    <section v-if="publicEdition" class="customer-card pick-panel" :style="{ '--panel-bg': `url(${panelBg})` }">
      <div class="card-title" :style="{ backgroundImage: `url(${titleBanner})` }">{{ t('选择本局顾客') }}</div>
      <div v-if="catalogReady" class="pick-grid">
        <button
          v-for="customer in customers"
          :key="customer.customerTypeId ?? customer.customerCode"
          type="button"
          class="pick-item"
          :class="{ on: String(customer.customerTypeId) === selectedCustomerId }"
          @click="selectedCustomerId = String(customer.customerTypeId ?? '')"
        >
          <img :src="getImageUrl(customer.imageUrl) || ''" :alt="t('顾客形象')" />
          <strong>{{ shownCatalog(customer).name }}</strong>
          <span><em>{{ t('效果触发概率') }}</em><b>{{ formatCatalogRate(customer.triggerChance) }}</b></span>
          <span><em>{{ t('效果类型') }}</em><b>{{ t(effectLabel(customer.effectType)) }}</b></span>
          <span><em>{{ t('效果数值') }}</em><b>{{ localizeEffect(formatCatalogEffect(customer)) }}</b></span>
        </button>
      </div>
      <div class="card-footer">
        <button type="button" class="primary-btn" :disabled="!selectedCustomerId || jumping" :style="{ backgroundImage: `url(${titleBanner})` }" @click.stop.prevent="goMatchMaking">
          {{ jumping ? t('跳转中...') : t('保护我们的顾客') }}
        </button>
      </div>
    </section>

    <section v-else-if="customerReady" class="customer-card" :style="{ '--panel-bg': `url(${panelBg})` }">
      <div class="card-title" :style="{ backgroundImage: `url(${titleBanner})` }">{{ t('顾客来访') }}</div>
      <div class="card-layout">
        <div class="card-body">
          <h1>{{ shownCustomer.name }}</h1>
          <p class="story">{{ t('顾客长期遭受霸凌者欺凌，已向 HIH 发起求助。请先了解本局顾客属性，再进入部门选择。') }}</p>

          <div class="trait-panel">
            <span class="trait-label">{{ t('顾客属性') }}</span>
            <strong>{{ shownCustomer.name }}</strong>
            <p>{{ shownCustomer.description }}</p>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span>{{ t('顾客类型触发概率') }}</span>
              <strong>{{ formatRate(trait?.typeTriggerRate) }}</strong>
            </div>
            <div class="info-item">
              <span>{{ t('效果触发概率') }}</span>
              <strong>{{ formatRate(trait?.effectTriggerRate) }}</strong>
            </div>
            <div class="info-item">
              <span>{{ t('顾客效果') }}</span>
              <strong>{{ effectText }}</strong>
            </div>
            <div v-if="trait?.bullyName" class="info-item">
              <span>{{ t('对应霸凌者') }}</span>
              <strong>{{ t(trait.bullyName) }}</strong>
            </div>
            <div v-if="trait?.bullySkillSummary" class="info-item info-item-skill">
              <span>{{ t('霸凌者特效') }}</span>
              <strong>{{ localizeEffect(trait.bullySkillSummary) }}</strong>
            </div>
            <div v-if="trait?.bullyName" class="info-item">
              <span>{{ t('霸凌者特效概率') }}</span>
              <strong>{{ bullyChanceText }}</strong>
            </div>
          </div>
        </div>
        <img v-if="customerAvatar" :src="customerAvatar" :alt="t('顾客形象')" class="customer-avatar" />
      </div>

      <div class="card-footer">
        <button type="button" class="primary-btn" :disabled="jumping" :style="{ backgroundImage: `url(${titleBanner})` }" @click.stop.prevent="goMatchMaking">
          {{ jumping ? t('跳转中...') : t('保护我们的顾客') }}
        </button>
        <p class="slogan">{{ t('顾客需要您们的帮助！！！') }}</p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '@/store/game'
import { useUserStore } from '@/store/user'
import { getCustomerCatalog, type CustomerApiItem } from '@/api'
import BackButton from '@/components/BackButton.vue'
import bg1 from '@/assets/hall-bg.webp'
import bg2 from '@/assets/hall-bg2.webp'
import panelBg from '@/assets/customer-panel-bg.webp'
import titleBanner from '@/assets/title-banner.webp'
import { getImageUrl } from '@/utils/imageUrl'
import { customerEffectSummary } from '@/utils/customerEffectText'
import { lang, t } from '@/i18n'
import { localizeCustomer, localizeEffect } from '@/i18n/catalog'

const bgDay = bg2
const bgNight = bg1
const bgImage = ref('')

const PUBLIC_CUSTOMER_KEY = 'publicSelectedCustomerId'
const router = useRouter()
const game = useGameStore()
const user = useUserStore()
const publicEdition = computed(() => user.audience === 'PUBLIC')
const jumping = ref(false)
const customers = ref<CustomerApiItem[]>([])
const catalogReady = ref(false)
const selectedCustomerId = ref(sessionStorage.getItem(PUBLIC_CUSTOMER_KEY) || '')
const trait = computed(() => game.employerTrait)
const customerReady = computed(() => {
  const name = (game.employerName || '').trim()
  return Boolean(trait.value && name && name !== '雇主')
})
const customerAvatar = computed(() => getImageUrl(trait.value?.imageUrl) || '')
const shownCustomer = computed(() => {
  const current = trait.value
  if (!current) return { name: '', description: '' }
  return localizeCustomer({ code: current.id, name: current.name, description: current.description })
})
const effectText = computed(() => {
  if (!trait.value) return ''
  const summary = customerEffectSummary({ id: trait.value.id, name: trait.value.name, effectType: trait.value.effectType })
  if (summary) return localizeEffect(summary)
  const value = trait.value.effectValue ?? 0
  if (trait.value.effectType === 'player_harsh') {
    const amount = Math.abs(value)
    return localizeEffect(`一名玩家调用机会-1且失血${amount}`)
  }
  const target = trait.value.effectType === 'player_hp'
    ? '我方血值'
    : trait.value.effectType === 'player_action'
      ? '一名玩家调用机会'
      : trait.value.effectType === 'hp'
        ? '霸凌者血量'
        : '霸凌者基础攻击'
  return localizeEffect(`${target}${value >= 0 ? '+' : ''}${value}`)
})
const bullyChanceText = computed(() => {
  const chance = trait.value?.bullySkillChance
  if (chance == null) return t('常驻')
  return formatRate(chance)
})

function formatRate(value?: number) {
  if (value == null) return t('后端未配置')
  return `${Math.round(value * 100)}%`
}

const hour = new Date().getHours()
bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
if (!publicEdition.value) void game.loadCurrentCustomer()

function shownCatalog(customer: CustomerApiItem) {
  return localizeCustomer({
    code: customer.customerCode,
    name: customer.customerName,
    description: customer.description,
  })
}

function formatCatalogRate(value?: number) {
  if (value == null) return t('后端未配置')
  return `${Math.round((value > 1 ? value / 100 : value) * 100)}%`
}

function effectLabel(effectType?: string) {
  const map: Record<string, string> = {
    bully_attack_down: '霸凌者攻击降低',
    bully_hp_up: '霸凌者血量提升',
    bully_attack_up: '霸凌者攻击提升',
    player_hp_up: '我方血值恢复',
    player_action_up: '一名玩家调用机会增加',
    player_draw: '两名玩家抽牌',
    player_discard: '一名玩家弃牌',
    player_shield_next: '下回合两人获得护盾',
    dual_jujube: '好坏两面',
    dual_phone: '好坏两面',
    player_action_hp_down: '一名玩家调用机会减少并失血',
    bully_defense_up: '霸凌者防御提升',
    hp: '血量变化',
    attack: '攻击变化',
  }
  return map[effectType || ''] || effectType || '未知'
}

function formatCatalogEffect(customer: CustomerApiItem) {
  const summary = customerEffectSummary({
    id: customer.customerCode,
    name: customer.customerName,
    effectType: customer.effectType,
  })
  if (summary) return summary
  const effectType = customer.effectType
  const num = customer.effectValue ?? 0
  const prefix = num >= 0 ? '+' : ''
  if (effectType === 'player_action_hp_down' || /action_hp|harsh/i.test(effectType || '')) {
    return `调用机会-1且失血${Math.abs(num)}`
  }
  if (effectType === 'player_action_up' || /action/i.test(effectType || '')) return `调用机会${prefix}${num}`
  if (effectType === 'player_hp_up' || /heal_player/i.test(effectType || '')) return `血值${prefix}${num}`
  const unit = effectType?.includes('hp') ? 'HP' : '攻击'
  return `${unit}${prefix}${num}`
}

onMounted(async () => {
  if (!publicEdition.value) return
  try {
    customers.value = await getCustomerCatalog()
  } catch {
    customers.value = []
  } finally {
    catalogReady.value = true
  }
})

async function goMatchMaking() {
  if (jumping.value) return
  if (publicEdition.value && !selectedCustomerId.value) return
  jumping.value = true
  try {
    if (publicEdition.value) sessionStorage.setItem(PUBLIC_CUSTOMER_KEY, selectedCustomerId.value)
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
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  padding: 64px var(--space-8);
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
.customer-page > :not(.back-btn) {
  position: relative;
  z-index: 1;
}
.customer-card {
  position: relative;
  width: min(82%, 1000px);
  min-height: 0;
  margin: 0;
  padding: 40px 48px 36px 96px;
  display: flex;
  flex-direction: column;
  justify-content: center;
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
  inset: -18px -3%;
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
  max-width: 420px;
  height: min(400px, 48vh);
  object-fit: contain;
  object-position: center;
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
  white-space: nowrap;
}
.customer-page.is-en .card-title {
  font-size: 18px;
}
.customer-page.is-en .primary-btn {
  font-size: 16px;
}
.customer-page.is-en .slogan {
  font-size: 18px;
  white-space: nowrap;
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
.card-footer {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 0;
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
.primary-btn:disabled {
  opacity: 0.55;
  cursor: default;
  transform: none;
}
.pick-panel {
  width: min(92%, 1080px);
  padding: 48px 28px 24px;
}
.pick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  max-height: min(58vh, 520px);
  overflow: auto;
  padding-right: 4px;
}
.pick-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  padding: 8px;
  border: 2px solid transparent;
  border-radius: 10px;
  background: rgba(255, 248, 230, 0.72);
  color: #3a1f0d;
  cursor: pointer;
  text-align: left;
}
.pick-item.on {
  border-color: #8b6914;
  background: rgba(255, 244, 214, 0.95);
}
.pick-item img {
  width: 100%;
  height: 88px;
  object-fit: contain;
  object-position: center bottom;
}
.pick-item strong {
  font-size: 14px;
  line-height: 1.2;
  text-align: center;
}
.pick-item span {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  font-size: 12px;
  line-height: 1.3;
}
.pick-item em {
  font-style: normal;
  color: #5c3d2e;
  flex: 0 0 auto;
}
.pick-item b {
  font-weight: 700;
  text-align: right;
  min-width: 0;
}
.slogan {
  position: relative;
  z-index: 2;
  text-align: center;
  font-size: 20px;
  font-weight: var(--weight-bold);
  color: #4a2c1a;
  margin: 8px 0 0;
}

@media (max-width: 767px) {
  .customer-page { padding: 56px var(--space-4); }
  .customer-card { padding: var(--space-5); }
  .card-layout { grid-template-columns: 1fr; }
  .customer-avatar { max-width: 280px; height: min(280px, 38vh); transform: none; }
}

@media (max-height: 780px) {
  .customer-page { padding: 52px var(--space-8); }
  .customer-card { padding: 32px 40px 28px 88px; }
  .customer-avatar { height: min(340px, 42vh); }
  h1 { font-size: var(--text-2xl); }
  .slogan { font-size: 18px; }
}
</style>
