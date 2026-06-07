<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon :size="64" color="#94A3B8">
        <component :is="icon" />
      </el-icon>
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p class="empty-desc">{{ description }}</p>
    <slot name="action" />
  </div>
</template>

<script setup lang="ts">
interface Props {
  icon?: string
  title?: string
  description?: string
}

withDefaults(defineProps<Props>(), {
  icon: 'Document',
  title: '功能开发中',
  description: '该功能正在紧张开发中，敬请期待',
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  margin-bottom: 24px;
  opacity: 0.6;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-regular;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 24px;
}
</style>
