<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">健康监控</h2>
    </div>

    <!-- 系统健康概览卡片 -->
    <div class="health-overview">
      <div
        v-for="card in overviewCards"
        :key="card.label"
        class="health-card"
        :style="{ borderTop: `3px solid ${getHealthColor(card.value)}` }"
      >
        <div class="health-card-icon" :style="{ backgroundColor: getHealthColor(card.value) + '15', color: getHealthColor(card.value) }">
          <el-icon :size="28">
            <component :is="card.icon" />
          </el-icon>
        </div>
        <div class="health-card-info">
          <div class="health-card-label">{{ card.label }}</div>
          <div class="health-card-value">{{ card.value }}%</div>
          <el-progress
            :percentage="card.value"
            :color="getHealthColor(card.value)"
            :show-text="false"
            :stroke-width="6"
            style="margin-top: 8px;"
          />
        </div>
      </div>
    </div>

    <!-- 岗位健康列表 -->
    <div class="data-card">
      <div class="section-header">
        <span class="section-title">岗位健康列表</span>
        <el-button type="primary" link>
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
      <el-table :data="jobHealthList" stripe style="width: 100%">
        <el-table-column prop="jobName" label="岗位" min-width="160" show-overflow-tooltip />
        <el-table-column label="数据充足度" width="160" align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="row.dataSufficiency"
              :color="getHealthColor(row.dataSufficiency)"
              :stroke-width="8"
              :format="(p: number) => p + '%'"
            />
          </template>
        </el-table-column>
        <el-table-column label="权重稳定性" width="160" align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="row.weightStability"
              :color="getHealthColor(row.weightStability)"
              :stroke-width="8"
              :format="(p: number) => p + '%'"
            />
          </template>
        </el-table-column>
        <el-table-column label="匹配质量" width="160" align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="row.matchQuality"
              :color="getHealthColor(row.matchQuality)"
              :stroke-width="8"
              :format="(p: number) => p + '%'"
            />
          </template>
        </el-table-column>
        <el-table-column label="进化新鲜度" width="160" align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="row.evolutionFreshness"
              :color="getHealthColor(row.evolutionFreshness)"
              :stroke-width="8"
              :format="(p: number) => p + '%'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="overallScore" label="综合评分" width="100" align="center">
          <template #default="{ row }">
            <span class="score-value" :style="{ color: getHealthColor(row.overallScore) }">
              {{ row.overallScore }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button type="warning" link size="small" @click="handleTriggerEvolution(row)">触发进化</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 健康告警 -->
    <div class="data-card">
      <div class="section-header">
        <span class="section-title">健康告警</span>
        <el-badge :value="alerts.length" :max="99" type="warning">
          <el-button type="primary" link>查看全部</el-button>
        </el-badge>
      </div>
      <div class="alert-list">
        <div
          v-for="(alert, index) in alerts"
          :key="index"
          class="alert-item"
          :class="'alert-' + alert.severity"
        >
          <div class="alert-icon">
            <el-icon v-if="alert.severity === 'error'" color="#DC2626"><CircleCloseFilled /></el-icon>
            <el-icon v-else-if="alert.severity === 'warning'" color="#D97706"><WarningFilled /></el-icon>
            <el-icon v-else color="#3B82F6"><InfoFilled /></el-icon>
          </div>
          <div class="alert-content">
            <div class="alert-title">{{ alert.jobName }} - {{ alert.title }}</div>
            <div class="alert-desc">{{ alert.description }}</div>
          </div>
          <div class="alert-time">{{ alert.time }}</div>
          <el-button type="primary" link size="small" @click="handleResolveAlert(alert)">处理</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis,
  TrendCharts,
  CircleCloseFilled,
  WarningFilled,
  InfoFilled,
  Refresh,
} from '@element-plus/icons-vue'

