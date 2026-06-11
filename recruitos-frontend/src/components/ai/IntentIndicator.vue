<template>
  <span class="intent-dot-wrapper" :class="{ clickable: !!candidateId }" @mouseenter="show = true" @mouseleave="show = false" @click.stop="candidateId && emit('click', candidateId)">
    <span class="intent-dot" :class="colorClass" />
    <Transition name="tooltip-fade">
      <div v-if="show && intent" class="intent-tooltip">
        <div class="tooltip-header">
          <span class="font-semibold">意向 {{ intent.intentScore?.toFixed(0) }} 分</span>
          <span class="text-xs text-muted-foreground">{{ label }}</span>
        </div>
        <div v-if="intent.riskFactors?.length" class="tooltip-risks">
          <span v-for="r in intent.riskFactors.slice(0,2)" :key="r.factor" class="risk-tag">{{ r.factor }}</span>
        </div>
        <div class="tooltip-confidence">置信度 {{ ((intent.confidence || 0) * 100).toFixed(0) }}%</div>
      </div>
    </Transition>
  </span>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { CandidateIntent } from '@/api/modules/brain'

const props = defineProps<{ intent?: CandidateIntent | null; loading?: boolean; candidateId?: number }>()
const emit = defineEmits<{ click: [candidateId: number] }>()
const show = ref(false)

const colorClass = computed(() => {
  if (props.loading) return 'dot-loading'
  if (!props.intent) return 'dot-unknown'
  if (props.intent.intentLevel === 'HIGH') return 'dot-high'
  if (props.intent.intentLevel === 'MEDIUM') return 'dot-medium'
  return 'dot-low'
})

const label = computed(() => {
  if (!props.intent) return '未评估'
  if (props.intent.intentLevel === 'HIGH') return '高意向'
  if (props.intent.intentLevel === 'MEDIUM') return '中等'
  return '低意向'
})
</script>

<style scoped>
.intent-dot-wrapper { position: relative; display: inline-flex; align-items: center; }
.intent-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot-high { background: #16a34a; box-shadow: 0 0 4px rgba(22,163,74,.4); }
.dot-medium { background: #f59e0b; box-shadow: 0 0 4px rgba(245,158,11,.4); }
.dot-low { background: #ef4444; box-shadow: 0 0 4px rgba(239,68,68,.4); }
.dot-unknown { background: #cbd5e1; }
.dot-loading { background: #94a3b8; animation: pulse 1s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: .4; } }

.intent-tooltip {
  position: absolute; bottom: calc(100% + 8px); left: 50%; transform: translateX(-50%);
  background: #fff; border: 1px solid #e2e8f0; border-radius: 8px;
  padding: 8px 12px; min-width: 160px; z-index: 999;
  box-shadow: 0 4px 12px rgba(0,0,0,.1); font-size: 12px; white-space: nowrap;
}
.tooltip-header { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 4px; }
.tooltip-risks { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 2px; }
.risk-tag { font-size: 10px; background: #fef3c7; color: #92400e; padding: 1px 6px; border-radius: 4px; }
.tooltip-confidence { font-size: 10px; color: #94a3b8; }
.tooltip-fade-enter-active, .tooltip-fade-leave-active { transition: opacity .15s; }
.tooltip-fade-enter-from, .tooltip-fade-leave-to { opacity: 0; }
.clickable { cursor: pointer; }
.clickable:hover .intent-dot { transform: scale(1.3); transition: transform .15s; }
</style>
