<template>
  <div class="page-container page-stack" v-loading="loading">
    <div class="page-header">
      <el-button text @click="$router.push('/planning/jobs')">
        <el-icon><ArrowLeft /></el-icon>
        返回在招职位
      </el-button>
      <div class="header-main">
        <div class="title-row">
          <h2 class="page-title">{{ job?.title || '在招职位工作台' }}</h2>
          <el-tag v-if="job" :type="statusTagType(job.status)" size="small" disable-transitions>
            {{ jobStatusLabel(job.status) }}
          </el-tag>
        </div>
        <p v-if="job" class="page-subtitle">
          编号 {{ job.jobNo }}
          <span v-if="job.demandNo"> · 关联需求 {{ job.demandNo }}</span>
          · 还差 {{ Math.max(0, headCount - filledCount) }} 人（{{ filledCount }}/{{ headCount }}）
        </p>
      </div>
      <div v-if="job" class="header-actions">
        <el-button
          v-if="job.status === 'DRAFT'"
          type="success"
          @click="handleActivate"
        >
          开始招聘
        </el-button>
        <el-button v-if="job.status === 'ACTIVE'" type="warning" @click="handlePause">
          暂停招聘
        </el-button>
        <el-button v-if="job.status !== 'CLOSED'" type="danger" plain @click="openCloseDialog">
          关闭职位
        </el-button>
      </div>
    </div>

    <div v-if="job && nextStep" class="next-step-bar data-card">
      <div class="next-step-text">
        <span class="next-label">下一步</span>
        <span>{{ nextStep.text }}</span>
      </div>
      <el-button type="primary" @click="nextStep.action">{{ ACTIONS.goProcess }}</el-button>
    </div>

    <JobContextBar v-if="job" :model-value="job.id" @update:model-value="switchJob">
      <el-button type="primary" link @click="goInbox">打开收件箱</el-button>
    </JobContextBar>

    <template v-if="job">
      <el-tabs v-model="activeTab" class="workspace-tabs">
        <el-tab-pane :label="JOB_WORKSPACE_TABS.overview" name="overview">
          <div class="overview-grid">
            <div class="data-card funnel-card">
              <h4>管人 · 在招候选人</h4>
              <div class="funnel-row">
                <div v-for="s in stageSummary" :key="s.stage" class="funnel-cell">
                  <span class="funnel-num">{{ s.count }}</span>
                  <span class="funnel-label">{{ s.label }}</span>
                </div>
              </div>
              <el-button type="primary" link @click="activeTab = 'candidates'">
                查看{{ OBJECTS.activeCandidates }}
              </el-button>
            </div>

            <div class="data-card funnel-card">
              <h4>找人 · 平台与待联系池</h4>
              <div v-if="campaignStats" class="funnel-row sourcing-stats">
                <div class="funnel-cell">
                  <span class="funnel-num">{{ campaignStats.published || 0 }}</span>
                  <span class="funnel-label">已发布</span>
                </div>
                <div class="funnel-cell">
                  <span class="funnel-num">{{ campaignStats.searched || 0 }}</span>
                  <span class="funnel-label">已检索</span>
                </div>
                <div class="funnel-cell">
                  <span class="funnel-num">{{ campaignStats.greeted || 0 }}</span>
                  <span class="funnel-label">已联系</span>
                </div>
                <div class="funnel-cell">
                  <span class="funnel-num">{{ campaignStats.imported || 0 }}</span>
                  <span class="funnel-label">已纳入</span>
                </div>
              </div>
              <p v-else class="hint">在「找人」中开启平台招人任务，或将人选纳入待联系池。</p>
              <el-button type="primary" link @click="activeTab = 'sourcing'">
                {{ campaignStats ? '查看平台招人任务' : ACTIONS.startPlatformTask }}
              </el-button>
            </div>

            <div class="data-card">
              <h4>{{ OBJECTS.jobDescription }}</h4>
              <p class="jd-snippet">{{ displayJdText }}</p>
            </div>

            <div class="data-card">
              <h4>匹配建议分布</h4>
              <p class="hint">基于本职位已关联候选人的最新评估</p>
              <div v-for="row in matchDistribution" :key="row.label" class="dist-row">
                <span>{{ row.label }}</span>
                <el-tag size="small" :type="row.type">{{ row.count }}</el-tag>
              </div>
              <EmptyStateCta
                v-if="!matchDistribution.length"
                :image-size="48"
                description="本职位尚无在招候选人，可先开始平台招人"
                :actions="[
                  { label: ACTIONS.startPlatformTask, type: 'primary', onClick: () => activeTab = 'sourcing' },
                ]"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="JOB_WORKSPACE_TABS.sourcing" name="sourcing">
          <el-segmented v-model="sourcingSub" :options="sourcingSubOptions" class="sub-seg" />
          <JobSourcing
            v-if="sourcingSub === 'task'"
            :job-id="job.id"
            :job-title="job.title"
            :job-status="job.status"
            @imported="onCandidateImported"
          />
          <ChannelStaging
            v-else
            embedded
            :default-job-id="job.id"
          />
        </el-tab-pane>

        <el-tab-pane :label="JOB_WORKSPACE_TABS.candidates" name="candidates">
          <el-segmented v-model="candidatesSub" :options="candidatesSubOptions" class="sub-seg" />
          <PipelineKanban
            v-if="candidatesSub === 'kanban'"
            ref="kanbanRef"
            :job-id="job.id"
            @loaded="onKanbanLoaded"
          />
          <JobHiringSummary
            v-else
            :job-id="job.id"
            @go-kanban="candidatesSub = 'kanban'"
          />
        </el-tab-pane>

        <el-tab-pane :label="JOB_WORKSPACE_TABS.rules" name="rules">
          <el-segmented v-model="rulesSub" :options="rulesSubOptions" class="sub-seg" />

          <div v-if="rulesSub === 'requirements'" class="data-card content-card">
            <div class="section-head">
              <h4>{{ OBJECTS.jobRequirements }}清单</h4>
              <el-button type="primary" link @click="goEditRequirements">编辑</el-button>
            </div>
            <p class="hint">必备与加分项将用于候选人匹配评估。</p>
            <el-table v-if="requirements.length" :data="requirements" size="small" stripe>
              <el-table-column prop="name" label="要求项" min-width="140" />
              <el-table-column label="类型" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.requirementType === 'REQUIRED' ? 'danger' : 'info'">
                    {{ REQUIREMENT_TYPE_LABEL[row.requirementType] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="重要程度" width="90">
                <template #default="{ row }">{{ IMPORTANCE_LABEL[row.importance] }}</template>
              </el-table-column>
            </el-table>
            <EmptyStateCta
              v-else
              title="尚未设置任职要求"
              description="粘贴职位描述后可自动提取，也可手动添加必备与加分项"
              :actions="[
                { label: ACTIONS.editRequirements, type: 'primary', onClick: goEditRequirements },
              ]"
            />
          </div>

          <div v-else-if="rulesSub === 'method'" class="content-card">
            <SourcingMethodWizard :job-id="job.id" @confirmed="onSourcingMethodConfirmed" />
          </div>

          <div v-else class="content-card">
            <JobAuditTimeline :job-id="job.id" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="closeDialogVisible" title="关闭在招职位" width="420px">
      <el-form label-width="80px">
        <el-form-item label="关闭原因">
          <el-input v-model="closeReason" type="textarea" :rows="3" placeholder="请输入关闭原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmClose">确定关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import JobContextBar from '@/components/common/JobContextBar.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import PipelineKanban from '@/components/pipeline/PipelineKanban.vue'
import ChannelStaging from '@/views/channel/ChannelStaging.vue'
import JobSourcing from './JobSourcing.vue'
import JobHiringSummary from './JobHiringSummary.vue'
import SourcingMethodWizard from '@/components/job/SourcingMethodWizard.vue'
import JobAuditTimeline from '@/components/job/JobAuditTimeline.vue'
import {
  ACTIONS,
  JOB_WORKSPACE_TABS,
  OBJECTS,
  jobStatusLabel,
  pipelineStageLabel,
} from '@/constants/businessLabels'
import {
  type RequirementItem,
  fromApiTag,
  IMPORTANCE_LABEL,
  REQUIREMENT_TYPE_LABEL,
} from '@/utils/jdRequirements'
import { parseMatchDetail, tierTagType } from '@/utils/matchVerdict'
import {
  activateJob,
  closeJob,
  getJobDetail,
  getTags,
  pauseJob,
} from '@/api/modules/job'
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
const kanbanRef = ref<InstanceType<typeof PipelineKanban> | null>(null)
const campaignStats = ref<Record<string, number> | null>(null)

const sourcingSubOptions = [
  { label: OBJECTS.platformTask, value: 'task' },
  { label: OBJECTS.stagingPool, value: 'pool' },
]
const candidatesSubOptions = [
  { label: OBJECTS.activeCandidates, value: 'kanban' },
  { label: '面试与录用', value: 'hiring' },
]
const rulesSubOptions = [
  { label: OBJECTS.jobRequirements, value: 'requirements' },
  { label: OBJECTS.sourcingMethod, value: 'method' },
  { label: '变更记录', value: 'audit' },
]

const headCount = computed(() => job.value?.headCount ?? job.value?.headcount ?? 0)
const filledCount = computed(() => job.value?.filledCount ?? job.value?.onboardCount ?? 0)

const nextStep = computed(() => {
  if (!job.value) return null
  if (sourcedCount.value > 0) {
    return {
      text: `有 ${sourcedCount.value} 位新人待你筛选`,
      action: () => { activeTab.value = 'candidates'; candidatesSub.value = 'kanban' },
    }
  }
  if (job.value.status === 'ACTIVE' && !boardItems.value.length) {
    return {
      text: '开启平台招人，开始为本职位找候选人',
      action: () => { activeTab.value = 'sourcing'; sourcingSub.value = 'task' },
    }
  }
  if (boardItems.value.length) {
    return {
      text: `本职位有 ${boardItems.value.length} 位在招候选人可跟进`,
      action: () => { activeTab.value = 'candidates'; candidatesSub.value = 'kanban' },
    }
  }
  return {
    text: '完善职位要求与招人方式',
    action: () => { activeTab.value = 'rules'; rulesSub.value = 'requirements' },
  }
})

const displayJdText = computed(() => {
  const text = String(job.value?.jdText || '').trim()
  if (!text) return '暂无职位描述，请在「规则」中完善任职要求'
  if (text.startsWith('[') || text.startsWith('{')) {
    try {
      JSON.parse(text)
      return '职位描述已保存，请在「规则 → 职位要求」查看结构化要求'
    } catch { /* plain text */ }
  }
  return text
})

const matchDistribution = computed(() => {
  const counts: Record<string, { count: number; tier: ReturnType<typeof parseMatchDetail>['status'] }> = {}
  for (const item of boardItems.value) {
    const v = parseMatchDetail(item.matchDetail, item.matchScore)
    if (!counts[v.label]) counts[v.label] = { count: 0, tier: v.status }
    counts[v.label].count++
  }
  return Object.entries(counts).map(([label, { count, tier }]) => ({
    label,
    count,
    type: tierTagType(tier),
  }))
})

const stageSummary = computed(() =>
  boardColumns.value.map(col => ({
    stage: col.stage,
    label: pipelineStageLabel(col.stage, 'column'),
    count: col.items?.length || 0,
  })).filter(s => s.count > 0 || ['SOURCED', 'INTERVIEWING', 'EVALUATED', 'OFFER'].includes(s.stage)),
)

function statusTagType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', ACTIVE: 'success', PAUSED: 'warning', CLOSED: 'info',
  }
  return map[status] || 'info'
}

