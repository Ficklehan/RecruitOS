<script setup lang="ts">
import { ref, useSlots, computed } from 'vue'
import { cn } from '@/lib/utils'
import { RCard, RButton, RSkeleton } from '@/components/ui'
import { ChevronDown, ChevronUp } from 'lucide-vue-next'

interface Props {
  title?: string
  subtitle?: string
  variant?: 'default' | 'list'
  constrained?: boolean
  bodyClass?: string
  class?: string
  loading?: boolean
  padded?: boolean
  plain?: boolean
  filtersCollapsed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  constrained: false,
  bodyClass: '',
  loading: false,
  padded: false,
  plain: false,
  filtersCollapsed: false,
})

const slots = useSlots()
const showAllFilters = ref(false)

const isList = computed(() => props.variant === 'list')
const hasFilters = computed(() => Boolean(slots.filters))
const showHeader = computed(() => Boolean(props.title || props.subtitle || slots.heading || slots.actions))
</script>

<template>
  <div
    :class="cn(
      'w-full min-w-0 flex flex-col',
      isList ? 'space-y-4' : 'gap-6',
      constrained && 'max-w-[960px]',
      props.class,
    )"
  >
    <!-- Header -->
    <header v-if="showHeader" class="flex items-start justify-between gap-4">
      <div class="min-w-0 flex-1">
        <h1 v-if="title" class="text-[20px] font-semibold text-text-primary leading-tight">{{ title }}</h1>
        <p v-if="subtitle" :class="cn('text-[13px] text-text-secondary mt-1', !title && 'mt-0')">{{ subtitle }}</p>
        <slot name="heading" />
      </div>
      <div v-if="$slots.actions" class="flex items-center gap-2 shrink-0">
        <slot name="actions" />
      </div>
    </header>

    <div v-if="$slots.toolbar">
      <slot name="toolbar" />
    </div>

    <!-- Filters (list variant) -->
    <template v-if="isList && hasFilters">
      <RCard :class="cn('!p-4', !showAllFilters && filtersCollapsed && 'overflow-hidden max-h-24')">
        <div class="flex flex-wrap items-center gap-3">
          <slot name="filters" />
        </div>
        <div v-if="$slots.filterActions || hasFilters" class="flex items-center gap-2 mt-3 pt-3 border-t border-divider">
          <slot name="filterActions" />
          <RButton variant="ghost" size="sm" class="ml-auto text-text-secondary" @click="showAllFilters = !showAllFilters">
            {{ showAllFilters ? '收起筛选' : '展开筛选' }}
            <ChevronUp v-if="showAllFilters" class="ml-1 h-4 w-4" />
            <ChevronDown v-else class="ml-1 h-4 w-4" />
          </RButton>
        </div>
      </RCard>
    </template>

    <div v-else-if="$slots.filters">
      <slot name="filters" />
    </div>

    <!-- Content (list variant) -->
    <template v-if="isList">
      <RCard :class="cn(padded && '!p-6', plain && '!bg-transparent !shadow-none !p-0')">
        <div v-if="loading" class="p-6 space-y-4">
          <RSkeleton :count="3" />
        </div>
        <slot v-else />
      </RCard>
      <slot name="below" />
    </template>

    <!-- Content (default variant) -->
    <section v-else :class="cn('flex flex-col gap-6 min-w-0', bodyClass)">
      <slot />
    </section>
  </div>
</template>
