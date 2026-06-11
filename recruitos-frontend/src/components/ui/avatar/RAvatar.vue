<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  src?: string
  alt?: string
  size?: 'sm' | 'md' | 'lg'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  alt: '',
})

const sizeClasses = {
  sm: 'w-7 h-7 text-[11px]',
  md: 'w-9 h-9 text-[13px]',
  lg: 'w-12 h-12 text-[15px]',
}

const initials = computed(() => {
  if (!props.alt) return '?'
  return props.alt.charAt(0).toUpperCase()
})

const bgColors = ['bg-primary/10 text-primary', 'bg-success/10 text-success', 'bg-warning/10 text-warning', 'bg-danger/10 text-danger', 'bg-info/10 text-info']
const bgColor = computed(() => bgColors[props.alt.charCodeAt(0) % bgColors.length])
</script>

<template>
  <div :class="cn('rounded-full overflow-hidden shrink-0 flex items-center justify-center font-medium', sizeClasses[size], bgColor, props.class)">
    <img v-if="src" :src="src" :alt="alt" class="w-full h-full object-cover" />
    <span v-else>{{ initials }}</span>
  </div>
</template>
