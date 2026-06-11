<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <th :class="cn('px-4 py-3 text-left text-[13px] font-medium text-text-secondary whitespace-nowrap', props.class)">
    <slot />
  </th>
</template>
