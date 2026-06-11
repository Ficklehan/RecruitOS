<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { cn } from '@/lib/utils'
import { RPageShell, RCard, RButton, RBadge, RInput } from '@/components/ui'
import { getBrainDashboard, type BrainDashboard, type UrgentItem, type AIInsight, askAI } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import {
  Sparkles, AlertTriangle, TrendingUp, DollarSign, Scale, BarChart3,
  ArrowRight, Inbox, Clock, CheckCircle, Loader2,
} from 'lucide-vue-next'
import AiValueDashboard from '@/components/ai/AiValueDashboard.vue'

const router = useRouter()
const loading = ref(true)
const dashboard = ref<BrainDashboard | null>(null)
const askInput = ref('')
const askLoading = ref(false)

onMounted(async () => {
  try {
    const res = await getBrainDashboard()
    dashboard.value = res.data
  } catch {
    toast.error('加载 AI 助手数据失败')
  } finally {
    loading.value = false
  }
})

const healthColor = computed(() => {
  const s = dashboard.value?.summary.healthScore ?? 0
  if (s >= 80) return 'text-success'
  if (s >= 60) return 'text-warning'
  return 'text-danger'
})

const healthBg = computed(() => {
  const s = dashboard.value?.summary.healthScore ?? 0
  if (s >= 80) return 'bg-success-light'
  if (s >= 60) return 'bg-warning-light'
  return 'bg-danger-light'
})

const aiTools = [
  { label: '需求诊断', desc: 'AI 分析团队缺口，生成招聘方案', icon: Sparkles, path: '/ai/demand-create', color: 'bg-primary-light text-primary' },
  { label: '面试评分卡', desc: 'AI 辅助面试评价与打分', icon: BarChart3, path: '/ai/scorecard', color: 'bg-success-light text-success' },
  { label: '校准会', desc: '多面试官评价对比与偏差检测', icon: Scale, path: '/ai/calibration', color: 'bg-warning-light text-warning' },
  { label: '意向预测', desc: '预测候选人接受 Offer 的概率', icon: TrendingUp, path: '/ai/intent', color: 'bg-info-light text-info' },
  { label: '周期预测', desc: '预测招聘完成所需时间', icon: Clock, path: '/ai/cycle', color: 'bg-danger-light text-danger' },
  { label: 'Offer 策略', desc: 'AI 生成薪酬建议与谈判策略', icon: DollarSign, path: '/ai/offer-strategy', color: 'bg-primary-light text-primary' },
]

async function handleAsk() {
  if (!askInput.value.trim()) return
  askLoading.value = true
  try {
    const res = await askAI(askInput.value)
    toast.info(res.data?.answer || '暂无回答')
  } catch {
    toast.error('AI 问答失败')
  } finally {
    askLoading.value = false
  }
}

function severityIcon(s: string) {
  return s === 'critical' ? AlertTriangle : s === 'high' ? AlertTriangle : CheckCircle
}
</script>

