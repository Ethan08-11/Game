<template>
  <div class="quests-page" :style="{ '--quests-bg': bgUrl ? `url(${bgUrl})` : '' }">
    <BackButton to="/game-hall" text="返回大厅" />

    <div class="quest-panel" :style="{ '--panel-bg': `url(${panelBg})`, '--list-bg': `url(${listBg})` }">
      <div v-if="loading" class="state-msg">加载中...</div>
      <div v-else-if="error" class="state-msg state-error">{{ error }}</div>
      <div v-else-if="taskGroups.length === 0" class="state-msg">暂无任务</div>

      <div v-else class="task-list-area">
        <template v-for="(entry, idx) in flatTaskList" :key="entry.task.id">
          <div v-if="entry.sectionStart" class="type-section">
            <div class="type-header">
              <span
                class="type-tag"
                :class="'tag-' + entry.type"
                :style="tagBgStyle(entry.type)"
              >{{ typeLabel(entry.type) }}</span>
              <span class="type-count">{{ groupTaskCount(entry.type) }} 个任务</span>
            </div>
          </div>
          <div class="card" :style="{ animationDelay: `${idx * 0.08}s` }">
            <div class="card-info">
              <div class="task-name">{{ entry.task.taskName }}</div>
              <div class="task-desc">{{ entry.task.description }}</div>
              <div class="task-target">目标: {{ entry.task.targetCount }}</div>
            </div>
            <div class="card-reward">
              <span class="reward-icon">{{ rewardIcon(entry.task.rewardType) }}</span>
              <span class="reward-text">{{ rewardText(entry.task) }}</span>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 成长标签调节器 -->
    <Teleport to="body">
      <div v-if="showGrowthAdjuster" class="tag-adjuster-panel" style="right: 12px; bottom: 108px;">
        <div class="tag-adjuster-header">
          <span>成长标签调节</span>
          <button class="tag-adjuster-close" @click="showGrowthAdjuster = false">✕</button>
        </div>
        <div class="tag-adjuster-body">
          <label>大小 <span>{{ growthSize }}px</span></label>
          <input type="range" v-model.number="growthSize" min="20" max="200" />
          <label>水平偏移 <span>{{ growthX }}px</span></label>
          <input type="range" v-model.number="growthX" min="-200" max="200" />
          <label>垂直偏移 <span>{{ growthY }}px</span></label>
          <input type="range" v-model.number="growthY" min="-100" max="100" />
        </div>
      </div>
      <button class="tag-adjuster-toggle" style="right: 12px; bottom: 56px;" @click="showGrowthAdjuster = !showGrowthAdjuster">⚙G</button>

      <!-- 事件标签调节器 -->
      <div v-if="showEventAdjuster" class="tag-adjuster-panel" style="right: 56px; bottom: 108px;">
        <div class="tag-adjuster-header">
          <span>事件标签调节</span>
          <button class="tag-adjuster-close" @click="showEventAdjuster = false">✕</button>
        </div>
        <div class="tag-adjuster-body">
          <label>大小 <span>{{ eventSize }}px</span></label>
          <input type="range" v-model.number="eventSize" min="20" max="200" />
          <label>水平偏移 <span>{{ eventX }}px</span></label>
          <input type="range" v-model.number="eventX" min="-200" max="200" />
          <label>垂直偏移 <span>{{ eventY }}px</span></label>
          <input type="range" v-model.number="eventY" min="-100" max="100" />
        </div>
      </div>
      <button class="tag-adjuster-toggle" style="right: 56px; bottom: 56px;" @click="showEventAdjuster = !showEventAdjuster">⚙E</button>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { fetchTasks } from '@/api'
import type { ApiTask } from '@/api'
import BackButton from '@/components/BackButton.vue'
import hallBgDay from '@/assets/hall-bg2.png'
import hallBgNight from '@/assets/hall-bg.png'
import panelBg from '@/assets/quest-panel-bg.png'
import listBg from '@/assets/quest-list-bg.png'
import dailyTagBg from '@/assets/tag-daily-bg.png'
import growthTagBg from '@/assets/tag-growth-bg.png'
import eventTagBg from '@/assets/tag-event-bg.png'

interface TaskGroup {
  type: string
  tasks: ApiTask[]
}

const tasks = ref<ApiTask[]>([])
const loading = ref(true)
const error = ref('')
const bgUrl = ref('')

const typeOrder = ['daily', 'growth', 'event']

const taskGroups = computed<TaskGroup[]>(() => {
  const map = new Map<string, ApiTask[]>()
  for (const t of tasks.value) {
    const list = map.get(t.taskType) || []
    list.push(t)
    map.set(t.taskType, list)
  }
  return typeOrder
    .filter(t => map.has(t))
    .map(type => ({ type, tasks: map.get(type)! }))
})

interface FlatTaskEntry {
  task: ApiTask
  type: string
  sectionStart: boolean
}

const flatTaskList = computed<FlatTaskEntry[]>(() => {
  const result: FlatTaskEntry[] = []
  for (const group of taskGroups.value) {
    for (let i = 0; i < group.tasks.length; i++) {
      result.push({
        task: group.tasks[i],
        type: group.type,
        sectionStart: i === 0,
      })
    }
  }
  return result
})

function groupTaskCount(type: string): number {
  return taskGroups.value.find(g => g.type === type)?.tasks.length ?? 0
}

