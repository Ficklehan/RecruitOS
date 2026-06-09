<template>
  <div class="candidate-workspace" :class="{ 'is-drawer': drawerMode }" v-loading="loading">
    <div v-if="!drawerMode" class="page-header">
      <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <div v-if="candidate" class="header-main">
        <h2 class="page-title">{{ candidate.name }}</h2>
        <p class="page-subtitle">{{ headerSubtitle }}</p>
      </div>
      <el-button v-if="candidate" link type="primary" @click="openFullWorkspace">全屏查看</el-button>
    </div>

    <template v-if="candidate">
      <header class="identity-bar">
        <div class="identity-left">
          <div class="avatar">{{ (candidate.name || '?').charAt(0) }}</div>
          <div class="identity-text">
            <div class="identity-top">
              <span class="identity-name">{{ candidate.name }}</span>
              <span class="identity-meta">{{ candidate.currentCompany || '—' }} · {{ candidate.currentTitle || '—' }}</span>
            </div>
            <div class="identity-sub">
              <span v-if="candidate.workYears != null" class="meta-pill">{{ candidate.workYears }}年</span>
              <span v-if="candidate.education" class="meta-pill">{{ educationLabel(candidate.education) }}</span>
              <span class="meta-pill">{{ sourceLabel(candidate.source) }}</span>
              <SkillChips
                v-if="normalizedResume.skills.length"
                :skills="normalizedResume.skills"
                :limit="6"
                compact
              />
            </div>
            <div v-if="contactLine" class="identity-contact">{{ contactLine }}</div>
          </div>
        </div>
        <div v-if="activeJobId && quickMatch" class="identity-verdict">
          <MatchVerdict
            :match-score="quickMatch.matchScore"
            :match-detail="quickMatch.matchDetail"
            mode="compact"
            :show-score="false"
          />
        </div>
      </header>

      <div class="workspace-body">
        <main class="workspace-main">
          <el-tabs v-model="activeTab" class="content-tabs">
            <el-tab-pane label="简历" name="resume">
              <div class="resume-toolbar">
                <el-radio-group v-model="resumeView" size="small">
                  <el-radio-button label="parsed">结构化简历</el-radio-button>
                  <el-radio-button label="original">原始 PDF</el-radio-button>
                </el-radio-group>
              </div>
              <ParsedResumePanel
                v-if="resumeView === 'parsed' && resumeLoaded"
                embedded
                hide-skills
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
                :actions="resumeId ? [
                  { label: '重新加载', type: 'primary', onClick: loadResume },
                ] : []"
              />
            </el-tab-pane>

            <el-tab-pane label="匹配评估" name="match">
              <MatchEvalPanel
                ref="matchPanelRef"
                :candidate-id="candidateId"
                :job-id="activeJobId"
                :show-actions="false"
                :show-full-page-link="true"
              />
            </el-tab-pane>
          </el-tabs>
        </main>

        <aside class="action-rail">
          <div class="rail-section">
            <label class="rail-label">在招职位</label>
            <el-select
              v-model="activeJobId"
              placeholder="选择职位"
              filterable
              size="default"
              class="rail-select"
              @change="onJobChange"
            >
              <el-option v-for="j in jobs" :key="j.jobId" :label="jobTitle(j.jobId)" :value="j.jobId" />
            </el-select>
          </div>

          <template v-if="activeJob">
            <div class="rail-stage">
              <span class="rail-label">当前进展</span>
              <el-tag size="small" type="primary" effect="light">
                {{ pipelineStageLabel(activeJob.pipelineStage) }}
              </el-tag>
            </div>

            <div class="rail-actions">
              <el-button type="primary" class="rail-btn" @click="handlePass">
                进入下一轮
              </el-button>
              <el-button class="rail-btn" @click="advanceActiveJob('INTERVIEWING')">
                安排面试
              </el-button>
              <el-button
                v-if="pendingFeedbackInterview"
                type="warning"
                plain
                class="rail-btn"
                @click="openFeedbackDrawer"
              >
                提交面试反馈
              </el-button>
              <el-button
                v-if="canPrepareOffer"
                type="success"
                plain
                class="rail-btn"
                @click="openOfferDialog"
              >
                准备录用通知
              </el-button>
              <el-button class="rail-btn" @click="handleReserve">
                储备至人才库
              </el-button>
              <el-button type="danger" plain class="rail-btn" @click="handleReject">
                标记不合适
              </el-button>
            </div>

            <el-button link type="primary" class="rail-link" @click="activeTab = 'match'">
              查看匹配评估
            </el-button>

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
            :actions="[
              { label: '去候选人列表', type: 'primary', onClick: () => router.push('/pipeline/candidates') },
            ]"
          />
        </aside>
      </div>
    </template>

    <el-dialog v-model="offerDialogVisible" title="准备录用通知" width="480px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="候选人">
          <span>{{ candidate?.name }}</span>
        </el-form-item>
        <el-form-item label="在招职位">
          <span>{{ activeJobId ? jobTitle(activeJobId) : '—' }}</span>
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="offerForm.department" placeholder="如：技术部" />
        </el-form-item>
        <el-form-item label="薪资">
          <el-input v-model="offerForm.salary" placeholder="如：35K · 15薪" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="offerForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="offerSubmitting" @click="submitOffer">创建并提交审批</el-button>
      </template>
    </el-dialog>

    <InterviewEvalDrawer
      v-model="feedbackDrawerVisible"
      :interview="pendingFeedbackInterview"
      @submitted="onFeedbackSubmitted"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import SkillChips from '@/components/candidate/SkillChips.vue'
