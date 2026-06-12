<script setup lang="ts">
/**
 * DrawerContent — pass-through wrapper for RDrawer/RSheet body content.
 * Use inside <RDrawer> or <RSheet> to structure drawer layout.
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
