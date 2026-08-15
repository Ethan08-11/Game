<template>
  <Teleport to="body">
    <div v-if="modelValue" class="avatar-overlay" @click.self="close">
      <div class="avatar-modal" role="dialog" aria-modal="true" aria-labelledby="avatar-picker-title">
        <button class="avatar-close" type="button" aria-label="关闭" @click="close">×</button>
        <header class="avatar-header">
          <span class="avatar-seal">名片</span>
          <h2 id="avatar-picker-title">更换头像</h2>
          <p class="avatar-sub">选一张形象，好友和组队房间里都会看到</p>
        </header>

        <div class="avatar-preview-wrap">
          <PlayerAvatar class="preview-avatar" :src="selectedUrl" />
          <div class="preview-meta">
            <strong>{{ selectedName }}</strong>
            <span>{{ user.username }}</span>
          </div>
        </div>

        <div class="avatar-grid">
          <button
            v-for="item in PRESET_AVATARS"
            :key="item.id"
            type="button"
            class="avatar-option"
            :class="{ selected: item.url === selectedUrl }"
            :title="item.name"
            @click="selectedUrl = item.url"
            @dblclick="save(item.url)"
          >
            <PlayerAvatar class="option-avatar" :src="item.url" :alt="item.name" />
            <span class="option-name">{{ item.name }}</span>
          </button>
        </div>

        <footer class="avatar-footer">
          <button type="button" class="nav-btn" @click="close">取消</button>
          <button type="button" class="save-btn" :disabled="saving || !selectedUrl" @click="save()">
            {{ saving ? '保存中...' : '保存头像' }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { PRESET_AVATARS } from '@/data/avatars'
import { useUserStore } from '@/store/user'
import PlayerAvatar from '@/components/PlayerAvatar.vue'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const user = useUserStore()
const selectedUrl = ref('')
const saving = ref(false)

const selectedName = computed(() => {
  return PRESET_AVATARS.find(item => item.url === selectedUrl.value)?.name || '当前头像'
})

watch(() => props.modelValue, (open) => {
  if (!open) return
  const current = user.avatar || PRESET_AVATARS[0].url
  selectedUrl.value = PRESET_AVATARS.some(item => item.url === current)
    ? current
    : (current || PRESET_AVATARS[0].url)
})

function close() {
  if (saving.value) return
  emit('update:modelValue', false)
}

async function save(url = selectedUrl.value) {
  if (!url || saving.value) return
  saving.value = true
  const previous = user.avatar
  user.avatar = url
  try {
    await user.updateProfile({ avatarUrl: url })
    ElMessage.success('头像已更新')
    emit('update:modelValue', false)
  } catch (e: any) {
    user.avatar = previous
    ElMessage.error(e?.message || '头像保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style>
.avatar-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 12, 6, 0.62);
  backdrop-filter: blur(4px);
}

.avatar-modal {
  position: relative;
  width: min(640px, calc(100vw - 32px));
  max-height: min(86vh, 760px);
  display: flex;
  flex-direction: column;
  padding: 24px 28px 18px;
  background: linear-gradient(180deg, #f7ecd6 0%, #e8d4b0 100%);
  border: 2px solid #6b4a28;
  border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 248, 230, 0.8);
  color: #3e2723;
}

.avatar-close {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #5d3a1a;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
}

.avatar-header h2 {
  margin: 8px 0 4px;
  font-size: 22px;
  color: #3a1f0d;
}

.avatar-seal {
  display: inline-block;
  padding: 1px 10px;
  border: 1px solid #8b6914;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: #5d3a1a;
}

.avatar-sub {
  margin: 0 0 14px;
  color: #8b6914;
  font-size: 13px;
}

.avatar-preview-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 10px 12px;
  border: 1px solid rgba(139, 105, 20, 0.28);
  border-radius: 10px;
  background: rgba(255, 248, 230, 0.45);
}

.preview-avatar {
  width: 72px;
  height: 72px;
  border: 2px solid #c4a962;
  box-shadow: 0 2px 8px rgba(80, 48, 16, 0.25);
}

.preview-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-meta strong {
  font-size: 16px;
  color: #3a1f0d;
}

.preview-meta span {
  font-size: 13px;
  color: #8b6914;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  overflow-y: auto;
  padding: 2px 2px 8px;
  min-height: 0;
}

.avatar-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px 6px 6px;
  border: 2px solid transparent;
  border-radius: 10px;
  background: rgba(255, 248, 230, 0.35);
  cursor: pointer;
  color: #4a3520;
}

.option-avatar {
  width: 72px;
  height: 72px;
}

.avatar-option:hover {
  background: rgba(255, 248, 230, 0.7);
}

.avatar-option.selected {
  border-color: #8b6914;
  background: rgba(196, 169, 98, 0.28);
  box-shadow: inset 0 0 0 1px rgba(196, 169, 98, 0.6);
}

.option-name {
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.avatar-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid rgba(139, 105, 20, 0.28);
}

.nav-btn,
.save-btn {
  border: 1px solid #8b6914;
  border-radius: 6px;
  padding: 6px 16px;
  cursor: pointer;
  font-weight: 600;
}

.nav-btn {
  background: rgba(255, 248, 230, 0.7);
  color: #4a3520;
}

.save-btn {
  background: #8b6914;
  color: #fff8e6;
}

.save-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .avatar-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
