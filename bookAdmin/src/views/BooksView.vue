<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/views/BooksView.vue
 * @Description: 馆藏管理：上下架、资料维护与库存调整
 */
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useToastStore } from '@/stores/toast'
import type { Book, BookStatus, Category, PageData } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const toast = useToastStore()
const books = ref<PageData<Book>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })
const categories = ref<Category[]>([])
const loading = ref(false)
const saving = ref(false)

const filters = reactive({
  keyword: '',
  categoryId: '' as string,
  status: '' as '' | BookStatus,
  availability: '',
})

const formOpen = ref(false)
const editing = ref<Book | null>(null)
const bookForm = reactive({
  title: '',
  author: '',
  isbn: '',
  publisher: '',
  publishDate: '',
  description: '',
  coverUrl: '',
  categoryId: null as number | null,
  totalStock: 1,
  status: 'ACTIVE' as BookStatus,
})

const stockOpen = ref(false)
const stockTarget = ref<Book | null>(null)
const stockForm = reactive({ change: 1, reason: '' })

async function loadCategories() {
  try {
    categories.value = await api.get<Category[]>('/api/categories', { includeDisabled: true })
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '分类加载失败')
  }
}

async function loadBooks(page = 1) {
  loading.value = true
  try {
    const data = await api.get<PageData<Book>>('/api/books', {
      page,
      size: 10,
      keyword: filters.keyword.trim(),
      categoryId: filters.categoryId,
      status: filters.status,
      availability: filters.availability,
    })
    // 下架末页最后一本后总页数减少，页码会越界；不回退会停在空白页
    if (data.pages > 0 && page > data.pages) {
      await loadBooks(data.pages)
      return
    }
    books.value = data
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '馆藏加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', categoryId: '', status: '', availability: '' })
  loadBooks(1)
}

function openCreate() {
  editing.value = null
  Object.assign(bookForm, {
    title: '',
    author: '',
    isbn: '',
    publisher: '',
    publishDate: '',
    description: '',
    coverUrl: '',
    categoryId: null,
    totalStock: 1,
    status: 'ACTIVE',
  })
  formOpen.value = true
}

function openEdit(book: Book) {
  editing.value = book
  Object.assign(bookForm, {
    title: book.title,
    author: book.author,
    isbn: book.isbn,
    publisher: book.publisher ?? '',
    publishDate: book.publishDate ?? '',
    description: book.description ?? '',
    coverUrl: book.coverUrl ?? '',
    categoryId: book.category.id,
    totalStock: book.totalStock,
    status: book.status,
  })
  formOpen.value = true
}

