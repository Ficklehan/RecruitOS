<script setup lang="ts">
import { ref } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
const containerRef = ref<HTMLElement | null>(null)
</script>

<template>
  <div ref="containerRef" :class="cn('overflow-auto', props.class)">
    <slot />
  </div>
</template>
