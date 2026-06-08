<template>
  <el-breadcrumb separator="/" class="breadcrumb">
    <el-breadcrumb-item :to="{ path: '/workspace/dashboard' }">
      <el-icon><HomeFilled /></el-icon>
    </el-breadcrumb-item>
    <el-breadcrumb-item
      v-for="item in breadcrumbs"
      :key="item.path"
      :to="item.path ? { path: item.path } : undefined"
    >
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.map(item => ({
    title: item.meta.title as string,
    path: item.redirect ? undefined : item.path,
  }))
})

watch(breadcrumbs, (val) => {
  appStore.setBreadcrumbs(val)
}, { immediate: true })
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.breadcrumb {
  margin-bottom: $spacing-lg;
  font-size: 13px;

  :deep(.el-breadcrumb__item) {
    .el-breadcrumb__inner {
      color: $text-placeholder;
      font-weight: 400;
      transition: color $transition-fast;

      &:hover {
        color: $primary-color;
      }
    }

    &:last-child .el-breadcrumb__inner {
      color: $text-secondary;
      font-weight: 500;
    }
  }

  :deep(.el-breadcrumb__separator) {
    color: $text-disabled;
    font-size: 12px;
  }
}
</style>
