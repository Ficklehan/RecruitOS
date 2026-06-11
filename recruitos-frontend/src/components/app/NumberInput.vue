<script setup lang="ts">
import { RInput } from '@/components/ui'

const props = withDefaults(
  defineProps<{
    modelValue?: number | null
    min?: number
    max?: number
    step?: number
    placeholder?: string
  }>(),
  { step: 1 }
)

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

function onInput(e: Event) {
  const raw = (e.target as HTMLInputElement).value
  if (raw === '') {
    emit('update:modelValue', null)
    return
  }
  let n = Number(raw)
  if (Number.isNaN(n)) return
  if (props.min != null) n = Math.max(props.min, n)
  if (props.max != null) n = Math.min(props.max, n)
  emit('update:modelValue', n)
}
</script>

<template>
  <RInput
    type="number"
    :value="modelValue ?? ''"
    :min="min"
    :max="max"
    :step="step"
    :placeholder="placeholder"
    @input="onInput"
  />
</template>
