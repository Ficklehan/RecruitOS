<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  title?: string
  subtitle?: string
  variant?: 'default' | 'list' | 'plain'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
})
</script>

<template>
  <div :class="cn('min-h-0 flex-1 flex flex-col', variant === 'plain' ? 'p-6' : 'p-6 gap-6', props.class)">
    <div v-if="title && variant !== 'plain'" class="flex items-center justify-between shrink-0">
      <div>
        <h1 class="text-[20px] font-semibold text-text-primary">{{ title }}</h1>
        <p v-if="subtitle" class="text-[13px] text-text-secondary mt-0.5">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="flex items-center gap-2">
        <slot name="actions" />
      </div>
    </div>
    <div v-if="$slots.toolbar && variant === 'list'" class="shrink-0">
      <slot name="toolbar" />
    </div>
    <div class="flex-1 min-h-0">
      <slot />
    </div>
  </div>
</template>
