<template>
  <PageShell title="入职任务" class="relative">
    <template #actions>
      <RButton @click="handleAddTask">
        <Plus class="mr-2 h-4 w-4" />
        添加任务
      </RButton>
    </template>

    <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <div class="candidate-card">
      <div class="candidate-main">
        <div class="candidate-avatar">{{ candidateInfo.name?.charAt(0) || '?' }}</div>
        <div class="candidate-detail">
          <h3 class="candidate-name">{{ candidateInfo.name }}</h3>
          <div class="candidate-meta">
            <span><Briefcase class="inline h-4 w-4" /> {{ candidateInfo.jobTitle }}</span>
            <span><Calendar class="inline h-4 w-4" /> 入职日期: {{ candidateInfo.onboardDate }}</span>
            <RBadge :variant="elTagTypeToBadge(getStatusType(candidateInfo.status))">
              {{ getStatusLabel(candidateInfo.status) }}
            </RBadge>
          </div>
        </div>
      </div>
    </div>

    <div class="progress-section">
      <div class="progress-header">
        <span class="progress-title">任务进度</span>
        <span class="progress-text">{{ completedCount }}/{{ tasks.length }} 已完成</span>
      </div>
      <RProgress :value="completionPercentage" :max="100" class="h-3" />
    </div>

    <div class="task-list">
      <div v-for="task in tasks" :key="task.id" class="task-item" :class="{ 'is-completed': task.status === 'COMPLETED' }">
        <div class="task-left">
          <RCheckbox
            :model-value="task.status === 'COMPLETED'"
            @update:model-value="() => handleToggleTask(task)"
          />
        </div>
        <div class="task-content">
          <div class="task-header">
            <span class="task-name" :class="{ 'is-done': task.status === 'COMPLETED' }">
              {{ task.name }}
            </span>
            <RBadge :variant="elTagTypeToBadge(getTaskTypeTagType(task.type))">
              {{ getTaskTypeLabel(task.type) }}
            </RBadge>
          </div>
          <div class="task-meta">
            <span class="meta-item">
              <User class="inline h-3.5 w-3.5" />
              {{ task.assignee }}
            </span>
            <span class="meta-item">
              <Calendar class="inline h-3.5 w-3.5" />
              截止: {{ task.dueDate }}
            </span>
            <RBadge variant="outline">
              {{ getTaskStatusLabel(task.status) }}
            </RBadge>
          </div>
        </div>
      </div>
    </div>

    <RDialog v-model:open="addTaskDialogVisible">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>添加任务</DialogTitle>
        </DialogHeader>
        <div class="grid gap-4 py-2">
          <FormField label="任务名称" required :error="formErrors.name">
            <RInput v-model="taskFormData.name" placeholder="请输入任务名称" />
          </FormField>
          <FormField label="任务类型" required :error="formErrors.type">
            <RSelect v-model="taskFormData.type" :options="taskTypeOptions" placeholder="请选择任务类型" />
          </FormField>
          <FormField label="负责人" required :error="formErrors.assignee">
            <RInput v-model="taskFormData.assignee" placeholder="请输入负责人" />
          </FormField>
          <FormField label="截止日期" required :error="formErrors.dueDate">
            <RInput v-model="taskFormData.dueDate" type="date" />
          </FormField>
        </div>
        <DialogFooter>
          <RButton variant="outline" @click="addTaskDialogVisible = false">取消</RButton>
          <RButton :disabled="addTaskLoading" @click="confirmAddTask">确定</RButton>
        </DialogFooter>
      </DialogContent>
    </RDialog>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Plus, User, Calendar, Briefcase, Loader2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import FormField from '@/components/app/FormField.vue'
import {
  RButton, RBadge, RInput, RSelect, RCheckbox, RProgress,
  RDialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui'
import {
  getOnboardDetail,
  getOnboardTasks,
  createOnboardTask,
  updateOnboardTaskStatus,
} from '@/api/modules/onboard'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const onboardId = ref<number | null>(null)

const candidateInfo = reactive({
  name: '',
  jobTitle: '',
  onboardDate: '',
  status: 'PENDING',
})

const tasks = ref<any[]>([])

const taskTypeOptions = [
  { label: '文档类', value: 'DOC' },
  { label: '准备工作', value: 'PREPARATION' },
  { label: '培训类', value: 'TRAINING' },
  { label: 'IT类', value: 'IT' },
  { label: '其他', value: 'OTHER' },
]

function mapTask(t: any) {
  const raw = t.taskStatus || t.status
  return {
    ...t,
    name: t.taskName || t.name,
    type: t.taskType || t.type,
    assignee: t.assigneeName || t.assignee,
    dueDate: t.dueDate || '',
    status: raw === 'DONE' ? 'COMPLETED' : raw,
  }
}

const addTaskDialogVisible = ref(false)
const addTaskLoading = ref(false)

const taskFormData = reactive({
  name: '',
  type: '' as string | number | undefined,
  assignee: '',
  dueDate: '',
})

const formErrors = reactive({
  name: '',
  type: '',
  assignee: '',
  dueDate: '',
})

const completedCount = computed(() => tasks.value.filter(t => t.status === 'COMPLETED').length)
const completionPercentage = computed(() => {
  if (tasks.value.length === 0) return 0
  return Math.round((completedCount.value / tasks.value.length) * 100)
})

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    CONFIRMED: '',
    CANCELLED: 'info',
    COMPLETED: 'success',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待入职',
    CONFIRMED: '已确认',
    CANCELLED: '已取消',
    COMPLETED: '已完成',
  }
  return map[status] || status
}

