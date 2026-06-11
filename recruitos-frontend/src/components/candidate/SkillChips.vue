<script setup lang="ts">
import { computed, ref } from 'vue'
import { cn } from '@/lib/utils'
import { RBadge, RButton } from '@/components/ui'

interface Props {
  skills: string[]
  limit?: number
  compact?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  limit: 6,
  compact: true,
})

const expanded = ref(false)

const effectiveLimit = computed(() => (expanded.value ? props.skills.length : props.limit))
const visibleSkills = computed(() => props.skills.slice(0, effectiveLimit.value))
const hiddenCount = computed(() => Math.max(0, props.skills.length - effectiveLimit.value))
</script>

<template>
  <div
    v-if="skills.length"
    :class="cn(
      'flex items-center gap-1',
      compact ? 'flex-nowrap overflow-hidden max-w-full' : 'flex-wrap',
      props.class,
    )"
  >
    <RBadge
      v-for="s in visibleSkills"
      :key="s"
      variant="outline"
      class="font-normal shrink-0"
    >
      {{ s }}
    </RBadge>
    <span v-if="hiddenCount > 0" class="text-[12px] text-text-secondary shrink-0">
      +{{ hiddenCount }}
    </span>
    <RButton
      v-if="!compact && hiddenCount > 0 && !expanded"
      variant="ghost"
      size="sm"
      class="h-5 px-1 text-[12px]"
      @click="expanded = true"
    >
      展开
    </RButton>
  </div>
</template>
