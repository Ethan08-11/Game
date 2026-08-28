<template>
  <div
    class="player-info"
    :class="[
      `dept-${deptKey}`,
      { danger: staminaPercentage <= 25, healed: justHealed },
    ]"
    :style="{ '--hp-pct': staminaPercentage / 100 }"
  >
    <img class="info-bg-img" :src="infoBg" alt="" />
    <div class="crest-frame" aria-hidden="true">
      <svg class="crest-corner tl" viewBox="0 0 24 24">
        <path d="M3 20C3 9 9 3 20 3" fill="none" stroke="var(--crest-metal)" stroke-width="1.8" stroke-linecap="round" />
        <circle cx="5.2" cy="5.2" r="2.1" fill="#f0d48a" stroke="var(--crest-metal-dark)" stroke-width="0.8" />
      </svg>
      <svg class="crest-corner tr" viewBox="0 0 24 24">
        <path d="M4 3C15 3 21 9 21 20" fill="none" stroke="var(--crest-metal)" stroke-width="1.8" stroke-linecap="round" />
        <circle cx="18.8" cy="5.2" r="2.1" fill="#f0d48a" stroke="var(--crest-metal-dark)" stroke-width="0.8" />
      </svg>
      <svg class="crest-corner bl" viewBox="0 0 24 24">
        <path d="M3 4C3 15 9 21 20 21" fill="none" stroke="var(--crest-metal)" stroke-width="1.8" stroke-linecap="round" />
        <circle cx="5.2" cy="18.8" r="2.1" fill="#f0d48a" stroke="var(--crest-metal-dark)" stroke-width="0.8" />
      </svg>
      <svg class="crest-corner br" viewBox="0 0 24 24">
        <path d="M4 21C15 21 21 15 21 4" fill="none" stroke="var(--crest-metal)" stroke-width="1.8" stroke-linecap="round" />
        <circle cx="18.8" cy="18.8" r="2.1" fill="#f0d48a" stroke="var(--crest-metal-dark)" stroke-width="0.8" />
      </svg>
    </div>
    <div v-if="defense > 0" class="defense-row" aria-label="本回合防御点数">
      <span class="defense-aura" aria-hidden="true" />
      <span
        v-for="n in 8"
        :key="n"
        class="defense-spark"
        aria-hidden="true"
        :style="{ '--spark-i': n }"
      />
      <img :src="shieldIcon" alt="盾" class="defense-icon" />
      <span class="defense-value">{{ defense }}</span>
    </div>
    <div class="info-text">
      <div class="crest-meta">
        <span class="nickname">{{ displayName }}<span v-if="isSelf">（我）</span></span>
        <span class="dept-label">{{ deptDisplay }}</span>
      </div>
      <div class="crest-hp" :aria-label="`${stamina}/${maxStamina} 血值`">
        <svg class="hp-gem" viewBox="0 0 32 32" aria-hidden="true">
          <defs>
            <radialGradient :id="gemGradId" cx="38%" cy="32%" r="68%">
              <stop offset="0%" stop-color="#ffe3ea" />
              <stop offset="38%" stop-color="#e44555" />
              <stop offset="100%" stop-color="#5a1018" />
            </radialGradient>
            <linearGradient :id="gemRimId" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#f3e0a8" />
              <stop offset="45%" stop-color="var(--crest-metal)" />
              <stop offset="100%" stop-color="var(--crest-metal-dark)" />
            </linearGradient>
          </defs>
          <circle cx="16" cy="16" r="15" :stroke="`url(#${gemRimId})`" stroke-width="2.2" fill="rgba(42, 22, 12, 0.88)" />
          <path
            d="M16 25.2C16 25.2 6.8 18.4 6.8 12.6C6.8 9.4 9.2 7.4 11.8 7.4C13.5 7.4 15 8.4 16 10C17 8.4 18.5 7.4 20.2 7.4C22.8 7.4 25.2 9.4 25.2 12.6C25.2 18.4 16 25.2 16 25.2Z"
            :fill="`url(#${gemGradId})`"
          />
        </svg>
        <div class="hp-well">
          <div class="hp-fill" :style="{ width: staminaPercentage + '%' }">
            <span class="hp-sheen" />
          </div>
          <div class="hp-ticks" aria-hidden="true">
            <span v-for="n in 4" :key="n" class="hp-tick" :style="{ left: `${n * 20}%` }" />
          </div>
          <span class="hp-readout">{{ stamina }}/{{ maxStamina }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, useId, watch } from 'vue'
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

