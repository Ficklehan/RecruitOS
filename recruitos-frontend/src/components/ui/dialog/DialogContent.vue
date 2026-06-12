<script setup lang="ts">
/**
 * DialogContent — pass-through wrapper for RDialog body content.
 * Use inside <RDialog> to structure dialog layout with DialogHeader/DialogTitle/DialogFooter.
 */
interface Props {
  class?: string
}

defineProps<Props>()
</script>

<template>
  <div :class="$props.class">
    <slot />
  </div>
</template>
