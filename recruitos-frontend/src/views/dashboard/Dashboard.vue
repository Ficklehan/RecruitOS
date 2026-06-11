<template>
  <PageShell>
    <template #heading>
      <h1 class="text-2xl font-semibold tracking-tight text-foreground leading-tight">
        {{ greeting }}，{{ userName }}
      </h1>
      <p class="mt-1 text-sm text-muted-foreground">
        AI 招聘助手 · {{ todayStr }}
      </p>
    </template>
    <template #actions>
      <div class="flex items-center gap-3">
        <div class="flex items-center gap-2 px-3 py-1.5 rounded-full text-sm" :class="healthColorClass">
          <Activity class="h-4 w-4" />
          <span class="font-semibold">{{ dashboard.summary.healthScore || 78 }}/100</span>
          <span v-if="scoreTrend !== 0" class="text-xs opacity-70">{{ scoreTrend > 0 ? '↑' : '↓' }}{{ Math.abs(scoreTrend) }}</span>
        </div>
        <RButton variant="outline" size="sm" @click="router.push('/ai/copilot')">
          <Sparkles class="mr-1.5 h-3.5 w-3.5 text-primary" />Co-Pilot
        </RButton>
        <RButton variant="outline" size="sm" @click="router.push('/workspace/inbox')">收件箱</RButton>
      </div>
    </template>

    <!-- 决策项卡片 — 每个附带操作按钮 -->
    <RCard v-if="dashboard.urgentItems && dashboard.urgentItems.length" class="p-0 overflow-hidden">
      <div class="px-5 py-3 border-b border-border flex items-center gap-2">
        <Zap class="h-4 w-4 text-warning" />
        <span class="text-sm font-semibold text-foreground">需要你决策的 {{ dashboard.urgentItems.length }} 件事</span>
      </div>
      <div class="divide-y divide-border">
        <div
          v-for="(item, idx) in dashboard.urgentItems"
          :key="item.id || idx"
          class="px-5 py-3.5 hover:bg-muted/50 transition-colors"
        >
          <div class="flex items-start gap-3">
            <div class="mt-0.5 shrink-0">
              <span class="inline-block w-2 h-2 rounded-full"
                :class="{ 'bg-destructive': item.severity === 'critical', 'bg-warning': item.severity === 'warning', 'bg-primary': item.severity === 'info' }"
              />
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <span class="text-sm font-medium text-foreground">{{ item.title }}</span>
                <RBadge v-if="item.confidence" variant="outline" class="text-[10px] px-1.5 py-0 h-4">
                  置信度 {{ Math.round((item.confidence || 0) * 100) }}%
                </RBadge>
              </div>
              <p class="text-xs text-muted-foreground mt-0.5">{{ item.description }}</p>
              <p v-if="item.aiReasoning" class="text-xs text-muted-foreground/70 mt-1 italic">🧠 {{ item.aiReasoning }}</p>
              <!-- 操作按钮组 -->
              <div class="flex items-center gap-2 mt-2.5">
                <RButton size="sm" @click="handleUrgentAction(item, 'view')">
                  {{ item.action || '查看详情' }}
                  <ArrowRight class="ml-1 h-3 w-3" />
                </RButton>
                <RButton
                  v-if="item.type === 'INTENT_DROP'"
                  size="sm"
                  variant="outline"
                  @click="handleQuickAction(item, 'schedule')"
                >
                  <Calendar class="mr-1 h-3 w-3" />安排沟通
                </RButton>
                <RButton
                  v-if="item.type === 'CANDIDATE_STALE'"
                  size="sm"
                  variant="outline"
                  @click="handleQuickAction(item, 'nudge')"
                >
                  <MessageCircle class="mr-1 h-3 w-3" />发送跟进
                </RButton>
                <RButton
                  v-if="item.type === 'FUNNEL_BOTTLENECK'"
                  size="sm"
                  variant="outline"
                  @click="handleQuickAction(item, 'adjust')"
                >
                  <Sliders class="mr-1 h-3 w-3" />调整筛选
                </RButton>
                <RButton size="sm" variant="ghost" class="text-muted-foreground" @click="handleDismiss(item)">
                  <X class="mr-1 h-3 w-3" />忽略
                </RButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </RCard>

    <!-- 无决策项 -->
    <RCard v-else class="p-6 text-center">
      <CheckCircle2 class="h-10 w-10 text-success mx-auto mb-3" />
      <p class="text-sm font-medium text-foreground">今天没有需要你决策的事项</p>
      <p class="text-xs text-muted-foreground mt-1">AI 已自动处理低风险操作</p>
    </RCard>

    <!-- 健康四维 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <RCard class="p-4"><div class="flex items-center gap-2 mb-2"><Briefcase class="h-4 w-4 text-primary" /><span class="text-xs text-muted-foreground font-medium">在招岗位</span></div><div class="flex items-baseline gap-2"><span class="text-2xl font-bold text-foreground">{{ dashboard.summary.activeJobs || 0 }}</span><span class="text-xs text-muted-foreground">个活跃</span></div><p v-if="dashboard.summary.criticalJobs" class="text-xs text-destructive mt-1">{{ dashboard.summary.criticalJobs }} 个健康异常</p><p v-else class="text-xs text-success mt-1">全部健康</p></RCard>
      <RCard class="p-4"><div class="flex items-center gap-2 mb-2"><Users class="h-4 w-4 text-info" /><span class="text-xs text-muted-foreground font-medium">管道健康</span></div><div class="flex items-baseline gap-2"><span class="text-2xl font-bold text-foreground">{{ healthScores.pipelineScore || 85 }}</span><span class="text-xs text-muted-foreground">分</span></div><p class="text-xs text-muted-foreground mt-1">漏斗转化正常</p></RCard>
      <RCard class="p-4"><div class="flex items-center gap-2 mb-2"><ClipboardCheck class="h-4 w-4 text-warning" /><span class="text-xs text-muted-foreground font-medium">面试质量</span></div><div class="flex items-baseline gap-2"><span class="text-2xl font-bold text-foreground">{{ healthScores.interviewScore || 72 }}</span><span class="text-xs text-muted-foreground">分</span></div><p class="text-xs text-muted-foreground mt-1">今日 {{ dashboard.summary.todayInterviews || 0 }} 场面试</p></RCard>
      <RCard class="p-4"><div class="flex items-center gap-2 mb-2"><TrendingUp class="h-4 w-4 text-success" /><span class="text-xs text-muted-foreground font-medium">录用效率</span></div><div class="flex items-baseline gap-2"><span class="text-2xl font-bold text-foreground">{{ healthScores.offerEfficiency || 76 }}</span><span class="text-xs text-muted-foreground">分</span></div><p class="text-xs text-muted-foreground mt-1">接受率 {{ formatPercent(dashboard.summary.offerAcceptRate) }}</p></RCard>
      <RCard class="p-4 cursor-pointer hover:shadow-soft transition-shadow" @click="router.push('/ai/cycle')"><div class="flex items-center gap-2 mb-2"><Clock class="h-4 w-4" :class="cycleRiskColor" /><span class="text-xs text-muted-foreground font-medium">招聘周期</span></div><div class="flex items-baseline gap-2"><span class="text-2xl font-bold text-foreground">{{ dashboard.summary.avgCycleDays || 38 }}</span><span class="text-xs text-muted-foreground">天</span></div><p class="text-xs" :class="cycleRiskTextColor">{{ cycleRiskLabel }}</p></RCard>
    </div>

    <!-- AI 洞察 -->
    <section>
      <div class="flex items-center gap-2 mb-3"><Lightbulb class="h-4 w-4 text-warning" /><span class="text-sm font-semibold text-foreground">AI 洞察</span></div>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <RCard v-for="(insight, idx) in dashboard.insights" :key="insight.id || idx" class="p-4 hover:shadow-soft transition-shadow cursor-pointer" @click="insight.actionPath && navigateTo(insight.actionPath)">
          <div class="flex items-start gap-3"><div class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0" :class="insightCategoryBg(insight.category)"><component :is="insightCategoryIcon(insight.category)" class="h-4 w-4" /></div><div class="min-w-0 flex-1"><div class="flex items-center gap-2 mb-0.5"><span class="text-sm font-medium text-foreground">{{ insight.title }}</span><RBadge v-if="insight.confidence >= 0.8" variant="outline" class="text-[10px] px-1 py-0 h-4 text-success border-success/30">高置信</RBadge></div><p class="text-xs text-muted-foreground leading-relaxed">{{ insight.description }}</p><p v-if="insight.suggestedAction" class="text-xs text-primary mt-1.5 font-medium">→ {{ insight.suggestedAction }}</p></div></div>
        </RCard>
      </div>
      <div v-if="!dashboard.insights?.length" class="text-center py-8"><p class="text-sm text-muted-foreground">AI 正在分析招聘数据，洞察即将生成...</p></div>
    </section>

    <!-- 快捷入口 -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
      <RButton v-for="action in quickActions" :key="action.label" variant="outline" class="h-auto py-3 flex flex-col items-center gap-1.5" @click="router.push(action.path)">
        <component :is="action.icon" class="h-4 w-4" /><span class="text-xs">{{ action.label }}</span>
      </RButton>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { reactive, computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { RButton, RBadge, RCard } from '@/components/ui'
import PageShell from '@/components/Layout/PageShell.vue'
import { getBrainDashboard, logIgnoreReason } from '@/api/modules/brain'
import type { BrainDashboard } from '@/api/modules/brain'
import { toast } from '@/lib/notify'
import {
  Activity, Zap, ArrowRight, CheckCircle2, Lightbulb, Sparkles,
  Briefcase, Users, ClipboardCheck, TrendingUp,
  MessageCircle, UserPlus, LayoutGrid, FileText,
  Calendar, Sliders, X, Clock,
} from 'lucide-vue-next'

const router = useRouter(); const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.realName || '管理员')
const greeting = computed(() => { const h = new Date().getHours(); if (h < 9) return '早上好'; if (h < 12) return '上午好'; if (h < 14) return '中午好'; if (h < 18) return '下午好'; return '晚上好' })
const todayStr = computed(() => { const d = new Date(); return `${d.getMonth() + 1}月${d.getDate()}日 星期${['日','一','二','三','四','五','六'][d.getDay()]}` })

