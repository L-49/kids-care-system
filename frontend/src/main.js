import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)

// 便于演示与截图：允许通过 ?token=xxx&user=json 直接注入登录态
const qs = new URLSearchParams(window.location.search)
if (qs.get('token')) {
  localStorage.setItem('kids_token', qs.get('token'))
  localStorage.setItem(
    'kids_user',
    qs.get('user') ||
      JSON.stringify({ id: 1, username: 'admin', realName: '园长', role: 'admin' })
  )
}

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')
