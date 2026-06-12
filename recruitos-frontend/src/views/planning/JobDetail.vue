<template>
  <PageShell>
<div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <div class="ws-breadcrumb">
      <RButton variant="ghost" @click="$router.push('/planning/jobs')">
        <ArrowLeft class="mr-2 h-4 w-4" />返回在招职位
      </RButton>
    </div>

    <ObjectHeader
      v-if="job"
      :meta="`编号 ${job.jobNo}${job.demandNo ? ` · 关联需求 ${job.demandNo}` : ''} · 还差 ${Math.max(0, headCount - filledCount)} 人（${filledCount}/${headCount}）`"
    >
      <template #title>
        {{ job.title }}
        <RBadge :variant="elTagTypeToBadge(statusTagType(job.status))" class="ml-2">
          {{ jobStatusLabel(job.status) }}
        </RBadge>
      </template>
      <template #actions>
        <RButton v-if="job.status === 'DRAFT'" class="bg-green-600 hover:bg-green-700" @click="handleActivate">开始招聘</RButton>
        <RButton v-if="job.status === 'ACTIVE'" variant="outline" @click="handlePause">暂停招聘</RButton>
        <RButton v-if="job.status !== 'CLOSED'" variant="destructive" @click="openCloseDialog">关闭职位</RButton>
      </template>
    </ObjectHeader>

    <div v-if="job && nextStep" class="next-step-bar rounded-xl bg-card text-card-foreground shadow-soft">
      <div class="next-step-text">
        <span class="next-label">下一步</span>
        <span>{{ nextStep.text }}</span>
      </div>
      <RButton @click="nextStep.action">{{ ACTIONS.goProcess }}</RButton>
    </div>

    <JobContextBar v-if="job" :model-value="job.id" @update:model-value="switchJob">
      <RButton variant="link" @click="goInbox">打开收件箱</RButton>
    </JobContextBar>

    <template v-if="job">
      <RTabs v-model="activeTab" class="workspace-tabs mt-4">
        <RTabsList>
          <RTabsTrigger value="overview">{{ JOB_WORKSPACE_TABS.overview }}</RTabsTrigger>
          <RTabsTrigger value="sourcing">{{ JOB_WORKSPACE_TABS.sourcing }}</RTabsTrigger>
          <RTabsTrigger value="candidates">{{ JOB_WORKSPACE_TABS.candidates }}</RTabsTrigger>
          <RTabsTrigger value="rules">{{ JOB_WORKSPACE_TABS.rules }}</RTabsTrigger>
          <RTabsTrigger value="ai">{{ JOB_WORKSPACE_TABS.ai }}</RTabsTrigger>
        </RTabsList>

        <RTabsContent value="overview">
          <div class="overview-grid">
            <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6 funnel-card">
              <h4>管人 · 在招候选人</h4>
              <div class="funnel-row">
                <div v-for="s in stageSummary" :key="s.stage" class="funnel-cell">
                  <span class="funnel-num">{{ s.count }}</span>
                  <span class="funnel-label">{{ s.label }}</span>
                </div>
              </div>
              <RButton variant="link" @click="activeTab = 'candidates'">查看{{ OBJECTS.activeCandidates }}</RButton>
            </div>

            <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6 funnel-card">
              <h4>找人 · 平台与待联系池</h4>
              <div v-if="campaignStats" class="funnel-row sourcing-stats">
                <div class="funnel-cell"><span class="funnel-num">{{ campaignStats.published || 0 }}</span><span class="funnel-label">已发布</span></div>
                <div class="funnel-cell"><span class="funnel-num">{{ campaignStats.searched || 0 }}</span><span class="funnel-label">已检索</span></div>
                <div class="funnel-cell"><span class="funnel-num">{{ campaignStats.greeted || 0 }}</span><span class="funnel-label">已联系</span></div>
                <div class="funnel-cell"><span class="funnel-num">{{ campaignStats.imported || 0 }}</span><span class="funnel-label">已纳入</span></div>
              </div>
              <p v-else class="hint">在「找人」中开启平台招人任务，或将人选纳入待联系池。</p>
              <RButton variant="link" @click="activeTab = 'sourcing'">{{ campaignStats ? '查看平台招人任务' : ACTIONS.startPlatformTask }}</RButton>
            </div>

            <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6">
              <h4>{{ OBJECTS.jobDescription }}</h4>
              <p class="jd-snippet">{{ displayJdText }}</p>
            </div>

            <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6">
              <h4>匹配建议分布</h4>
              <p class="hint">基于本职位已关联候选人的最新评估</p>
              <div v-for="row in matchDistribution" :key="row.label" class="dist-row">
                <span>{{ row.label }}</span>
                <RBadge :variant="elTagTypeToBadge(row.type)">{{ row.count }}</RBadge>
              </div>
              <EmptyStateCta v-if="!matchDistribution.length" :image-size="48" description="本职位尚无在招候选人" :actions="[{ label: ACTIONS.startPlatformTask, type: 'primary', onClick: () => activeTab = 'sourcing' }]" />
            </div>
          </div>
        </RTabsContent>

        <RTabsContent value="sourcing">
          <SegmentedControl v-model="sourcingSub" :options="sourcingSubOptions" class="sub-seg" />
          <JobSourcing v-if="sourcingSub === 'task'" :job-id="job.id" :job-title="job.title" :job-status="job.status" @imported="onCandidateImported" />
          <ChannelStaging v-else embedded :default-job-id="job.id" />
        </RTabsContent>

        <RTabsContent value="candidates">
          <SegmentedControl v-model="candidatesSub" :options="candidatesSubOptions" class="sub-seg" />
          <PipelineKanban v-if="candidatesSub === 'kanban'" ref="kanbanRef" :job-id="job.id" @loaded="onKanbanLoaded" />
          <JobHiringSummary v-else :job-id="job.id" @go-kanban="candidatesSub = 'kanban'" />
        </RTabsContent>

        <RTabsContent value="rules">
          <SegmentedControl v-model="rulesSub" :options="rulesSubOptions" class="sub-seg" />
          <div v-if="rulesSub === 'requirements'" class="rounded-xl bg-card text-card-foreground shadow-soft p-6 content-card">
            <div class="section-head">
              <h4>{{ OBJECTS.jobRequirements }}清单</h4>
              <RButton variant="link" @click="goEditRequirements">编辑</RButton>
            </div>
            <p class="hint">必备与加分项将用于候选人匹配评估。</p>
            <RTable v-if="requirements.length">
              <RTableHead>
                <RTableRow>
                  <RTableTh class="min-w-[140px]">要求项</RTableTh>
                  <RTableTh class="w-[90px]">类型</RTableTh>
                  <RTableTh class="w-[90px]">重要程度</RTableTh>
                </RTableRow>
              </RTableHead>
              <RTableBody>
                <RTableRow v-for="row in requirements" :key="row.name">
                  <RTableCell>{{ row.name }}</RTableCell>
                  <RTableCell>
                    <RBadge :variant="row.requirementType === 'REQUIRED' ? 'destructive' : 'secondary'">
                      {{ REQUIREMENT_TYPE_LABEL[row.requirementType] }}
                    </RBadge>
                  </RTableCell>
                  <RTableCell>{{ IMPORTANCE_LABEL[row.importance] }}</RTableCell>
                </RTableRow>
              </RTableBody>
            </RTable>
            <EmptyStateCta v-else title="尚未设置任职要求" description="粘贴职位描述后可自动提取，也可手动添加" :actions="[{ label: ACTIONS.editRequirements, type: 'primary', onClick: goEditRequirements }]" />
          </div>
          <div v-else-if="rulesSub === 'method'" class="content-card"><SourcingMethodWizard :job-id="job.id" @confirmed="onSourcingMethodConfirmed" /></div>
          <div v-else class="content-card"><JobAuditTimeline :job-id="job.id" /></div>
        </RTabsContent>
        <RTabsContent value="ai">
          <div class="ai-insight-grid">
            <div v-if="aiJudgment" class="rounded-xl bg-card text-card-foreground shadow-soft p-6 border-l-4" :class="aiHealthBorder">
              <div class="flex items-center gap-2 mb-3">
                <Sparkles class="h-4 w-4 text-primary" />
                <h4 class="text-sm font-semibold">AI 综合判断</h4>
                <RBadge v-if="aiJudgment.confidence > 0.7" variant="default" size="sm">置信度 {{ (aiJudgment.confidence * 100).toFixed(0) }}%</RBadge>
              </div>
              <p class="text-sm text-text-secondary mb-3">{{ aiJudgment.judgmentText }}</p>
              <div v-if="aiContradiction" class="p-3 bg-amber-50 rounded-lg text-xs text-amber-700">
                ⚠️ AI自我质疑：{{ aiContradiction.alternative_explanation }}
              </div>
            </div>
            <div v-if="aiObservations.length" class="rounded-xl bg-card text-card-foreground shadow-soft p-6">
              <div class="flex items-center gap-2 mb-3">
                <Eye class="h-4 w-4 text-amber-500" />
                <h4 class="text-sm font-semibold">AI 关注事项</h4>
                <RBadge size="sm">{{ aiObservations.length }}</RBadge>
              </div>
              <div class="space-y-2">
                <div v-for="(obs, i) in aiObservations" :key="i" class="flex items-start gap-2 p-2 rounded-lg" :class="obs.severity === 'CRITICAL' ? 'bg-red-50' : obs.severity === 'WARNING' ? 'bg-amber-50' : 'bg-blue-50'">
                  <AlertTriangle v-if="obs.severity === 'CRITICAL'" class="h-4 w-4 text-red-500 shrink-0 mt-0.5" />
                  <div><p class="text-sm font-medium">{{ obs.title }}</p><p class="text-xs text-text-secondary">{{ obs.body }}</p></div>
                </div>
              </div>
            </div>
            <div v-if="!aiJudgment && !aiObservations.length" class="rounded-xl bg-card text-card-foreground shadow-soft p-6 text-center">
              <p class="text-sm text-text-placeholder">AI 正在分析本岗位数据，积累足够信号后将自动生成洞察。</p>
            </div>
          </div>
        </RTabsContent>

      </RTabs>
    </template>

    <RDialog :model-value="closeDialogVisible" title="关闭在招职位" @update:model-value="closeDialogVisible = $event">
      <FormField label="关闭原因">
        <RTextarea v-model="closeReason" :rows="3" placeholder="请输入关闭原因" />
      </FormField>
      <template #footer>
        <RButton variant="outline" @click="closeDialogVisible = false">取消</RButton>
        <RButton @click="confirmClose">确定关闭</RButton>
      </template>
    </RDialog>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import ObjectHeader from '@/components/Layout/ObjectHeader.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2, Sparkles, Eye, AlertTriangle } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import FormField from '@/components/app/FormField.vue'
