<script setup lang="ts">
/**
 * DialogFooter — styled footer area for action buttons.
 * Use inside <DialogContent> at the bottom.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div :class="cn('mt-4 flex justify-end gap-3', props.class)">
    <slot />
  </div>
</template>
