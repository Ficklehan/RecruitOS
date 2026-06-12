<script setup lang="ts">
import { ref, provide, computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  modelValue?: string
  class?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const activeTab = computed({
  get: () => props.modelValue,
  set: (v) => { if (v !== undefined) emit('update:modelValue', v) },
})

provide('activeTab', activeTab)
</script>

<template>
  <div :class="cn('', props.class)">
    <slot />
  </div>
</template>
