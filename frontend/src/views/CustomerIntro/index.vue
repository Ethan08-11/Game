<template>
  <main class="customer-page" :style="{ '--hall-bg': bgImage ? `url(${bgImage})` : '' }">
    <BackButton to="/game-hall" text="返回大厅" />

    <section class="customer-card">
      <p class="eyebrow">顾客图鉴</p>
      <h1>全部顾客</h1>

      <div class="customer-scroll" :style="{ '--card-bg': `url(${cardBg})` }">
        <div class="customer-grid">
          <article v-for="customer in customers" :key="customer.customerTypeId ?? customer.customerCode" class="customer-item">
            <div class="name-section">
              <h2>{{ customer.customerName }}</h2>
            </div>

            <div class="highlight-section" :style="{ top: (traitsTop - 240) + 'px', height: (240) + 'px' }">
              <img :src="getImageUrl(customer.imageUrl) || avatarImg" alt="顾客形象" class="customer-avatar-img" loading="lazy" decoding="async" />
            </div>

            <div class="traits-section" :style="{ top: traitsTop + 'px' }">
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
            </div>
          </article>

          <article class="customer-item placeholder-card">
            <div class="name-section">
              <h2>敬请期待中。。。</h2>
            </div>
          </article>
        </div>
      </div>
    </section>
    <!-- 绿色框位置调节器 -->
    <Teleport to="body">
      <div v-if="showAdjuster" class="traits-adjuster-panel">
        <div class="adjuster-header">
          <span>绿色框上下位置 (top)</span>
          <button class="adjuster-close" @click="showAdjuster = false">✕</button>
        </div>
        <div class="adjuster-body">
          <label>top <span>{{ traitsTop }}px</span></label>
          <input type="range" v-model.number="traitsTop" min="-200" max="400" />
        </div>
      </div>
      <button class="adjuster-toggle" @click="showAdjuster = !showAdjuster">⚙T</button>
    </Teleport>
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
const showAdjuster = ref(false)
const traitsTop = ref(180)

function formatRate(value?: number) {
  if (value == null) return '后端未配置'
  return `${Math.round((value > 1 ? value / 100 : value) * 100)}%`
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

const mockCustomers: CustomerApiItem[] = [
  {
    customerTypeId: 1,
    customerCode: 'BOSS_WANG',
    customerName: '王总',
    description: '外贸行业资深客户，对交货周期要求极高，但订单量大且稳定。',
    effectType: 'bully_attack_down',
    effectValue: -8,
    selectionWeight: 35,
    triggerChance: 60,
    status: 1,
  },
  {
    customerTypeId: 2,
    customerCode: 'BUYER_LI',
    customerName: '李采购',
    description: '大型连锁企业采购经理，擅长压价但信誉良好，偶尔会追加紧急订单。',
    effectType: 'bully_hp_up',
    effectValue: 15,
    selectionWeight: 28,
    triggerChance: 45,
    status: 1,
  },
  {
    customerTypeId: 3,
    customerCode: 'CLIENT_ZHANG',
    customerName: '张客户',
    description: '初创公司创始人，对产品质量吹毛求疵，但愿意为新供应商提供试单机会。',
    effectType: 'attack',
    effectValue: 5,
    selectionWeight: 22,
    triggerChance: 55,
    status: 1,
  },
  {
    customerTypeId: 4,
    customerCode: 'CUSTOMER_WINDOW',
    customerName: '闲逛双客',
    description: '结伴闲逛却从不落单，偶尔会给两名护卫恢复血值。',
    imageUrl: '/images/customer/p4.webp',
    effectType: 'player_hp_up',
    effectValue: 2,
    selectionWeight: 10,
    triggerChance: 20,
    status: 1,
  },
]

onMounted(async () => {
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bgDay : bgNight
  try {
    customers.value = await getCustomerCatalog()
    console.log('[调试] CustomerIntro getCustomerCatalog 原始返回:', JSON.stringify(customers.value))
    customers.value.forEach((c, i) => {
      console.log(`[调试] 顾客[${i}] ${c.customerName} imageUrl:`, c.imageUrl)
    })
    if (!customers.value || customers.value.length === 0) {
      customers.value = mockCustomers
      console.log('[调试] API返回空，使用 mockCustomers')
    }
  } catch (e) {
    console.log('[调试] API调用失败:', e)
    customers.value = mockCustomers
  }
})
</script>

<style scoped>
.customer-page {
  height: 100%;
  padding: var(--space-10);
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
  padding: var(--space-8);
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
  margin: var(--space-2) 0 var(--space-4);
  font-size: var(--text-4xl);
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
  padding: 130px var(--space-2) 160px;
}
.customer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 300px var(--space-4);
}
.customer-item {
  position: relative;
  z-index: 1;
  padding: var(--space-5);
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  overflow: visible;
  min-height: 260px;
}
.customer-item:nth-child(3n+1) { z-index: 3; }
.customer-item:nth-child(3n+2) { z-index: 2; }
.customer-item:nth-child(3n+3) { z-index: 1; }
.customer-item::before {
  content: '';
  position: absolute;
  width: 210%;
  height: 210%;
  left: -55%;
  top: -55%;
  background: var(--card-bg, var(--color-surface-01)) center/contain no-repeat;
  z-index: -1;
  pointer-events: none;
}
.name-section {
  position: absolute;
  bottom: -110px;
  left: 50%;
  transform: translateX(-50%);
  display: inline-block;
  padding: var(--space-2) var(--space-3);

  white-space: nowrap;
}
.highlight-section {
  position: absolute;
  left: var(--space-5);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 280px;
  padding: var(--space-2) var(--space-3);

  overflow: visible;
}
.customer-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transform: scale(1.04);
}
.traits-section {
  position: absolute;
  left: var(--space-5);
  display: inline-block;
  width: 280px;
  padding: var(--space-2) var(--space-3);

}
h2 {
  margin: 0;
  font-size: var(--text-2xl);
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
  margin: var(--space-2) 0;
  color: #5c3d2e;
  line-height: 1.4;
}
.customer-meta {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 4px 0;
  border-top: 1px solid rgba(139, 105, 20, 0.2);
  color: #5c3d2e;
}
.customer-meta strong {
  color: #3a1f0d;
}
.placeholder-card .name-section {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4) var(--space-8);

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
.traits-adjuster-panel {
  position: fixed;
  z-index: 9999;
  right: 12px;
  bottom: 108px;
  width: 260px;
  background: rgba(20, 20, 20, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 12px;
  color: #ccc;
  font-size: 13px;
}
.adjuster-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: bold;
  color: #fff;
}
.adjuster-close {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 14px;
}
.adjuster-body label {
  display: flex;
  justify-content: space-between;
  margin: 6px 0 2px;
}
.adjuster-body label span {
  color: #c4a962;
}
.adjuster-body input[type="range"] {
  width: 100%;
  accent-color: #c4a962;
}
.adjuster-toggle {
  position: fixed;
  z-index: 9999;
  right: 12px;
  bottom: 56px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
