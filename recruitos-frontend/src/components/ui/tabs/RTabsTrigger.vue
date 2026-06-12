<script setup lang="ts">
import { inject, computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  value: string
  class?: string
}

const props = defineProps<Props>()
const activeTab = inject<import('vue').Ref<string>>('activeTab')

const isActive = computed(() => activeTab?.value === props.value)

function handleClick() {
  if (activeTab) activeTab.value = props.value
}

const classes = computed(() => cn(
  'px-3 py-1.5 text-[13px] font-medium rounded-[var(--r-radius-sm)] cursor-pointer',
  'transition-all duration-200',
  isActive.value
    ? 'bg-bg-card text-text-primary r-shadow-sm'
    : 'text-text-secondary hover:text-text-primary',
  props.class,
))
</script>

<template>
  <button type="button" :class="classes" @click="handleClick">
    <slot />
  </button>
</template>
