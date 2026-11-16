<template>
  <div class="lang-switch" role="group" aria-label="Language">
    <button type="button" :class="{ on: lang === 'zh' }" @click="setLang('zh')">中文</button>
    <button type="button" :class="{ on: lang === 'en' }" @click="setLang('en')">EN</button>
  </div>
</template>

<script setup lang="ts">
import { lang, setLang } from '@/i18n'
</script>

<style scoped>
.lang-switch {
  display: inline-flex;
  border: 1px solid rgba(255, 224, 160, 0.55);
  border-radius: 999px;
  overflow: hidden;
  background: rgba(16, 10, 5, 0.72);
}
.lang-switch button {
  border: 0;
  background: transparent;
  color: #f4e6c8;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 4px 10px;
  cursor: pointer;
}
.lang-switch button.on {
  background: rgba(240, 192, 80, 0.88);
  color: #2a1608;
}
</style>
