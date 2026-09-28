<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/App.vue
 * @Description: 后台页面入口，负责登录态校验与页面路由
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppShell, { type PageKey } from '@/components/AppShell.vue'
import ToastStack from '@/components/ToastStack.vue'
import LoginView from '@/layout/login.vue'
import DashboardView from '@/views/DashboardView.vue'
import FeaturedBooksView from '@/views/FeaturedBooksView.vue'
import BooksView from '@/views/BooksView.vue'
import CategoriesView from '@/views/CategoriesView.vue'
import OrdersView from '@/views/OrdersView.vue'
import UsersView from '@/views/UsersView.vue'
import SiteSettingsView from '@/views/SiteSettingsView.vue'
import AuditLogsView from '@/views/AuditLogsView.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()

const ALL_PAGES: PageKey[] = [
  'dashboard',
  'featured',
  'books',
  'categories',
  'orders',
  'users',
  'settings',
  'audit',
]

const PAGE_MAP = {
  dashboard: DashboardView,
  featured: FeaturedBooksView,
  books: BooksView,
  categories: CategoriesView,
  orders: OrdersView,
  users: UsersView,
  settings: SiteSettingsView,
  audit: AuditLogsView,
} as const

function readHash(): PageKey {
  const value = window.location.hash.replace('#/', '') as PageKey
  return ALL_PAGES.includes(value) ? value : 'dashboard'
}

const currentPage = ref<PageKey>(readHash())

function canVisit(page: PageKey) {
  // 站点配置与审计日志只开放给系统管理员。菜单层已隐藏，
  // 这里再兜一层，防止手动改地址栏 hash 直接进入。
  if (page === 'settings' || page === 'audit') return auth.isAdmin
  return true
}

const pageComponent = computed(() => {
  const key = canVisit(currentPage.value) ? currentPage.value : 'dashboard'
  return PAGE_MAP[key]
})

function navigate(page: PageKey) {
  const target = canVisit(page) ? page : 'dashboard'
  currentPage.value = target
  window.location.hash = `/${target}`
  window.scrollTo({ top: 0 })
}

function syncHash() {
  const page = readHash()
  if (canVisit(page)) currentPage.value = page
}

function handleExpired() {
  // 令牌续期失败或被吊销时触发；不提示的话用户只会看到界面莫名跳回登录页
  auth.reset()
  toast.error('登录状态已失效，请重新登录')
}

// 管理员把自己降权后，当前页会立刻变成无权访问，需要回退到概览页
watch(
  () => auth.user?.role,
  () => {
    if (auth.user && !canVisit(currentPage.value)) navigate('dashboard')
  },
)

onMounted(() => {
  auth.loadProfile()
  window.addEventListener('hashchange', syncHash)
  window.addEventListener('auth-expired', handleExpired)
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', syncHash)
  window.removeEventListener('auth-expired', handleExpired)
})
</script>

<template>
  <div v-if="auth.initializing" class="center-note">
    <p>正在载入管理后台…</p>
  </div>

  <LoginView v-else-if="!auth.authenticated" />

  <div v-else-if="!auth.isManager" class="center-note">
    <h2>该账号没有后台访问权限</h2>
    <p>读者账号请使用前台站点，或联系系统管理员开通管理权限。</p>
    <button class="btn btn-primary" type="button" @click="auth.logout()">退出登录</button>
  </div>

  <AppShell v-else :current-page="currentPage" @navigate="navigate">
    <component :is="pageComponent" :key="currentPage" />
  </AppShell>

  <ToastStack />
</template>
