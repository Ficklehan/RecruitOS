<script setup lang="ts">
import { computed } from 'vue'
import { RPagination, RSelect } from '@/components/ui'

const props = defineProps<{
  total: number
  pageNum: number
  pageSize: number
}>()

const emit = defineEmits<{
  'update:pageNum': [value: number]
  'update:pageSize': [value: number]
  change: []
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const pageSizeOptions = [
  { label: '10 条/页', value: 10 },
  { label: '20 条/页', value: 20 },
  { label: '50 条/页', value: 50 },
  { label: '100 条/页', value: 100 },
]

function onPageChange(page: number) {
  emit('update:pageNum', page)
  emit('change')
}

function onSizeChange(size: string | number | null) {
  if (size === null) return
  emit('update:pageSize', Number(size))
  emit('update:pageNum', 1)
  emit('change')
}
</script>

<template>
  <div class="flex flex-col gap-3 pt-4 sm:flex-row sm:items-center sm:justify-between border-t border-divider">
    <p class="text-[13px] text-text-secondary">共 {{ total }} 条</p>
    <div class="flex flex-wrap items-center gap-3">
      <RSelect
        :model-value="pageSize"
        :options="pageSizeOptions"
        class="w-[120px]"
        @update:model-value="onSizeChange"
      />
      <RPagination
        :model-value="pageNum"
        :total="total"
        :page-size="pageSize"
        @update:model-value="onPageChange"
      />
    </div>
  </div>
</template>
