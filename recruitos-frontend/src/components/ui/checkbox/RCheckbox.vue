<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import { Check } from 'lucide-vue-next'

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

const classes = computed(() => cn(
  'peer h-4 w-4 shrink-0 rounded-[4px] border border-border',
  'transition-all duration-200 cursor-pointer',
  'focus:outline-none focus:ring-2 focus:ring-primary/20',
  'disabled:opacity-50 disabled:cursor-not-allowed',
  props.modelValue && 'bg-primary border-primary',
  props.class,
))
</script>

<template>
  <label class="inline-flex items-center gap-2 cursor-pointer">
    <div :class="classes" @click="emit('update:modelValue', !modelValue)">
      <Check v-if="modelValue" class="h-3 w-3 text-white m-0.5" />
    </div>
    <slot />
  </label>
</template>
