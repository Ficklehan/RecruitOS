<script setup lang="ts">
import { inject, computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div :class="cn('flex gap-1 p-1 bg-bg-muted rounded-[var(--r-radius)]', props.class)">
    <slot />
  </div>
</template>
