<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RPageShell, RCard, RButton } from '@/components/ui'
import { getWeeklyReport, type WeeklyHealthReport } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import { CheckCircle, AlertTriangle, Loader2 } from 'lucide-vue-next'

const report = ref<WeeklyHealthReport | null>(null)
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
    const res = await getWeeklyReport()
    if (res?.data) report.value = res.data
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
      <!-- Score hero -->
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

      <!-- Highlights -->
      <RCard v-if="report.highlights?.length" padding="md" class="mb-6 !bg-success-light/50 !border-success/20">
        <h3 class="font-semibold text-[15px] text-text-primary mb-2 flex items-center gap-2">
          <CheckCircle class="h-4 w-4 text-success" />
          做得好的
        </h3>
        <ul class="list-disc pl-6 text-[13px] space-y-1 text-text-primary">
          <li v-for="(h, i) in report.highlights" :key="i">{{ h }}</li>
        </ul>
      </RCard>

      <!-- Concerns -->
      <div v-if="report.concerns?.length" class="mb-6">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2">
          <AlertTriangle class="h-4 w-4 text-warning" />
          需要关注
        </h3>
        <ul class="list-disc pl-6 text-[13px] space-y-1 text-text-primary">
          <li v-for="(c, i) in report.concerns" :key="i">{{ c }}</li>
        </ul>
      </div>

      <!-- Quick actions -->
      <RCard padding="md">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3">快捷操作</h3>
        <div class="flex gap-3">
          <RButton variant="outline" size="sm" @click="$router.push('/insight/funnel')">查看招聘漏斗</RButton>
          <RButton variant="outline" size="sm" @click="$router.push('/insight/health')">查看进化健康</RButton>
          <RButton variant="outline" size="sm" @click="$router.push('/ai/diagnose')">AI 诊断</RButton>
        </div>
      </RCard>
    </template>
  </RPageShell>
</template>