import ParsedResumePanel from '@/components/candidate/ParsedResumePanel.vue'
import ResumeOriginalPanel from '@/components/candidate/ResumeOriginalPanel.vue'
import MatchEvalPanel from '@/components/candidate/MatchEvalPanel.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import InterviewEvalDrawer from '@/components/interview/InterviewEvalDrawer.vue'
import {
  pipelineStageLabel,
  sourceLabel,
  educationLabel,
} from '@/constants/businessLabels'
import { normalizeResumeData, type NormalizedResume } from '@/utils/resumeParser'
import { getDecisionPanel, screening } from '@/api/modules/candidate'
import { getCandidate360, advancePipelineStage } from '@/api/modules/pipeline'
import { getJobList } from '@/api/modules/job'
import { getResumeDetail } from '@/api/modules/resume'
import { createOffer, submitOfferApproval } from '@/api/modules/offer'
import { getInterviewList } from '@/api/modules/interview'

const props = withDefaults(defineProps<{
  candidateIdProp?: number
  jobIdProp?: number | null
  drawerMode?: boolean
}>(), {
  drawerMode: false,
  jobIdProp: null,
})

const emit = defineEmits<{
  loaded: [payload: { name: string; candidateId: number }]
}>()

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activeTab = ref('resume')
const resumeView = ref<'parsed' | 'original'>('parsed')
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
const offerForm = reactive({
  department: '',
  salary: '',
  remark: '',
})

const canPrepareOffer = computed(() => {
  const stage = activeJob.value?.pipelineStage
  return stage === 'EVALUATED' || stage === 'INTERVIEWING' || stage === 'OFFER'
})

const candidateId = computed(() => {
  if (props.candidateIdProp) return props.candidateIdProp
  return Number(route.params.id || route.params.candidateId) || 0
})

const normalizedResume = computed<NormalizedResume>(() =>
  normalizeResumeData(resumeRaw.value, candidate.value),
)

const activeJob = computed(() =>
  jobs.value.find(j => j.jobId === activeJobId.value) || null,
)

const jobTimeline = computed(() => {
  if (!activeJob.value) return timeline.value.slice(0, 5)
  return timeline.value
    .filter((log: any) => log.candidateJobId === activeJob.value?.id)
    .slice(0, 5)
})

const headerSubtitle = computed(() => {
  if (!candidate.value) return ''
  return [candidate.value.currentCompany, candidate.value.currentTitle].filter(Boolean).join(' · ')
})

const contactLine = computed(() => {
  if (!candidate.value) return ''
  const parts = [
    candidate.value.phone,
    candidate.value.email,
    candidate.value.workLocation ? `期望 ${candidate.value.workLocation}` : '',
  ].filter(Boolean)
  return parts.join(' · ')
})

function formatTime(val?: string) {
  if (!val) return '—'
  return val.replace('T', ' ').slice(0, 16)
}

function jobTitle(jobId: number) {
  return jobTitleMap.value[jobId] || `职位 #${jobId}`
}

