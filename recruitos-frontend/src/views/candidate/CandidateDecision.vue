<template>
  <div class="page-container">
    <div v-if="!ready" class="data-card picker-card mb-12">
      <h3 class="picker-title">选择候选人与在招职位</h3>
      <p class="picker-desc">匹配评估需同时指定在招职位与候选人，系统将给出匹配建议、符合点与待确认项。</p>
      <div class="picker-row">
        <span class="picker-label">在招职位</span>
        <el-select
          v-model="pickerJobId"
          placeholder="选择在招职位"
          filterable
          style="width: 300px"
          @change="onPickJobOnly"
        >
          <el-option v-for="j in jobOptions" :key="j.id" :label="j.title" :value="j.id" />
        </el-select>
      </div>
      <div class="picker-row">
        <span class="picker-label">候选人</span>
        <el-select
          v-model="pickerCandidateId"
          placeholder="先选职位，再选候选人"
          filterable
          remote
          :remote-method="searchCandidates"
          :loading="candidateSearchLoading"
          :disabled="!pickerJobId"
          style="width: 300px"
          @change="onPickCandidate"
        >
          <el-option
            v-for="c in candidateOptions"
            :key="c.id"
            :label="`${c.name} · ${c.currentCompany || '-'}`"
            :value="c.id"
          />
        </el-select>
      </div>
      <el-button type="primary" :disabled="!pickerJobId || !pickerCandidateId" @click="applyPicker">
        查看匹配评估
      </el-button>
    </div>

    <!-- 页面头部 -->
    <div v-if="ready" class="page-header">
      <h2 class="page-title">候选人匹配评估</h2>
      <div class="header-actions">
        <el-button type="success" @click="handlePass">
          <el-icon><Select /></el-icon>
          进入下一轮
        </el-button>
        <el-button type="danger" @click="handleReject">
          <el-icon><CloseBold /></el-icon>
          标记不合适
        </el-button>
        <el-button type="warning" @click="handleReserve">
          <el-icon><FolderOpened /></el-icon>
          储备至人才库
        </el-button>
      </div>
    </div>

    <!-- 候选人信息栏 -->
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

    <!-- 主内容区 -->
    <div v-if="ready" class="decision-body">
      <!-- 左侧: 雷达图 -->
      <div class="decision-left">
        <el-card shadow="never" class="radar-card">
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
              <el-progress
                :percentage="dim.value"
                :stroke-width="6"
                :color="radarColors[index]"
                :show-text="false"
                style="flex: 1"
              />
              <span class="dim-value" :style="{ color: radarColors[index] }">{{ dim.value }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧: 标签匹配详情 -->
      <div class="decision-right">
        <el-card shadow="never" class="tags-card">
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
              <el-tag
                size="small"
                :type="row.judgment === '符合' ? 'success' : row.judgment === '待确认' ? 'warning' : 'danger'"
              >
                {{ row.judgment }}
              </el-tag>
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
        </el-card>
      </div>
    </div>

    <!-- AI 洞察卡片 -->
    <el-card v-if="ready" shadow="never" class="ai-insight-card">
      <div class="insight-header">
        <div class="insight-icon">
          <el-icon :size="20"><MagicStick /></el-icon>
        </div>
        <span class="insight-title">匹配说明</span>
        <el-tag size="small" type="info" effect="plain">系统建议</el-tag>
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
    </el-card>

    <el-dialog v-model="rejectVisible" title="标记不合适" width="480px">
      <p class="reject-hint">该候选人将结束在本职位的招聘流程。储备至人才库是独立操作，不会自动执行。</p>
      <el-checkbox v-model="alsoReserveOnReject">同时储备至人才库（长期跟进）</el-checkbox>
      <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="说明不合适原因（选填）" class="mt-12" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认不合适</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Select, CloseBold, FolderOpened, MagicStick } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
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

// 候选人信息
const candidateInfo = reactive({
  id: 0,
  name: '',
  company: '',
  position: '',
  workYears: 0,
  education: '',
})

// 匹配结论
const overallScore = ref(0)
const matchDetailJson = ref<string | null>(null)
const candidateJobId = ref(0)
const currentPipelineStage = ref('SOURCED')
const rejectVisible = ref(false)
const alsoReserveOnReject = ref(false)
const rejectComment = ref('')
// 雷达图
const radarChartRef = ref<HTMLElement>()
let radarChart: echarts.ECharts | null = null

