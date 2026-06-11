<template>
  <PageShell title="效果对比">
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

    <div class="rounded-xl bg-card text-card-foreground shadow-soft">
      <RTable>
        <RTableHead>
          <RTableRow>
            <RTableTh class="min-w-[160px]">供应商名称</RTableTh>
            <RTableTh class="w-[110px] text-center">总推荐数</RTableTh>
            <RTableTh class="w-[100px] text-center">成功入职</RTableTh>
            <RTableTh class="w-[180px] text-center">入职率</RTableTh>
            <RTableTh class="w-[130px] text-center">平均入职天数</RTableTh>
            <RTableTh class="w-[130px] text-right">总成本</RTableTh>
            <RTableTh class="w-[130px] text-right">单位成本</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="row in vendorPerformance" :key="row.vendorName">
            <RTableCell>{{ row.vendorName }}</RTableCell>
            <RTableCell class="text-center">{{ row.totalRecommendations }}</RTableCell>
            <RTableCell class="text-center">{{ row.successfulHires }}</RTableCell>
            <RTableCell>
              <div class="rate-cell">
                <RProgress :value="row.hireRate" :max="100" class="h-3.5" />
                <span class="rate-text">{{ row.hireRate }}%</span>
              </div>
            </RTableCell>
            <RTableCell class="text-center">{{ row.avgTimeToHire }} 天</RTableCell>
            <RTableCell class="text-right">¥ {{ row.totalCost.toLocaleString() }}</RTableCell>
            <RTableCell class="text-right">
              <span :class="{ 'cost-highlight': row.costPerHire === minCostPerHire }">
                ¥ {{ row.costPerHire.toLocaleString() }}
              </span>
            </RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </div>

    <div class="rounded-xl bg-card text-card-foreground shadow-soft">
      <h3 class="section-title">入职率对比</h3>
      <div class="bar-chart">
        <div v-for="item in vendorPerformance" :key="item.vendorName" class="bar-row">
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
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, computed, onMounted } from 'vue'
import { RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell, RProgress } from '@/components/ui'
import { getHeadhunterPerformance } from '@/api/modules/headhunter'

const vendorPerformance = ref<any[]>([])

const bestVendor = computed(() => {
  if (!vendorPerformance.value.length) return '-'
  const best = vendorPerformance.value.reduce((prev, curr) =>
    prev.hireRate > curr.hireRate ? prev : curr
  )
  return best.vendorName
})

const minCostPerHire = computed(() => {
  if (!vendorPerformance.value.length) return 0
  return Math.min(...vendorPerformance.value.map(v => v.costPerHire))
})

const avgCostPerHire = computed(() => {
  if (!vendorPerformance.value.length) return 0
  const total = vendorPerformance.value.reduce((sum, v) => sum + v.costPerHire, 0)
  return Math.round(total / vendorPerformance.value.length)
})

function getBarColor(rate: number): string {
  if (rate >= 30) return '#059669'
  if (rate >= 20) return '#3b82f6'
  if (rate >= 10) return '#d97706'
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
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 20px 0;
}

.rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rate-text {
  font-size: 12px;
  color: $text-secondary;
  min-width: 36px;
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
  background: $bg-page;
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
  color: var(--r-bg-card);
}
</style>
