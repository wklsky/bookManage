<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/views/FeaturedBooksView.vue
 * @Description: 前台首页推荐位维护，控制前台展示哪些书及其顺序
 */
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useToastStore } from '@/stores/toast'
import type { Book, FeaturedBook, PageData } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const toast = useToastStore()
const featured = ref<PageData<FeaturedBook>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })
const loading = ref(false)
const saving = ref(false)
const enabledOnly = ref(false)

/** 新增弹窗：先检索图书再选中，避免在一个下拉里塞几千条书目 */
const addOpen = ref(false)
const searchKeyword = ref('')
const candidates = ref<Book[]>([])
const picked = ref<Book | null>(null)
const addForm = reactive({ position: 10, remark: '', enabled: true })

const editOpen = ref(false)
const editing = ref<FeaturedBook | null>(null)
const editForm = reactive({ position: 0, remark: '', enabled: true })

async function load(page = 1) {
  loading.value = true
  try {
    const data = await api.get<PageData<FeaturedBook>>('/api/admin/featured-books', {
      page,
      size: 10,
      enabledOnly: enabledOnly.value,
    })
    // 移除末页最后一条后总页数减少，页码会越界；不回退会停在空白页
    if (data.pages > 0 && page > data.pages) {
      await load(data.pages)
      return
    }
    featured.value = data
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '推荐位加载失败')
  } finally {
    loading.value = false
  }
}

async function searchBooks() {
  try {
    const page = await api.get<PageData<Book>>('/api/books', {
      keyword: searchKeyword.value.trim(),
      status: 'ACTIVE',
      size: 10,
    })
    candidates.value = page.records
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '图书检索失败')
  }
}

function openAdd() {
  picked.value = null
  searchKeyword.value = ''
  candidates.value = []
  Object.assign(addForm, { position: 10, remark: '', enabled: true })
  addOpen.value = true
}

function openEdit(item: FeaturedBook) {
  editing.value = item
  Object.assign(editForm, {
    position: item.position,
    remark: item.remark ?? '',
    enabled: item.enabled,
  })
  editOpen.value = true
}

async function saveAdd() {
  if (!picked.value) {
    toast.error('请先检索并选中一本图书')
    return
  }
  if (!Number.isInteger(addForm.position)) {
    toast.error('排序权重需为整数')
    return
  }
  saving.value = true
  try {
    await api.post('/api/admin/featured-books', {
      bookId: picked.value.id,
      position: addForm.position,
      remark: addForm.remark.trim() || undefined,
      enabled: addForm.enabled,
    })
    toast.success('推荐位已添加')
    addOpen.value = false
    await load(1)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '添加失败')
  } finally {
    saving.value = false
  }
}

async function saveEdit() {
  if (!editing.value) return
  if (!Number.isInteger(editForm.position)) {
    toast.error('排序权重需为整数')
    return
  }
  saving.value = true
  try {
    await api.put(`/api/admin/featured-books/${editing.value.id}`, {
      position: editForm.position,
      remark: editForm.remark.trim() || undefined,
      enabled: editForm.enabled,
    })
    toast.success('推荐位已更新')
    editOpen.value = false
    await load(featured.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '更新失败')
  } finally {
    saving.value = false
  }
}

