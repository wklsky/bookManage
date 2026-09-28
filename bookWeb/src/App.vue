<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppShell, { type PageKey } from '@/components/AppShell.vue'
import ToastStack from '@/components/ToastStack.vue'
import LoginView from '@/layout/login.vue'
import DashboardView from '@/views/DashboardView.vue'
import BooksView from '@/views/BooksView.vue'
import OrdersView from '@/views/OrdersView.vue'
import CategoriesView from '@/views/CategoriesView.vue'
import UsersView from '@/views/UsersView.vue'
import ProfileView from '@/views/ProfileView.vue'
import { useAuthStore } from '@/stores/auth'
import { useSiteStore } from '@/stores/site'
import { useToast } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToast()
const site = useSiteStore()
const allPages: PageKey[] = ['dashboard', 'books', 'orders', 'categories', 'users', 'profile']
const hashPage = window.location.hash.replace('#/', '') as PageKey
const currentPage = ref<PageKey>(allPages.includes(hashPage) ? hashPage : 'books')

const pageComponent = computed(
  () =>
    ({
      dashboard: DashboardView,
      books: BooksView,
      orders: OrdersView,
      categories: CategoriesView,
      users: UsersView,
      profile: ProfileView,
    })[currentPage.value],
)

function canVisit(page: PageKey) {
  if (page === 'dashboard' || page === 'categories') return auth.isManager
  if (page === 'users') return auth.isAdmin
  return true
}

function navigate(page: PageKey) {
  const target = canVisit(page) ? page : auth.isManager ? 'dashboard' : 'books'
  currentPage.value = target
  window.location.hash = `/${target}`
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function syncHash() {
  const page = window.location.hash.replace('#/', '') as PageKey
  if (allPages.includes(page) && canVisit(page)) currentPage.value = page
}

function handleExpired() {
  // 令牌续期失败（刷新令牌过期或被吊销）时触发。不提示的话用户只会看到界面莫名跳回登录页。
  auth.reset()
  toast.error('登录状态已失效，请重新登录')
}

watch(
  () => auth.user?.role,
  (role) => {
    if (!role) return
    if (!canVisit(currentPage.value)) navigate(auth.isManager ? 'dashboard' : 'books')
    else if (!window.location.hash) navigate(auth.isManager ? 'dashboard' : 'books')
  },
)

// 站点展示配置要登录后才读得到。首次进入往往是未登录状态（停在登录页），
// 只在 onMounted 里读一次的话，登录成功后就再也没有机会补拉，
// 整站的站点名称、公告条与主题会一直停在默认值，直到用户手动刷新页面。
watch(
  () => auth.authenticated,
  (authenticated) => {
    if (authenticated) site.load()
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
  <div v-if="auth.initializing" class="app-loading">
    <div class="brand-mark large"><i></i><i></i><i></i></div>
    <span class="spinner dark"></span>
    <p>正在载入阅界…</p>
  </div>
  <LoginView v-else-if="!auth.authenticated" />
  <AppShell v-else :current-page="currentPage" @navigate="navigate">
    <Transition name="page" mode="out-in"
      ><component :is="pageComponent" :key="currentPage" @navigate="navigate"
    /></Transition>
  </AppShell>
  <ToastStack />
</template>
