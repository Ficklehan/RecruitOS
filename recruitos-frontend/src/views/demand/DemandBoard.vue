<template>
  <PageShell title="招聘需求看板" subtitle="总览招聘需求审批与执行进度">
    <template #actions>
      <RButton variant="outline" @click="handleRefresh">
        <RefreshCw class="mr-2 h-4 w-4" />
        刷新
      </RButton>
    </template>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <StatCard
        label="招聘需求总数"
        :value="boardStats.total"
        icon="Document"
        color="$primary-color"
        :trend="12"
      />
      <StatCard
        label="审批中"
        :value="boardStats.pending"
        icon="Clock"
        color="$warning-color"
        :trend="-5"
      />
      <StatCard
        label="招聘中"
        :value="boardStats.recruiting"
        icon="UserFilled"
        color="$success-color"
        :trend="8"
      />
      <StatCard
        label="已完成"
        :value="boardStats.completed"
        icon="CircleCheck"
        color="$text-secondary"
        :trend="3"
      />
    </div>

    <!-- 图表区域 -->
    <div class="chart-grid">
      <!-- 需求漏斗图 -->
      <div class="chart-card">
        <h3 class="chart-title">需求漏斗</h3>
        <div ref="funnelChartRef" class="chart-container"></div>
      </div>

      <!-- 需求来源统计饼图 -->
      <div class="chart-card">
        <h3 class="chart-title">需求来源统计</h3>
        <div ref="pieChartRef" class="chart-container"></div>
      </div>
    </div>

    <!-- 编制消耗率 -->
    <div class="chart-card full-width">
      <h3 class="chart-title">编制消耗率</h3>
      <div ref="barChartRef" class="chart-container-lg"></div>
    </div>

    <!-- SLA 告警列表 -->
    <div class="chart-card full-width">
      <h3 class="chart-title">
        <span>SLA 告警</span>
        <RBadge v-if="slaAlerts.length > 0" variant="destructive">
          {{ slaAlerts.length }} 条告警
        </RBadge>
      </h3>
      <RTable v-if="slaAlerts.length">
        <RTableHead>
          <RTableRow>
            <RTableTh class="w-[160px]">需求编号</RTableTh>
            <RTableTh class="min-w-[200px]">需求标题</RTableTh>
            <RTableTh class="w-[120px]">部门</RTableTh>
            <RTableTh class="w-[160px]">告警类型</RTableTh>
            <RTableTh class="w-[120px] text-center">超期天数</RTableTh>
            <RTableTh class="w-[120px] text-center">当前状态</RTableTh>
            <RTableTh class="w-[170px]">创建时间</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="row in slaAlerts" :key="row.demandNo" class="sla-row">
            <RTableCell>{{ row.demandNo }}</RTableCell>
            <RTableCell>{{ row.title }}</RTableCell>
            <RTableCell>{{ row.department }}</RTableCell>
            <RTableCell><RBadge variant="destructive">{{ row.alertType }}</RBadge></RTableCell>
            <RTableCell class="text-center"><span class="overdue-days">{{ row.overdueDays }} 天</span></RTableCell>
            <RTableCell class="text-center">
              <RBadge :variant="demandStatusBadge(row.status)">{{ getStatusLabel(row.status) }}</RBadge>
            </RTableCell>
            <RTableCell>{{ row.createdAt }}</RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
      <div v-else class="empty-alert">
        <CircleCheck class="h-12 w-12 text-muted-foreground" />
        <p>暂无 SLA 告警</p>
      </div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { RefreshCw, CircleCheck } from 'lucide-vue-next'
import { RButton, RBadge, RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell } from '@/components/ui'
import { demandStatusBadge } from '@/lib/badgeVariants'
import StatCard from '@/components/StatCard.vue'
import { demandStatusLabel } from '@/constants/businessLabels'
import { getDemandBoard } from '@/api/modules/demand'

// 统计数据
const boardStats = reactive({
  total: 0,
  pending: 0,
  recruiting: 0,
  completed: 0,
})

// SLA 告警
const slaAlerts = ref<any[]>([])

// 图表 refs
const funnelChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
let funnelChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

function getStatusLabel(status: string) {
  return demandStatusLabel(status)
}

