<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <tr :class="cn('border-b border-divider last:border-0 hover:bg-bg-hover/50 transition-colors duration-150', props.class)">
    <slot />
  </tr>
</template>
