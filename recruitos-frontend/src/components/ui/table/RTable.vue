<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div :class="cn('w-full overflow-auto rounded-[var(--r-radius)]', props.class)">
    <table class="w-full text-[14px]">
      <slot />
    </table>
  </div>
</template>
