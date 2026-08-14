<template>
  <div class="page">
    <BackButton to="/game-hall" text="返回大厅" />
    <h2>顾客图鉴</h2>
    <p class="subtitle">所有顾客都遭受同样困扰，但不同顾客会带来不同作战影响。</p>

    <div class="grid">
      <div v-for="customer in customers" :key="customer.id" class="card">
        <div class="avatar">{{ customer.name.charAt(0) }}</div>
        <div class="name">{{ customer.name }}</div>
        <p class="desc">{{ customer.description || '暂无顾客描述' }}</p>
        <div class="meta">
          <span>类型触发</span>
          <strong>{{ formatRate(customer.typeTriggerRate) }}</strong>
        </div>
        <div class="meta">
          <span>效果触发</span>
          <strong>{{ formatRate(customer.effectTriggerRate) }}</strong>
        </div>
        <div class="effect">{{ getEffectText(customer) }}</div>
      </div>
    </div>

    <div v-if="customers.length === 0" class="empty">暂无顾客数据</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCustomers } from '@/api'
import type { EmployerTrait } from '@/types/gameEntities'
import BackButton from '@/components/BackButton.vue'

const customers = ref<EmployerTrait[]>([])

function formatRate(value?: number) {
  if (value == null) return '未配置'
  return `${Math.round(value * 100)}%`
}

function getEffectText(customer: EmployerTrait) {
  const target = customer.effectType === 'hp' ? '霸凌者血量' : '霸凌者基础攻击'
  const value = customer.effectValue ?? 0
  return `${target}${value >= 0 ? '+' : ''}${value}`
}

onMounted(async () => {
  try {
    customers.value = await getCustomers()
  } catch {
    customers.value = []
  }
})
</script>

<style scoped>
.page {
  position: relative;
  padding: var(--space-10);
  color: var(--color-text-primary);
  text-align: center;
}
h2 {
  margin-bottom: var(--space-2);
  font-size: var(--text-3xl);
  font-weight: var(--weight-semibold);
}
.subtitle {
  margin-bottom: var(--space-6);
  color: var(--color-text-secondary);
  font-size: var(--text-md);
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-4);
  max-width: 960px;
  margin: 0 auto var(--space-8);
}
.card {
  padding: var(--space-5);
  background: var(--color-surface-02);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-subtle);
  transition: all var(--transition-base);
}
.card:hover {
  background: var(--color-surface-03);
  border-color: var(--color-accent);
  transform: translateY(-2px);
}
.avatar {
  width: 64px;
  height: 64px;
  margin: 0 auto var(--space-3);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-accent-muted);
  color: var(--color-accent);
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
}
.name {
  margin-bottom: var(--space-2);
  color: var(--color-text-primary);
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}
.desc {
  min-height: 44px;
  margin-bottom: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  line-height: 1.6;
}
.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) 0;
  border-top: 1px solid var(--color-border-subtle);
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}
.meta strong {
  color: var(--color-accent);
}
.effect {
  margin-top: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-surface-01);
  color: var(--color-text-primary);
  font-weight: var(--weight-semibold);
}
.empty {
  color: var(--color-text-tertiary);
  margin-top: var(--space-8);
}
</style>