const radarColors = ['#3B82F6', '#059669', '#D97706', '#DC2626', '#64748B']

const radarDimensions = ref<{ name: string; value: number }[]>([])

function initRadarChart() {
  if (!radarChartRef.value || !radarDimensions.value.length) return

  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
  radarChart = echarts.init(radarChartRef.value)

  const option: echarts.EChartsOption = {
    radar: {
      indicator: radarDimensions.value.map(dim => ({
        name: dim.name,
        max: 100,
      })),
      shape: 'polygon',
      splitNumber: 5,
      axisName: {
        color: '#334155',
        fontSize: 13,
        fontWeight: 500,
      },
      splitLine: {
        lineStyle: {
          color: '#F1F5F9',
        },
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(64, 158, 255, 0.02)', 'rgba(64, 158, 255, 0.04)'],
        },
      },
      axisLine: {
        lineStyle: {
          color: '#DCDFE6',
        },
      },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: radarDimensions.value.map(dim => dim.value),
            name: '候选人评分',
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                { offset: 1, color: 'rgba(64, 158, 255, 0.05)' },
              ]),
            },
            lineStyle: {
              color: '#3B82F6',
              width: 2,
            },
            itemStyle: {
              color: '#3B82F6',
              borderColor: '#fff',
              borderWidth: 2,
            },
            symbol: 'circle',
            symbolSize: 8,
          },
        ],
      },
    ],
    tooltip: {
      trigger: 'item',
    },
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

// 选择器
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

function onPickCandidate() {
  /* 等待用户点击加载 */
}

async function searchCandidates(keyword: string) {
  if (!pickerJobId.value) return
  candidateSearchLoading.value = true
  try {
    const res: any = await getCandidateList({
      jobId: pickerJobId.value,
      name: keyword || undefined,
      pageNum: 1,
      pageSize: 50,
    })
    candidateOptions.value = res.data?.list || res.data?.records || []
  } finally {
    candidateSearchLoading.value = false
  }
}

async function applyPicker() {
  if (!pickerJobId.value || !pickerCandidateId.value) return
  await router.replace({
    query: {
      candidateId: String(pickerCandidateId.value),
      jobId: String(pickerJobId.value),
    },
  })
  await loadDecisionData()
  await nextTick()
  if (radarDimensions.value.length > 0) initRadarChart()
}

async function handlePass() {
  try {
    await ElMessageBox.confirm('确认让该候选人进入下一轮招聘流程？', ACTIONS.nextRound, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'success',
    })
    const toStage = nextStageAfterPass(currentPipelineStage.value)
    if (candidateJobId.value) {
      await advancePipelineStage(candidateJobId.value, { toStage })
    }
    await screening(candidateId.value, jobId.value, {
      screeningStatus: 'PASSED',
      screenerComment: '通过初筛，进入下一轮',
    })
    ElMessage.success('已进入下一轮')
    await loadDecisionData()
  } catch {
    // 取消操作
  }
}

