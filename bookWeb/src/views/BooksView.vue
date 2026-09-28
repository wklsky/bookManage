<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'
import type { Book, BookForm, Category, FeaturedBook, PageData } from '@/types/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const auth = useAuthStore()
const toast = useToast()
const loading = ref(false)
const actionLoading = ref(false)
const categories = ref<Category[]>([])
const books = ref<PageData<Book>>({ page: 1, size: 10, total: 0, pages: 0, records: [] })
const filters = reactive({
  keyword: '',
  categoryId: '',
  availability: '',
  status: auth.isManager ? '' : 'ACTIVE',
})
const modal = ref<'detail' | 'form' | 'stock' | 'reserve' | null>(null)
const selected = ref<Book | null>(null)
const editing = ref(false)
const stockForm = reactive({ change: 1, reason: '' })
const reserveRemark = ref('')
const bookForm = reactive<BookForm>(emptyBook())

function emptyBook(): BookForm {
  return {
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
  }
}

const modalTitle = computed(() => {
  if (modal.value === 'detail') return '图书详情'
  if (modal.value === 'stock') return '调整库存'
  if (modal.value === 'reserve') return '确认预约'
  return editing.value ? '编辑图书' : '新增图书'
})

function displayDate(value?: string) {
  return value ? new Intl.DateTimeFormat('zh-CN').format(new Date(value)) : '—'
}

/** 用图书 ID 生成稳定的封面渐变色，没有封面图时也能区分不同书目 */
function coverStyle(seed: { id: number }) {
  const hue = (seed.id * 47) % 360
  return { background: `linear-gradient(145deg, hsl(${hue} 35% 38%), hsl(${hue + 28} 42% 25%))` }
}

async function loadCategories() {
  try {
    categories.value = await api.get<Category[]>('/api/categories', {
      includeDisabled: auth.isManager,
    })
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
      availability: filters.availability,
      status: filters.status,
    })
    // 下架末页最后一本后总页数减少，当前页码可能已越界；不回退会停在空白页
    if (data.pages > 0 && page > data.pages) {
      await loadBooks(data.pages)
      return
    }
    books.value = data
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '图书加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = ''
  filters.availability = ''
  filters.status = auth.isManager ? '' : 'ACTIVE'
  loadBooks(1)
}

async function openDetail(book: Book) {
  selected.value = book
  modal.value = 'detail'
  try {
    selected.value = await api.get<Book>(`/api/books/${book.id}`)
  } catch {
    // 列表数据足够展示，详情请求失败时保留当前内容。
  }
}

function openCreate() {
  editing.value = false
  Object.assign(bookForm, emptyBook())
  modal.value = 'form'
}

function openEdit(book: Book) {
  editing.value = true
  selected.value = book
  Object.assign(bookForm, {
    title: book.title,
    author: book.author,
    isbn: book.isbn,
    publisher: book.publisher || '',
    publishDate: book.publishDate || '',
    description: book.description || '',
    coverUrl: book.coverUrl || '',
    categoryId: book.category.id,
    totalStock: book.totalStock,
    status: book.status,
  })
  modal.value = 'form'
}

function openStock(book: Book) {
  selected.value = book
  stockForm.change = 1
  stockForm.reason = ''
  modal.value = 'stock'
}

function openReserve(book: Book) {
  selected.value = book
  reserveRemark.value = ''
  modal.value = 'reserve'
}

async function saveBook() {
  if (
    !bookForm.title.trim() ||
    !bookForm.author.trim() ||
    !bookForm.isbn.trim() ||
    !bookForm.categoryId
  ) {
    toast.error('请填写书名、作者、ISBN 和分类')
    return
  }
  // 契约规定 totalStock 为 0~100000 的整数。v-model.number 在输入「1.5」时写入小数、清空输入时写入空串，
  // 原校验完全没覆盖，小数会直接提交并被后端 400 拒绝，提示也不明确，故在此拦截。
  if (
    !editing.value &&
    (!Number.isInteger(bookForm.totalStock) ||
      bookForm.totalStock < 0 ||
      bookForm.totalStock > 100000)
  ) {
    toast.error('馆藏册数需为 0 到 100000 之间的整数')
    return
  }
  actionLoading.value = true
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
    if (editing.value && selected.value) {
      await api.put(`/api/books/${selected.value.id}`, payload)
      toast.success('图书资料已更新')
    } else {
      await api.post('/api/books', { ...payload, totalStock: bookForm.totalStock })
      toast.success('图书已新增')
    }
    modal.value = null
    await refreshBooks(editing.value ? books.value.page : 1)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    actionLoading.value = false
  }
}