onMounted(() => {
  const hour = new Date().getHours()
  bgUrl.value = hour >= 6 && hour < 18 ? hallBgDay : hallBgNight

  fetchTasks()
    .then(data => { tasks.value = data })
    .catch(e => { error.value = e.message || '加载任务失败' })
    .finally(() => { loading.value = false })
})

// 成长标签调节器
const showGrowthAdjuster = ref(false)
const growthSize = ref(80)
const growthX = ref(0)
const growthY = ref(0)
// 事件标签调节器
const showEventAdjuster = ref(false)
const eventSize = ref(80)
const eventX = ref(0)
const eventY = ref(0)

const tagBgMap: Record<string, string> = {
  daily: dailyTagBg,
  growth: growthTagBg,
  event: eventTagBg,
}
function tagBgStyle(type: string) {
  const bg = tagBgMap[type]
  if (!bg) return {}
  const style: Record<string, string> = {
    background: `url(${bg}) center/contain no-repeat`,
    color: '#4a3520',
  }
  if (type === 'growth') {
    style.width = growthSize.value + 'px'
    style.height = growthSize.value + 'px'
    style.transform = `translate(${growthX.value}px, ${growthY.value}px)`
  } else if (type === 'event') {
    style.width = eventSize.value + 'px'
    style.height = eventSize.value + 'px'
    style.transform = `translate(${eventX.value}px, ${eventY.value}px)`
  }
  return style
}

function typeLabel(type: string): string {
  const labels: Record<string, string> = {
    daily: '每日',
    growth: '成长',
    event: '事件',
  }
  return labels[type] || type
}

function parseReward(rewardValue: string): Record<string, any> {
  try { return JSON.parse(rewardValue) } catch { return {} }
}

function rewardIcon(rewardType: string): string {
  const icons: Record<string, string> = {
    money: '$',
    exp: '★',
    item: '◇',
  }
  return icons[rewardType] || '?'
}

function rewardText(task: ApiTask): string {
  const val = parseReward(task.rewardValue)
  switch (task.rewardType) {
    case 'money':
      return `+${val.amount || 0} 金币`
    case 'exp':
      return `+${val.amount || 0} 经验`
    case 'item':
      return `${val.itemCode || ''} x${val.count || 0}`
    default:
      return task.rewardValue
  }
}
</script>

<style scoped>
.quests-page {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  isolation: isolate;
}
.quests-page::before {
  content: '';
  position: absolute;
  inset: -20px;
  z-index: -1;
  background: var(--quests-bg) center/cover no-repeat;
  filter: blur(8px);
}

.quest-panel {
  position: relative;
  width: 100%;
  height: 100%;
  background: var(--panel-bg) center/contain no-repeat;
  padding: var(--space-10) var(--space-12);
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #4a3520;
}

.state-msg {
  text-align: center;
  padding: var(--space-16) 0;
  font-size: var(--text-lg);
  color: #4a3520;
}
.state-error { color: #4a3520; }

.task-list-area {
  width: 100%;
  max-width: 640px;
  max-height: 65vh;
  margin-top: 7em;
  overflow-y: auto;
  min-height: 0;
}

.type-section {
  width: 100%;
  margin-bottom: var(--space-3);
}
.type-header {
  display: flex; align-items: center; gap: var(--space-2);
  margin-bottom: var(--space-6);
}
.type-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-2) var(--space-3); border-radius: var(--radius-sm);
  font-size: var(--text-sm); font-weight: var(--weight-semibold);
  min-width: 80px;
  min-height: 40px;
}
.tag-daily { color: #4a3520; }
.tag-growth { color: #4a3520; }
.tag-event { color: #4a3520; }
.type-count {
  font-size: var(--text-xs); color: #4a3520;
  margin-left: auto;
}

.card {
  display: flex; align-items: center; gap: var(--space-4);
  padding: 32px var(--space-5); margin-bottom: var(--space-1);
  background: var(--list-bg) center/100% 100% no-repeat;
  border-radius: 0;
  border: none;
  zoom: 0.75;
  overflow: visible;
  transition: filter var(--transition-fast);
  animation: bounceIn 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
.card:hover { filter: brightness(1.1); }

@keyframes bounceIn {
  0% { opacity: 0; transform: translateY(24px) scale(0.92); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

.card-info { flex: 1; min-width: 0; margin-left: 8em; }
.task-name { font-weight: var(--weight-semibold); }
.task-desc { font-size: var(--text-sm); color: #4a3520; }
.task-target { font-size: var(--text-xs); color: #4a3520; margin-top: 2px; }

.card-reward {
  display: flex; align-items: center; gap: var(--space-1);
  color: #4a3520; flex-shrink: 0; margin-right: 3em;
}
.reward-icon { font-size: var(--text-lg); font-weight: var(--weight-bold); }
.reward-text { font-size: var(--text-sm); }
</style>

<style>
.tag-adjuster-toggle {
  position: fixed;
  z-index: 9998;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tag-adjuster-panel {
  position: fixed;
  z-index: 9998;
  width: 220px;
  background: rgba(20, 20, 20, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 12px;
  color: #ccc;
  font-size: 13px;
}
.tag-adjuster-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: bold;
  color: #fff;
}
.tag-adjuster-close {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 14px;
}
.tag-adjuster-body label {
  display: flex;
  justify-content: space-between;
  margin: 6px 0 2px;
}
.tag-adjuster-body label span {
  color: var(--color-accent, #c4a962);
}
.tag-adjuster-body input[type="range"] {
  width: 100%;
  accent-color: var(--color-accent, #c4a962);
}
</style>