function nextStage(stage?: string) {
  const flow = ['SOURCED', 'SCREENING', 'CONTACTED', 'INTERVIEWING', 'EVALUATED', 'OFFER', 'HIRED']
  const idx = flow.indexOf(stage || 'SOURCED')
  return idx < 0 || idx >= flow.length - 1 ? 'SCREENING' : flow[idx + 1]
}

function goBack() {
  if (props.drawerMode) return
  router.back()
}

function openFullWorkspace() {
  router.push({
    path: `/pipeline/candidates/${candidateId.value}`,
    query: activeJobId.value ? { jobId: String(activeJobId.value) } : {},
  })
}

async function loadQuickMatch() {
  if (!activeJobId.value || !candidateId.value) {
    quickMatch.value = null
    return
  }
  try {
    const res: any = await getDecisionPanel(candidateId.value, activeJobId.value)
    quickMatch.value = res.data || {}
  } catch {
    quickMatch.value = activeJob.value
  }
}

function onJobChange() {
  loadQuickMatch()
  loadPendingFeedback()
  matchPanelRef.value?.reload()
}

async function loadPendingFeedback() {
  if (!candidateId.value || !activeJobId.value) {
    pendingFeedbackInterview.value = null
    return
  }
  try {
    const res: any = await getInterviewList({
      candidateId: candidateId.value,
      jobId: activeJobId.value,
      status: 'COMPLETED',
      pageNum: 1,
      pageSize: 10,
    })
    const list = res.data?.list || []
    pendingFeedbackInterview.value = list.find((i: any) => !i.evaluation) || null
  } catch {
    pendingFeedbackInterview.value = null
  }
}

function openFeedbackDrawer() {
  if (!pendingFeedbackInterview.value) return
  feedbackDrawerVisible.value = true
}

async function onFeedbackSubmitted() {
  await loadPendingFeedback()
  load()
}

async function loadResume() {
  resumeLoaded.value = false
  if (!resumeId.value) {
    resumeRaw.value = null
    return
  }
  try {
    const res: any = await getResumeDetail(resumeId.value)
    resumeRaw.value = res.data || res
    resumeLoaded.value = true
  } catch {
    resumeRaw.value = null
    resumeLoaded.value = false
  }
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
    if (queryJob && jobs.value.some(j => j.jobId === queryJob)) {
      activeJobId.value = queryJob
    } else if (jobs.value.length) {
      activeJobId.value = jobs.value[0].jobId
    }

    if (route.query.tab && typeof route.query.tab === 'string') {
      const tab = route.query.tab
      activeTab.value = tab === 'overview' ? 'resume' : tab
    }

    await Promise.all([loadResume(), loadQuickMatch(), loadPendingFeedback()])
    emit('loaded', { name: candidate.value?.name || '候选人', candidateId: candidateId.value })
  } finally {
    loading.value = false
  }
}

async function advanceActiveJob(stage: string) {
  if (!activeJob.value) return
  await advancePipelineStage(activeJob.value.id, { toStage: stage })
  ElMessage.success('进展已更新')
  load()
}

async function handlePass() {
  if (!activeJob.value) return
  await advancePipelineStage(activeJob.value.id, { toStage: nextStage(activeJob.value.pipelineStage) })
  await screening(activeJob.value.candidateId, activeJob.value.jobId, { screeningStatus: 'PASSED' })
  ElMessage.success('已进入下一轮')
  load()
  matchPanelRef.value?.reload()
}

async function handleReject() {
  if (!activeJob.value) return
  const { value, action } = await ElMessageBox.prompt(
    '该候选人将结束在本职位的招聘流程。',
    '标记不合适',
    { inputPlaceholder: '不合适原因（选填）', distinguishCancelAndClose: true },
  ).catch((e) => e)
  if (action === 'cancel' || action === 'close') return
  await advancePipelineStage(activeJob.value.id, {
    toStage: 'ARCHIVED',
    reasonCode: 'NOT_FIT',
    comment: value || '本职位不合适',
    archivedToPool: false,
  })
  await screening(activeJob.value.candidateId, activeJob.value.jobId, {
    screeningStatus: 'REJECTED',
    screenerComment: value || '',
  })
  ElMessage.success('已标记不合适')
  load()
}

