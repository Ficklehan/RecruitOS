<script setup lang="ts">
/**
 * DialogHeader — styled header area for dialog title and description.
 * Use inside <DialogContent> above <DialogTitle>.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div :class="cn('mb-4', props.class)">
    <slot />
  </div>
</template>
