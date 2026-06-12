<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'outline' | 'default' | 'link' | 'destructive'
  size?: 'sm' | 'md' | 'lg' | 'icon' | 'xs'
  disabled?: boolean
  loading?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
})

const variantClasses = {
  primary: 'bg-primary text-white hover:bg-primary-hover active:scale-[0.98]',
  secondary: 'bg-bg-muted text-text-regular hover:bg-bg-hover active:scale-[0.98]',
  ghost: 'bg-transparent text-text-secondary hover:bg-bg-hover active:scale-[0.98]',
  danger: 'bg-danger text-white hover:bg-red-600 active:scale-[0.98]',
  outline: 'bg-transparent text-text-regular border border-border hover:bg-bg-hover active:scale-[0.98]',
  default: 'bg-bg-card text-text-regular border border-border hover:bg-bg-hover active:scale-[0.98]',
  link: 'bg-transparent text-primary hover:underline p-0 h-auto',
  destructive: 'bg-danger text-white hover:bg-red-600 active:scale-[0.98]',
}

const sizeClasses = {
  xs: 'h-6 px-2 text-[11px] gap-1 rounded-[var(--r-radius-sm)]',
  sm: 'h-8 px-3 text-[13px] gap-1.5 rounded-[var(--r-radius-sm)]',
  md: 'h-9 px-4 text-[14px] gap-2 rounded-[var(--r-radius)]',
  lg: 'h-10 px-6 text-[14px] gap-2 rounded-[var(--r-radius)]',
  icon: 'h-9 w-9 p-0 rounded-[var(--r-radius)]',
}

const classes = computed(() => cn(
  'inline-flex items-center justify-center font-medium transition-all duration-200 cursor-pointer select-none',
  'focus:outline-none focus:ring-2 focus:ring-primary/20',
  'disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100',
  variantClasses[props.variant],
  sizeClasses[props.size],
  props.class,
))
</script>

<template>
  <button :class="classes" :disabled="disabled || loading">
    <svg v-if="loading" class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
    <slot />
  </button>
</template>