// 初始化漏斗图
function initFunnelChart() {
  if (!funnelChartRef.value) return
  funnelChart = echarts.init(funnelChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}',
    },
    color: ['$primary-color', '$success-color', '$warning-color', '#DC2626', '$text-secondary', '#b37feb', '#36cfc9'],
    series: [
      {
        type: 'funnel',
        left: '10%',
        top: 20,
        bottom: 20,
        width: '80%',
        min: 0,
        max: Math.max(boardStats.total, 1),
        minSize: '0%',
        maxSize: '100%',
        sort: 'descending',
        gap: 4,
        label: {
          show: true,
          position: 'inside',
          formatter: '{b}\n{c}',
          fontSize: 13,
          color: '#fff',
        },
        itemStyle: {
          borderWidth: 0,
          borderRadius: 4,
        },
        data: [
          { value: boardStats.total, name: '总需求' },
          { value: boardStats.pending, name: '审批中' },
          { value: boardStats.recruiting, name: '招聘中' },
          { value: boardStats.completed, name: '已完成' },
        ],
      },
    ],
  }
  funnelChart.setOption(option)
}

// 初始化饼图
function initPieChart() {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { fontSize: 13, color: '$text-regular' },
    },
    color: ['$primary-color', '$success-color', '$warning-color', '#DC2626', '$text-secondary', '#b37feb', '#36cfc9'],
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data: [
          { value: 45, name: '技术部' },
          { value: 22, name: '产品部' },
          { value: 18, name: '运营部' },
          { value: 15, name: '设计部' },
          { value: 12, name: '市场部' },
          { value: 10, name: '人力资源部' },
          { value: 6, name: '财务部' },
        ],
      },
    ],
  }
  pieChart.setOption(option)
}

// 初始化柱状图（编制消耗率）
function initBarChart() {
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)

  const demandNames = [
    '高级前端工程师', 'Java后端工程师', '产品经理', 'UI设计师',
    '数据分析师', '测试工程师', '运营经理', '算法工程师',
  ]
  const required = [2, 3, 1, 1, 2, 2, 1, 2]
  const hired = [1, 2, 1, 0, 1, 0, 1, 1]

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    legend: {
      data: ['需求人数', '已录用'],
      top: 0,
      right: 0,
      textStyle: { fontSize: 13, color: '$text-regular' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 40,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: demandNames,
      axisLabel: {
        rotate: 30,
        fontSize: 12,
        color: '$text-secondary',
      },
      axisLine: { lineStyle: { color: '$bg-muted' } },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '$bg-muted' } },
      axisLabel: { color: '$text-secondary' },
    },
    series: [
      {
        name: '需求人数',
        type: 'bar',
        barWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '$primary-color' },
            { offset: 1, color: '#a0cfff' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        data: required,
      },
      {
        name: '已录用',
        type: 'bar',
        barWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '$success-color' },
            { offset: 1, color: '#b3e19d' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        data: hired,
      },
    ],
  }
  barChart.setOption(option)
}

// 加载看板数据
async function loadBoardData() {
  try {
    const res: any = await getDemandBoard()
    const data = res.data || {}
    Object.assign(boardStats, {
      total: data.totalDemands || 0,
      pending: data.pendingApproval || 0,
      recruiting: data.recruiting || 0,
      completed: data.completed || 0,
    })
  } catch {
    // API 失败时保持默认值
  }
}

function handleRefresh() {
  loadBoardData()
}

// 窗口大小变化处理
function handleResize() {
  funnelChart?.resize()
  pieChart?.resize()
  barChart?.resize()
}

onMounted(async () => {
  await loadBoardData()
  await nextTick()
  initFunnelChart()
  initPieChart()
  initBarChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  funnelChart?.dispose()
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 16px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 16px;
  transition: box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
  }

  &.full-width {
    width: 100%;
  }
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-container {
  width: 100%;
  height: 360px;
}

.chart-container-lg {
  width: 100%;
  height: 400px;
}

.overdue-days {
  color: $danger-color;
  font-weight: 600;
}

.empty-alert {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;

  p {
    margin-top: 12px;
    font-size: 14px;
    color: $text-placeholder;
  }
}

:deep(.sla-row) {
  background-color: $danger-lighter !important;
}
</style>
