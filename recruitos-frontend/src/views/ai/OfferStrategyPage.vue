<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { RPageShell, RCard, RBadge } from '@/components/ui'
import { getOfferStrategy, type OfferStrategy } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import { Sparkles, Loader2 } from 'lucide-vue-next'

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
</script>

<template>
  <RPageShell :title="`Offer策略 — ${data?.candidateName || ''}`" :subtitle="data?.jobTitle">
    <div v-if="!data" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>
    <template v-else>
      <RCard padding="md" class="mb-6">
        <h3 class="font-semibold text-[15px] text-text-primary mb-4">建议薪酬区间</h3>
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

      <RCard padding="md" class="mb-6">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3">建议薪酬结构</h3>
        <div class="space-y-3">
          <div v-for="c in data.components" :key="c.type" class="flex justify-between items-center p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <span class="font-medium text-[13px] text-text-primary">{{ { base: '基本工资', bonus: '目标奖金', options: '期权/股票' }[c.type] || c.type }}</span>
            <div class="text-right">
              <span class="font-semibold text-[14px] text-text-primary">{{ c.amount > 0 ? (c.amount / 10000).toFixed(0) + '万' : '待定' }}</span>
              <p class="text-[12px] text-text-secondary">{{ c.note }}</p>
            </div>
          </div>
        </div>
      </RCard>

      <RCard padding="md" class="!bg-primary-light/50 !border-primary/20 mb-6">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2"><Sparkles class="h-4 w-4 text-primary" />谈判策略</h3>
        <p class="text-[13px] text-text-secondary mb-3">{{ data.strategySummary }}</p>
        <ul class="space-y-2">
          <li v-for="(t, i) in data.negotiationTips" :key="i" class="flex items-start gap-2 text-[13px] text-text-primary"><span class="text-primary mt-0.5">•</span>{{ t }}</li>
        </ul>
      </RCard>

      <RCard v-if="data.risks?.length" padding="md">
        <h3 class="font-semibold text-[15px] text-warning mb-3">风险提示</h3>
        <div class="space-y-2">
          <div v-for="(r, i) in data.risks" :key="i" class="flex items-center gap-3 text-[13px] p-2 rounded-[var(--r-radius)] border border-warning/20">
            <RBadge :variant="r.severity === 'HIGH' ? 'danger' : 'warning'" size="sm">{{ r.severity }}</RBadge>
            <span class="text-text-primary">{{ r.risk }}</span>
          </div>
        </div>
      </RCard>
    </template>
  </RPageShell>
</template>
