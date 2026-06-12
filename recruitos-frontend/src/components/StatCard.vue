<script setup lang="ts">
import { cn } from '@/lib/utils'
import { RCard } from '@/components/ui'
import type { Component } from 'vue'

interface Props {
  label: string
  value: string | number
  icon?: string | Component
  color?: string
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <RCard padding="md" :class="cn('flex items-center gap-3', props.class)">
    <div v-if="icon" class="w-10 h-10 rounded-[var(--r-radius)] bg-primary-light flex items-center justify-center shrink-0">
      <component v-if="typeof icon !== 'string'" :is="icon" class="h-5 w-5" :style="color ? { color } : {}" />
      <span v-else class="text-[18px]" :style="color ? { color } : {}">{{ icon }}</span>
    </div>
    <div class="flex-1 min-w-0">
      <p class="text-[12px] text-text-secondary">{{ label }}</p>
      <p class="text-[20px] font-bold text-text-primary mt-0.5">{{ value }}</p>
    </div>
  </RCard>
</template>
