<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/views/UsersView.vue
 * @Description: 用户管理：角色与账号状态维护、密码重置、借阅记录查看
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import type { BorrowOrder, OrderStatus, PageData, User, UserRole, UserStatus } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime } from '@/utils/datetime'

const toast = useToastStore()
const auth = useAuthStore()
const users = ref<PageData<User>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })
const loading = ref(false)
const saving = ref(false)
const filters = reactive({ keyword: '', role: '' as '' | UserRole, status: '' as '' | UserStatus })

const ROLE_LABELS: Record<UserRole, string> = {
  READER: '读者',
  LIBRARIAN: '图书管理员',
  ADMIN: '系统管理员',
}

const STATUS_LABELS: Record<UserStatus, string> = {
  ACTIVE: '正常',
  DISABLED: '已停用',
  LOCKED: '已锁定',
}

const STATUS_TONES: Record<UserStatus, string> = {
  ACTIVE: 'badge-success',
  DISABLED: 'badge-muted',
  LOCKED: 'badge-danger',
}

const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  BORROWED: '借阅中',
  RETURN_REQUESTED: '待验收',
  RETURNED: '已归还',
  CANCELLED: '已取消',
  OVERDUE: '已逾期',
}

const accessOpen = ref(false)
const accessTarget = ref<User | null>(null)
const accessForm = reactive({
  role: 'READER' as UserRole,
  status: 'ACTIVE' as UserStatus,
  remark: '',
})

const passwordOpen = ref(false)
const passwordTarget = ref<User | null>(null)
const passwordForm = reactive({ newPassword: '', remark: '' })

const ordersOpen = ref(false)
const ordersTarget = ref<User | null>(null)
const orders = ref<PageData<BorrowOrder>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })

/** 后端禁止调整当前登录账号自己的角色与状态，这里提前禁用对应入口 */
const isSelf = computed(() => (id: number) => auth.user?.id === id)

async function load(page = 1) {
  loading.value = true
  try {
    const data = await api.get<PageData<User>>('/api/users', {
      page,
      size: 10,
      keyword: filters.keyword.trim(),
      role: filters.role,
      status: filters.status,
    })
    // 末页最后一条被删掉后总页数减少，页码会越界；不回退会停在空白页
    if (data.pages > 0 && page > data.pages) {
      await load(data.pages)
      return
    }
    users.value = data
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '用户加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', role: '', status: '' })
  load(1)
}

function openAccess(user: User) {
  accessTarget.value = user
  Object.assign(accessForm, { role: user.role, status: user.status, remark: '' })
  accessOpen.value = true
}

async function saveAccess() {
  if (!accessTarget.value) return
  saving.value = true
  try {
    await api.put(`/api/users/${accessTarget.value.id}/status`, {
      role: accessForm.role,
      status: accessForm.status,
      remark: accessForm.remark.trim() || undefined,
    })
    toast.success('用户权限已更新')
    accessOpen.value = false
    await load(users.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '更新失败')
  } finally {
    saving.value = false
  }
}

function openPassword(user: User) {
  passwordTarget.value = user
  Object.assign(passwordForm, { newPassword: '', remark: '' })
  passwordOpen.value = true
}

async function savePassword() {
  if (!passwordTarget.value) return
  // 后端要求 8~64 位，先本地校验，避免把一次明显的无效请求打到服务端
  if (passwordForm.newPassword.length < 8 || passwordForm.newPassword.length > 64) {
    toast.error('新密码长度需为 8 到 64 个字符')
    return
  }
  saving.value = true
  try {
    await api.put(`/api/admin/users/${passwordTarget.value.id}/password`, {
      newPassword: passwordForm.newPassword,
      remark: passwordForm.remark.trim() || undefined,
    })
    // 重置成功后后端会吊销该用户全部刷新令牌，对方会被强制下线
    toast.success('密码已重置，该用户需要重新登录')
    passwordOpen.value = false
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '重置失败')
  } finally {
    saving.value = false
  }
}

async function openOrders(user: User) {
  ordersTarget.value = user
  ordersOpen.value = true
  await loadOrders(1)
}

async function loadOrders(page = 1) {
  if (!ordersTarget.value) return
  try {
    const data = await api.get<PageData<BorrowOrder>>(
      `/api/admin/users/${ordersTarget.value.id}/orders`,
      { page, size: 10 },
    )
    if (data.pages > 0 && page > data.pages) {
      await loadOrders(data.pages)
      return
    }
    orders.value = data
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '借阅记录加载失败')
  }
}

onMounted(() => load())
</script>

