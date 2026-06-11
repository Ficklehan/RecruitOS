<template>
  <div class="candidate-workspace relative" :class="{ 'is-drawer': drawerMode }">
    <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <div v-if="!drawerMode" class="ws-breadcrumb">
      <RButton variant="ghost" @click="goBack">
        <ArrowLeft class="mr-2 h-4 w-4" />
        返回候选人列表
      </RButton>
    </div>

    <template v-if="candidate">
      <ObjectHeader
        :name="candidate.name"
        :avatar-text="candidate.name"
        :meta="[
          candidate.currentCompany || '—',
          candidate.currentTitle || '—',
          candidate.workYears != null ? `${candidate.workYears}年` : '',
          candidate.education ? educationLabel(candidate.education) : '',
        ].filter(Boolean).join(' · ')"
      >
        <template #actions>
          <RButton v-if="drawerMode" variant="link" @click="openFullWorkspace">全屏查看</RButton>
        </template>
      </ObjectHeader>

      <div v-if="activeJobId" class="context-strip">
        <span class="context-strip__label">当前职位：</span>
        <span>{{ jobTitle(activeJobId) }}</span>
        <span>·</span>
        <span>阶段：</span>
        <RBadge variant="default">{{ pipelineStageLabel(activeJob?.pipelineStage) }}</RBadge>
        <span v-if="quickMatch?.matchScore != null">· 匹配 {{ quickMatch.matchScore }}%</span>
      </div>

      <div class="workspace-body">
        <main class="workspace-main">
          <RTabs v-model="activeTab" class="content-tabs">
            <RTabsList>
              <RTabsTrigger value="resume">简历</RTabsTrigger>
              <RTabsTrigger value="match">匹配评估</RTabsTrigger>
            </RTabsList>

            <RTabsContent value="resume">
              <div class="resume-toolbar">
                <SegmentedControl
                  v-model="resumeView"
                  :options="resumeViewOptions"
                />
              </div>
              <ParsedResumePanel
                v-if="resumeView === 'parsed' && resumeLoaded"
                embedded hide-skills
                :resume="normalizedResume"
                :candidate="candidate"
              />
              <ResumeOriginalPanel
                v-else-if="resumeView === 'original' && resumeLoaded"
                :resume="normalizedResume"
              />
              <EmptyStateCta
                v-else
                title="暂无简历"
                description="候选人尚未关联已解析简历"
                :image-size="56"
                :actions="resumeId ? [{ label: '重新加载', type: 'primary', onClick: loadResume }] : []"
              />
            </RTabsContent>

            <RTabsContent value="match">
              <MatchEvalPanel
                ref="matchPanelRef"
                :candidate-id="candidateId"
                :job-id="activeJobId"
                :show-actions="false"
                :show-full-page-link="true"
              />
            </RTabsContent>
          </RTabs>
        </main>

        <aside class="action-rail">
          <div class="rail-section">
            <label class="rail-label">在招职位</label>
            <RSelect
              v-model="activeJobId"
              :options="jobSelectOptions"
              placeholder="选择职位"
              class="rail-select w-full"
              @update:model-value="onJobChange"
            />
          </div>

          <template v-if="activeJob">
            <div class="rail-stage">
              <span class="rail-label">当前进展</span>
              <RBadge variant="default">{{ pipelineStageLabel(activeJob.pipelineStage) }}</RBadge>
            </div>

            <div class="rail-actions">
              <RButton class="rail-btn" @click="handlePass">进入下一轮</RButton>
              <RButton variant="outline" class="rail-btn" @click="advanceActiveJob('INTERVIEWING')">安排面试</RButton>
              <RButton variant="destructive" class="rail-btn" @click="handleReject">标记不合适</RButton>
              <RDropdown>
                <DropdownMenuTrigger>
                  <RButton variant="outline" class="rail-btn w-full">更多操作</RButton>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem v-if="pendingFeedbackInterview" @click="feedbackDrawerVisible = true">
                    提交面试反馈
                  </DropdownMenuItem>
                  <DropdownMenuItem v-if="canPrepareOffer" @click="openOfferDialog">
                    准备录用通知
                  </DropdownMenuItem>
                  <DropdownMenuItem @click="handleReserve">储备至人才库</DropdownMenuItem>
                </DropdownMenuContent>
              </RDropdown>
            </div>

            <RButton variant="link" class="rail-link" @click="activeTab = 'match'">
              查看匹配评估
            </RButton>

            <div v-if="jobTimeline.length" class="rail-timeline">
              <div class="rail-label">最近进展</div>
              <div v-for="log in jobTimeline" :key="log.id" class="rail-timeline-item">
                <span class="rail-timeline-time">{{ formatTime(log.createdAt) }}</span>
                <span>{{ pipelineStageLabel(log.fromStage) }} → {{ pipelineStageLabel(log.toStage) }}</span>
              </div>
            </div>
          </template>

          <EmptyStateCta
            v-else-if="!jobs.length"
            title="未关联职位"
            description="请先在候选人列表关联在招职位"
            :image-size="48"
            :actions="[{ label: '去候选人列表', type: 'primary', onClick: () => router.push('/pipeline/candidates') }]"
          />
        </aside>
      </div>
    </template>

    <RDialog v-model:open="offerDialogVisible">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>准备录用通知</DialogTitle>
        </DialogHeader>
        <FormField label="候选人">
          <span>{{ candidate?.name }}</span>
        </FormField>
        <FormField label="在招职位" class="mt-4">
          <span>{{ activeJobId ? jobTitle(activeJobId) : '—' }}</span>
        </FormField>
        <FormField label="部门" class="mt-4">
          <RInput v-model="offerForm.department" placeholder="如：技术部" />
        </FormField>
        <FormField label="薪资" class="mt-4">
          <RInput v-model="offerForm.salary" placeholder="如：35K · 15薪" />
        </FormField>
        <div v-if="offerStrategy" class="mt-4 p-3 rounded-lg bg-primary/5 border border-primary/10">
          <div class="flex items-center gap-2 mb-2">
            <Sparkles class="h-3.5 w-3.5 text-primary" />
            <span class="text-[12px] font-semibold text-foreground">AI 薪资建议</span>
          </div>
          <div class="flex items-center gap-3 text-[12px]">
            <span class="text-muted-foreground">{{ (offerStrategy.suggestedRange.min / 10000).toFixed(0) }}万</span>
            <span class="text-primary font-bold">{{ (offerStrategy.suggestedRange.mid / 10000).toFixed(0) }}万</span>
            <span class="text-muted-foreground">{{ (offerStrategy.suggestedRange.max / 10000).toFixed(0) }}万</span>
            <span class="text-[10px] text-muted-foreground ml-auto">置信度 {{ ((offerStrategy.confidence || 0) * 100).toFixed(0) }}%</span>
          </div>
        </div>
        <FormField label="备注" class="mt-4">
          <RTextarea v-model="offerForm.remark" :rows="2" />
        </FormField>
        <DialogFooter>
          <RButton variant="outline" @click="offerDialogVisible = false">取消</RButton>
          <RButton :disabled="offerSubmitting" @click="submitOffer">创建并提交审批</RButton>
        </DialogFooter>
      </DialogContent>
    </RDialog>

    <InterviewEvalDrawer v-model="feedbackDrawerVisible" :interview="pendingFeedbackInterview" @submitted="onFeedbackSubmitted" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2, Sparkles } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { prompt } from '@/lib/prompt'
