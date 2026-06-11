<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { RPageShell, RCard, RBadge } from '@/components/ui'
import { getCyclePrediction, type CyclePrediction } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import { Sparkles, Loader2 } from 'lucide-vue-next'

const route = useRoute()
const jobId = Number(route.params.jobId)
const data = ref<CyclePrediction | null>(null)

onMounted(async () => {
  try {
    const res = await getCyclePrediction(jobId)
    data.value = res.data
  } catch (e: any) {
    toast.error(e?.message || '加载周期预测失败')
  }
})
</script>

<template>
  <RPageShell :title="data?.jobTitle || '招聘周期预测'">
    <div v-if="!data" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>
    <template v-else>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <RCard padding="md" class="text-center">
          <p class="text-[12px] text-text-secondary mb-1">预计到岗</p>
          <p class="text-[28px] font-bold text-primary">{{ data.estimatedDays }}<span class="text-[14px] font-normal text-text-secondary"> 天</span></p>
          <p class="text-[12px] text-text-secondary mt-1">{{ data.minDays }}–{{ data.maxDays }} 天（80%置信区间）</p>
        </RCard>
        <RCard :class="cn('p-5 text-center !border', data.riskLevel === 'HIGH' ? '!bg-danger-light/30 !border-danger/20' : data.riskLevel === 'MEDIUM' ? '!bg-warning-light/30 !border-warning/20' : '!bg-success-light/30 !border-success/20')">
          <p class="text-[12px] text-text-secondary mb-1">风险等级</p>
          <RBadge :variant="data.riskLevel === 'HIGH' ? 'danger' : data.riskLevel === 'MEDIUM' ? 'warning' : 'success'" class="text-[14px] px-4 py-1 mt-2">
            {{ data.riskLevel === 'HIGH' ? '高风险' : data.riskLevel === 'MEDIUM' ? '中等风险' : '低风险' }}
          </RBadge>
        </RCard>
        <RCard padding="md" class="text-center">
          <p class="text-[12px] text-text-secondary mb-1">置信度</p>
          <p class="text-[28px] font-bold text-text-primary">{{ ((data.confidence || 0) * 100).toFixed(0) }}%</p>
        </RCard>
      </div>

      <RCard v-if="data.bottlenecks?.length" padding="md" class="mb-6">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3">瓶颈识别</h3>
        <div class="space-y-3">
          <div v-for="b in data.bottlenecks" :key="b.stage" class="flex items-start gap-3 p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <span class="shrink-0 w-20 text-[13px] font-medium text-text-secondary">{{ b.stage }}阶段</span>
            <div class="flex-1">
              <p class="text-[13px] font-medium text-text-primary">{{ b.issue }}</p>
              <p class="text-[12px] text-text-secondary">影响 +{{ b.impact }} 天</p>
            </div>
          </div>
        </div>
      </RCard>

      <RCard v-if="data.interventions?.length" padding="md" class="!bg-primary-light/50 !border-primary/20">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2"><Sparkles class="h-4 w-4 text-primary" />AI 干预建议</h3>
        <div class="space-y-3">
          <div v-for="(iv, i) in data.interventions" :key="i" class="flex items-start gap-3">
            <span class="shrink-0 w-6 h-6 rounded-full bg-primary text-white text-[12px] flex items-center justify-center font-medium">{{ i + 1 }}</span>
            <div>
              <p class="text-[13px] font-medium text-text-primary">{{ iv.action }}</p>
              <p class="text-[12px] text-text-secondary">{{ iv.expectedEffect }} · 预计 {{ iv.effortDays }} 天内见效</p>
            </div>
          </div>
        </div>
      </RCard>
    </template>
  </RPageShell>
</template>
