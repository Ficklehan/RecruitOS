<script setup lang="ts">
import { computed, type Component } from 'vue'
import { resolveActionIcon } from '@/lib/actionIcons'
import { cn } from '@/lib/utils'
import { RButton, RTooltip, RDropdown } from '@/components/ui'
import { MoreHorizontal } from 'lucide-vue-next'

export interface ActionItem {
  command: string
  label: string
  icon?: string | Component
  type?: 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link'
  divided?: boolean
  primary?: boolean
}

const props = withDefaults(defineProps<{
  actions: ActionItem[]
  maxVisible?: number
}>(), {
  maxVisible: 1,
})

const emit = defineEmits<{
  action: [command: string]
}>()

const visibleActions = computed(() =>
  props.actions.filter(a => a.primary !== false).slice(0, props.maxVisible)
)

const dropdownActions = computed(() => {
  const visible = new Set(visibleActions.value.map(a => a.command))
  return props.actions.filter(a => !visible.has(a.command))
})
</script>

<template>
  <div class="flex items-center gap-1">
    <RTooltip v-for="action in visibleActions" :key="action.command" :content="action.label">
      <RButton variant="ghost" size="sm" class="p-1.5" @click="emit('action', action.command)">
        <component v-if="action.icon" :is="resolveActionIcon(action.icon)" class="h-4 w-4" />
      </RButton>
    </RTooltip>

    <RDropdown v-if="dropdownActions.length">
      <template #trigger>
        <RButton variant="ghost" size="sm" class="p-1.5">
          <MoreHorizontal class="h-4 w-4" />
        </RButton>
      </template>
      <div class="w-[160px] py-1">
        <button
          v-for="action in dropdownActions"
          :key="action.command"
          :class="cn(
            'w-full px-3 py-2 text-[13px] text-left flex items-center gap-2 hover:bg-bg-hover transition-colors',
            action.type === 'destructive' ? 'text-danger' : 'text-text-primary',
          )"
          @click="emit('action', action.command)"
        >
          <component v-if="action.icon" :is="resolveActionIcon(action.icon)" class="h-4 w-4 shrink-0" />
          {{ action.label }}
        </button>
      </div>
    </RDropdown>
  </div>
</template>
