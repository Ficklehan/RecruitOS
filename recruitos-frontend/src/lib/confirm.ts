import { reactive } from 'vue'
import RDialog from '@/components/ui/dialog/RDialog.vue'
import RButton from '@/components/ui/button/RButton.vue'

interface ConfirmOptions {
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  type?: 'info' | 'warning' | 'danger'
  destructive?: boolean
}

export const confirmState = reactive({
  open: false,
  options: { title: '', message: '', confirmText: '', cancelText: '', destructive: false },
  resolve: null as ((value: boolean) => void) | null,
})

export function finishConfirm(result: boolean) {
  confirmState.open = false
  confirmState.resolve?.(result)
}

export function confirm(messageOrOptions: string | ConfirmOptions, title?: string, options?: ConfirmOptions): Promise<boolean> {
  const opts: ConfirmOptions = typeof messageOrOptions === 'string'
    ? { message: messageOrOptions, title, ...options }
    : messageOrOptions

  return new Promise((resolve) => {
    confirmState.options = {
      title: opts.title || '确认',
      message: opts.message || '',
      confirmText: opts.confirmText || '确定',
      cancelText: opts.cancelText || '取消',
      destructive: opts.destructive || opts.type === 'danger',
    }
    confirmState.resolve = resolve
    confirmState.open = true
  })
}
