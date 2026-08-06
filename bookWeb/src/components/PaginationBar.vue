<script setup lang="ts">
const props = defineProps<{
  page: number
  pages: number
  total: number
}>()
const emit = defineEmits<{ change: [page: number] }>()

function go(page: number) {
  if (page >= 1 && page <= props.pages && page !== props.page) emit('change', page)
}
</script>

<template>
  <div v-if="pages > 0" class="pagination">
    <span>共 {{ total }} 条</span>
    <div class="pagination-actions">
      <button
        class="btn btn-ghost btn-sm"
        type="button"
        :disabled="page <= 1"
        @click="go(page - 1)"
      >
        上一页
      </button>
      <span class="page-indicator">{{ page }} / {{ pages }}</span>
      <button
        class="btn btn-ghost btn-sm"
        type="button"
        :disabled="page >= pages"
        @click="go(page + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>