async function adjustStock() {
  // change 必须是非零整数：v-model.number 在输入「1.5」时写入小数、清空输入时写入空串，
  // 原判断只挡住了 0 与空值，小数仍会被提交并触发后端 400。
  if (!selected.value || !Number.isInteger(stockForm.change) || stockForm.change === 0) {
    toast.error('调整数量需为非零整数')
    return
  }
  if (!stockForm.reason.trim()) {
    toast.error('请填写调整原因')
    return
  }
  actionLoading.value = true
  try {
    await api.patch(`/api/books/${selected.value.id}/stock`, {
      change: stockForm.change,
      reason: stockForm.reason.trim(),
    })
    toast.success('库存已调整')
    modal.value = null
    await refreshBooks(books.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '库存调整失败')
  } finally {
    actionLoading.value = false
  }
}

async function reserveBook() {
  if (!selected.value) return
  actionLoading.value = true
  try {
    await api.post('/api/orders/reserve', {
      bookId: selected.value.id,
      remark: reserveRemark.value.trim() || undefined,
    })
    toast.success('预约已提交，请留意审核状态')
    modal.value = null
    await refreshBooks(books.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '预约失败')
  } finally {
    actionLoading.value = false
  }
}

async function removeBook(book: Book) {
  if (!window.confirm(`确定下架并删除《${book.title}》吗？`)) return
  try {
    await api.delete(`/api/books/${book.id}`)
    toast.success('图书已下架')
    await refreshBooks(books.value.page)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '删除失败')
  }
}

const featured = ref<FeaturedBook[]>([])

async function loadFeatured() {
  try {
    featured.value = await api.get<FeaturedBook[]>('/api/featured-books')
  } catch {
    // 推荐位是增强展示项，读取失败时置空即可，不能影响检索主流程
    featured.value = []
  }
}

/**
 * 图书发生写操作后同时刷新列表与推荐位。
 *
 * 推荐位冗余了书名、库存与上下架状态快照。只刷列表的话，管理端在本书页把某本书
 * 下架或删除后，顶部推荐区仍会展示这本书（后端已清推荐位，前端却是旧快照），
 * 读者点进去就会拿到 404；库存调整同理，可借册数会停在旧值。
 */
async function refreshBooks(page: number) {
  await Promise.all([loadBooks(page), loadFeatured()])
}

/** 推荐位只冗余图书 ID，仍需按 ID 取完整书目后才能复用详情弹窗 */
async function openFeatured(item: FeaturedBook) {
  try {
    const book = await api.get<Book>(`/api/books/${item.bookId}`)
    await openDetail(book)
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '图书详情加载失败')
  }
}

onMounted(() => Promise.all([loadCategories(), loadBooks(), loadFeatured()]))
</script>

