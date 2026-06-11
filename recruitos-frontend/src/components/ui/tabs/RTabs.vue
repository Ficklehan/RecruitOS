<script setup lang="ts">
import { ref, provide, computed, type Ref } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  modelValue?: string
  class?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const activeTab = computed<string>({
  get: () => props.modelValue || '',
  set: (v: string) => emit('update:modelValue', v),
})

provide<Ref<string>>('activeTab', activeTab as any)
</script>

<template>
  <div :class="cn('', props.class)">
    <slot />
  </div>
</template>
