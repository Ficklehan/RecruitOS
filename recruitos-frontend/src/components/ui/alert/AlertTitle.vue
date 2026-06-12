<script setup lang="ts">
/**
 * AlertTitle — styled title text for alerts.
 * Use inside <RAlert>.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <p :class="cn('text-[14px] font-medium', props.class)">
    <slot />
  </p>
</template>
