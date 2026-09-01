<template>
  <div v-if="game.employerTrait" class="employer-card" tabindex="0">
    <div class="employer-trait">
      {{ game.employerTrait.name }}
      <span class="info-dot" aria-hidden="true">i</span>
    </div>
    <div class="employer-attrs" role="tooltip">
      <p class="attr-name">{{ game.employerTrait.name }}</p>
      <p v-if="game.employerTrait.description" class="attr-desc">{{ game.employerTrait.description }}</p>
      <p class="attr-row">
        <span>顾客类型触发概率</span>
        <strong>{{ formatRate(game.employerTrait.typeTriggerRate) }}</strong>
      </p>
      <p class="attr-row">
        <span>效果触发概率</span>
        <strong>{{ formatRate(game.employerTrait.effectTriggerRate) }}</strong>
      </p>
      <p class="attr-row">
        <span>顾客效果</span>
        <strong>{{ effectText }}</strong>
      </p>
      <p v-if="game.employerTrait.bullyName" class="attr-row">
        <span>对应霸凌者</span>
        <strong>{{ game.employerTrait.bullyName }}</strong>
      </p>
      <p v-if="game.employerTrait.bullySkillSummary" class="attr-desc">{{ game.employerTrait.bullySkillSummary }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useGameStore } from '@/store/game'

const game = useGameStore()

const effectText = computed(() => {
  const trait = game.employerTrait
  if (!trait) return '加载中'
  const target = trait.effectType === 'player_hp'
    ? '我方血值'
    : trait.effectType === 'hp'
      ? '霸凌者血量'
      : '霸凌者基础攻击'
  const value = trait.effectValue ?? 0
  return `${target}${value >= 0 ? '+' : ''}${value}`
})

function formatRate(value?: number) {
  if (value == null) return '未配置'
  return `${Math.round(value * 100)}%`
}
</script>

<style scoped>
.employer-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 0;
  pointer-events: auto;
  outline: none;
  cursor: help;
}
.employer-trait {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #fff8e8;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.9), 0 0 10px rgba(0, 0, 0, 0.45);
  background: transparent;
}
.info-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 248, 232, 0.92);
  font-size: 10px;
  font-style: italic;
  font-weight: 700;
  line-height: 1;
  color: #fff8e8;
}
.employer-attrs {
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  min-width: 220px;
  max-width: 280px;
  padding: 8px 10px;
  background: transparent;
  color: #fff8e8;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.92), 0 0 8px rgba(0, 0, 0, 0.5);
  text-align: left;
  pointer-events: none;
  visibility: hidden;
  opacity: 0;
  z-index: 8;
}
.employer-card:hover .employer-attrs,
.employer-card:focus-within .employer-attrs {
  visibility: visible;
  opacity: 1;
  pointer-events: auto;
}
.attr-name {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
}
.attr-desc {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}
.attr-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin: 0 0 4px;
  font-size: 13px;
  line-height: 1.4;
}
.attr-row:last-child {
  margin-bottom: 0;
}
.attr-row span {
  opacity: 0.92;
  white-space: nowrap;
}
.attr-row strong {
  font-weight: 700;
  white-space: nowrap;
}
</style>
