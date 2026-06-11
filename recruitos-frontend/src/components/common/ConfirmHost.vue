<script setup lang="ts">
import { confirmState, finishConfirm } from '@/lib/confirm'
import ConfirmDialog from './ConfirmDialog.vue'
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
