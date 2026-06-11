<template>
  <div class="page-container page-stack">
    <div v-if="!ready" class="data-card picker-card mb-12">
      <h3 class="picker-title">选择候选人与在招职位</h3>
      <p class="picker-desc">匹配评估需同时指定在招职位与候选人，系统将给出匹配建议、符合点与待确认项。</p>
      <div class="picker-row">
        <span class="picker-label">在招职位</span>
        <RSelect
          v-model="pickerJobId"
          placeholder="选择在招职位"
          :options="jobOptions.map(j => ({ label: j.title, value: j.id }))"
          class="w-[300px]"
          @change="onPickJobOnly"
        />
      </div>
      <div class="picker-row">
        <span class="picker-label">候选人</span>
        <RSelect
          v-model="pickerCandidateId"
          placeholder="先选职位，再选候选人"
          :options="candidateOptions.map(c => ({ label: `${c.name} · ${c.currentCompany || '-'}`, value: c.id }))"
          :disabled="!pickerJobId"
          :loading="candidateSearchLoading"
          filterable
          remote
          class="w-[300px]"
          @remote-method="searchCandidates"
          @change="onPickCandidate"
        />
      </div>
      <RButton :disabled="!pickerJobId || !pickerCandidateId" @click="applyPicker">
        查看匹配评估
      </RButton>
    </div>

    <div v-if="ready" class="page-header">
      <h2 class="page-title">候选人匹配评估</h2>
      <div class="header-actions">
        <RButton variant="primary" @click="handlePass">
          <Check class="h-4 w-4" />
          进入下一轮
        </RButton>
        <RButton variant="danger" @click="handleReject">
          <X class="h-4 w-4" />
          标记不合适
        </RButton>
        <RButton variant="outline" @click="handleReserve">
          <FolderOpen class="h-4 w-4" />
          储备至人才库
        </RButton>
        <RButton variant="outline" @click="openAiAnalysis">
          <Sparkles class="h-4 w-4 text-primary" />
          AI 深度分析
        </RButton>
      </div>
    </div>

    <div v-if="ready" class="candidate-banner">
      <div class="banner-left">
        <div class="candidate-avatar">{{ candidateInfo.name?.charAt(0) || '?' }}</div>
        <div class="candidate-meta">
          <h2 class="candidate-name">{{ candidateInfo.name }}</h2>
          <div class="candidate-detail">
            <span>{{ candidateInfo.company }}</span>
            <span class="divider">|</span>
            <span>{{ candidateInfo.position }}</span>
            <span class="divider">|</span>
            <span>{{ candidateInfo.workYears }}年经验</span>
            <span class="divider">|</span>
            <span>{{ candidateInfo.education }}</span>
          </div>
        </div>
      </div>
      <div class="banner-right verdict-panel">
        <MatchVerdict
          :match-score="overallScore"
          :match-detail="matchDetailJson"
          mode="card"
          :show-score="false"
        />
      </div>
    </div>

    <div v-if="ready" class="decision-body">
      <div class="decision-left">
        <RCard class="radar-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">能力画像</span>
              <span class="card-subtitle">辅助了解候选人与职位要求的差距</span>
            </div>
          </template>
          <div v-if="radarDimensions.length" ref="radarChartRef" class="radar-chart" />
          <EmptyStateCta
            v-else
            description="暂无能力画像数据，请先完成简历解析"
            :image-size="64"
            :actions="[
              { label: '返回候选人详情', type: 'primary', onClick: () => router.push(`/pipeline/candidates/${candidateId}`) },
            ]"
          />
          <div v-if="radarDimensions.length" class="radar-scores">
            <div
              v-for="(dim, index) in radarDimensions"
              :key="dim.name"
              class="radar-score-item"
            >
              <span class="dim-name">{{ dim.name }}</span>
              <RProgress :value="dim.value" :color="radarColors[index]" :show-text="false" class="flex-1" />
              <span class="dim-value" :style="{ color: radarColors[index] }">{{ dim.value }}</span>
            </div>
          </div>
        </RCard>
      </div>

      <div class="decision-right">
        <RCard class="tags-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">任职要求对照</span>
              <span class="card-subtitle">岗位要求与候选人情况的逐项判断</span>
            </div>
          </template>
          <div v-if="requirementRows.length" class="req-table">
            <div class="req-header">
              <span>任职要求</span>
              <span>判断</span>
            </div>
            <div v-for="row in requirementRows" :key="row.name" class="req-row">
              <span class="req-name">{{ row.name }}</span>
              <RBadge :variant="row.judgment === '符合' ? 'success' : row.judgment === '待确认' ? 'warning' : 'danger'" size="sm">
                {{ row.judgment }}
              </RBadge>
              <span v-if="row.hint" class="req-hint">{{ row.hint }}</span>
            </div>
          </div>
          <EmptyStateCta
            v-else
            description="暂无任职要求对照，请先完善职位描述并提取任职要求"
            :image-size="64"
            :actions="jobId ? [
              { label: '编辑任职要求', type: 'primary', onClick: () => router.push(`/planning/jobs/${jobId}/jd`) },
            ] : []"
          />
        </RCard>
      </div>
    </div>

    <RCard v-if="ready" class="ai-insight-card">
      <div class="insight-header">
        <div class="insight-icon">
          <Sparkles class="h-5 w-5" />
        </div>
        <span class="insight-title">匹配说明</span>
        <RBadge variant="info" size="sm">系统建议</RBadge>
      </div>
      <div class="insight-body">
        <div
          v-for="(insight, index) in aiInsights"
          :key="index"
          class="insight-item"
        >
          <div class="insight-dot" :class="insight.type"></div>
          <p class="insight-text">{{ insight.text }}</p>
        </div>
      </div>
    </RCard>

    <RDialog v-model="rejectVisible" title="标记不合适" width="480px">
      <p class="reject-hint">该候选人将结束在本职位的招聘流程。储备至人才库是独立操作，不会自动执行。</p>
      <RCheckbox v-model="alsoReserveOnReject">同时储备至人才库（长期跟进）</RCheckbox>
      <RInput
        v-model="rejectComment"
        type="textarea"
        placeholder="说明不合适原因（选填）"
        class="mt-12"
      />
      <template #footer>
        <RButton variant="outline" @click="rejectVisible = false">取消</RButton>
        <RButton variant="danger" @click="confirmReject">确认不合适</RButton>
      </template>
    </RDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, X, FolderOpen, Sparkles } from 'lucide-vue-next'
