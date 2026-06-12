<script setup lang="ts">
/**
 * DialogTitle — styled title text for dialogs.
 * Use inside <DialogHeader>.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <h2 :class="cn('text-[16px] font-semibold text-text-primary', props.class)">
    <slot />
  </h2>
</template>
