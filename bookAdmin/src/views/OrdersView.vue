<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/views/OrdersView.vue
 * @Description: 借阅流程管理：审核预约、确认借出与验收归还
 */
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useToastStore } from '@/stores/toast'
import type { BorrowOrder, OrderStatus, PageData, ReturnCondition } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime } from '@/utils/datetime'

const toast = useToastStore()
const orders = ref<PageData<BorrowOrder>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })
const loading = ref(false)
const saving = ref(false)
const filters = reactive({ keyword: '', status: '' as '' | OrderStatus })

const STATUS_META: Record<OrderStatus, { label: string; tone: string }> = {
  PENDING: { label: '待审核', tone: 'badge-warning' },
  APPROVED: { label: '已通过', tone: 'badge-info' },
  REJECTED: { label: '已拒绝', tone: 'badge-danger' },
  BORROWED: { label: '借阅中', tone: 'badge-info' },
  RETURN_REQUESTED: { label: '待验收', tone: 'badge-warning' },
  RETURNED: { label: '已归还', tone: 'badge-success' },
  CANCELLED: { label: '已取消', tone: 'badge-muted' },
  OVERDUE: { label: '已逾期', tone: 'badge-danger' },
}

const CONDITION_OPTIONS: Array<{ value: ReturnCondition; label: string }> = [
  { value: 'GOOD', label: '完好' },
  { value: 'DAMAGED', label: '损坏' },
  { value: 'LOST', label: '遗失' },
]

type ActionKind = 'detail' | 'audit' | 'checkout' | 'confirmReturn'

const action = ref<ActionKind | null>(null)
const selected = ref<BorrowOrder | null>(null)
const actionForm = reactive({
  decision: 'APPROVE' as 'APPROVE' | 'REJECT',
  remark: '',
  dueAt: '',
  condition: 'GOOD' as ReturnCondition,
})

const MODAL_TITLES: Record<ActionKind, string> = {
  detail: '借阅单详情',
  audit: '审核预约',
  checkout: '确认借出',
  confirmReturn: '验收归还',
}

/** 默认应还时间为 30 天后，与后端 book.borrow.default-days 保持一致 */
function defaultDueDate() {
  const date = new Date()
  date.setDate(date.getDate() + 30)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}

async function load(page = 1) {
  loading.value = true
  try {
    const data = await api.get<PageData<BorrowOrder>>('/api/orders', {
      page,
      size: 10,
      keyword: filters.keyword.trim(),
      status: filters.status,
    })
    // 处理完末页最后一条后总页数减少，页码会越界；不回退会停在空白页
    if (data.pages > 0 && page > data.pages) {
      await load(data.pages)
      return
    }
    orders.value = data
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '借阅记录加载失败')
  } finally {
    loading.value = false
  }
}

async function openAction(order: BorrowOrder, kind: ActionKind) {
  selected.value = order
  action.value = kind
  Object.assign(actionForm, {
    decision: 'APPROVE',
    remark: '',
    dueAt: defaultDueDate(),
    condition: 'GOOD',
  })
  if (kind === 'detail') {
    try {
      selected.value = await api.get<BorrowOrder>(`/api/orders/${order.id}`)
    } catch {
      // 详情拉取失败时保留列表数据即可，不必打断查看
    }
  }
}