import ObjectHeader from '@/components/Layout/ObjectHeader.vue'
import FormField from '@/components/app/FormField.vue'
import SegmentedControl from '@/components/app/SegmentedControl.vue'
import ParsedResumePanel from '@/components/candidate/ParsedResumePanel.vue'
import ResumeOriginalPanel from '@/components/candidate/ResumeOriginalPanel.vue'
import MatchEvalPanel from '@/components/candidate/MatchEvalPanel.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import InterviewEvalDrawer from '@/components/interview/InterviewEvalDrawer.vue'
import {
  RButton, RBadge, RSelect, RTabs, RTabsList, RTabsTrigger, RTabsContent,
  RDialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
  RInput, RTextarea,
  RDropdown, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem,
} from '@/components/ui'
import { pipelineStageLabel, educationLabel } from '@/constants/businessLabels'
import { normalizeResumeData, type NormalizedResume } from '@/utils/resumeParser'
import { getDecisionPanel, screening } from '@/api/modules/candidate'
import { getCandidate360, advancePipelineStage } from '@/api/modules/pipeline'
import { getJobList } from '@/api/modules/job'
import { getResumeDetail } from '@/api/modules/resume'
import { createOffer, submitOfferApproval } from '@/api/modules/offer'
import { getOfferStrategy, type OfferStrategy } from '@/api/modules/brain'
import { getInterviewList } from '@/api/modules/interview'

