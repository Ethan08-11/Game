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
  // 下次打开「你的0来了」时恢复下一行
  // zero: '你的0来了',
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
  left: 0;
  top: -20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  padding: 0;
  margin: 0;
  border-radius: 0;
  background: transparent;
  color: #fffef2;
  border: none;
  box-shadow: none;
  line-height: 1;
  z-index: 2;
  pointer-events: none;
}
.defense-icon {
  width: 52px;
  height: 52px;
  object-fit: contain;
  display: block;
  filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.85)) drop-shadow(0 0 8px rgba(0, 0, 0, 0.45));
}
.defense-value {
  position: absolute;
  left: 50%;
  top: 48%;
  transform: translate(-50%, -50%);
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
  color: #fffef2;
  -webkit-text-stroke: 2px #2a1508;
  paint-order: stroke fill;
  text-shadow: 0 2px 3px rgba(0, 0, 0, 0.75);
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
