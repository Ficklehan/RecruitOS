<template>
  <div class="match-eval-panel" v-loading="loading">
    <EmptyStateCta
      v-if="!jobId"
      description="请先选择在招职位，再查看匹配建议、符合点与待确认项"
      :image-size="64"
    />

    <template v-else-if="loaded">
      <div class="verdict-banner">
        <MatchVerdict
          :match-score="overallScore"
          :match-detail="matchDetailJson"
          mode="card"
          :show-score="false"
        />
        <div v-if="rankText" class="rank-text">{{ rankText }}</div>
      </div>

      <div class="eval-grid">
        <section class="eval-card">
          <div class="card-head">
            <h4>能力画像</h4>
            <span class="card-sub">辅助了解候选人与职位要求的差距</span>
          </div>
          <div v-if="radarDimensions.length" ref="radarChartRef" class="radar-chart" />
          <div v-if="radarDimensions.length" class="radar-bars">
            <div v-for="(dim, index) in radarDimensions" :key="dim.name" class="radar-bar-row">
              <span class="dim-name">{{ dim.name }}</span>
              <el-progress
                :percentage="dim.value"
                :stroke-width="6"
                :color="radarColors[index % radarColors.length]"
                :show-text="false"
                style="flex: 1"
              />
              <span class="dim-value">{{ dim.value }}</span>
            </div>
          </div>
          <p v-else class="empty-hint">暂无能力画像，请先完善简历与任职要求</p>
        </section>

        <section class="eval-card">
          <div class="card-head">
            <h4>任职要求对照</h4>
            <span class="card-sub">岗位要求与候选人情况的逐项判断</span>
          </div>
          <div v-if="requirementRows.length" class="req-table">
            <div class="req-header">
              <span>任职要求</span>
              <span>判断</span>
            </div>
            <div v-for="row in requirementRows" :key="row.name" class="req-row">
              <div class="req-left">
                <span class="req-name">{{ row.name }}</span>
                <span v-if="row.hint" class="req-hint">{{ row.hint }}</span>
              </div>
              <el-tag
                size="small"
                :type="row.judgment === '符合' ? 'success' : row.judgment === '待确认' ? 'warning' : 'danger'"
              >
                {{ row.judgment }}
              </el-tag>
            </div>
          </div>
          <p v-else class="empty-hint">暂无任职要求对照，请先完善职位描述并提取任职要求</p>
        </section>
      </div>

      <section v-if="aiInsights.length" class="insight-card">
        <div class="insight-head">
          <el-icon><MagicStick /></el-icon>
          <span>匹配说明</span>
          <el-tag size="small" type="info" effect="plain">系统建议</el-tag>
        </div>
        <div v-for="(insight, index) in aiInsights" :key="index" class="insight-row">
          <span class="insight-dot" :class="insight.type" />
          <p>{{ insight.text }}</p>
        </div>
      </section>

      <div v-if="showActions" class="action-bar">
        <slot name="actions" />
        <template v-if="!$slots.actions">
          <el-button type="success" @click="$emit('pass')">进入下一轮</el-button>
          <el-button type="danger" @click="$emit('reject')">标记不合适</el-button>
          <el-button type="warning" @click="$emit('reserve')">储备至人才库</el-button>
        </template>
        <el-button v-if="showFullPageLink" link type="primary" @click="openFullPage">全屏匹配评估</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { MagicStick } from '@element-plus/icons-vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { getDecisionPanel } from '@/api/modules/candidate'
import {
  parseMatchDetail,
  extractRadarDimensions,
  extractRequirementRows,
  extractAiInsights,
  type AiInsightRow,
} from '@/utils/matchVerdict'

const props = withDefaults(defineProps<{
  candidateId: number
  jobId?: number | null
  showActions?: boolean
  showFullPageLink?: boolean
}>(), {
  jobId: null,
  showActions: true,
  showFullPageLink: true,
})

defineEmits<{
  pass: []
  reject: []
  reserve: []
}>()

