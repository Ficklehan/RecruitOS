<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RPageShell, RCard, RButton, RBadge } from '@/components/ui'
import { getWeeklyReport, getLessons, getPatterns, type WeeklyHealthReport } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import { CheckCircle, AlertTriangle, Loader2, Sparkles, BookOpen, Target } from 'lucide-vue-next'

const report = ref<WeeklyHealthReport | null>(null)
const lessons = ref<any[]>([])
const patterns = ref<any[]>([])
const loading = ref(false)

const periodLabel = computed(() => {
  if (!report.value) return ''
  return `${report.value.periodStart} — ${report.value.periodEnd}`
})

const scoreColor = computed(() => {
  const s = report.value?.overallScore ?? 0
  if (s >= 80) return '#16A34A'
  if (s >= 60) return '#F59E0B'
  return '#EF4444'
})

onMounted(async () => {
  loading.value = true
  try {
    const [reportRes, lessonsRes, patternsRes] = await Promise.all([
      getWeeklyReport().catch(() => ({ data: null } as any)),
      getLessons().catch(() => ({ data: [] } as any)),
      getPatterns('CANDIDATE_SOURCE_PERFORMANCE').catch(() => ({ data: [] } as any)),
    ])
    report.value = (reportRes as any).data
    lessons.value = ((lessonsRes as any).data || []).slice(0, 3)
    patterns.value = ((patternsRes as any).data || []).slice(0, 3)
  } catch (e: any) {
    toast.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <RPageShell title="招聘健康周报" :subtitle="periodLabel">
    <div v-if="loading" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <template v-else-if="report">
      <!-- Score -->
      <div class="flex justify-center mb-8">
        <div class="flex items-center gap-5">
          <div class="w-20 h-20 rounded-full border-4 flex items-center justify-center" :style="{ borderColor: scoreColor }">
            <span class="text-[32px] font-extrabold" :style="{ color: scoreColor }">{{ report.overallScore }}</span>
          </div>
          <div>
            <span class="text-[16px] font-semibold text-text-primary block">招聘健康分</span>
            <span class="text-[14px] font-semibold" :class="report.scoreChange >= 0 ? 'text-success' : 'text-danger'">
              {{ report.scoreChange >= 0 ? '↑' : '↓' }} {{ Math.abs(report.scoreChange) }}
              <span class="text-text-secondary text-[12px] font-normal">较上周</span>
            </span>
          </div>
        </div>
      </div>

      <!-- Metrics -->
      <RCard v-if="report.metrics?.length" padding="md" class="mb-6">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3">关键指标</h3>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div v-for="m in report.metrics" :key="m.name" class="p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <p class="text-[12px] text-text-secondary">{{ m.name }}</p>
            <p class="text-[18px] font-bold text-text-primary mt-1">{{ m.value }}{{ m.unit }}</p>
            <p class="text-[12px] mt-1" :class="m.trend >= 0 ? 'text-success' : 'text-danger'">
              {{ m.trend >= 0 ? '↑' : '↓' }} {{ Math.abs(m.trend) }}%
            </p>
          </div>
        </div>
      </RCard>

      <!-- Highlights & Concerns -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <RCard v-if="report.highlights?.length" padding="md" class="!bg-success-light/50 !border-success/20">
          <h3 class="font-semibold text-[15px] text-text-primary mb-2 flex items-center gap-2">
            <CheckCircle class="h-4 w-4 text-success" /> 做得好的
          </h3>
          <ul class="list-disc pl-6 text-[13px] space-y-1 text-text-primary">
            <li v-for="(h, i) in report.highlights" :key="i">{{ h }}</li>
          </ul>
        </RCard>

        <div v-if="report.concerns?.length">
          <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2">
            <AlertTriangle class="h-4 w-4 text-warning" /> 需要关注
          </h3>
          <ul class="list-disc pl-6 text-[13px] space-y-1 text-text-primary">
            <li v-for="(c, i) in report.concerns" :key="i">{{ c }}</li>
          </ul>
        </div>
      </div>

      <!-- v8: AI 发现的模式 -->
      <RCard v-if="patterns.length" padding="md" class="mb-6 border-l-4 border-blue-200">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2">
          <Target class="h-4 w-4 text-blue-500" /> AI 本周发现的规律
        </h3>
        <div class="space-y-2">
          <div v-for="(p, i) in patterns" :key="i"
            class="flex items-start gap-2 p-2 bg-blue-50 rounded text-sm">
            <Sparkles class="h-3.5 w-3.5 text-blue-500 shrink-0 mt-0.5" />
            <div>
              <span class="font-medium">{{ p.patternName }}</span>
              <span class="text-text-secondary"> — {{ (() => { try { return JSON.parse(p.patternRule).statement } catch { return '' } })() }}</span>
            </div>
          </div>
        </div>
      </RCard>

      <!-- v8: 教训提醒 -->
      <RCard v-if="lessons.length" padding="md" class="mb-6 border-l-4 border-amber-200">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2">
          <BookOpen class="h-4 w-4 text-amber-500" /> 值得注意的教训
        </h3>
        <div class="space-y-2">
          <div v-for="(l, i) in lessons" :key="i"
            class="flex items-start gap-2 p-2 bg-amber-50 rounded text-sm">
            <AlertTriangle class="h-3.5 w-3.5 text-amber-500 shrink-0 mt-0.5" />
            <span class="text-text-primary">{{ l.title }}<span v-if="l.correctiveAction" class="text-green-700"> — {{ l.correctiveAction }}</span></span>
          </div>
        </div>
      </RCard>

      <!-- Quick actions -->
      <RCard padding="md">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3">快捷操作</h3>
        <div class="flex gap-3 flex-wrap">
          <RButton variant="outline" size="sm" @click="$router.push('/insight/alerts')">查看洞察</RButton>
          <RButton variant="outline" size="sm" @click="$router.push('/knowledge/lessons')">教训库</RButton>
          <RButton variant="outline" size="sm" @click="$router.push('/knowledge/profiles')">成功画像</RButton>
        </div>
      </RCard>
    </template>
  </RPageShell>
</template>
