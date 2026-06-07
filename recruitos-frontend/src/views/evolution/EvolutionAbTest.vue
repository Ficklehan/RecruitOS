<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">A/B测试</h2>
      <el-button type="primary" @click="showCreate = true">
        <el-icon><Plus /></el-icon>
        新建实验
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="statusFilter" placeholder="实验状态" clearable style="width: 150px">
        <el-option label="全部" value="" />
        <el-option label="草稿" value="DRAFT" />
        <el-option label="运行中" value="RUNNING" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-input v-model="searchText" placeholder="搜索实验名称" :prefix-icon="Search" clearable style="width: 240px" />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">
        <el-icon><RefreshLeft /></el-icon>
        重置
      </el-button>
    </div>

    <div class="data-card">
      <el-table :data="filteredExperiments" stripe style="width: 100%">
        <el-table-column prop="name" label="实验名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-link" @click="openDetail(row)">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type]" size="small" disable-transitions>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jobName" label="关联岗位" width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status]" size="small" disable-transitions>{{ statusLabelMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160">
          <template #default="{ row }">{{ row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="样本量(A/B)" width="130" align="center">
          <template #default="{ row }">
            <span>{{ row.sampleA }} / {{ row.sampleB }}</span>
          </template>
        </el-table-column>
        <el-table-column label="转化率(A vs B)" width="180" align="center">
          <template #default="{ row }">
            <div class="conversion-cell">
              <span :class="{ 'winner-a': row.conversionA > row.conversionB && row.status === 'COMPLETED' }">{{ row.conversionA }}% </span>
              <span class="vs">vs</span>
              <span :class="{ 'winner-b': row.conversionB > row.conversionA && row.status === 'COMPLETED' }">{{ row.conversionB }}% </span>
              <el-icon v-if="row.status === 'COMPLETED'" class="winner-icon" :color="row.conversionA > row.conversionB ? '#3B82F6' : '#059669'">
                <Trophy />
              </el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT'" type="success" link size="small" @click="handleStart(row)"> 启动 </el-button>
            <el-button v-if="row.status === 'RUNNING'" type="danger" link size="small" @click="handleStop(row)"> 停止 </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Detail Drawer -->
    <el-drawer v-model="drawerVisible" title="实验详情" size="600px">
      <template v-if="currentExp">
        <div class="detail-section">
          <h4 class="detail-label">基本信息</h4>
          <el-descriptions :column="2" size="small">
            <el-descriptions-item label="实验名称">{{ currentExp.name }}</el-descriptions-item>
            <el-descriptions-item label="类型">
              <el-tag :type="typeTagMap[currentExp.type]" size="small">{{ currentExp.type }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="关联岗位">{{ currentExp.jobName }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagMap[currentExp.status]" size="small">{{ statusLabelMap[currentExp.status] }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ currentExp.startTime }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ currentExp.endTime || '进行中' }}</el-descriptions-item>
          </el-descriptions>
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
                <div class="chart-bar-fill bar-a" :style="{ width: currentExp.conversionA + '%' }"></div>
              </div>
              <div class="chart-value">{{ currentExp.conversionA }}%</div>
            </div>
            <div class="chart-bar-group">
              <div class="chart-label">变体 B</div>
              <div class="chart-bar-track">
                <div class="chart-bar-fill bar-b" :style="{ width: currentExp.conversionB + '%' }"></div>
              </div>
              <div class="chart-value">{{ currentExp.conversionB }}%</div>
            </div>
            <div class="chart-summary">
              样本量：A组 {{ currentExp.sampleA }} 人 / B组 {{ currentExp.sampleB }} 人
            </div>
            <div v-if="currentExp.status === 'COMPLETED'" class="chart-winner">
              <el-icon><Trophy /></el-icon>
              <span>{{ currentExp.conversionA > currentExp.conversionB ? '变体 A' : '变体 B' }} 胜出 （提升 {{ Math.abs(currentExp.conversionA - currentExp.conversionB).toFixed(1) }}%）</span>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreate" title="新建实验" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="实验名称" required>
          <el-input v-model="createForm.name" placeholder="请输入实验名称" />
        </el-form-item>
        <el-form-item label="实验类型" required>
          <el-select v-model="createForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="WEIGHT - 权重优化" value="WEIGHT" />
            <el-option label="MATCH - 匹配算法" value="MATCH" />
            <el-option label="SEARCH - 搜索策略" value="SEARCH" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联岗位" required>
          <el-select v-model="createForm.jobId" placeholder="请选择岗位" style="width: 100%">
            <el-option v-for="job in jobOptions" :key="job.id" :label="job.title" :value="job.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="变体A配置">
          <el-input v-model="createForm.variantA" type="textarea" :rows="5" placeholder='请输入 JSON 配置，例如：{"weight": {"vue": 0.85}}' />
        </el-form-item>
        <el-form-item label="变体B配置">
          <el-input v-model="createForm.variantB" type="textarea" :rows="5" placeholder='请输入 JSON 配置，例如：{"weight": {"vue": 0.90}}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建实验</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, RefreshLeft, Trophy } from '@element-plus/icons-vue'

const statusFilter = ref('')
const searchText = ref('')
const jobOptions = ref([
  { id: 1, title: '高级前端工程师' },
  { id: 2, title: 'Java后端工程师' },
  { id: 3, title: '产品经理' },
  { id: 4, title: 'UI设计师' },
])

const experiments = ref([
  {
    id: 1,
    name: '前端岗位权重优化实验',
    type: 'WEIGHT',
    jobName: '高级前端工程师',
    status: 'RUNNING',
    startTime: '2026-05-20 10:00',
    endTime: null,
    sampleA: 342,
    sampleB: 338,
    conversionA: 23.4,
    conversionB: 27.1,
    variantA: { matchWeight: { 'Vue.js': 0.85, React: 0.72, TypeScript: 0.8 }, searchWeight: { 'Vue.js': 0.78, React: 0.65, TypeScript: 0.75 } },
    variantB: { matchWeight: { 'Vue.js': 0.9, React: 0.68, TypeScript: 0.85 }, searchWeight: { 'Vue.js': 0.82, React: 0.6, TypeScript: 0.8 } },
  },
  {
    id: 2,
    name: '搜索权重A/B测试',
    type: 'SEARCH',
    jobName: 'Java后端工程师',
    status: 'COMPLETED',
    startTime: '2026-05-01 09:00',
    endTime: '2026-05-15 18:00',
    sampleA: 520,
    sampleB: 515,
    conversionA: 31.2,
    conversionB: 35.8,
    variantA: { searchStrategy: 'default', boostExactMatch: false, maxResults: 50 },
    variantB: { searchStrategy: 'weighted', boostExactMatch: true, maxResults: 50 },
  },
  {
    id: 3,
    name: '匹配算法对比实验',
    type: 'MATCH',
    jobName: '产品经理',
    status: 'DRAFT',
    startTime: '-',
    endTime: null,
    sampleA: 0,
    sampleB: 0,
    conversionA: 0,
    conversionB: 0,
    variantA: { algorithm: 'cosine_similarity', threshold: 0.7, topK: 10 },
    variantB: { algorithm: 'hybrid_score', threshold: 0.65, topK: 10 },
  },
  {
    id: 4,
    name: '旧版权重测试',
    type: 'WEIGHT',
    jobName: 'UI设计师',
    status: 'CANCELLED',
    startTime: '2026-04-10 14:00',
    endTime: '2026-04-12 09:00',
    sampleA: 85,
    sampleB: 80,
    conversionA: 18.5,
    conversionB: 17.2,
    variantA: { matchWeight: { Figma: 0.8, Sketch: 0.7 } },
    variantB: { matchWeight: { Figma: 0.85, Sketch: 0.65 } },
  },
])

const filteredExperiments = computed(() => {
  let list = experiments.value
  if (statusFilter.value) {
    list = list.filter(e => e.status === statusFilter.value)
  }
  if (searchText.value) {
    const q = searchText.value.toLowerCase()
    list = list.filter(e => e.name.toLowerCase().includes(q))
  }
  return list
})

const drawerVisible = ref(false)
const currentExp = ref<any>(null)
const showCreate = ref(false)
const createForm = ref({ name: '', type: '', jobId: null as number | null, variantA: '', variantB: '' })

const typeTagMap: Record<string, string> = { WEIGHT: '', MATCH: 'success', SEARCH: 'warning' }
const statusTagMap: Record<string, string> = { DRAFT: 'info', RUNNING: '', COMPLETED: 'success', CANCELLED: 'danger' }
const statusLabelMap: Record<string, string> = { DRAFT: '草稿', RUNNING: '运行中', COMPLETED: '已完成', CANCELLED: '已取消' }

function formatJson(obj: any) {
  return JSON.stringify(obj, null, 2)
}

function handleSearch() {}

function handleReset() {
  statusFilter.value = ''
  searchText.value = ''
}

function openDetail(row: any) {
  currentExp.value = row
  drawerVisible.value = true
}

function handleStart(row: any) {
  ElMessageBox.confirm(`确定要启动实验「${row.name}」吗？`, '启动实验', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info',
  }).then(() => {
    row.status = 'RUNNING'
    row.startTime = new Date().toLocaleString('zh-CN')
    ElMessage.success('实验已启动')
  }).catch(() => {})
}

function handleStop(row: any) {
  ElMessageBox.confirm(`确定要停止实验「${row.name}」吗？停止后不可恢复。`, '停止实验', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    row.status = 'COMPLETED'
    row.endTime = new Date().toLocaleString('zh-CN')
    ElMessage.success('实验已停止')
  }).catch(() => {})
}