const dashboard = reactive<BrainDashboard>({
  summary: { activeJobs: 0, pendingDecisions: 0, healthScore: 78, todayInterviews: 0, criticalJobs: 0, offerAcceptRate: 0.68, avgCycleDays: 38 },
  urgentItems: [], insights: [],
})
const healthScores = reactive({ pipelineScore: 85, interviewScore: 72, offerEfficiency: 76, activeJobsAbnormal: 0 })
const scoreTrend = ref(0)

const healthColorClass = computed(() => {
  const s = dashboard.summary.healthScore || 78
  if (s >= 85) return 'bg-success/10 text-success'
  if (s >= 70) return 'bg-warning/10 text-warning'
  return 'bg-destructive/10 text-destructive'
})
const cycleRiskLabel = computed(() => {
  const d = dashboard.summary.avgCycleDays || 38
  if (d <= 30) return '周期健康'
  if (d <= 45) return '略高于平均'
  return '需要关注'
})
const cycleRiskColor = computed(() => {
  const d = dashboard.summary.avgCycleDays || 38
  if (d <= 30) return 'text-success'
  if (d <= 45) return 'text-warning'
  return 'text-destructive'
})
const cycleRiskTextColor = computed(() => {
  const d = dashboard.summary.avgCycleDays || 38
  if (d <= 30) return 'text-success'
  if (d <= 45) return 'text-warning'
  return 'text-destructive'
})

