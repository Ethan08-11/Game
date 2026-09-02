<template>
  <div
    class="card-item"
    :class="[`card-${type}`, { disabled }]"
    @click="!disabled && $emit('play')"
  >
    <div class="card-art" :style="cardArtStyle"></div>
    <div class="card-name-ribbon" aria-hidden="true"></div>
    <div class="card-desc-plate" aria-hidden="true"></div>
    <span class="card-cost">{{ cost }}</span>
    <span class="card-dept">{{ displayDept }}</span>
    <strong class="card-name">{{ name }}</strong>
    <div ref="descBoxRef" class="card-desc-box">
      <p
        ref="descRef"
        class="card-desc"
        :style="{ fontSize: `${descFontPx}px`, lineHeight: DESC_LINE_HEIGHT }"
      >{{ description }}</p>
    </div>
    <div class="card-bottom">
      <div class="card-tag" :class="`tag-${type}`">
        <el-icon class="tag-icon"><component :is="typeIcon" /></el-icon>
        <span>{{ typeLabel }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Switch, Aim, Download, Delete, MagicStick, Lightning, FirstAidKit, CirclePlus } from '@element-plus/icons-vue'
import { getImageUrl } from '@/utils/imageUrl'
import { displayCardDept } from '@/utils/cardDept'
import fallbackBg from '@/assets/card-background1.webp'

const DESC_MAX_PX = 16
const DESC_MIN_PX = 9
const DESC_LINE_HEIGHT = 1.2

const props = defineProps<{
  name: string
  dept: string
  cost: number
  type: 'attack' | 'defend' | 'draw' | 'consume' | 'support' | 'attack_defend' | 'special' | 'trigger' | 'heal' | 'buff' | string
  description: string
  damage: number
  shield: number
  disabled?: boolean
  imageUrl?: string | null
}>()

defineEmits<{ play: [] }>()

const descBoxRef = ref<HTMLElement | null>(null)
const descRef = ref<HTMLElement | null>(null)
const descFontPx = ref(DESC_MAX_PX)
let resizeObserver: ResizeObserver | null = null

const cardArtStyle = computed(() => {
  const img = getImageUrl(props.imageUrl) || fallbackBg
  return { backgroundImage: `url('${img}')` }
})

const displayDept = computed(() => displayCardDept(props.dept, props.imageUrl, props.dept))

function fitsInBox(box: HTMLElement, text: HTMLElement) {
  return text.scrollHeight <= box.clientHeight + 0.5
    && text.scrollWidth <= box.clientWidth + 0.5
}

async function fitDescription() {
  await nextTick()
  const box = descBoxRef.value
  const text = descRef.value
  if (!box || !text || !props.description) {
    descFontPx.value = DESC_MAX_PX
    return
  }

  let size = DESC_MAX_PX
  descFontPx.value = size
  await nextTick()

  while (size > DESC_MIN_PX && !fitsInBox(box, text)) {
    size -= 0.5
    descFontPx.value = size
    await nextTick()
  }
}

const typeIcon = computed(() => {
  const icons: Record<string, any> = {
    defend: Switch,
    attack: Aim,
    draw: Download,
    consume: Delete,
    support: CirclePlus,
    attack_defend: Aim,
    special: MagicStick,
    trigger: Lightning,
    heal: FirstAidKit,
    buff: CirclePlus,
  }
  return icons[props.type] || MagicStick
})

const typeLabel = computed(() => {
  const labels: Record<string, string> = {
    defend: '防御',
    attack: '攻击',
    draw: '过牌',
    consume: '消耗',
    support: '辅助',
    attack_defend: '攻防',
    special: '特殊',
    trigger: '触发',
    heal: '治疗',
    buff: '增益',
  }
  return labels[props.type] || props.type || '特殊'
})

onMounted(() => {
  fitDescription()
  if (typeof ResizeObserver !== 'undefined' && descBoxRef.value) {
    resizeObserver = new ResizeObserver(() => {
      fitDescription()
    })
    resizeObserver.observe(descBoxRef.value)
  }
})

watch(() => [props.description, props.imageUrl, props.name], () => {
  fitDescription()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
})
</script>

<style scoped>
.card-item {
  width: var(--card-width);
  aspect-ratio: 441 / 800;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-subtle);
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  user-select: none;
  position: relative;
  overflow: hidden;
  background: #e6d4a8;
}
.card-item:hover:not(.disabled) {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-accent);
}
.card-item.disabled {
  opacity: 0.38;
  cursor: not-allowed;
}

