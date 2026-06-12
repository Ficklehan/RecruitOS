import { ref } from 'vue'

export type ToastVariant = 'default' | 'destructive' | 'success' | 'warning'

export interface ToastItem {
  id: number
  message: string
  variant: ToastVariant
}

let seq = 0
const timers = new Map<number, ReturnType<typeof window.setTimeout>>()
export const toasts = ref<ToastItem[]>([])

export function notify(message: string, variant: ToastVariant = 'default', duration = 2800) {
  const id = ++seq
  toasts.value.push({ id, message, variant })
  if (duration > 0) {
    const timer = window.setTimeout(() => dismissToast(id), duration)
    timers.set(id, timer)
  }
}

export function dismissToast(id: number) {
  const timer = timers.get(id)
  if (timer) {
    clearTimeout(timer)
    timers.delete(id)
  }
  toasts.value = toasts.value.filter((t) => t.id !== id)
}

export const toast = {
  success: (msg: string) => notify(msg, 'success'),
  error: (msg: string) => notify(msg, 'destructive'),
  warning: (msg: string) => notify(msg, 'warning'),
  info: (msg: string) => notify(msg, 'default'),
}
