<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/components/AppShell.vue
 * @Description: 后台侧边栏布局，按角色过滤可见菜单
 */
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export type PageKey =
  | 'dashboard'
  | 'featured'
  | 'books'
  | 'categories'
  | 'orders'
  | 'users'
  | 'settings'
  | 'audit'

interface NavItem {
  /** 页面键，与 App.vue 中的组件映射一一对应 */
  key: PageKey
  label: string
  /** 顶栏副标题，说明该菜单负责哪块运营工作 */
  hint: string
  /** 仅系统管理员可见 */
  adminOnly?: boolean
}

interface Props {
  /** 当前展示的页面键 */
  currentPage: PageKey
}

const props = defineProps<Props>()

/** 点击侧边栏菜单时触发，回传目标页面键 */
const emit = defineEmits<{ navigate: [page: PageKey] }>()

const auth = useAuthStore()

const NAV_ITEMS: NavItem[] = [
  { key: 'dashboard', label: '数据概览', hint: '馆藏与借阅的核心指标' },
  { key: 'featured', label: '首页推荐位', hint: '控制前台首页展示哪些书' },
  { key: 'books', label: '馆藏管理', hint: '图书上下架、资料维护与库存调整' },
  { key: 'categories', label: '分类管理', hint: '维护前台分类导航' },
  { key: 'orders', label: '借阅管理', hint: '审核预约、借出与归还验收' },
  { key: 'users', label: '用户管理', hint: '角色、账号状态与密码重置' },
  { key: 'settings', label: '站点展示配置', hint: '站点名称、公告、横幅与主题', adminOnly: true },
  { key: 'audit', label: '操作审计日志', hint: '追溯管理员的关键操作', adminOnly: true },
]

const visibleNav = computed(() => NAV_ITEMS.filter((item) => !item.adminOnly || auth.isAdmin))
const current = computed(() => NAV_ITEMS.find((item) => item.key === props.currentPage))

const roleLabel = computed(
  () => ({ READER: '读者', LIBRARIAN: '图书管理员', ADMIN: '系统管理员' })[auth.user?.role ?? 'READER'],
)
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <div class="brand">
        <div class="brand-mark">阅</div>
        <div class="brand-text">
          <strong>阅界管理后台</strong>
          <span>Book Admin Console</span>
        </div>
      </div>

      <nav class="side-nav">
        <button
          v-for="(item, index) in visibleNav"
          :key="item.key"
          type="button"
          :class="{ active: item.key === currentPage }"
          @click="emit('navigate', item.key)"
        >
          <span class="nav-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="side-foot">
        <div class="side-user">
          <strong>{{ auth.user?.nickname || auth.user?.username }}</strong>
          <span>{{ roleLabel }} · #{{ auth.user?.id }}</span>
        </div>
        <button class="btn btn-sm" type="button" @click="auth.logout()">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-top">
        <div>
          <h2>{{ current?.label }}</h2>
          <p>{{ current?.hint }}</p>
        </div>
        <slot name="actions" />
      </header>
      <div class="admin-body">
        <slot />
      </div>
    </main>
  </div>
</template>
