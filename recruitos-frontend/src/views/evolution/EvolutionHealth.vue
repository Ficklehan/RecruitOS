<template>
  <PageShell title="健康监控">
<div class="health-overview">
      <div
        v-for="card in overviewCards"
        :key="card.label"
        class="health-card"
        :style="{ borderTop: `3px solid ${getHealthColor(card.value)}` }"
      >
        <div
          class="health-card-icon"
          :style="{ backgroundColor: getHealthColor(card.value) + '15', color: getHealthColor(card.value) }"
        >
          <component :is="card.icon" class="h-7 w-7" />
        </div>
        <div class="health-card-info">
          <div class="health-card-label">{{ card.label }}</div>
          <div class="health-card-value">{{ card.value }}%</div>
          <RProgress :value="card.value" class="h-1.5 mt-2" />
        </div>
      </div>
    </div>

    <RCard class="p-4 mb-6">
      <div class="section-header">
        <span class="section-title">岗位健康列表</span>
        <RButton variant="link" size="sm" :disabled="loading" @click="loadData">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新数据
        </RButton>
      </div>
      <RTable>
        <RTableHead>
          <RTableRow>
            <RTableTh class="min-w-[160px]">岗位</RTableTh>
            <RTableTh class="w-[160px] text-center">数据充足度</RTableTh>
            <RTableTh class="w-[160px] text-center">权重稳定性</RTableTh>
            <RTableTh class="w-[160px] text-center">匹配质量</RTableTh>
            <RTableTh class="w-[160px] text-center">进化新鲜度</RTableTh>
            <RTableTh class="w-[100px] text-center">综合评分</RTableTh>
            <RTableTh class="w-[90px] text-center">状态</RTableTh>
            <RTableTh class="w-[100px] text-center">操作</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-if="!loading && jobHealthList.length === 0">
            <RTableCell colspan="8" class="text-center text-muted-foreground py-8">暂无进化数据岗位</RTableCell>
          </RTableRow>
          <RTableRow v-for="row in jobHealthList" :key="row.jobId ?? row.jobName">
            <RTableCell class="truncate max-w-[200px]">{{ row.jobName }}</RTableCell>
            <RTableCell>
              <div class="metric-cell">
                <RProgress :value="row.dataSufficiency" class="h-2" />
                <span class="metric-label">{{ row.dataSufficiency }}%</span>
              </div>
            </RTableCell>
            <RTableCell>
              <div class="metric-cell">
                <RProgress :value="row.weightStability" class="h-2" />
                <span class="metric-label">{{ row.weightStability }}%</span>
              </div>
            </RTableCell>
            <RTableCell>
              <div class="metric-cell">
                <RProgress :value="row.matchQuality" class="h-2" />
                <span class="metric-label">{{ row.matchQuality }}%</span>
              </div>
            </RTableCell>
            <RTableCell>
              <div class="metric-cell">
                <RProgress :value="row.evolutionFreshness" class="h-2" />
                <span class="metric-label">{{ row.evolutionFreshness }}%</span>
              </div>
            </RTableCell>
            <RTableCell class="text-center">
              <span class="score-value" :style="{ color: getHealthColor(row.overallScore) }">
                {{ row.overallScore }}
              </span>
            </RTableCell>
            <RTableCell class="text-center">
              <RBadge :variant="elTagTypeToBadge(getStatusType(row.status))">{{ row.status }}</RBadge>
            </RTableCell>
            <RTableCell class="text-center">
              <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
            </RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </RCard>

    <RCard class="p-4">
      <div class="section-header">
        <span class="section-title">健康告警</span>
        <div class="flex items-center gap-2">
          <RBadge variant="outline">{{ alerts.length }}</RBadge>
          <RButton variant="link" size="sm">查看全部</RButton>
        </div>
      </div>
      <div class="alert-list">
        <div
          v-for="(alert, index) in alerts"
          :key="index"
          class="alert-item"
          :class="'alert-' + alert.severity"
        >
          <div class="alert-icon">
            <CircleX v-if="alert.severity === 'error'" class="h-5 w-5 text-destructive" />
            <AlertTriangle v-else-if="alert.severity === 'warning'" class="h-5 w-5 text-yellow-600" />
            <Info v-else class="h-5 w-5 text-primary" />
          </div>
          <div class="alert-content">
            <div class="alert-title">{{ alert.jobName }} - {{ alert.title }}</div>
            <div class="alert-desc">{{ alert.description }}</div>
          </div>
          <div class="alert-time">{{ alert.time }}</div>
          <RButton variant="link" size="sm" @click="handleResolveAlert(alert)">处理</RButton>
        </div>
      </div>
    </RCard>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, onMounted } from 'vue'
import {
  BarChart3, TrendingUp, RefreshCw, CircleX, AlertTriangle, Info,
} from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import { getJobList } from '@/api/modules/job'
import { getJobHealth, getSystemHealth, getHealthAlerts } from '@/api/modules/evolution'
import {
  RButton, RBadge, RCard, RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell, RProgress,
} from '@/components/ui'

interface JobHealth {
  jobId?: number
  jobName: string
  dataSufficiency: number
  weightStability: number
  matchQuality: number
  evolutionFreshness: number
  overallScore: number
  status: string
}

