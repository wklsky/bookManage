<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/components/ToastStack.vue
 * @Description: 全局操作消息展示
 */
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
</script>

<template>
  <div class="toast-stack">
    <div v-for="item in toast.items" :key="item.id" :class="['toast', `toast-${item.tone}`]">
      {{ item.message }}
    </div>
  </div>
</template>
