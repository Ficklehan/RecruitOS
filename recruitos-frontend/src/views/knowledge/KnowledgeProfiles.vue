<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPatterns } from '@/api/modules/brain'
import { RCard, RBadge, REmpty, RSkeleton } from '@/components/ui'
import { Target, TrendingUp, Shield, AlertTriangle } from 'lucide-vue-next'

const loading = ref(true)
const patterns = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    const [sourceRes, skillRes] = await Promise.all([
      getPatterns('CANDIDATE_SOURCE_PERFORMANCE').catch(() => ({ data: [] }) as any),
      getPatterns('SKILL_RETENTION_CORRELATION').catch(() => ({ data: [] }) as any),
    ])
    patterns.value = [
      ...((sourceRes as any).data || []),
      ...((skillRes as any).data || []),
    ]
  } finally { loading.value = false }
}

function parsePatternRule(p: any): any {
  try { return typeof p.patternRule === 'string' ? JSON.parse(p.patternRule) : p.patternRule }
  catch { return { statement: p.patternName || '未知模式' } }
}

onMounted(load)
</script>

<template>
  <div class="p-6 max-w-3xl mx-auto space-y-6">
    <div class="flex items-center gap-3 mb-6">
      <Target class="h-6 w-6 text-blue-500" />
      <div>
        <h1 class="text-lg font-semibold text-text-primary">成功画像</h1>
        <p class="text-sm text-text-placeholder">AI 从历史招聘数据中自动发现的规律</p>
      </div>
    </div>

    <RSkeleton v-if="loading" class="h-32" />
    <REmpty v-else-if="!patterns.length" description="数据积累不足，AI 尚未发现足够可靠的模式。当积累更多录用和离职数据后，AI 将自动在此展示发现。" />

    <RCard v-for="(p, i) in patterns" :key="i" class="p-4 border-l-4 border-blue-200">
      <div class="flex items-start gap-3">
        <component :is="p.patternType?.includes('DEPARTURE') ? AlertTriangle : p.patternType?.includes('BIAS') ? Shield : TrendingUp"
          class="h-5 w-5 mt-0.5"
          :class="p.patternType?.includes('DEPARTURE') ? 'text-red-400' : p.patternType?.includes('BIAS') ? 'text-amber-400' : 'text-green-400'" />
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <h3 class="text-sm font-semibold text-text-primary">{{ p.patternName }}</h3>
            <RBadge v-if="p.confidence > 0.7" variant="default" size="sm">置信度 {{ (p.confidence * 100).toFixed(0) }}%</RBadge>
            <RBadge v-else variant="warning" size="sm">置信度 {{ (p.confidence * 100).toFixed(0) }}%</RBadge>
          </div>
          <p class="text-sm text-text-secondary">{{ parsePatternRule(p).statement }}</p>
          <p class="text-xs text-text-placeholder mt-1">基于 {{ p.sampleSize }} 个样本 · {{ p.discoveredAt?.substring(0, 10) }}</p>
        </div>
      </div>
    </RCard>

    <RCard v-if="!loading" class="p-4 bg-blue-50 border-blue-200">
      <h3 class="text-sm font-semibold text-blue-800 mb-2">💡 什么是成功画像？</h3>
      <p class="text-sm text-blue-700">
        AI 自动分析所有录用-离职-绩效数据，回答"什么样的候选人更可能成功"。
        不是人为设定的标签，而是从真实业务结果中统计出来的规律。
        每次新的录用和离职事件都会让这些画像更精确。
      </p>
    </RCard>
  </div>
</template>
