import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCommonStore = defineStore('common', () => {
  const cgPlaying = ref<boolean>(true)
  const dialogVisible = ref<boolean>(false)
  const dialogType = ref<'tip' | 'confirm' | 'warning'>('tip')
  const dialogMessage = ref<string>('')
  const loadingVisible = ref<boolean>(false)

  function showDialog(type: 'tip' | 'confirm' | 'warning', msg: string) {
    dialogType.value = type
    dialogMessage.value = msg
    dialogVisible.value = true
  }

  function hideDialog() {
    dialogVisible.value = false
  }

  function showLoading() {
    loadingVisible.value = true
  }

  function hideLoading() {
    loadingVisible.value = false
  }

  return { cgPlaying, dialogVisible, dialogType, dialogMessage, loadingVisible, showDialog, hideDialog, showLoading, hideLoading }
})
