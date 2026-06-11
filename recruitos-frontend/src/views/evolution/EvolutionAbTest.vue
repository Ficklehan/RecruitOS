<template>
  <PageShell variant="list" title="A/B测试">
    <template #actions>
      <RButton @click="showCreate = true">
        <Plus class="mr-2 h-4 w-4" />
        新建实验
      </RButton>
    </template>

    <template #filters>
      <RSelect
        v-model="statusFilter"
        :options="statusOptions"
        placeholder="实验状态"
        clearable
        class="w-full sm:w-40"
      />
      <RInput
        v-model="searchText"
        placeholder="搜索实验名称"
        class="w-full sm:w-60"
        @keyup.enter="handleSearch"
      />
    </template>

    <template #filterActions>
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </RButton>
      <RButton variant="outline" @click="handleReset">
        <RotateCcw class="mr-2 h-4 w-4" />
        重置
      </RButton>
    </template>

    <RTable v-if="filteredExperiments.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="min-w-[200px]">实验名称</RTableTh>
          <RTableTh class="w-[110px] text-center">类型</RTableTh>
          <RTableTh class="w-[160px]">关联岗位</RTableTh>
          <RTableTh class="w-[100px] text-center">状态</RTableTh>
          <RTableTh class="w-[160px]">开始时间</RTableTh>
          <RTableTh class="w-[160px]">结束时间</RTableTh>
          <RTableTh class="w-[130px] text-center">样本量(A/B)</RTableTh>
          <RTableTh class="w-[180px] text-center">转化率(A vs B)</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in filteredExperiments" :key="row.id">
          <RTableCell>
            <button type="button" class="title-link" @click="openDetail(row)">{{ row.name }}</button>
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(typeTagMap[row.type])">{{ row.type }}</RBadge>
          </RTableCell>
          <RTableCell class="truncate max-w-[160px]">{{ row.jobName }}</RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(statusTagMap[row.status])">{{ statusLabelMap[row.status] }}</RBadge>
          </RTableCell>
          <RTableCell>{{ row.startTime }}</RTableCell>
          <RTableCell>{{ row.endTime || '-' }}</RTableCell>
          <RTableCell class="text-center">{{ row.sampleA }} / {{ row.sampleB }}</RTableCell>
          <RTableCell class="text-center">
            <div class="conversion-cell">
              <span :class="{ 'winner-a': row.conversionA > row.conversionB && row.status === 'COMPLETED' }">{{ row.conversionA }}%</span>
              <span class="vs">vs</span>
              <span :class="{ 'winner-b': row.conversionB > row.conversionA && row.status === 'COMPLETED' }">{{ row.conversionB }}%</span>
              <Trophy
                v-if="row.status === 'COMPLETED'"
                class="winner-icon h-4 w-4"
                :class="row.conversionA > row.conversionB ? 'text-primary' : 'text-green-600'"
              />
            </div>
          </RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta v-else title="暂无实验" description="点击「新建实验」创建 A/B 测试" />

    <template #below>
      <RSheet v-model:open="drawerVisible">
        <RSheetContent class="overflow-y-auto sm:max-w-xl">
          <h3 class="text-lg font-semibold mb-4">实验详情</h3>
          <template v-if="currentExp">
            <div class="detail-section">
              <h4 class="detail-label">基本信息</h4>
              <dl class="detail-grid">
                <div><dt>实验名称</dt><dd>{{ currentExp.name }}</dd></div>
                <div>
                  <dt>类型</dt>
                  <dd><RBadge :variant="elTagTypeToBadge(typeTagMap[currentExp.type])">{{ currentExp.type }}</RBadge></dd>
                </div>
                <div><dt>关联岗位</dt><dd>{{ currentExp.jobName }}</dd></div>
                <div>
                  <dt>状态</dt>
                  <dd><RBadge :variant="elTagTypeToBadge(statusTagMap[currentExp.status])">{{ statusLabelMap[currentExp.status] }}</RBadge></dd>
                </div>
                <div><dt>开始时间</dt><dd>{{ currentExp.startTime }}</dd></div>
                <div><dt>结束时间</dt><dd>{{ currentExp.endTime || '进行中' }}</dd></div>
              </dl>
            </div>

            <div class="detail-section">
              <h4 class="detail-label">变体配置</h4>
              <div class="variant-grid">
                <div class="variant-card variant-a">
                  <div class="variant-header">变体 A (对照组)</div>
                  <pre class="variant-config">{{ formatJson(currentExp.variantA) }}</pre>
                </div>
                <div class="variant-card variant-b">
                  <div class="variant-header">变体 B (实验组)</div>
                  <pre class="variant-config">{{ formatJson(currentExp.variantB) }}</pre>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h4 class="detail-label">转化率对比</h4>
              <div class="conversion-chart">
                <div class="chart-bar-group">
                  <div class="chart-label">变体 A</div>
                  <div class="chart-bar-track">
                    <div class="chart-bar-fill bar-a" :style="{ width: currentExp.conversionA + '%' }" />
                  </div>
                  <div class="chart-value">{{ currentExp.conversionA }}%</div>
                </div>
                <div class="chart-bar-group">
                  <div class="chart-label">变体 B</div>
                  <div class="chart-bar-track">
                    <div class="chart-bar-fill bar-b" :style="{ width: currentExp.conversionB + '%' }" />
                  </div>
                  <div class="chart-value">{{ currentExp.conversionB }}%</div>
                </div>
                <div class="chart-summary">
                  样本量：A组 {{ currentExp.sampleA }} 人 / B组 {{ currentExp.sampleB }} 人
                </div>
                <div v-if="currentExp.status === 'COMPLETED'" class="chart-winner">
                  <Trophy class="h-4 w-4" />
                  <span>{{ currentExp.conversionA > currentExp.conversionB ? '变体 A' : '变体 B' }} 胜出 （提升 {{ Math.abs(currentExp.conversionA - currentExp.conversionB).toFixed(1) }}%）</span>
                </div>
              </div>
            </div>
          </template>
        </RSheetContent>
      </RSheet>

      <RDialog v-model:open="showCreate">
        <RDialogContent class="max-w-2xl">
          <RDialogHeader>
            <RDialogTitle>新建实验</RDialogTitle>
          </RDialogHeader>
          <div class="grid gap-4 py-2">
            <FormField label="实验名称" required>
              <RInput v-model="createForm.name" placeholder="请输入实验名称" />
            </FormField>
            <FormField label="实验类型" required>
              <RSelect v-model="createForm.type" :options="typeOptions" placeholder="请选择类型" />
            </FormField>
            <FormField label="关联岗位" required>
              <RSelect v-model="createForm.jobId" :options="jobSelectOptions" placeholder="请选择岗位" />
            </FormField>
            <FormField label="变体A配置">
              <RTextarea v-model="createForm.variantA" :rows="5" placeholder='请输入 JSON 配置，例如：{"weight": {"vue": 0.85}}' />
            </FormField>
            <FormField label="变体B配置">
              <RTextarea v-model="createForm.variantB" :rows="5" placeholder='请输入 JSON 配置，例如：{"weight": {"vue": 0.90}}' />
            </FormField>
          </div>
          <RDialogFooter>
            <RButton variant="outline" @click="showCreate = false">取消</RButton>
            <RButton @click="handleCreate">创建实验</RButton>
          </RDialogFooter>
        </RDialogContent>
      </RDialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, Plus, RotateCcw, Trophy } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import FormField from '@/components/app/FormField.vue'
