<template>
  <div class="stat-card">
    <div class="stat-icon-wrap" :style="iconStyle">
      <el-icon :size="20">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="stat-body">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">{{ formattedValue }}</div>
      <div class="stat-trend" v-if="trend !== undefined">
        <span :class="trend >= 0 ? 'trend-up' : 'trend-down'">
          <el-icon :size="12"><Top v-if="trend >= 0" /><Bottom v-else /></el-icon>
          {{ Math.abs(trend) }}%
        </span>
        <span class="trend-label">较上周</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  label: string
  value: string | number
  icon?: string
  color?: string
  trend?: number
}

const props = withDefaults(defineProps<Props>(), {
  icon: 'DataLine',
  color: '#3B82F6',
})

const formattedValue = computed(() => {
  if (typeof props.value === 'number' && props.value >= 1000) {
    return props.value.toLocaleString()
  }
  return props.value
})

const iconStyle = computed(() => ({
  background: `${props.color}14`,
  color: props.color,
}))
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.stat-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $border-radius;
  padding: $spacing-lg $spacing-xl;
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  min-height: 88px;
}

.stat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: $border-radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: $spacing-sm;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1.2;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.stat-trend {
  margin-top: 6px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;

  .trend-up {
    color: $success-color;
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-weight: 500;
  }

  .trend-down {
    color: $danger-color;
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-weight: 500;
  }

  .trend-label {
    color: $text-placeholder;
  }
}
</style>
