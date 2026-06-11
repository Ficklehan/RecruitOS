<template>
  <div class="ai-value-dashboard">
    <div class="avd-header">
      <h3 class="avd-title">AI 价值看板</h3>
      <span class="avd-period">近 8 周</span>
    </div>

    <!-- 核心指标行 -->
    <div class="avd-kpi-row">
      <div class="avd-kpi">
        <span class="avd-kpi-value">{{ overall.totalAiDecisions?.toLocaleString() }}</span>
        <span class="avd-kpi-label">AI 决策次数</span>
      </div>
      <div class="avd-kpi">
        <span class="avd-kpi-value">{{ overall.humanAdoptionRate }}%</span>
        <span class="avd-kpi-label">采纳率</span>
      </div>
      <div class="avd-kpi">
        <span class="avd-kpi-value">{{ overall.avgTimeSavedMin }}min</span>
        <span class="avd-kpi-label">平均节省时间</span>
      </div>
      <div class="avd-kpi kpi-positive">
        <span class="avd-kpi-value">+{{ overall.offerAcceptLift }}%</span>
        <span class="avd-kpi-label">Offer接受提升</span>
      </div>
      <div class="avd-kpi kpi-positive">
        <span class="avd-kpi-value">-{{ overall.cycleReductionDays }}d</span>
        <span class="avd-kpi-label">周期缩短</span>
      </div>
    </div>

    <!-- 趋势图 -->
    <div class="avd-trend">
      <div class="avd-trend-title">采纳率 & Offer提升趋势</div>
      <div class="avd-trend-chart">
        <div v-for="(pt, i) in trend" :key="pt.week" class="avd-bar-group">
          <div class="avd-bar-col">
            <div class="avd-bar avd-bar-adoption" :style="{ height: pt.adoptionRate + '%' }" :title="'采纳率: ' + pt.adoptionRate + '%'"></div>
            <span class="avd-bar-label">{{ pt.week }}</span>
          </div>
        </div>
      </div>
      <div class="avd-legend">
        <span class="avd-legend-item"><span class="avd-dot adoption"></span> 采纳率</span>
        <span class="avd-legend-item"><span class="avd-dot offer"></span> Offer 提升</span>
      </div>
    </div>

    <!-- 触点详情表 -->
    <div class="avd-touchpoints">
      <div class="avd-trend-title">触点效果明细</div>
      <table class="avd-table">
        <thead>
          <tr>
            <th>触点</th>
            <th class="avd-num">使用次数</th>
            <th class="avd-num">采纳率</th>
            <th class="avd-num">趋势</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tp in touchpoints" :key="tp.name">
            <td>{{ tp.name }}</td>
            <td class="avd-num">{{ tp.totalUses?.toLocaleString() }}</td>
            <td class="avd-num">{{ tp.adoptionRate }}%</td>
            <td class="avd-num">
              <span :class="trendClass(tp.trend)">{{ tp.trend }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAiValue, type AiValueData } from '../../api/modules/brain'

const touchpoints = ref<AiValueData['touchpoints']>([])
const overall = ref<AiValueData['overall']>({ totalAiDecisions: 0, humanAdoptionRate: 0, avgTimeSavedMin: 0, offerAcceptLift: 0, cycleReductionDays: 0, calibrationKappaAvg: 0 })
const trend = ref<AiValueData['trend']>([])

onMounted(async () => {
  try {
    const res = await getAiValue()
    if (res.data) {
      touchpoints.value = res.data.touchpoints || []
      overall.value = res.data.overall || overall.value
      trend.value = res.data.trend || []
    }
  } catch (e) { /* use defaults */ }
})

function trendClass(t: string) {
  if (t === 'NEW') return 'avd-trend-new'
  if (t.startsWith('+')) return 'avd-trend-up'
  if (t.startsWith('-')) return 'avd-trend-down'
  return ''
}
</script>

<style scoped>
.ai-value-dashboard {
  background: var(--r-bg-primary, #fff);
  border: 1px solid var(--r-border, #e5e7eb);
  border-radius: 8px;
  padding: 20px;
}
.avd-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.avd-title { font-size: 15px; font-weight: 600; margin: 0; }
.avd-period { font-size: 12px; color: var(--r-text-secondary, #6b7280); }

.avd-kpi-row { display: flex; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
.avd-kpi { flex: 1; min-width: 100px; text-align: center; padding: 12px 8px; background: var(--r-bg-secondary, #f9fafb); border-radius: 6px; }
.avd-kpi-value { display: block; font-size: 22px; font-weight: 700; color: var(--r-text-primary, #111827); }
.avd-kpi-label { display: block; font-size: 11px; color: var(--r-text-secondary, #6b7280); margin-top: 2px; }
.kpi-positive .avd-kpi-value { color: #059669; }

.avd-trend { margin-bottom: 20px; }
.avd-trend-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; color: var(--r-text-primary, #111827); }
.avd-trend-chart { display: flex; align-items: flex-end; gap: 6px; height: 100px; padding: 0 4px; }
.avd-bar-group { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.avd-bar-col { display: flex; flex-direction: column; align-items: center; width: 100%; height: 100%; justify-content: flex-end; }
.avd-bar { width: 60%; min-width: 12px; border-radius: 3px 3px 0 0; transition: height 0.3s; }
.avd-bar-adoption { background: #6366f1; }
.avd-bar-label { font-size: 10px; color: var(--r-text-secondary, #9ca3af); margin-top: 4px; }
.avd-legend { display: flex; gap: 16px; margin-top: 8px; font-size: 11px; color: var(--r-text-secondary, #6b7280); }
.avd-legend-item { display: flex; align-items: center; gap: 4px; }
.avd-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.avd-dot.adoption { background: #6366f1; }
.avd-dot.offer { background: #10b981; }

.avd-touchpoints { }
.avd-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.avd-table th { text-align: left; padding: 8px 12px; border-bottom: 1px solid var(--r-border, #e5e7eb); font-weight: 600; color: var(--r-text-secondary, #6b7280); font-size: 12px; }
.avd-table td { padding: 8px 12px; border-bottom: 1px solid var(--r-border, #f3f4f6); }
.avd-num { text-align: right; font-variant-numeric: tabular-nums; }
.avd-trend-up { color: #059669; font-weight: 600; }
.avd-trend-down { color: #dc2626; font-weight: 600; }
.avd-trend-new { color: #6366f1; font-weight: 600; }
</style>