import * as echarts from 'echarts'
import { RButton, RBadge, RCard, RDialog, RInput, RCheckbox, RProgress, RSelect } from '@/components/ui'
import { toast } from '@/lib/toast'
import { confirm } from '@/lib/confirm'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { OBJECTS, ACTIONS } from '@/constants/businessLabels'
import {
  parseMatchDetail,
  extractRadarDimensions,
  extractRequirementRows,
  extractAiInsights,
  type AiInsightRow,
} from '@/utils/matchVerdict'
import { getDecisionPanel, screening, getCandidateList } from '@/api/modules/candidate'
import { advancePipelineStage } from '@/api/modules/pipeline'
import { getJobList } from '@/api/modules/job'

const route = useRoute()
const router = useRouter()

const candidateInfo = reactive({
  id: 0,
  name: '',
  company: '',
  position: '',
  workYears: 0,
  education: '',
})

const overallScore = ref(0)
const matchDetailJson = ref<string | null>(null)
const candidateJobId = ref(0)
const currentPipelineStage = ref('SOURCED')
const rejectVisible = ref(false)
const alsoReserveOnReject = ref(false)
const rejectComment = ref('')
const radarChartRef = ref<HTMLElement>()
let radarChart: echarts.ECharts | null = null

const radarColors = ['#3B82F6', '#059669', '#D97706', '#DC2626', '#64748B']
const radarDimensions = ref<{ name: string; value: number }[]>([])

function initRadarChart() {
  if (!radarChartRef.value || !radarDimensions.value.length) return
  if (radarChart) { radarChart.dispose(); radarChart = null }
  radarChart = echarts.init(radarChartRef.value)
  const option: echarts.EChartsOption = {
    radar: {
      indicator: radarDimensions.value.map(dim => ({ name: dim.name, max: 100 })),
      shape: 'polygon',
      splitNumber: 5,
      axisName: { color: '#334155', fontSize: 13, fontWeight: 500 },
      splitLine: { lineStyle: { color: '#F1F5F9' } },
      splitArea: { show: true, areaStyle: { color: ['rgba(64, 158, 255, 0.02)', 'rgba(64, 158, 255, 0.04)'] } },
      axisLine: { lineStyle: { color: '#DCDFE6' } },
    },
    series: [{
      type: 'radar',
      data: [{
        value: radarDimensions.value.map(dim => dim.value),
        name: '候选人评分',
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64, 158, 255, 0.3)' }, { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }]) },
        lineStyle: { color: '#3B82F6', width: 2 },
        itemStyle: { color: '#3B82F6', borderColor: '#fff', borderWidth: 2 },
        symbol: 'circle',
        symbolSize: 8,
      }],
    }],
    tooltip: { trigger: 'item' },
  }
  radarChart.setOption(option)
}

const requirementRows = ref<ReturnType<typeof extractRequirementRows>>([])
const aiInsights = ref<AiInsightRow[]>([])

