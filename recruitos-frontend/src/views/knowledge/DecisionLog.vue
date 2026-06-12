<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTimeline, getJudgment } from '@/api/modules/brain'
import { RCard, RBadge, REmpty, RSkeleton } from '@/components/ui'
import { History, Check, X, TrendingUp, AlertTriangle } from 'lucide-vue-next'

const loading = ref(true)
const events = ref<any[]>([])

const eventLabel: Record<string, string> = {
  HIRE: '录用', REJECT: '拒绝', DEPARTURE: '离职',
  OFFER_NEGOTIATION: 'Offer谈判', SCREENING_OVERRIDE: 'HR覆盖AI筛选',
}

const eventColor: Record<string, string> = {
  HIRE: 'text-green-600', REJECT: 'text-red-600', DEPARTURE: 'text-amber-600',
}

async function load() {
  loading.value = true
  try {
    // Load recent HIRE and REJECT events from cognitive memory
    const hireEvents = await Promise.all([
      getTimeline('CANDIDATE', 1).catch(() => ({ data: [] } as any)),
    ])

    // Combine and sort
    const all = [...((hireEvents[0] as any).data || [])]
    all.sort((a: any, b: any) => (b.occurredAt || '').localeCompare(a.occurredAt || ''))
    events.value = all.filter((e: any) => ['HIRE', 'REJECT', 'DEPARTURE'].includes(e.eventType))
  } finally { loading.value = false }
}

function parseContext(ctx: string): any {
  try { return JSON.parse(ctx) } catch { return {} }
}

onMounted(load)
</script>

<template>
  <div class="p-6 max-w-3xl mx-auto space-y-4">
    <div class="flex items-center gap-3 mb-6">
      <History class="h-6 w-6 text-indigo-500" />
      <div>
        <h1 class="text-lg font-semibold text-text-primary">决策记录</h1>
        <p class="text-sm text-text-placeholder">每个录用和拒绝决策的完整上下文，供 AI 学习和校准</p>
      </div>
    </div>

    <RSkeleton v-if="loading" class="h-32" />
    <REmpty v-else-if="!events.length"
      description="暂无决策记录。当系统积累更多录用/拒绝数据后，AI 将在此展示决策模式分析。" />

    <RCard v-for="(evt, i) in events" :key="i"
      class="p-4 border-l-4"
      :class="evt.eventType === 'HIRE' ? 'border-green-200' : evt.eventType === 'DEPARTURE' ? 'border-red-200' : 'border-gray-200'">
      <div class="flex items-start justify-between">
        <div class="flex items-center gap-2">
          <component :is="evt.eventType === 'HIRE' ? Check : evt.eventType === 'DEPARTURE' ? AlertTriangle : X"
            class="h-4 w-4" :class="eventColor[evt.eventType] || 'text-gray-400'" />
          <RBadge :variant="evt.eventType === 'HIRE' ? 'default' : evt.eventType === 'DEPARTURE' ? 'danger' : 'outline'" size="sm">
            {{ eventLabel[evt.eventType] || evt.eventType }}
          </RBadge>
          <span class="text-xs text-text-placeholder">{{ evt.occurredAt?.substring(0, 16) }}</span>
        </div>
        <RBadge v-if="evt.decisionQuality === 'POOR'" variant="danger" size="sm">⚠ 不良决策</RBadge>
        <RBadge v-else-if="evt.decisionQuality === 'GOOD'" variant="default" size="sm">良好</RBadge>
      </div>
      <div v-if="evt.outcomeReason" class="mt-2 text-sm text-text-secondary">
        原因：{{ evt.outcomeReason }}
      </div>
      <div v-if="evt.decisionQuality === 'POOR' && evt.outcomeReason" class="mt-2 p-2 bg-red-50 rounded text-xs text-red-700">
        ⚠️ 此决策被标记为不良。AI 已将其纳入教训库，避免类似模式重现。
      </div>
    </RCard>

    <RCard v-if="!loading" class="p-4 bg-indigo-50 border-indigo-200">
      <h3 class="text-sm font-semibold text-indigo-800 mb-2">📋 决策记录如何驱动 AI 进化</h3>
      <p class="text-sm text-indigo-700">
        每个录用/拒绝决策被记录后，AI 持续对比面试评分与实际入职表现。
        当员工离职时，AI 回溯面试时的所有信号，寻找可预防的模式。
        不良决策的模式会被提炼为教训，面试官评分偏差会被追踪校准。
      </p>
    </RCard>
  </div>
</template>
