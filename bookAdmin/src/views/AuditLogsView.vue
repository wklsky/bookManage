<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/views/AuditLogsView.vue
 * @Description: 管理员操作审计日志查询
 */
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useToastStore } from '@/stores/toast'
import type { AuditAction, AuditLog, AuditTargetType, PageData } from '@/types/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime } from '@/utils/datetime'

const toast = useToastStore()
const logs = ref<PageData<AuditLog>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })
const loading = ref(false)
const filters = reactive({
  keyword: '',
  action: '' as '' | AuditAction,
})

const ACTION_OPTIONS: Array<{ value: AuditAction; label: string }> = [
  { value: 'FEATURED_CREATE', label: '新增推荐位' },
  { value: 'FEATURED_UPDATE', label: '修改推荐位' },
  { value: 'FEATURED_DELETE', label: '移除推荐位' },
  { value: 'SITE_SETTING_UPDATE', label: '修改站点展示配置' },
  { value: 'USER_ACCESS_UPDATE', label: '调整用户角色或状态' },
  { value: 'USER_PASSWORD_RESET', label: '重置用户密码' },
  { value: 'BOOK_STATUS_UPDATE', label: '变更图书展示状态' },
]

const TARGET_LABELS: Record<AuditTargetType, string> = {
  BOOK: '图书',
  FEATURED: '推荐位',
  SITE: '站点配置',
  USER: '用户',
}

async function load(page = 1) {
  loading.value = true
  try {
    const data = await api.get<PageData<AuditLog>>('/api/admin/audit-logs', {
      page,
      size: 10,
      keyword: filters.keyword.trim(),
      action: filters.action,
    })
    // 日志只会新增不会减少，这里仍保留越界回退，防止筛选后页数变小停在空白页
    if (data.pages > 0 && page > data.pages) {
      await load(data.pages)
      return
    }
    logs.value = data
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '审计日志加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', action: '' })
  load(1)
}

onMounted(() => load())
</script>

<template>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h3>操作记录</h3>
        <p>仅记录管理端关键操作，用于责任追溯</p>
      </div>
    </div>

    <div class="filter-bar">
      <input v-model="filters.keyword" placeholder="搜索操作人 / 摘要 / 对象ID" />
      <select v-model="filters.action" @change="load(1)">
        <option value="">全部操作类型</option>
        <option v-for="item in ACTION_OPTIONS" :key="item.value" :value="item.value">
          {{ item.label }}
        </option>
      </select>
      <button class="btn" type="button" @click="load(1)">搜索</button>
      <button class="btn" type="button" @click="resetFilters">重置</button>
    </div>

    <div class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>时间</th>
            <th>操作人</th>
            <th>操作类型</th>
            <th>对象</th>
            <th>摘要</th>
            <th>来源 IP</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs.records" :key="log.id">
            <td>{{ formatDateTime(log.createdAt) }}</td>
            <td>{{ log.operatorName }}（#{{ log.operatorId }}）</td>
            <td>{{ log.actionLabel }}</td>
            <td>{{ TARGET_LABELS[log.targetType] }}<span v-if="log.targetId"> · {{ log.targetId }}</span></td>
            <td>{{ log.summary || '—' }}</td>
            <td>{{ log.ip || '—' }}</td>
          </tr>
          <tr v-if="!logs.records.length">
            <td colspan="6" class="empty">{{ loading ? '加载中…' : '暂无操作记录' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <PaginationBar
      :page="logs.page"
      :pages="logs.pages"
      :total="logs.total"
      @update:page="load"
    />
  </section>
</template>
