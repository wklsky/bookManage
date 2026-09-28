<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/components/PaginationBar.vue
 * @Description: 分页条
 */
interface Props {
  /** 当前页码，从 1 开始 */
  page: number
  /** 总页数 */
  pages: number
  /** 总记录数 */
  total: number
}

const props = defineProps<Props>()

/** 切换页码时触发，回传目标页码（从 1 开始，后端不做越界纠正） */
const emit = defineEmits<{ 'update:page': [page: number] }>()

function go(delta: number) {
  const next = props.page + delta
  if (next < 1 || (props.pages > 0 && next > props.pages)) return
  emit('update:page', next)
}
</script>

<template>
  <div class="pager">
    <span>共 {{ total }} 条 · {{ page }} / {{ pages || 1 }} 页</span>
    <button class="btn btn-sm" type="button" :disabled="page <= 1" @click="go(-1)">上一页</button>
    <button
      class="btn btn-sm"
      type="button"
      :disabled="pages === 0 || page >= pages"
      @click="go(1)"
    >
      下一页
    </button>
  </div>
</template>
