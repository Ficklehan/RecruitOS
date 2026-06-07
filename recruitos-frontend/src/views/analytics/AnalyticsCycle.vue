<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">招聘周期</h2>
    </div>

    <!-- 日期选择 -->
    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 300px"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
    </div>

    <!-- 整体统计 -->
    <div class="overall-card">
      <div class="overall-item">
        <div class="overall-label">平均招聘周期</div>
        <div class="overall-value">{{ avgCycleDays }} 天</div>
      </div>
      <div class="overall-item">
        <div class="overall-label">本月</div>
        <div class="overall-value">
          {{ currentMonthDays }} 天
          <span v-if="monthDiff < 0" class="trend-down">
            <el-icon><Bottom /></el-icon> {{ Math.abs(monthDiff) }}天
          </span>
          <span v-else-if="monthDiff > 0" class="trend-up">
            <el-icon><Top /></el-icon> +{{ monthDiff }}天
          </span>
          <span v-else class="trend-flat">持平</span>
        </div>
      </div>
      <div class="overall-item">
        <div class="overall-label">上月</div>
        <div class="overall-value">{{ lastMonthDays }} 天</div>
      </div>
    </div>

    <!-- 各阶段耗时 -->
    <div class="data-card">
      <h3 class="section-title">各阶段平均耗时</h3>
      <div class="stage-chart">
        <div
          v-for="stage in stageData"
          :key="stage.name"
          class="stage-row"
        >
          <div class="stage-label">{{ stage.name }}</div>
          <div class="stage-track">
            <div
              class="stage-bar"
              :style="{ width: getStageWidth(stage.days) + '%', backgroundColor: stage.color }"
            >
              <span class="stage-value">{{ stage.days }}天</span>
            </div>
          </div>
          <div class="stage-compare">
            <span v-if="stage.diff < 0" class="trend-down">
              <el-icon><Bottom /></el-icon> {{ Math.abs(stage.diff) }}天
            </span>
            <span v-else-if="stage.diff > 0" class="trend-up">
              <el-icon><Top /></el-icon> +{{ stage.diff }}天
            </span>
            <span v-else class="trend-flat">持平</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 时间线视图 -->
    <div class="data-card">
      <h3 class="section-title">招聘周期时间线</h3>
      <div class="timeline">
        <div
          v-for="stage in stageData"
          :key="stage.name"
          class="timeline-item"
        >
          <div class="timeline-dot" :style="{ backgroundColor: stage.color }"></div>
          <div class="timeline-content">
            <div class="timeline-name">{{ stage.name }}</div>
            <div class="timeline-days">{{ stage.days }} 天</div>
          </div>
          <div class="timeline-bar-wrapper">
            <div
              class="timeline-bar"
              :style="{ width: getStageWidth(stage.days) + '%', backgroundColor: stage.color }"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search, Top, Bottom } from '@element-plus/icons-vue'

const dateRange = ref<string[]>([])

// 阶段数据
const stageData = ref([
  { name: '需求审批', days: 3.2, diff: -0.5, color: '#3B82F6' },
  { name: '简历筛选', days: 5.1, diff: 0.8, color: '#059669' },
  { name: '初面安排', days: 4.8, diff: -0.3, color: '#D97706' },
  { name: '复试安排', days: 3.5, diff: 0.2, color: '#DC2626' },
  { name: 'Offer审批', days: 2.1, diff: -0.4, color: '#64748B' },
  { name: '入职准备', days: 7.0, diff: 1.5, color: '#3B82F6' },
])

// 最大天数（用于计算条形图宽度）
const maxDays = computed(() =>
  Math.max(...stageData.value.map(s => s.days))
)

// 平均招聘周期
const avgCycleDays = computed(() =>
  stageData.value.reduce((sum, s) => sum + s.days, 0).toFixed(1)
)

// 本月与上月对比
const currentMonthDays = computed(() =>
  Number(avgCycleDays.value)
)

const lastMonthDays = computed(() =>
  (Number(avgCycleDays.value) + 1.3).toFixed(1)
)

const monthDiff = computed(() =>
  (currentMonthDays.value - Number(lastMonthDays.value)).toFixed(1)
)

// 条形图宽度百分比
function getStageWidth(days: number): number {
  return maxDays.value > 0 ? (days / maxDays.value) * 100 : 0
}

function handleSearch() {
  // Mock: 重新加载数据
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.overall-card {
  background: $bg-card;
  border-radius: 8px;
  padding: 24px 32px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: flex;
  gap: 48px;
}

.overall-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overall-label {
  font-size: 13px;
  color: $text-secondary;
}

.overall-value {
  font-size: 20px;
  font-weight: 600;
  color: $text-primary;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 20px 0;
}

.stage-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stage-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stage-label {
  width: 80px;
  font-size: 13px;
  color: $text-regular;
  text-align: right;
  flex-shrink: 0;
}

.stage-track {
  flex: 1;
  height: 32px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.stage-bar {
  height: 100%;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 12px;
  transition: width 0.6s ease;
  min-width: 60px;
}

.stage-value {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.stage-compare {
  width: 100px;
  font-size: 13px;
  display: flex;
  align-items: center;
}

.trend-up {
  color: $danger-color;
  display: flex;
  align-items: center;
  gap: 2px;
}

.trend-down {
  color: $success-color;
  display: flex;
  align-items: center;
  gap: 2px;
}

.trend-flat {
  color: $text-secondary;
}

// 时间线视图
.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  position: relative;
  padding-left: 20px;

  &::before {
    content: '';
    position: absolute;
    left: 8px;
    top: 8px;
    bottom: 8px;
    width: 2px;
    background: $border-color;
  }
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  position: relative;
}

.timeline-dot {
  position: absolute;
  left: -20px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 0 0 2px $border-color;
  z-index: 1;
}

.timeline-content {
  width: 120px;
  flex-shrink: 0;
}

.timeline-name {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
}

.timeline-days {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}

.timeline-bar-wrapper {
  flex: 1;
  height: 24px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.timeline-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}
</style>