.card-art {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-position: center;
  background-size: 100% 100%;
  background-repeat: no-repeat;
  pointer-events: none;
  -webkit-mask-image: linear-gradient(
    to bottom,
    #000 0%,
    #000 64%,
    rgba(0, 0, 0, 0.55) 68%,
    transparent 72.5%
  );
  mask-image: linear-gradient(
    to bottom,
    #000 0%,
    #000 64%,
    rgba(0, 0, 0, 0.55) 68%,
    transparent 72.5%
  );
}

.card-name-ribbon {
  position: absolute;
  top: 68.8%;
  left: 16%;
  right: 16%;
  height: 6.2%;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(180deg, #f3e6c8 0%, #e6d3a8 52%, #d7c08c 100%);
  border: 1px solid rgba(122, 86, 40, 0.35);
  border-radius: 40% / 70%;
  box-shadow: 0 1px 2px rgba(40, 24, 8, 0.22);
}

.card-desc-plate {
  position: absolute;
  top: 74.2%;
  left: 10.5%;
  right: 10.5%;
  bottom: 7.2%;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(180deg, #e2cfab 0%, #d4bc90 100%);
  border: 1px solid rgba(90, 62, 36, 0.32);
  border-radius: 6px;
  box-shadow: inset 0 0 0 1px rgba(255, 244, 214, 0.28);
}

.card-cost {
  position: absolute;
  top: 0;
  left: 8px;
  font-size: calc(var(--text-3xl) * 2);
  font-weight: var(--weight-bold);
  color: #3E2723;
  z-index: 2;
}

.card-dept {
  position: absolute;
  top: 12px;
  left: 52%;
  transform: translateX(-50%);
  max-width: 46%;
  font-size: calc(var(--text-2xs) * 2);
  font-weight: var(--weight-semibold);
  color: #3E2723;
  text-align: center;
  line-height: 1.1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  z-index: 2;
}

.card-name {
  position: absolute;
  top: 71.9%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 62%;
  font-size: calc(var(--text-base) * 1.55);
  line-height: 1;
  color: #3E2723;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  z-index: 2;
  pointer-events: none;
  text-shadow: 0 0 4px rgba(243, 230, 200, 0.9);
}

.card-desc-box {
  position: absolute;
  top: 75.6%;
  bottom: 10.5%;
  left: 13%;
  right: 13%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  z-index: 2;
  pointer-events: none;
  box-sizing: border-box;
  padding: 2px 4px;
}

.card-desc {
  margin: 0;
  width: 100%;
  color: #5b4a3a;
  text-align: center;
  overflow: hidden;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.card-bottom {
  position: absolute;
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0;
  width: 85%;
  z-index: 2;
}

.card-attack       { border-left: 3px solid var(--card-attack); }
.card-defend       { border-left: 3px solid var(--card-defend); }
.card-draw         { border-left: 3px solid var(--card-draw); }
.card-consume      { border-left: 3px solid var(--card-consume); }
.card-support      { border-left: 3px solid var(--card-support); }
.card-attack_defend{ border-left: 3px solid #c96b2b; }
.card-special      { border-left: 3px solid #9b59b6; }
.card-trigger      { border-left: 3px solid #e67e22; }
.card-heal         { border-left: 3px solid #27ae60; }
.card-buff         { border-left: 3px solid #2980b9; }

.card-tag {
  font-size: calc(var(--text-2xs) * 1.6);
  padding: 1px var(--space-2);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 2px;
  background: rgba(0, 0, 0, 0.34);
  color: #3E2723;
}
.tag-icon { width: 14px; height: 14px; }
.tag-attack       { background: rgba(184, 55, 55, 0.72); color: #3E2723; }
.tag-defend       { background: rgba(53, 97, 176, 0.72); color: #3E2723; }
.tag-draw         { background: rgba(187, 145, 42, 0.72); color: #3E2723; }
.tag-consume      { background: rgba(71, 71, 71, 0.72); color: #3E2723; }
.tag-support      { background: rgba(22, 160, 133, 0.72); color: #3E2723; }
.tag-attack_defend{ background: rgba(201, 107, 43, 0.72); color: #3E2723; }
.tag-special      { background: rgba(155, 89, 182, 0.72); color: #3E2723; }
.tag-heal         { background: rgba(39, 174, 96, 0.72); color: #3E2723; }
.tag-buff         { background: rgba(41, 128, 185, 0.72); color: #3E2723; }
.tag-trigger      { background: rgba(230, 126, 34, 0.72); color: #3E2723; }
</style>
