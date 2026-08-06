<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useToast } from '@/stores/toast'
import type { PageData, User, UserRole, UserStatus } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const toast = useToast()
const loading = ref(false)
const saving = ref(false)
const users = ref<PageData<User>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })
const filters = reactive({ keyword: '', role: '' as '' | UserRole, status: '' as '' | UserStatus })
const selected = ref<User | null>(null)
const modalOpen = ref(false)
const form = reactive({ role: 'READER' as UserRole, status: 'ACTIVE' as UserStatus, remark: '' })
const roleLabels: Record<UserRole, string> = {
  READER: '读者',
  LIBRARIAN: '图书管理员',
  ADMIN: '系统管理员',
}
const statusLabels: Record<UserStatus, string> = {
  ACTIVE: '正常',
  DISABLED: '已停用',
  LOCKED: '已锁定',
}

function displayDate(value?: string) {
  return value ? new Intl.DateTimeFormat('zh-CN').format(new Date(value)) : '—'
}

async function loadUsers(page = 1) {
  loading.value = true
  try {
    users.value = await api.get<PageData<User>>('/api/users', {
      page,
      size: 10,
      keyword: filters.keyword.trim(),
      role: filters.role,
      status: filters.status,
    })
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '用户加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', role: '', status: '' })
  loadUsers(1)
}

async function openEdit(user: User) {
  selected.value = user
  Object.assign(form, { role: user.role, status: user.status, remark: '' })
  modalOpen.value = true
  try {
    selected.value = await api.get<User>(`/api/users/${user.id}`)
  } catch {
    /* 保留列表数据 */
  }
}

async function saveAccess() {
  if (!selected.value) return
  saving.value = true
  try {
    await api.put(`/api/users/${selected.value.id}/status`, {
      role: form.role,
      status: form.status,
      remark: form.remark.trim() || undefined,
    })
    toast.success('用户权限与状态已更新')
    modalOpen.value = false
    await loadUsers(users.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '更新失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadUsers())
</script>

<template>
  <div class="page-wrap">
    <header class="page-header">
      <div>
        <p class="eyebrow dark">ACCESS CONTROL</p>
        <h1>用户管理</h1>
        <p>查询用户并维护角色与账号状态</p>
      </div>
    </header>
    <section class="filter-bar">
      <form class="search-box" @submit.prevent="loadUsers(1)">
        <span>⌕</span
        ><input v-model="filters.keyword" placeholder="搜索用户名、昵称、邮箱或手机号" /><button
          type="submit"
        >
          搜索
        </button>
      </form>
      <select v-model="filters.role" @change="loadUsers(1)">
        <option value="">全部角色</option>
        <option value="READER">读者</option>
        <option value="LIBRARIAN">图书管理员</option>
        <option value="ADMIN">系统管理员</option></select
      ><select v-model="filters.status" @change="loadUsers(1)">
        <option value="">全部状态</option>
        <option value="ACTIVE">正常</option>
        <option value="DISABLED">已停用</option>
        <option value="LOCKED">已锁定</option></select
      ><button class="btn btn-ghost btn-sm" type="button" @click="resetFilters">重置</button>
    </section>

    <section class="panel table-panel">
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>联系方式</th>
              <th>角色</th>
              <th>状态</th>
              <th>注册日期</th>
              <th class="align-right">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr v-for="n in 6" :key="n">
              <td colspan="6"><span class="skeleton table-line"></span></td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr v-for="user in users.records" :key="user.id">
              <td>
                <div class="user-cell">
                  <span class="avatar">{{ (user.nickname || user.username).slice(0, 1) }}</span
                  ><span
                    ><strong>{{ user.nickname || user.username }}</strong
                    ><small>@{{ user.username }}</small></span
                  >
                </div>
              </td>
              <td>
                <span>{{ user.email || '—' }}</span
                ><small class="cell-sub">{{ user.phone || '未填写手机号' }}</small>
              </td>
              <td>
                <span class="role-pill" :class="`role-${user.role.toLowerCase()}`">{{
                  roleLabels[user.role]
                }}</span>
              </td>
              <td>
                <span
                  class="status-badge"
                  :class="
                    user.status === 'ACTIVE'
                      ? 'badge-success'
                      : user.status === 'LOCKED'
                        ? 'badge-warning'
                        : 'badge-muted'
                  "
                  >{{ statusLabels[user.status] }}</span
                >
              </td>
              <td>{{ displayDate(user.createdAt) }}</td>
              <td>
                <div class="table-actions">
                  <button class="btn btn-ghost btn-sm" type="button" @click="openEdit(user)">
                    设置权限
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!users.records.length">
              <td colspan="6">
                <div class="table-empty">
                  <span>◎</span><strong>没有找到用户</strong>
                  <p>调整筛选条件后重试。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        :page="users.page"
        :pages="users.pages"
        :total="users.total"
        @change="loadUsers"
      />
    </section>

    <BaseModal :open="modalOpen" title="设置用户权限" width="560px" @close="modalOpen = false"
      ><template v-if="selected"
        ><div class="selected-user">
          <span class="avatar large">{{
            (selected.nickname || selected.username).slice(0, 1)
          }}</span>
          <div>
            <h3>{{ selected.nickname || selected.username }}</h3>
            <p>@{{ selected.username }} · {{ selected.email || '未填写邮箱' }}</p>
          </div>
        </div>
        <form class="form-stack" @submit.prevent="saveAccess">
          <div class="form-grid two">
            <label class="field"
              ><span>用户角色</span
              ><select v-model="form.role">
                <option value="READER">读者</option>
                <option value="LIBRARIAN">图书管理员</option>
                <option value="ADMIN">系统管理员</option>
              </select></label
            ><label class="field"
              ><span>账号状态</span
              ><select v-model="form.status">
                <option value="ACTIVE">正常</option>
                <option value="DISABLED">停用</option>
                <option value="LOCKED">锁定</option>
              </select></label
            >
          </div>
          <label class="field"
            ><span>变更备注</span
            ><textarea v-model="form.remark" rows="4" placeholder="选填，记录变更原因"></textarea>
          </label>
          <div class="warning-banner">
            角色和状态修改会立即影响该用户的访问权限，请确认后再保存。
          </div>
        </form></template
      ><template #footer
        ><button class="btn btn-ghost" type="button" @click="modalOpen = false">取消</button
        ><button class="btn btn-primary" type="button" :disabled="saving" @click="saveAccess">
          {{ saving ? '保存中…' : '保存设置' }}
        </button></template
      ></BaseModal
    >
  </div>
</template>
