<template>
  <div class="bully-card" :style="{ backgroundImage: `url(${panelBg})` }">
    <div class="bully-status">
      <div v-if="game.bullyDebuff" class="bully-effect">{{ game.bullyDebuff }}</div>
      <div v-if="game.bullyDefense > 0" class="bully-shield">
        <img :src="shieldIcon" alt="盾" class="shield-icon" />
        <span>{{ game.bullyDefense }}</span>
      </div>
      <div class="bully-hp-bar">
        <div class="bully-hp-fill" :style="{ width: hpPercent + '%' }" />
        <span class="bully-hp-text">HP: {{ game.bullyHP }}/{{ game.maxBullyHP }}</span>
      </div>
    </div>

    <div class="bully-name">{{ game.bullyName }}</div>
    <div class="bully-damage">
      <img :src="attackIcon" alt="攻击" class="attack-icon" />
      <span>攻击力: {{ game.bullyMinDamage }}-{{ game.bullyMaxDamage }}</span>
    </div>
    <div v-if="game.bullyTarget" class="bully-target">
      <el-icon :size="12"><WarningFilled /></el-icon>
      <span>{{ targetText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import shieldIcon from '@/assets/boss-shield.webp'
import attackIcon from '@/assets/Boss-attack.webp'
import panelBg from '@/assets/bully-panel-bg.webp'
import { useGameStore } from '@/store/game'

const game = useGameStore()

const hpPercent = computed(() => {
  return game.maxBullyHP > 0 ? Math.max(0, Math.min(100, (game.bullyHP / game.maxBullyHP) * 100)) : 0
})

const targetText = computed(() => {
  if (game.bullyTarget === 'self') return '防御自身'
  if (game.bullyTarget === 'all') return '攻击全体'
  return `目标: ${game.bullyTarget === 'player1' ? 'P1' : 'P2'}`
})
</script>

<style scoped>
.bully-card {
  text-align: center;
  color: var(--color-text-primary);
  padding: 18px var(--space-4) var(--space-4);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  border-radius: var(--radius-lg);
  overflow: visible;
}
.bully-status {
  width: 180px;
  margin: 0 auto var(--space-2);
  overflow: visible;
}
.bully-effect {
  margin-bottom: var(--space-1);
  padding: 2px var(--space-2);
  border-radius: var(--radius-full);
  background: var(--color-surface-02);
  color: var(--color-accent);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
}
.bully-shield {
  display: inline-flex;
  align-items: center;
  gap: 0;
  margin-bottom: var(--space-1);
  padding: 0;
  margin-left: -4px;
  border-radius: 0;
  background: transparent;
  color: var(--color-accent);
  border: none;
  box-shadow: none;
  font-size: 36px;
  font-weight: var(--weight-bold);
  line-height: 1;
}
.shield-icon {
  width: 56px;
  height: 56px;
  object-fit: contain;
  display: inline-block;
  filter: drop-shadow(0 0 4px rgba(0, 0, 0, 0.22));
}
.bully-shield span {
  font-size: 36px;
  line-height: 1;
  transform: translateY(-1px);
}
.bully-damage {
  color: var(--color-danger);
  font-size: var(--text-sm);
  margin-bottom: var(--space-1);
  display: flex; align-items: center; justify-content: center; gap: 4px;
}
.attack-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
  display: inline-block;
}
.bully-name {
  font-size: var(--text-lg);
  margin-bottom: var(--space-3);
  font-weight: var(--weight-bold);
}
.bully-hp-bar {
  position: relative;
  height: 28px;
  margin-top: 2px;
  background: var(--color-surface-03);
  border-radius: var(--radius-full);
  overflow: hidden;
  flex-shrink: 0;
}
.bully-hp-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-success), var(--color-warning), var(--color-danger));
  transition: width var(--transition-slow);
  border-radius: var(--radius-full);
}
.bully-hp-text {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  font-weight: var(--weight-bold); font-size: var(--text-base);
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}
.bully-damage {
  color: var(--color-danger);
  font-size: var(--text-sm);
  margin-bottom: var(--space-1);
  display: flex; align-items: center; justify-content: center; gap: var(--space-1);
}
.bully-target {
  color: var(--color-warning);
  font-size: var(--text-sm);
  margin-top: var(--space-1);
  display: flex; align-items: center; justify-content: center; gap: var(--space-1);
}
</style>
