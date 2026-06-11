<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown } from 'lucide-vue-next'
import { cn } from '@/lib/utils'
import { RButton } from '@/components/ui'

const props = withDefaults(
  defineProps<{
    title: string
    defaultOpen?: boolean
    class?: string
  }>(),
  { defaultOpen: false }
)

const open = ref(props.defaultOpen)
</script>

<template>
  <div :class="cn('rounded-[var(--r-radius)] bg-bg-muted/50', $props.class)">
    <RButton
      variant="ghost"
      class="w-full justify-between px-4 py-3 h-auto font-medium text-[14px]"
      @click="open = !open"
    >
      {{ title }}
      <ChevronDown :class="cn('h-4 w-4 transition-transform duration-200', open && 'rotate-180')" />
    </RButton>
    <div v-show="open" class="px-4 pb-4">
      <slot />
    </div>
  </div>
</template>