function getTaskTypeTagType(type: string): string {
  const map: Record<string, string> = {
    DOC: '',
    PREPARATION: 'warning',
    TRAINING: 'success',
    IT: 'danger',
    OTHER: 'info',
  }
  return map[type] || 'info'
}

function getTaskTypeLabel(type: string): string {
  const map: Record<string, string> = {
    DOC: '文档类',
    PREPARATION: '准备工作',
    TRAINING: '培训类',
    IT: 'IT类',
    OTHER: '其他',
  }
  return map[type] || type
}

function getTaskStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
  }
  return map[status] || status
}

function validateTaskForm(): boolean {
  formErrors.name = taskFormData.name.trim() ? '' : '请输入任务名称'
  formErrors.type = taskFormData.type ? '' : '请选择任务类型'
  formErrors.assignee = taskFormData.assignee.trim() ? '' : '请输入负责人'
  formErrors.dueDate = taskFormData.dueDate ? '' : '请选择截止日期'
  return !formErrors.name && !formErrors.type && !formErrors.assignee && !formErrors.dueDate
}

function goBack() {
  router.push('/onboard/list')
}

async function handleToggleTask(task: any) {
  const ok = await confirm({
    title: '确认操作',
    message: task.status === 'COMPLETED' ? '确定将此任务标记为未完成吗？' : '确定将此任务标记为已完成吗？',
  })
  if (!ok) return
  const next = task.status === 'COMPLETED' ? 'PENDING' : 'DONE'
  await updateOnboardTaskStatus(task.id, next)
  task.status = next
  toast.success(next === 'DONE' ? '任务已完成' : '任务已恢复')
}

function handleAddTask() {
  taskFormData.name = ''
  taskFormData.type = undefined
  taskFormData.assignee = ''
  taskFormData.dueDate = ''
  Object.assign(formErrors, { name: '', type: '', assignee: '', dueDate: '' })
  addTaskDialogVisible.value = true
}

async function confirmAddTask() {
  if (!validateTaskForm() || !onboardId.value) return
  addTaskLoading.value = true
  try {
    await createOnboardTask({
      onboardId: onboardId.value,
      taskName: taskFormData.name,
      taskType: String(taskFormData.type),
      assigneeName: taskFormData.assignee,
      dueDate: taskFormData.dueDate,
    })
    addTaskDialogVisible.value = false
    toast.success('任务添加成功')
    await loadTasks()
  } finally {
    addTaskLoading.value = false
  }
}

async function loadDetail() {
  if (!onboardId.value) return
  const res = await getOnboardDetail(onboardId.value)
  const d = res.data
  candidateInfo.name = d.candidateName || ''
  candidateInfo.jobTitle = d.jobTitle || ''
  candidateInfo.onboardDate = d.onboardDate || ''
  candidateInfo.status = d.onboardStatus || d.status || 'PENDING'
}

async function loadTasks() {
  if (!onboardId.value) return
  const res = await getOnboardTasks(onboardId.value)
  tasks.value = (res.data || []).map(mapTask)
}

onMounted(async () => {
  const id = Number(route.query.id)
  if (!id) return
  onboardId.value = id
  loading.value = true
  try {
    await Promise.all([loadDetail(), loadTasks()])
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.candidate-card {
  background: $bg-card;
  border: none;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 20px;

  .candidate-main {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .candidate-avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: linear-gradient(135deg, $primary-color, $primary-light);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    font-weight: 600;
    flex-shrink: 0;
  }

  .candidate-detail {
    .candidate-name {
      font-size: 18px;
      font-weight: 600;
      color: $text-primary;
      margin: 0 0 8px 0;
    }

    .candidate-meta {
      display: flex;
      align-items: center;
      gap: 16px;
      font-size: 14px;
      color: $text-regular;

      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.progress-section {
  background: $bg-card;
  border: none;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 20px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .progress-title {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
    }

    .progress-text {
      font-size: 13px;
      color: $text-secondary;
    }
  }
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  background: $bg-card;
  border: none;
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  &.is-completed {
    background: $bg-muted;

    .task-name {
      color: $text-placeholder;
    }
  }

  .task-left {
    padding-top: 2px;
  }

  .task-content {
    flex: 1;

    .task-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;

      .task-name {
        font-size: 14px;
        font-weight: 500;
        color: $text-primary;

        &.is-done {
          text-decoration: line-through;
          color: $text-placeholder;
        }
      }
    }

    .task-meta {
      display: flex;
      align-items: center;
      gap: 16px;
      font-size: 13px;
      color: $text-secondary;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}
</style>