import { getJobList } from '@/api/modules/job'
import {
  getAbTestList, createAbTest, startAbTest, stopAbTest,
} from '@/api/modules/evolution'
import {
  RButton, RInput, RSelect, RBadge, RTextarea,
  RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell,
  RDialog, RDialogContent, RDialogHeader, RDialogTitle, RDialogFooter,
  RSheet, RSheetContent,
} from '@/components/ui'

const statusFilter = ref<string | undefined>()
const searchText = ref('')
const loading = ref(false)

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '运行中', value: 'RUNNING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
]

const typeOptions = [
  { label: 'WEIGHT - 权重优化', value: 'WEIGHT' },
  { label: 'MATCH - 匹配算法', value: 'MATCH' },
  { label: 'SEARCH - 搜索策略', value: 'SEARCH' },
]

const jobOptions = ref<{ id: number; title: string }[]>([])
const jobSelectOptions = computed(() => jobOptions.value.map((j) => ({ label: j.title, value: j.id })))

const experiments = ref<any[]>([])

const filteredExperiments = computed(() => experiments.value)

const drawerVisible = ref(false)
const currentExp = ref<any>(null)
const showCreate = ref(false)
const createForm = ref({ name: '', type: '' as string | undefined, jobId: null as number | null, variantA: '', variantB: '' })

