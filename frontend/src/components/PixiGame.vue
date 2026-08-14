<template>
  <div ref="pixiContainer" class="pixi-game-container" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Application, Container } from 'pixi.js'
import { useGameStore } from '@/store/game'

const pixiContainer = ref<HTMLDivElement>()
const game = useGameStore()
let app: Application | null = null

// 三层容器
let shopLayer: Container
let battleLayer: Container
let overlayLayer: Container

onMounted(() => {
  if (!pixiContainer.value) return
  const bgColor = getComputedStyle(document.documentElement).getPropertyValue('--color-bg-base').trim() || '#0d0d14'
  app = new Application({
    width: pixiContainer.value.clientWidth,
    height: pixiContainer.value.clientHeight,
    backgroundColor: parseInt(bgColor.replace('#', ''), 16),
    antialias: true,
  })
  pixiContainer.value.appendChild(app.view as HTMLCanvasElement)

  shopLayer = new Container()
  battleLayer = new Container()
  overlayLayer = new Container()
  app.stage.addChild(shopLayer, battleLayer, overlayLayer)
})

onUnmounted(() => {
  app?.destroy(true)
})

watch(() => game.bullyHP, (_val) => {
  // 画布内数值变化自动刷新动画
})
</script>

<style scoped>
.pixi-game-container { width: 100%; height: 100%; }
</style>
