<template>
  <canvas ref="trailCanvas" class="magic-trail-canvas"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

interface Particle {
  x: number; y: number
  vx: number; vy: number
  life: number; maxLife: number
  size: number
  hue: number
}

const trailCanvas = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let particles: Particle[] = []
let animId = 0
let mouseX = -1000; let mouseY = -1000
let prevX = -1000; let prevY = -1000
let lastSpawnTime = 0
let canvasW = 0; let canvasH = 0

function resizeCanvas() {
  if (!trailCanvas.value) return
  canvasW = window.innerWidth
  canvasH = window.innerHeight
  trailCanvas.value.width = canvasW
  trailCanvas.value.height = canvasH
}

function spawnParticles(x: number, y: number, count: number) {
  for (let i = 0; i < count; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = 15 + Math.random() * 55
    particles.push({
      x, y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      life: 1,
      maxLife: 0.7 + Math.random() * 1.8,
      size: 1.2 + Math.random() * 3.5,
      hue: 35 + Math.random() * 25,
    })
  }
}

function onMouseMove(e: MouseEvent) {
  prevX = mouseX; prevY = mouseY
  mouseX = e.clientX; mouseY = e.clientY
}

function animate(timestamp: number) {
  if (!ctx) { animId = requestAnimationFrame(animate); return }

  ctx.clearRect(0, 0, canvasW, canvasH)

  if (mouseX > 0 && mouseY > 0 && (mouseX !== prevX || mouseY !== prevY)) {
    if (timestamp - lastSpawnTime > 16) {
      const dx = mouseX - prevX; const dy = mouseY - prevY
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 2) {
        const steps = Math.min(Math.floor(dist / 6), 8)
        for (let s = 1; s <= steps; s++) {
          const t = s / steps
          spawnParticles(prevX + dx * t, prevY + dy * t, 2)
        }
      } else {
        spawnParticles(mouseX, mouseY, 2)
      }
      lastSpawnTime = timestamp
    }
  }

  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i]
    p.life -= 0.016 / p.maxLife
    if (p.life <= 0) { particles.splice(i, 1); continue }
    p.x += p.vx * 0.016
    p.y += p.vy * 0.016
    p.vx *= 0.97
    p.vy *= 0.97

    const alpha = p.life * 0.85
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size * p.life, 0, Math.PI * 2)
    ctx.fillStyle = `hsla(${p.hue}, 100%, ${65 + p.life * 20}%, ${alpha})`
    ctx.fill()

    if (p.size > 2.2) {
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size * p.life * 2.8, 0, Math.PI * 2)
      ctx.fillStyle = `hsla(${p.hue}, 90%, 60%, ${alpha * 0.2})`
      ctx.fill()
    }
  }

  // 光标光圈
  if (mouseX > 0 && mouseY > 0) {
    const glow = ctx.createRadialGradient(mouseX, mouseY, 0, mouseX, mouseY, 38)
    glow.addColorStop(0, 'rgba(255, 230, 140, 0)')
    glow.addColorStop(0.35, 'rgba(255, 210, 80, 0.18)')
    glow.addColorStop(0.65, 'rgba(255, 180, 40, 0.25)')
    glow.addColorStop(0.85, 'rgba(255, 150, 20, 0.08)')
    glow.addColorStop(1, 'rgba(255, 120, 0, 0)')
    ctx.beginPath()
    ctx.arc(mouseX, mouseY, 38, 0, Math.PI * 2)
    ctx.fillStyle = glow
    ctx.fill()

    ctx.beginPath()
    ctx.arc(mouseX, mouseY, 4, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255, 240, 180, 0.85)'
    ctx.fill()
    ctx.beginPath()
    ctx.arc(mouseX, mouseY, 8, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255, 220, 100, 0.35)'
    ctx.fill()
  }

  animId = requestAnimationFrame(animate)
}

onMounted(() => {
  resizeCanvas()
  window.addEventListener('resize', resizeCanvas)
  document.addEventListener('mousemove', onMouseMove)
  ctx = trailCanvas.value!.getContext('2d')!
  animId = requestAnimationFrame(animate)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resizeCanvas)
  document.removeEventListener('mousemove', onMouseMove)
})
</script>

<style scoped>
.magic-trail-canvas {
  position: fixed;
  inset: 0;
  z-index: 20;
  pointer-events: none;
}
</style>
