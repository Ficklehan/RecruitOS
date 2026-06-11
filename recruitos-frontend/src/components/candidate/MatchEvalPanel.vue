<template>
  <div class="match-eval-panel relative">
    <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60 rounded-lg">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

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
              <Progress :value="dim.value" :max="100" class="flex-1 h-2" />
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
              <Badge :variant="judgmentBadge(row.judgment)">
                {{ row.judgment }}
              </Badge>
            </div>
          </div>
          <p v-else class="empty-hint">暂无任职要求对照，请先完善职位描述并提取任职要求</p>
        </section>
      </div>

      <section v-if="aiInsights.length" class="insight-card">
        <div class="insight-head">
          <Wand2 class="h-5 w-5" />
          <span>匹配说明</span>
          <Badge variant="secondary">系统建议</Badge>
        </div>
        <div v-for="(insight, index) in aiInsights" :key="index" class="insight-row">
          <span class="insight-dot" :class="insight.type" />
          <p>{{ insight.text }}</p>
        </div>
      </section>

      <div v-if="showActions" class="action-bar">
        <slot name="actions" />
        <template v-if="!$slots.actions">
          <Button class="bg-green-600 hover:bg-green-700" @click="$emit('pass')">进入下一轮</Button>
          <Button variant="destructive" @click="$emit('reject')">标记不合适</Button>
          <Button variant="outline" @click="$emit('reserve')">储备至人才库</Button>
        </template>
        <Button v-if="showFullPageLink" variant="link" @click="openFullPage">全屏匹配评估</Button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Loader2, Wand2 } from 'lucide-vue-next'
import * as echarts from 'echarts'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import { Badge, Button, Progress } from '@/components/ui'
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

function judgmentBadge(judgment: string) {
  if (judgment === '符合') return elTagTypeToBadge('success')
  if (judgment === '待确认') return elTagTypeToBadge('warning')
  return elTagTypeToBadge('danger')
}

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
  background: var(--r-bg-card);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
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
  background: var(--r-bg-page);
  border-radius: 12px;
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
  border-bottom: 1px solid var(--r-divider);
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
