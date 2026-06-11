<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
const open = ref(false)
const containerRef = ref<HTMLElement | null>(null)

function handleClickOutside(e: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<template>
  <div ref="containerRef" :class="cn('relative inline-flex', props.class)">
    <div @click="open = !open">
      <slot name="trigger" />
    </div>
    <Transition
      enter-active-class="transition-all duration-150"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-100"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <div
        v-if="open"
        class="absolute z-[var(--r-z-dropdown)] mt-1 min-w-[160px] bg-bg-card rounded-[var(--r-radius)] r-shadow-lg py-1 border border-divider"
        @click="open = false"
      >
        <slot />
      </div>
    </Transition>
  </div>
</template>
