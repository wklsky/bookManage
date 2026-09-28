/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/stores/toast.ts
 * @Description: 全局操作消息
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'

export type ToastTone = 'success' | 'error' | 'info'

export interface ToastItem {
  id: number
  tone: ToastTone
  message: string
}

/** 提示停留时长：后台操作反馈需要看得清，但也不该长期占据视线 */
const TOAST_TTL_MS = 3200

export const useToastStore = defineStore('toast', () => {
  const items = ref<ToastItem[]>([])
  let seed = 0

  function push(tone: ToastTone, message: string) {
    const id = ++seed
    items.value = [...items.value, { id, tone, message }]
    window.setTimeout(() => {
      items.value = items.value.filter((item) => item.id !== id)
    }, TOAST_TTL_MS)
  }

  return {
    items,
    success: (message: string) => push('success', message),
    error: (message: string) => push('error', message),
    info: (message: string) => push('info', message),
  }
})
