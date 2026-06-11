<script setup lang="ts">
import { ChevronRight } from 'lucide-vue-next'
import { cn } from '@/lib/utils'
import type { TreeNode } from './TreeSelect.vue'

const props = defineProps<{
  node: TreeNode
  depth?: number
  expanded: Set<string>
  selected?: number | string | null
}>()

const emit = defineEmits<{
  toggle: [key: string]
  select: [id: number | string]
}>()

const key = String(props.node.id)
const hasChildren = (props.node.children?.length ?? 0) > 0

function onClick() {
  if (hasChildren) emit('toggle', key)
  else emit('select', props.node.id)
}
</script>

<template>
  <div>
    <button
      type="button"
      :class="cn(
        'flex w-full items-center gap-1 rounded px-2 py-1.5 text-sm hover:bg-accent text-left',
        selected === node.id && 'bg-accent font-medium'
      )"
      :style="{ paddingLeft: `${(depth ?? 0) * 12 + 8}px` }"
      @click="onClick"
    >
      <ChevronRight
        v-if="hasChildren"
        :class="cn('h-3 w-3 shrink-0 transition-transform', expanded.has(key) && 'rotate-90')"
      />
      <span class="truncate">{{ node.label }}</span>
    </button>
    <template v-if="hasChildren && expanded.has(key)">
      <TreeSelectNode
        v-for="child in node.children"
        :key="String(child.id)"
        :node="child"
        :depth="(depth ?? 0) + 1"
        :expanded="expanded"
        :selected="selected"
        @toggle="emit('toggle', $event)"
        @select="emit('select', $event)"
      />
    </template>
  </div>
</template>
