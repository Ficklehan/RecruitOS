<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">自动招聘任务</h2>
        <p class="page-subtitle">由在招职位的渠道招聘自动创建，无需手动建任务</p>
      </div>
      <el-button type="primary" @click="$router.push('/planning/jobs')">
        <el-icon><Plus /></el-icon>
        去职位开始渠道招聘
      </el-button>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
      title="推荐流程：在招职位工作台 → 渠道招聘，选择平台并启动后系统自动执行发布职位、搜寻候选人、打招呼与加入候选人。"
    />

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-running">
        <div class="stat-icon"><el-icon :size="28"><VideoPlay /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.running }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </div>
      <div class="stat-card stat-paused">
        <div class="stat-icon"><el-icon :size="28"><VideoPause /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.paused }}</div>
          <div class="stat-label">已暂停</div>
        </div>
      </div>
      <div class="stat-card stat-done">
        <div class="stat-icon"><el-icon :size="28"><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.done }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card stat-fail">
        <div class="stat-icon"><el-icon :size="28"><CircleClose /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.fail }}</div>
          <div class="stat-label">失败数</div>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索任务关键词"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.status" placeholder="任务状态" clearable style="width: 140px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="queryParams.taskType" placeholder="任务类型" clearable style="width: 140px">
        <el-option v-for="t in taskTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">
        <el-icon><RefreshRight /></el-icon>
        重置
      </el-button>
    </div>

    <!-- 数据表格 -->
    <div class="data-card">
      <el-table :data="filteredTasks" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="taskType" label="任务类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTaskTypeTag(row.taskType)" size="small" disable-transitions>
              {{ getTaskTypeLabel(row.taskType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jobTitle" label="在招职位" min-width="180" show-overflow-tooltip />
        <el-table-column prop="platform" label="平台" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getPlatformTag(row.platform)" size="small" disable-transitions>
              {{ row.platform }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small" disable-transitions>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getPriorityTag(row.priority)" size="small" disable-transitions>
              P{{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="200">
          <template #default="{ row }">
            <div class="progress-cell">
              <span class="progress-text">{{ row.completedCount }}/{{ row.targetCount }}</span>
              <el-progress
                :percentage="Math.round((row.completedCount / row.targetCount) * 100)"
                :stroke-width="6"
                :show-text="false"
                :color="getProgressColor(row.status)"
                style="flex: 1"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PAUSED' || row.status === 'PENDING'"
              type="success" link size="small"
              @click="handleStart(row)"
            >
              <el-icon><VideoPlay /></el-icon>启动
            </el-button>
            <el-button
              v-if="row.status === 'RUNNING'"
              type="warning" link size="small"
              @click="handlePause(row)"
            >
              <el-icon><VideoPause /></el-icon>暂停
            </el-button>
            <el-button type="primary" link size="small" @click="handleView(row)">
              <el-icon><View /></el-icon>查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>

    <!-- 任务详情 -->
    <el-drawer v-model="detailVisible" title="任务详情" size="520px" destroy-on-close>
      <template v-if="taskDetail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务类型">{{ getTaskTypeLabel(taskDetail.taskType) }}</el-descriptions-item>
          <el-descriptions-item label="在招职位">{{ taskDetail.jobTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ taskDetail.platform || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getStatusLabel(taskDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="进度">{{ taskDetail.completedCount }}/{{ taskDetail.targetCount }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ taskDetail.startedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="taskDetail.errorMessage" label="错误">{{ taskDetail.errorMessage }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 20px 0 12px">最近日志</h4>
        <el-timeline v-if="taskDetail.recentLogs?.length">
          <el-timeline-item
            v-for="log in taskDetail.recentLogs"
            :key="log.id"
            :type="log.success ? 'success' : 'danger'"
            :timestamp="log.createdAt"
          >
            {{ log.action || log.step }} — {{ log.message || (log.success ? '成功' : '失败') }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无日志" />
        <div v-if="taskDetail.jobId" style="margin-top: 16px">
          <el-button type="primary" @click="$router.push({ path: `/planning/jobs/${taskDetail.jobId}`, query: { tab: 'sourcing' } })">
            查看渠道招聘
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 高级：手动建任务（一般不需要） -->
    <el-dialog v-model="showCreateDialog" title="手动创建任务" width="520px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="createForm.taskType" placeholder="请选择任务类型" style="width: 100%">
            <el-option v-for="t in taskTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="在招职位" prop="jobId">
          <el-select v-model="createForm.jobId" placeholder="请选择在招职位" style="width: 100%">
            <el-option v-for="j in jobOptions" :key="j.id" :label="j.title" :value="j.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="招聘账号" prop="agentAccountId">
          <el-select v-model="createForm.agentAccountId" placeholder="请选择招聘平台账号" style="width: 100%">
            <el-option v-for="a in accountOptions" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="createForm.priority">
            <el-radio-button :value="1">
              <el-tag type="danger" size="small" disable-transitions>P1 高</el-tag>
            </el-radio-button>
            <el-radio-button :value="2">
              <el-tag type="warning" size="small" disable-transitions>P2 中</el-tag>
            </el-radio-button>
            <el-radio-button :value="3">
              <el-tag type="info" size="small" disable-transitions>P3 低</el-tag>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">创建任务</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Search, Plus, RefreshRight, VideoPlay, VideoPause,
  CircleCheck, CircleClose, View,
} from '@element-plus/icons-vue'
import {
  getAgentTaskList, getAgentAccountList, createAgentTask,
  startAgentTask, pauseAgentTask, resumeAgentTask, getAgentTaskDetail,
} from '@/api/modules/agent'

// 查询参数
const queryParams = reactive({
  keyword: '',
  status: '',
  taskType: '',
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const showCreateDialog = ref(false)
const detailVisible = ref(false)
const taskDetail = ref<any>(null)
const createFormRef = ref<FormInstance>()

// 选项
const statusOptions = [
  { label: '全部', value: '' },
  { label: '待执行', value: 'PENDING' },
  { label: '运行中', value: 'RUNNING' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已完成', value: 'DONE' },
  { label: '失败', value: 'FAILED' },
]

const taskTypeOptions = [
  { label: '全部', value: '' },
  { label: '搜索', value: 'SEARCH' },
  { label: '联系', value: 'CONTACT' },
  { label: '跟进', value: 'FOLLOWUP' },
  { label: '安排', value: 'SCHEDULE' },
]

const jobOptions = ref<any[]>([])
const accountOptions = ref<any[]>([])
const taskList = ref<any[]>([])

// 创建表单
const createForm = reactive({
  taskType: '',
  jobId: '',
  agentAccountId: '',
  priority: 2,
})

const createRules: FormRules = {
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  jobId: [{ required: true, message: '请选择在招职位', trigger: 'change' }],
  agentAccountId: [{ required: true, message: '请选择招聘账号', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
}

// 统计
const stats = computed(() => {
  const all = taskList.value
  return {
    running: all.filter(t => t.status === 'RUNNING').length,
    paused: all.filter(t => t.status === 'PAUSED').length,
    done: all.filter(t => t.status === 'DONE').length,
    fail: all.filter(t => t.status === 'FAILED').length,
  }
})

// 过滤
const filteredTasks = computed(() => {
  let list = [...taskList.value]
  if (queryParams.keyword) {
    const kw = queryParams.keyword.toLowerCase()
    list = list.filter(t => (t.jobTitle || '').toLowerCase().includes(kw))
  }
  if (queryParams.status) {
    list = list.filter(t => t.status === queryParams.status)
  }
  if (queryParams.taskType) {
    list = list.filter(t => t.taskType === queryParams.taskType)
  }
  total.value = list.length
  const start = (queryParams.pageNum - 1) * queryParams.pageSize
  return list.slice(start, start + queryParams.pageSize)
})

// 标签映射
function getTaskTypeTag(type: string) {
  const map: Record<string, string> = { SEARCH: '', CONTACT: 'success', FOLLOWUP: 'warning', SCHEDULE: 'info' }
  return map[type] || 'info'
}

function getTaskTypeLabel(type: string) {
  const map: Record<string, string> = { SEARCH: '搜索', CONTACT: '联系', FOLLOWUP: '跟进', SCHEDULE: '安排' }
  return map[type] || type
}

function getPlatformTag(p: string) {
  const map: Record<string, string> = { BOSS: '', LAGOU: 'success', ZHILIAN: 'warning', LIEPIN: 'info' }
  return map[p] || 'info'
}

function getStatusTag(status: string) {
  const map: Record<string, string> = {
    PENDING: 'info', RUNNING: '', PAUSED: 'warning', DONE: 'success', FAILED: 'danger',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待执行', RUNNING: '运行中', PAUSED: '已暂停', DONE: '已完成', FAILED: '失败',
  }
  return map[status] || status
}

function getPriorityTag(p: number) {
  const map: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'info' }
  return map[p] || 'info'
}

function getProgressColor(status: string) {
  if (status === 'FAILED') return '#DC2626'
  if (status === 'PAUSED') return '#D97706'
  return '#3B82F6'
}

// 操作
function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.taskType = ''
  handleSearch()
}

function handleStart(row: any) {
  ElMessageBox.confirm('确定要启动该任务吗？', '启动确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info',
  }).then(async () => {
    try {
      const api = row.status === 'PAUSED' ? resumeAgentTask : startAgentTask
      await api(row.id)
      ElMessage.success('任务已启动')
      loadData()
    } catch {
      ElMessage.error('启动失败')
    }
  }).catch(() => {})
}

function handlePause(row: any) {
  ElMessageBox.confirm('确定要暂停该任务吗？', '暂停确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await pauseAgentTask(row.id)
      ElMessage.success('任务已暂停')
      loadData()
    } catch {
      ElMessage.error('暂停失败')
    }
  }).catch(() => {})
}

async function handleView(row: any) {
  try {
    const res: any = await getAgentTaskDetail(row.id)
    taskDetail.value = res.data || res
    detailVisible.value = true
  } catch {
    ElMessage.error('加载任务详情失败')
  }
}

async function handleCreateSubmit() {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await createAgentTask({
        taskType: createForm.taskType,
        jobId: createForm.jobId,
        agentAccountId: createForm.agentAccountId,
        priority: createForm.priority,
      })
      ElMessage.success('任务创建成功')
      showCreateDialog.value = false
      createForm.taskType = ''
      createForm.jobId = ''
      createForm.agentAccountId = ''
      createForm.priority = 2
      loadData()
    } catch {
      // error handled by interceptor
    }
  })
}

async function loadData() {
  try {
    const res: any = await getAgentTaskList({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      status: queryParams.status || undefined,
      taskType: queryParams.taskType || undefined,
      keyword: queryParams.keyword || undefined,
    })
    taskList.value = res.data?.list || res.data?.records || res.data || []
    total.value = res.data?.total || taskList.value.length
  } catch {
    taskList.value = []
    total.value = 0
  }
}

async function loadOptions() {
  try {
    const [taskRes, accountRes] = await Promise.all([
      getAgentTaskList({ pageSize: 1 }),
      getAgentAccountList({ pageSize: 100 }),
    ])
    // Extract unique jobs from tasks for job options
    const tasks = taskRes.data?.list || taskRes.data?.records || []
    const jobMap = new Map()
    tasks.forEach((t: any) => {
      if (t.jobId && t.jobTitle) jobMap.set(t.jobId, { id: t.jobId, title: t.jobTitle })
    })
    jobOptions.value = Array.from(jobMap.values())
    accountOptions.value = (accountRes.data?.list || accountRes.data?.records || accountRes.data || []).map((a: any) => ({
      id: a.id,
      name: a.accountName || a.name,
    }))
  } catch {
    // options stay empty
  }
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-running .stat-icon { background: linear-gradient(135deg, $primary-color, $primary-light); }
.stat-paused .stat-icon { background: linear-gradient(135deg, $warning-color, $warning-color); }
.stat-done .stat-icon { background: linear-gradient(135deg, $success-color, $success-color); }
.stat-fail .stat-icon { background: linear-gradient(135deg, $danger-color, $danger-color); }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 4px;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-size: 12px;
  color: $text-regular;
  white-space: nowrap;
  min-width: 50px;
}
</style>