const props = withDefaults(defineProps<{
  candidateIdProp?: number
  jobIdProp?: number | null
  drawerMode?: boolean
}>(), { drawerMode: false, jobIdProp: null })

const emit = defineEmits<{ loaded: [payload: { name: string; candidateId: number }] }>()

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activeTab = ref('resume')
const resumeView = ref('parsed')
const resumeViewOptions = [
  { label: '结构化简历', value: 'parsed' },
  { label: '原始 PDF', value: 'original' },
]
const candidate = ref<any>(null)
const jobs = ref<any[]>([])
const timeline = ref<any[]>([])
const activeJobId = ref<number | null>(null)
const quickMatch = ref<any>(null)
const resumeId = ref<number | null>(null)
const resumeRaw = ref<Record<string, unknown> | null>(null)
const resumeLoaded = ref(false)
const jobTitleMap = ref<Record<number, string>>({})
const matchPanelRef = ref<InstanceType<typeof MatchEvalPanel> | null>(null)
const offerDialogVisible = ref(false)
const offerSubmitting = ref(false)
const feedbackDrawerVisible = ref(false)
const pendingFeedbackInterview = ref<any | null>(null)
const offerForm = reactive({ department: '', salary: '', remark: '' })
const offerStrategy = ref<OfferStrategy | null>(null)
const offerStrategyLoading = ref(false)

const jobSelectOptions = computed(() =>
  jobs.value.map(j => ({ label: jobTitle(j.jobId), value: j.jobId }))
)

const canPrepareOffer = computed(() => {
  const stage = activeJob.value?.pipelineStage
  return stage === 'EVALUATED' || stage === 'INTERVIEWING' || stage === 'OFFER'
})

const candidateId = computed(() => {
  if (props.candidateIdProp) return props.candidateIdProp
  return Number(route.params.id || route.params.candidateId) || 0
})

const normalizedResume = computed<NormalizedResume>(() => normalizeResumeData(resumeRaw.value, candidate.value))
const activeJob = computed(() => jobs.value.find(j => j.jobId === activeJobId.value) || null)
const jobTimeline = computed(() => {
  if (!activeJob.value) return timeline.value.slice(0, 5)
  return timeline.value.filter((log: any) => log.candidateJobId === activeJob.value?.id).slice(0, 5)
})

function formatTime(val?: string) {
  if (!val) return '—'
  return val.replace('T', ' ').slice(0, 16)
}

function jobTitle(jobId: number) { return jobTitleMap.value[jobId] || `职位 #${jobId}` }

function nextStage(stage?: string) {
  const flow = ['SOURCED', 'SCREENING', 'CONTACTED', 'INTERVIEWING', 'EVALUATED', 'OFFER', 'HIRED']
  const idx = flow.indexOf(stage || 'SOURCED')
  return idx < 0 || idx >= flow.length - 1 ? 'SCREENING' : flow[idx + 1]
}

