<template>
  <span class="medal" :title="kind">
    <svg viewBox="0 0 64 64" aria-hidden="true">
      <defs>
        <radialGradient :id="gid('bg')" cx="35%" cy="30%" r="70%">
          <stop offset="0%" stop-color="#4a3424" />
          <stop offset="100%" stop-color="#23160e" />
        </radialGradient>
        <linearGradient :id="gid('rim')" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#f0d48a" />
          <stop offset="45%" stop-color="#b8893a" />
          <stop offset="100%" stop-color="#6e4a18" />
        </linearGradient>
      </defs>
      <circle cx="32" cy="32" r="30" :fill="`url(#${gid('bg')})`" />
      <circle cx="32" cy="32" r="30" fill="none" :stroke="`url(#${gid('rim')})`" stroke-width="3.2" />
      <circle cx="32" cy="32" r="26.4" fill="none" stroke="#7a5a28" stroke-width="0.7" opacity="0.55" />
      <g fill="none" stroke="#f4efe2" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <g v-if="kind === 'sun'">
          <circle cx="32" cy="30" r="8" fill="#e8c56a" stroke="#e8c56a" />
          <path d="M32 14v4M32 42v4M18 30h4M42 30h4M21 19l3 3M40 37l3 3M21 41l3-3M40 23l3-3" stroke="#e8c56a" />
          <path d="M16 50h32" />
        </g>
        <g v-else-if="kind === 'crown'">
          <path d="M16 42h32v4H16z" fill="#e8c56a" stroke="#e8c56a" />
          <path d="M18 42l6-16 8 10 8-10 6 16" fill="#e8c56a" stroke="#f4efe2" />
          <circle cx="24" cy="24" r="2.2" fill="#f4efe2" stroke="none" />
          <circle cx="32" cy="20" r="2.2" fill="#f4efe2" stroke="none" />
          <circle cx="40" cy="24" r="2.2" fill="#f4efe2" stroke="none" />
        </g>
        <g v-else-if="kind === 'sword'">
          <path d="M32 12v28" stroke-width="3" />
          <path d="M28 14l4-4 4 4" />
          <path d="M22 40h20" stroke="#e8c56a" />
          <path d="M32 40v8" stroke="#e8c56a" />
          <circle cx="32" cy="50" r="2.4" fill="#e8c56a" stroke="#e8c56a" />
        </g>
        <g v-else-if="kind === 'shield'">
          <path d="M32 12l16 6v14c0 10-7 16-16 20-9-4-16-10-16-20V18z" />
          <path d="M32 18v28M22 28h20" stroke="#e8c56a" stroke-width="1.8" />
        </g>
        <g v-else-if="kind === 'castle'">
          <path d="M14 48h36V32H14z" />
          <path d="M14 32v-8h8v8M28 32v-12h8v12M42 32v-8h8v8" />
          <path d="M14 24h8M28 20h8M42 24h8" />
          <path d="M29 48v-10h6v10" fill="#23160e" />
        </g>
        <g v-else-if="kind === 'helms'">
          <path d="M18 40c0-8 5-14 12-14s12 6 12 14" />
          <path d="M18 40h24" />
          <path d="M24 32v8M30 32v8" />
          <path d="M34 42c0-7 5-12 11-12s11 5 11 12" />
          <path d="M34 42h22" />
          <path d="M40 35v7M46 35v7" />
        </g>
        <g v-else-if="kind === 'scales'">
          <path d="M32 14v28" stroke="#e8c56a" />
          <path d="M20 18h24" stroke="#e8c56a" />
          <path d="M20 18l-8 14h16zM44 18l-8 14h16z" />
          <circle cx="32" cy="46" r="3" fill="#e8c56a" stroke="#e8c56a" />
        </g>
        <g v-else-if="kind === 'crate'">
          <path d="M16 24h32v24H16z" />
          <path d="M16 24l16-8 16 8" />
          <path d="M32 16v32M16 36h32" />
        </g>
        <g v-else-if="kind === 'axe'">
          <path d="M22 48L42 16" stroke-width="2.6" stroke="#e8c56a" />
          <path d="M36 12c10 2 14 10 12 16-8-2-16-4-18-10 2-4 4-6 6-6z" fill="#f4efe2" />
        </g>
        <g v-else-if="kind === 'scroll'">
          <path d="M20 18h24v28H20z" />
          <path d="M20 18c-4 0-4 8 0 8M44 46c4 0 4-8 0-8" />
          <path d="M26 28h12M26 34h12M26 40h8" stroke-width="1.7" />
        </g>
        <g v-else-if="kind === 'owl'">
          <circle cx="32" cy="30" r="12" />
          <circle cx="27" cy="28" r="3.2" fill="#e8c56a" stroke="#e8c56a" />
          <circle cx="37" cy="28" r="3.2" fill="#e8c56a" stroke="#e8c56a" />
          <path d="M32 32l-3 5h6z" fill="#e8c56a" stroke="#e8c56a" />
          <path d="M22 42c4 6 16 6 20 0" />
        </g>
        <g v-else-if="kind === 'chalice'">
          <path d="M22 16h20v8c0 8-6 12-10 14-4-2-10-6-10-14z" />
          <path d="M32 38v8M24 48h16" stroke="#e8c56a" />
        </g>
        <g v-else-if="kind === 'bow'">
          <path d="M18 16c14 8 14 24 0 32" />
          <path d="M18 16l28 16L18 48" />
          <path d="M46 32h-8" stroke="#e8c56a" />
        </g>
        <g v-else-if="kind === 'candles'">
          <path d="M18 48V28h6v20M29 48V22h6v26M40 48V30h6v18" />
          <path d="M21 24c0-3 3-5 3-8M32 18c0-3 3-5 3-8M43 26c0-3 3-5 3-8" stroke="#e8c56a" />
        </g>
        <g v-else-if="kind === 'sundial'">
          <circle cx="32" cy="34" r="14" />
          <path d="M32 34L42 24" stroke="#e8c56a" />
          <path d="M32 16v4M18 34h4M42 34h4M32 48v4" />
        </g>
        <g v-else-if="kind === 'potion'">
          <path d="M28 16h8v8l8 10c0 10-6 16-12 16s-12-6-12-16l8-10z" />
          <path d="M26 36h12" stroke="#e8c56a" />
        </g>
        <g v-else-if="kind === 'horn'">
          <path d="M14 40c8-16 20-22 36-22-2 10-8 18-20 22-6 2-12 2-16 0z" />
          <path d="M18 38c6 4 14 4 22 0" stroke="#e8c56a" stroke-width="1.6" />
        </g>
        <g v-else-if="kind === 'anvil'">
          <path d="M16 28h32v6H16z" />
          <path d="M24 34h16v10H24z" />
          <path d="M20 46h24" />
          <path d="M14 22h16v6" stroke="#e8c56a" />
        </g>
        <g v-else-if="kind === 'key'">
          <circle cx="24" cy="24" r="8" />
          <path d="M30 28l18 18M40 38l6 0M44 42l6 0" stroke="#e8c56a" />
        </g>
        <g v-else>
          <path d="M20 44l12-28 12 28" />
          <path d="M24 36h16" />
        </g>
      </g>
    </svg>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  code?: string
}>()

