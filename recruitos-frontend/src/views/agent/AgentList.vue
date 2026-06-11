<template>
  <PageShell variant="list"
    title="自动招聘任务"
    subtitle="由在招职位的渠道招聘自动创建，无需手动建任务"
  >
    <template #actions>
      <RButton @click="$router.push('/planning/jobs')">
        <Plus class="mr-2 h-4 w-4" />
        去职位开始渠道招聘
      </RButton>
    </template>

    <template #toolbar>
      <RAlert variant="default">
        <RAlertDescription>
          推荐流程：在招职位工作台 → 渠道招聘，选择平台并启动后系统自动执行发布职位、搜寻候选人、打招呼与加入候选人。
        </RAlertDescription>
      </RAlert>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="stat-card stat-running">
          <div class="stat-icon"><Video class="h-7 w-7" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.running }}</div>
            <div class="stat-label">运行中</div>
          </div>
        </div>
        <div class="stat-card stat-paused">
          <div class="stat-icon"><Pause class="h-7 w-7" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.paused }}</div>
            <div class="stat-label">已暂停</div>
          </div>
        </div>
        <div class="stat-card stat-done">
          <div class="stat-icon"><CircleCheck class="h-7 w-7" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.done }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
        <div class="stat-card stat-fail">
          <div class="stat-icon"><CircleX class="h-7 w-7" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.fail }}</div>
            <div class="stat-label">失败数</div>
          </div>
        </div>
      </div>
    </template>

    <template #filters>
      <RInput
        v-model="queryParams.keyword"
        placeholder="搜索任务关键词"
        class="w-full sm:w-60"
        @keyup.enter="handleSearch"
      />
      <RSelect
        v-model="queryParams.status"
        :options="statusOptions"
        placeholder="任务状态"
        clearable
        class="w-full sm:w-36"
      />
      <RSelect
        v-model="queryParams.taskType"
        :options="taskTypeOptions"
        placeholder="任务类型"
        clearable
        class="w-full sm:w-36"
      />
    </template>

    <template #filterActions>
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </RButton>
      <RButton variant="outline" @click="handleReset">
        <RefreshCw class="mr-2 h-4 w-4" />
        重置
      </RButton>
    </template>

    <RTable v-if="filteredTasks.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[120px] text-center">任务类型</RTableTh>
          <RTableTh class="min-w-[180px]">在招职位</RTableTh>
          <RTableTh class="w-[110px] text-center">平台</RTableTh>
          <RTableTh class="w-[100px] text-center">状态</RTableTh>
          <RTableTh class="w-[90px] text-center">优先级</RTableTh>
          <RTableTh class="w-[200px]">进度</RTableTh>
          <RTableTh class="w-[170px]">开始时间</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in filteredTasks" :key="row.id">
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(getTaskTypeTag(row.taskType))">
              {{ getTaskTypeLabel(row.taskType) }}
            </RBadge>
          </RTableCell>
          <RTableCell class="truncate max-w-[200px]">{{ row.jobTitle }}</RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(getPlatformTag(row.platform))">{{ row.platform }}</RBadge>
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(getStatusTag(row.status))">
              {{ getStatusLabel(row.status) }}
            </RBadge>
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(getPriorityTag(row.priority))">P{{ row.priority }}</RBadge>
          </RTableCell>
          <RTableCell>
            <div class="progress-cell">
              <span class="progress-text">{{ row.completedCount }}/{{ row.targetCount }}</span>
              <RProgress
                :value="row.targetCount ? Math.round((row.completedCount / row.targetCount) * 100) : 0"
                class="h-1.5 flex-1"
              />
            </div>
          </RTableCell>
          <RTableCell class="text-muted-foreground">{{ row.startedAt }}</RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta
      v-else
      title="暂无自动招聘任务"
      description="在招职位工作台启动渠道招聘后，系统将自动创建任务"
      :actions="[{ label: '去职位开始渠道招聘', type: 'primary', onClick: () => $router.push('/planning/jobs') }]"
    />

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="loadData"
    />

    <template #below>
      <RSheet v-model:open="detailVisible">
        <RSheetContent class="overflow-y-auto">
          <h3 class="text-lg font-semibold mb-4">任务详情</h3>
          <template v-if="taskDetail">
            <dl class="detail-dl">
              <div><dt>任务类型</dt><dd>{{ getTaskTypeLabel(taskDetail.taskType) }}</dd></div>
              <div><dt>在招职位</dt><dd>{{ taskDetail.jobTitle || '-' }}</dd></div>
              <div><dt>平台</dt><dd>{{ taskDetail.platform || '-' }}</dd></div>
              <div><dt>状态</dt><dd>{{ getStatusLabel(taskDetail.status) }}</dd></div>
              <div><dt>进度</dt><dd>{{ taskDetail.completedCount }}/{{ taskDetail.targetCount }}</dd></div>
              <div><dt>开始时间</dt><dd>{{ taskDetail.startedAt || '-' }}</dd></div>
              <div v-if="taskDetail.errorMessage"><dt>错误</dt><dd>{{ taskDetail.errorMessage }}</dd></div>
            </dl>

            <h4 class="text-sm font-semibold mt-5 mb-3">最近日志</h4>
            <div v-if="taskDetail.recentLogs?.length" class="timeline">
              <div v-for="log in taskDetail.recentLogs" :key="log.id" class="timeline-item">
                <div
                  class="timeline-dot"
                  :class="log.success ? 'timeline-dot--success' : 'timeline-dot--danger'"
                />
                <div class="timeline-content">
                  <div class="timeline-time">{{ log.createdAt }}</div>
                  <div>{{ log.action || log.step }} — {{ log.message || (log.success ? '成功' : '失败') }}</div>
                </div>
              </div>
            </div>
            <p v-else class="text-sm text-muted-foreground">暂无日志</p>

            <RButton
              v-if="taskDetail.jobId"
              class="mt-4"
              @click="$router.push({ path: `/planning/jobs/${taskDetail.jobId}`, query: { tab: 'sourcing' } })"
            >
              查看渠道招聘
            </RButton>
          </template>
        </RSheetContent>
      </RSheet>

      <RDialog v-model:open="showCreateDialog">
        <RDialogContent class="max-w-lg">
          <RDialogHeader>
            <RDialogTitle>手动创建任务</RDialogTitle>
          </RDialogHeader>
          <div class="grid gap-4 py-2">
            <FormField label="任务类型" required :error="formErrors.taskType">
              <RSelect v-model="createForm.taskType" :options="createTaskTypeOptions" placeholder="请选择任务类型" />
            </FormField>
            <FormField label="在招职位" required :error="formErrors.jobId">
              <RSelect v-model="createForm.jobId" :options="jobSelectOptions" placeholder="请选择在招职位" />
            </FormField>
            <FormField label="招聘账号" required :error="formErrors.agentAccountId">
              <RSelect v-model="createForm.agentAccountId" :options="accountSelectOptions" placeholder="请选择招聘平台账号" />
            </FormField>
            <FormField label="优先级" required>
              <div class="flex flex-wrap gap-2">
                <RButton
                  v-for="p in priorityOptions"
                  :key="p.value"
                  size="sm"
                  :variant="createForm.priority === p.value ? 'default' : 'outline'"
                  @click="createForm.priority = p.value"
                >
                  <RBadge :variant="elTagTypeToBadge(p.tag)">P{{ p.value }} {{ p.label }}</RBadge>
                </RButton>
              </div>
            </FormField>
          </div>
          <RDialogFooter>
            <RButton variant="outline" @click="showCreateDialog = false">取消</RButton>
            <RButton @click="handleCreateSubmit">创建任务</RButton>
          </RDialogFooter>
        </RDialogContent>
      </RDialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Search, Plus, RefreshCw, Video, Pause, CircleCheck, CircleX,
} from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import FormField from '@/components/app/FormField.vue'
import {
  RButton,
  RInput,
  RSelect,
  RBadge,
  RTable,
  RTableHead,
  RTableBody,
  RTableRow,
  RTableTh,
  RTableCell,
  RDialog,
  RDialogContent,
  RDialogHeader,
  RDialogTitle,
  RDialogFooter,
  RSheet,
  RSheetContent,
  RAlert,
  RAlertDescription,
  RProgress,
} from '@/components/ui'
import {
  getAgentTaskList, getAgentAccountList, createAgentTask,
  startAgentTask, pauseAgentTask, resumeAgentTask, getAgentTaskDetail,
} from '@/api/modules/agent'