function handleCreate() {
  if (!createForm.value.name || !createForm.value.type || !createForm.value.jobId) {
    ElMessage.warning('请填写必填项')
    return
  }
  let variantA = {}
  let variantB = {}
  try {
    if (createForm.value.variantA) variantA = JSON.parse(createForm.value.variantA)
  } catch {
    ElMessage.error('变体A配置 JSON 格式错误')
    return
  }
  try {
    if (createForm.value.variantB) variantB = JSON.parse(createForm.value.variantB)
  } catch {
    ElMessage.error('变体B配置 JSON 格式错误')
    return
  }
  const job = jobOptions.value.find(j => j.id === createForm.value.jobId)
  experiments.value.unshift({
    id: Date.now(),
    name: createForm.value.name,
    type: createForm.value.type,
    jobName: job?.title || '',
    status: 'DRAFT',
    startTime: '-',
    endTime: null,
    sampleA: 0,
    sampleB: 0,
    conversionA: 0,
    conversionB: 0,
    variantA,
    variantB,
  })
  showCreate.value = false
  createForm.value = { name: '', type: '', jobId: null, variantA: '', variantB: '' }
  ElMessage.success('实验创建成功')
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.title-link {
  color: $primary-color;
  cursor: pointer;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
}

.conversion-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;

  .vs {
    color: $text-placeholder;
    font-size: 12px;
  }

  .winner-a {
    color: $primary-color;
    font-weight: 700;
  }

  .winner-b {
    color: $success-color;
    font-weight: 700;
  }

  .winner-icon {
    margin-left: 2px;
  }
}

