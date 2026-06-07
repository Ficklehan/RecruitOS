<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">AI 自动化工作流</h2>
        <p class="page-subtitle">一键从JD发起全链路：检索 → 打招呼 → 收简历 → 入库</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          新建工作流
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: #EFF6FF; color: #3B82F6">
          <el-icon :size="24"><VideoPlay /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.running }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #D1FAE5; color: #059669">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalResumes }}</div>
          <div class="stat-label">已收简历</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #FEF3C7; color: #D97706">
          <el-icon :size="24"><ChatDotRound /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalGreeted }}</div>
          <div class="stat-label">已打招呼</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #FEE2E2; color: #DC2626">
          <el-icon :size="24"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.alerts }}</div>
          <div class="stat-label">异常告警</div>
        </div>
      </div>
    </div>

    <!-- 工作流列表 -->
    <div class="workflow-list">
      <div v-for="wf in workflowList" :key="wf.id" class="workflow-card">
        <div class="wf-header">
          <div class="wf-info">
            <h3 class="wf-title">{{ wf.name }}</h3>
            <p class="wf-job">关联岗位：{{ wf.jobTitle }}</p>
          </div>
          <div class="wf-status">
            <el-tag :type="wf.status === 'RUNNING' ? 'success' : wf.status === 'PAUSED' ? 'warning' : 'info'" size="small">
              {{ statusLabelMap[wf.status] }}
            </el-tag>
          </div>
        </div>

        <!-- 流程进度条 -->
        <div class="wf-pipeline">
          <div class="pipeline-step" :class="{ active: wf.stats.searched > 0 }">
            <div class="step-icon">
              <el-icon><Search /></el-icon>
            </div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.searched }}</div>
              <div class="step-label">已检索</div>
            </div>
          </div>
          <div class="pipeline-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
          <div class="pipeline-step" :class="{ active: wf.stats.greeted > 0 }">
            <div class="step-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.greeted }}</div>
              <div class="step-label">已打招呼</div>
            </div>
          </div>
          <div class="pipeline-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
          <div class="pipeline-step" :class="{ active: wf.stats.replied > 0 }">
            <div class="step-icon">
              <el-icon><ChatLineRound /></el-icon>
            </div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.replied }}</div>
              <div class="step-label">已回复</div>
            </div>
          </div>
          <div class="pipeline-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
          <div class="pipeline-step" :class="{ active: wf.stats.resumes > 0 }">
            <div class="step-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.resumes }}</div>
              <div class="step-label">已收简历</div>
            </div>
          </div>
          <div class="pipeline-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
          <div class="pipeline-step" :class="{ active: wf.stats.imported > 0 }">
            <div class="step-icon">
              <el-icon><FolderAdd /></el-icon>
            </div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.imported }}</div>
              <div class="step-label">已入库</div>
            </div>
          </div>
        </div>

        <div class="wf-footer">
          <div class="wf-meta">
            <span>平台：{{ wf.platforms.join('、') }}</span>
            <span>创建时间：{{ wf.createdAt }}</span>
          </div>
          <div class="wf-actions">
            <el-button v-if="wf.status === 'RUNNING'" type="warning" link size="small" @click="handlePause(wf)">
              <el-icon><VideoPause /></el-icon>
              暂停
            </el-button>
            <el-button v-if="wf.status === 'PAUSED'" type="success" link size="small" @click="handleResume(wf)">
              <el-icon><VideoPlay /></el-icon>
              恢复
            </el-button>
            <el-button type="primary" link size="small" @click="goDetail(wf)">详情</el-button>
          </div>
        </div>
      </div>

      <div v-if="!workflowList.length" class="empty-state">
        <el-icon :size="48" color="#94A3B8"><DataLine /></el-icon>
        <p>暂无工作流，点击「新建工作流」开始</p>
      </div>
    </div>

    <!-- 新建工作流弹窗 -->
    <el-dialog v-model="showCreateDialog" title="新建 AI 工作流" width="640px" destroy-on-close>
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="工作流名称" required>
          <el-input v-model="createForm.name" placeholder="如：高级前端-BOSS自动招聘" />
        </el-form-item>
        <el-form-item label="关联岗位" required>
          <el-select v-model="createForm.jobId" placeholder="选择已激活的岗位" style="width: 100%">
            <el-option v-for="job in jobOptions" :key="job.id" :label="job.title" :value="job.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="检索平台" required>
          <el-checkbox-group v-model="createForm.platforms">
            <el-checkbox label="BOSS">Boss直聘</el-checkbox>
            <el-checkbox label="LAGOU">拉勾</el-checkbox>
            <el-checkbox label="ZHILIAN">智联招聘</el-checkbox>
            <el-checkbox label="LIEPIN">猎聘</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="检索关键词">
          <el-select
            v-model="createForm.keywords"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="从JD标签自动生成，可手动调整"
            style="width: 100%"
          >
            <el-option v-for="tag in jdTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="每日上限">
          <el-input-number v-model="createForm.dailyLimit" :min="10" :max="500" :step="10" />
          <span style="margin-left: 8px; color: #94A3B8; font-size: 13px">次/平台/天</span>
        </el-form-item>
        <el-form-item label="打招呼话术">
          <el-select v-model="createForm.templateId" placeholder="选择话术模板" style="width: 100%">
            <el-option v-for="tpl in templateOptions" :key="tpl.id" :label="tpl.name" :value="tpl.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建并启动</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Refresh, Plus, VideoPlay, VideoPause, CircleCheck, ChatDotRound, Warning,
  Search, ArrowRight, ChatLineRound, Document, FolderAdd, DataLine,
} from '@element-plus/icons-vue'
import { getWorkflowList, createWorkflow, pauseWorkflow, resumeWorkflow } from '@/api/modules/agent'
import { getJobList } from '@/api/modules/job'
import { getTemplateList } from '@/api/modules/communication'

