import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import './style/tokens.scss'
import './style/fonts.scss'
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
