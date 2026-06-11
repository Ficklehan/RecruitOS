<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  rows?: number
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  rows: 3,
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const classes = computed(() => cn(
  'w-full px-3 py-2 text-[14px] rounded-[var(--r-radius)]',
  'bg-bg-muted border-none resize-none',
  'text-text-primary placeholder:text-text-placeholder',
  'transition-all duration-200',
  'focus:outline-none focus:ring-2 focus:ring-primary/20 focus:bg-bg-card',
  'disabled:opacity-50 disabled:cursor-not-allowed',
  props.class,
))
</script>

<template>
  <textarea
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :rows="rows"
    :class="classes"
    @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  />
</template>
