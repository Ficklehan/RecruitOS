<script setup lang="ts">
/**
 * CardFooter — styled footer area for card actions.
 * Use inside <RCard padding="none">.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div :class="cn('px-6 py-4 border-t border-divider flex items-center', props.class)">
    <slot />
  </div>
</template>