const router = useRouter()
const loading = ref(false)
const loaded = ref(false)
const overallScore = ref(0)
const matchDetailJson = ref<string | null>(null)
const rankText = ref('')
const radarDimensions = ref<{ name: string; value: number }[]>([])
const requirementRows = ref<ReturnType<typeof extractRequirementRows>>([])
const aiInsights = ref<AiInsightRow[]>([])
const radarChartRef = ref<HTMLElement>()
let radarChart: echarts.ECharts | null = null

const radarColors = ['#3B82F6', '#059669', '#D97706', '#DC2626', '#64748B']

function disposeChart() {
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
}

function initRadarChart() {
  disposeChart()
  if (!radarChartRef.value || !radarDimensions.value.length) return
  radarChart = echarts.init(radarChartRef.value)
  radarChart.setOption({
    radar: {
      indicator: radarDimensions.value.map(dim => ({ name: dim.name, max: 100 })),
      shape: 'polygon',
      splitNumber: 5,
      axisName: { color: '#334155', fontSize: 12 },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      splitArea: {
        show: true,
        areaStyle: { color: ['rgba(59,130,246,0.02)', 'rgba(59,130,246,0.05)'] },
      },
    },
    series: [{
      type: 'radar',
      data: [{
        value: radarDimensions.value.map(d => d.value),
        areaStyle: { color: 'rgba(59,130,246,0.2)' },
        lineStyle: { color: '#3B82F6', width: 2 },
        itemStyle: { color: '#3B82F6' },
      }],
    }],
    tooltip: { trigger: 'item' },
  })
}

function applyPanels() {
  const verdict = parseMatchDetail(matchDetailJson.value, overallScore.value)
  radarDimensions.value = extractRadarDimensions(verdict)
  requirementRows.value = extractRequirementRows(verdict)
  aiInsights.value = extractAiInsights(verdict)
  if (verdict.rankInJob && verdict.totalInJob) {
    rankText.value = `本职位排名 ${verdict.rankInJob} / ${verdict.totalInJob}`
  } else {
    rankText.value = ''
  }
}

async function load() {
  if (!props.candidateId || !props.jobId) {
    loaded.value = false
    return
  }
  loading.value = true
  try {
    const res: any = await getDecisionPanel(props.candidateId, props.jobId)
    const data = res.data || {}
    overallScore.value = data.matchScore ?? data.overallScore ?? 0
    matchDetailJson.value = data.matchDetail ?? null
    applyPanels()
    loaded.value = true
    await nextTick()
    initRadarChart()
  } catch {
    loaded.value = false
  } finally {
    loading.value = false
  }
}

function openFullPage() {
  if (!props.jobId) return
  router.push({
    path: '/pipeline/decision',
    query: { candidateId: String(props.candidateId), jobId: String(props.jobId) },
  })
}

watch(() => [props.candidateId, props.jobId], load, { immediate: true })
onBeforeUnmount(disposeChart)

defineExpose({ reload: load })
</script>

<style scoped lang="scss">
.match-eval-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.verdict-banner {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
}

.rank-text {
  margin-top: 8px;
  font-size: 13px;
  color: #64748b;
}

.eval-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.eval-card,
.insight-card {
  background: #fafbfc;
  border: 1px solid #eef2f6;
  border-radius: 8px;
  padding: 12px 14px;
}

.card-head h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.card-sub {
  font-size: 12px;
  color: #94a3b8;
}

.radar-chart {
  height: 220px;
  margin-top: 8px;
}

.radar-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.radar-bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dim-name {
  width: 72px;
  font-size: 12px;
  color: #64748b;
  flex-shrink: 0;
}

.dim-value {
  width: 28px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.req-table {
  margin-top: 8px;
}

.req-header,
.req-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
}

.req-header {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
}

.req-left {
  flex: 1;
  min-width: 0;
}

.req-name {
  font-size: 14px;
  color: #334155;
}

.req-hint {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.insight-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
}

.insight-row {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
    color: #475569;
  }
}

.insight-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;

  &.highlight { background: #059669; }
  &.warning { background: #d97706; }
  &.info { background: #3b82f6; }
}

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 4px;
}

.empty-hint {
  margin: 12px 0 0;
  font-size: 13px;
  color: #94a3b8;
}
</style>
