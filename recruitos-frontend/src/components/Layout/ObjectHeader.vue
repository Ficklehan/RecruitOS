<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import { RCard, RAvatar } from '@/components/ui'

interface Props {
  name?: string
  meta?: string
  avatarText?: string
  class?: string
}

const props = defineProps<Props>()

const initials = computed(() => {
  const text = props.avatarText || props.name || '?'
  return text.charAt(0).toUpperCase()
})
</script>

<template>
  <RCard padding="md" :class="cn('flex items-center gap-4', props.class)">
    <RAvatar :alt="name || avatarText || ''" size="lg" />
    <div class="flex-1 min-w-0">
      <slot name="title">
        <h2 class="text-[16px] font-semibold text-text-primary truncate">{{ name }}</h2>
      </slot>
      <slot name="meta">
        <p v-if="meta" class="text-[13px] text-text-secondary mt-0.5">{{ meta }}</p>
      </slot>
    </div>
    <div v-if="$slots.actions" class="flex items-center gap-2 shrink-0">
      <slot name="actions" />
    </div>
  </RCard>
</template>