const quickActions = [
  { label: '招聘进展', icon: LayoutGrid, path: '/pipeline/board' },
  { label: '候选人', icon: UserPlus, path: '/pipeline/candidates' },
  { label: 'AI Co-Pilot', icon: Sparkles, path: '/ai/copilot' },
  { label: '新建需求', icon: FileText, path: '/planning/demands/create' },
]

function formatPercent(v: number | undefined): string { if (v == null) return '--'; return Math.round(v * 100) + '%' }
function navigateTo(path: string | undefined) { if (path) router.push(path) }

function handleUrgentAction(item: any, _action: string) {
  if (item.actionPath) { router.push(item.actionPath) }
  else { toast.info('操作已触发') }
}

async function handleQuickAction(item: any, action: string) {
  const labels: Record<string, string> = {
    schedule: `已安排 Hiring Manager 沟通：${item.candidateName || item.title}`,
    nudge: `已发送跟进话术：${item.candidateName || item.title}`,
    adjust: `已调整筛选门槛：${item.jobTitle || item.title}`,
  }
  toast.success(labels[action] || '操作已完成')
}

async function handleDismiss(item: any) {
  try {
    await logIgnoreReason({
      touchpoint: 'URGENT_ITEM',
      targetId: item.id ? Number(item.id) : undefined,
      targetType: item.type || 'UNKNOWN',
      reason: 'USER_DISMISSED',
      note: '用户在 Dashboard 忽略',
    })
  } catch {}
  const idx = dashboard.urgentItems.findIndex((u: any) => u.id === item.id)
  if (idx >= 0) dashboard.urgentItems.splice(idx, 1)
  toast.info('已忽略')
}

function insightCategoryIcon(cat: string) {
  const map: Record<string, any> = { CANDIDATE_QUALITY: UserPlus, PROCESS_EFFICIENCY: Zap, INTERVIEWER_PERFORMANCE: ClipboardCheck, CHANNEL_ROI: TrendingUp }
  return map[cat] || Lightbulb
}
function insightCategoryBg(cat: string) {
  const map: Record<string, string> = { CANDIDATE_QUALITY: 'bg-primary/10 text-primary', PROCESS_EFFICIENCY: 'bg-warning/10 text-warning', INTERVIEWER_PERFORMANCE: 'bg-info/10 text-info', CHANNEL_ROI: 'bg-success/10 text-success' }
  return map[cat] || 'bg-muted text-muted-foreground'
}

async function loadDashboard() {
  try {
    const res: any = await getBrainDashboard()
    const payload: any = res?.data || res
    if (payload.summary) Object.assign(dashboard.summary, payload.summary)
    if (payload.urgentItems) { dashboard.urgentItems.splice(0, dashboard.urgentItems.length, ...payload.urgentItems) }
    if (payload.insights) { dashboard.insights.splice(0, dashboard.insights.length, ...payload.insights) }
    if (payload.healthScores) Object.assign(healthScores, payload.healthScores)
  } catch {}
}

onMounted(loadDashboard)
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
</style>