import SegmentedControl from '@/components/app/SegmentedControl.vue'
import JobContextBar from '@/components/common/JobContextBar.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import PipelineKanban from '@/components/pipeline/PipelineKanban.vue'
import ChannelStaging from '@/views/channel/ChannelStaging.vue'
import JobSourcing from './JobSourcing.vue'
import JobHiringSummary from './JobHiringSummary.vue'
import SourcingMethodWizard from '@/components/job/SourcingMethodWizard.vue'
import JobAuditTimeline from '@/components/job/JobAuditTimeline.vue'
import {
  RButton, RBadge, RTextarea,
  RTabs, RTabsList, RTabsTrigger, RTabsContent,
  RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell,
  RDialog,
} from '@/components/ui'
import { ACTIONS, JOB_WORKSPACE_TABS, OBJECTS, jobStatusLabel, pipelineStageLabel } from '@/constants/businessLabels'
import { type RequirementItem, fromApiTag, IMPORTANCE_LABEL, REQUIREMENT_TYPE_LABEL } from '@/utils/jdRequirements'
import { parseMatchDetail, tierTagType } from '@/utils/matchVerdict'
import { activateJob, closeJob, getJobDetail, getTags, pauseJob } from '@/api/modules/job'
import { getJudgment, getObservations } from '@/api/modules/brain'
import { getPipelineBoard } from '@/api/modules/pipeline'
import { getWorkflowDetail, getWorkflowList } from '@/api/modules/agent'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const job = ref<any>(null)
const activeTab = ref('overview')
const sourcingSub = ref('task')
const candidatesSub = ref('kanban')
const rulesSub = ref('requirements')
const boardColumns = ref<any[]>([])
const boardItems = ref<any[]>([])
const sourcedCount = ref(0)
const requirements = ref<RequirementItem[]>([])
const closeDialogVisible = ref(false)
const closeReason = ref('')
const aiJudgment = ref<any>(null)
const aiObservations = ref<any[]>([])
const aiContradiction = ref<any>(null)
const kanbanRef = ref<InstanceType<typeof PipelineKanban> | null>(null)
const aiHealthBorder = computed(() => {
  const t = aiJudgment.value?.judgmentText || ''
  if (t.includes('枯')) return 'border-red-200'
  if (t.includes('缓慢')) return 'border-amber-200'
  return 'border-green-200'
})
const campaignStats = ref<Record<string, number> | null>(null)

