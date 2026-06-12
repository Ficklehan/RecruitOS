<script setup lang="ts">
/**
 * CardContent — wrapper for card body content.
 * Use inside <RCard padding="none"> to add custom padding.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div :class="cn('pt-6', props.class)">
    <slot />
  </div>
</template>