<template>
  <RPageShell variant="plain">
    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <template v-else-if="dashboard">
      <!-- Health Score Hero -->
      <RCard variant="flat" padding="lg" class="mb-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div :class="cn('w-16 h-16 rounded-full flex items-center justify-center', healthBg)">
              <span :class="cn('text-2xl font-bold', healthColor)">{{ dashboard.summary.healthScore }}</span>
            </div>
            <div>
              <h2 class="text-[18px] font-semibold text-text-primary">招聘健康分</h2>
              <p class="text-[13px] text-text-secondary mt-0.5">实时评估招聘流程健康度</p>
            </div>
          </div>
          <RButton variant="ghost" size="sm" class="text-primary" @click="router.push('/ai/diagnose')">
            查看周报 <ArrowRight class="h-4 w-4 ml-1" />
          </RButton>
        </div>

        <!-- Summary stats -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
          <div class="p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <p class="text-[12px] text-text-secondary">活跃职位</p>
            <p class="text-[20px] font-bold text-text-primary mt-1">{{ dashboard.summary.activeJobs }}</p>
          </div>
          <div class="p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <p class="text-[12px] text-text-secondary">待决策</p>
            <p class="text-[20px] font-bold text-text-primary mt-1">{{ dashboard.summary.pendingDecisions }}</p>
          </div>
          <div class="p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <p class="text-[12px] text-text-secondary">今日面试</p>
            <p class="text-[20px] font-bold text-text-primary mt-1">{{ dashboard.summary.todayInterviews }}</p>
          </div>
          <div class="p-3 rounded-[var(--r-radius)] bg-bg-muted">
            <p class="text-[12px] text-text-secondary">Offer 接受率</p>
            <p class="text-[20px] font-bold text-text-primary mt-1">{{ dashboard.summary.offerAcceptRate }}%</p>
          </div>
        </div>
      </RCard>

      <!-- Urgent Items -->
      <RCard v-if="dashboard.urgentItems?.length" padding="md" class="mb-6">
        <h3 class="text-[15px] font-semibold text-text-primary mb-3">紧急事项</h3>
        <div class="space-y-2">
          <div
            v-for="item in dashboard.urgentItems"
            :key="item.id"
            class="flex items-start gap-3 p-3 rounded-[var(--r-radius)] hover:bg-bg-hover transition-colors cursor-pointer"
            @click="item.actionPath && router.push(item.actionPath)"
          >
            <div :class="cn('w-8 h-8 rounded-full flex items-center justify-center shrink-0', item.severity === 'critical' ? 'bg-danger-light' : 'bg-warning-light')">
              <component :is="severityIcon(item.severity)" :class="cn('h-4 w-4', item.severity === 'critical' ? 'text-danger' : 'text-warning')" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-[14px] font-medium text-text-primary">{{ item.title }}</p>
              <p class="text-[12px] text-text-secondary mt-0.5 line-clamp-2">{{ item.description }}</p>
            </div>
            <ArrowRight class="h-4 w-4 text-text-placeholder shrink-0 mt-1" />
          </div>
        </div>
      </RCard>

      <!-- AI Co-Pilot Banner -->
      <RCard
        variant="flat"
        padding="lg"
        class="mb-6 !bg-gradient-to-r !from-primary-light/30 !to-info-light/20 !border-primary/10 cursor-pointer"
        @click="router.push('/ai/copilot')"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
              <Sparkles class="h-6 w-6 text-primary" />
            </div>
            <div>
              <h3 class="text-[16px] font-semibold text-text-primary">AI Co-Pilot</h3>
              <p class="text-[13px] text-text-secondary mt-0.5">世界级 PM + 招聘专家，随时为你分析需求、评估候选人、制定策略</p>
            </div>
          </div>
          <div class="flex items-center gap-1 text-primary">
            <span class="text-[13px] font-medium">开始对话</span>
            <ArrowRight class="h-4 w-4" />
          </div>
        </div>
      </RCard>

      <!-- AI Tools Grid -->
      <div class="mb-6">
        <h3 class="text-[15px] font-semibold text-text-primary mb-3">AI 工具</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
          <RCard
            v-for="tool in aiTools"
            :key="tool.path"
            variant="interactive"
            padding="md"
            class="cursor-pointer"
            @click="router.push(tool.path)"
          >
            <div class="flex items-start gap-3">
              <div :class="cn('w-10 h-10 rounded-[var(--r-radius)] flex items-center justify-center shrink-0', tool.color)">
                <component :is="tool.icon" class="h-5 w-5" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-[14px] font-medium text-text-primary">{{ tool.label }}</p>
                <p class="text-[12px] text-text-secondary mt-0.5">{{ tool.desc }}</p>
              </div>
            </div>
          </RCard>
        </div>
      </div>

      <!-- AI Insights -->
      <RCard v-if="dashboard.insights?.length" padding="md">
        <h3 class="text-[15px] font-semibold text-text-primary mb-3">AI 洞察</h3>
        <div class="space-y-2">
          <div
            v-for="insight in dashboard.insights"
            :key="insight.id"
            class="flex items-start gap-3 p-3 rounded-[var(--r-radius)] bg-bg-muted/50 hover:bg-bg-hover transition-colors"
          >
            <Sparkles class="h-4 w-4 text-primary mt-0.5 shrink-0" />
            <div class="flex-1 min-w-0">
              <p class="text-[14px] font-medium text-text-primary">{{ insight.title }}</p>
              <p class="text-[12px] text-text-secondary mt-0.5">{{ insight.description }}</p>
            </div>
            <RBadge v-if="insight.confidence >= 0.8" variant="success" size="sm">高置信</RBadge>
          </div>
        </div>
      </RCard>

      <!-- AI 价值看板 -->
      <div class="mt-6">
        <AiValueDashboard />
      </div>
    </template>
  </RPageShell>
</template>
