<template>
  <div class="page-container" :class="containerClass">
    <header class="page-header">
      <div class="page-header-main">
        <h2 class="page-title">{{ title }}</h2>
        <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
        <slot name="heading" />
      </div>
      <div v-if="$slots.actions" class="page-header-actions">
        <slot name="actions" />
      </div>
    </header>

    <slot name="toolbar" />

    <div v-if="$slots.filters || $slots.filterActions" class="filter-bar">
      <slot name="filters" />
      <div v-if="$slots.filterActions" class="filter-actions">
        <slot name="filterActions" />
      </div>
    </div>

    <div
      class="data-card"
      :class="{ 'data-card--padded': padded, 'data-card--plain': plain }"
      v-loading="loading"
    >
      <slot />
    </div>

    <slot name="below" />
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    loading?: boolean
    /** 内容区使用内边距（列表、卡片流） */
    padded?: boolean
    /** 无表格外壳，用于自定义布局 */
    plain?: boolean
    containerClass?: string
  }>(),
  {
    loading: false,
    padded: false,
    plain: false,
    containerClass: '',
  },
)
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.page-header-main {
  min-width: 0;
  flex: 1;
}

.page-header-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: $spacing-sm;
}

:slotted(.page-header),
.page-header {
  margin-bottom: 0;
}
</style>
