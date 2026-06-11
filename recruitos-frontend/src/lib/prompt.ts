import { ref } from 'vue'

export interface PromptOptions {
  title: string
  message?: string
  placeholder?: string
  confirmText?: string
  cancelText?: string
  validator?: (value: string) => boolean | string
}

type Resolver = (value: string | null) => void

export const promptState = ref<{
  open: boolean
  options: PromptOptions
  value: string
  error: string
  resolve: Resolver | null
}>({
  open: false,
  options: { title: '' },
  value: '',
  error: '',
  resolve: null,
})

export function prompt(options: PromptOptions): Promise<string | null> {
  return new Promise((resolve) => {
    promptState.value = {
      open: true,
      options,
      value: '',
      error: '',
      resolve,
    }
  })
}

export function finishPrompt(value: string | null) {
  const r = promptState.value.resolve
  promptState.value.open = false
  promptState.value.resolve = null
  r?.(value)
}

export function validatePromptInput(): boolean {
  const { options, value } = promptState.value
  if (options.validator) {
    const result = options.validator(value)
    if (result !== true) {
      promptState.value.error = typeof result === 'string' ? result : '输入无效'
      return false
    }
  }
  promptState.value.error = ''
  return true
}
