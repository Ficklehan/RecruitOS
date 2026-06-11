<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'outline'
  size?: 'sm' | 'md'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'md',
})

const variantClasses = {
  default: 'bg-primary/10 text-primary',
  success: 'bg-success/10 text-success',
  warning: 'bg-warning/10 text-warning',
  danger: 'bg-danger/10 text-danger',
  info: 'bg-info/10 text-info',
  outline: 'bg-transparent border border-border text-text-secondary',
}

const sizeClasses = {
  sm: 'px-2 py-0.5 text-[11px]',
  md: 'px-2.5 py-0.5 text-[12px]',
}

const classes = computed(() => cn(
  'inline-flex items-center font-medium rounded-[var(--r-radius-full)]',
  variantClasses[props.variant],
  sizeClasses[props.size],
  props.class,
))
</script>

<template>
  <span :class="classes"><slot /></span>
</template>
