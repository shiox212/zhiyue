import { ref } from 'vue'
import { defineStore } from 'pinia'

export type ToastTone = 'success' | 'error' | 'info'

export interface ToastItem {
  id: number
  message: string
  tone: ToastTone
}

export const useUiStore = defineStore('ui', () => {
  const toasts = ref<ToastItem[]>([])
  let nextId = 0

  const notify = (message: string, tone: ToastTone = 'info') => {
    const id = ++nextId
    toasts.value.push({ id, message, tone })
    window.setTimeout(() => dismiss(id), 4200)
  }

  const dismiss = (id: number) => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  return { toasts, notify, dismiss }
})
