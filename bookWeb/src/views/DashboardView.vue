<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import { useToast } from '@/stores/toast'
import type { DashboardSummary } from '@/types/api'
import type { PageKey } from '@/components/AppShell.vue'

const emit = defineEmits<{ navigate: [page: PageKey] }>()
const toast = useToast()
const loading = ref(true)
const summary = ref<DashboardSummary>({
  bookTitles: 0,
  totalCopies: 0,
  availableCopies: 0,
  activeReaders: 0,
  pendingOrders: 0,
  borrowedOrders: 0,
  overdueOrders: 0,
})

const cards = [
  { key: 'bookTitles', label: '馆藏书目', unit: '种', icon: '▤', tone: 'blue' },
  { key: 'totalCopies', label: '馆藏总册', unit: '册', icon: '▥', tone: 'indigo' },
  { key: 'availableCopies', label: '当前可借', unit: '册', icon: '✓', tone: 'green' },
  { key: 'activeReaders', label: '活跃读者', unit: '人', icon: '◎', tone: 'purple' },
] as const

async function loadSummary() {
  loading.value = true
  try {
    summary.value = await api.get<DashboardSummary>('/api/dashboard/summary')
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '统计数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadSummary)
</script>

<template>
  <div class="page-wrap">
    <header class="page-header">
      <div>
        <p class="eyebrow dark">MANAGEMENT OVERVIEW</p>
        <h1>数据概览</h1>
        <p>馆藏与借阅状态的实时摘要</p>
      </div>
      <button class="btn btn-ghost" type="button" :disabled="loading" @click="loadSummary">
        ↻ 刷新数据
      </button>
    </header>

    <section class="stat-grid">
      <article v-for="card in cards" :key="card.key" class="stat-card" :class="`tone-${card.tone}`">
        <span class="stat-icon">{{ card.icon }}</span>
        <div>
          <p>{{ card.label }}</p>
          <strong v-if="!loading"
            >{{ summary[card.key].toLocaleString() }}<small>{{ card.unit }}</small></strong
          ><span v-else class="skeleton number"></span>
        </div>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="panel circulation-panel">
        <div class="panel-heading">
          <div>
            <h2>借阅流转</h2>
            <p>当前需要关注的借阅状态</p>
          </div>
          <button class="text-button" type="button" @click="emit('navigate', 'orders')">
            查看全部 →
          </button>
        </div>
        <div class="flow-list">
          <button type="button" class="flow-item pending" @click="emit('navigate', 'orders')">
            <span class="flow-mark">待</span
            ><span
              ><strong>{{ summary.pendingOrders }}</strong
              ><small>待审核预约</small></span
            ><i>需要处理</i>
          </button>
          <button type="button" class="flow-item borrowed" @click="emit('navigate', 'orders')">
            <span class="flow-mark">借</span
            ><span
              ><strong>{{ summary.borrowedOrders }}</strong
              ><small>借阅中</small></span
            ><i>正常流转</i>
          </button>
          <button type="button" class="flow-item overdue" @click="emit('navigate', 'orders')">
            <span class="flow-mark">逾</span
            ><span
              ><strong>{{ summary.overdueOrders }}</strong
              ><small>已逾期</small></span
            ><i>优先跟进</i>
          </button>
        </div>
      </article>

      <article class="panel stock-panel">
        <div class="panel-heading">
          <div>
            <h2>馆藏可用率</h2>
            <p>可借册数占馆藏总册比例</p>
          </div>
        </div>
        <div class="stock-figure">
          <div
            class="ring"
            :style="{
              '--percent': `${summary.totalCopies ? Math.round((summary.availableCopies / summary.totalCopies) * 100) : 0}%`,
            }"
          >
            <strong
              >{{
                summary.totalCopies
                  ? Math.round((summary.availableCopies / summary.totalCopies) * 100)
                  : 0
              }}%</strong
            ><span>可借</span>
          </div>
          <div class="stock-legend">
            <p>
              <i class="dot available"></i><span>可借馆藏</span
              ><strong>{{ summary.availableCopies }}</strong>
            </p>
            <p>
              <i class="dot occupied"></i><span>借出/其他</span
              ><strong>{{ Math.max(0, summary.totalCopies - summary.availableCopies) }}</strong>
            </p>
          </div>
        </div>
      </article>
    </section>

    <section class="panel quick-panel">
      <div class="panel-heading">
        <div>
          <h2>快捷操作</h2>
          <p>常用管理入口</p>
        </div>
      </div>
      <div class="quick-actions">
        <button type="button" @click="emit('navigate', 'books')">
          <span>＋</span><strong>新增图书</strong><small>录入新馆藏资料</small>
        </button>
        <button type="button" @click="emit('navigate', 'orders')">
          <span>✓</span><strong>审核预约</strong><small>处理待读者申请</small>
        </button>
        <button type="button" @click="emit('navigate', 'categories')">
          <span>⌗</span><strong>维护分类</strong><small>整理图书分类结构</small>
        </button>
      </div>
    </section>
  </div>
</template>
