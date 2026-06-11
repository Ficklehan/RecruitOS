<script setup lang="ts">
import { Button } from '@/components/ui'

defineProps<{
  modelValue: string
  options: { label: string; value: string }[]
  class?: string
}>()

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <div :class="['inline-flex rounded-xl bg-muted/50 p-1', $props.class]">
    <Button
      v-for="opt in options"
      :key="opt.value"
      size="sm"
      :variant="modelValue === opt.value ? 'default' : 'ghost'"
      @click="emit('update:modelValue', opt.value)"
    >
      {{ opt.label }}
    </Button>
  </div>
</template>
