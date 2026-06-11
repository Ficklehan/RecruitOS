<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-vue-next'

interface Toast {
  id: number
  type: 'success' | 'error' | 'warning' | 'info'
  message: string
}

const toasts = ref<Toast[]>([])
let nextId = 0

const icons = { success: CheckCircle, error: XCircle, warning: AlertTriangle, info: Info }
const colors = {
  success: 'bg-success-light text-success border-success/20',
  error: 'bg-danger-light text-danger border-danger/20',
  warning: 'bg-warning-light text-warning border-warning/20',
  info: 'bg-info-light text-info border-info/20',
}

function addToast(type: Toast['type'], message: string) {
  const id = nextId++
  toasts.value.push({ id, type, message })
  setTimeout(() => removeToast(id), 4000)
}

function removeToast(id: number) {
  toasts.value = toasts.value.filter(t => t.id !== id)
}

function success(message: string) { addToast('success', message) }
function error(message: string) { addToast('error', message) }
function warning(message: string) { addToast('warning', message) }
function info(message: string) { addToast('info', message) }

defineExpose({ success, error, warning, info })
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-[var(--r-z-toast)] flex flex-col gap-2 pointer-events-none">
      <TransitionGroup
        enter-active-class="transition-all duration-300 ease-out"
        enter-from-class="opacity-0 translate-x-8"
        enter-to-class="opacity-100 translate-x-0"
        leave-active-class="transition-all duration-200 ease-in"
        leave-from-class="opacity-100 translate-x-0"
        leave-to-class="opacity-0 translate-x-8"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="cn(
            'pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-[var(--r-radius)]',
            'r-shadow-lg border min-w-[280px] max-w-[420px]',
            colors[toast.type],
          )"
        >
          <component :is="icons[toast.type]" class="h-4 w-4 shrink-0" />
          <span class="text-[14px] flex-1">{{ toast.message }}</span>
          <button class="p-0.5 hover:opacity-70 transition-opacity" @click="removeToast(toast.id)">
            <X class="h-3.5 w-3.5" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
