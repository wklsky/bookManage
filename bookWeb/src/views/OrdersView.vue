<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'
import type { BorrowOrder, OrderStatus, PageData, ReturnCondition } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const auth = useAuthStore()
const toast = useToast()
const loading = ref(false)
const actionLoading = ref(false)
const orders = ref<PageData<BorrowOrder>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })
const filters = reactive({ keyword: '', status: '' as '' | OrderStatus, from: '', to: '' })
const selected = ref<BorrowOrder | null>(null)
const action = ref<'detail' | 'audit' | 'checkout' | 'return' | 'confirmReturn' | null>(null)
const actionForm = reactive({
  decision: 'APPROVE' as 'APPROVE' | 'REJECT',
  remark: '',
  dueAt: '',
  condition: 'GOOD' as ReturnCondition,
})

const statusInfo: Record<OrderStatus, { label: string; tone: string }> = {
  PENDING: { label: '待审核', tone: 'warning' },
  APPROVED: { label: '已通过', tone: 'info' },
  REJECTED: { label: '已拒绝', tone: 'danger' },
  BORROWED: { label: '借阅中', tone: 'primary' },
  RETURN_REQUESTED: { label: '待验收', tone: 'purple' },
  RETURNED: { label: '已归还', tone: 'success' },
  CANCELLED: { label: '已取消', tone: 'muted' },
  OVERDUE: { label: '已逾期', tone: 'danger' },
}

const modalTitle = computed(
  () =>
    ({
      detail: '借阅单详情',
      audit: '审核预约',
      checkout: '确认借出',
      return: '发起还书',
      confirmReturn: '验收归还',
    })[action.value || 'detail'],
)

function displayDate(value?: string, withTime = false) {
  if (!value) return '—'
  return new Intl.DateTimeFormat(
    'zh-CN',
    withTime
      ? { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
      : undefined,
  ).format(new Date(value))
}

function defaultDueDate() {
  const date = new Date()
  date.setDate(date.getDate() + 30)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}

async function loadOrders(page = 1) {
  loading.value = true
  try {
    const path = auth.isReader ? '/api/orders/mine' : '/api/orders'
    orders.value = await api.get<PageData<BorrowOrder>>(path, {
      page,
      size: 10,
      keyword: filters.keyword.trim(),
      status: filters.status,
      from: auth.isManager ? filters.from : undefined,
      to: auth.isManager ? filters.to : undefined,
    })
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '借阅记录加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', status: '', from: '', to: '' })
  loadOrders(1)
}

async function openDetail(order: BorrowOrder) {
  selected.value = order
  action.value = 'detail'
  try {
    selected.value = await api.get<BorrowOrder>(`/api/orders/${order.id}`)
  } catch {
    /* 保留列表数据 */
  }
}

function openAction(order: BorrowOrder, next: Exclude<typeof action.value, 'detail' | null>) {
  selected.value = order
  action.value = next
  actionForm.decision = 'APPROVE'
  actionForm.remark = ''
  actionForm.dueAt = defaultDueDate()
  actionForm.condition = 'GOOD'
}

async function cancelOrder(order: BorrowOrder) {
  if (!window.confirm(`确定取消预约 ${order.orderNo} 吗？`)) return
  try {
    await api.put(`/api/orders/${order.id}/cancel`)
    toast.success('预约已取消')
    await loadOrders(orders.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '取消失败')
  }
}

