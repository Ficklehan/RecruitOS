<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChevronRight } from 'lucide-vue-next'
import { cn } from '@/lib/utils'
import { RButton, RDropdown } from '@/components/ui'
import TreeSelectNode from './TreeSelectNode.vue'

export interface TreeNode {
  id: number | string
  label: string
  children?: TreeNode[]
}

const props = defineProps<{
  modelValue?: number | string | null
  nodes: TreeNode[]
  placeholder?: string
  class?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | string | null]
}>()

const open = ref(false)
const expanded = ref<Set<string>>(new Set())

function toggleExpand(id: string) {
  const s = new Set(expanded.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  expanded.value = s
}

function findLabel(nodes: TreeNode[], id: number | string | null | undefined): string {
  if (id == null) return ''
  for (const n of nodes) {
    if (n.id === id) return n.label
    if (n.children?.length) {
      const found = findLabel(n.children, id)
      if (found) return found
    }
  }
  return ''
}

const displayLabel = computed(() => findLabel(props.nodes, props.modelValue) || props.placeholder || '请选择')

function select(id: number | string) {
  emit('update:modelValue', id)
  open.value = false
}
</script>

<template>
  <RDropdown v-model:open="open" :class="cn('w-full', $props.class)">
    <template #trigger>
      <RButton variant="outline" class="w-full justify-between font-normal h-9">
        <span :class="!modelValue && 'text-text-placeholder'">{{ displayLabel }}</span>
        <ChevronRight class="h-4 w-4 rotate-90 opacity-50" />
      </RButton>
    </template>
    <div class="max-h-64 w-56 overflow-auto p-1">
      <TreeSelectNode
        v-for="node in nodes"
        :key="String(node.id)"
        :node="node"
        :expanded="expanded"
        :selected="modelValue"
        @toggle="toggleExpand"
        @select="select"
      />
    </div>
  </RDropdown>
</template>