const sourcingSubOptions = [{ label: OBJECTS.platformTask, value: 'task' }, { label: OBJECTS.stagingPool, value: 'pool' }]
const candidatesSubOptions = [{ label: OBJECTS.activeCandidates, value: 'kanban' }, { label: '面试与录用', value: 'hiring' }]
const rulesSubOptions = [{ label: OBJECTS.jobRequirements, value: 'requirements' }, { label: OBJECTS.sourcingMethod, value: 'method' }, { label: '变更记录', value: 'audit' }]

const headCount = computed(() => job.value?.headCount ?? job.value?.headcount ?? 0)
const filledCount = computed(() => job.value?.filledCount ?? job.value?.onboardCount ?? 0)

const nextStep = computed(() => {
  if (!job.value) return null
  if (sourcedCount.value > 0) return { text: `有 ${sourcedCount.value} 位新人待你筛选`, action: () => { activeTab.value = 'candidates'; candidatesSub.value = 'kanban' } }
  if (job.value.status === 'ACTIVE' && !boardItems.value.length) return { text: '开启平台招人，开始为本职位找候选人', action: () => { activeTab.value = 'sourcing'; sourcingSub.value = 'task' } }
  if (boardItems.value.length) return { text: `本职位有 ${boardItems.value.length} 位在招候选人可跟进`, action: () => { activeTab.value = 'candidates'; candidatesSub.value = 'kanban' } }
  return { text: '完善职位要求与招人方式', action: () => { activeTab.value = 'rules'; rulesSub.value = 'requirements' } }
})

