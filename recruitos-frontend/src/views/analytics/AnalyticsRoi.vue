<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">渠道ROI</h2>
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

    <!-- 汇总卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">总投入</div>
        <div class="stat-value">¥ {{ totalInvestment.toLocaleString() }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">总入职</div>
        <div class="stat-value success">{{ totalHires }} 人</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均招聘成本</div>
        <div class="stat-value primary">¥ {{ avgCost.toLocaleString() }}</div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="data-card">
      <el-table :data="channelData" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="channelName" label="渠道名称" min-width="140" />
        <el-table-column prop="investment" label="投入成本" width="130" align="right">
          <template #default="{ row }">
            ¥ {{ row.investment.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="hires" label="入职人数" width="100" align="center" />
        <el-table-column prop="costPerHire" label="单位成本" width="130" align="right">
          <template #default="{ row }">
            ¥ {{ row.costPerHire.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="conversionRate" label="转化率" width="100" align="center">
          <template #default="{ row }">
            {{ row.conversionRate }}%
          </template>
        </el-table-column>
        <el-table-column prop="roiRating" label="ROI评级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoiTagType(row.roiRating)" size="small" disable-transitions>
              {{ row.roiRating }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 单位成本横向对比 -->
    <div class="data-card">
      <h3 class="section-title">单位成本对比</h3>
      <div class="bar-chart">
        <div
          v-for="item in channelData"
          :key="item.channelName"
          class="bar-row"
        >
          <div class="bar-label">{{ item.channelName }}</div>
          <div class="bar-track">
            <div
              class="bar-fill"
              :style="{ width: getCostBarWidth(item.costPerHire) + '%', backgroundColor: getCostBarColor(item.roiRating) }"
            >
              <span class="bar-value">¥ {{ item.costPerHire.toLocaleString() }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

const dateRange = ref<string[]>([])

// 渠道数据
const channelData = ref([
  { channelName: 'BOSS直聘', investment: 85000, hires: 15, costPerHire: 5667, conversionRate: 12.5, roiRating: 'A' },
  { channelName: '拉勾', investment: 62000, hires: 8, costPerHire: 7750, conversionRate: 8.2, roiRating: 'B' },
  { channelName: '智联', investment: 48000, hires: 5, costPerHire: 9600, conversionRate: 6.5, roiRating: 'C' },
  { channelName: '猎头', investment: 180000, hires: 12, costPerHire: 15000, conversionRate: 24.0, roiRating: 'D' },
  { channelName: '内推', investment: 25000, hires: 10, costPerHire: 2500, conversionRate: 35.7, roiRating: 'A' },
])

// 汇总计算
const totalInvestment = computed(() =>
  channelData.value.reduce((sum, c) => sum + c.investment, 0)
)

const totalHires = computed(() =>
  channelData.value.reduce((sum, c) => sum + c.hires, 0)
)

const avgCost = computed(() =>
  totalHires.value > 0 ? Math.round(totalInvestment.value / totalHires.value) : 0
)

// 最高单位成本（用于计算条形图宽度）
const maxCost = computed(() =>
  Math.max(...channelData.value.map(c => c.costPerHire))
)

// ROI 评级标签颜色
function getRoiTagType(rating: string): string {
  const map: Record<string, string> = {
    A: 'success',
    B: '',
    C: 'warning',
    D: 'danger',
  }
  return map[rating] || 'info'
}

// 条形图宽度百分比
function getCostBarWidth(cost: number): number {
  return maxCost.value > 0 ? (cost / maxCost.value) * 100 : 0
}

// 条形图颜色
function getCostBarColor(rating: string): string {
  const map: Record<string, string> = {
    A: '#059669',
    B: '#3B82F6',
    C: '#D97706',
    D: '#DC2626',
  }
  return map[rating] || '#64748B'
}

function handleSearch() {
  // Mock: 重新加载数据
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: $bg-card;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

  .stat-label {
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: $text-primary;

    &.primary {
      color: $primary-color;
    }

    &.success {
      color: $success-color;
    }
  }
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 20px 0;
}

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bar-label {
  width: 80px;
  font-size: 13px;
  color: $text-regular;
  text-align: right;
  flex-shrink: 0;
}

.bar-track {
  flex: 1;
  height: 28px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 10px;
  transition: width 0.6s ease;
  min-width: 100px;
}

.bar-value {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}
</style>
