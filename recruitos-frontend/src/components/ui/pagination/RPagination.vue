<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

interface Props {
  modelValue?: number
  total?: number
  pageSize?: number
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: 1,
  total: 0,
  pageSize: 10,
})

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(Number(props.total) / Number(props.pageSize))))

const pages = computed(() => {
  const result: (number | '...')[] = []
  const current = props.modelValue
  const total = totalPages.value
  if (total <= 7) {
    for (let i = 1; i <= total; i++) result.push(i)
  } else {
    result.push(1)
    if (current > 3) result.push('...')
    for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
      result.push(i)
    }
    if (current < total - 2) result.push('...')
    result.push(total)
  }
  return result
})

function goTo(page: number) {
  if (page >= 1 && page <= totalPages.value) emit('update:modelValue', page)
}
</script>

<template>
  <div v-if="Number(total) > 0" :class="cn('flex items-center gap-1', props.class)">
    <button
      :class="cn('w-8 h-8 flex items-center justify-center rounded-[var(--r-radius-sm)] text-[13px] transition-colors', modelValue <= 1 ? 'text-text-disabled cursor-not-allowed' : 'text-text-secondary hover:bg-bg-muted')"
      :disabled="modelValue <= 1"
      @click="goTo(modelValue - 1)"
    >
      <ChevronLeft class="h-4 w-4" />
    </button>
    <template v-for="p in pages" :key="p">
      <span v-if="p === '...'" class="w-8 h-8 flex items-center justify-center text-text-placeholder text-[13px]">...</span>
      <button
        v-else
        :class="cn(
          'w-8 h-8 flex items-center justify-center rounded-[var(--r-radius-sm)] text-[13px] font-medium transition-colors',
          p === modelValue ? 'bg-primary text-white' : 'text-text-secondary hover:bg-bg-muted',
        )"
        @click="goTo(p)"
      >
        {{ p }}
      </button>
    </template>
    <button
      :class="cn('w-8 h-8 flex items-center justify-center rounded-[var(--r-radius-sm)] text-[13px] transition-colors', modelValue >= totalPages ? 'text-text-disabled cursor-not-allowed' : 'text-text-secondary hover:bg-bg-muted')"
      :disabled="modelValue >= totalPages"
      @click="goTo(modelValue + 1)"
    >
      <ChevronRight class="h-4 w-4" />
    </button>
  </div>
</template>
