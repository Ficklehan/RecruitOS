<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { RPageShell, RCard, RBadge } from '@/components/ui'
import { getTalentDensity, type TalentDensity } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import { Loader2 } from 'lucide-vue-next'

const route = useRoute()
const data = ref<TalentDensity | null>(null)

onMounted(async () => {
  try {
    const orgId = Number(route.params.orgId)
    const orgName = route.params.orgName as string | undefined
    const res = await getTalentDensity(orgId, orgName)
    data.value = res.data
  } catch (e: any) {
    toast.error(e?.message || '加载人才密度数据失败')
  }
})
</script>

<template>
  <RPageShell :title="`人才密度 — ${data?.orgName || ''}`">
    <div v-if="!data" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>
    <template v-else>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <RCard padding="md" class="text-center">
          <p class="text-[12px] text-text-secondary">密度指数</p>
          <p class="text-[28px] font-bold mt-1" :class="data.densityScore >= 0.85 ? 'text-success' : data.densityScore >= 0.6 ? 'text-primary' : 'text-warning'">{{ (data.densityScore * 100).toFixed(0) }}</p>
          <RBadge :variant="data.densityLevel === 'HIGH' ? 'success' : data.densityLevel === 'MEDIUM' ? 'warning' : 'danger'" class="mt-2">
            {{ { HIGH: '高密度', MEDIUM: '中等', LOW: '偏低' }[data.densityLevel] }}
          </RBadge>
        </RCard>
        <RCard padding="md">
          <p class="text-[12px] text-text-secondary mb-2">Bar Raiser 判断</p>
          <p class="text-[13px] text-text-primary">{{ data.barRaiserVerdict }}</p>
        </RCard>
        <RCard padding="md" class="text-center">
          <p class="text-[12px] text-text-secondary mb-1">置信度</p>
          <p class="text-[28px] font-bold text-text-primary">{{ ((data.confidence || 0) * 100).toFixed(0) }}%</p>
        </RCard>
      </div>

      <RCard padding="md" class="mb-6">
        <h3 class="font-semibold text-[15px] text-text-primary mb-4">能力维度热力图</h3>
        <div class="space-y-3">
          <div v-for="h in data.heatmap" :key="h.capability" class="flex items-center gap-3">
            <span class="w-24 text-[13px] shrink-0 text-text-primary">{{ h.capability }}</span>
            <div class="flex-1 h-6 bg-bg-muted rounded-full overflow-hidden relative">
              <div class="absolute inset-y-0 left-0 rounded-full transition-all" :class="h.status === 'GAP' ? 'bg-danger/60' : h.status === 'ADEQUATE' ? 'bg-warning/60' : 'bg-success/60'" :style="{ width: Math.min(100, (h.currentLevel / Math.max(h.targetLevel, 0.01) * 100)) + '%' }" />
              <div class="absolute inset-y-0 border-r-2 border-dashed border-text-placeholder" :style="{ left: (h.targetLevel * 100) + '%' }" />
            </div>
            <RBadge :variant="h.status === 'GAP' ? 'danger' : h.status === 'ADEQUATE' ? 'warning' : 'success'" size="sm">
              {{ { GAP: '缺口', ADEQUATE: '适中', SUFFICIENT: '充足' }[h.status] }}
            </RBadge>
          </div>
        </div>
        <p class="text-[12px] text-text-secondary mt-3">虚线 = 目标水位，实色 = 当前水位</p>
      </RCard>

      <RCard v-if="data.attritionImpacts?.length" padding="md" class="!bg-warning-light/50 !border-warning/20">
        <h3 class="font-semibold text-[15px] text-text-primary mb-3">流失冲击评估</h3>
        <div v-for="a in data.attritionImpacts" :key="a.capability" class="flex items-center gap-3 text-[13px] mb-2">
          <span :class="a.critical ? 'text-danger' : 'text-warning'">⚠</span>
          <span class="font-medium text-text-primary">{{ a.capability }}</span>
          <span class="text-text-secondary">影响 {{ (a.impact * 100).toFixed(0) }}%</span>
          <RBadge v-if="a.critical" variant="danger" size="sm">关键岗位</RBadge>
        </div>
      </RCard>
    </template>
  </RPageShell>
</template>