interface Alert {
  jobName: string
  title: string
  description: string
  severity: 'info' | 'warning' | 'error'
  time: string
}

const loading = ref(false)

const overviewCards = reactive([
  { label: '数据充足度', value: 0, icon: BarChart3 },
  { label: '权重稳定性', value: 0, icon: TrendingUp },
  { label: '匹配质量', value: 0, icon: TrendingUp },
  { label: '进化新鲜度', value: 0, icon: TrendingUp },
])

const jobHealthList = ref<JobHealth[]>([])
const alerts = ref<Alert[]>([])

function mapStatusLabel(status?: string): string {
  const map: Record<string, string> = { HEALTHY: '健康', WARNING: '警告', CRITICAL: '异常' }
  return status ? (map[status] || status) : '未知'
}

function mapHealthRow(vo: any, titleFallback?: string): JobHealth {
  return {
    jobId: vo.jobId,
    jobName: vo.jobTitle || titleFallback || `岗位 #${vo.jobId}`,
    dataSufficiency: vo.dataSufficiencyScore ?? 0,
    weightStability: vo.weightStabilityScore ?? 0,
    matchQuality: vo.matchQualityScore ?? 0,
    evolutionFreshness: vo.evolutionFreshnessScore ?? 0,
    overallScore: vo.overallScore ?? 0,
    status: mapStatusLabel(vo.status),
  }
}

function alertSeverity(status?: string): Alert['severity'] {
  if (status === 'CRITICAL') return 'error'
  if (status === 'WARNING') return 'warning'
  return 'info'
}

async function loadData() {
  loading.value = true
  try {
    const sysRes: any = await getSystemHealth()
    const sys = sysRes?.data ?? sysRes
    overviewCards[0].value = sys?.dataSufficiencyScore ?? 0
    overviewCards[1].value = sys?.weightStabilityScore ?? 0
    overviewCards[2].value = sys?.matchQualityScore ?? 0
    overviewCards[3].value = sys?.evolutionFreshnessScore ?? 0

    const jobsRes: any = await getJobList({ pageNum: 1, pageSize: 50, status: 'ACTIVE' })
    const jobs = jobsRes?.data?.records ?? jobsRes?.records ?? []
    const rows: JobHealth[] = []
    for (const job of jobs) {
      try {
        const hRes: any = await getJobHealth(job.id)
        const vo = hRes?.data ?? hRes
        rows.push(mapHealthRow(vo, job.title))
      } catch {
        /* skip jobs without evolution data */
      }
    }
    jobHealthList.value = rows.sort((a, b) => a.overallScore - b.overallScore)

    const alertRes: any = await getHealthAlerts()
    const alertList = alertRes?.data ?? alertRes ?? []
    alerts.value = (Array.isArray(alertList) ? alertList : []).map((vo: any) => ({
      jobName: vo.jobTitle || `岗位 #${vo.jobId}`,
      title: mapStatusLabel(vo.status),
      description: (vo.alerts && vo.alerts.length) ? vo.alerts.join('；') : '健康指标异常，建议查看岗位进化数据',
      severity: alertSeverity(vo.status),
      time: '—',
    }))
  } catch (e: any) {
    toast.error(e?.message || '加载健康数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

function getHealthColor(value: number): string {
  if (value >= 80) return '#16a34a'
  if (value >= 60) return '#ca8a04'
  return '#DC2626'
}

function getStatusType(status: string): string {
  const map: Record<string, string> = { '健康': 'success', '警告': 'warning', '异常': 'danger' }
  return map[status] || 'info'
}

function handleViewDetail(row: JobHealth) {
  toast.info(`查看「${row.jobName}」健康详情`)
}

function handleResolveAlert(alert: Alert) {
  toast.info(`处理告警：${alert.title}`)
}

function getRowActions(_row: JobHealth) {
  return [{ command: 'view', label: '查看详情', icon: 'View', type: 'primary', primary: true }]
}

function handleRowCommand(cmd: string, row: JobHealth) {
  if (cmd === 'view') handleViewDetail(row)
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.health-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;

  @media (max-width: 1200px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}

.health-card {
  background: $bg-card;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
  }

  .health-card-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .health-card-label {
    font-size: 14px;
    color: $text-secondary;
    margin-bottom: 4px;
  }

  .health-card-value {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1.2;
  }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }
}

.metric-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .metric-label {
    font-size: 12px;
    color: $text-secondary;
    min-width: 36px;
    text-align: right;
  }
}

.score-value {
  font-size: 16px;
  font-weight: 700;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid transparent;

  &.alert-error {
    background-color: $danger-lighter;
    border-left-color: $danger-color;
  }

  &.alert-warning {
    background-color: $warning-lighter;
    border-left-color: $warning-color;
  }

  &.alert-info {
    background-color: $primary-lighter;
    border-left-color: $primary-color;
  }

  .alert-content {
    flex: 1;
    min-width: 0;
  }

  .alert-title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .alert-desc {
    font-size: 13px;
    color: $text-regular;
    line-height: 1.5;
  }

  .alert-time {
    font-size: 12px;
    color: $text-placeholder;
    white-space: nowrap;
    flex-shrink: 0;
  }
}
</style>
