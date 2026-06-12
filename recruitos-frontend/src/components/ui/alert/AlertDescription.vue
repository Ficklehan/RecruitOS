<script setup lang="ts">
/**
 * AlertDescription — styled description text for alerts.
 * Use inside <RAlert>.
 */
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div :class="cn('text-[13px] opacity-90', props.class)">
    <slot />
  </div>
</template>