function applyVerdictPanels() {
  const verdict = parseMatchDetail(matchDetailJson.value, overallScore.value)
  radarDimensions.value = extractRadarDimensions(verdict)
  requirementRows.value = extractRequirementRows(verdict)
  aiInsights.value = extractAiInsights(verdict)
}

function nextStageAfterPass(stage: string): string {
  const flow = ['SOURCED', 'SCREENING', 'CONTACTED', 'INTERVIEWING', 'EVALUATED', 'OFFER', 'HIRED']
  const idx = flow.indexOf(stage)
  if (idx < 0 || idx >= flow.length - 1) return 'SCREENING'
  return flow[idx + 1]
}

const pickerJobId = ref<number | null>(null)
const pickerCandidateId = ref<number | null>(null)
const jobOptions = ref<any[]>([])
const candidateOptions = ref<any[]>([])
const candidateSearchLoading = ref(false)

const candidateId = computed(() => Number(route.query.candidateId) || 0)
const jobId = computed(() => Number(route.query.jobId) || 0)
const ready = computed(() => candidateId.value > 0 && jobId.value > 0)

function onPickJobOnly() {
  pickerCandidateId.value = null
  candidateOptions.value = []
  if (pickerJobId.value) searchCandidates('')
}

function onPickCandidate() {}

async function searchCandidates(keyword: string) {
  if (!pickerJobId.value) return
  candidateSearchLoading.value = true
  try {
    const res: any = await getCandidateList({ jobId: pickerJobId.value, name: keyword || undefined, pageNum: 1, pageSize: 50 })
    candidateOptions.value = res.data?.list || res.data?.records || []
  } finally {
    candidateSearchLoading.value = false
  }
}

async function applyPicker() {
  if (!pickerJobId.value || !pickerCandidateId.value) return
  await router.replace({ query: { candidateId: String(pickerCandidateId.value), jobId: String(pickerJobId.value) } })
  await loadDecisionData()
  await nextTick()
  if (radarDimensions.value.length > 0) initRadarChart()
}

async function handlePass() {
  const ok = await confirm('确认让该候选人进入下一轮招聘流程？', ACTIONS.nextRound, { type: 'danger' })
  if (!ok) return
  const toStage = nextStageAfterPass(currentPipelineStage.value)
  if (candidateJobId.value) await advancePipelineStage(candidateJobId.value, { toStage })
  await screening(candidateId.value, jobId.value, { screeningStatus: 'PASSED', screenerComment: '通过初筛，进入下一轮' })
  toast.success('已进入下一轮')
  await loadDecisionData()
}

function handleReject() {
  alsoReserveOnReject.value = false
  rejectComment.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  try {
    if (candidateJobId.value) {
      await advancePipelineStage(candidateJobId.value, { toStage: 'ARCHIVED', reasonCode: 'NOT_FIT', comment: rejectComment.value || '本职位不合适', archivedToPool: alsoReserveOnReject.value })
    }
    await screening(candidateId.value, jobId.value, { screeningStatus: 'REJECTED', screenerComment: rejectComment.value })
    if (alsoReserveOnReject.value) await screening(candidateId.value, jobId.value, { screeningStatus: 'RESERVE' })
    toast.success(alsoReserveOnReject.value ? '已标记不合适并储备至人才库' : '已标记不合适')
    rejectVisible.value = false
    await loadDecisionData()
  } catch {
    toast.error('操作失败')
  }
}

function openAiAnalysis() {
  if (candidateInfo.id && pickerJobId.value) {
    router.push(`/ai/intent/${candidateInfo.id}`)
  }
}

async function handleReserve() {
  const ok = await confirm(`储备至${OBJECTS.talentPool}不会结束本职位流程，仅便于长期跟进。确定储备吗？`, ACTIONS.reserveToPool)
  if (!ok) return
  await screening(candidateId.value, jobId.value, { screeningStatus: 'RESERVE' })
  toast.success('已储备至人才库')
}

async function loadDecisionData() {
  if (!candidateId.value || !jobId.value) return
  try {
    const res: any = await getDecisionPanel(candidateId.value, jobId.value)
    const data = res.data || {}
    candidateInfo.id = data.candidateId || candidateId.value
    candidateInfo.name = data.candidateName || ''
    candidateInfo.company = data.candidateCompany || ''
    candidateInfo.position = data.candidateTitle || ''
    overallScore.value = data.matchScore != null ? Number(data.matchScore) : 0
    matchDetailJson.value = data.matchDetail || null
    candidateJobId.value = data.id || 0
    currentPipelineStage.value = data.pipelineStage || 'SOURCED'
    applyVerdictPanels()
  } catch {
    toast.error('加载匹配评估失败')
  }
}

watch(() => [route.query.candidateId, route.query.jobId], async () => {
  if (!ready.value) return
  await loadDecisionData()
  await nextTick()
  if (radarDimensions.value.length > 0) initRadarChart()
}, { immediate: false })

