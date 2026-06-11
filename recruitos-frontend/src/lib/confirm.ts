import { ref } from 'vue'

export interface ConfirmOptions {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  destructive?: boolean
}

type Resolver = (value: boolean) => void

export const confirmState = ref<{
  open: boolean
  options: ConfirmOptions
  resolve: Resolver | null
}>({
  open: false,
  options: { title: '', message: '' },
  resolve: null,
})

export function confirm(options: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    confirmState.value = {
      open: true,
      options,
      resolve,
    }
  })
}

export function finishConfirm(result: boolean) {
  const r = confirmState.value.resolve
  confirmState.value.open = false
  confirmState.value.resolve = null
  r?.(result)
}
