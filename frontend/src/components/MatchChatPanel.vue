<template>
  <aside class="match-chat-panel" role="dialog" aria-label="对局聊天">
    <header class="match-chat-head">
      <strong>对局聊天</strong>
      <button type="button" class="match-chat-close" aria-label="关闭聊天" @click="$emit('close')">×</button>
    </header>
    <div ref="listRef" class="match-chat-list">
      <p v-if="messages.length === 0" class="match-chat-empty">点快捷语，或输入一句话发给队友</p>
      <div
        v-for="item in messages"
        :key="item.id"
        class="match-chat-item"
        :class="{ mine: item.mine }"
      >
        <span class="match-chat-name">{{ item.mine ? '我' : item.name }}</span>
        <p class="match-chat-text">{{ item.text }}</p>
      </div>
    </div>
    <div class="match-chat-phrases">
      <button
        v-for="phrase in phrases"
        :key="phrase"
        type="button"
        class="match-chat-phrase"
        :disabled="disabled"
        @click="$emit('send', phrase)"
      >{{ phrase }}</button>
    </div>
    <form class="match-chat-form" @submit.prevent="submitDraft">
      <input
        v-model="draft"
        class="match-chat-input"
        maxlength="40"
        :disabled="disabled"
        placeholder="发给队友…"
        @keydown.esc="$emit('close')"
      />
      <button type="submit" class="match-chat-send" :disabled="disabled || !draft.trim()">发送</button>
    </form>
  </aside>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

export interface MatchChatMessage {
  id: string
  name: string
  text: string
  mine: boolean
}

const props = defineProps<{
  messages: MatchChatMessage[]
  disabled?: boolean
  phrases: string[]
}>()

const emit = defineEmits<{
  close: []
  send: [text: string]
}>()

const draft = ref('')
const listRef = ref<HTMLElement | null>(null)

watch(() => props.messages.length, async () => {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}, { flush: 'post' })

function submitDraft() {
  const text = draft.value.trim()
  if (!text) return
  emit('send', text)
  draft.value = ''
}
</script>

<style scoped>
.match-chat-panel {
  position: absolute;
  left: 12px;
  bottom: 16px;
  z-index: 50;
  width: 248px;
  max-height: min(320px, 46vh);
  display: flex;
  flex-direction: column;
  padding: 10px 10px 8px;
  border: 1px solid rgba(139, 105, 20, 0.55);
  border-radius: 10px;
  background: rgba(246, 236, 214, 0.96);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.38);
  color: #3e2723;
  pointer-events: auto;
}
.match-chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid rgba(139, 105, 20, 0.28);
}
.match-chat-head strong {
  font-size: 14px;
  letter-spacing: 0.06em;
}
.match-chat-close {
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: transparent;
  color: #5d3a1a;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}
.match-chat-list {
  flex: 1;
  min-height: 72px;
  max-height: 112px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-right: 2px;
}
.match-chat-empty {
  margin: 8px 0;
  font-size: 12px;
  color: rgba(62, 39, 35, 0.62);
  line-height: 1.4;
}
.match-chat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}
.match-chat-item.mine {
  align-items: flex-end;
}
.match-chat-name {
  font-size: 11px;
  color: rgba(62, 39, 35, 0.62);
}
.match-chat-text {
  margin: 0;
  max-width: 92%;
  padding: 5px 8px;
  border-radius: 8px;
  background: rgba(255, 248, 230, 0.92);
  font-size: 13px;
  line-height: 1.35;
  word-break: break-word;
}
.match-chat-item.mine .match-chat-text {
  background: #e8d7a8;
}
.match-chat-phrases {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 8px 0 6px;
}
.match-chat-phrase {
  padding: 3px 7px;
  border: 1px solid rgba(93, 58, 26, 0.28);
  border-radius: 999px;
  background: rgba(255, 250, 236, 0.9);
  color: #3e2723;
  font-size: 12px;
  line-height: 1.3;
  cursor: pointer;
}
.match-chat-phrase:hover:not(:disabled) {
  background: #f3e4b8;
}
.match-chat-phrase:disabled,
.match-chat-send:disabled,
.match-chat-input:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.match-chat-form {
  display: flex;
  gap: 6px;
}
.match-chat-input {
  flex: 1;
  min-width: 0;
  height: 30px;
  padding: 0 8px;
  border: 1px solid rgba(93, 58, 26, 0.3);
  border-radius: 6px;
  background: #fffaf0;
  color: #3e2723;
  font-size: 13px;
}
.match-chat-send {
  flex: 0 0 auto;
  height: 30px;
  padding: 0 10px;
  border: none;
  border-radius: 6px;
  background: #8b5a1a;
  color: #fff8e8;
  font-size: 13px;
  cursor: pointer;
}
</style>
