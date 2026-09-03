<template>
  <div class="bully-card" :style="{ backgroundImage: `url(${panelBg})` }">
    <div class="bully-status">
      <div v-if="game.bullyDebuff" class="bully-effect">{{ game.bullyDebuff }}</div>
      <div class="bully-hp-row">
        <div class="bully-hp-bar">
          <div class="bully-hp-fill" :style="{ width: hpPercent + '%' }" />
          <span class="bully-hp-text">HP: {{ game.bullyHP }}/{{ game.maxBullyHP }}</span>
        </div>
        <div class="bully-shield" :class="{ 'is-empty': game.bullyDefense <= 0 }" title="本回合护盾">
          <img :src="shieldIcon" alt="护盾" class="shield-icon" />
          <span>{{ game.bullyDefense }}</span>
        </div>
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
import { useRoomStore } from '@/store/room'

const game = useGameStore()
const room = useRoomStore()

const hpPercent = computed(() => {
  return game.maxBullyHP > 0 ? Math.max(0, Math.min(100, (game.bullyHP / game.maxBullyHP) * 100)) : 0
})

const targetText = computed(() => {
  const target = String(game.bullyTarget || '').trim()
  if (target === 'self') return '防御自身'
  if (target === 'all') {
    const both = bothDeptLabel()
    return both ? `目标: ${both}` : '目标: 护卫'
  }
  if (target === 'player1' || target === 'player2') {
    const seat = target === 'player1' ? 0 : 1
    const dept = seatDept(seat)
    return dept ? `目标: ${dept}` : '目标: 护卫'
  }
  if (/^p[12]$/i.test(target)) {
    const seat = target.toLowerCase() === 'p1' ? 0 : 1
    const dept = seatDept(seat)
    return dept ? `目标: ${dept}` : '目标: 护卫'
  }
  if (target === 'sales' || target === '销售' || target === '销售部') return '目标: 销售部'
  if (target === 'purchase' || target === '采购' || target === '采购部') return '目标: 采购部'
  return `目标: ${target}`
})

function bothDeptLabel() {
  const labels = [seatDept(0), seatDept(1)]
    .map((label) => {
      if (label === 'sales' || label === '销售') return '销售部'
      if (label === 'purchase' || label === '采购') return '采购部'
      return label
    })
    .filter((label) => label === '销售部' || label === '采购部')
  return [...new Set(labels)].join('、')
}

function seatDept(seat: number) {
  const fromRoom = seat === 0 ? room.player1Dept : room.player2Dept
  if (fromRoom === '销售部' || fromRoom === '采购部') return fromRoom
  if (fromRoom === 'sales') return '销售部'
  if (fromRoom === 'purchase') return '采购部'
  return fromRoom || ''
}
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
.bully-hp-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bully-shield {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 1px;
  padding: 0;
  margin: 0;
  color: var(--color-accent);
  font-size: 20px;
  font-weight: var(--weight-bold);
  line-height: 1;
}
.bully-shield.is-empty {
  opacity: 0.45;
  color: var(--color-text-secondary);
}
.shield-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
  display: inline-block;
  filter: drop-shadow(0 0 4px rgba(0, 0, 0, 0.22));
}
.bully-shield span {
  font-size: 20px;
  line-height: 1;
  min-width: 1ch;
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
  flex: 1;
  min-width: 0;
  height: 28px;
  margin-top: 2px;
  background: var(--color-surface-03);
  border-radius: var(--radius-full);
  overflow: hidden;
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