function goBack() { if (!props.drawerMode) router.back() }
function openFullWorkspace() { router.push({ path: `/pipeline/candidates/${candidateId.value}`, query: activeJobId.value ? { jobId: String(activeJobId.value) } : {} }) }

async function loadQuickMatch() {
  if (!activeJobId.value || !candidateId.value) { quickMatch.value = null; return }
  try { const res: any = await getDecisionPanel(candidateId.value, activeJobId.value); quickMatch.value = res.data || {} } catch { quickMatch.value = activeJob.value }
}

function onJobChange() { loadQuickMatch(); loadPendingFeedback(); matchPanelRef.value?.reload() }

async function loadPendingFeedback() {
  if (!candidateId.value || !activeJobId.value) { pendingFeedbackInterview.value = null; return }
  try {
    const res: any = await getInterviewList({ candidateId: candidateId.value, jobId: activeJobId.value, status: 'COMPLETED', pageNum: 1, pageSize: 10 })
    const list = res.data?.list || []
    pendingFeedbackInterview.value = list.find((i: any) => !i.evaluation) || null
  } catch { pendingFeedbackInterview.value = null }
}

async function onFeedbackSubmitted() { await loadPendingFeedback(); load() }

async function loadResume() {
  resumeLoaded.value = false
  if (!resumeId.value) { resumeRaw.value = null; return }
  try { const res: any = await getResumeDetail(resumeId.value); resumeRaw.value = res.data || res; resumeLoaded.value = true } catch { resumeRaw.value = null; resumeLoaded.value = false }
}

async function load() {
  if (!candidateId.value) return
  loading.value = true
  try {
    const { data } = await getCandidate360(candidateId.value)
    candidate.value = data.candidate
    jobs.value = data.jobs || []
    timeline.value = data.timeline || []
    resumeId.value = data.candidate?.resumeId || null

    const queryJob = props.jobIdProp || Number(route.query.jobId) || null
    if (queryJob && jobs.value.some(j => j.jobId === queryJob)) activeJobId.value = queryJob
    else if (jobs.value.length) activeJobId.value = jobs.value[0].jobId

    if (route.query.tab && typeof route.query.tab === 'string') activeTab.value = route.query.tab === 'overview' ? 'resume' : route.query.tab

    await Promise.all([loadResume(), loadQuickMatch(), loadPendingFeedback()])
    emit('loaded', { name: candidate.value?.name || '候选人', candidateId: candidateId.value })
  } finally { loading.value = false }
}

async function advanceActiveJob(stage: string) {
  if (!activeJob.value) return
  await advancePipelineStage(activeJob.value.id, { toStage: stage })
  toast.success('进展已更新')
  load()
}

async function handlePass() {
  if (!activeJob.value) return
  await advancePipelineStage(activeJob.value.id, { toStage: nextStage(activeJob.value.pipelineStage) })
  await screening(activeJob.value.candidateId, activeJob.value.jobId, { screeningStatus: 'PASSED' })
  toast.success('已进入下一轮')
  load()
  matchPanelRef.value?.reload()
}

async function handleReject() {
  if (!activeJob.value) return
  const value = await prompt({
    title: '标记不合适',
    message: '该候选人将结束在本职位的招聘流程。',
    placeholder: '不合适原因（选填）',
  })
  if (value === null) return
  await advancePipelineStage(activeJob.value.id, { toStage: 'ARCHIVED', reasonCode: 'NOT_FIT', comment: value || '本职位不合适', archivedToPool: false })
  await screening(activeJob.value.candidateId, activeJob.value.jobId, { screeningStatus: 'REJECTED', screenerComment: value || '' })
  toast.success('已标记不合适')
  load()
}