function handleReject() {
  alsoReserveOnReject.value = false
  rejectComment.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  try {
    if (candidateJobId.value) {
      await advancePipelineStage(candidateJobId.value, {
        toStage: 'ARCHIVED',
        reasonCode: 'NOT_FIT',
        comment: rejectComment.value || '本职位不合适',
        archivedToPool: alsoReserveOnReject.value,
      })
    }
    await screening(candidateId.value, jobId.value, {
      screeningStatus: 'REJECTED',
      screenerComment: rejectComment.value,
    })
    if (alsoReserveOnReject.value) {
      await screening(candidateId.value, jobId.value, { screeningStatus: 'RESERVE' })
    }
    ElMessage.success(alsoReserveOnReject.value ? '已标记不合适并储备至人才库' : '已标记不合适')
    rejectVisible.value = false
    await loadDecisionData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleReserve() {
  try {
    await ElMessageBox.confirm(
      `储备至${OBJECTS.talentPool}不会结束本职位流程，仅便于长期跟进。确定储备吗？`,
      ACTIONS.reserveToPool,
      { confirmButtonText: '确认储备', cancelButtonText: '取消', type: 'info' },
    )
    await screening(candidateId.value, jobId.value, { screeningStatus: 'RESERVE' })
    ElMessage.success('已储备至人才库')
  } catch {
    // 取消操作
  }
}

// 加载匹配评估数据
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
    ElMessage.error('加载匹配评估失败')
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
  color: #fff;

  .banner-left {
    display: flex;
    align-items: center;

    .candidate-avatar {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.15);
      backdrop-filter: blur(10px);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 26px;
      font-weight: 700;
      margin-right: 20px;
      border: 2px solid rgba(255, 255, 255, 0.2);
    }

    .candidate-meta {
      .candidate-name {
        font-size: 24px;
        font-weight: 700;
        margin: 0 0 6px;
      }

      .candidate-detail {
        font-size: 14px;
        opacity: 0.8;

        .divider {
          margin: 0 10px;
          opacity: 0.4;
        }
      }
    }
  }

  .banner-right.verdict-panel {
    background: rgba(255, 255, 255, 0.95);
    border-radius: 10px;
    padding: 12px 16px;
    max-width: 420px;
    color: #1e293b;
  }

  .banner-right {
    display: flex;
    align-items: center;
    gap: 20px;

    .overall-score {
      text-align: center;

      .score-number {
        font-size: 52px;
        font-weight: 800;
        line-height: 1;
        background: linear-gradient(135deg, $success-color, #95D475);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .score-label {
        font-size: 13px;
        opacity: 0.7;
        margin-top: 4px;
      }
    }

    .score-trend {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 13px;

      &.trend-up {
        background: rgba(103, 194, 58, 0.15);
        color: #95D475;
      }

      &.trend-down {
        background: rgba(245, 108, 108, 0.15);
        color: #F89898;
      }

      &.trend-stable {
        background: rgba(144, 147, 153, 0.15);
        color: #B0B4BB;
      }

      .trend-arrow {
        font-size: 16px;
        font-weight: 700;
      }
    }
  }
}

.decision-body {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;

  .decision-left,
  .decision-right {
    flex: 1;
    min-width: 0;
  }
}

.radar-card,
.tags-card {
  height: 100%;

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid $border-color-light;
  }

  :deep(.el-card__body) {
    padding: 20px;
  }
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 10px;

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }

  .card-subtitle {
    font-size: 12px;
    color: $text-placeholder;
  }
}

.radar-chart {
  width: 100%;
  height: 320px;
}

.radar-scores {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  .radar-score-item {
    display: flex;
    align-items: center;
    gap: 12px;

    .dim-name {
      width: 70px;
      font-size: 13px;
      color: $text-regular;
      text-align: right;
      flex-shrink: 0;
    }

    .dim-value {
      width: 36px;
      text-align: right;
      font-size: 14px;
      font-weight: 700;
      flex-shrink: 0;
    }
  }
}

.req-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.req-header, .req-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
}
.req-header {
  font-size: 12px;
  color: $text-placeholder;
  font-weight: 600;
}
.req-row {
  background: #fafbfc;
}
.req-name { font-size: 13px; font-weight: 500; }
.req-hint { grid-column: 1 / -1; font-size: 12px; color: $text-secondary; }
.reject-hint { margin: 0 0 12px; color: #64748b; font-size: 13px; }
.mt-12 { margin-top: 12px; }

.ai-insight-card {
  :deep(.el-card__body) {
    padding: 20px 24px;
  }

  .insight-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;

    .insight-icon {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      background: linear-gradient(135deg, $primary-color, $primary-light);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .insight-title {
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
    }
  }

  .insight-body {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .insight-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 16px;
      border-radius: 8px;
      background: #f8f9fb;

      .insight-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-top: 6px;
        flex-shrink: 0;

        &.highlight {
          background: $success-color;
          box-shadow: 0 0 6px rgba(103, 194, 58, 0.4);
        }

        &.warning {
          background: $warning-color;
          box-shadow: 0 0 6px rgba(230, 162, 60, 0.4);
        }

        &.info {
          background: $primary-color;
          box-shadow: 0 0 6px rgba(64, 158, 255, 0.4);
        }
      }

      .insight-text {
        font-size: 14px;
        color: $text-regular;
        line-height: 1.6;
        margin: 0;
      }
    }
  }
}

.header-actions {
  display: flex;
  gap: 8px;
}

// 响应式适配
@media (max-width: 1200px) {
  .decision-body {
    flex-direction: column;
  }

  .candidate-banner {
    flex-direction: column;
    gap: 20px;
    align-items: flex-start;

    .banner-right {
      align-self: flex-end;
    }
  }
}
</style>