async function submit() {
  if (!selected.value || !action.value) return
  // 详情态没有要执行的动作，必须显式拦截：它与操作弹窗共用一个表单，回车会误触发提交
  if (action.value === 'detail') {
    action.value = null
    return
  }
  const id = selected.value.id
  saving.value = true
  try {
    if (action.value === 'audit') {
      // 拒绝时 remark 为条件必填，后端会校验，这里先拦一道减少一次无效往返
      if (actionForm.decision === 'REJECT' && !actionForm.remark.trim()) {
        toast.error('拒绝时必须填写原因')
        return
      }
      await api.put(`/api/orders/${id}/audit`, {
        decision: actionForm.decision,
        remark: actionForm.remark.trim() || undefined,
      })
      toast.success(actionForm.decision === 'APPROVE' ? '预约已通过' : '预约已拒绝')
    } else if (action.value === 'checkout') {
      await api.put(`/api/orders/${id}/checkout`, {
        dueAt: actionForm.dueAt ? new Date(actionForm.dueAt).toISOString() : undefined,
        remark: actionForm.remark.trim() || undefined,
      })
      toast.success('已确认借出')
    } else if (action.value === 'confirmReturn') {
      if (actionForm.condition !== 'GOOD' && !actionForm.remark.trim()) {
        toast.error('损坏或遗失时必须填写说明')
        return
      }
      await api.put(`/api/orders/${id}/confirm-return`, {
        condition: actionForm.condition,
        remark: actionForm.remark.trim() || undefined,
      })
      toast.success('归还已验收')
    }
    action.value = null
    await load(orders.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    saving.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', status: '' })
  load(1)
}

onMounted(() => load())
</script>

<template>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h3>借阅单</h3>
        <p>状态流转的最终校验在后端，前端只展示当前状态允许的动作</p>
      </div>
    </div>

    <div class="filter-bar">
      <input v-model="filters.keyword" placeholder="单号 / 用户名 / 书名 / ISBN" @keyup.enter="load(1)" />
      <select v-model="filters.status" @change="load(1)">
        <option value="">全部状态</option>
        <option v-for="(meta, key) in STATUS_META" :key="key" :value="key">{{ meta.label }}</option>
      </select>
      <button class="btn" type="button" @click="load(1)">搜索</button>
      <button class="btn" type="button" @click="resetFilters">重置</button>
    </div>

    <div class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>单号</th>
            <th>读者</th>
            <th>图书</th>
            <th>状态</th>
            <th>应还时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders.records" :key="order.id">
            <td>{{ order.orderNo }}</td>
            <td>{{ order.user.nickname || order.user.username }}</td>
            <td>《{{ order.book.title }}》</td>
            <td>
              <span class="badge" :class="STATUS_META[order.status].tone">
                {{ STATUS_META[order.status].label }}
              </span>
            </td>
            <td>{{ formatDateTime(order.dueAt) }}</td>
            <td>
              <div class="cell-actions">
                <button class="btn btn-sm" type="button" @click="openAction(order, 'detail')">
                  详情
                </button>
                <button
                  v-if="order.status === 'PENDING'"
                  class="btn btn-sm btn-primary"
                  type="button"
                  @click="openAction(order, 'audit')"
                >
                  审核
                </button>
                <button
                  v-if="order.status === 'APPROVED'"
                  class="btn btn-sm btn-primary"
                  type="button"
                  @click="openAction(order, 'checkout')"
                >
                  确认借出
                </button>
                <button
                  v-if="order.status === 'RETURN_REQUESTED'"
                  class="btn btn-sm btn-primary"
                  type="button"
                  @click="openAction(order, 'confirmReturn')"
                >
                  验收归还
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!orders.records.length">
            <td colspan="6" class="empty">{{ loading ? '加载中…' : '暂无借阅单' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <PaginationBar
      :page="orders.page"
      :pages="orders.pages"
      :total="orders.total"
      @update:page="load"
    />
  </section>

  <BaseModal
    :open="action !== null"
    :title="action ? MODAL_TITLES[action] : ''"
    @close="action = null"
  >
    <template v-if="selected">
      <dl style="display: grid; gap: 6px; margin-bottom: 14px; font-size: 13px">
        <div><strong>单号：</strong>{{ selected.orderNo }}</div>
        <div><strong>读者：</strong>{{ selected.user.nickname || selected.user.username }}</div>
        <div><strong>图书：</strong>《{{ selected.book.title }}》</div>
        <div><strong>状态：</strong>{{ STATUS_META[selected.status].label }}</div>
        <div v-if="selected.remark"><strong>读者备注：</strong>{{ selected.remark }}</div>
        <div v-if="selected.auditRemark"><strong>审核备注：</strong>{{ selected.auditRemark }}</div>
      </dl>

      <form @submit.prevent="submit">
        <template v-if="action === 'audit'">
          <label class="field">
            <span>审核结论</span>
            <select v-model="actionForm.decision">
              <option value="APPROVE">通过</option>
              <option value="REJECT">拒绝</option>
            </select>
          </label>
          <label class="field">
            <span>备注</span>
            <input v-model="actionForm.remark" maxlength="200" placeholder="拒绝时必填" />
          </label>
        </template>

        <template v-else-if="action === 'checkout'">
          <label class="field">
            <span>应还时间</span>
            <input v-model="actionForm.dueAt" type="datetime-local" />
            <small>留空则由后端按默认借阅天数计算</small>
          </label>
          <label class="field">
            <span>备注</span>
            <input v-model="actionForm.remark" maxlength="200" />
          </label>
        </template>

        <template v-else-if="action === 'confirmReturn'">
          <label class="field">
            <span>验收结果</span>
            <select v-model="actionForm.condition">
              <option v-for="item in CONDITION_OPTIONS" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>备注</span>
            <input v-model="actionForm.remark" maxlength="500" placeholder="损坏或遗失时必填" />
          </label>
        </template>

        <div class="modal-foot">
          <button class="btn" type="button" @click="action = null">关闭</button>
          <button v-if="action !== 'detail'" class="btn btn-primary" type="submit" :disabled="saving">
            {{ saving ? '提交中…' : '确认' }}
          </button>
        </div>
      </form>
    </template>
  </BaseModal>
</template>