function goInbox() {
  router.push({ path: '/workspace/inbox', query: { jobId: String(job.value?.id || '') } })
}

function goEditRequirements() {
  if (!job.value?.id) return
  router.push(`/planning/jobs/${job.value.id}/jd`)
}

function goEditSourcingMethod() {
  activeTab.value = 'rules'
  rulesSub.value = 'method'
}

async function onSourcingMethodConfirmed() {
  ElMessage.success('招人方式已更新，可前往「找人」开启平台招人任务')
}

function switchJob(id: number | null) {
  if (!id || id === job.value?.id) return
  router.push(`/planning/jobs/${id}`)
}

function onKanbanLoaded(payload: { total: number; sourced: number }) {
  sourcedCount.value = payload.sourced
}

async function onCandidateImported() {
  await loadBoardOnly()
  kanbanRef.value?.reload()
  try {
    await ElMessageBox.confirm(
      `已${ACTIONS.importCandidate}。下一步：在「新人待筛选」中做初筛（约 2 分钟）。`,
      '纳入成功',
      { confirmButtonText: ACTIONS.goScreen, cancelButtonText: '稍后', type: 'success' },
    )
    activeTab.value = 'candidates'
    candidatesSub.value = 'kanban'
  } catch {
    ElMessage.success(`已${ACTIONS.importCandidate}`)
  }
}

