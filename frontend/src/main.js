import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/global.css'
import { initTableColumnResize } from './utils/tableColumnResize'

createApp(App).use(router).mount('#app')
initTableColumnResize()
