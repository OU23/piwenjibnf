import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { useAppStore } from './store/index.js'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

// 全局初始化：加载收藏数据
const store = useAppStore(pinia)
store.loadFavorites()

app.mount('#app')