interface JobHealth {
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

// 系统概览卡片
const overviewCards = reactive([
  { label: '数据充足度', value: 87, icon: 'DataAnalysis' },
  { label: '权重稳定性', value: 92, icon: 'TrendCharts' },
  { label: '匹配质量', value: 78, icon: 'TrendCharts' },
  { label: '进化新鲜度', value: 65, icon: 'TrendCharts' },
])

// 岗位健康列表
const jobHealthList = ref<JobHealth[]>([
  {
    jobName: '高级前端工程师',
    dataSufficiency: 92,
    weightStability: 88,
    matchQuality: 85,
    evolutionFreshness: 70,
    overallScore: 84,
    status: '健康',
  },
  {
    jobName: 'Java后端工程师',
    dataSufficiency: 85,
    weightStability: 90,
    matchQuality: 82,
    evolutionFreshness: 75,
    overallScore: 83,
    status: '健康',
  },
  {
    jobName: '产品经理',
    dataSufficiency: 70,
    weightStability: 75,
    matchQuality: 68,
    evolutionFreshness: 55,
    overallScore: 67,
    status: '警告',
  },
  {
    jobName: 'UI设计师',
    dataSufficiency: 60,
    weightStability: 65,
    matchQuality: 58,
    evolutionFreshness: 45,
    overallScore: 57,
    status: '异常',
  },
  {
    jobName: '算法工程师',
    dataSufficiency: 78,
    weightStability: 82,
    matchQuality: 75,
    evolutionFreshness: 60,
    overallScore: 74,
    status: '警告',
  },
  {
    jobName: '测试工程师',
    dataSufficiency: 88,
    weightStability: 85,
    matchQuality: 80,
    evolutionFreshness: 72,
    overallScore: 81,
    status: '健康',
  },
])

// 健康告警
const alerts = ref<Alert[]>([
  {
    jobName: 'UI设计师',
    title: '数据严重不足',
    description: '近30天内搜索信号仅收到12条，建议增加搜索频率或扩展搜索渠道，当前数据量无法支撑可靠的权重进化。',
    severity: 'error',
    time: '10分钟前',
  },
  {
    jobName: '高级前端工程师',
    title: '数据不足',
    description: '建议增加搜索信号采集，当前数据量接近临界值，可能影响下一轮进化质量。',
    severity: 'warning',
    time: '1小时前',
  },
  {
    jobName: '产品经理',
    title: '匹配质量下降',
    description: '近7天匹配准确率下降5.2%，建议检查权重配置或增加训练数据。',
    severity: 'warning',
    time: '3小时前',
  },
  {
    jobName: '算法工程师',
    title: '进化周期延长',
    description: '距上次进化已超过14天，系统建议触发一次手动进化。',
    severity: 'info',
    time: '6小时前',
  },
])

// 获取健康颜色
function getHealthColor(value: number): string {
  if (value >= 80) return '#059669'
  if (value >= 60) return '#D97706'
  return '#DC2626'
}

// 获取状态类型
function getStatusType(status: string): string {
  const map: Record<string, string> = {
    '健康': 'success',
    '警告': 'warning',
    '异常': 'danger',
  }
  return map[status] || 'info'
}

function handleViewDetail(row: JobHealth) {
  ElMessage.info(`查看「${row.jobName}」健康详情`)
}

function handleTriggerEvolution(row: JobHealth) {
  ElMessage.success(`已触发「${row.jobName}」进化任务`)
}

function handleResolveAlert(alert: Alert) {
  ElMessage.info(`处理告警：${alert.title}`)
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.health-overview {
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

  .health-card-info {
    flex: 1;

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
  transition: background-color 0.2s;

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

  .alert-icon {
    flex-shrink: 0;
  }

  .alert-content {
    flex: 1;
    min-width: 0;

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
  }

  .alert-time {
    font-size: 12px;
    color: $text-placeholder;
    white-space: nowrap;
    flex-shrink: 0;
  }
}
</style>
