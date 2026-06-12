<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { RPageShell, RCard, RBadge } from '@/components/ui'
import { getOfferStrategy, type OfferStrategy } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import { Sparkles, Loader2, Shield, TrendingUp, AlertTriangle, DollarSign, Swords } from 'lucide-vue-next'

const route = useRoute()
const cid = Number(route.params.candidateId)
const data = ref<OfferStrategy | null>(null)

onMounted(async () => {
  try {
    const jobId = route.params.jobId ? Number(route.params.jobId) : undefined
    const res = await getOfferStrategy(cid, jobId)
    data.value = res.data
  } catch (e: any) {
    toast.error(e?.message || '加载 Offer 策略失败')
  }
})

function riskBadge(level: string) {
  return level === 'HIGH' ? 'danger' : level === 'MEDIUM' ? 'warning' : 'secondary'
}
</script>

<template>
  <RPageShell :title="`Offer策略 — ${data?.candidateName || ''}`" :subtitle="data?.jobTitle">
    <div v-if="!data" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>
    <template v-else>
      <!-- 建议薪酬区间 -->
      <RCard padding="md" class="mb-4">
        <h3 class="font-semibold text-[15px] text-text-primary mb-4 flex items-center gap-2">
          <DollarSign class="h-4 w-4 text-blue-500" />建议薪酬区间
        </h3>
        <div class="flex items-end gap-6 mb-4">
          <div class="text-center">
            <p class="text-[12px] text-text-secondary mb-1">下限</p>
            <p class="text-[18px] font-bold text-text-secondary">{{ (data.suggestedRange.min / 10000).toFixed(0) }}万</p>
          </div>
          <div class="text-center bg-primary-light rounded-[var(--r-radius)] px-8 py-4 border-2 border-primary/20">
            <p class="text-[12px] text-primary mb-1 font-medium">建议</p>
            <p class="text-[28px] font-bold text-primary">{{ (data.suggestedRange.mid / 10000).toFixed(0) }}万</p>
          </div>
          <div class="text-center">
            <p class="text-[12px] text-text-secondary mb-1">上限</p>
            <p class="text-[18px] font-bold text-text-secondary">{{ (data.suggestedRange.max / 10000).toFixed(0) }}万</p>
          </div>
          <RBadge variant="outline">{{ data.suggestedRange.currency }}</RBadge>
        </div>
        <div class="h-2 bg-bg-muted rounded-full overflow-hidden">
          <div class="h-full bg-gradient-to-r from-gray-300 via-primary to-gray-300" style="width:100%" />
        </div>
      </RCard>

      <!-- 方案对比 -->
      <RCard v-if="data.strategyOptions?.length" padding="md" class="mb-4">
        <h3 class="font-semibold text-[15px] text-text-primary mb-4 flex items-center gap-2">
          <TrendingUp class="h-4 w-4 text-green-500" />三方案对比
        </h3>
        <div class="grid grid-cols-3 gap-3">
          <div v-for="opt in data.strategyOptions" :key="opt.name"
               class="p-3 rounded-lg border text-center"
               :class="opt.name.includes('标准') ? 'border-blue-300 bg-blue-50' : 'border-gray-200'">
            <p class="font-semibold text-sm mb-2"
               :class="opt.name.includes('标准') ? 'text-blue-700' : 'text-gray-700'">{{ opt.name }}</p>
            <p class="text-lg font-bold text-gray-900 mb-1">{{ opt.totalComp }}</p>
            <div class="text-xs text-gray-500 space-y-0.5 mb-2">
              <p>base: {{ opt.baseSalary }}</p>
              <p>奖金: {{ opt.bonusPercent }}</p>
              <p v-if="opt.signingBonus !== '0'">签字费: {{ opt.signingBonus }}</p>
              <p>期权: {{ opt.options }}</p>
            </div>
            <p class="text-xs text-green-600">{{ opt.pros }}</p>
            <p class="text-xs text-red-400 mt-1">{{ opt.cons }}</p>
          </div>
        </div>
      </RCard>

      <!-- 竞品场景模拟 -->
      <RCard v-if="data.competingScenarios?.length" padding="md" class="mb-4">
        <h3 class="font-semibold text-[15px] text-text-primary mb-4 flex items-center gap-2">
          <Swords class="h-4 w-4 text-orange-500" />竞品场景模拟
        </h3>
        <div class="space-y-3">
          <div v-for="sc in data.competingScenarios" :key="sc.scenario"
               class="p-4 rounded-lg border" :class="sc.riskLevel === 'HIGH' ? 'border-red-200 bg-red-50' : sc.riskLevel === 'MEDIUM' ? 'border-orange-200 bg-orange-50' : 'border-green-200 bg-green-50'">
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-2">
                <RBadge :variant="sc.riskLevel === 'HIGH' ? 'danger' : sc.riskLevel === 'MEDIUM' ? 'warning' : 'secondary'" size="sm">
                  胜率 {{ sc.winProbability }}
                </RBadge>
                <span class="font-semibold text-sm">{{ sc.scenario }}</span>
              </div>
              <span class="text-xs text-gray-500">{{ sc.competitor }}</span>
            </div>
            <div class="grid grid-cols-2 gap-3 text-xs">
              <div>
                <span class="text-gray-400">竞品出价：</span>
                <span class="text-red-600">{{ sc.theirOffer }}</span>
              </div>
              <div>
                <span class="text-gray-400">我方建议：</span>
                <span class="text-green-600">{{ sc.ourSuggested }}</span>
              </div>
            </div>
            <p class="text-xs text-gray-600 mt-2">{{ sc.recommendation }}</p>
          </div>
        </div>
      </RCard>

      <!-- 薪酬结构 -->
      <RCard padding="md" class="mb-4">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3">建议薪酬结构</h3>
        <div class="space-y-3">
          <div v-for="c in data.components" :key="c.type" class="flex justify-between items-center p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <span class="font-medium text-[13px] text-text-primary">{{ { base: '基本工资', bonus: '目标奖金', signing: '签字费', options: '期权/股票' }[c.type] || c.type }}</span>
            <div class="text-right">
              <span class="font-semibold text-[14px] text-text-primary">{{ c.amount > 0 ? (c.amount / 10000).toFixed(0) + '万' : '待定' }}</span>
              <p class="text-[12px] text-text-secondary">{{ c.note }}</p>
            </div>
          </div>
        </div>
      </RCard>

      <!-- 谈判策略 -->
      <RCard padding="md" class="!bg-primary-light/50 !border-primary/20 mb-4">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2">
          <Sparkles class="h-4 w-4 text-primary" />谈判策略
        </h3>
        <p class="text-[13px] text-text-secondary mb-3">{{ data.strategySummary }}</p>
        <ul class="space-y-2">
          <li v-for="(t, i) in data.negotiationTips" :key="i" class="flex items-start gap-2 text-[13px] text-text-primary">
            <span class="text-primary mt-0.5">•</span>{{ t }}
          </li>
        </ul>
      </RCard>

      <!-- 风险 -->
      <RCard v-if="data.risks?.length" padding="md">
        <h3 class="font-semibold text-[15px] text-warning mb-3 flex items-center gap-2">
          <AlertTriangle class="h-4 w-4 text-orange-500" />风险提示
        </h3>
        <div class="space-y-2">
          <div v-for="(r, i) in data.risks" :key="i" class="flex items-center gap-3 text-[13px] p-2 rounded-[var(--r-radius)] border border-warning/20">
            <RBadge :variant="riskBadge(r.severity)" size="sm">{{ r.severity }}</RBadge>
            <span class="text-text-primary">{{ r.risk }}</span>
          </div>
        </div>
      </RCard>
    </template>
  </RPageShell>
</template>
