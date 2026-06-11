<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'
import { X } from 'lucide-vue-next'

interface Props {
  modelValue?: boolean
  title?: string
  description?: string
  width?: string
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  width: '480px',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function close() { emit('update:modelValue', false) }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
}

onMounted(() => document.addEventListener('keydown', handleKeydown))
onUnmounted(() => document.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="modelValue" class="fixed inset-0 z-[var(--r-z-modal)] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40" @click="close" />
        <Transition
          enter-active-class="transition-all duration-200"
          enter-from-class="opacity-0 scale-95"
          enter-to-class="opacity-100 scale-100"
          leave-active-class="transition-all duration-150"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-95"
        >
          <div
            v-if="modelValue"
            :class="cn('relative bg-bg-card rounded-[var(--r-radius-lg)] r-shadow-xl w-full max-h-[85vh] overflow-auto', props.class)"
            :style="{ maxWidth: width }"
          >
            <div v-if="title" class="flex items-center justify-between px-6 py-4 border-b border-divider">
              <div>
                <h2 class="text-[16px] font-semibold text-text-primary">{{ title }}</h2>
                <p v-if="description" class="text-[13px] text-text-secondary mt-0.5">{{ description }}</p>
              </div>
              <button class="p-1 rounded-[var(--r-radius-sm)] hover:bg-bg-muted transition-colors" @click="close">
                <X class="h-4 w-4 text-text-secondary" />
              </button>
            </div>
            <div class="p-6">
              <slot />
            </div>
            <div v-if="$slots.footer" class="px-6 py-4 border-t border-divider flex justify-end gap-3">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
