<template>
  <div class="employer-card">
    <div v-if="game.employerTrait" class="employer-trait" :class="traitClass">
      {{ game.employerTrait.name }}
      <el-tooltip :content="game.employerTrait.description" placement="top">
        <el-icon :size="12"><QuestionFilled /></el-icon>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'
import { useGameStore } from '@/store/game'

const game = useGameStore()

const traitClass = computed(() => {
  if (!game.employerTrait) return ''
  return game.employerTrait.helpChance >= 0.6 ? 'trait-helpful' : 'trait-harmful'
})
</script>

<style scoped>
.employer-card {
  text-align: center;
  padding: var(--space-4);
}
.employer-trait {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  padding: 2px var(--space-3);
  border-radius: var(--radius-full);
}
.trait-helpful { background: var(--color-success-muted); color: var(--color-success); }
.trait-harmful { background: var(--color-danger-muted); color: var(--color-danger); }
</style>