const displayJdText = computed(() => {
  const text = String(job.value?.jdText || '').trim()
  if (!text) return '暂无职位描述，请在「规则」中完善任职要求'
  if (text.startsWith('[') || text.startsWith('{')) { try { JSON.parse(text); return '职位描述已保存，请在「规则 → 职位要求」查看结构化要求' } catch {} }
  return text
})

const matchDistribution = computed(() => {
  const counts: Record<string, { count: number; tier: ReturnType<typeof parseMatchDetail>['status'] }> = {}
  for (const item of boardItems.value) { const v = parseMatchDetail(item.matchDetail, item.matchScore); if (!counts[v.label]) counts[v.label] = { count: 0, tier: v.status }; counts[v.label].count++ }
  return Object.entries(counts).map(([label, { count, tier }]) => ({ label, count, type: tierTagType(tier) }))
})

const stageSummary = computed(() =>
  boardColumns.value.map(col => ({ stage: col.stage, label: pipelineStageLabel(col.stage, 'column'), count: col.items?.length || 0 })).filter(s => s.count > 0 || ['SOURCED', 'INTERVIEWING', 'EVALUATED', 'OFFER'].includes(s.stage)),
)

function statusTagType(status: string) { return ({ DRAFT: 'info', ACTIVE: 'success', PAUSED: 'warning', CLOSED: 'info' } as Record<string, string>)[status] || 'info' }
function goInbox() { router.push({ path: '/workspace/inbox', query: { jobId: String(job.value?.id || '') } }) }
function goEditRequirements() { if (job.value?.id) router.push(`/planning/jobs/${job.value.id}/jd`) }
async function onSourcingMethodConfirmed() { toast.success('招人方式已更新') }
function switchJob(id: number | null) { if (id && id !== job.value?.id) router.push(`/planning/jobs/${id}`) }
function onKanbanLoaded(payload: { total: number; sourced: number }) { sourcedCount.value = payload.sourced }

async function onCandidateImported() {
  await loadBoardOnly()
  kanbanRef.value?.reload()
  const ok = await confirm({
    title: '纳入成功',
    message: `已${ACTIONS.importCandidate}。下一步：在「新人待筛选」中做初筛。`,
    confirmText: ACTIONS.goScreen,
    cancelText: '稍后',
  })
  if (ok) { activeTab.value = 'candidates'; candidatesSub.value = 'kanban' }
  else toast.success(`已${ACTIONS.importCandidate}`)
}

function normalizeTab(raw: string) { return ({ pipeline: 'candidates', requirements: 'rules', sourcing: 'sourcing', overview: 'overview', candidates: 'candidates', rules: 'rules' } as Record<string, string>)[raw] || 'overview' }

function parseTagsFromJobField(raw: unknown): RequirementItem[] {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.map((t: Record<string, unknown>) => fromApiTag(t))
  if (typeof raw === 'string') { try { const p = JSON.parse(raw); if (Array.isArray(p)) return p.map((t: Record<string, unknown>) => fromApiTag(t)) } catch {} }
  return []
}