const queryParams = reactive({
  keyword: '',
  status: '' as string | undefined,
  taskType: '' as string | undefined,
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const showCreateDialog = ref(false)
const detailVisible = ref(false)
const taskDetail = ref<any>(null)
const formErrors = reactive({ taskType: '', jobId: '', agentAccountId: '' })

const statusOptions = [
  { label: '待执行', value: 'PENDING' },
  { label: '运行中', value: 'RUNNING' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已完成', value: 'DONE' },
  { label: '失败', value: 'FAILED' },
]

const taskTypeOptions = [
  { label: '搜索', value: 'SEARCH' },
  { label: '联系', value: 'CONTACT' },
  { label: '跟进', value: 'FOLLOWUP' },
  { label: '安排', value: 'SCHEDULE' },
]

const createTaskTypeOptions = taskTypeOptions
const priorityOptions = [
  { value: 1, label: '高', tag: 'danger' },
  { value: 2, label: '中', tag: 'warning' },
  { value: 3, label: '低', tag: 'info' },
]

const jobOptions = ref<any[]>([])
const accountOptions = ref<any[]>([])
const taskList = ref<any[]>([])

const jobSelectOptions = computed(() => jobOptions.value.map((j) => ({ label: j.title, value: j.id })))
const accountSelectOptions = computed(() => accountOptions.value.map((a) => ({ label: a.name, value: a.id })))

const createForm = reactive({
  taskType: '' as string | undefined,
  jobId: '' as string | number | undefined,
  agentAccountId: '' as string | number | undefined,
  priority: 2,
})

const stats = computed(() => {
  const all = taskList.value
  return {
    running: all.filter((t) => t.status === 'RUNNING').length,
    paused: all.filter((t) => t.status === 'PAUSED').length,
    done: all.filter((t) => t.status === 'DONE').length,
    fail: all.filter((t) => t.status === 'FAILED').length,
  }
})

const filteredTasks = computed(() => {
  let list = [...taskList.value]
  if (queryParams.keyword) {
    const kw = queryParams.keyword.toLowerCase()
    list = list.filter((t) => (t.jobTitle || '').toLowerCase().includes(kw))
  }
  if (queryParams.status) list = list.filter((t) => t.status === queryParams.status)
  if (queryParams.taskType) list = list.filter((t) => t.taskType === queryParams.taskType)
  total.value = list.length
  const start = (queryParams.pageNum - 1) * queryParams.pageSize
  return list.slice(start, start + queryParams.pageSize)
})

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

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.taskType = undefined
  handleSearch()
}

async function handleStart(row: any) {
  const ok = await confirm({ title: '启动确认', message: '确定要启动该任务吗？', confirmText: '确定', cancelText: '取消' })
  if (!ok) return
  try {
    const api = row.status === 'PAUSED' ? resumeAgentTask : startAgentTask
    await api(row.id)
    toast.success('任务已启动')
    loadData()
  } catch {
    toast.error('启动失败')
  }
}

async function handlePause(row: any) {
  const ok = await confirm({ title: '暂停确认', message: '确定要暂停该任务吗？', confirmText: '确定', cancelText: '取消' })
  if (!ok) return
  try {
    await pauseAgentTask(row.id)
    toast.success('任务已暂停')
    loadData()
  } catch {
    toast.error('暂停失败')
  }
}

async function handleView(row: any) {
  try {
    const res: any = await getAgentTaskDetail(row.id)
    taskDetail.value = res.data || res
    detailVisible.value = true
  } catch {
    toast.error('加载任务详情失败')
  }
}

function validateCreateForm() {
  formErrors.taskType = createForm.taskType ? '' : '请选择任务类型'
  formErrors.jobId = createForm.jobId ? '' : '请选择在招职位'
  formErrors.agentAccountId = createForm.agentAccountId ? '' : '请选择招聘账号'
  return !formErrors.taskType && !formErrors.jobId && !formErrors.agentAccountId
}

async function handleCreateSubmit() {
  if (!validateCreateForm()) return
  try {
    await createAgentTask({
      taskType: createForm.taskType,
      jobId: createForm.jobId,
      agentAccountId: createForm.agentAccountId,
      priority: createForm.priority,
    })
    toast.success('任务创建成功')
    showCreateDialog.value = false
    createForm.taskType = undefined
    createForm.jobId = undefined
    createForm.agentAccountId = undefined
    createForm.priority = 2
    loadData()
  } catch {
    /* handled by interceptor */
  }
}

function getRowActions(_row: any) {
  return [
    { command: 'view', label: '查看详情', icon: 'View', type: 'primary', primary: true },
    { command: 'edit', label: '编辑', icon: 'Edit' },
    { command: 'delete', label: '删除', icon: 'Delete', divided: true },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view' || cmd === 'edit') handleView(row)
  else if (cmd === 'delete') handlePause(row)
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
    /* options stay empty */
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
  margin-top: 16px;
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

.detail-dl {
  display: grid;
  gap: 10px;
  font-size: 14px;

  div {
    display: grid;
    grid-template-columns: 100px 1fr;
    gap: 8px;
  }

  dt { color: $text-secondary; }
  dd { margin: 0; }
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;

  &--success { background: $success-color; }
  &--danger { background: $danger-color; }
}

.timeline-time {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 2px;
}
</style>
