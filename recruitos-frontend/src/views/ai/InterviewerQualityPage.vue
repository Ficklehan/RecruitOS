<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { cn } from '@/lib/utils'
import { RPageShell, RCard, RBadge, RButton } from '@/components/ui'
import { getInterviewerQuality, type InterviewerQuality } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import { Sparkles, Loader2, GraduationCap, Calendar } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const data = ref<InterviewerQuality | null>(null)

function formatTrendPeriod(period: string): string {
  if (!period) return ''
  // period format: "2026-W22" or "2026-06"
  const parts = period.split('-')
  if (period.includes('W')) return period.replace('W', '第') + '周'
  if (parts.length >= 2) return parts[1] + '月'
  return period
}

function trendBarColor(score: number): string {
  if (score >= 80) return '#16a34a'
  if (score >= 60) return '#2563eb'
  return '#d97706'
}

onMounted(async () => {
  try {
    const res = await getInterviewerQuality(Number(route.params.interviewerId))
    data.value = res.data
  } catch (e: any) {
    toast.error(e?.message || '加载面试官质量数据失败')
  }
})
</script>

<template>
  <RPageShell :title="`面试官质量 — ${data?.interviewerName || ''}`">
    <div v-if="!data" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>
    <template v-else>
      <div v-if="data.totalEvaluations < 5" class="text-center py-12 text-text-secondary">数据不足，需至少5场面试评价</div>
      <template v-else>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <RCard padding="sm" class="text-center">
            <p class="text-[12px] text-text-secondary">质量评分</p>
            <p class="text-[24px] font-bold mt-1" :class="data.qualityScore >= 80 ? 'text-success' : data.qualityScore >= 60 ? 'text-primary' : 'text-warning'">{{ data.qualityScore?.toFixed(0) }}</p>
            <RBadge :variant="data.qualityLevel === 'EXCELLENT' ? 'success' : data.qualityLevel === 'GOOD' ? 'info' : 'warning'" class="text-[11px] mt-1">
              {{ { EXCELLENT: '优秀', GOOD: '良好', NEEDS_IMPROVEMENT: '需改进' }[data.qualityLevel] || data.qualityLevel }}
            </RBadge>
          </RCard>
          <RCard padding="sm" class="text-center">
            <p class="text-[12px] text-text-secondary">评价总数</p>
            <p class="text-[24px] font-bold mt-1 text-text-primary">{{ data.totalEvaluations }}</p>
          </RCard>
          <RCard padding="sm" class="text-center">
            <p class="text-[12px] text-text-secondary">宽松指数</p>
            <p class="text-[24px] font-bold mt-1" :class="Math.abs((data.leniencyIndex || 1) - 1) > 0.2 ? 'text-warning' : 'text-success'">{{ data.leniencyIndex?.toFixed(2) }}</p>
            <p class="text-[12px] text-text-secondary">{{ data.leniencyIndex > 1.05 ? '偏松' : data.leniencyIndex < 0.95 ? '偏严' : '正常' }}</p>
          </RCard>
          <RCard padding="sm" class="text-center">
            <p class="text-[12px] text-text-secondary">预测准确度</p>
            <p class="text-[24px] font-bold mt-1 text-text-primary">{{ ((data.predictionAccuracy || 0) * 100).toFixed(0) }}%</p>
          </RCard>
        </div>

        <!-- 趋势图 -->
        <RCard v-if="data.trend?.length" padding="md" class="mb-6">
          <h3 class="font-semibold text-[15px] text-text-primary mb-4">评分趋势</h3>
          <div class="trend-chart">
            <div class="trend-bars">
              <div v-for="(t, i) in data.trend" :key="t.period" class="trend-bar-group">
                <div class="trend-ticks">
                  <div class="trend-bar-wrapper">
                    <div class="trend-bar" :style="{ height: (t.avgScore / 100 * 100) + '%', background: trendBarColor(t.avgScore) }" />
                  </div>
                  <span class="trend-value">{{ t.avgScore?.toFixed(0) }}</span>
                </div>
                <span class="trend-label">{{ formatTrendPeriod(t.period) }}</span>
              </div>
            </div>
            <div class="flex items-center gap-4 mt-3 text-[11px] text-text-secondary">
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-sm" :style="{ background: '#16a34a' }" />≥80</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-sm" :style="{ background: '#2563eb' }" />60–79</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-sm" :style="{ background: '#d97706' }" />&lt;60</span>
            </div>
          </div>
        </RCard>

        <RCard v-if="data.biasTags?.length" padding="md" class="mb-6">
          <h3 class="font-semibold text-[15px] text-text-primary mb-3">偏差检测</h3>
          <div v-for="b in data.biasTags" :key="b.tag" class="flex items-center gap-3 p-3 rounded-[var(--r-radius)] mb-2" :class="b.severity > 0.3 ? 'bg-danger-light border border-danger/20' : 'bg-warning-light border border-warning/20'">
            <RBadge variant="danger" class="text-[11px]">{{ b.tag }}</RBadge>
            <p class="text-[13px] text-text-primary">{{ b.description }}</p>
          </div>
        </RCard>

        <RCard v-if="data.coachingSuggestions?.length" padding="md" class="!bg-primary-light/50 !border-primary/20">
          <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2"><Sparkles class="h-4 w-4 text-primary" />AI Coaching 建议</h3>
          <ul class="space-y-2">
            <li v-for="(s, i) in data.coachingSuggestions" :key="i" class="flex items-start gap-2 text-[13px] text-text-primary"><span class="text-primary">•</span>{{ s }}</li>
          </ul>
          <div class="flex flex-wrap gap-2 mt-4">
            <RButton size="sm" @click="router.push('/pipeline/calendar')">
              <Calendar class="mr-1.5 h-3.5 w-3.5" />安排 Coaching 陪面
            </RButton>
            <RButton size="sm" variant="outline" @click="router.push('/insight/interviewer')">
              <GraduationCap class="mr-1.5 h-3.5 w-3.5" />查看面试效能
            </RButton>
          </div>
        </RCard>

        <div v-if="data.needsRecertification" class="p-4 bg-danger-light border border-danger/20 rounded-[var(--r-radius)] text-[13px] text-danger mb-6">
          该面试官需重新认证后才能继续独立面试
        </div>
      </template>
    </template>
  </RPageShell>
</template>
