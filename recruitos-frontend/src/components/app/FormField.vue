<script setup lang="ts">
import { RLabel } from '@/components/ui'
import { cn } from '@/lib/utils'

defineProps<{
  label: string
  htmlFor?: string
  required?: boolean
  error?: string
  class?: string
}>()
</script>

<template>
  <div :class="cn('flex flex-col gap-1.5', $props.class)">
    <RLabel :for="htmlFor">
      {{ label }}
      <span v-if="required" class="text-danger ml-0.5">*</span>
    </RLabel>
    <slot />
    <p v-if="error" class="text-[12px] text-danger">{{ error }}</p>
  </div>
</template>
