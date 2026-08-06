import { computed, ref } from 'vue'

export interface ToastMessage {
  id: number
  type: 'success' | 'error' | 'info'
  text: string
}

const messages = ref<ToastMessage[]>([])
let nextId = 1

export function useToast() {
  function show(text: string, type: ToastMessage['type'] = 'info') {
    const id = nextId++
    messages.value.push({ id, type, text })
    window.setTimeout(() => dismiss(id), 3200)
  }

  function dismiss(id: number) {
    messages.value = messages.value.filter((message) => message.id !== id)
  }

  return {
    messages: computed(() => messages.value),
    show,
    success: (text: string) => show(text, 'success'),
    error: (text: string) => show(text, 'error'),
    dismiss,
  }
}
