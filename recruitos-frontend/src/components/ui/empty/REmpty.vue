<script setup lang="ts">
import { cn } from '@/lib/utils'
import { Inbox } from 'lucide-vue-next'
import RButton from '../button/RButton.vue'

interface Props {
  title?: string
  description?: string
  icon?: any
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '暂无数据',
  description: '',
})
</script>

<template>
  <div :class="cn('flex flex-col items-center justify-center py-16 text-center', props.class)">
    <div class="w-14 h-14 rounded-full bg-bg-muted flex items-center justify-center mb-4">
      <component :is="icon || Inbox" class="h-6 w-6 text-text-placeholder" />
    </div>
    <p class="text-[15px] font-medium text-text-primary mb-1">{{ title }}</p>
    <p v-if="description" class="text-[13px] text-text-secondary mb-4 max-w-sm">{{ description }}</p>
    <slot />
  </div>
</template>
