<template>
  <div class="page">
    <BackButton to="/game-hall" text="返回大厅" />
    <h2>积分</h2>
    <div class="points-display">
      <span class="amount">{{ user.points }}</span>
      <span class="label">当前积分</span>
    </div>
    <p class="hint">通过完成对局获取积分，胜利可获得满意度对应积分 + 额外50分</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import BackButton from '@/components/BackButton.vue'
const user = useUserStore()
onMounted(() => user.loadPoints())
</script>

<style scoped>
.page { position: relative; padding: var(--space-10); color: var(--color-text-primary); text-align: center; }
h2 { margin-bottom: var(--space-6); font-size: var(--text-3xl); font-weight: var(--weight-semibold); }
.points-display { margin-bottom: var(--space-5); }
.amount { font-size: var(--text-6xl); font-weight: var(--weight-bold); color: var(--color-accent); display: block; line-height: var(--leading-tight); }
.label { font-size: var(--text-md); color: var(--color-text-tertiary); }
.hint { color: var(--color-text-secondary); font-size: var(--text-base); margin-bottom: var(--space-6); max-width: 480px; margin-left: auto; margin-right: auto; }
</style>
