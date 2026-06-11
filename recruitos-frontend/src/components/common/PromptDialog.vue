<script setup lang="ts">
import { promptState, finishPrompt, validatePromptInput } from '@/lib/prompt'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Button,
  Input,
  Label,
} from '@/components/ui'

function onCancel() {
  finishPrompt(null)
}

function onConfirm() {
  if (!validatePromptInput()) return
  finishPrompt(promptState.value.value)
}
</script>

<template>
  <Dialog :open="promptState.open" @update:open="(v: boolean) => !v && onCancel()">
    <DialogContent class="max-w-md">
      <DialogHeader>
        <DialogTitle>{{ promptState.options.title }}</DialogTitle>
      </DialogHeader>
      <p v-if="promptState.options.message" class="text-sm text-muted-foreground">
        {{ promptState.options.message }}
      </p>
      <div class="space-y-2">
        <Label :for="'prompt-input'">输入</Label>
        <Input
          id="prompt-input"
          v-model="promptState.value"
          :placeholder="promptState.options.placeholder"
          @keyup.enter="onConfirm"
        />
        <p v-if="promptState.error" class="text-xs text-destructive">{{ promptState.error }}</p>
      </div>
      <DialogFooter class="gap-2 sm:gap-0">
        <Button variant="outline" @click="onCancel">{{ promptState.options.cancelText ?? '取消' }}</Button>
        <Button @click="onConfirm">{{ promptState.options.confirmText ?? '确定' }}</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
