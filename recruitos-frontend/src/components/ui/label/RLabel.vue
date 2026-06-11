<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  required?: boolean
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <label :class="cn('text-[13px] font-medium text-text-secondary inline-flex items-center gap-1', props.class)">
    <slot />
    <span v-if="required" class="text-danger">*</span>
  </label>
</template>
