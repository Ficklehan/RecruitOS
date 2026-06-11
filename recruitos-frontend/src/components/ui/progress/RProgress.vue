<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  value?: number
  max?: number
  variant?: 'line' | 'circle'
  size?: 'sm' | 'md' | 'lg'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  max: 100,
  variant: 'line',
  size: 'md',
})

const percent = computed(() => Math.min(100, Math.max(0, (props.value || 0) / props.max * 100)))

const color = computed(() => {
  if (percent.value >= 80) return 'bg-success'
  if (percent.value >= 50) return 'bg-primary'
  if (percent.value >= 30) return 'bg-warning'
  return 'bg-danger'
})

const heights = { sm: 'h-1', md: 'h-1.5', lg: 'h-2' }
const circleSizes = { sm: 'w-12 h-12', md: 'w-16 h-16', lg: 'w-20 h-20' }
</script>

<template>
  <div v-if="variant === 'line'" :class="cn('w-full bg-bg-muted rounded-full overflow-hidden', heights[size], props.class)">
    <div :class="cn('h-full rounded-full transition-all duration-500', color)" :style="{ width: `${percent}%` }" />
  </div>
  <div v-else :class="cn('relative inline-flex items-center justify-center', circleSizes[size], props.class)">
    <svg class="w-full h-full -rotate-90" viewBox="0 0 36 36">
      <circle cx="18" cy="18" r="15.9" fill="none" stroke="currentColor" stroke-width="3" class="text-bg-muted" />
      <circle cx="18" cy="18" r="15.9" fill="none" stroke-width="3" stroke-linecap="round"
        :class="color.replace('bg-', 'text-')"
        :stroke-dasharray="`${percent} 100`" />
    </svg>
    <span class="absolute text-[12px] font-semibold text-text-primary">{{ Math.round(percent) }}%</span>
  </div>
</template>
