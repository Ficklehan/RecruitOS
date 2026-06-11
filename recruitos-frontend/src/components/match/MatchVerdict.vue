<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import { RBadge } from '@/components/ui'
import { Info } from 'lucide-vue-next'
import { parseMatchDetail } from '@/utils/matchVerdict'

interface Props {
  matchScore?: number | null
  matchDetail?: string | null
  mode?: 'compact' | 'card' | 'full'
  showScore?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'card',
  showScore: false,
})

const verdict = computed(() => parseMatchDetail(props.matchDetail, props.matchScore))

const confidenceLabel = computed(() => {
  const m: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return m[verdict.value.confidence || ''] || verdict.value.confidence
})

const badgeVariant = computed(() => {
  const variants: Record<string, 'default' | 'success' | 'danger' | 'warning' | 'info' | 'outline'> = {
    STRONG: 'success',
    MODERATE: 'info',
    WEAK: 'warning',
    POOR: 'danger',
  }
  return variants[verdict.value.status] || 'default'
})
</script>

<template>
  <div :class="cn('text-[14px] leading-relaxed', props.class)">
    <div class="flex items-start gap-2 flex-wrap">
      <RBadge :variant="badgeVariant" class="shrink-0">
        {{ verdict.label }}
      </RBadge>
      <span v-if="verdict.summary && mode !== 'compact'" class="text-text-secondary text-[12px]">
        {{ verdict.summary }}
      </span>
    </div>

    <div v-if="mode !== 'compact'" class="mt-3">
      <div v-if="verdict.pros?.length" class="mb-2">
        <div class="font-semibold text-[12px] text-success mb-1">符合点</div>
        <ul class="list-disc pl-4 space-y-0.5">
          <li v-for="(p, i) in verdict.pros" :key="'p'+i" class="text-[13px]">{{ p }}</li>
        </ul>
      </div>

      <div v-if="verdict.cons?.length" class="mb-2">
        <div class="font-semibold text-[12px] text-warning mb-1">待确认</div>
        <ul class="list-disc pl-4 space-y-0.5">
          <li v-for="(c, i) in verdict.cons" :key="'c'+i" class="text-[13px]">{{ c }}</li>
        </ul>
      </div>

      <div v-if="verdict.suggestedAction" class="flex items-start gap-1 p-2 bg-bg-muted rounded-[var(--r-radius-sm)] text-[12px] text-text-secondary mt-2">
        <Info class="h-4 w-4 shrink-0 mt-0.5" />
        <span>
          建议下一步：{{ verdict.suggestedAction }}
          <span v-if="verdict.autoActionAllowed === false" class="text-text-placeholder">（需您确认后执行）</span>
        </span>
      </div>
    </div>

    <div v-if="showScore && verdict.modelScore != null" class="mt-2 text-[12px] text-text-secondary">
      参考分 {{ verdict.modelScore }}
      <span v-if="verdict.confidence"> · 置信度 {{ confidenceLabel }}</span>
      <span> · 仅供辅助判断</span>
    </div>
  </div>
</template>
