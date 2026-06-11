<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  modelValue?: string | number
  placeholder?: string
  disabled?: boolean
  error?: boolean
  type?: string
  rows?: number
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  rows: 3,
  disabled: false,
  error: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>()

const classes = computed(() => cn(
  'w-full px-3 text-[14px] rounded-[var(--r-radius)]',
  'bg-bg-muted border-none',
  'text-text-primary placeholder:text-text-placeholder',
  'transition-all duration-200',
  'focus:outline-none focus:ring-2 focus:ring-primary/20 focus:bg-bg-card',
  'disabled:opacity-50 disabled:cursor-not-allowed',
  props.error && 'ring-2 ring-danger/20 bg-danger-light/30',
  props.type === 'textarea' ? 'min-h-[80px] py-2 resize-y' : 'h-9',
  props.class,
))
</script>

<template>
  <textarea
    v-if="type === 'textarea'"
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :rows="rows"
    :class="classes"
    @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  />
  <input
    v-else
    :type="type"
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :class="classes"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>