async function submitAction() {
  if (!selected.value || !action.value) return
  actionLoading.value = true
  try {
    const id = selected.value.id
    if (action.value === 'audit') {
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
    } else if (action.value === 'return') {
      await api.put(`/api/orders/${id}/return`, { remark: actionForm.remark.trim() || undefined })
      toast.success('还书申请已提交')
    } else if (action.value === 'confirmReturn') {
      await api.put(`/api/orders/${id}/confirm-return`, {
        condition: actionForm.condition,
        remark: actionForm.remark.trim() || undefined,
      })
      toast.success('归还已验收')
    }
    action.value = null
    await loadOrders(orders.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => loadOrders())
</script>

<template>
  <div class="page-wrap">
    <header class="page-header">
      <div>
        <p class="eyebrow dark">BORROWING WORKFLOW</p>
        <h1>{{ auth.isReader ? '我的借阅' : '借阅管理' }}</h1>
        <p>
          {{ auth.isReader ? '查看预约进度、借阅期限并发起归还' : '审核预约并完成借出、归还流转' }}
        </p>
      </div>
      <button
        class="btn btn-ghost"
        type="button"
        :disabled="loading"
        @click="loadOrders(orders.page)"
      >
        ↻ 刷新
      </button>
    </header>

    <section class="filter-bar">
      <form class="search-box" @submit.prevent="loadOrders(1)">
        <span>⌕</span
        ><input
          v-model="filters.keyword"
          :placeholder="auth.isReader ? '搜索书名或借阅单号' : '搜索单号、读者或图书'"
        /><button type="submit">搜索</button>
      </form>
      <select v-model="filters.status" aria-label="借阅状态" @change="loadOrders(1)">
        <option value="">全部状态</option>
        <option v-for="(info, key) in statusInfo" :key="key" :value="key">{{ info.label }}</option>
      </select>
      <template v-if="auth.isManager"
        ><label class="date-filter"
          ><span>从</span
          ><input v-model="filters.from" type="date" @change="loadOrders(1)" /></label
        ><label class="date-filter"
          ><span>至</span><input v-model="filters.to" type="date" @change="loadOrders(1)" /></label
      ></template>
      <button class="btn btn-ghost btn-sm" type="button" @click="resetFilters">重置</button>
    </section>

    <section class="panel table-panel">
      <div class="table-scroll">
        <table class="data-table orders-table">
          <thead>
            <tr>
              <th>借阅单 / 图书</th>
              <th v-if="auth.isManager">读者</th>
              <th>状态</th>
              <th>预约时间</th>
              <th>应还日期</th>
              <th class="align-right">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr v-for="n in 6" :key="n">
              <td colspan="6"><span class="skeleton table-line"></span></td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr v-for="order in orders.records" :key="order.id">
              <td>
                <div class="order-book">
                  <span class="order-cover">{{ order.book.title.slice(0, 1) }}</span
                  ><span
                    ><button type="button" @click="openDetail(order)">{{ order.book.title }}</button
                    ><small>{{ order.orderNo }} · {{ order.book.isbn }}</small></span
                  >
                </div>
              </td>
              <td v-if="auth.isManager">
                <strong class="reader-name">{{ order.user.nickname || order.user.username }}</strong
                ><small class="cell-sub">@{{ order.user.username }}</small>
              </td>
              <td>
                <span class="status-badge" :class="`badge-${statusInfo[order.status].tone}`">{{
                  statusInfo[order.status].label
                }}</span>
              </td>
              <td>{{ displayDate(order.reservedAt || order.createdAt, true) }}</td>
              <td :class="{ 'text-danger': order.status === 'OVERDUE' }">
                {{ displayDate(order.dueAt) }}
              </td>
              <td>
                <div class="table-actions">
                  <button class="text-button" type="button" @click="openDetail(order)">详情</button>
                  <template v-if="auth.isReader"
                    ><button
                      v-if="order.status === 'PENDING'"
                      class="text-button danger-text"
                      type="button"
                      @click="cancelOrder(order)"
                    >
                      取消</button
                    ><button
                      v-if="order.status === 'BORROWED' || order.status === 'OVERDUE'"
                      class="btn btn-primary btn-sm"
                      type="button"
                      @click="openAction(order, 'return')"
                    >
                      发起还书
                    </button></template
                  >
                  <template v-else
                    ><button
                      v-if="order.status === 'PENDING'"
                      class="btn btn-primary btn-sm"
                      type="button"
                      @click="openAction(order, 'audit')"
                    >
                      审核</button
                    ><button
                      v-if="order.status === 'APPROVED'"
                      class="btn btn-primary btn-sm"
                      type="button"
                      @click="openAction(order, 'checkout')"
                    >
                      确认借出</button
                    ><button
                      v-if="order.status === 'RETURN_REQUESTED'"
                      class="btn btn-primary btn-sm"
                      type="button"
                      @click="openAction(order, 'confirmReturn')"
                    >
                      验收归还
                    </button></template
                  >
                </div>
              </td>
            </tr>
            <tr v-if="!orders.records.length">
              <td colspan="6">
                <div class="table-empty">
                  <span>⇄</span><strong>暂无借阅记录</strong>
                  <p>当前筛选条件下没有数据。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        :page="orders.page"
        :pages="orders.pages"
        :total="orders.total"
        @change="loadOrders"
      />
    </section>

    <BaseModal :open="Boolean(action)" :title="modalTitle" width="620px" @close="action = null">
      <template v-if="selected">
        <div class="order-modal-summary">
          <span class="order-cover large">{{ selected.book.title.slice(0, 1) }}</span>
          <div>
            <h3>{{ selected.book.title }}</h3>
            <p>{{ selected.orderNo }}</p>
          </div>
          <span class="status-badge" :class="`badge-${statusInfo[selected.status].tone}`">{{
            statusInfo[selected.status].label
          }}</span>
        </div>
        <div v-if="action === 'detail'" class="detail-list">
          <dl>
            <div>
              <dt>读者</dt>
              <dd>{{ selected.user.nickname || selected.user.username }}</dd>
            </div>
            <div>
              <dt>预约时间</dt>
              <dd>{{ displayDate(selected.reservedAt || selected.createdAt, true) }}</dd>
            </div>
            <div>
              <dt>借出时间</dt>
              <dd>{{ displayDate(selected.borrowedAt, true) }}</dd>
            </div>
            <div>
              <dt>应还日期</dt>
              <dd>{{ displayDate(selected.dueAt, true) }}</dd>
            </div>
            <div>
              <dt>归还时间</dt>
              <dd>{{ displayDate(selected.returnedAt, true) }}</dd>
            </div>
            <div>
              <dt>归还状况</dt>
              <dd>
                {{
                  selected.returnCondition
                    ? { GOOD: '完好', DAMAGED: '损坏', LOST: '遗失' }[selected.returnCondition]
                    : '—'
                }}
              </dd>
            </div>
          </dl>
          <div v-if="selected.remark" class="note-box">
            <strong>预约备注</strong>
            <p>{{ selected.remark }}</p>
          </div>
          <div v-if="selected.auditRemark" class="note-box">
            <strong>审核备注</strong>
            <p>{{ selected.auditRemark }}</p>
          </div>
        </div>
        <form v-else class="form-stack" @submit.prevent="submitAction">
          <template v-if="action === 'audit'"
            ><label class="field"
              ><span>审核结果 <b>*</b></span
              ><span class="segmented"
                ><button
                  type="button"
                  :class="{ active: actionForm.decision === 'APPROVE' }"
                  @click="actionForm.decision = 'APPROVE'"
                >
                  通过</button
                ><button
                  type="button"
                  :class="{ active: actionForm.decision === 'REJECT' }"
                  @click="actionForm.decision = 'REJECT'"
                >
                  拒绝
                </button></span
              ></label
            ></template
          >
          <label v-if="action === 'checkout'" class="field"
            ><span>应还日期</span><input v-model="actionForm.dueAt" type="datetime-local" /><small
              >默认借期 30 天，可按实际情况调整。</small
            ></label
          >
          <label v-if="action === 'confirmReturn'" class="field"
            ><span>图书状况 <b>*</b></span
            ><select v-model="actionForm.condition">
              <option value="GOOD">完好</option>
              <option value="DAMAGED">损坏</option>
              <option value="LOST">遗失</option>
            </select></label
          >
          <label class="field"
            ><span>{{ action === 'return' ? '还书备注' : '处理备注' }}</span
            ><textarea
              v-model="actionForm.remark"
              rows="4"
              :placeholder="
                action === 'audit' && actionForm.decision === 'REJECT' ? '建议填写拒绝原因' : '选填'
              "
            ></textarea>
          </label>
          <div v-if="action === 'return'" class="info-banner">
            提交后，请将图书交至服务台，等待管理员验收。
          </div>
        </form>
      </template>
      <template #footer
        ><button class="btn btn-ghost" type="button" @click="action = null">关闭</button
        ><button
          v-if="action !== 'detail'"
          class="btn btn-primary"
          type="button"
          :disabled="actionLoading"
          @click="submitAction"
        >
          {{ actionLoading ? '提交中…' : '确认提交' }}
        </button></template
      >
    </BaseModal>
  </div>
</template>
