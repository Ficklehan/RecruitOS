import { ref } from 'vue'

export type ToastVariant = 'default' | 'destructive' | 'success'

export interface ToastItem {
  id: number
  message: string
  variant: ToastVariant
}

let seq = 0
export const toasts = ref<ToastItem[]>([])

export function notify(message: string, variant: ToastVariant = 'default', duration = 2800) {
  const id = ++seq
  toasts.value.push({ id, message, variant })
  if (duration > 0) {
    window.setTimeout(() => dismissToast(id), duration)
  }
}

export function dismissToast(id: number) {
  toasts.value = toasts.value.filter((t) => t.id !== id)
}

export const toast = {
  success: (msg: string) => notify(msg, 'success'),
  error: (msg: string) => notify(msg, 'destructive'),
  info: (msg: string) => notify(msg, 'default'),
}