const uid = useId().replace(/:/g, '')
const gemGradId = `hp-gem-${uid}`
const gemRimId = `hp-rim-${uid}`

const staminaPercentage = computed(() => {
  if (props.maxStamina <= 0) return 0
  return Math.max(0, Math.min(100, Math.round((props.stamina / props.maxStamina) * 100)))
})

const displayName = computed(() => formatPlayerName(props.username))
const deptKey = computed(() => (props.dept || '').toLowerCase() || 'neutral')

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
const deptDisplay = computed(() => deptLabelMap[deptKey.value] || props.dept || '')

const justHealed = ref(false)
let healTimer: ReturnType<typeof setTimeout> | null = null
watch(() => props.stamina, (next, prev) => {
  if (typeof prev === 'number' && next > prev) {
    justHealed.value = true
    if (healTimer) clearTimeout(healTimer)
    healTimer = setTimeout(() => {
      justHealed.value = false
      healTimer = null
    }, 560)
  }
})
onBeforeUnmount(() => {
  if (healTimer) clearTimeout(healTimer)
})
</script>

<style scoped>
.player-info {
  --crest-metal: #d7b56a;
  --crest-metal-dark: #6e4a18;
  --crest-glow: rgba(212, 176, 90, 0.42);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 300px;
  min-height: 78px;
  padding: 10px 14px 12px 54px;
  background-color: transparent;
  border-radius: 14px;
  color: #3E2723;
  transition: box-shadow var(--transition-base), filter 0.35s ease;
  position: relative;
  overflow: visible;
}
.player-info.dept-purchase {
  --crest-metal: #e2b75a;
  --crest-metal-dark: #8a5a14;
  --crest-glow: rgba(226, 183, 90, 0.5);
}
.player-info.dept-sales {
  --crest-metal: #c5d48c;
  --crest-metal-dark: #4a6230;
  --crest-glow: rgba(140, 170, 96, 0.48);
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
.crest-frame {
  position: absolute;
  inset: 3px;
  z-index: 1;
  border-radius: 12px;
  border: 1.5px solid var(--crest-metal);
  box-shadow:
    inset 0 0 0 1px var(--crest-metal-dark),
    inset 0 1px 0 rgba(255, 236, 190, 0.5),
    0 0 0 1px rgba(62, 36, 10, 0.55),
    0 0 10px var(--crest-glow);
  pointer-events: none;
}
.crest-corner {
  position: absolute;
  width: 22px;
  height: 22px;
  overflow: visible;
  filter: drop-shadow(0 0 2px var(--crest-glow));
}
.crest-corner.tl { top: -6px; left: -6px; }
.crest-corner.tr { top: -6px; right: -6px; }
.crest-corner.bl { bottom: -6px; left: -6px; }
.crest-corner.br { bottom: -6px; right: -6px; }
.player-info.danger {
  box-shadow: 0 0 12px rgba(180, 32, 28, 0.55);
}
.player-info.healed {
  filter: drop-shadow(0 0 10px rgba(255, 210, 120, 0.8));
}
.info-text {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 52px;
  width: 100%;
}
.defense-row {
  position: absolute;
  left: -8px;
  top: -36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
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
  overflow: visible;
}
.defense-aura {
  position: absolute;
  inset: -14px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 50% 46%, rgba(255, 236, 160, 0.7) 0%, rgba(255, 196, 70, 0.28) 38%, rgba(255, 160, 40, 0) 70%);
  animation: defense-aura-pulse 1.7s ease-in-out infinite;
}
.defense-spark {
  position: absolute;
  left: 50%;
  top: 50%;
  width: calc(4px + (var(--spark-i) % 3) * 2px);
  height: calc(4px + (var(--spark-i) % 3) * 2px);
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #fffdf0 0%, #ffe58a 28%, #ffc14a 62%, transparent 78%);
  box-shadow:
    0 0 6px 2px rgba(255, 220, 110, 0.95),
    0 0 14px 5px rgba(255, 176, 48, 0.45);
  animation: defense-spark-orbit 2.4s linear infinite;
  animation-delay: calc(var(--spark-i) * -0.3s);
}
.defense-icon {
  position: relative;
  z-index: 1;
  width: 60px;
  height: 60px;
  object-fit: contain;
  display: block;
  filter:
    drop-shadow(0 0 5px rgba(255, 224, 120, 0.95))
    drop-shadow(0 0 12px rgba(255, 186, 52, 0.7))
    drop-shadow(0 2px 2px rgba(0, 0, 0, 0.8));
  animation: defense-icon-pulse 1.7s ease-in-out infinite;
}
.defense-value {
  position: absolute;
  left: 50%;
  top: 48%;
  z-index: 2;
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
@keyframes defense-aura-pulse {
  0%, 100% { opacity: 0.72; transform: scale(0.92); }
  50% { opacity: 1; transform: scale(1.08); }
}
@keyframes defense-icon-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.06); }
}
@keyframes defense-spark-orbit {
  0% {
    transform: rotate(calc(var(--spark-i) * 45deg)) translateX(calc(22px + var(--spark-i) * 2px)) scale(0.7);
    opacity: 0.15;
  }
  35% { opacity: 1; }
  100% {
    transform: rotate(calc(var(--spark-i) * 45deg + 360deg)) translateX(calc(22px + var(--spark-i) * 2px)) scale(1);
    opacity: 0.2;
  }
}
.crest-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}
.nickname {
  font-size: 13px;
  font-weight: var(--weight-bold);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #2a160c;
  text-shadow: 0 1px 0 rgba(255, 244, 214, 0.7);
}
.dept-label {
  font-size: 12px;
  white-space: nowrap;
  color: var(--crest-metal-dark);
  font-weight: 700;
}
.crest-hp {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.hp-gem {
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  filter:
    drop-shadow(0 0 4px rgba(226, 70, 80, calc(0.25 + var(--hp-pct) * 0.45)))
    saturate(calc(0.4 + var(--hp-pct) * 0.6))
    brightness(calc(0.62 + var(--hp-pct) * 0.45));
}
.hp-well {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 26px;
  border-radius: 13px;
  overflow: hidden;
  background: linear-gradient(180deg, #2c1610 0%, #140a08 58%, #3a2216 100%);
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.7),
    inset 0 -1px 0 rgba(255, 224, 170, 0.18),
    0 0 0 1px var(--crest-metal-dark),
    0 0 0 2px var(--crest-metal);
}
.hp-fill {
  position: relative;
  height: 100%;
  width: calc(var(--hp-pct) * 100%);
  max-width: 100%;
  background:
    linear-gradient(180deg, rgba(255, 210, 170, 0.42) 0%, transparent 42%),
    linear-gradient(90deg, #6a0d12 0%, #b41c26 46%, #dc3a3a 78%, #f06a52 100%);
  box-shadow: inset 0 1px 0 rgba(255, 228, 190, 0.35);
  transition: width 0.35s ease;
}
.hp-sheen {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 42%;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 244, 210, 0.38) 50%, transparent 100%);
  animation: hp-shimmer 2.8s ease-in-out infinite;
  pointer-events: none;
}
.hp-ticks {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.hp-tick {
  position: absolute;
  top: 3px;
  bottom: 3px;
  width: 1px;
  background: rgba(255, 232, 180, 0.28);
  box-shadow: 1px 0 0 rgba(40, 16, 8, 0.35);
  transform: translateX(-50%);
}
.hp-readout {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #fff8e8;
  -webkit-text-stroke: 1.2px #2a120c;
  paint-order: stroke fill;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.75);
  font-feature-settings: 'tnum';
  pointer-events: none;
}
.player-info.danger .hp-well {
  animation: hp-danger-pulse 1.05s ease-in-out infinite;
}
.player-info.danger .hp-fill {
  background:
    linear-gradient(180deg, rgba(255, 180, 140, 0.35) 0%, transparent 42%),
    linear-gradient(90deg, #4a080c 0%, #a01018 55%, #d62828 100%);
}
.player-info.healed .hp-fill {
  filter: brightness(1.35) saturate(1.2);
}
@keyframes hp-shimmer {
  0% { transform: translateX(-130%); }
  100% { transform: translateX(280%); }
}
@keyframes hp-danger-pulse {
  0%, 100% { box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.7), 0 0 0 1px #6a1810, 0 0 0 2px #d4a056, 0 0 8px rgba(180, 24, 20, 0.35); }
  50% { box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.7), 0 0 0 1px #8a2018, 0 0 0 2px #f0c878, 0 0 14px rgba(220, 40, 28, 0.7); }
}
</style>
