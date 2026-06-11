<template>
  <PageShell title="渠道ROI">
<div class="flex flex-wrap items-center gap-3 rounded-lg border bg-card p-4 shadow-sm">
      <DateRangePicker v-model="dateRange" />
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        查询
      </RButton>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
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

    <div class="rounded-xl bg-card text-card-foreground shadow-soft">
      <RTable v-if="channelData.length">
        <RTableHead>
          <RTableRow>
            <RTableTh class="min-w-[140px]">渠道名称</RTableTh>
            <RTableTh class="w-[130px] text-right">投入成本</RTableTh>
            <RTableTh class="w-[100px] text-center">入职人数</RTableTh>
            <RTableTh class="w-[130px] text-right">单位成本</RTableTh>
            <RTableTh class="w-[100px] text-center">转化率</RTableTh>
            <RTableTh class="w-[100px] text-center">ROI评级</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="row in channelData" :key="row.channelName">
            <RTableCell>{{ row.channelName }}</RTableCell>
            <RTableCell class="text-right">¥ {{ row.investment.toLocaleString() }}</RTableCell>
            <RTableCell class="text-center">{{ row.hires }}</RTableCell>
            <RTableCell class="text-right">¥ {{ row.costPerHire.toLocaleString() }}</RTableCell>
            <RTableCell class="text-center">{{ row.conversionRate }}%</RTableCell>
            <RTableCell class="text-center">
              <RBadge :variant="roiBadge(row.roiRating)">{{ row.roiRating }}</RBadge>
            </RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </div>

    <div class="rounded-xl bg-card text-card-foreground shadow-soft">
      <h3 class="section-title">单位成本对比</h3>
      <div class="bar-chart">
        <div v-for="item in channelData" :key="item.channelName" class="bar-row">
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
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, computed, onMounted } from 'vue'
import { Search } from 'lucide-vue-next'
import { RButton, RBadge, RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell } from '@/components/ui'
import DateRangePicker from '@/components/app/DateRangePicker.vue'
import type { BadgeVariant } from '@/lib/badgeVariants'
import { getRoiData } from '@/api/modules/analytics'

const dateRange = ref<[string, string] | null>(null)

const channelData = ref<{
  channelName: string
  investment: number
  hires: number
  costPerHire: number
  conversionRate: number
  roiRating: string
}[]>([])

function roiRating(costPerHire: number): string {
  if (costPerHire <= 5000) return 'A'
  if (costPerHire <= 10000) return 'B'
  if (costPerHire <= 15000) return 'C'
  return 'D'
}

const totalInvestment = computed(() => channelData.value.reduce((sum, c) => sum + c.investment, 0))
const totalHires = computed(() => channelData.value.reduce((sum, c) => sum + c.hires, 0))
const avgCost = computed(() => (totalHires.value > 0 ? Math.round(totalInvestment.value / totalHires.value) : 0))
const maxCost = computed(() => Math.max(...channelData.value.map((c) => c.costPerHire)))

function roiBadge(rating: string): BadgeVariant {
  const map: Record<string, BadgeVariant> = { A: 'default', B: 'secondary', C: 'outline', D: 'destructive' }
  return map[rating] ?? 'secondary'
}

function getCostBarWidth(cost: number): number {
  return maxCost.value > 0 ? (cost / maxCost.value) * 100 : 0
}

function getCostBarColor(rating: string): string {
  const map: Record<string, string> = {
    A: '$success-color',
    B: '$primary-color',
    C: '$warning-color',
    D: '#DC2626',
  }
  return map[rating] || '$text-secondary'
}

async function loadRoi() {
  try {
    const params: { dateFrom?: string; dateTo?: string } = {}
    if (dateRange.value?.length === 2) {
      params.dateFrom = dateRange.value[0]
      params.dateTo = dateRange.value[1]
    }
    const res: any = await getRoiData(params)
    const channels = res.data?.channels || []
    channelData.value = channels.map((c: any) => ({
      channelName: c.channelName,
      investment: c.cost || 0,
      hires: c.hires || 0,
      costPerHire: c.costPerHire || 0,
      conversionRate: c.conversionRate || 0,
      roiRating: roiRating(c.costPerHire || 0),
    }))
  } catch {
    channelData.value = []
  }
}

function handleSearch() {
  loadRoi()
}

onMounted(loadRoi)
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

    &.primary { color: $primary-color; }
    &.success { color: $success-color; }
  }
}

.section-title {
  font-size: 14px;
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
  min-width: 100px;
}

.bar-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--r-bg-card);
}
</style>
