<template>
  <div class="announcement-bar">
    <button
      class="announcement-main"
      type="button"
      @click="openCurrent"
      @mouseenter="paused = true"
      @mouseleave="paused = false"
    >
      <span class="announcement-badge">公告</span>
      <div class="announcement-viewport">
        <transition name="notice-slide" mode="out-in">
          <p :key="current.id" class="announcement-title">{{ current.title }}</p>
        </transition>
      </div>
      <span class="announcement-hint">点击查看全文</span>
    </button>
    <div class="announcement-dots" aria-hidden="true">
      <button
        v-for="(item, i) in notices"
        :key="item.id"
        type="button"
        class="dot"
        :class="{ active: i === index }"
        :aria-label="item.title"
        @click.stop="go(i)"
      />
    </div>
    <button class="rules-link" type="button" @click="$router.push('/rules')">规则说明</button>
  </div>

  <Teleport to="body">
    <div v-if="dialogVisible" class="notice-overlay" @click.self="closeDialog">
      <div class="notice-modal" role="dialog" aria-modal="true">
        <header class="notice-header">
          <span class="notice-seal">告示</span>
          <h2>{{ activeNotice.title }}</h2>
          <p class="notice-date">{{ activeNotice.date }}</p>
          <button class="notice-close" type="button" aria-label="关闭" @click="closeDialog">×</button>
        </header>
        <div class="notice-body">{{ activeNotice.content }}</div>
        <footer class="notice-footer">
          <button type="button" class="nav-btn" :disabled="index <= 0" @click="go(index - 1)">上一则</button>
          <span class="notice-index">{{ index + 1 }} / {{ notices.length }}</span>
          <button type="button" class="nav-btn" :disabled="index >= notices.length - 1" @click="go(index + 1)">下一则</button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { visibleAnnouncements, type Announcement } from '@/data/announcements'

const notices = visibleAnnouncements

const index = ref(0)
const paused = ref(false)
const dialogVisible = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const current = computed(() => notices[index.value] || notices[0])
const activeNotice = computed<Announcement>(() => notices[index.value] || notices[0])

function go(i: number) {
  if (i < 0 || i >= notices.length) return
  index.value = i
}

function openCurrent() {
  dialogVisible.value = true
  paused.value = true
}

function closeDialog() {
  dialogVisible.value = false
  paused.value = false
}

function tick() {
  if (paused.value || dialogVisible.value || notices.length < 2) return
  index.value = (index.value + 1) % notices.length
}

onMounted(() => {
  timer = setInterval(tick, 4500)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.announcement-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 46px;
  padding: 6px 16px 6px 12px;
  background: linear-gradient(180deg, #f6ead2 0%, #ead7b4 100%);
  border-top: 2px solid #6b4a28;
  box-shadow: 0 -4px 16px rgba(40, 24, 8, 0.28), inset 0 1px 0 rgba(255, 248, 230, 0.7);
}

.announcement-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  padding: 0;
}

.announcement-badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border: 1px solid #8b6914;
  border-radius: 999px;
  background: rgba(139, 105, 20, 0.12);
  color: #5d3a1a;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.announcement-viewport {
  flex: 1;
  min-width: 0;
  height: 22px;
  overflow: hidden;
  position: relative;
}

.announcement-title {
  margin: 0;
  color: #3e2723;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.announcement-hint {
  flex-shrink: 0;
  color: #8b6914;
  font-size: 12px;
}

.announcement-dots {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.dot {
  width: 7px;
  height: 7px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(93, 58, 26, 0.28);
  cursor: pointer;
}

.dot.active {
  background: #5d3a1a;
}

.rules-link {
  flex-shrink: 0;
  border: 1px solid #8b6914;
  border-radius: 6px;
  background: rgba(255, 248, 230, 0.55);
  color: #4a3520;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 12px;
  cursor: pointer;
}

.rules-link:hover,
.announcement-main:hover .announcement-title {
  color: #2a1a10;
}

.notice-slide-enter-active,
.notice-slide-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.notice-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.notice-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>

<style>
.notice-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 12, 6, 0.62);
  backdrop-filter: blur(4px);
}

.notice-modal {
  position: relative;
  width: min(560px, calc(100vw - 40px));
  max-height: min(72vh, 640px);
  display: flex;
  flex-direction: column;
  padding: 28px 32px 20px;
  background: linear-gradient(180deg, #f7ecd6 0%, #e8d4b0 100%);
  border: 2px solid #6b4a28;
  border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 248, 230, 0.8);
  color: #3e2723;
}

.notice-header h2 {
  margin: 8px 0 6px;
  font-size: 22px;
  line-height: 1.35;
  color: #3a1f0d;
}

.notice-seal {
  display: inline-block;
  padding: 1px 10px;
  border: 1px solid #8b6914;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: #5d3a1a;
}

.notice-date {
  margin: 0;
  color: #8b6914;
  font-size: 13px;
}

.notice-close {
  position: absolute;
  top: 12px;
  right: 14px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #5d3a1a;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
}

.notice-body {
  margin: 16px 0;
  overflow-y: auto;
  white-space: pre-wrap;
  line-height: 1.75;
  font-size: 15px;
  color: #4a3520;
}

.notice-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(139, 105, 20, 0.28);
}

.notice-index {
  color: #8b6914;
  font-size: 13px;
}

.nav-btn {
  border: 1px solid #8b6914;
  border-radius: 6px;
  background: rgba(255, 248, 230, 0.7);
  color: #4a3520;
  padding: 6px 14px;
  cursor: pointer;
}

.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
