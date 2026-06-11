<template>
  <div class="pd-container">
    <!-- Layer 1: Summary (always visible) -->
    <div class="pd-layer1" @click="expanded = !expanded">
      <div class="pd-summary">
        <slot name="layer1">
          <span class="pd-title">{{ title }}</span>
          <span v-if="badge" class="pd-badge" :class="badgeClass">{{ badge }}</span>
        </slot>
      </div>
      <button class="pd-toggle">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          :class="{ rotated: expanded }">
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>
    </div>

    <!-- Layer 2: Details (expandable) -->
    <div v-if="expanded" class="pd-layer2">
      <slot name="layer2">
        <p class="pd-empty">暂无详细信息</p>
      </slot>

      <!-- Layer 3: Advanced (toggleable inside layer 2) -->
      <div v-if="hasLayer3" class="pd-layer3-toggle">
        <button class="pd-advanced-btn" @click.stop="showAdvanced = !showAdvanced">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            :class="{ rotated: showAdvanced }">
            <polyline points="6 9 12 15 18 9" />
          </svg>
          {{ showAdvanced ? '收起' : '高级详情' }}
        </button>
      </div>
      <div v-if="showAdvanced && hasLayer3" class="pd-layer3">
        <slot name="layer3" />
      </div>

      <!-- Action buttons -->
      <div v-if="$slots.actions || (actions && actions.length)" class="pd-actions">
        <slot name="actions">
          <button v-for="action in actions" :key="action.label"
            class="pd-action-btn" :class="action.variant"
            @click.stop="action.onClick">
            {{ action.label }}
          </button>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, useSlots } from 'vue'

const slots = useSlots()

const props = withDefaults(defineProps<{
  title?: string
  badge?: string
  badgeClass?: string
  actions?: { label: string; variant?: string; onClick: () => void }[]
}>(), {
  actions: () => [],
})

const expanded = ref(false)
const showAdvanced = ref(false)

const hasLayer3 = computed(() => !!slots.layer3)
</script>

<style scoped>
.pd-container {
  border: 1px solid var(--r-border, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
  background: var(--r-bg-primary, #fff);
}
.pd-layer1 {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; cursor: pointer; user-select: none;
  transition: background 0.15s;
}
.pd-layer1:hover { background: var(--r-bg-secondary, #f9fafb); }
.pd-summary { display: flex; align-items: center; gap: 8px; flex: 1; }
.pd-title { font-size: 14px; font-weight: 600; }
.pd-badge {
  font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 500;
  background: #eef2ff; color: #4f46e5;
}
.pd-toggle {
  background: none; border: none; cursor: pointer; padding: 4px; color: var(--r-text-secondary, #6b7280);
  display: flex; align-items: center; transition: transform 0.2s;
}
.pd-toggle svg.rotated { transform: rotate(180deg); }

.pd-layer2 {
  padding: 0 16px 16px; border-top: 1px solid var(--r-border, #f3f4f6);
  animation: pd-slide-in 0.2s ease;
}
@keyframes pd-slide-in { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }
.pd-empty { font-size: 13px; color: var(--r-text-secondary, #9ca3af); padding: 8px 0; margin: 0; }

.pd-layer3-toggle { margin-top: 8px; }
.pd-advanced-btn {
  display: flex; align-items: center; gap: 4px; background: none; border: none;
  font-size: 12px; color: #6366f1; cursor: pointer; padding: 4px 0;
}
.pd-advanced-btn svg.rotated { transform: rotate(180deg); }
.pd-layer3 {
  margin-top: 8px; padding: 12px; background: var(--r-bg-secondary, #f9fafb);
  border-radius: 6px; font-size: 13px;
}

.pd-actions {
  display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap;
}
.pd-action-btn {
  padding: 6px 14px; border-radius: 6px; font-size: 13px; font-weight: 500;
  border: 1px solid var(--r-border, #d1d5db); background: #fff;
  cursor: pointer; transition: all 0.15s;
}
.pd-action-btn:hover { border-color: #6366f1; color: #6366f1; }
.pd-action-btn.primary { background: #6366f1; color: #fff; border-color: #6366f1; }
.pd-action-btn.primary:hover { background: #4f46e5; }
.pd-action-btn.danger { color: #dc2626; border-color: #fecaca; }
.pd-action-btn.danger:hover { background: #fef2f2; }
</style>
