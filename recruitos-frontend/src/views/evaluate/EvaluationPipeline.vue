<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getInterviewPrep, getJudgment, type InterviewPrep } from '@/api/modules/brain'
import { RCard, RButton, RBadge, RProgress, REmpty, RSkeleton, RTextarea, RSelect } from '@/components/ui'
import { PenTool, Check, AlertTriangle, Lightbulb, Sparkles, ChevronRight } from 'lucide-vue-next'

const route = useRoute()
const loading = ref(true)
const prep = ref<InterviewPrep | null>(null)
const judgment = ref<any>(null)

// 5 dimensions with extracted evidence (simulated for now)
const dimensions = ref([
  { name: 'Product Sense', weight: 25, evidence: ['候选人在描述XX项目时展现了从用户问题出发的思考方式', '他提到了A/B测试验证假设'], score: 0, confidence: 'HIGH' },
  { name: 'Execution', weight: 25, evidence: ['主导过跨部门项目从0到1落地', '用数据驱动决策的案例'], score: 0, confidence: 'MODERATE' },
  { name: 'Leadership', weight: 20, evidence: ['提到培养了2个初级PM独立负责模块'], score: 0, confidence: 'MODERATE' },
  { name: 'Culture', weight: 15, evidence: ['主动提到从失败中学到的经验', '价值观表述与公司一致'], score: 0, confidence: 'HIGH' },
  { name: 'Craft', weight: 15, evidence: ['有可复用的产品方法论框架'], score: 0, confidence: 'LOW' },
])

const overallScore = ref(0)
const decision = ref('')
const decisionOptions = [
  { label: 'Strong Hire', value: 'STRONG_HIRE' },
  { label: 'Hire', value: 'HIRE' },
  { label: 'Leaning Hire', value: 'LEANING_HIRE' },
  { label: 'Leaning No', value: 'LEANING_NO' },
  { label: 'No Hire', value: 'NO_HIRE' },
]

const scoreAnchors: Record<number, string> = {
  1: '无法识别问题，方案与需求脱节',
  2: '能识别问题但方案浅层',
  3: '定义清晰问题，方案合理',
  4: '洞察深层需求，方案有创新',
  5: '重新定义问题，方案优雅',
}

onMounted(async () => {
  await new Promise(r => setTimeout(r, 600))
  loading.value = false
})
</script>

<template>
  <div class="p-6 max-w-2xl mx-auto space-y-5">
    <RSkeleton v-if="loading" class="h-64" />

    <template v-else>
      <div class="flex items-center gap-3 mb-4">
        <PenTool class="h-5 w-5 text-blue-500" />
        <div>
          <h1 class="text-base font-semibold text-text-primary">面试评估</h1>
          <p class="text-xs text-text-placeholder">AI 从面试转录中提取证据 → 你验证评分</p>
        </div>
      </div>

      <!-- AI一句话 -->
      <RCard class="p-4 bg-blue-50 border-blue-200">
        <div class="flex items-start gap-2">
          <Sparkles class="h-4 w-4 text-blue-500 shrink-0 mt-0.5" />
          <div>
            <p class="text-sm text-blue-800 font-medium">AI 初评摘要</p>
            <p class="text-sm text-blue-700 mt-1">基于面试转录，Product Sense 信号强（2个具体案例），Execution 信号中等（1个案例但个人角色不够清晰），Craft 信号偏弱。建议重点关注 Execution 的个人贡献细节。</p>
          </div>
        </div>
      </RCard>

      <!-- Dimensions -->
      <RCard v-for="(dim, di) in dimensions" :key="dim.name" class="p-5"
        :class="dim.score === 0 ? 'border-gray-200' : dim.score >= 4 ? 'border-green-200' : dim.score <= 2 ? 'border-red-200' : 'border-amber-200'">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h3 class="text-sm font-semibold text-text-primary">{{ dim.name }}</h3>
            <p class="text-xs text-text-placeholder">权重 {{ dim.weight }}% · 证据置信度 {{ dim.confidence }}</p>
          </div>
          <RBadge v-if="!dim.score" variant="outline" size="sm">待评分</RBadge>
          <RBadge v-else-if="dim.score >= 4" variant="default" size="sm">{{ dim.score }}分</RBadge>
          <RBadge v-else :variant="dim.score <= 2 ? 'danger' : 'warning'" size="sm">{{ dim.score }}分</RBadge>
        </div>

        <!-- Evidence (AI extracted) -->
        <div class="space-y-2 mb-3">
          <div v-for="(ev, ei) in dim.evidence" :key="ei"
            class="flex items-start gap-2 p-2 bg-bg-muted rounded text-sm">
            <Lightbulb class="h-3.5 w-3.5 text-amber-400 shrink-0 mt-0.5" />
            <span class="text-text-secondary">{{ ev }}</span>
          </div>
        </div>

        <!-- Score -->
        <div class="flex items-center gap-2">
          <span class="text-xs text-text-placeholder w-16">你的评分</span>
          <div class="flex gap-1">
            <button v-for="s in 5" :key="s"
              class="w-8 h-8 rounded text-sm font-medium border transition-colors"
              :class="dim.score === s
                ? 'bg-blue-600 text-white border-blue-600'
                : 'border-gray-200 text-gray-600 hover:border-blue-300'"
              @click="dim.score = s">
              {{ s }}
            </button>
          </div>
          <span v-if="dim.score" class="text-xs text-text-placeholder ml-2">
            {{ scoreAnchors[dim.score] || '' }}
          </span>
        </div>
      </RCard>

      <!-- Decision -->
      <RCard class="p-5">
        <h3 class="text-sm font-semibold text-text-primary mb-3">综合决策</h3>
        <div class="flex gap-2 flex-wrap mb-4">
          <RButton v-for="opt in decisionOptions" :key="opt.value" size="sm"
            :variant="decision === opt.value ? 'default' : 'outline'"
            @click="decision = opt.value">
            {{ opt.label }}
          </RButton>
        </div>
        <RTextarea placeholder="补充评价（可选）..." :rows="2" class="text-sm" />
        <RButton class="mt-3 w-full" :disabled="!decision">
          <Check class="h-4 w-4 mr-1" /> 提交评估
        </RButton>
      </RCard>
    </template>
  </div>
</template>