async function saveBook() {
  if (!bookForm.title.trim() || !bookForm.author.trim() || !bookForm.isbn.trim()) {
    toast.error('请填写书名、作者与 ISBN')
    return
  }
  // 契约要求 ISBN 为 10~20 位（去连字符），前端先拦一道，避免提交后只有一句 400
  if (bookForm.isbn.trim().length < 10 || bookForm.isbn.trim().length > 20) {
    toast.error('ISBN 长度需为 10 到 20 个字符')
    return
  }
  if (!bookForm.categoryId) {
    toast.error('请选择图书分类')
    return
  }
  // 编辑时 totalStock 不可改（库存走调整接口并留痕），因此只在新书时校验
  if (!editing.value && (!Number.isInteger(bookForm.totalStock)
    || bookForm.totalStock < 0 || bookForm.totalStock > 100000)) {
    toast.error('馆藏册数需为 0 到 100000 之间的整数')
    return
  }

  saving.value = true
  try {
    const payload = {
      title: bookForm.title.trim(),
      author: bookForm.author.trim(),
      isbn: bookForm.isbn.trim(),
      publisher: bookForm.publisher.trim() || undefined,
      publishDate: bookForm.publishDate || undefined,
      description: bookForm.description.trim() || undefined,
      coverUrl: bookForm.coverUrl.trim() || undefined,
      categoryId: bookForm.categoryId,
      status: bookForm.status,
    }
    if (editing.value) {
      await api.put(`/api/books/${editing.value.id}`, payload)
      toast.success('图书资料已更新')
    } else {
      await api.post('/api/books', { ...payload, totalStock: bookForm.totalStock })
      toast.success('图书已入库')
    }
    formOpen.value = false
    await loadBooks(editing.value ? books.value.page : 1)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

/** 上下架是独立动作：它直接决定图书是否对读者可见，不必打开完整表单 */
async function toggleStatus(book: Book) {
  const next: BookStatus = book.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await api.put(`/api/books/${book.id}`, { status: next })
    toast.success(next === 'ACTIVE' ? '已重新上架' : '已下架')
    await loadBooks(books.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function remove(book: Book) {
  if (!window.confirm(`确定下架并删除《${book.title}》吗？存在未完成借阅时会被拒绝。`)) return
  try {
    await api.delete(`/api/books/${book.id}`)
    toast.success('图书已下架')
    await loadBooks(books.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '删除失败')
  }
}

function openStock(book: Book) {
  stockTarget.value = book
  Object.assign(stockForm, { change: 1, reason: '' })
  stockOpen.value = true
}

async function saveStock() {
  if (!stockTarget.value) return
  // change 必须是非零整数：v-model.number 在输入 1.5 时写入小数、清空输入时写入空串，
  // 只判 0 与空值会把小数放行，最终被后端 400 拒绝
  if (!Number.isInteger(stockForm.change) || stockForm.change === 0) {
    toast.error('调整数量需为非零整数')
    return
  }
  const reason = stockForm.reason.trim()
  if (reason.length < 2 || reason.length > 200) {
    toast.error('调整原因长度需为 2 到 200 个字符')
    return
  }
  saving.value = true
  try {
    await api.patch(`/api/books/${stockTarget.value.id}/stock`, {
      change: stockForm.change,
      reason,
    })
    toast.success('库存已调整')
    stockOpen.value = false
    await loadBooks(books.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '库存调整失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => Promise.all([loadCategories(), loadBooks()]))
</script>

<template>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h3>馆藏列表</h3>
        <p>下架的图书对读者不可见，上下架操作会记入审计日志</p>
      </div>
      <button class="btn btn-primary" type="button" @click="openCreate">新增图书</button>
    </div>

    <div class="filter-bar">
      <input v-model="filters.keyword" placeholder="书名 / 作者 / ISBN / 出版社" @keyup.enter="loadBooks(1)" />
      <select v-model="filters.categoryId" @change="loadBooks(1)">
        <option value="">全部分类</option>
        <option v-for="category in categories" :key="category.id" :value="String(category.id)">
          {{ category.name }}
        </option>
      </select>
      <select v-model="filters.status" @change="loadBooks(1)">
        <option value="">全部状态</option>
        <option value="ACTIVE">已上架</option>
        <option value="INACTIVE">已下架</option>
      </select>
      <select v-model="filters.availability" @change="loadBooks(1)">
        <option value="">不限库存</option>
        <option value="AVAILABLE">有库存</option>
        <option value="UNAVAILABLE">无库存</option>
      </select>
      <button class="btn" type="button" @click="loadBooks(1)">搜索</button>
      <button class="btn" type="button" @click="resetFilters">重置</button>
    </div>

    <div class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>图书</th>
            <th>分类</th>
            <th>库存</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="book in books.records" :key="book.id">
            <td>《{{ book.title }}》<br /><small>{{ book.author }} · {{ book.isbn }}</small></td>
            <td>{{ book.category.name }}</td>
            <td>{{ book.availableStock }} / {{ book.totalStock }}</td>
            <td>
              <span class="badge" :class="book.status === 'ACTIVE' ? 'badge-success' : 'badge-muted'">
                {{ book.status === 'ACTIVE' ? '已上架' : '已下架' }}
              </span>
            </td>
            <td>
              <div class="cell-actions">
                <button class="btn btn-sm" type="button" @click="openEdit(book)">编辑</button>
                <button class="btn btn-sm" type="button" @click="toggleStatus(book)">
                  {{ book.status === 'ACTIVE' ? '下架' : '上架' }}
                </button>
                <button class="btn btn-sm" type="button" @click="openStock(book)">库存</button>
                <button class="btn btn-sm btn-danger" type="button" @click="remove(book)">
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!books.records.length">
            <td colspan="5" class="empty">{{ loading ? '加载中…' : '未找到匹配的图书' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <PaginationBar
      :page="books.page"
      :pages="books.pages"
      :total="books.total"
      @update:page="loadBooks"
    />
  </section>

  <BaseModal
    :open="formOpen"
    :title="editing ? '编辑图书' : '新增图书'"
    @close="formOpen = false"
  >
    <form @submit.prevent="saveBook">
      <div class="form-grid">
        <label class="field">
          <span>书名</span>
          <input v-model="bookForm.title" maxlength="200" />
        </label>
        <label class="field">
          <span>作者</span>
          <input v-model="bookForm.author" maxlength="100" />
        </label>
      </div>
      <div class="form-grid">
        <label class="field">
          <span>ISBN</span>
          <input v-model="bookForm.isbn" maxlength="20" />
          <small>10~20 位，不含连字符</small>
        </label>
        <label class="field">
          <span>出版社</span>
          <input v-model="bookForm.publisher" maxlength="100" />
        </label>
      </div>
      <div class="form-grid">
        <label class="field">
          <span>分类</span>
          <select v-model="bookForm.categoryId">
            <option :value="null">请选择</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>状态</span>
          <select v-model="bookForm.status">
            <option value="ACTIVE">已上架</option>
            <option value="INACTIVE">已下架</option>
          </select>
        </label>
      </div>
      <label v-if="!editing" class="field">
        <span>馆藏册数</span>
        <input v-model.number="bookForm.totalStock" type="number" min="0" max="100000" />
        <small>编辑图书时不可在此调整，请走库存调整并留痕</small>
      </label>
      <div class="form-grid">
        <label class="field">
          <span>出版日期</span>
          <input v-model="bookForm.publishDate" type="date" />
        </label>
        <label class="field">
          <span>封面地址</span>
          <input v-model="bookForm.coverUrl" maxlength="500" placeholder="https://…" />
        </label>
      </div>
      <label class="field">
        <span>简介</span>
        <textarea v-model="bookForm.description" maxlength="5000" />
      </label>
      <div class="modal-foot">
        <button class="btn" type="button" @click="formOpen = false">取消</button>
        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </form>
  </BaseModal>

  <BaseModal :open="stockOpen" title="调整库存" @close="stockOpen = false">
    <form v-if="stockTarget" @submit.prevent="saveStock">
      <p style="margin-bottom: 12px">
        《{{ stockTarget.title }}》 当前 {{ stockTarget.availableStock }} / {{ stockTarget.totalStock }}
      </p>
      <label class="field">
        <span>调整数量</span>
        <input v-model.number="stockForm.change" type="number" />
        <small>正数入库，负数出库，不能为 0</small>
      </label>
      <label class="field">
        <span>调整原因</span>
        <input v-model="stockForm.reason" maxlength="200" placeholder="如：新书采购 / 破损报废" />
      </label>
      <div class="modal-foot">
        <button class="btn" type="button" @click="stockOpen = false">取消</button>
        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '提交中…' : '确认调整' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>
