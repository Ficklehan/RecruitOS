<template>
  <PageShell title="权重面板">
<div class="job-selector">
      <span class="selector-label">选择岗位：</span>
      <RSelect
        v-model="selectedJob"
        :options="jobSelectOptions"
        placeholder="请选择岗位"
        class="w-[240px]"
        @update:model-value="handleJobChange"
      />
    </div>

    <div class="weight-columns">
      <div class="weight-column">
        <div class="column-header">
          <BarChart3 class="h-4 w-4 text-primary" />
          <span>匹配权重 (matchWeight)</span>
        </div>
        <div class="weight-bars">
          <div v-for="item in currentWeights" :key="'m-' + item.tag" class="weight-bar-item" @click="openAdjustDialog(item)">
            <div class="bar-label">{{ item.tag }}</div>
            <div class="bar-track">
              <div class="bar-fill match" :style="{ width: item.matchWeight * 100 + '%' }" />
            </div>
            <div class="bar-value">{{ (item.matchWeight * 100).toFixed(0) }}%</div>
          </div>
        </div>
      </div>

      <div class="weight-column">
        <div class="column-header">
          <Search class="h-4 w-4 text-green-600" />
          <span>搜索权重 (searchWeight)</span>
        </div>
        <div class="weight-bars">
          <div v-for="item in currentWeights" :key="'s-' + item.tag" class="weight-bar-item" @click="openAdjustDialog(item)">
            <div class="bar-label">{{ item.tag }}</div>
            <div class="bar-track">
              <div class="bar-fill search" :style="{ width: item.searchWeight * 100 + '%' }" />
            </div>
            <div class="bar-value">{{ (item.searchWeight * 100).toFixed(0) }}%</div>
          </div>
        </div>
      </div>

      <div class="weight-column">
        <div class="column-header">
          <LineChart class="h-4 w-4 text-yellow-600" />
          <span>决策权重 (decisionWeight)</span>
        </div>
        <div class="weight-bars">
          <div v-for="item in currentWeights" :key="'d-' + item.tag" class="weight-bar-item" @click="openAdjustDialog(item)">
            <div class="bar-label">{{ item.tag }}</div>
            <div class="bar-track">
              <div class="bar-fill decision" :style="{ width: item.decisionWeight * 100 + '%' }" />
            </div>
            <div class="bar-value">{{ (item.decisionWeight * 100).toFixed(0) }}%</div>
          </div>
        </div>
      </div>
    </div>

    <RCard class="p-4">
      <div class="section-header">
        <span class="section-title">版本历史</span>
      </div>
      <RTable>
        <RTableHead>
          <RTableRow>
            <RTableTh class="w-[120px]">版本号</RTableTh>
            <RTableTh class="w-[180px]">变更时间</RTableTh>
            <RTableTh class="w-[120px]">操作人</RTableTh>
            <RTableTh class="min-w-[200px]">变更说明</RTableTh>
            <RTableTh class="min-w-[200px]">涉及标签</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="row in versionHistory" :key="row.version">
            <RTableCell>{{ row.version }}</RTableCell>
            <RTableCell>{{ row.date }}</RTableCell>
            <RTableCell>{{ row.operator }}</RTableCell>
            <RTableCell class="truncate max-w-[240px]">{{ row.description }}</RTableCell>
            <RTableCell>
              <RBadge v-for="tag in row.changedTags" :key="tag" variant="secondary" class="mr-1 mb-1">{{ tag }}</RBadge>
            </RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </RCard>

    <RDialog v-model:open="adjustDialogVisible">
      <RDialogContent class="max-w-lg">
        <RDialogHeader>
          <RDialogTitle>调整权重</RDialogTitle>
        </RDialogHeader>
        <div v-if="adjustingItem" class="adjust-content">
          <div class="adjust-tag-name">
            <RBadge>{{ adjustingItem.tag }}</RBadge>
          </div>
          <FormField label="匹配权重">
            <div class="slider-row">
              <input v-model.number="adjustForm.matchWeight" type="range" min="0" max="100" step="1" class="slider" />
              <NumberInput v-model="adjustForm.matchWeight" :min="0" :max="100" class="w-20" />
            </div>
          </FormField>
          <FormField label="搜索权重">
            <div class="slider-row">
              <input v-model.number="adjustForm.searchWeight" type="range" min="0" max="100" step="1" class="slider" />
              <NumberInput v-model="adjustForm.searchWeight" :min="0" :max="100" class="w-20" />
            </div>
          </FormField>
          <FormField label="决策权重">
            <div class="slider-row">
              <input v-model.number="adjustForm.decisionWeight" type="range" min="0" max="100" step="1" class="slider" />
              <NumberInput v-model="adjustForm.decisionWeight" :min="0" :max="100" class="w-20" />
            </div>
          </FormField>
        </div>
        <RDialogFooter>
          <RButton variant="outline" @click="adjustDialogVisible = false">取消</RButton>
          <RButton @click="confirmAdjust">确认调整</RButton>
        </RDialogFooter>
      </RDialogContent>
    </RDialog>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, computed, onMounted } from 'vue'