const router = useRouter()

const showCreateDialog = ref(false)
const workflowList = ref<any[]>([])
const jobOptions = ref<any[]>([])
const templateOptions = ref<any[]>([])
const jdTags = ref<string[]>(['Vue.js', 'React', 'TypeScript', 'Node.js', 'Python'])

const createForm = reactive({
  name: '',
  jobId: null as number | null,
  platforms: ['BOSS'] as string[],
  keywords: [] as string[],
  dailyLimit: 50,
  templateId: null as number | null,
})

const statusLabelMap: Record<string, string> = {
  RUNNING: '运行中',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
  DRAFT: '草稿',
}

const stats = computed(() => {
  const running = workflowList.value.filter(w => w.status === 'RUNNING').length
  const totalResumes = workflowList.value.reduce((s, w) => s + (w.stats?.resumes || 0), 0)
  const totalGreeted = workflowList.value.reduce((s, w) => s + (w.stats?.greeted || 0), 0)
  const alerts = workflowList.value.reduce((s, w) => s + (w.stats?.alerts || 0), 0)
  return { running, totalResumes, totalGreeted, alerts }
})

function goDetail(wf: any) {
  router.push(`/ai-tools/workflow/${wf.id}`)
}

async function handlePause(wf: any) {
  try {
    await pauseWorkflow(wf.id)
    wf.status = 'PAUSED'
    ElMessage.success('已暂停')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleResume(wf: any) {
  try {
    await resumeWorkflow(wf.id)
    wf.status = 'RUNNING'
    ElMessage.success('已恢复')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleCreate() {
  if (!createForm.name || !createForm.jobId || !createForm.platforms.length) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    await createWorkflow({
      name: createForm.name,
      jobId: createForm.jobId,
      platforms: createForm.platforms,
      keywords: createForm.keywords,
      dailyLimit: createForm.dailyLimit,
      templateId: createForm.templateId,
    })
    showCreateDialog.value = false
    ElMessage.success('工作流已创建并启动')
    loadData()
  } catch {
    ElMessage.error('创建失败')
  }
}

function handleRefresh() {
  loadData()
  ElMessage.success('刷新成功')
}

async function loadData() {
  try {
    const res: any = await getWorkflowList()
    const data = res.data || res
    workflowList.value = Array.isArray(data) ? data : data.records || []
  } catch {
    workflowList.value = []
  }
}

async function loadJobOptions() {
  try {
    const res: any = await getJobList({ status: 'ACTIVE' })
    const data = res.data || res
    jobOptions.value = Array.isArray(data) ? data : data.records || []
  } catch {
    jobOptions.value = []
  }
}

async function loadTemplateOptions() {
  try {
    const res: any = await getTemplateList()
    const data = res.data || res
    templateOptions.value = Array.isArray(data) ? data : data.records || []
  } catch {
    templateOptions.value = []
  }
}

onMounted(() => {
  loadData()
  loadJobOptions()
  loadTemplateOptions()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.header-actions {
  display: flex;
  gap: 8px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: $spacing-lg;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 13px;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.workflow-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workflow-card {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 24px;
}

.wf-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.wf-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.wf-job {
  font-size: 13px;
  color: $text-secondary;
}

.wf-pipeline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 0;
  margin-bottom: 16px;
  background: $bg-muted;
  border-radius: $border-radius-sm;
}

.pipeline-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
  transition: opacity $transition-fast;

  &.active {
    opacity: 1;
  }
}

.step-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: $bg-card;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: $primary-color;
}

.step-info {
  text-align: center;
}

.step-count {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
}

.step-label {
  font-size: 11px;
  color: $text-secondary;
  margin-top: 2px;
}

.pipeline-arrow {
  color: $text-placeholder;
  font-size: 16px;
  margin: 0 4px;
}

.wf-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wf-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: $text-secondary;
}

.wf-actions {
  display: flex;
  gap: 4px;
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: $text-secondary;

  p {
    margin-top: 12px;
    font-size: 14px;
  }
}
</style>