const typeTagMap: Record<string, string> = { WEIGHT: '', MATCH: 'success', SEARCH: 'warning' }
const statusTagMap: Record<string, string> = { DRAFT: 'info', RUNNING: '', COMPLETED: 'success', CANCELLED: 'danger' }
const statusLabelMap: Record<string, string> = { DRAFT: '草稿', RUNNING: '运行中', COMPLETED: '已完成', CANCELLED: '已取消' }

function parseVariant(raw: any) {
  if (!raw) return {}
  if (typeof raw === 'object') return raw
  try { return JSON.parse(raw) } catch { return { raw } }
}

function mapRow(vo: any) {
  return {
    id: vo.id,
    name: vo.testName,
    type: vo.testType,
    jobName: vo.jobTitle || `岗位#${vo.jobId}`,
    status: vo.status,
    startTime: vo.startDate || '—',
    endTime: vo.endDate || null,
    sampleA: vo.sampleSizeA ?? 0,
    sampleB: vo.sampleSizeB ?? 0,
    conversionA: vo.conversionRateA ?? 0,
    conversionB: vo.conversionRateB ?? 0,
    variantA: parseVariant(vo.variantA),
    variantB: parseVariant(vo.variantB),
  }
}

function formatJson(obj: any) {
  return JSON.stringify(obj, null, 2)
}

async function loadJobs() {
  const res: any = await getJobList({ pageNum: 1, pageSize: 50, status: 'ACTIVE' })
  const jobs = res?.data?.records ?? res?.records ?? []
  jobOptions.value = jobs.map((j: any) => ({ id: j.id, title: j.title }))
}

