<template>
  <RPageShell title="面试评价" :subtitle="prep ? `${prep.candidateName} · ${prep.jobTitle}` : ''">
    <div v-if="!prep && !errMsg" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-muted-foreground" />
    </div>
    <div v-if="errMsg" class="text-center py-20 text-muted-foreground">{{ errMsg }}</div>

    <template v-if="prep">
      <!-- AI prep panel -->
      <RCard class="p-5 mb-6 bg-blue-50/30 border-blue-100">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles class="h-4 w-4 text-blue-500" />
          <span class="font-semibold text-sm">面试准备</span>
          <RBadge variant="outline" class="text-xs">你的评估维度：{{ prep.evaluatorDimension }}（权重{{ (prep.dimensionWeight * 100).toFixed(0) }}%）</RBadge>
        </div>

        <div v-if="prep.resumeSignals.length" class="mb-3">
          <p class="text-xs font-medium text-muted-foreground mb-2">简历关键信号</p>
          <div class="space-y-1">
            <div v-for="sig in prep.resumeSignals" :key="sig.content" class="flex items-start gap-2 text-sm">
              <RBadge :variant="sig.type === 'strong' ? 'default' : sig.type === 'concern' ? 'destructive' : 'outline'" class="text-xs shrink-0">
                {{ sig.type === 'strong' ? '强信号' : sig.type === 'concern' ? '⚠ 注意' : sig.type === 'moderate' ? '中等' : '弱信号' }}
              </RBadge>
              <span>{{ sig.content }}<span v-if="sig.detail" class="text-muted-foreground"> — {{ sig.detail }}</span></span>
            </div>
          </div>
        </div>

        <div v-if="prep.suggestedQuestions.length" class="mb-2">
          <p class="text-xs font-medium text-muted-foreground mb-1">AI 建议追问</p>
          <ul class="list-disc pl-4 text-sm space-y-0.5">
            <li v-for="q in prep.suggestedQuestions" :key="q">{{ q }}</li>
          </ul>
        </div>

        <div v-if="prep.cautions.length">
          <p class="text-xs font-medium text-muted-foreground mb-1">注意</p>
          <ul class="list-disc pl-4 text-sm space-y-0.5 text-amber-700">
            <li v-for="c in prep.cautions" :key="c">{{ c }}</li>
          </ul>
        </div>
      </RCard>

      <!-- Scoring form -->
      <RCard class="p-6 mb-6">
        <h3 class="font-semibold mb-4">评分</h3>

        <div
          v-for="(dim, di) in scorecard.dimensions"
          :key="dim.name"
          class="dimension-block"
        >
          <div class="flex justify-between items-baseline mb-2">
            <span class="font-medium">{{ dim.name }}</span>
            <span class="text-sm text-muted-foreground">权重 {{ (scorecardWeights[di] * 100).toFixed(0) }}%</span>
          </div>

          <!-- Score buttons -->
          <div class="score-row">
            <button
              v-for="s in 5"
              :key="s"
              class="score-btn"
              :class="{ active: dim.score === s }"
              @click="dim.score = s"
            >
              {{ s }}
            </button>
          </div>

          <!-- Anchor description -->
          <div v-if="prep.scoreAnchors.length && dim.score" class="anchor-box">
            <p class="text-xs text-muted-foreground">
              {{ dim.score }}分标准：{{ prep.scoreAnchors.find(a => a.score === dim.score)?.description || '—' }}
            </p>
          </div>

          <!-- Evidence -->
          <div class="mt-3">
            <label class="text-xs text-muted-foreground">证据（候选人的具体回答或行为）</label>
            <RTextarea v-model="dim.evidence" :rows="2" :placeholder="'例如：候选人描述了...'" class="mt-1" />
          </div>

          <!-- Confidence -->
          <div class="mt-2 flex items-center gap-2">
            <span class="text-xs text-muted-foreground">置信度</span>
            <RSelect v-model="dim.confidence" :options="confidenceOptions" class="w-24" size="sm" />
          </div>
        </div>
      </RCard>

      <!-- Overall decision -->
      <RCard class="p-6 mb-6">
        <h3 class="font-semibold mb-3">综合决策</h3>
        <div class="flex gap-3 flex-wrap">
          <RButton
            v-for="opt in decisionOptions"
            :key="opt.value"
            :variant="scorecard.decision === opt.value ? 'default' : 'outline'"
            @click="scorecard.decision = opt.value"
          >
            {{ opt.label }}
          </RButton>
        </div>
      </RCard>

      <div class="flex gap-3">
        <RButton :disabled="!canSubmit" @click="handleSubmit">提交评价</RButton>
        <RButton variant="outline" @click="$router.back()">暂存</RButton>
      </div>
    </template>
  </RPageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Sparkles, Loader2 } from 'lucide-vue-next'
