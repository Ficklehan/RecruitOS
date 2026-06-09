<template>
  <div class="empty-state-cta" :class="{ 'empty-state-cta--compact': compact }">
    <el-empty :description="description" :image-size="imageSize">
      <template v-if="title" #default>
        <p class="empty-title">{{ title }}</p>
      </template>
    </el-empty>
    <div v-if="actions.length" class="empty-actions">
      <el-button
        v-for="action in actions"
        :key="action.label"
        :type="action.type || 'default'"
        @click="action.onClick"
      >
        {{ action.label }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  description: string
  imageSize?: number
  compact?: boolean
  actions?: { label: string; type?: '' | 'default' | 'primary' | 'success' | 'warning' | 'info' | 'danger'; onClick: () => void }[]
}>(), {
  imageSize: 72,
  compact: false,
  actions: () => [],
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.empty-state-cta {
  text-align: center;
  padding: 32px 24px 28px;
  width: 100%;
}

.empty-state-cta--compact {
  padding: 20px 16px;
}

.empty-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
}
</style>
