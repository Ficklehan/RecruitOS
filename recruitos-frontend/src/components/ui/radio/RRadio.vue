<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  modelValue?: string | number
  value?: string | number
  disabled?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>()

const checked = computed(() => props.modelValue === props.value)

const dotClasses = computed(() => cn(
  'w-4 h-4 rounded-full border-2 transition-all duration-200 flex items-center justify-center',
  checked.value ? 'border-primary bg-primary' : 'border-border bg-bg-card',
))

const innerDot = computed(() => cn(
  'w-1.5 h-1.5 rounded-full bg-white transition-all duration-200',
  checked.value ? 'scale-100' : 'scale-0',
))
</script>

<template>
  <label class="inline-flex items-center gap-2 cursor-pointer" :class="cn('disabled:opacity-50 disabled:cursor-not-allowed', props.class)">
    <div :class="dotClasses" @click="!disabled && emit('update:modelValue', value)">
      <div :class="innerDot" />
    </div>
    <slot />
  </label>
</template>
