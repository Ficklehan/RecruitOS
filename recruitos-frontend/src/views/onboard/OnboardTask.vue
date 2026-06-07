<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2 class="page-title">入职任务</h2>
      </div>
      <el-button type="primary" @click="handleAddTask">
        <el-icon><Plus /></el-icon>
        添加任务
      </el-button>
    </div>

    <!-- 候选人信息卡片 -->
    <div class="candidate-card">
      <div class="candidate-main">
        <div class="candidate-avatar">{{ candidateInfo.name.charAt(0) }}</div>
        <div class="candidate-detail">
          <h3 class="candidate-name">{{ candidateInfo.name }}</h3>
          <div class="candidate-meta">
            <span><el-icon><Briefcase /></el-icon> {{ candidateInfo.jobTitle }}</span>
            <span><el-icon><Calendar /></el-icon> 入职日期: {{ candidateInfo.onboardDate }}</span>
            <el-tag :type="getStatusType(candidateInfo.status)" size="small">
              {{ getStatusLabel(candidateInfo.status) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 进度概览 -->
    <div class="progress-section">
      <div class="progress-header">
        <span class="progress-title">任务进度</span>
        <span class="progress-text">{{ completedCount }}/{{ tasks.length }} 已完成</span>
      </div>
      <el-progress
        :percentage="completionPercentage"
        :stroke-width="12"
        :color="progressColors"
      />
    </div>

    <!-- 任务列表 -->
    <div class="task-list">
      <div v-for="task in tasks" :key="task.id" class="task-item" :class="{ 'is-completed': task.status === 'COMPLETED' }">
        <div class="task-left">
          <el-checkbox
            :model-value="task.status === 'COMPLETED'"
            @change="handleToggleTask(task)"
          />
        </div>
        <div class="task-content">
          <div class="task-header">
            <span class="task-name" :class="{ 'is-done': task.status === 'COMPLETED' }">
              {{ task.name }}
            </span>
            <el-tag
              :type="getTaskTypeTagType(task.type)"
              size="small"
              disable-transitions
            >
              {{ getTaskTypeLabel(task.type) }}
            </el-tag>
          </div>
          <div class="task-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ task.assignee }}
            </span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              截止: {{ task.dueDate }}
            </span>
            <el-tag
              :type="getTaskStatusType(task.status)"
              size="small"
              effect="plain"
            >
              {{ getTaskStatusLabel(task.status) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加任务对话框 -->
    <el-dialog
      v-model="addTaskDialogVisible"
      title="添加任务"
      width="500px"
      destroy-on-close
    >
      <el-form ref="taskFormRef" :model="taskFormData" :rules="taskFormRules" label-width="80px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="taskFormData.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务类型" prop="type">
          <el-select v-model="taskFormData.type" placeholder="请选择任务类型" style="width: 100%">
            <el-option label="文档类" value="DOC" />
            <el-option label="准备工作" value="PREPARATION" />
            <el-option label="培训类" value="TRAINING" />
            <el-option label="IT类" value="IT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="assignee">
          <el-input v-model="taskFormData.assignee" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="截止日期" prop="dueDate">
          <el-date-picker
            v-model="taskFormData.dueDate"
            type="date"
            placeholder="选择截止日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTaskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addTaskLoading" @click="confirmAddTask">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft, Plus, User, Calendar, Briefcase } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// 候选人信息
const candidateInfo = reactive({
  name: '张伟',
  jobTitle: '高级前端工程师',
  onboardDate: '2026-06-16',
  status: 'CONFIRMED',
})

// 任务列表
const tasks = ref([
  { id: 1, name: '签署劳动合同', type: 'DOC', assignee: '陈HR', dueDate: '2026-06-16', status: 'COMPLETED' },
  { id: 2, name: '准备工位及办公设备', type: 'PREPARATION', assignee: 'IT运维组', dueDate: '2026-06-15', status: 'COMPLETED' },
  { id: 3, name: '开通企业邮箱及内部系统账号', type: 'IT', assignee: 'IT支持', dueDate: '2026-06-16', status: 'IN_PROGRESS' },
  { id: 4, name: '新员工入职培训', type: 'TRAINING', assignee: '培训组', dueDate: '2026-06-17', status: 'PENDING' },
  { id: 5, name: '收集学历证明及离职证明', type: 'DOC', assignee: '陈HR', dueDate: '2026-06-20', status: 'PENDING' },
  { id: 6, name: '安排导师及团队介绍', type: 'OTHER', assignee: '技术部-李经理', dueDate: '2026-06-16', status: 'PENDING' },
])

// 添加任务对话框
const addTaskDialogVisible = ref(false)
const addTaskLoading = ref(false)
const taskFormRef = ref<FormInstance>()

const taskFormData = reactive({
  name: '',
  type: '',
  assignee: '',
  dueDate: '',
})

const taskFormRules: FormRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  assignee: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  dueDate: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
}

// 计算属性
const completedCount = computed(() => tasks.value.filter(t => t.status === 'COMPLETED').length)
const completionPercentage = computed(() => {
  if (tasks.value.length === 0) return 0
  return Math.round((completedCount.value / tasks.value.length) * 100)
})

const progressColors = [
  { color: '#DC2626', percentage: 20 },
  { color: '#D97706', percentage: 40 },
  { color: '#3B82F6', percentage: 60 },
  { color: '#059669', percentage: 100 },
]

// 状态映射
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

function getTaskStatusType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'info',
    IN_PROGRESS: 'warning',
    COMPLETED: 'success',
  }
  return map[status] || 'info'
}

function getTaskStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
  }
  return map[status] || status
}

function goBack() {
  router.push('/onboard/list')
}

function handleToggleTask(task: any) {
  ElMessageBox.confirm(
    task.status === 'COMPLETED' ? '确定将此任务标记为未完成吗？' : '确定将此任务标记为已完成吗？',
    '确认操作',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    }
  ).then(() => {
    task.status = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
    ElMessage.success(task.status === 'COMPLETED' ? '任务已完成' : '任务已恢复')
  }).catch(() => {})
}

function handleAddTask() {
  taskFormData.name = ''
  taskFormData.type = ''
  taskFormData.assignee = ''
  taskFormData.dueDate = ''
  addTaskDialogVisible.value = true
}

async function confirmAddTask() {
  if (!taskFormRef.value) return
  await taskFormRef.value.validate((valid) => {
    if (valid) {
      addTaskLoading.value = true
      setTimeout(() => {
        tasks.value.push({
          id: Date.now(),
          name: taskFormData.name,
          type: taskFormData.type,
          assignee: taskFormData.assignee,
          dueDate: taskFormData.dueDate,
          status: 'PENDING',
        })
        addTaskLoading.value = false
        addTaskDialogVisible.value = false
        ElMessage.success('任务添加成功')
      }, 500)
    }
  })
}

onMounted(() => {
  // 可根据路由参数加载不同候选人的任务
  const id = route.query.id
  if (id) {
    // 模拟根据 id 加载数据
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
  border: 1px solid $border-color-light;
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
  border: 1px solid $border-color-light;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 20px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .progress-title {
      font-size: 15px;
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
  border: 1px solid $border-color-light;
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
        font-size: 15px;
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
