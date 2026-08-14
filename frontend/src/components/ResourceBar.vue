<template>
  <div class="resource-bar">
    <div class="res-item">
      <el-icon><Coin /></el-icon>
      <span>资金: {{ game.teamFunds }}</span>
    </div>
    <div class="res-item">
      <el-icon><Aim /></el-icon>
      <span>P1体力: {{ game.player1Stamina }}</span>
    </div>
    <div class="res-item">
      <el-icon><Aim /></el-icon>
      <span>P2体力: {{ game.player2Stamina }}</span>
    </div>
    <div class="bully-hp-bar">
      <div class="hp-fill" :style="{ width: (game.bullyHP / game.maxBullyHP * 100) + '%' }" />
      <span class="hp-text">霸凌者 HP: {{ game.bullyHP }}/{{ game.maxBullyHP }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Coin, Aim } from '@element-plus/icons-vue'
import { useGameStore } from '@/store/game'
const game = useGameStore()
</script>

<style scoped>
.resource-bar {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-2) var(--space-5);
  background: var(--color-surface-02);
  color: var(--color-text-primary);
  font-size: var(--text-md);
  height: var(--header-height);
}
.res-item {
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: var(--space-1);
}
.bully-hp-bar {
  position: relative;
  flex: 1;
  height: 24px;
  background: var(--color-surface-03);
  border-radius: var(--radius-full);
  overflow: hidden;
  min-width: 200px;
}
.hp-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-success), var(--color-warning), var(--color-danger));
  transition: width var(--transition-slow);
  border-radius: var(--radius-full);
}
.hp-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}
</style>
