<template>
  <div class="page-shell" :class="{ 'page-shell--constrained': constrained }">
    <header v-if="showHeader" class="page-shell-header">
      <div class="page-shell-heading">
        <h1 v-if="title" class="page-shell-title">{{ title }}</h1>
        <p v-if="subtitle" class="page-shell-subtitle">{{ subtitle }}</p>
        <slot name="heading" />
      </div>
      <div v-if="$slots.actions" class="page-shell-actions">
        <slot name="actions" />
      </div>
    </header>

    <div v-if="$slots.toolbar" class="page-shell-toolbar">
      <slot name="toolbar" />
    </div>

    <div v-if="$slots.filters" class="page-shell-filters">
      <slot name="filters" />
    </div>

    <section class="page-shell-body" :class="bodyClass">
      <slot />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'

const props = withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    /** 仅表单/文章类页面需要收窄时使用 */
    constrained?: boolean
    bodyClass?: string
  }>(),
  { constrained: false, bodyClass: '' },
)

const slots = useSlots()

const showHeader = computed(
  () => Boolean(props.title || props.subtitle || slots.heading || slots.actions),
)
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.page-shell {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.page-shell--constrained {
  max-width: 960px;
}

.page-shell-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: $spacing-xl;
}

.page-shell-heading {
  min-width: 0;
  flex: 1;
}

.page-shell-title {
  font-size: $text-heading-lg;
  font-weight: 600;
  color: $text-primary;
  line-height: $line-height-heading;
  letter-spacing: -0.01em;
}

.page-shell-subtitle {
  margin-top: 4px;
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.5;
}

.page-shell-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: $spacing-sm;
}

.page-shell-body {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  min-width: 0;
}
</style>