async function handleReserve() {
  if (!activeJob.value) return
  const ok = await confirm({
    title: '储备至人才库',
    message: '储备至人才库不会结束本职位流程，确定储备吗？',
    confirmText: '确认储备',
  })
  if (!ok) return
  await screening(activeJob.value.candidateId, activeJob.value.jobId, { screeningStatus: 'RESERVE' })
  toast.success('已储备至人才库')
}

async function openOfferDialog() {
  offerForm.department = ''; offerForm.salary = ''; offerForm.remark = ''
  offerStrategy.value = null
  offerDialogVisible.value = true
  if (candidate.value?.name && activeJobId.value) {
    offerStrategyLoading.value = true
    try {
      const res = await getOfferStrategy(candidateId.value, activeJobId.value, candidate.value.name, jobTitle(activeJobId.value))
      offerStrategy.value = res.data
      if (!offerForm.salary && res.data?.suggestedRange?.mid) {
        offerForm.salary = (res.data.suggestedRange.mid / 10000).toFixed(0) + '万'
      }
    } catch { offerStrategy.value = null } finally { offerStrategyLoading.value = false }
  }
}

async function submitOffer() {
  if (!candidate.value || !activeJobId.value) return
  if (!offerForm.salary.trim()) { toast.error('请填写薪资'); return }
  offerSubmitting.value = true
  try {
    const res: any = await createOffer({ candidateId: candidateId.value, candidateName: candidate.value.name, jobId: activeJobId.value, jobTitle: jobTitle(activeJobId.value), department: offerForm.department, salary: offerForm.salary, remark: offerForm.remark })
    const offerId = res.data?.id || res?.id
    if (offerId) await submitOfferApproval(offerId).catch(() => null)
    if (activeJob.value && activeJob.value.pipelineStage !== 'OFFER') await advancePipelineStage(activeJob.value.id, { toStage: 'OFFER' })
    toast.success('录用通知已创建')
    offerDialogVisible.value = false
    load()
    router.push('/pipeline/offers')
  } catch (e: any) { toast.error(e?.message || '创建失败') } finally { offerSubmitting.value = false }
}

watch(() => candidateId.value, load)
watch(() => props.jobIdProp, (id) => { if (id) { activeJobId.value = id; loadQuickMatch(); matchPanelRef.value?.reload() } })

onMounted(async () => {
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 200 })
    const list = res.data?.list || res.data?.records || []
    jobTitleMap.value = Object.fromEntries(list.map((j: any) => [j.id, j.title]))
  } catch {}
  load()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.ws-breadcrumb {
  margin-bottom: $spacing-sm;
}

.workspace-body {
  display: grid;
  grid-template-columns: 1fr #{$action-rail-width};
  gap: $spacing-section;
  align-items: start;

  @media (max-width: #{$bp-desktop}) {
    grid-template-columns: 1fr;
  }
}

.workspace-main {
  min-width: 0;
  background: $bg-card;
  border-radius: $border-radius;
  padding: 0 $spacing-lg $spacing-lg;
  box-shadow: $shadow-soft;
}

.content-tabs {
  margin-top: 8px;
}

.resume-toolbar { margin-bottom: 10px; }

.action-rail {
  position: sticky;
  top: 0;
  background: $bg-card;
  border-radius: $border-radius;
  padding: $spacing-lg;
  box-shadow: $shadow-soft;
}

.rail-section { margin-bottom: $spacing-md; }

.rail-label {
  display: block;
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: $spacing-sm;
}

.rail-select { width: 100%; }

.rail-stage {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: $divider;
}

.rail-actions {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.rail-btn { width: 100%; margin: 0; }

.rail-link { margin-top: $spacing-sm; padding: 0; font-size: 12px; }

.rail-timeline {
  margin-top: $spacing-lg;
  padding-top: $spacing-md;
  border-top: $divider;
}

.rail-timeline-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11px;
  color: $text-regular;
  padding: $spacing-xs 0;
  border-bottom: $divider;
  &:last-child { border-bottom: none; }
}

.rail-timeline-time { color: $text-placeholder; font-size: 11px; }
</style>