import { BarChart3, Search, LineChart } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import FormField from '@/components/app/FormField.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import { getJobList } from '@/api/modules/job'
import { getJobWeightSnapshot, getJobWeightHistory } from '@/api/modules/evolution'
import {
  RSelect, RBadge, RCard, RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell,
  RDialog, RDialogContent, RDialogHeader, RDialogTitle, RDialogFooter, RButton,
} from '@/components/ui'

interface WeightItem {
  tag: string
  matchWeight: number
  searchWeight: number
  decisionWeight: number
}

interface VersionRecord {
  version: string
  date: string
  operator: string
  description: string
  changedTags: string[]
}

const jobOptions = ref<{ id: number; title: string }[]>([])
const jobSelectOptions = computed(() => jobOptions.value.map((j) => ({ label: j.title, value: j.id })))
const selectedJob = ref<number | undefined>(undefined)
const currentWeights = ref<WeightItem[]>([])
const versionHistory = ref<VersionRecord[]>([])

function parseTagsSnapshot(json?: string): WeightItem[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    if (!Array.isArray(arr)) return []
    return arr.map((item: any) => ({
      tag: item.tag || item.name || '—',
      matchWeight: Number(item.match_weight ?? item.matchWeight ?? 0),
      searchWeight: Number(item.search_weight ?? item.searchWeight ?? 0),
      decisionWeight: Number(item.decision_weight ?? item.decisionWeight ?? 0),
    }))
  } catch {
    return []
  }
}

function extractTags(json?: string): string[] {
  return parseTagsSnapshot(json).map((w) => w.tag)
}

async function loadJobs() {
  const res: any = await getJobList({ pageNum: 1, pageSize: 50, status: 'ACTIVE' })
  const jobs = res?.data?.records ?? res?.records ?? []
  jobOptions.value = jobs.map((j: any) => ({ id: j.id, title: j.title }))
  if (!selectedJob.value && jobOptions.value.length) {
    selectedJob.value = jobOptions.value[0].id
  }
}

async function loadWeights() {
  if (!selectedJob.value) return
  try {
    const snapRes: any = await getJobWeightSnapshot(selectedJob.value)
    const snapshots = snapRes?.data ?? snapRes ?? []
    const latest = Array.isArray(snapshots) ? snapshots[0] : null
    currentWeights.value = parseTagsSnapshot(latest?.tagsSnapshot)

    const histRes: any = await getJobWeightHistory(selectedJob.value)
    const history = histRes?.data ?? histRes ?? []
    versionHistory.value = (Array.isArray(history) ? history : []).map((row: any, idx: number) => ({
      version: row.snapshotType || `v${history.length - idx}`,
      date: row.createdAt ? String(row.createdAt).replace('T', ' ').slice(0, 16) : '—',
      operator: row.snapshotType === 'EVOLUTION' ? '系统自动' : '初始化',
      description: `健康分 ${row.healthScore ?? '—'}${row.signalId ? ` · 信号 #${row.signalId}` : ''}`,
      changedTags: extractTags(row.tagsSnapshot),
    }))
  } catch (e: any) {
    currentWeights.value = []
    versionHistory.value = []
    toast.error(e?.message || '加载权重数据失败')
  }
}

const adjustDialogVisible = ref(false)
const adjustingItem = ref<WeightItem | null>(null)
const adjustForm = reactive({ matchWeight: 50, searchWeight: 50, decisionWeight: 50 })

function openAdjustDialog(item: WeightItem) {
  adjustingItem.value = item
  adjustForm.matchWeight = Math.round(item.matchWeight * 100)
  adjustForm.searchWeight = Math.round(item.searchWeight * 100)
  adjustForm.decisionWeight = Math.round(item.decisionWeight * 100)
  adjustDialogVisible.value = true
}

function confirmAdjust() {
  adjustDialogVisible.value = false
  toast.info('权重调整请通过「招人方式建议」确认流程生效，本页为只读快照')
}

async function handleJobChange() {
  await loadWeights()
}

onMounted(async () => {
  try {
    await loadJobs()
    await loadWeights()
  } catch (e: any) {
    toast.error(e?.message || '加载岗位失败')
  }
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.job-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);

  .selector-label {
    font-size: 14px;
    color: $text-regular;
    font-weight: 500;
    white-space: nowrap;
  }
}

.weight-columns {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;

  @media (max-width: 1200px) { grid-template-columns: 1fr; }
}

.weight-column {
  background: $bg-card;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);

  .column-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid $border-color-light;
  }
}

.weight-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.weight-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
  transition: background-color 0.2s;

  &:hover { background-color: $bg-muted; }

  .bar-label {
    width: 72px;
    font-size: 13px;
    color: $text-regular;
    text-align: right;
    flex-shrink: 0;
  }

  .bar-track {
    flex: 1;
    height: 20px;
    background: $bg-page;
    border-radius: 4px;
    overflow: hidden;
  }

  .bar-fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.6s ease;

    &.match { background: linear-gradient(90deg, $primary-color, $primary-light); }
    &.search { background: linear-gradient(90deg, $success-color, $success-color); }
    &.decision { background: linear-gradient(90deg, $warning-color, $warning-color); }
  }

  .bar-value {
    width: 40px;
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
    text-align: right;
    flex-shrink: 0;
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

.adjust-content .adjust-tag-name {
  text-align: center;
  margin-bottom: 20px;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .slider {
    flex: 1;
    accent-color: $primary-color;
  }
}
</style>
