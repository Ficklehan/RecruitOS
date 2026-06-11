<script setup lang="ts">
import { computed } from 'vue'
import { RInput } from '@/components/ui'

const props = defineProps<{
  modelValue?: [string, string] | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: [string, string] | null]
}>()

const start = computed({
  get: () => props.modelValue?.[0] ?? '',
  set: (v: string) => {
    const end = props.modelValue?.[1] ?? ''
    emit('update:modelValue', v || end ? [v, end] : null)
  },
})

const end = computed({
  get: () => props.modelValue?.[1] ?? '',
  set: (v: string) => {
    const s = props.modelValue?.[0] ?? ''
    emit('update:modelValue', s || v ? [s, v] : null)
  },
})
</script>

<template>
  <div class="flex flex-wrap items-center gap-2">
    <RInput v-model="start" type="date" class="w-[150px]" />
    <span class="text-[13px] text-text-secondary">至</span>
    <RInput v-model="end" type="date" class="w-[150px]" />
  </div>
</template>
