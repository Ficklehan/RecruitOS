<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  class?: string
  count?: number
}

const props = withDefaults(defineProps<Props>(), {
  count: 3,
})
</script>

<template>
  <div :class="cn('animate-pulse space-y-3', props.class)">
    <div v-for="i in count" :key="i" class="space-y-2">
      <div class="h-3 bg-bg-muted rounded-[var(--r-radius-sm)]" :style="{ width: `${60 + Math.random() * 30}%` }" />
      <div class="h-3 bg-bg-muted rounded-[var(--r-radius-sm)]" :style="{ width: `${40 + Math.random() * 40}%` }" />
    </div>
  </div>
</template>
