<script setup lang="ts">
import { inject, computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  value: string
  class?: string
}

const props = defineProps<Props>()
const activeTab = inject<import('vue').Ref<string>>('activeTab')
const visible = computed(() => activeTab?.value === props.value)
</script>

<template>
  <div v-if="visible" :class="cn('mt-4', props.class)">
    <slot />
  </div>
</template>
