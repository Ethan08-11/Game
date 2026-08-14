<template>
  <Teleport to="body">
    <div v-if="common.dialogVisible" class="owl-overlay" @click.self="common.hideDialog">
      <div class="owl-dialog">
        <h3>{{ title }}</h3>
        <p>{{ common.dialogMessage }}</p>
        <div class="dialog-actions">
          <el-button v-if="common.dialogType === 'confirm'" @click="onCancel">取消</el-button>
          <el-button type="primary" @click="common.hideDialog">确定</el-button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useCommonStore } from '@/store/common'

const common = useCommonStore()

const title = computed(() => {
  const map = { tip: '提示', confirm: '确认', warning: '警告' }
  return map[common.dialogType] || '提示'
})

const emit = defineEmits<{ cancel: [] }>()
function onCancel() {
  emit('cancel')
  common.hideDialog()
}
</script>

<style scoped>
h3 {
  margin-bottom: var(--space-2);
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
}
p {
  color: var(--color-text-secondary);
  font-size: var(--text-md);
  line-height: var(--leading-normal);
}
.dialog-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  margin-top: var(--space-5);
}
</style>