.detail-section {
  margin-bottom: 28px;

  &:last-child {
    margin-bottom: 0;
  }

  .detail-label {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid $bg-muted;
  }
}

.variant-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.variant-card {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid $bg-muted;

  .variant-header {
    padding: 10px 16px;
    font-size: 13px;
    font-weight: 600;
    color: #fff;
  }

  &.variant-a .variant-header {
    background: $primary-color;
  }

  &.variant-b .variant-header {
    background: $success-color;
  }

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

    .chart-label {
      width: 56px;
      font-size: 13px;
      color: $text-regular;
      text-align: right;
      flex-shrink: 0;
    }

    .chart-bar-track {
      flex: 1;
      height: 28px;
      background: #f0f2f5;
      border-radius: 4px;
      overflow: hidden;
    }

    .chart-bar-fill {
      height: 100%;
      border-radius: 4px;
      transition: width 0.6s ease;

      &.bar-a {
        background: linear-gradient(90deg, #3B82F6, #93C5FD);
      }

      &.bar-b {
        background: linear-gradient(90deg, #059669, #059669);
      }
    }

    .chart-value {
      width: 50px;
      font-size: 14px;
      font-weight: 700;
      color: $text-primary;
      flex-shrink: 0;
    }
  }

  .chart-summary {
    margin-top: 16px;
    font-size: 13px;
    color: $text-secondary;
    text-align: center;
  }

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