import { RPageShell, RCard, RButton, RBadge, RTextarea, RSelect } from '@/components/ui'
import { getInterviewPrep, submitScorecard, type InterviewPrep } from '@/api/modules/brain'
import { toast } from '@/lib/notify'

const route = useRoute()
const router = useRouter()
const interviewId = computed(() => Number(route.params.interviewId))
const prep = ref<InterviewPrep | null>(null)
const errMsg = ref('')
const loading = ref(false)

const defaultDimensions = [
  { name: '评估维度一', score: 0, evidence: '', confidence: 'MEDIUM' },
  { name: '评估维度二', score: 0, evidence: '', confidence: 'MEDIUM' },
  { name: '评估维度三', score: 0, evidence: '', confidence: 'MEDIUM' },
  { name: '评估维度四', score: 0, evidence: '', confidence: 'MEDIUM' },
]

const scorecardWeights = computed(() => {
  const n = scorecard.dimensions.length
  const w = 1 / n
  return scorecard.dimensions.map(() => w)
})

const scorecard = reactive({
  dimensions: defaultDimensions.map(d => ({ ...d })),
  decision: '' as string,
})

const confidenceOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
]

const decisionOptions = [
  { label: '强烈推荐', value: 'STRONG_HIRE' },
  { label: '推荐录用', value: 'HIRE' },
  { label: '倾向录用', value: 'LEANING_HIRE' },
  { label: '倾向不录用', value: 'LEANING_NO' },
  { label: '不推荐', value: 'NO_HIRE' },
]

const canSubmit = computed(() =>
  scorecard.dimensions.every(d => d.score > 0 && d.evidence.trim()) && scorecard.decision
)

onMounted(async () => {
  try {
    const res = await getInterviewPrep(interviewId.value)
    prep.value = res.data
    if (prep.value?.dimensionCoverage?.length) {
      scorecard.dimensions = prep.value.dimensionCoverage.map(d => ({
        name: d.dimension, score: 0, evidence: '', confidence: 'MEDIUM' as string,
      }))
    } else if (prep.value) {
      scorecard.dimensions[0].name = prep.value.evaluatorDimension
    }
  } catch {
    errMsg.value = '加载面试信息失败'
  }
})

async function handleSubmit() {
  const dimsWithScores = scorecard.dimensions.filter(d => d.score > 0)
  const weights = scorecardWeights.value
  const overall = dimsWithScores.reduce((sum, d, i) => sum + d.score * (weights[i] || 0), 0) / dimsWithScores.length
  try {
    await submitScorecard({
      interviewId: interviewId.value,
      dimensions: dimsWithScores.map(d => ({ name: d.name, score: d.score, evidence: d.evidence, confidence: d.confidence })),
      overallScore: Math.round(overall * 20),
      decision: scorecard.decision,
    })
    toast.success('评价已提交')
    router.push('/workspace/inbox')
  } catch (e: any) {
    toast.error(e?.message || '提交失败')
  }
}
</script>

<style scoped lang="scss">
.dimension-block {
  padding: 16px 0;
  border-bottom: 1px solid var(--r-bg-muted);
  &:last-child { border-bottom: none; }
}
.score-row { display: flex; gap: 8px; }
.score-btn {
  width: 40px; height: 40px;
  border: 1px solid var(--r-text-disabled);
  border-radius: 8px;
  background: var(--r-bg-card);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all .15s;
  &:hover { border-color: #3b82f6; }
  &.active { background: #3b82f6; color: var(--r-bg-card); border-color: #3b82f6; }
}
.anchor-box {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0fdf4;
  border-radius: 6px;
  border-left: 3px solid var(--r-success);
}
</style>
