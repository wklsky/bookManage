<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useSiteStore } from '@/stores/site'

export type PageKey = 'dashboard' | 'books' | 'orders' | 'categories' | 'users' | 'profile'

const props = defineProps<{ currentPage: PageKey }>()
const emit = defineEmits<{ navigate: [page: PageKey] }>()
const auth = useAuthStore()
const site = useSiteStore()
const mobileOpen = ref(false)

const roleLabel = computed(
  () =>
    ({ READER: '读者', LIBRARIAN: '图书管理员', ADMIN: '系统管理员' })[auth.user?.role || 'READER'],
)
const userName = computed(() => auth.user?.nickname || auth.user?.username || '用户')
const initials = computed(() => userName.value.slice(0, 1).toUpperCase())

const navItems = computed(() => {
  const items: Array<{ key: PageKey; label: string; icon: string; show: boolean }> = [
    { key: 'dashboard', label: '数据概览', icon: '▦', show: auth.isManager },
    { key: 'books', label: '图书检索', icon: '▤', show: true },
    { key: 'orders', label: auth.isReader ? '我的借阅' : '借阅管理', icon: '⇄', show: true },
    { key: 'categories', label: '分类管理', icon: '⌗', show: auth.isManager },
    { key: 'users', label: '用户管理', icon: '◎', show: auth.isAdmin },
    { key: 'profile', label: '个人中心', icon: '○', show: true },
  ]
  return items.filter((item) => item.show)
})

function navigate(page: PageKey) {
  mobileOpen.value = false
  emit('navigate', page)
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ 'is-open': mobileOpen }">
      <div class="brand" @click="navigate(auth.isManager ? 'dashboard' : 'books')">
        <div class="brand-mark" aria-hidden="true"><i></i><i></i><i></i></div>
        <div><strong>阅界</strong><span>图书管理系统</span></div>
      </div>

      <nav class="main-nav" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.key"
          type="button"
          :class="{ active: props.currentPage === item.key }"
          @click="navigate(item.key)"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="shelf-line" aria-hidden="true"><i></i><i></i><i></i><i></i><i></i></div>
      <button class="sidebar-user" type="button" @click="navigate('profile')">
        <span class="avatar">{{ initials }}</span>
        <span
          ><strong>{{ userName }}</strong
          ><small>{{ roleLabel }}</small></span
        >
      </button>
    </aside>

    <button
      v-if="mobileOpen"
      class="sidebar-scrim"
      aria-label="关闭菜单"
      @click="mobileOpen = false"
    ></button>

    <main class="main-area">
      <header class="mobile-header">
        <button class="icon-button" type="button" aria-label="打开菜单" @click="mobileOpen = true">
          ☰
        </button>
        <div class="mobile-brand"><span class="mini-book"></span>阅界</div>
        <button class="avatar small" type="button" @click="navigate('profile')">
          {{ initials }}
        </button>
      </header>
      <div v-if="site.announcement" class="site-announcement">
        <span class="announce-tag">公告</span>
        <span>{{ site.announcement }}</span>
      </div>
      <slot />
    </main>
  </div>
</template>
