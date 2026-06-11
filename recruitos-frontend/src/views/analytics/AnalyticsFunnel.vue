<template>
  <PageShell title="招聘漏斗" subtitle="渠道寻源 + 管道筛选至入职的全链路转化">
    <div class="flex flex-wrap items-center gap-3 rounded-lg border bg-card p-4 shadow-sm">
      <DateRangePicker v-model="dateRange" />
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        查询
      </RButton>
    </div>

    <div class="overall-card">
      <div class="overall-left">
        <div class="overall-label">整体转化率</div>
        <div class="overall-value">{{ overallRate }}<span class="overall-unit">%</span></div>
        <div class="overall-desc">从平台检索到入职的最终转化</div>
      </div>
      <div class="overall-right">
        <div class="overall-stat">
          <span class="overall-stat-value">{{ sourcingFunnel[0]?.count || funnelData[0]?.count || 0 }}</span>
          <span class="overall-stat-label">平台检索</span>
        </div>
        <div class="overall-arrow">
          <svg width="32" height="16" viewBox="0 0 32 16" fill="none"><path d="M0 8h28M22 2l6 6-6 6" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" opacity="0.6"/></svg>
        </div>
        <div class="overall-stat">
          <span class="overall-stat-value">{{ funnelData[funnelData.length - 1]?.count || 0 }}</span>
          <span class="overall-stat-label">入职</span>
        </div>
      </div>
    </div>

    <div class="section-title-bar">渠道寻源漏斗（Campaign 统计）</div>
    <div class="rounded-xl bg-card text-card-foreground shadow-soft mb-6">
      <div class="funnel-container">
        <div v-for="(stage, index) in sourcingFunnel" :key="'s-' + stage.name" class="funnel-stage">
          <div class="funnel-label">{{ stage.name }}</div>
          <div class="funnel-bar-wrap">
            <div class="funnel-bar" :style="{ width: stage.percentage + '%', background: stage.color }">
              <span class="funnel-count">{{ stage.count }}</span>
            </div>
          </div>
          <div class="funnel-meta">
            <span class="funnel-percentage">{{ stage.percentage }}%</span>
            <span v-if="index > 0" class="funnel-conversion">转化 {{ stage.conversionRate }}%</span>
          </div>
        </div>
      </div>
    </div>

    <div class="section-title-bar">招聘管道漏斗</div>
    <div class="rounded-xl bg-card text-card-foreground shadow-soft mb-6">
      <div class="funnel-container">
        <div v-for="(stage, index) in funnelData" :key="stage.name" class="funnel-stage">
          <div class="funnel-label">{{ stage.name }}</div>
          <div class="funnel-bar-wrap">
            <div
              class="funnel-bar"
              :style="{ width: stage.percentage + '%', background: stage.color }"
            >
              <span class="funnel-count">{{ stage.count }}</span>
            </div>
          </div>
          <div class="funnel-meta">
            <span class="funnel-percentage">{{ stage.percentage }}%</span>
            <span v-if="index > 0" class="funnel-conversion">转化 {{ stage.conversionRate }}%</span>
          </div>
        </div>
      </div>
    </div>

    <div class="section-title-bar">渠道对比（自招 / 内推 / 猎头）</div>
    <div class="rounded-xl bg-card text-card-foreground shadow-soft p-4">
      <RTable>
        <RTableHead>
          <RTableRow>
            <RTableTh>渠道</RTableTh>
            <RTableTh class="text-right">候选人数</RTableTh>
            <RTableTh class="text-right">入职人数</RTableTh>
            <RTableTh class="text-right">转化率</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="ch in channelData" :key="ch.source">
            <RTableCell>{{ ch.channelName }}</RTableCell>
            <RTableCell class="text-right">{{ ch.candidates }}</RTableCell>
            <RTableCell class="text-right">{{ ch.hires }}</RTableCell>
            <RTableCell class="text-right">{{ ch.conversionRate }}%</RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, computed, onMounted } from 'vue'
import { Search } from 'lucide-vue-next'
import { RButton, RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell } from '@/components/ui'
import DateRangePicker from '@/components/app/DateRangePicker.vue'
import { getFunnelData } from '@/api/modules/analytics'

