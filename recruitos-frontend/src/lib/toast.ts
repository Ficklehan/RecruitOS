import { createApp, h, ref } from 'vue'
import RToast from '@/components/ui/toast/RToast.vue'

let toastInstance: any = null
let toastApp: any = null

function ensureToast() {
  if (toastInstance) return toastInstance
  const container = document.createElement('div')
  document.body.appendChild(container)
  toastApp = createApp({
    render() {
      return h(RToast, { ref: 'toast' })
    },
  })
  toastApp.mount(container)
  toastInstance = toastApp._instance?.refs?.toast
  return toastInstance
}

export const toast = {
  success(message: string) {
    ensureToast()?.success(message)
  },
  error(message: string) {
    ensureToast()?.error(message)
  },
  warning(message: string) {
    ensureToast()?.warning(message)
  },
  info(message: string) {
    ensureToast()?.info(message)
  },
}
