import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import './style/tokens.scss'
import 'element-plus/dist/index.css'
import './style/element-overrides.scss'
import App from './App.vue'
import router from './router'
import './style/global.scss'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')

// 装饰字体约 24MB，首屏用系统字体，空闲后再加载
const loadDisplayFonts = () => import('./style/fonts.scss')
if ('requestIdleCallback' in window) {
  window.requestIdleCallback(() => { loadDisplayFonts() }, { timeout: 2000 })
} else {
  setTimeout(loadDisplayFonts, 1200)
}
