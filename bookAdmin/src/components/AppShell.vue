<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 16:20
 * @FilePath: bookAdmin/src/components/AppShell.vue
 * @Description: 后台侧边栏布局，菜单按职责分组并按角色过滤可见项
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

interface NavGroup {
  /** 分组名，同时作为顶栏面包屑的上一级 */
  label: string
  items: NavItem[]
}

interface Props {
  /** 当前展示的页面键 */
  currentPage: PageKey
}

const props = defineProps<Props>()

/** 点击侧边栏菜单时触发，回传目标页面键 */
const emit = defineEmits<{ navigate: [page: PageKey] }>()

const auth = useAuthStore()

// 8 个入口平铺会让侧边栏变成一堵墙，按「谁日常用」分成两组：
// 馆藏运营是图书管理员每天打开的，用户与系统属于管理员才知道的配置项。
const NAV_GROUPS: NavGroup[] = [
  {
    label: '馆藏运营',
    items: [
      { key: 'dashboard', label: '数据概览', hint: '馆藏与借阅的核心指标' },
      { key: 'featured', label: '首页推荐位', hint: '控制前台首页展示哪些书' },
      { key: 'books', label: '馆藏管理', hint: '图书上下架、资料维护与库存调整' },
      { key: 'categories', label: '分类管理', hint: '维护前台分类导航' },
      { key: 'orders', label: '借阅管理', hint: '审核预约、借出与归还验收' },
    ],
  },
  {
    label: '用户与系统',
    items: [
      { key: 'users', label: '用户管理', hint: '角色、账号状态与密码重置' },
      { key: 'settings', label: '站点展示配置', hint: '站点名称、公告、横幅与主题', adminOnly: true },
      { key: 'audit', label: '操作审计日志', hint: '追溯管理员的关键操作', adminOnly: true },
    ],
  },
]

const visibleGroups = computed(() =>
  NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => !item.adminOnly || auth.isAdmin),
  })).filter((group) => group.items.length > 0),
)

const current = computed(() => {
  for (const group of NAV_GROUPS) {
    const item = group.items.find((entry) => entry.key === props.currentPage)
    if (item) return { group: group.label, ...item }
  }
  return null
})

const roleLabel = computed(
  () => ({ READER: '读者', LIBRARIAN: '图书管理员', ADMIN: '系统管理员' })[auth.user?.role ?? 'READER'],
)

const avatarText = computed(() => (auth.user?.nickname || auth.user?.username || '?').slice(0, 1))
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <div class="brand">
        <div class="brand-mark">阅</div>
        <div class="brand-text">
          <strong>阅界管理后台</strong>
          <span>Book Admin</span>
        </div>
      </div>

      <nav class="side-nav">
        <section v-for="group in visibleGroups" :key="group.label" class="nav-group">
          <p class="nav-label">{{ group.label }}</p>
          <button
            v-for="item in group.items"
            :key="item.key"
            type="button"
            :class="{ active: item.key === currentPage }"
            @click="emit('navigate', item.key)"
          >
            {{ item.label }}
          </button>
        </section>
      </nav>

      <div class="side-foot">
        <div class="side-user">
          <span class="side-avatar">{{ avatarText }}</span>
          <div class="side-meta">
            <strong>{{ auth.user?.nickname || auth.user?.username }}</strong>
            <span>{{ roleLabel }} · #{{ auth.user?.id }}</span>
          </div>
        </div>
        <button class="btn btn-sm" type="button" @click="auth.logout()">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-top">
        <div>
          <p v-if="current" class="crumb">{{ current.group }} / {{ current.label }}</p>
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