const dateRange = ref<[string, string] | null>(null)
const funnelColors = ['$primary-color', '#60A5FA', '#818CF8', '#A78BFA', '#C084FC', '$success-color']

type FunnelItem = { name: string; count: number; percentage: number; conversionRate: number; color: string }

const funnelData = ref<FunnelItem[]>([])
const sourcingFunnel = ref<FunnelItem[]>([])
const channelData = ref<{ channelName: string; source: string; candidates: number; hires: number; conversionRate: number }[]>([])

function mapStages(stages: any[], colors: string[]): FunnelItem[] {
  const max = Math.max(...stages.map((s: any) => s.count || 0), 1)
  return stages.map((s: any, i: number) => ({
    name: s.stageName,
    count: s.count || 0,
    percentage: Math.round(((s.count || 0) / max) * 1000) / 10,
    conversionRate: s.conversionRate || 0,
    color: colors[i % colors.length],
  }))
}

const overallRate = computed(() => {
  const src = sourcingFunnel.value.length ? sourcingFunnel.value : funnelData.value
  if (!src.length) return 0
  const first = src[0].count || 1
  const last = src[src.length - 1].count
  return ((last / first) * 100).toFixed(1)
})

async function loadFunnel() {
  try {
    const params: { dateFrom?: string; dateTo?: string } = {}
    if (dateRange.value?.length === 2) {
      params.dateFrom = dateRange.value[0]
      params.dateTo = dateRange.value[1]
    }
    const res: any = await getFunnelData(params)
    const data = res.data ?? res
    funnelData.value = mapStages(data?.stages || [], funnelColors)
    sourcingFunnel.value = mapStages(data?.sourcingStages || [], ['#0EA5E9', '#38BDF8', '#6366F1', '#8B5CF6'])
    channelData.value = data?.channels || []
  } catch {
    funnelData.value = []
    sourcingFunnel.value = []
    channelData.value = []
  }
}

function handleSearch() {
  loadFunnel()
}

onMounted(loadFunnel)
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.overall-card {
  background: linear-gradient(135deg, $primary-dark 0%, $primary-color 60%, #60A5FA 100%);
  border-radius: $border-radius-lg;
  padding: 28px 32px;
  margin-bottom: $spacing-xl;
  color: var(--r-bg-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overall-left {
  .overall-label {
    font-size: 13px;
    opacity: 0.75;
    margin-bottom: 4px;
  }

  .overall-value {
    font-size: 42px;
    font-weight: 700;
    line-height: 1.1;
    letter-spacing: -0.03em;
  }

  .overall-unit {
    font-size: 22px;
    font-weight: 500;
    opacity: 0.8;
  }

  .overall-desc {
    font-size: 13px;
    opacity: 0.6;
    margin-top: 4px;
  }
}

.overall-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.overall-stat {
  text-align: center;

  .overall-stat-value {
    display: block;
    font-size: 24px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  .overall-stat-label {
    display: block;
    font-size: 12px;
    opacity: 0.7;
    margin-top: 2px;
  }
}

.overall-arrow {
  opacity: 0.5;
}

.section-title-bar {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  margin: 8px 0 12px;
}

.funnel-container {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px 0;
}

.funnel-stage {
  display: grid;
  grid-template-columns: 80px 1fr 120px;
  align-items: center;
  gap: 16px;
}

.funnel-label {
  font-size: 13px;
  font-weight: 500;
  color: $text-regular;
  text-align: right;
}

.funnel-bar-wrap {
  height: 40px;
  background: $bg-muted;
  border-radius: 6px;
  overflow: hidden;
}

.funnel-bar {
  height: 100%;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 14px;
  transition: width 0.8s cubic-bezier(0.22, 1, 0.36, 1);
  min-width: 60px;
}

.funnel-count {
  font-size: 14px;
  font-weight: 700;
  color: var(--r-bg-card);
  letter-spacing: -0.01em;
}

.funnel-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.funnel-percentage {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.funnel-conversion {
  font-size: 12px;
  color: $text-placeholder;
}
</style>
