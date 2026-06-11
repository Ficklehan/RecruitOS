<script setup lang="ts">
import { watch, onMounted, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'
import { X } from 'lucide-vue-next'

interface Props {
  modelValue?: boolean
  title?: string
  side?: 'left' | 'right'
  width?: string
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  side: 'right',
  width: '400px',
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
      <div v-if="modelValue" class="fixed inset-0 z-[var(--r-z-drawer)]">
        <div class="absolute inset-0 bg-black/40" @click="close" />
        <Transition
          enter-active-class="transition-transform duration-300 ease-out"
          :enter-from-class="side === 'right' ? 'translate-x-full' : '-translate-x-full'"
          enter-to-class="translate-x-0"
          leave-active-class="transition-transform duration-200 ease-in"
          leave-from-class="translate-x-0"
          :leave-to-class="side === 'right' ? 'translate-x-full' : '-translate-x-full'"
        >
          <div
            v-if="modelValue"
            :class="cn(
              'absolute top-0 bottom-0 bg-bg-card r-shadow-xl flex flex-col',
              side === 'right' ? 'right-0' : 'left-0',
              props.class,
            )"
            :style="{ width }"
          >
            <div v-if="title" class="flex items-center justify-between px-5 py-4 border-b border-divider">
              <h2 class="text-[16px] font-semibold text-text-primary">{{ title }}</h2>
              <button class="p-1 rounded-[var(--r-radius-sm)] hover:bg-bg-muted transition-colors" @click="close">
                <X class="h-4 w-4 text-text-secondary" />
              </button>
            </div>
            <div class="flex-1 overflow-auto p-5">
              <slot />
            </div>
            <div v-if="$slots.footer" class="px-5 py-4 border-t border-divider flex justify-end gap-3">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
