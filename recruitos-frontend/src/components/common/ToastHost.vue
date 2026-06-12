<script setup lang="ts">
import { toasts, dismissToast } from '@/lib/notify'
import { cn } from '@/lib/utils'
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-vue-next'

const icons = {
  success: CheckCircle,
  destructive: XCircle,
  warning: AlertTriangle,
  default: Info,
}

const colors = {
  success: 'bg-success-light text-success border-success/20',
  destructive: 'bg-danger-light text-danger border-danger/20',
  warning: 'bg-warning-light text-warning border-warning/20',
  default: 'bg-info-light text-info border-info/20',
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-[var(--r-z-toast)] flex w-full max-w-sm flex-col gap-2 pointer-events-none">
      <TransitionGroup
        enter-active-class="transition-all duration-300 ease-out"
        enter-from-class="opacity-0 translate-x-8"
        enter-to-class="opacity-100 translate-x-0"
        leave-active-class="transition-all duration-200 ease-in"
        leave-from-class="opacity-100 translate-x-0"
        leave-to-class="opacity-0 translate-x-8"
      >
        <div
          v-for="item in toasts"
          :key="item.id"
          :class="cn(
            'pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-[var(--r-radius)]',
            'r-shadow-lg border min-w-[280px]',
            colors[item.variant] || colors.default,
          )"
        >
          <component :is="icons[item.variant] || icons.default" class="h-4 w-4 shrink-0" />
          <span class="text-[14px] flex-1">{{ item.message }}</span>
          <button class="p-0.5 hover:opacity-70 transition-opacity" @click="dismissToast(item.id)">
            <X class="h-3.5 w-3.5" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
