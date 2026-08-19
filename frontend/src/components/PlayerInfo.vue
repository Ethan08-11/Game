<template>
  <div class="player-info" :class="{ danger: staminaPercentage <= 25 }">
    <img class="info-bg-img" :src="infoBg" alt="" />
    <div v-if="defense > 0" class="defense-row" aria-label="本回合防御点数">
      <img :src="shieldIcon" alt="盾" class="defense-icon" />
      <span class="defense-value">{{ defense }}</span>
    </div>
    <div class="info-text">
      <span class="stamina">{{ stamina }}/{{ maxStamina }} 血值</span>
      <div class="stamina-row">
        <span class="nickname">{{ displayName }}<span v-if="isSelf">（我）</span></span>
        <el-progress :percentage="staminaPercentage" :color="staminaColor" :show-text="false" class="stamina-bar" />
        <span class="dept-label">{{ deptDisplay }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import shieldIcon from '@/assets/player-shield.webp'
import infoBg from '@/assets/battle/player-info-bg.webp'
import { formatPlayerName } from '@/utils/playerName'

const props = withDefaults(defineProps<{
  dept: string
  username?: string
  stamina: number
  maxStamina?: number
  defense?: number
  isSelf?: boolean
}>(), {
  username: '',
  maxStamina: 100,
  defense: 0,
  isSelf: false,
})

const staminaPercentage = computed(() => {
  if (props.maxStamina <= 0) return 0
  return Math.max(0, Math.min(100, Math.round((props.stamina / props.maxStamina) * 100)))
})

const staminaColor = computed(() => '#8B0000')
const displayName = computed(() => formatPlayerName(props.username))

const deptLabelMap: Record<string, string> = {
  sales: '销售部',
  purchase: '采购部',
  logistics: '物流部',
  marketing: '营销部',
  design: '设计部',
  tech: '技术部',
  finance: '财务部',
  hr: '人事部',
}
const deptDisplay = computed(() => deptLabelMap[props.dept?.toLowerCase()] || props.dept || '')
</script>

<style scoped>
.player-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 248px;
  min-height: 64px;
  padding: 10px 16px 12px;
  background-color: transparent;
  border-radius: var(--radius-md);
  color: #3E2723;
  transition: box-shadow var(--transition-base);
  position: relative;
  overflow: visible;
}
.info-bg-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: fill;
  z-index: 0;
  pointer-events: none;
}
.player-info.danger {
  box-shadow: 0 0 8px var(--color-danger-muted);
}
.info-text {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-height: 44px;
  width: 100%;
}
.defense-row {
  position: absolute;
  left: 4px;
  top: -8px;
  display: inline-flex;
  align-items: center;
  gap: 0;
  width: fit-content;
  padding: 0;
  margin: 0;
  border-radius: 0;
  background: transparent;
  color: var(--color-accent);
  border: none;
  box-shadow: none;
  font-size: 28px;
  font-weight: var(--weight-bold);
  line-height: 1;
  z-index: 2;
  pointer-events: none;
}
.defense-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
  display: inline-block;
  filter: drop-shadow(0 0 4px rgba(0, 0, 0, 0.22));
}
.defense-value {
  font-size: 22px;
  line-height: 1;
  transform: translateY(-1px);
  font-feature-settings: 'tnum';
}
.stamina-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.nickname {
  font-size: var(--text-base);
  font-weight: var(--weight-bold);
  white-space: nowrap;
}
.stamina-bar { width: 100px; }
.dept-label {
  font-size: var(--text-sm);
  white-space: nowrap;
}
.stamina {
  font-size: var(--text-base);
}
</style>
