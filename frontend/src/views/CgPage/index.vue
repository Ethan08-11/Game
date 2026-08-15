<template>
  <div class="cg-page">
    <video
      v-if="!videoError"
      ref="videoRef"
      class="cg-video"
      :src="openingVideo"
      autoplay
      muted
      loop
      playsinline
      preload="auto"
      @error="onVideoError"
    />
    <div v-else class="cg-bg">
      <h1>这单我们护了！！！！</h1>
      <p>CG视频待添加</p>
    </div>
    <button class="start-btn" @click="enter">开始游戏</button>
    <button class="enter-btn" @click="showPopup">进入游戏</button>

    <div v-show="showStory" class="story-overlay" @click="handleOverlayClick">
      <div ref="storyPopup" class="story-popup" @click.stop>
        <img :src="storyStep === 3 ? storyImg3 : storyStep === 2 ? storyImg2 : storyImg" class="story-image" alt="剧情" />
        <p class="story-text">扫一扫跳过广告哦（广告位招租）</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useCommonStore } from '@/store/common'
import gsap from 'gsap'
import openingVideo from '@/assets/cg/opening.mp4'
import storyImg from '@/assets/cg/story.webp'
import storyImg2 from '@/assets/cg/story2.webp'
import storyImg3 from '@/assets/cg/story3.webp'

const router = useRouter()
const common = useCommonStore()

const videoError = ref(false)
const videoRef = ref<HTMLVideoElement | null>(null)
const showStory = ref(false)
const storyStep = ref(0)
const storyPopup = ref<HTMLElement | null>(null)

onMounted(async () => {
  const video = videoRef.value
  if (!video) return
  try {
    // 浏览器通常要求静音才允许自动播放；先静音播放，用户点击后再开声
    video.muted = true
    await video.play()
  } catch {
    // 自动播放被拦时保留画面第一帧，等待用户点击按钮
  }
})

function onVideoError() {
  videoError.value = true
}

function enter() {
  common.cgPlaying = false
  const video = videoRef.value
  if (video) {
    video.muted = false
    video.pause()
  }
  router.push('/login')
}

function animateIn(el: HTMLElement) {
  gsap.killTweensOf(el)
  const vw = window.innerWidth
  const vh = window.innerHeight
  const tl = gsap.timeline()
  tl.fromTo(el, {
    top: vh,
    left: vw,
    xPercent: 0,
    yPercent: 0,
    scale: 0,
    rotation: 0,
  }, {
    top: '50%',
    left: '50%',
    xPercent: -50,
    yPercent: -50,
    scale: 1,
    rotation: 2160,
    duration: 1.5,
    ease: 'power2.inOut',
  })
  tl.to(el, { x: -12, duration: 0.08, ease: 'power2.out' })
  tl.to(el, { x: 12, duration: 0.08, ease: 'power2.out' })
  tl.to(el, { x: -6, duration: 0.06, ease: 'power2.out' })
  tl.to(el, { x: 6, duration: 0.06, ease: 'power2.out' })
  tl.to(el, { x: 0, duration: 0.04, ease: 'power2.out' })
}

function animateOut(el: HTMLElement, onComplete: () => void) {
  gsap.killTweensOf(el)
  const vw = window.innerWidth
  const vh = window.innerHeight
  const tl = gsap.timeline()
  tl.set(el, { x: 0 })
  tl.to(el, {
    top: vh,
    left: vw,
    xPercent: 0,
    yPercent: 0,
    scale: 0,
    rotation: 0,
    duration: 1,
    ease: 'power2.inOut',
    onComplete,
  })
}

function showPopup() {
  const video = videoRef.value
  if (video) video.muted = false
  showStory.value = true
  storyStep.value = 1
  nextTick(() => {
    if (storyPopup.value) animateIn(storyPopup.value)
  })
}

function handleOverlayClick() {
  const el = storyPopup.value
  if (!el) {
    showStory.value = false
    return
  }

  if (storyStep.value === 1) {
    animateOut(el, () => {
      storyStep.value = 2
      nextTick(() => {
        if (storyPopup.value) animateIn(storyPopup.value)
      })
    })
  } else if (storyStep.value === 2) {
    animateOut(el, () => {
      storyStep.value = 3
      nextTick(() => {
        if (storyPopup.value) animateIn(storyPopup.value)
      })
    })
  } else {
    animateOut(el, () => {
      showStory.value = false
    })
  }
}
</script>

<style scoped>
.cg-page {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.cg-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
}
.cg-bg {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-bg-base), var(--color-bg-elevated));
  color: var(--color-text-primary);
}
.cg-bg h1 {
  font-size: var(--text-5xl);
  margin-bottom: var(--space-4);
  font-weight: var(--weight-bold);
  color: var(--color-accent);
}
.cg-bg p {
  font-size: var(--text-lg);
  color: var(--color-text-secondary);
}

.start-btn {
  position: absolute;
  left: var(--space-6);
  bottom: var(--space-6);
  padding: var(--space-3) var(--space-6);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--text-md);
  color: rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(4px);
  transition: all var(--transition-fast);
  z-index: 10;
}
.start-btn:hover {
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.12);
}

.enter-btn {
  position: absolute;
  right: var(--space-6);
  bottom: var(--space-6);
  padding: var(--space-3) var(--space-8);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--text-lg);
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(6px);
  transition: all var(--transition-fast);
  z-index: 10;
}
.enter-btn:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.6);
}

.story-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 1000;
  animation: fadeIn var(--transition-fast) ease;
}

.story-popup {
  position: absolute;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--space-4);
}

.story-image {
  max-width: 90vw;
  max-height: 80vh;
  object-fit: contain;
}

.story-text {
  color: #fff;
  font-size: var(--text-xl);
  margin: 0;
}
</style>