function normalizeTab(raw: string) {
  const map: Record<string, string> = {
    pipeline: 'candidates',
    requirements: 'rules',
    sourcing: 'sourcing',
    overview: 'overview',
    candidates: 'candidates',
    rules: 'rules',
  }
  return map[raw] || 'overview'
}

function parseTagsFromJobField(raw: unknown): RequirementItem[] {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.map((t: Record<string, unknown>) => fromApiTag(t))
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.map((t: Record<string, unknown>) => fromApiTag(t))
    } catch { /* ignore */ }
  }
  return []
}

async function loadCampaignStats(jobId: number) {
  try {
    const wfRes: any = await getWorkflowList({ jobId })
    const workflows = wfRes.data || []
    const active = workflows.find((w: any) => w.status === 'RUNNING' || w.status === 'PAUSED') || workflows[0]
    if (!active?.id) {
      campaignStats.value = null
      return
    }
    const detailRes: any = await getWorkflowDetail(active.id)
    campaignStats.value = detailRes?.data?.stats || active.stats || null
  } catch {
    campaignStats.value = null
  }
}

async function loadBoardOnly() {
  if (!job.value?.id) return
  const boardRes: any = await getPipelineBoard(job.value.id)
  boardColumns.value = boardRes.data?.columns || []
  boardItems.value = boardColumns.value.flatMap((c: any) =>
    (c.items || []).map((item: any) => ({
      ...item,
      pipelineStage: item.pipelineStage || c.stage,
    })),
  )
  const sourcedCol = boardColumns.value.find((c: any) => c.stage === 'SOURCED')
  sourcedCount.value = sourcedCol?.items?.length || 0
}

