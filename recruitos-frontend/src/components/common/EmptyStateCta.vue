<script setup lang="ts">
import { cn } from '@/lib/utils'
import { RButton, REmpty } from '@/components/ui'

interface Action {
  label: string
  type?: 'default' | 'primary' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link'
  onClick: () => void
}

function buttonVariant(type?: Action['type']) {
  if (type === 'primary' || type === 'default') return 'primary' as const
  if (type === 'destructive') return 'danger' as const
  return 'outline' as const
}

interface Props {
  title?: string
  description: string
  imageSize?: number
  compact?: boolean
  actions?: Action[]
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  imageSize: 72,
  compact: false,
  actions: () => [],
})
</script>

<template>
  <div :class="cn('flex flex-col items-center justify-center text-center', compact ? 'py-5 px-4' : 'py-8 px-6', props.class)">
    <REmpty :title="title || '暂无数据'" :description="description" />
    <div v-if="actions.length" class="flex flex-wrap justify-center gap-2 mt-4">
      <RButton
        v-for="action in actions"
        :key="action.label"
        :variant="buttonVariant(action.type)"
        @click="action.onClick"
      >
        {{ action.label }}
      </RButton>
    </div>
  </div>
</template>
