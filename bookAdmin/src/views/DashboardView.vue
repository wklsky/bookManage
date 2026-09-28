<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/views/DashboardView.vue
 * @Description: 管理后台数据概览
 */
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import { useToastStore } from '@/stores/toast'
import type { DashboardSummary } from '@/types/api'

const toast = useToastStore()
const summary = ref<DashboardSummary | null>(null)

/** 指标卡与后端字段的映射，顺序即展示顺序 */
const CARDS = [
  { key: 'bookTitles', label: '馆藏书目' },
  { key: 'totalCopies', label: '馆藏总册数' },
  { key: 'availableCopies', label: '当前可借' },
  { key: 'activeReaders', label: '借阅人数' },
  { key: 'pendingOrders', label: '待审核预约' },
  { key: 'borrowedOrders', label: '借阅中' },
  { key: 'overdueOrders', label: '已逾期' },
] as const

async function load() {
  try {
    summary.value = await api.get<DashboardSummary>('/api/dashboard/summary')
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '概览数据加载失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="stat-grid">
    <div v-for="card in CARDS" :key="card.key" class="stat-card">
      <span>{{ card.label }}</span>
      <strong>{{ summary ? (summary[card.key] ?? 0).toLocaleString() : '—' }}</strong>
    </div>
  </div>

  <section class="panel">
    <div class="panel-heading">
      <div>
        <h3>运营入口</h3>
        <p>高频操作可直接从左侧菜单进入</p>
      </div>
    </div>
    <ul style="margin: 0; padding-left: 18px; color: var(--ink-soft)">
      <li>首页推荐位：决定前台首页展示哪些书，权重越小越靠前。</li>
      <li>馆藏管理：上下架直接决定图书对读者是否可见，会记入审计日志。</li>
      <li>借阅管理：审核预约、确认借出、验收归还。</li>
      <li>站点展示配置：站点名称、公告、横幅与主题，仅系统管理员可改。</li>
    </ul>
  </section>
</template>