async function loadJob(jobId: number) {
  loading.value = true
  try {
    const { data } = await getJobDetail(jobId)
    job.value = data

    try {
      const tagRes: any = await getTags(jobId)
      const raw = tagRes.data ?? tagRes
      const list = Array.isArray(raw) ? raw : []
      requirements.value = list.map((t: Record<string, unknown>) => fromApiTag(t))
    } catch {
      requirements.value = []
    }
    if (!requirements.value.length) {
      requirements.value = parseTagsFromJobField(data?.tags)
    }

    await loadBoardOnly()
    await loadCampaignStats(jobId)
  } finally {
    loading.value = false
  }
}

async function handleActivate() {
  if (!job.value?.id) return
  await activateJob(job.value.id)
  ElMessage.success('已开始招聘')
  await loadJob(job.value.id)
}

async function handlePause() {
  if (!job.value?.id) return
  await pauseJob(job.value.id)
  ElMessage.success('已暂停招聘')
  await loadJob(job.value.id)
}

function openCloseDialog() {
  closeReason.value = ''
  closeDialogVisible.value = true
}

async function confirmClose() {
  if (!job.value?.id || !closeReason.value.trim()) {
    ElMessage.warning('请填写关闭原因')
    return
  }
  await closeJob(job.value.id, closeReason.value.trim())
  ElMessage.success('职位已关闭')
  closeDialogVisible.value = false
  await loadJob(job.value.id)
}

watch(
  () => route.params.id,
  (id) => { if (id) loadJob(Number(id)) },
)

onMounted(() => {
  if (route.query.tab && typeof route.query.tab === 'string') {
    activeTab.value = normalizeTab(route.query.tab)
    if (activeTab.value === 'candidates') candidatesSub.value = 'kanban'
    if (activeTab.value === 'rules') {
      rulesSub.value = route.query.sub === 'method' ? 'method' : 'requirements'
    }
  }
  loadJob(Number(route.params.id))
})
</script>

<style scoped lang="scss">
.header-main { flex: 1; min-width: 0; }
.title-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.header-actions { display: flex; gap: 8px; flex-shrink: 0; }

.next-step-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  margin-bottom: 16px;
  background: linear-gradient(90deg, #eff6ff 0%, #f8fafc 100%);
  border-color: #bfdbfe;
}
.next-step-text { font-size: 14px; color: #334155; }
.next-label { font-weight: 700; color: #1d4ed8; margin-right: 8px; }

.sub-seg { margin-bottom: 16px; }

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  @media (max-width: 960px) { grid-template-columns: 1fr; }
}

.funnel-card { padding: 16px; }
.funnel-row { display: flex; flex-wrap: wrap; gap: 12px; margin: 12px 0; }
.funnel-cell {
  min-width: 88px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  text-align: center;
}
.funnel-num { display: block; font-size: 20px; font-weight: 700; color: #0f172a; }
.funnel-label { font-size: 11px; color: #64748b; }

.data-card, .content-card { padding: 16px; }
.jd-snippet { white-space: pre-wrap; line-height: 1.7; color: #334155; margin: 0; }
.hint { color: #64748b; font-size: 12px; margin-bottom: 8px; }
.dist-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; font-size: 13px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.ml-8 { margin-left: 8px; }
</style>
