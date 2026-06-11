<script setup lang="ts">
import { RInput } from '@/components/ui'
import { cn } from '@/lib/utils'

const props = withDefaults(
  defineProps<{
    modelValue?: number
    min?: number
    max?: number
    step?: number
    class?: string
  }>(),
  { min: 0, max: 100, step: 1 }
)

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

function onInput(e: Event) {
  emit('update:modelValue', Number((e.target as HTMLInputElement).value))
}
</script>

<template>
  <div :class="cn('flex items-center gap-3', $props.class)">
    <input
      type="range"
      :value="modelValue ?? min"
      :min="min"
      :max="max"
      :step="step"
      class="flex-1 h-2 accent-primary"
      @input="onInput"
    />
    <RInput
      type="number"
      :model-value="String(modelValue ?? min)"
      class="w-16 h-8"
      :min="min"
      :max="max"
      @update:model-value="(v) => emit('update:modelValue', Number(v))"
    />
  </div>
</template>
