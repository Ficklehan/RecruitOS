<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import { CheckCircle, XCircle, AlertTriangle, Info } from 'lucide-vue-next'

interface Props {
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'default'
  title?: string
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'info',
})

const icons = { success: CheckCircle, warning: AlertTriangle, danger: XCircle, info: Info, default: Info }
const colors = {
  success: 'bg-success-light text-success border-success/20',
  warning: 'bg-warning-light text-warning border-warning/20',
  danger: 'bg-danger-light text-danger border-danger/20',
  info: 'bg-info-light text-info border-info/20',
  default: 'bg-info-light text-info border-info/20',
}
</script>

<template>
  <div :class="cn('flex items-start gap-3 px-4 py-3 rounded-[var(--r-radius)] border', colors[variant], props.class)">
    <component :is="icons[variant]" class="h-4 w-4 mt-0.5 shrink-0" />
    <div class="flex-1 min-w-0">
      <p v-if="title" class="text-[14px] font-medium mb-0.5">{{ title }}</p>
      <div class="text-[13px] opacity-90"><slot /></div>
    </div>
  </div>
</template>
