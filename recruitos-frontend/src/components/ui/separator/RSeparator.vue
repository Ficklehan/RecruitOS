<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  direction?: 'horizontal' | 'vertical'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  direction: 'horizontal',
})
</script>

<template>
  <div
    :class="cn(
      props.direction === 'horizontal' ? 'w-full h-px bg-divider' : 'h-full w-px bg-divider',
      props.class,
    )"
  />
</template>
