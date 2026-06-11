<script setup lang="ts">
import { ref, computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  content?: string
  side?: 'top' | 'bottom' | 'left' | 'right'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  side: 'top',
})

const show = ref(false)
</script>

<template>
  <div class="relative inline-flex" @mouseenter="show = true" @mouseleave="show = false">
    <slot />
    <Transition
      enter-active-class="transition-all duration-150"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition-all duration-100"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="show && content"
        :class="cn(
          'absolute z-[var(--r-z-tooltip)] px-2.5 py-1.5 text-[12px] text-text-inverse bg-text-primary rounded-[var(--r-radius-sm)] whitespace-nowrap pointer-events-none',
          side === 'top' && 'bottom-full left-1/2 -translate-x-1/2 mb-2',
          side === 'bottom' && 'top-full left-1/2 -translate-x-1/2 mt-2',
          side === 'left' && 'right-full top-1/2 -translate-y-1/2 mr-2',
          side === 'right' && 'left-full top-1/2 -translate-y-1/2 ml-2',
          props.class,
        )"
      >
        {{ content }}
      </div>
    </Transition>
  </div>
</template>
