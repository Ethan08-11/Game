<template>
  <div class="countdown" :class="{ warning: remaining < 30 }">
    {{ formatTime }}
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{ seconds?: number }>(), { seconds: 300 })
const emit = defineEmits<{ timeout: [] }>()

const remaining = ref(props.seconds)
let timer: ReturnType<typeof setInterval> | null = null

const formatTime = computed(() => {
  const m = Math.floor(remaining.value / 60)
  const s = remaining.value % 60
  return `${m}:${String(s).padStart(2, '0')}`
})

function start() {
  timer = setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) {
      stop()
      emit('timeout')
    }
  }, 1000)
}

function stop() {
  if (timer) { clearInterval(timer); timer = null }
}

defineExpose({ start, stop, remaining })

onUnmounted(() => stop())
</script>

<style scoped>
.countdown {
  font-size: var(--text-3xl);
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
}
.countdown.warning {
  color: var(--color-danger);
  animation: pulse 1s infinite;
}
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
