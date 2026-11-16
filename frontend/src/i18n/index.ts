import { ref, watch } from 'vue'
import { phrases } from './phrases'

export type Lang = 'zh' | 'en'

const STORAGE_KEY = 'wa-lang'

function readLang(): Lang {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'en' ? 'en' : 'zh'
  } catch {
    return 'zh'
  }
}

export const lang = ref<Lang>(readLang())

watch(lang, (value) => {
  try {
    localStorage.setItem(STORAGE_KEY, value)
  } catch {
    // 无痕模式写不进本地存储时，本次会话仍然生效
  }
})

export function setLang(next: Lang) {
  lang.value = next
}

/** 界面固定中文句子。英文词表没有时仍显示中文。 */
export function t(zh: string): string {
  if (lang.value !== 'en' || !zh) return zh
  return phrases[zh] ?? zh
}
