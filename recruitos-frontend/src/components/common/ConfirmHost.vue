<script setup lang="ts">
import { watch } from 'vue'
import { confirmState, finishConfirm } from '@/lib/confirm'
import ConfirmDialog from './ConfirmDialog.vue'

// Resolve promise if dialog is closed without explicit confirm/cancel
watch(() => confirmState.value.open, (open) => {
  if (!open && confirmState.value.resolve) {
    finishConfirm(false)
  }
})
</script>

<template>
  <ConfirmDialog
    v-model:open="confirmState.open"
    :title="confirmState.options.title"
    :message="confirmState.options.message"
    :confirm-text="confirmState.options.confirmText"
    :cancel-text="confirmState.options.cancelText"
    :destructive="confirmState.options.destructive"
    @confirm="finishConfirm(true)"
    @cancel="finishConfirm(false)"
  />
</template>
