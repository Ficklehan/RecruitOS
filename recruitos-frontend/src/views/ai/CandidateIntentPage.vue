<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { RPageShell, RCard, RBadge, RButton } from '@/components/ui'
import { getCandidateIntent, type CandidateIntent } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import { Sparkles, Loader2, Calendar, MessageCircle, User } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const data = ref<CandidateIntent | null>(null)

onMounted(async () => {
  try {
    const res = await getCandidateIntent(Number(route.params.candidateId))
    data.value = res.data
  } catch (e: any) {
    toast.error(e?.message || '加载意向预测失败')
  }
})
</script>

<template>
  <RPageShell :title="`意向预测 — ${data?.candidateName || ''}`" :subtitle="data?.jobTitle">
    <div v-if="!data" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>
    <template v-else>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <RCard padding="md" class="text-center">
          <p class="text-[12px] text-text-secondary mb-1">意向评分</p>
          <div class="relative w-20 h-20 mx-auto my-2">
            <svg viewBox="0 0 36 36" class="w-full h-full -rotate-90">
              <circle cx="18" cy="18" r="15.9" fill="none" stroke="currentColor" stroke-width="3" class="text-bg-muted" />
              <circle cx="18" cy="18" r="15.9" fill="none" :stroke="data.intentScore >= 70 ? '#16A34A' : data.intentScore >= 40 ? '#F59E0B' : '#EF4444'" stroke-width="3" :stroke-dasharray="`${data.intentScore} 100`" stroke-linecap="round" />
            </svg>
            <span class="absolute inset-0 flex items-center justify-center text-[18px] font-bold text-text-primary">{{ data.intentScore?.toFixed(0) }}</span>
          </div>
        </RCard>
        <RCard padding="md" class="text-center">
          <p class="text-[12px] text-text-secondary mb-1">意向等级</p>
          <RBadge :variant="data.intentLevel === 'HIGH' ? 'success' : data.intentLevel === 'MEDIUM' ? 'warning' : 'danger'" class="text-[14px] px-5 py-1.5 mt-4">
            {{ { HIGH: '高意向', MEDIUM: '中等', LOW: '低意向' }[data.intentLevel] }}
          </RBadge>
        </RCard>
        <RCard padding="md" class="text-center">
          <p class="text-[12px] text-text-secondary mb-1">置信度</p>
          <p class="text-[28px] font-bold text-text-primary mt-2">{{ ((data.confidence || 0) * 100).toFixed(0) }}%</p>
        </RCard>
      </div>

      <RCard v-if="data.riskFactors?.length" padding="md" class="mb-6">
        <h3 class="font-semibold text-[15px] text-warning mb-3">流失风险</h3>
        <div class="space-y-2">
          <div v-for="(r, i) in data.riskFactors" :key="i" class="flex items-start gap-3 p-3 rounded-[var(--r-radius)]" :class="r.level === 'CRITICAL' ? 'bg-danger-light border border-danger/20' : r.level === 'HIGH' ? 'bg-warning-light border border-warning/20' : 'bg-bg-muted'">
            <RBadge :variant="r.level === 'CRITICAL' ? 'danger' : r.level === 'HIGH' ? 'warning' : 'outline'" class="text-[11px] shrink-0">{{ r.level }}</RBadge>
            <div><p class="text-[13px] font-medium text-text-primary">{{ r.factor }}</p><p class="text-[12px] text-text-secondary">{{ r.detail }}</p></div>
          </div>
        </div>
      </RCard>

      <RCard v-if="data.interventionSuggestions?.length" padding="md" class="!bg-primary-light/50 !border-primary/20">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3 flex items-center gap-2"><Sparkles class="h-4 w-4 text-primary" />干预建议</h3>
        <ul class="space-y-2">
          <li v-for="(s, i) in data.interventionSuggestions" :key="i" class="flex items-start gap-2 text-[13px] text-text-primary"><span class="text-primary">•</span>{{ s }}</li>
        </ul>
        <div class="flex flex-wrap gap-2 mt-4">
          <RButton size="sm" @click="router.push(`/pipeline/candidates/${data.candidateId}`)">
            <User class="mr-1.5 h-3.5 w-3.5" />候选人工作台
          </RButton>
          <RButton size="sm" variant="outline" @click="router.push(`/pipeline/calendar`)">
            <Calendar class="mr-1.5 h-3.5 w-3.5" />安排沟通
          </RButton>
          <RButton size="sm" variant="outline" @click="router.push(`/talent/conversations`)">
            <MessageCircle class="mr-1.5 h-3.5 w-3.5" />发送跟进
          </RButton>
        </div>
      </RCard>
    </template>
  </RPageShell>
</template>