<template>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h3>用户列表</h3>
        <p>停用账号会使其无法登录，且已签发的刷新令牌会被吊销</p>
      </div>
    </div>

    <div class="filter-bar">
      <input v-model="filters.keyword" placeholder="用户名 / 昵称 / 邮箱 / 手机号" @keyup.enter="load(1)" />
      <select v-model="filters.role" @change="load(1)">
        <option value="">全部角色</option>
        <option v-for="(label, key) in ROLE_LABELS" :key="key" :value="key">{{ label }}</option>
      </select>
      <select v-model="filters.status" @change="load(1)">
        <option value="">全部状态</option>
        <option v-for="(label, key) in STATUS_LABELS" :key="key" :value="key">{{ label }}</option>
      </select>
      <button class="btn" type="button" @click="load(1)">搜索</button>
      <button class="btn" type="button" @click="resetFilters">重置</button>
    </div>

    <div class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>用户</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users.records" :key="user.id">
            <td>
              {{ user.nickname || user.username }}<br /><small>@{{ user.username }}</small>
            </td>
            <td>{{ user.email || '—' }}</td>
            <td>{{ ROLE_LABELS[user.role] }}</td>
            <td>
              <span class="badge" :class="STATUS_TONES[user.status]">
                {{ STATUS_LABELS[user.status] }}
              </span>
            </td>
            <td>{{ formatDateTime(user.createdAt) }}</td>
            <td>
              <div class="cell-actions">
                <button
                  class="btn btn-sm"
                  type="button"
                  :disabled="isSelf(user.id)"
                  :title="isSelf(user.id) ? '不能调整当前登录账号自身' : ''"
                  @click="openAccess(user)"
                >
                  权限
                </button>
                <button class="btn btn-sm" type="button" @click="openPassword(user)">重置密码</button>
                <button class="btn btn-sm" type="button" @click="openOrders(user)">借阅记录</button>
              </div>
            </td>
          </tr>
          <tr v-if="!users.records.length">
            <td colspan="6" class="empty">{{ loading ? '加载中…' : '未找到匹配的用户' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <PaginationBar :page="users.page" :pages="users.pages" :total="users.total" @update:page="load" />
  </section>

  <BaseModal :open="accessOpen" title="调整用户权限" @close="accessOpen = false">
    <form v-if="accessTarget" @submit.prevent="saveAccess">
      <p style="margin-bottom: 12px">
        {{ accessTarget.nickname || accessTarget.username }}（@{{ accessTarget.username }}）
      </p>
      <div class="form-grid">
        <label class="field">
          <span>角色</span>
          <select v-model="accessForm.role">
            <option v-for="(label, key) in ROLE_LABELS" :key="key" :value="key">{{ label }}</option>
          </select>
        </label>
        <label class="field">
          <span>账号状态</span>
          <select v-model="accessForm.status">
            <option v-for="(label, key) in STATUS_LABELS" :key="key" :value="key">{{ label }}</option>
          </select>
        </label>
      </div>
      <label class="field">
        <span>调整原因</span>
        <input v-model="accessForm.remark" maxlength="200" placeholder="选填，会记入审计日志" />
      </label>
      <div class="modal-foot">
        <button class="btn" type="button" @click="accessOpen = false">取消</button>
        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </form>
  </BaseModal>

  <BaseModal :open="passwordOpen" title="重置用户密码" @close="passwordOpen = false">
    <form v-if="passwordTarget" @submit.prevent="savePassword">
      <p style="margin-bottom: 12px; font-size: 13px; color: var(--ink-soft)">
        正在重置
        <strong>{{ passwordTarget.nickname || passwordTarget.username }}</strong>
        的密码。重置后该用户所有设备都会被强制下线。
      </p>
      <label class="field">
        <span>新密码</span>
        <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" />
        <small>8~64 位，请通过其他渠道告知该用户</small>
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model="passwordForm.remark" maxlength="200" placeholder="选填，会记入审计日志" />
      </label>
      <div class="modal-foot">
        <button class="btn" type="button" @click="passwordOpen = false">取消</button>
        <button class="btn btn-danger" type="submit" :disabled="saving">
          {{ saving ? '提交中…' : '确认重置' }}
        </button>
      </div>
    </form>
  </BaseModal>

  <BaseModal :open="ordersOpen" title="用户借阅记录" @close="ordersOpen = false">
    <p v-if="ordersTarget" style="margin-bottom: 12px; font-size: 13px">
      {{ ordersTarget.nickname || ordersTarget.username }} 共 {{ orders.total }} 条借阅记录
    </p>
    <div class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>单号</th>
            <th>图书</th>
            <th>状态</th>
            <th>应还时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders.records" :key="order.id">
            <td>{{ order.orderNo }}</td>
            <td>《{{ order.book.title }}》</td>
            <td>{{ ORDER_STATUS_LABELS[order.status] }}</td>
            <td>{{ formatDateTime(order.dueAt) }}</td>
          </tr>
          <tr v-if="!orders.records.length">
            <td colspan="4" class="empty">该用户暂无借阅记录</td>
          </tr>
        </tbody>
      </table>
    </div>
    <PaginationBar
      :page="orders.page"
      :pages="orders.pages"
      :total="orders.total"
      @update:page="loadOrders"
    />
  </BaseModal>
</template>
