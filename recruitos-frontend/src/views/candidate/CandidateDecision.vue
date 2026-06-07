<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">决策面板</h2>
      <div class="header-actions">
        <el-button type="success" @click="handlePass">
          <el-icon><Select /></el-icon>
          通过筛选
        </el-button>
        <el-button type="danger" @click="handleReject">
          <el-icon><CloseBold /></el-icon>
          淘汰
        </el-button>
        <el-button type="warning" @click="handleReserve">
          <el-icon><FolderOpened /></el-icon>
          放入储备
        </el-button>
        <el-button type="primary" @click="handleComment">
          <el-icon><EditPen /></el-icon>
          添加评价
        </el-button>
      </div>
    </div>

    <!-- 候选人信息栏 -->
    <div class="candidate-banner">
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
      <div class="banner-right">
        <div class="overall-score">
          <div class="score-number">{{ overallScore }}</div>
          <div class="score-label">综合匹配度</div>
        </div>
        <div class="score-trend" :class="scoreTrendClass">
          <span class="trend-arrow">{{ scoreTrendArrow }}</span>
          <span class="trend-text">{{ scoreTrendText }}</span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="decision-body">
      <!-- 左侧: 雷达图 -->
      <div class="decision-left">
        <el-card shadow="never" class="radar-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">能力雷达图</span>
              <span class="card-subtitle">五维评估模型</span>
            </div>
          </template>
          <div ref="radarChartRef" class="radar-chart"></div>
          <div class="radar-scores">
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
              <span class="card-title">标签匹配详情</span>
              <span class="card-subtitle">权重 / 协比 / 趋势</span>
            </div>
          </template>
          <div class="tag-rows">
            <div
              v-for="tag in tagMatches"
              :key="tag.name"
              class="tag-row"
            >
              <span class="tag-name">{{ tag.name }}</span>
              <el-progress
                :percentage="tag.score"
                :stroke-width="8"
                :color="getTagProgressColor(tag.score)"
                style="flex: 1; min-width: 120px"
              />
              <span class="tag-weight">权重: {{ tag.weight.toFixed(2) }}</span>
              <span class="tag-corr">协比: {{ tag.correlation.toFixed(2) }}</span>
              <span
                class="tag-trend"
                :class="getTrendClass(tag.trend)"
              >
                {{ getTrendArrow(tag.trend) }}
              </span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- AI 洞察卡片 -->
    <el-card shadow="never" class="ai-insight-card">
      <div class="insight-header">
        <div class="insight-icon">
          <el-icon :size="20"><MagicStick /></el-icon>
        </div>
        <span class="insight-title">AI 洞察</span>
        <el-tag size="small" type="info" effect="plain">智能分析</el-tag>
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

    <!-- 添加评价对话框 -->
    <el-dialog v-model="commentVisible" title="添加评价" width="520px">
      <el-form label-width="80px">
        <el-form-item label="评价维度">
          <el-select v-model="commentForm.dimension" placeholder="选择评价维度" style="width: 100%">
            <el-option label="技术能力" value="tech" />
            <el-option label="沟通表达" value="communication" />
            <el-option label="文化匹配" value="culture" />
            <el-option label="综合评价" value="overall" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="commentForm.rating" :max="5" show-score />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="commentForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入详细评价..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commentVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComment">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Select, CloseBold, FolderOpened, EditPen, MagicStick } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDecisionPanel, screening } from '@/api/modules/candidate'

const route = useRoute()

// 候选人信息
const candidateInfo = reactive({
  id: 0,
  name: '',
  company: '',
  position: '',
  workYears: 0,
  education: '',
})

// 综合匹配度
const overallScore = ref(0)
const scoreTrend = ref<'up' | 'down' | 'stable'>('stable')

const scoreTrendClass = computed(() => ({
  'trend-up': scoreTrend.value === 'up',
  'trend-down': scoreTrend.value === 'down',
  'trend-stable': scoreTrend.value === 'stable',
}))

const scoreTrendArrow = computed(() => {
  if (scoreTrend.value === 'up') return '↑'
  if (scoreTrend.value === 'down') return '↓'
  return '→'
})

const scoreTrendText = computed(() => {
  if (scoreTrend.value === 'up') return '较上轮提升'
  if (scoreTrend.value === 'down') return '较上轮下降'
  return '持平'
})

// 雷达图
const radarChartRef = ref<HTMLElement>()
let radarChart: echarts.ECharts | null = null

const radarColors = ['#3B82F6', '#059669', '#D97706', '#DC2626', '#64748B']