const KIND_BY_CODE: Record<string, string> = {
  'T-DAILY-MATCH-1': 'sword',
  'T-DAILY-WIN-1': 'crown',
  'T-DAILY-MATCH-2': 'shield',
  'T-DAILY-WIN-2': 'chalice',
  'T-DAILY-MATCH-3': 'castle',
  'T-DAILY-WIN-3': 'candles',
  'T-WEEKLY-TEAM-10': 'helms',
}

const FALLBACKS = ['potion', 'horn', 'anvil', 'key', 'sword', 'shield', 'owl']

const kind = computed(() => {
  const code = props.code || ''
  if (KIND_BY_CODE[code]) return KIND_BY_CODE[code]
  let hash = 0
  for (let i = 0; i < code.length; i++) hash = (hash + code.charCodeAt(i) * (i + 1)) % FALLBACKS.length
  return FALLBACKS[hash]
})

function gid(name: string) {
  return `quest-${name}-${(props.code || 'x').replace(/[^a-zA-Z0-9-]/g, '')}`
}
</script>

<style scoped>
.medal {
  display: block;
  width: 100%;
  height: 100%;
  line-height: 0;
}
svg {
  width: 100%;
  height: 100%;
  display: block;
  filter: drop-shadow(0 1px 1px rgba(40, 24, 8, 0.45));
}
</style>
