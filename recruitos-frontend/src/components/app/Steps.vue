<script setup lang="ts">
import { cn } from '@/lib/utils'

defineProps<{
  active: number
  steps: { title: string }[]
  class?: string
}>()
</script>

<template>
  <div :class="cn('flex items-center justify-center gap-2 flex-wrap', $props.class)">
    <template v-for="(step, index) in steps" :key="index">
      <div class="flex items-center gap-2">
        <div
          :class="cn(
            'flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium',
            index < active ? 'bg-primary text-primary-foreground' :
            index === active ? 'text-primary' :
            'text-muted-foreground'
          )"
          :style="{
            boxShadow: index < active
              ? '0 0 0 2px #4F6BED'
              : index === active
                ? '0 0 0 2px #4F6BED'
                : '0 0 0 2px #F0F2F5'
          }"
        >
          {{ index + 1 }}
        </div>
        <span
          :class="cn(
            'text-sm font-medium',
            index <= active ? 'text-foreground' : 'text-muted-foreground'
          )"
        >
          {{ step.title }}
        </span>
      </div>
      <div
        v-if="index < steps.length - 1"
        class="h-px w-8 sm:w-16 bg-border"
        :class="index < active ? 'bg-primary' : ''"
      />
    </template>
  </div>
</template>
