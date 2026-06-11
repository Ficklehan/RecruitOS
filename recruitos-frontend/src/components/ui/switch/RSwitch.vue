<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  modelValue?: boolean
  disabled?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const trackClasses = computed(() => cn(
  'relative inline-flex h-5 w-9 items-center rounded-full transition-all duration-200 cursor-pointer',
  props.modelValue ? 'bg-primary' : 'bg-border',
  props.disabled && 'opacity-50 cursor-not-allowed',
))

const thumbClasses = computed(() => cn(
  'inline-block h-3.5 w-3.5 rounded-full bg-white shadow-sm transition-all duration-200',
  props.modelValue ? 'translate-x-[18px]' : 'translate-x-[3px]',
))
</script>

<template>
  <button
    type="button"
    :class="trackClasses"
    :disabled="disabled"
    @click="emit('update:modelValue', !modelValue)"
  >
    <span :class="thumbClasses" />
  </button>
</template>
