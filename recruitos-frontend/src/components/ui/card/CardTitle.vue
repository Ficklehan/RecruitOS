<script setup lang="ts">
/**
 * CardTitle — styled title text for cards.
 * Use inside <CardHeader>.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <h3 :class="cn('text-[16px] font-semibold text-text-primary', props.class)">
    <slot />
  </h3>
</template>