async function handleReserve() {
  if (!activeJob.value) return
  try {
    await ElMessageBox.confirm('储备至人才库不会结束本职位流程，确定储备吗？', '储备至人才库', {
      confirmButtonText: '确认储备',
      cancelButtonText: '取消',
    })
    await screening(activeJob.value.candidateId, activeJob.value.jobId, { screeningStatus: 'RESERVE' })
    ElMessage.success('已储备至人才库')
  } catch { /* cancel */ }
}

function openOfferDialog() {
  offerForm.department = ''
  offerForm.salary = ''
  offerForm.remark = ''
  offerDialogVisible.value = true
}

async function submitOffer() {
  if (!candidate.value || !activeJobId.value) return
  if (!offerForm.salary.trim()) {
    ElMessage.warning('请填写薪资')
    return
  }
  offerSubmitting.value = true
  try {
    const res: any = await createOffer({
      candidateId: candidateId.value,
      candidateName: candidate.value.name,
      jobId: activeJobId.value,
      jobTitle: jobTitle(activeJobId.value),
      department: offerForm.department,
      salary: offerForm.salary,
      remark: offerForm.remark,
    })
    const offerId = res.data?.id || res?.id
    if (offerId) {
      await submitOfferApproval(offerId).catch(() => null)
    }
    if (activeJob.value && activeJob.value.pipelineStage !== 'OFFER') {
      await advancePipelineStage(activeJob.value.id, { toStage: 'OFFER' })
    }
    ElMessage.success('录用通知已创建，可在「录用相关」待办中跟进')
    offerDialogVisible.value = false
    load()
    router.push('/pipeline/offers')
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    offerSubmitting.value = false
  }
}

watch(() => candidateId.value, load)
watch(() => props.jobIdProp, (id) => {
  if (id) {
    activeJobId.value = id
    loadQuickMatch()
    matchPanelRef.value?.reload()
  }
})

onMounted(async () => {
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 200 })
    const list = res.data?.list || res.data?.records || []
    jobTitleMap.value = Object.fromEntries(list.map((j: any) => [j.id, j.title]))
  } catch { /* ignore */ }
  load()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.candidate-workspace {
  &.is-drawer {
    padding: 0;
  }
}

.header-main {
  flex: 1;
  min-width: 0;
}

.identity-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 12px;
  background: $bg-card;
  border: 1px solid $header-border;
  border-radius: 8px;
}

.identity-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: $primary-color;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.identity-text {
  min-width: 0;
  flex: 1;
}

.identity-top {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.identity-name {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.identity-meta {
  font-size: 13px;
  color: $text-secondary;
}

.identity-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  flex-wrap: wrap;
  min-height: 20px;
}

.meta-pill {
  font-size: 11px;
  line-height: 18px;
  padding: 0 6px;
  border-radius: 4px;
  background: $bg-muted;
  color: $text-secondary;
  flex-shrink: 0;
}

.identity-verdict {
  flex-shrink: 0;
  max-width: 220px;
}

.workspace-body {
  display: grid;
  grid-template-columns: 1fr 248px;
  gap: 14px;
  align-items: start;

  @media (max-width: 860px) {
    grid-template-columns: 1fr;
  }
}

.workspace-main {
  min-width: 0;
  background: $bg-card;
  border: 1px solid $header-border;
  border-radius: 8px;
  padding: 0 14px 14px;
}

.content-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }

  :deep(.el-tabs__item) {
    font-size: 13px;
    height: 40px;
  }
}

.identity-contact {
  margin-top: 4px;
  font-size: 12px;
  color: $text-placeholder;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.resume-toolbar {
  margin-bottom: 10px;
}

.action-rail {
  position: sticky;
  top: 0;
  background: $bg-card;
  border: 1px solid $header-border;
  border-radius: 8px;
  padding: 14px;
}

.rail-section {
  margin-bottom: 12px;
}

.rail-label {
  display: block;
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 6px;
}

.rail-select {
  width: 100%;
}

.rail-stage {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid $bg-muted;
}

.rail-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rail-btn {
  width: 100%;
  margin: 0;
}

.rail-link {
  margin-top: 10px;
  padding: 0;
  font-size: 12px;
}

.rail-timeline {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid $bg-muted;
}

.rail-timeline-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11px;
  color: $text-regular;
  padding: 5px 0;
  border-bottom: 1px solid #f8fafc;

  &:last-child {
    border-bottom: none;
  }
}

.rail-timeline-time {
  color: $text-placeholder;
  font-size: 10px;
}
</style>
