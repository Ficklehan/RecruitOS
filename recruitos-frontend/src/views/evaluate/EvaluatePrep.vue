<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getInterviewPrep, getJudgment, type InterviewPrep } from '@/api/modules/brain'
import { RCard, RButton, RBadge, REmpty, RSkeleton } from '@/components/ui'
import { Sparkles, AlertTriangle, Lightbulb, Target, ChevronRight } from 'lucide-vue-next'

const route = useRoute()
const loading = ref(true)
const prep = ref<InterviewPrep | null>(null)
const candidateJudgment = ref<any>(null)
const questions = ref<string[]>([])

async function load() {
  loading.value = true
  try {
    const interviewId = Number(route.query.interviewId || route.params.interviewId || 1)
    const res = await getInterviewPrep(interviewId)
    prep.value = (res as any).data

    // Get AI candidate judgment
    if (prep.value) {
      try {
        const jRes = await getJudgment('CANDIDATE', 1).catch(() => null) // candidateId from route
        candidateJudgment.value = (jRes as any)?.data
      } catch {}
    }

    // Parse questions - pick the 3 most important ones
    if (prep.value?.suggestedQuestions) {
      questions.value = prep.value.suggestedQuestions.slice(0, 3)
    }
  } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <div class="p-6 max-w-2xl mx-auto space-y-5">
    <RSkeleton v-if="loading" class="h-64" />

    <template v-else-if="prep">
      <!-- Header -->
      <div class="mb-2">
        <div class="flex items-center gap-2 text-sm text-text-placeholder mb-1">
          <span>面试准备</span>
          <ChevronRight class="h-3 w-3" />
          <span class="text-text-primary font-medium">{{ prep.candidateName }}</span>
        </div>
        <div class="text-xs text-text-placeholder">
          {{ prep.jobTitle }} · 评估维度：{{ prep.evaluatorDimension }}
        </div>
      </div>

      <!-- AI一句话 -->
      <RCard v-if="candidateJudgment?.judgmentText" class="p-4 bg-blue-50 border-blue-200">
        <div class="flex items-start gap-2">
          <Sparkles class="h-4 w-4 text-blue-500 shrink-0 mt-0.5" />
          <p class="text-sm text-blue-800">{{ candidateJudgment.judgmentText }}</p>
        </div>
      </RCard>

      <!-- 3个最该问的问题 -->
      <RCard class="p-5 border-l-4 border-blue-300">
        <div class="flex items-center gap-2 mb-4">
          <Target class="h-5 w-5 text-blue-500" />
          <h2 class="text-base font-semibold text-text-primary">3 个最该问的问题</h2>
        </div>
        <div class="space-y-4">
          <div v-for="(q, i) in questions" :key="i" class="flex gap-3">
            <span class="flex-shrink-0 w-7 h-7 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-sm font-bold">
              {{ i + 1 }}
            </span>
            <div>
              <p class="text-sm font-medium text-text-primary">{{ q }}</p>
            </div>
          </div>
          <div v-if="!questions.length" class="text-sm text-text-placeholder">
            暂无AI建议的追问问题。请在面试中重点关注候选人的 {{ prep.evaluatorDimension }} 表现。
          </div>
        </div>
      </RCard>

      <!-- 关键信号 -->
      <RCard v-if="prep.resumeSignals?.length" class="p-4">
        <div class="flex items-center gap-2 mb-3">
          <Lightbulb class="h-4 w-4 text-amber-500" />
          <h3 class="text-sm font-semibold text-text-primary">值得注意的信号</h3>
        </div>
        <div class="space-y-2">
          <div v-for="sig in prep.resumeSignals" :key="sig.content" class="flex items-start gap-2">
            <RBadge :variant="sig.type === 'strong' ? 'default' : sig.type === 'concern' ? 'danger' : 'outline'" size="sm" class="shrink-0 mt-0.5">
              {{ sig.type === 'strong' ? '强' : sig.type === 'concern' ? '风险' : '中' }}
            </RBadge>
            <span class="text-sm text-text-secondary">{{ sig.content }}</span>
          </div>
        </div>
      </RCard>

      <!-- 注意 -->
      <RCard v-if="prep.cautions?.length" class="p-4 border-l-4 border-amber-200">
        <div class="flex items-center gap-2 mb-2">
          <AlertTriangle class="h-4 w-4 text-amber-500" />
          <h3 class="text-sm font-semibold text-text-primary">提醒</h3>
        </div>
        <ul class="space-y-1">
          <li v-for="c in prep.cautions" :key="c" class="text-sm text-text-secondary flex gap-2">
            <span class="text-amber-500">•</span> {{ c }}
          </li>
        </ul>
      </RCard>

      <!-- 评分锚点速查 -->
      <RCard v-if="prep.scoreAnchors?.length" class="p-4">
        <h3 class="text-sm font-semibold text-text-primary mb-3">评分标准速查</h3>
        <div class="grid grid-cols-5 gap-1 text-center">
          <div v-for="a in prep.scoreAnchors" :key="a.score" class="p-2 rounded bg-bg-muted">
            <div class="text-lg font-bold text-text-primary">{{ a.score }}</div>
            <div class="text-[10px] text-text-placeholder leading-tight">{{ a.description?.substring(0, 20) }}...</div>
          </div>
        </div>
      </RCard>

      <!-- Start interview -->
      <RButton class="w-full" size="lg" @click="$router.back()">
        开始面试
      </RButton>
    </template>

    <REmpty v-else description="暂无面试数据" />
  </div>
</template>
