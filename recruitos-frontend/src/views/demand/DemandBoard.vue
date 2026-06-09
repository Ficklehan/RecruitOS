<template>
  <div class="page-container page-stack">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">招聘需求看板</h2>
        <p class="page-subtitle">总览招聘需求审批与执行进度</p>
      </div>
      <el-button @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <StatCard
        label="招聘需求总数"
        :value="boardStats.total"
        icon="Document"
        color="#3B82F6"
        :trend="12"
      />
      <StatCard
        label="审批中"
        :value="boardStats.pending"
        icon="Clock"
        color="#D97706"
        :trend="-5"
      />
      <StatCard
        label="招聘中"
        :value="boardStats.recruiting"
        icon="UserFilled"
        color="#059669"
        :trend="8"
      />
      <StatCard
        label="已完成"
        :value="boardStats.completed"
        icon="CircleCheck"
        color="#64748B"
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
        <el-tag v-if="slaAlerts.length > 0" type="danger" size="small" disable-transitions>
          {{ slaAlerts.length }} 条告警
        </el-tag>
      </h3>
      <el-table :data="slaAlerts" stripe style="width: 100%" :row-class-name="() => 'sla-row'">
        <el-table-column prop="demandNo" label="需求编号" width="160" />
        <el-table-column prop="title" label="需求标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="alertType" label="告警类型" width="160">
          <template #default="{ row }">
            <el-tag type="danger" size="small" disable-transitions>{{ row.alertType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="overdueDays" label="超期天数" width="120" align="center">
          <template #default="{ row }">
            <span class="overdue-days">{{ row.overdueDays }} 天</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="当前状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
      </el-table>
      <div v-if="slaAlerts.length === 0" class="empty-alert">
        <el-icon :size="48" color="#94A3B8"><CircleCheck /></el-icon>
        <p>暂无 SLA 告警</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Refresh, CircleCheck } from '@element-plus/icons-vue'
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

// 状态映射
function getStatusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger',
    JOB_CREATED: 'primary', RECRUITING: 'warning', COMPLETED: 'success', CLOSED: 'info',
  }
  return map[status] || 'info'
}

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
    color: ['#3B82F6', '#059669', '#D97706', '#DC2626', '#64748B', '#b37feb', '#36cfc9'],
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
      textStyle: { fontSize: 13, color: '#334155' },
    },
    color: ['#3B82F6', '#059669', '#D97706', '#DC2626', '#64748B', '#b37feb', '#36cfc9'],
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
      textStyle: { fontSize: 13, color: '#334155' },
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
        color: '#64748B',
      },
      axisLine: { lineStyle: { color: '#F1F5F9' } },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#F1F5F9' } },
      axisLabel: { color: '#64748B' },
    },
    series: [
      {
        name: '需求人数',
        type: 'bar',
        barWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3B82F6' },
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
            { offset: 0, color: '#059669' },
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