const radarDimensions = ref<{ name: string; value: number }[]>([])

function initRadarChart() {
  if (!radarChartRef.value) return

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

// 标签匹配详情
interface TagMatch {
  name: string
  score: number
  weight: number
  correlation: number
  trend: 'up' | 'down' | 'stable'
}

const tagMatches = ref<TagMatch[]>([])

function getTagProgressColor(score: number): string {
  if (score >= 90) return '#059669'
  if (score >= 80) return '#3B82F6'
  if (score >= 70) return '#D97706'
  return '#DC2626'
}

function getTrendClass(trend: string): string {
  if (trend === 'up') return 'text-success'
  if (trend === 'down') return 'text-danger'
  return 'text-info'
}

function getTrendArrow(trend: string): string {
  if (trend === 'up') return '↑'
  if (trend === 'down') return '↓'
  return '→'
}

// AI 洞察
interface AiInsight {
  text: string
  type: 'highlight' | 'warning' | 'info'
}

const aiInsights = ref<AiInsight[]>([])

// 添加评价
const commentVisible = ref(false)
const commentForm = reactive({
  dimension: '',
  rating: 4,
  content: '',
})

function handleComment() {
  commentForm.dimension = ''
  commentForm.rating = 4
  commentForm.content = ''
  commentVisible.value = true
}

function submitComment() {
  if (!commentForm.content) {
    ElMessage.warning('请输入评价内容')
    return
  }
  ElMessage.success('评价已提交')
  commentVisible.value = false
}

// 操作按钮
const candidateId = computed(() => Number(route.query.candidateId) || 0)
const jobId = computed(() => Number(route.query.jobId) || 0)

async function handlePass() {
  try {
    await ElMessageBox.confirm('确定通过该候选人的筛选吗？', '通过筛选', {
      confirmButtonText: '确定通过',
      cancelButtonText: '取消',
      type: 'success',
    })
    await screening(candidateId.value, jobId.value, { decision: 'PASS' })
    ElMessage.success('已通过筛选')
  } catch {
    // 取消操作
  }
}

async function handleReject() {
  try {
    await ElMessageBox.confirm('确定淘汰该候选人吗？此操作不可撤销。', '淘汰候选人', {
      confirmButtonText: '确定淘汰',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await screening(candidateId.value, jobId.value, { decision: 'REJECT' })
    ElMessage.success('已淘汰')
  } catch {
    // 取消操作
  }
}

async function handleReserve() {
  try {
    await ElMessageBox.confirm('确定将该候选人放入储备人才库吗？', '放入储备', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await screening(candidateId.value, jobId.value, { decision: 'RESERVE' })
    ElMessage.success('已放入储备')
  } catch {
    // 取消操作
  }
}

// 加载决策面板数据
async function loadDecisionData() {
  if (!candidateId.value || !jobId.value) {
    ElMessage.error('缺少候选人或岗位参数')
    return
  }

  try {
    const res: any = await getDecisionPanel(candidateId.value, jobId.value)
    const data = res.data || {}
    Object.assign(candidateInfo, data.candidate || {})
    overallScore.value = data.overallScore ?? 0
    scoreTrend.value = data.scoreTrend ?? 'stable'
    if (data.radarDimensions) radarDimensions.value = data.radarDimensions
    if (data.tagMatches) tagMatches.value = data.tagMatches
    if (data.aiInsights) aiInsights.value = data.aiInsights
  } catch {
    ElMessage.error('加载决策面板数据失败')
  }
}

onMounted(async () => {
  await loadDecisionData()
  await nextTick()
  if (radarDimensions.value.length > 0) {
    initRadarChart()
  }

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    radarChart?.resize()
  })
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
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

.tag-rows {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .tag-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    border-radius: 6px;
    background: #fafbfc;
    transition: background 0.2s;

    &:hover {
      background: #f0f2f5;
    }

    .tag-name {
      width: 90px;
      font-size: 13px;
      font-weight: 600;
      color: $text-primary;
      flex-shrink: 0;
    }

    .tag-weight,
    .tag-corr {
      width: 80px;
      font-size: 12px;
      color: $text-secondary;
      text-align: center;
      flex-shrink: 0;
    }

    .tag-trend {
      width: 24px;
      text-align: center;
      font-size: 18px;
      font-weight: 700;
      flex-shrink: 0;

      &.text-success {
        color: $success-color;
      }

      &.text-danger {
        color: $danger-color;
      }

      &.text-info {
        color: $text-secondary;
      }
    }
  }
}

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