async function toggle(item: FeaturedBook) {
  try {
    await api.put(`/api/admin/featured-books/${item.id}`, { enabled: !item.enabled })
    toast.success(item.enabled ? '已从前台隐藏' : '已在前台展示')
    await load(featured.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function remove(item: FeaturedBook) {
  if (!window.confirm(`确定移除《${item.title}》的推荐位吗？`)) return
  try {
    await api.delete(`/api/admin/featured-books/${item.id}`)
    toast.success('推荐位已移除')
    await load(featured.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '移除失败')
  }
}

onMounted(() => load())
</script>

<template>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h3>推荐位列表</h3>
        <p>权重越小越靠前；仅「已启用 + 图书已上架」的条目会出现在前台首页</p>
      </div>
      <div class="cell-actions">
        <label style="display: flex; align-items: center; gap: 6px; font-size: 13px">
          <input v-model="enabledOnly" type="checkbox" @change="load(1)" />只看已启用
        </label>
        <button class="btn btn-primary" type="button" @click="openAdd">添加推荐位</button>
      </div>
    </div>

    <div class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>权重</th>
            <th>图书</th>
            <th>库存</th>
            <th>图书状态</th>
            <th>前台展示</th>
            <th>推荐语</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in featured.records" :key="item.id">
            <td>{{ item.position }}</td>
            <td>《{{ item.title }}》<br /><small>{{ item.author }}</small></td>
            <td>{{ item.availableStock }} / {{ item.totalStock }}</td>
            <td>
              <span
                class="badge"
                :class="item.bookStatus === 'ACTIVE' ? 'badge-success' : 'badge-muted'"
              >
                {{ item.bookStatus === 'ACTIVE' ? '上架' : '下架' }}
              </span>
            </td>
            <td>
              <span class="badge" :class="item.enabled ? 'badge-info' : 'badge-muted'">
                {{ item.enabled ? '展示中' : '已隐藏' }}
              </span>
            </td>
            <td>{{ item.remark || '—' }}</td>
            <td>
              <div class="cell-actions">
                <button class="btn btn-sm" type="button" @click="openEdit(item)">编辑</button>
                <button class="btn btn-sm" type="button" @click="toggle(item)">
                  {{ item.enabled ? '隐藏' : '展示' }}
                </button>
                <button class="btn btn-sm btn-danger" type="button" @click="remove(item)">
                  移除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!featured.records.length">
            <td colspan="7" class="empty">{{ loading ? '加载中…' : '暂无推荐位' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <PaginationBar
      :page="featured.page"
      :pages="featured.pages"
      :total="featured.total"
      @update:page="load"
    />
  </section>

  <BaseModal :open="addOpen" title="添加推荐位" @close="addOpen = false">
    <div class="filter-bar">
      <input v-model="searchKeyword" placeholder="检索书名 / 作者 / ISBN" @keyup.enter="searchBooks" />
      <button class="btn" type="button" @click="searchBooks">检索</button>
    </div>
    <div class="table-wrap" style="max-height: 220px; overflow-y: auto">
      <table class="data">
        <tbody>
          <tr
            v-for="book in candidates"
            :key="book.id"
            :style="picked?.id === book.id ? { background: 'var(--brand-soft)' } : undefined"
          >
            <td>《{{ book.title }}》</td>
            <td>{{ book.author }}</td>
            <td>{{ book.availableStock }} / {{ book.totalStock }}</td>
            <td>
              <button class="btn btn-sm" type="button" @click="picked = book">选择</button>
            </td>
          </tr>
          <tr v-if="!candidates.length">
            <td class="empty">输入关键词后检索可加入的图书</td>
          </tr>
        </tbody>
      </table>
    </div>

    <form @submit.prevent="saveAdd">
      <label class="field" style="margin-top: 14px">
        <span>已选中</span>
        <input :value="picked ? `《${picked.title}》 · ${picked.author}` : '未选择'" disabled />
      </label>
      <div class="form-grid">
        <label class="field">
          <span>排序权重</span>
          <input v-model.number="addForm.position" type="number" />
          <small>数值越小越靠前</small>
        </label>
        <label class="field">
          <span>前台展示</span>
          <select v-model="addForm.enabled">
            <option :value="true">展示</option>
            <option :value="false">隐藏</option>
          </select>
        </label>
      </div>
      <label class="field">
        <span>推荐语</span>
        <input v-model="addForm.remark" maxlength="200" placeholder="选填，可展示在前台" />
      </label>
      <div class="modal-foot">
        <button class="btn" type="button" @click="addOpen = false">取消</button>
        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '提交中…' : '添加' }}
        </button>
      </div>
    </form>
  </BaseModal>

  <BaseModal :open="editOpen" title="编辑推荐位" @close="editOpen = false">
    <form v-if="editing" @submit.prevent="saveEdit">
      <p style="margin-bottom: 12px">《{{ editing.title }}》</p>
      <div class="form-grid">
        <label class="field">
          <span>排序权重</span>
          <input v-model.number="editForm.position" type="number" />
        </label>
        <label class="field">
          <span>前台展示</span>
          <select v-model="editForm.enabled">
            <option :value="true">展示</option>
            <option :value="false">隐藏</option>
          </select>
        </label>
      </div>
      <label class="field">
        <span>推荐语</span>
        <input v-model="editForm.remark" maxlength="200" />
      </label>
      <div class="modal-foot">
        <button class="btn" type="button" @click="editOpen = false">取消</button>
        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>
