<script setup lang="ts">
/**
 * CardHeader — styled header area for card title and description.
 * Use inside <RCard padding="none">.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div :class="cn('px-6 pt-6 pb-2', props.class)">
    <slot />
  </div>
</template>
