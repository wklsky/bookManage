/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/main.ts
 * @Description: 管理后台应用入口
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import './styles/main.css'

createApp(App).use(createPinia()).mount('#app')
