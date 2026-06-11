<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  variant?: 'default' | 'interactive' | 'flat'
  padding?: 'none' | 'sm' | 'md' | 'lg'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  padding: 'md',
})

const variantClasses = {
  default: 'bg-bg-card r-shadow-sm',
  interactive: 'bg-bg-card r-shadow-sm hover:r-shadow-md cursor-pointer transition-shadow duration-200',
  flat: 'bg-bg-muted',
}

const paddingClasses = {
  none: '',
  sm: 'p-4',
  md: 'p-5',
  lg: 'p-6',
}

const classes = computed(() => cn(
  'rounded-[var(--r-radius)]',
  variantClasses[props.variant],
  props.class,
))
</script>

<template>
  <div :class="classes">
    <div v-if="$slots.header" class="px-5 py-4 border-b border-divider">
      <slot name="header" />
    </div>
    <div :class="paddingClasses[padding]">
      <slot />
    </div>
  </div>
</template>
