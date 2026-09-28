<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/views/CategoriesView.vue
 * @Description: 图书分类维护，分类构成前台的分类导航
 */
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useToastStore } from '@/stores/toast'
import type { Category, CategoryStatus } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'

const toast = useToastStore()
const categories = ref<Category[]>([])
const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const editing = ref<Category | null>(null)
const form = reactive({
  name: '',
  description: '',
  sortOrder: 0,
  status: 'ACTIVE' as CategoryStatus,
})

async function load() {
  loading.value = true
  try {
    // includeDisabled=true：后台必须能看到停用分类，否则无法把它重新启用
    categories.value = await api.get<Category[]>('/api/categories', { includeDisabled: true })
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '分类加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', description: '', sortOrder: 0, status: 'ACTIVE' })
  modalOpen.value = true
}

function openEdit(category: Category) {
  editing.value = category
  Object.assign(form, {
    name: category.name,
    description: category.description ?? '',
    sortOrder: category.sortOrder ?? 0,
    status: category.status,
  })
  modalOpen.value = true
}

async function save() {
  if (!form.name.trim()) {
    toast.error('请填写分类名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      sortOrder: form.sortOrder,
      status: form.status,
    }
    if (editing.value) {
      await api.put(`/api/categories/${editing.value.id}`, payload)
      toast.success('分类已更新')
    } else {
      await api.post('/api/categories', payload)
      toast.success('分类已创建')
    }
    modalOpen.value = false
    await load()
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(category: Category) {
  // 分类下有书时后端会返回 409，这里只做二次确认，不做前端预判，避免与后端规则不一致
  if (!window.confirm(`确定删除分类「${category.name}」吗？`)) return
  try {
    await api.delete(`/api/categories/${category.id}`)
    toast.success('分类已删除')
    await load()
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h3>分类列表</h3>
        <p>停用分类不会出现在前台导航，但其下图书仍可被检索</p>
      </div>
      <button class="btn btn-primary" type="button" @click="openCreate">新增分类</button>
    </div>

    <div class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>分类名称</th>
            <th>描述</th>
            <th>排序权重</th>
            <th>图书数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="category in categories" :key="category.id">
            <td>{{ category.name }}</td>
            <td>{{ category.description || '—' }}</td>
            <td>{{ category.sortOrder ?? 0 }}</td>
            <td>{{ category.bookCount }}</td>
            <td>
              <span
                class="badge"
                :class="category.status === 'ACTIVE' ? 'badge-success' : 'badge-muted'"
              >
                {{ category.status === 'ACTIVE' ? '启用' : '停用' }}
              </span>
            </td>
            <td>
              <div class="cell-actions">
                <button class="btn btn-sm" type="button" @click="openEdit(category)">编辑</button>
                <button class="btn btn-sm btn-danger" type="button" @click="remove(category)">
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!categories.length">
            <td colspan="6" class="empty">{{ loading ? '加载中…' : '暂无分类' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <BaseModal
    :open="modalOpen"
    :title="editing ? '编辑分类' : '新增分类'"
    @close="modalOpen = false"
  >
    <form @submit.prevent="save">
      <label class="field">
        <span>分类名称</span>
        <input v-model="form.name" maxlength="50" />
      </label>
      <label class="field">
        <span>描述</span>
        <textarea v-model="form.description" maxlength="500" />
      </label>
      <div class="form-grid">
        <label class="field">
          <span>排序权重</span>
          <input v-model.number="form.sortOrder" type="number" />
          <small>数值越小越靠前</small>
        </label>
        <label class="field">
          <span>状态</span>
          <select v-model="form.status">
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">停用</option>
          </select>
        </label>
      </div>
      <div class="modal-foot">
        <button class="btn" type="button" @click="modalOpen = false">取消</button>
        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>