<template>
  <div class="page-wrap">
    <header class="page-header">
      <div>
        <p class="eyebrow dark">COLLECTION CATALOG</p>
        <h1>{{ auth.isReader ? '发现馆藏' : '图书管理' }}</h1>
        <p>{{ auth.isReader ? '检索馆藏并预约你想阅读的书' : '维护图书资料、上下架状态与库存' }}</p>
      </div>
      <button v-if="auth.isManager" class="btn btn-primary" type="button" @click="openCreate">
        ＋ 新增图书
      </button>
    </header>

    <section v-if="featured.length" class="featured-strip">
      <div class="strip-head">
        <h2>编辑推荐</h2>
        <p>管理后台编排的首页展示位</p>
      </div>
      <div class="strip-scroll">
        <article
          v-for="item in featured"
          :key="item.id"
          class="strip-card"
          role="button"
          tabindex="0"
          @click="openFeatured(item)"
          @keyup.enter="openFeatured(item)"
        >
          <div class="strip-cover" :style="coverStyle({ id: item.bookId })">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
          </div>
          <strong>{{ item.title }}</strong>
          <small>{{ item.author }}</small>
          <p v-if="item.remark" class="strip-remark">{{ item.remark }}</p>
          <span class="status-badge" :class="item.availableStock > 0 ? 'badge-success' : 'badge-muted'">
            {{ item.availableStock > 0 ? `可借 ${item.availableStock}` : '暂无库存' }}
          </span>
        </article>
      </div>
    </section>

    <section class="filter-bar">
      <form class="search-box" @submit.prevent="loadBooks(1)">
        <span>⌕</span><input v-model="filters.keyword" placeholder="搜索书名、作者或 ISBN" /><button
          type="submit"
        >
          搜索
        </button>
      </form>
      <select v-model="filters.categoryId" aria-label="图书分类" @change="loadBooks(1)">
        <option value="">全部分类</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">
          {{ category.name }}
        </option>
      </select>
      <select v-model="filters.availability" aria-label="库存状态" @change="loadBooks(1)">
        <option value="">全部库存</option>
        <option value="AVAILABLE">有可借库存</option>
        <option value="UNAVAILABLE">暂无库存</option>
      </select>
      <select
        v-if="auth.isManager"
        v-model="filters.status"
        aria-label="上架状态"
        @change="loadBooks(1)"
      >
        <option value="">全部状态</option>
        <option value="ACTIVE">已上架</option>
        <option value="INACTIVE">已下架</option>
      </select>
      <button class="btn btn-ghost btn-sm" type="button" @click="resetFilters">重置</button>
    </section>

    <div v-if="loading" class="book-grid">
      <article v-for="n in 8" :key="n" class="book-card skeleton-card">
        <span class="skeleton cover"></span>
        <div>
          <span class="skeleton line wide"></span><span class="skeleton line"></span
          ><span class="skeleton line short"></span>
        </div>
      </article>
    </div>

    <template v-else-if="books.records.length">
      <section class="book-grid">
        <article v-for="book in books.records" :key="book.id" class="book-card">
          <button
            class="book-cover"
            type="button"
            :style="coverStyle(book)"
            @click="openDetail(book)"
          >
            <img
              v-if="book.coverUrl"
              :src="book.coverUrl"
              :alt="`${book.title}封面`"
              @error="($event.currentTarget as HTMLImageElement).style.display = 'none'"
            />
            <span>{{ book.title.slice(0, 8) }}</span
            ><small>{{ book.author }}</small>
          </button>
          <div class="book-info">
            <div class="book-meta">
              <span>{{ book.category.name }}</span
              ><i :class="book.status === 'ACTIVE' ? 'status-active' : 'status-muted'">{{
                book.status === 'ACTIVE' ? '在架' : '下架'
              }}</i>
            </div>
            <button class="book-title" type="button" @click="openDetail(book)">
              {{ book.title }}
            </button>
            <p>{{ book.author || '未知作者' }} · {{ book.publisher || '出版社未录入' }}</p>
            <div class="stock-line">
              <span
                >可借 <strong>{{ book.availableStock }}</strong> / {{ book.totalStock }} 册</span
              ><i
                ><b
                  :style="{
                    width: `${book.totalStock ? Math.round((book.availableStock / book.totalStock) * 100) : 0}%`,
                  }"
                ></b
              ></i>
            </div>
            <div class="card-actions">
              <template v-if="auth.isReader"
                ><button
                  class="btn btn-primary btn-sm"
                  type="button"
                  :disabled="book.availableStock <= 0 || book.status !== 'ACTIVE'"
                  @click="openReserve(book)"
                >
                  {{ book.availableStock > 0 ? '预约借阅' : '暂无库存' }}</button
                ><button class="btn btn-ghost btn-sm" type="button" @click="openDetail(book)">
                  详情
                </button></template
              >
              <template v-else
                ><button class="btn btn-ghost btn-sm" type="button" @click="openEdit(book)">
                  编辑</button
                ><button class="btn btn-ghost btn-sm" type="button" @click="openStock(book)">
                  库存</button
                ><button
                  class="icon-button danger"
                  type="button"
                  title="删除"
                  @click="removeBook(book)"
                >
                  ×
                </button></template
              >
            </div>
          </div>
        </article>
      </section>
      <PaginationBar
        :page="books.page"
        :pages="books.pages"
        :total="books.total"
        @change="loadBooks"
      />
    </template>

    <section v-else class="empty-state">
      <div class="empty-books"><i></i><i></i><i></i></div>
      <h2>没有找到图书</h2>
      <p>调整筛选条件，或{{ auth.isManager ? '录入一本新书' : '换个关键词再试试' }}。</p>
      <button v-if="auth.isManager" class="btn btn-primary" type="button" @click="openCreate">
        新增图书
      </button>
    </section>

    <BaseModal
      :open="Boolean(modal)"
      :title="modalTitle"
      :width="modal === 'detail' ? '760px' : '640px'"
      @close="modal = null"
    >
      <template v-if="modal === 'detail' && selected">
        <div class="book-detail">
          <div class="detail-cover" :style="coverStyle(selected)">
            <img v-if="selected.coverUrl" :src="selected.coverUrl" alt="" /><span>{{
              selected.title
            }}</span>
          </div>
          <div class="detail-copy">
            <span class="tag">{{ selected.category.name }}</span>
            <h3>{{ selected.title }}</h3>
            <p class="detail-author">
              {{ selected.author }} · {{ selected.publisher || '出版社未录入' }}
            </p>
            <dl>
              <div>
                <dt>ISBN</dt>
                <dd>{{ selected.isbn }}</dd>
              </div>
              <div>
                <dt>出版日期</dt>
                <dd>{{ displayDate(selected.publishDate) }}</dd>
              </div>
              <div>
                <dt>馆藏库存</dt>
                <dd>{{ selected.availableStock }} 可借 / {{ selected.totalStock }} 总册</dd>
              </div>
              <div>
                <dt>状态</dt>
                <dd>{{ selected.status === 'ACTIVE' ? '已上架' : '已下架' }}</dd>
              </div>
            </dl>
            <p class="description">{{ selected.description || '暂无内容简介。' }}</p>
          </div>
        </div>
      </template>
      <template v-else-if="modal === 'form'">
        <form class="form-stack" @submit.prevent="saveBook">
          <div class="form-grid two">
            <label class="field"
              ><span>书名 <b>*</b></span
              ><input v-model="bookForm.title" /></label
            ><label class="field"
              ><span>作者 <b>*</b></span
              ><input v-model="bookForm.author"
            /></label>
          </div>
          <div class="form-grid two">
            <label class="field"
              ><span>ISBN <b>*</b></span
              ><input v-model="bookForm.isbn" /></label
            ><label class="field"
              ><span>分类 <b>*</b></span
              ><select v-model.number="bookForm.categoryId">
                <option :value="null" disabled>选择分类</option>
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select></label
            >
          </div>
          <div class="form-grid two">
            <label class="field"><span>出版社</span><input v-model="bookForm.publisher" /></label
            ><label class="field"
              ><span>出版日期</span><input v-model="bookForm.publishDate" type="date"
            /></label>
          </div>
          <div class="form-grid two">
            <label v-if="!editing" class="field"
              ><span>初始库存 <b>*</b></span
              ><input v-model.number="bookForm.totalStock" type="number" min="0" /></label
            ><label class="field"
              ><span>上架状态</span
              ><select v-model="bookForm.status">
                <option value="ACTIVE">已上架</option>
                <option value="INACTIVE">已下架</option>
              </select></label
            >
          </div>
          <label class="field"
            ><span>封面地址</span
            ><input v-model="bookForm.coverUrl" type="url" placeholder="https://…"
          /></label>
          <label class="field"
            ><span>内容简介</span><textarea v-model="bookForm.description" rows="4"></textarea>
          </label>
        </form>
      </template>
      <template v-else-if="modal === 'stock' && selected"
        ><div class="stock-summary">
          <span>《{{ selected.title }}》</span
          ><strong
            >当前 {{ selected.totalStock }} 册 / 可借 {{ selected.availableStock }} 册</strong
          >
        </div>
        <div class="form-stack">
          <label class="field"
            ><span>调整数量 <b>*</b></span
            ><input v-model.number="stockForm.change" type="number" /><small
              >增加库存填正数，减少库存填负数。</small
            ></label
          ><label class="field"
            ><span>调整原因 <b>*</b></span
            ><textarea
              v-model="stockForm.reason"
              rows="3"
              placeholder="例如：新书入库、盘点损耗"
            ></textarea>
          </label></div
      ></template>
      <template v-else-if="modal === 'reserve' && selected"
        ><div class="reserve-summary">
          <div class="mini-cover" :style="coverStyle(selected)">
            {{ selected.title.slice(0, 4) }}
          </div>
          <div>
            <h3>{{ selected.title }}</h3>
            <p>{{ selected.author }}</p>
            <span>当前可借 {{ selected.availableStock }} 册</span>
          </div>
        </div>
        <label class="field"
          ><span>预约备注</span
          ><textarea
            v-model="reserveRemark"
            rows="3"
            placeholder="选填，例如：周末到馆领取"
          ></textarea>
        </label>
        <p class="form-hint">提交后需等待管理员审核，审核通过后可到馆借出。</p></template
      >
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="modal = null">取消</button>
        <button
          v-if="modal === 'form'"
          class="btn btn-primary"
          type="button"
          :disabled="actionLoading"
          @click="saveBook"
        >
          {{ actionLoading ? '保存中…' : '保存图书' }}
        </button>
        <button
          v-if="modal === 'stock'"
          class="btn btn-primary"
          type="button"
          :disabled="actionLoading"
          @click="adjustStock"
        >
          {{ actionLoading ? '提交中…' : '确认调整' }}
        </button>
        <button
          v-if="modal === 'reserve'"
          class="btn btn-primary"
          type="button"
          :disabled="actionLoading"
          @click="reserveBook"
        >
          {{ actionLoading ? '提交中…' : '提交预约' }}
        </button>
        <button
          v-if="modal === 'detail' && auth.isReader && selected && selected.availableStock > 0"
          class="btn btn-primary"
          type="button"
          @click="modal = 'reserve'"
        >
          预约借阅
        </button>
      </template>
    </BaseModal>
  </div>
</template>