onMounted(async () => {
  const res: any = await getJobList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' })
  jobOptions.value = res.data?.list || res.data?.records || []
  if (route.query.jobId) pickerJobId.value = Number(route.query.jobId)
  if (route.query.candidateId) pickerCandidateId.value = Number(route.query.candidateId)
  if (pickerJobId.value) await searchCandidates('')
  if (ready.value) {
    await loadDecisionData()
    await nextTick()
    if (radarDimensions.value.length > 0) initRadarChart()
  }
  window.addEventListener('resize', () => radarChart?.resize())
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.mb-12 { margin-bottom: 12px; }
.mt-12 { margin-top: 12px; }
.w-\[300px\] { width: 300px; }
.picker-card { padding: 20px; }
.picker-title { margin: 0 0 8px; font-size: 16px; }
.picker-desc { margin: 0 0 16px; color: #64748b; font-size: 13px; }
.picker-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.picker-label { width: 56px; font-size: 13px; color: #64748b; flex-shrink: 0; }

.candidate-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 20px;
  color: white;

  .banner-left { display: flex; align-items: center; }
  .candidate-avatar {
    width: 64px; height: 64px; border-radius: 50%;
    background: rgba(255,255,255,0.15); backdrop-filter: blur(10px);
    display: flex; align-items: center; justify-content: center;
    font-size: 26px; font-weight: 700; margin-right: 20px;
    border: 2px solid rgba(255,255,255,0.2);
  }
  .candidate-name { font-size: 24px; font-weight: 700; margin: 0 0 6px; }
  .candidate-detail { font-size: 14px; opacity: 0.8; }
  .divider { margin: 0 10px; opacity: 0.4; }
  .verdict-panel {
    background: rgba(255,255,255,0.95); border-radius: 10px;
    padding: 12px 16px; max-width: 420px; color: #1e293b;
  }
}

.decision-body { display: flex; gap: 20px; margin-bottom: 20px; }
.decision-left, .decision-right { flex: 1; min-width: 0; }

.card-header {
  display: flex; align-items: baseline; gap: 10px;
  .card-title { font-size: 16px; font-weight: 600; color: var(--r-text-primary); }
  .card-subtitle { font-size: 12px; color: var(--r-text-placeholder); }
}

.radar-chart { width: 100%; height: 320px; }
.radar-scores { margin-top: 16px; display: flex; flex-direction: column; gap: 10px; }
.radar-score-item { display: flex; align-items: center; gap: 12px; }
.dim-name { width: 70px; font-size: 13px; color: var(--r-text-secondary); text-align: right; flex-shrink: 0; }
.dim-value { width: 36px; text-align: right; font-size: 14px; font-weight: 700; flex-shrink: 0; }

.req-table { display: flex; flex-direction: column; gap: 8px; }
.req-header, .req-row { display: grid; grid-template-columns: 1fr auto; gap: 12px; align-items: center; padding: 8px 12px; border-radius: 6px; }
.req-header { font-size: 12px; color: var(--r-text-placeholder); font-weight: 600; }
.req-row { background: var(--r-bg-page); }
.req-name { font-size: 13px; font-weight: 500; }
.req-hint { grid-column: 1 / -1; font-size: 12px; color: var(--r-text-secondary); }

.reject-hint { margin: 0 0 12px; color: #64748b; font-size: 13px; }

.ai-insight-card {
  .insight-header { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
  .insight-icon {
    width: 36px; height: 36px; border-radius: 8px;
    background: linear-gradient(135deg, var(--r-color-primary), #818cf8);
    display: flex; align-items: center; justify-content: center; color: white;
  }
  .insight-title { font-size: 16px; font-weight: 600; color: var(--r-text-primary); }
  .insight-body { display: flex; flex-direction: column; gap: 12px; }
  .insight-item { display: flex; align-items: flex-start; gap: 12px; padding: 12px 16px; border-radius: 8px; background: var(--r-bg-muted); }
  .insight-dot {
    width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0;
    &.highlight { background: #059669; box-shadow: 0 0 6px rgba(103,194,58,0.4); }
    &.warning { background: #D97706; box-shadow: 0 0 6px rgba(230,162,60,0.4); }
    &.info { background: #3B82F6; box-shadow: 0 0 6px rgba(64,158,255,0.4); }
  }
  .insight-text { font-size: 14px; color: var(--r-text-secondary); line-height: 1.6; margin: 0; }
}

.header-actions { display: flex; gap: 8px; }

@media (max-width: 1200px) {
  .decision-body { flex-direction: column; }
  .candidate-banner { flex-direction: column; gap: 20px; align-items: flex-start; }
}
</style>