async function loadExperiments() {
  loading.value = true
  try {
    const params: any = { pageNum: 1, pageSize: 50 }
    if (statusFilter.value) params.status = statusFilter.value
    if (searchText.value) params.testName = searchText.value
    const res: any = await getAbTestList(params)
    const page = res.data ?? res
    const rows = page.records || page.list || []
    experiments.value = (Array.isArray(rows) ? rows : []).map(mapRow)
  } catch (e: any) {
    experiments.value = []
    toast.error(e?.message || '加载实验失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadExperiments()
}

function handleReset() {
  statusFilter.value = undefined
  searchText.value = ''
  loadExperiments()
}

function openDetail(row: any) {
  currentExp.value = row
  drawerVisible.value = true
}

async function handleStart(row: any) {
  const ok = await confirm({ title: '启动实验', message: `确定要启动实验「${row.name}」吗？` })
  if (!ok) return
  await startAbTest(row.id)
  toast.success('实验已启动')
  loadExperiments()
}

async function handleStop(row: any) {
  const winner: 'A' | 'B' = row.conversionA >= row.conversionB ? 'A' : 'B'
  const ok = await confirm({
    title: '停止实验',
    message: `确定停止实验「${row.name}」并宣布变体 ${winner} 胜出？`,
    destructive: true,
  })
  if (!ok) return
  await stopAbTest(row.id, winner)
  toast.success('实验已停止')
  loadExperiments()
}

async function handleCreate() {
  if (!createForm.value.name || !createForm.value.type || !createForm.value.jobId) {
    toast.error('请填写必填项')
    return
  }
  const variantA = createForm.value.variantA || '{}'
  const variantB = createForm.value.variantB || '{}'
  try { JSON.parse(variantA); JSON.parse(variantB) } catch {
    toast.error('变体配置 JSON 格式错误')
    return
  }
  const job = jobOptions.value.find((j) => j.id === createForm.value.jobId)
  await createAbTest({
    testName: createForm.value.name,
    testType: createForm.value.type,
    jobId: createForm.value.jobId,
    jobTitle: job?.title,
    variantA,
    variantB,
  })
  showCreate.value = false
  createForm.value = { name: '', type: undefined, jobId: null, variantA: '', variantB: '' }
  toast.success('实验创建成功')
  loadExperiments()
}

function getRowActions(row: any) {
  const actions: any[] = [{ command: 'view', label: '查看详情', icon: 'View', type: 'primary', primary: true }]
  if (row.status === 'DRAFT') actions.push({ command: 'start', label: '启动', icon: 'VideoPlay' })
  if (row.status === 'RUNNING') actions.push({ command: 'stop', label: '停止', icon: 'VideoPause', divided: true })
  return actions
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view' || cmd === 'edit') openDetail(row)
  else if (cmd === 'start') handleStart(row)
  else if (cmd === 'stop') handleStop(row)
}

onMounted(async () => {
  await loadJobs()
  await loadExperiments()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.title-link {
  color: $primary-color;
  cursor: pointer;
  font-weight: 500;
  background: none;
  border: none;
  padding: 0;
  text-align: left;

  &:hover { text-decoration: underline; }
}

.conversion-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;

  .vs { color: $text-placeholder; font-size: 12px; }
  .winner-a { color: $primary-color; font-weight: 700; }
  .winner-b { color: $success-color; font-weight: 700; }
  .winner-icon { margin-left: 2px; }
}

.detail-section {
  margin-bottom: 28px;

  .detail-label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid $bg-muted;
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  font-size: 14px;

  div { display: flex; flex-direction: column; gap: 4px; }
  dt { color: $text-secondary; font-size: 12px; }
  dd { margin: 0; }
}

.variant-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.variant-card {
  border-radius: 8px;
  overflow: hidden;
  border: 2px dashed $border-color-light;

  .variant-header {
    padding: 10px 16px;
    font-size: 13px;
    font-weight: 600;
    color: var(--r-bg-card);
  }

  &.variant-a .variant-header { background: $primary-color; }
  &.variant-b .variant-header { background: $success-color; }

  .variant-config {
    margin: 0;
    padding: 12px 16px;
    font-size: 12px;
    font-family: SF Mono, Monaco, Menlo, monospace;
    color: $text-regular;
    background: $bg-muted;
    line-height: 1.6;
    overflow-x: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.conversion-chart {
  .chart-bar-group {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    .chart-label { width: 56px; font-size: 13px; color: $text-regular; text-align: right; flex-shrink: 0; }
    .chart-bar-track { flex: 1; height: 28px; background: $bg-page; border-radius: 4px; overflow: hidden; }
    .chart-bar-fill {
      height: 100%;
      border-radius: 4px;
      transition: width 0.6s ease;
      &.bar-a { background: linear-gradient(90deg, $primary-color, #93C5FD); }
      &.bar-b { background: linear-gradient(90deg, $success-color, $success-color); }
    }
    .chart-value { width: 50px; font-size: 14px; font-weight: 700; color: $text-primary; flex-shrink: 0; }
  }

  .chart-summary { margin-top: 16px; font-size: 13px; color: $text-secondary; text-align: center; }

  .chart-winner {
    margin-top: 12px;
    padding: 10px 16px;
    background: $success-lighter;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 14px;
    font-weight: 600;
    color: $success-color;
  }
}
</style>
