<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'
import { ChevronDown, Check, X } from 'lucide-vue-next'

interface Option {
  label: string
  value: string | number
}

interface Props {
  modelValue?: string | number | null
  options?: Option[]
  placeholder?: string
  disabled?: boolean
  clearable?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  options: () => [],
  placeholder: '请选择',
  disabled: false,
  clearable: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | null]
}>()

const open = ref(false)
const containerRef = ref<HTMLElement | null>(null)

const selectedLabel = computed(() => {
  const opt = props.options.find(o => o.value === props.modelValue)
  return opt?.label || ''
})

function select(value: string | number | null) {
  emit('update:modelValue', value)
  open.value = false
}

function handleClickOutside(e: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<template>
  <div ref="containerRef" :class="cn('relative', props.class)">
    <button
      type="button"
      :class="cn(
        'w-full h-9 px-3 text-[14px] rounded-[var(--r-radius)]',
        'bg-bg-muted border-none text-left flex items-center justify-between gap-2',
        'transition-all duration-200 cursor-pointer',
        'focus:outline-none focus:ring-2 focus:ring-primary/20',
        'disabled:opacity-50 disabled:cursor-not-allowed',
        !selectedLabel && 'text-text-placeholder',
      )"
      :disabled="disabled"
      @click="open = !open"
    >
      <span class="truncate">{{ selectedLabel || placeholder }}</span>
      <div class="flex items-center gap-1">
        <X v-if="clearable && modelValue" class="h-3.5 w-3.5 text-text-placeholder hover:text-text-secondary" @click.stop="select(null)" />
        <ChevronDown class="h-4 w-4 text-text-placeholder transition-transform duration-200" :class="open && 'rotate-180'" />
      </div>
    </button>

    <Transition
      enter-active-class="transition-all duration-200"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-150"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <div v-if="open" class="absolute z-[var(--r-z-dropdown)] mt-1 w-full bg-bg-card rounded-[var(--r-radius)] r-shadow-lg max-h-60 overflow-auto">
        <div
          v-for="opt in options"
          :key="opt.value"
          :class="cn(
            'px-3 py-2 text-[14px] cursor-pointer flex items-center justify-between',
            'hover:bg-bg-hover transition-colors duration-150',
            opt.value === modelValue && 'bg-primary-light text-primary font-medium',
          )"
          @click="select(opt.value)"
        >
          <span class="truncate">{{ opt.label }}</span>
          <Check v-if="opt.value === modelValue" class="h-4 w-4 shrink-0" />
        </div>
        <div v-if="!options.length" class="px-3 py-4 text-center text-text-placeholder text-[13px]">暂无选项</div>
      </div>
    </Transition>
  </div>
</template>
