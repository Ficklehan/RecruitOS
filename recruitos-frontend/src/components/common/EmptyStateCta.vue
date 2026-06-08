<template>
  <div class="empty-state-cta">
    <el-empty :description="description" :image-size="imageSize">
      <template v-if="title" #default>
        <p v-if="title" class="empty-title">{{ title }}</p>
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
  actions?: { label: string; type?: '' | 'default' | 'primary' | 'success' | 'warning' | 'info' | 'danger'; onClick: () => void }[]
}>(), {
  imageSize: 80,
  actions: () => [],
})
</script>

<style scoped lang="scss">
.empty-state-cta {
  text-align: center;
  padding: 8px 0 16px;
}
.empty-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}
.empty-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: -8px;
}
</style>
