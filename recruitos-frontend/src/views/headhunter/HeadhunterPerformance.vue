<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">效果对比</h2>
    </div>

    <!-- 汇总统计 -->
    <div class="summary-row">
      <div class="summary-card">
        <div class="summary-label">最佳供应商</div>
        <div class="summary-value success">{{ bestVendor }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">平均单位成本</div>
        <div class="summary-value primary">¥ {{ avgCostPerHire.toLocaleString() }}</div>
      </div>
    </div>

    <!-- 对比表格 -->
    <div class="data-card">
      <el-table :data="vendorPerformance" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="vendorName" label="供应商名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="totalRecommendations" label="总推荐数" width="110" align="center" />
        <el-table-column prop="successfulHires" label="成功入职" width="100" align="center" />
        <el-table-column label="入职率" width="180" align="center">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="row.hireRate"
                :stroke-width="14"
                :text-inside="true"
                :color="getProgressColor(row.hireRate)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="avgTimeToHire" label="平均入职天数" width="130" align="center">
          <template #default="{ row }">
            {{ row.avgTimeToHire }} 天
          </template>
        </el-table-column>
        <el-table-column prop="totalCost" label="总成本" width="130" align="right">
          <template #default="{ row }">
            ¥ {{ row.totalCost.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="costPerHire" label="单位成本" width="130" align="right">
          <template #default="{ row }">
            <span :class="{ 'cost-highlight': row.costPerHire === minCostPerHire }">
              ¥ {{ row.costPerHire.toLocaleString() }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 入职率横向对比条形图 -->
    <div class="data-card">
      <h3 class="section-title">入职率对比</h3>
      <div class="bar-chart">
        <div
          v-for="item in vendorPerformance"
          :key="item.vendorName"
          class="bar-row"
        >
          <div class="bar-label">{{ item.vendorName }}</div>
          <div class="bar-track">
            <div
              class="bar-fill"
              :style="{ width: item.hireRate + '%', backgroundColor: getBarColor(item.hireRate) }"
            >
              <span class="bar-value">{{ item.hireRate }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getHeadhunterPerformance } from '@/api/modules/headhunter'

const vendorPerformance = ref<any[]>([])

// 计算最佳供应商
const bestVendor = computed(() => {
  if (!vendorPerformance.value.length) return '-'
  const best = vendorPerformance.value.reduce((prev, curr) =>
    prev.hireRate > curr.hireRate ? prev : curr
  )
  return best.vendorName
})

// 计算最低单位成本
const minCostPerHire = computed(() => {
  if (!vendorPerformance.value.length) return 0
  return Math.min(...vendorPerformance.value.map(v => v.costPerHire))
})

// 计算平均单位成本
const avgCostPerHire = computed(() => {
  if (!vendorPerformance.value.length) return 0
  const total = vendorPerformance.value.reduce((sum, v) => sum + v.costPerHire, 0)
  return Math.round(total / vendorPerformance.value.length)
})

// 进度条颜色
function getProgressColor(rate: number): string {
  if (rate >= 30) return '#059669'
  if (rate >= 20) return '#3B82F6'
  if (rate >= 10) return '#D97706'
  return '#DC2626'
}

// 条形图颜色
function getBarColor(rate: number): string {
  if (rate >= 30) return '#059669'
  if (rate >= 20) return '#3B82F6'
  if (rate >= 10) return '#D97706'
  return '#DC2626'
}

async function loadData() {
  const res = await getHeadhunterPerformance()
  vendorPerformance.value = res.data.records || res.data.list || res.data || []
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.summary-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  background: $bg-card;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

  .summary-label {
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: 8px;
  }

  .summary-value {
    font-size: 20px;
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

.rate-cell {
  width: 100%;
}

.cost-highlight {
  color: $success-color;
  font-weight: 600;
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
  width: 120px;
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
  min-width: 60px;
}

.bar-value {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}
</style>
