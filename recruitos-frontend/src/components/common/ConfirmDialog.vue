<script setup lang="ts">
import { RDialog, RButton } from '@/components/ui'

defineProps<{
  open: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  destructive?: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  confirm: []
  cancel: []
}>()

function onCancel() {
  emit('update:open', false)
  emit('cancel')
}

function onConfirm() {
  emit('confirm')
}
</script>

<template>
  <RDialog :model-value="open" :title="title" @update:model-value="emit('update:open', $event)">
    <p class="text-[14px] text-text-secondary leading-relaxed">{{ message }}</p>
    <template #footer>
      <RButton variant="outline" @click="onCancel">{{ cancelText ?? '取消' }}</RButton>
      <RButton :variant="destructive ? 'danger' : 'primary'" @click="onConfirm">
        {{ confirmText ?? '确定' }}
      </RButton>
    </template>
  </RDialog>
</template>