async function loadAiInsight(jobId: number) {
  try {
    const jRes = await getJudgment('JOB', jobId).catch(() => ({ data: null }))
    aiJudgment.value = (jRes as any).data
    if (aiJudgment.value?.contradiction) {
      try { aiContradiction.value = JSON.parse(aiJudgment.value.contradiction) } catch {}
    }
    const oRes = await getObservations().catch(() => ({ data: null }))
    const obs = (oRes as any).data
    if (obs) aiObservations.value = [...(obs.critical||[]),...(obs.warnings||[])].filter((o:any)=>{
      try{return JSON.parse(o.relatedObjects||'[]').some((r:any)=>r.type==='JOB'&&r.id===jobId)}catch{return false}})
  } catch {}
}
async function loadCampaignStats(jobId: number) {
  try { const wfRes: any = await getWorkflowList({ jobId }); const workflows = wfRes.data || []; const active = workflows.find((w: any) => w.status === 'RUNNING' || w.status === 'PAUSED') || workflows[0]; if (!active?.id) { campaignStats.value = null; return }; const detailRes: any = await getWorkflowDetail(active.id); campaignStats.value = detailRes?.data?.stats || active.stats || null } catch { campaignStats.value = null }
}

async function loadBoardOnly() {
  if (!job.value?.id) return
  const boardRes: any = await getPipelineBoard(job.value.id); boardColumns.value = boardRes.data?.columns || []
  boardItems.value = boardColumns.value.flatMap((c: any) => (c.items || []).map((item: any) => ({ ...item, pipelineStage: item.pipelineStage || c.stage })))
  sourcedCount.value = boardColumns.value.find((c: any) => c.stage === 'SOURCED')?.items?.length || 0
}

async function loadJob(jobId: number) {
  loading.value = true
  try {
    const { data } = await getJobDetail(jobId); job.value = data
    try { const tagRes: any = await getTags(jobId); const raw = tagRes.data ?? tagRes; requirements.value = (Array.isArray(raw) ? raw : []).map((t: Record<string, unknown>) => fromApiTag(t)) } catch { requirements.value = [] }
    if (!requirements.value.length) requirements.value = parseTagsFromJobField(data?.tags)
    await loadBoardOnly(); await loadCampaignStats(jobId)
    loadAiInsight(jobId)
  } finally { loading.value = false }
}

async function handleActivate() { if (job.value?.id) { await activateJob(job.value.id); toast.success('已开始招聘'); await loadJob(job.value.id) } }
async function handlePause() { if (job.value?.id) { await pauseJob(job.value.id); toast.success('已暂停招聘'); await loadJob(job.value.id) } }
function openCloseDialog() { closeReason.value = ''; closeDialogVisible.value = true }
async function confirmClose() { if (!job.value?.id || !closeReason.value.trim()) { toast.error('请填写关闭原因'); return }; await closeJob(job.value.id, closeReason.value.trim()); toast.success('职位已关闭'); closeDialogVisible.value = false; await loadJob(job.value.id) }

watch(() => route.params.id, (id) => { if (id) loadJob(Number(id)) })

onMounted(() => {
  if (route.query.tab && typeof route.query.tab === 'string') { activeTab.value = normalizeTab(route.query.tab); if (activeTab.value === 'candidates') candidatesSub.value = 'kanban'; if (activeTab.value === 'rules') rulesSub.value = route.query.sub === 'method' ? 'method' : 'requirements' }
  loadJob(Number(route.params.id))
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.ws-breadcrumb { margin-bottom: $spacing-sm; }

.next-step-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $spacing-lg;
  padding: $spacing-md $spacing-lg;
  background: $primary-lighter;
  border-color: $primary-light;
}
.next-step-text { font-size: 14px; color: $text-regular; }
.next-label { font-weight: 700; color: $primary-dark; margin-right: $spacing-sm; }

.sub-seg { margin-bottom: $spacing-lg; }

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
  @media (max-width: #{$bp-desktop}) { grid-template-columns: 1fr; }
}

.funnel-row { display: flex; flex-wrap: wrap; gap: $spacing-md; margin: $spacing-md 0; }
.funnel-cell { min-width: 88px; padding: $spacing-sm $spacing-md; background: $bg-muted; border-radius: $border-radius-sm; text-align: center; }
.funnel-num { display: block; font-size: 20px; font-weight: 700; color: $text-primary; }
.funnel-label { font-size: 11px; color: $text-secondary; }

.content-card { padding: $spacing-lg; }
.jd-snippet { white-space: pre-wrap; line-height: 1.7; color: $text-regular; margin: 0; }
.hint { color: $text-secondary; font-size: 12px; margin-bottom: $spacing-sm; }
.dist-row { display: flex; justify-content: space-between; align-items: center; padding: $spacing-xs 0; font-size: 13px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: $spacing-md; margin-bottom: $spacing-md; }
</style>
