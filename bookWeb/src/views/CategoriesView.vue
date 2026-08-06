<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useToast } from '@/stores/toast'
import type { Category, CategoryStatus } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'

const toast = useToast()
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const categories = ref<Category[]>([])
const modalOpen = ref(false)
const editing = ref<Category | null>(null)
const form = reactive({
  name: '',
  description: '',
  sortOrder: 0,
  status: 'ACTIVE' as CategoryStatus,
})

async function loadCategories() {
  loading.value = true
  try {
    categories.value = await api.get<Category[]>('/api/categories', {
      keyword: keyword.value.trim(),
      includeDisabled: true,
    })
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
    description: category.description || '',
    sortOrder: category.sortOrder || 0,
    status: category.status,
  })
  modalOpen.value = true
}

async function saveCategory() {
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
    await loadCategories()
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeCategory(category: Category) {
  if (!window.confirm(`确定删除分类“${category.name}”吗？`)) return
  try {
    await api.delete(`/api/categories/${category.id}`)
    toast.success('分类已删除')
    await loadCategories()
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '删除失败，该分类下可能仍有图书')
  }
}

onMounted(loadCategories)
</script>

<template>
  <div class="page-wrap">
    <header class="page-header">
      <div>
        <p class="eyebrow dark">CATALOG STRUCTURE</p>
        <h1>分类管理</h1>
        <p>维护馆藏分类及其展示顺序</p>
      </div>
      <button class="btn btn-primary" type="button" @click="openCreate">＋ 新增分类</button>
    </header>
    <section class="filter-bar compact">
      <form class="search-box" @submit.prevent="loadCategories">
        <span>⌕</span><input v-model="keyword" placeholder="搜索分类名称" /><button type="submit">
          搜索
        </button>
      </form>
      <span class="result-count">共 {{ categories.length }} 个分类</span>
    </section>

    <section v-if="loading" class="category-grid">
      <article v-for="n in 6" :key="n" class="category-card">
        <span class="skeleton line wide"></span><span class="skeleton line"></span>
      </article>
    </section>
    <section v-else-if="categories.length" class="category-grid">
      <article v-for="category in categories" :key="category.id" class="category-card">
        <div class="category-top">
          <span class="category-symbol">{{ category.name.slice(0, 1) }}</span
          ><span
            class="status-badge"
            :class="category.status === 'ACTIVE' ? 'badge-success' : 'badge-muted'"
            >{{ category.status === 'ACTIVE' ? '启用' : '停用' }}</span
          >
        </div>
        <h2>{{ category.name }}</h2>
        <p>{{ category.description || '暂无分类说明' }}</p>
        <div class="category-stats">
          <span
            ><strong>{{ category.bookCount }}</strong> 种图书</span
          ><span>排序 {{ category.sortOrder || 0 }}</span>
        </div>
        <div class="category-actions">
          <button class="btn btn-ghost btn-sm" type="button" @click="openEdit(category)">
            编辑</button
          ><button class="text-button danger-text" type="button" @click="removeCategory(category)">
            删除
          </button>
        </div>
      </article>
    </section>
    <section v-else class="empty-state">
      <div class="empty-books"><i></i><i></i><i></i></div>
      <h2>还没有分类</h2>
      <p>先创建分类，再录入馆藏图书。</p>
      <button class="btn btn-primary" type="button" @click="openCreate">新增分类</button>
    </section>

    <BaseModal
      :open="modalOpen"
      :title="editing ? '编辑分类' : '新增分类'"
      width="560px"
      @close="modalOpen = false"
    >
      <form class="form-stack" @submit.prevent="saveCategory">
        <label class="field"
          ><span>分类名称 <b>*</b></span
          ><input v-model="form.name" maxlength="50" placeholder="例如：计算机科学" /></label
        ><label class="field"
          ><span>分类说明</span
          ><textarea
            v-model="form.description"
            rows="4"
            placeholder="简要说明该分类收录范围"
          ></textarea>
        </label>
        <div class="form-grid two">
          <label class="field"
            ><span>排序值</span
            ><input v-model.number="form.sortOrder" type="number" min="0" /><small
              >数字越小越靠前</small
            ></label
          ><label class="field"
            ><span>状态</span
            ><select v-model="form.status">
              <option value="ACTIVE">启用</option>
              <option value="DISABLED">停用</option>
            </select></label
          >
        </div>
      </form>
      <template #footer
        ><button class="btn btn-ghost" type="button" @click="modalOpen = false">取消</button
        ><button class="btn btn-primary" type="button" :disabled="saving" @click="saveCategory">
          {{ saving ? '保存中…' : '保存分类' }}
        </button></template
      >
    </BaseModal>
  </div>
</template>
