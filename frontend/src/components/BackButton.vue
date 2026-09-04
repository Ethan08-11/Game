<template>
  <button class="back-btn" @click="goBack">
    <span class="back-btn-icon">
      <slot name="icon">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </slot>
    </span>
    <span v-if="text || $slots.default" class="back-btn-text">
      <slot>{{ text }}</slot>
    </span>
  </button>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = withDefaults(defineProps<{
  to: string
  text?: string
}>(), {
  text: '返回',
})

const emit = defineEmits<{
  click: []
}>()

const router = useRouter()

function goBack() {
  emit('click')
  if (props.to) {
    router.push(props.to)
  }
}
</script>

<style scoped>
.back-btn {
  position: absolute;
  top: var(--space-4);
  left: var(--space-4);
  z-index: 100;
  display: inline-flex;
  align-items: center;
  width: max-content;
  max-width: max-content;
  flex: none;
  align-self: flex-start;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4) var(--space-2) var(--space-3);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  background: var(--color-surface-02);
  color: var(--color-text-primary);
  font-size: var(--text-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  backdrop-filter: blur(6px);
}
.back-btn:hover {
  background: var(--color-surface-hover);
  border-color: var(--color-accent);
}
.back-btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.back-btn-text {
  white-space: nowrap;
}
</style>
