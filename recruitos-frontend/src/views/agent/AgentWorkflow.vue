<template>
  <PageShell title="AI 自动化工作流" subtitle="从任职要求发起渠道招聘全链路：搜寻 → 打招呼 → 收简历 → 加入候选人">
    <template #toolbar>
      <RpaSafetyBar />
    </template>
    <template #actions>
      <RButton variant="outline" size="icon" @click="handleRefresh">
        <RefreshCw class="h-4 w-4" />
      </RButton>
      <RButton @click="showCreateDialog = true">
        <Plus class="mr-2 h-4 w-4" />
        新建工作流
      </RButton>
    </template>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="stat-card">
        <div class="stat-icon stat-icon--primary">
          <Video class="h-6 w-6" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.running }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--success">
          <CircleCheck class="h-6 w-6" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalResumes }}</div>
          <div class="stat-label">已收简历</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--warning">
          <MessageCircle class="h-6 w-6" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalGreeted }}</div>
          <div class="stat-label">已打招呼</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--danger">
          <AlertTriangle class="h-6 w-6" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.alerts }}</div>
          <div class="stat-label">异常告警</div>
        </div>
      </div>
    </div>

    <div class="workflow-list">
      <div v-for="wf in workflowList" :key="wf.id" class="workflow-card">
        <div class="wf-header">
          <div class="wf-info">
            <h3 class="wf-title">{{ wf.name }}</h3>
            <p class="wf-job">在招职位：{{ wf.jobTitle }}</p>
          </div>
          <RBadge :variant="statusBadge(wf.status)">{{ statusLabelMap[wf.status] }}</RBadge>
        </div>

        <div class="wf-pipeline">
          <div class="pipeline-step" :class="{ active: wf.stats.searched > 0 }">
            <div class="step-icon"><Search class="h-4 w-4" /></div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.searched }}</div>
              <div class="step-label">已检索</div>
            </div>
          </div>
          <ArrowRight class="pipeline-arrow h-4 w-4" />
          <div class="pipeline-step" :class="{ active: wf.stats.greeted > 0 }">
            <div class="step-icon"><MessageCircle class="h-4 w-4" /></div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.greeted }}</div>
              <div class="step-label">已打招呼</div>
            </div>
          </div>
          <ArrowRight class="pipeline-arrow h-4 w-4" />
          <div class="pipeline-step" :class="{ active: wf.stats.replied > 0 }">
            <div class="step-icon"><MessagesSquare class="h-4 w-4" /></div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.replied }}</div>
              <div class="step-label">已回复</div>
            </div>
          </div>
          <ArrowRight class="pipeline-arrow h-4 w-4" />
          <div class="pipeline-step" :class="{ active: wf.stats.resumes > 0 }">
            <div class="step-icon"><FileText class="h-4 w-4" /></div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.resumes }}</div>
              <div class="step-label">已收简历</div>
            </div>
          </div>
          <ArrowRight class="pipeline-arrow h-4 w-4" />
          <div class="pipeline-step" :class="{ active: wf.stats.imported > 0 }">
            <div class="step-icon"><FolderPlus class="h-4 w-4" /></div>
            <div class="step-info">
              <div class="step-count">{{ wf.stats.imported }}</div>
              <div class="step-label">已加入候选人</div>
            </div>
          </div>
        </div>

        <div class="wf-footer">
          <div class="wf-meta">
            <span>平台：{{ wf.platforms.join('、') }}</span>
            <span>创建时间：{{ wf.createdAt }}</span>
          </div>
          <div class="wf-actions">
            <RButton v-if="wf.status === 'RUNNING'" variant="link" size="sm" @click="handlePause(wf)">
              <Pause class="mr-1 h-3.5 w-3.5" />
              暂停
            </RButton>
            <RButton v-if="wf.status === 'PAUSED'" variant="link" size="sm" @click="handleResume(wf)">
              <Video class="mr-1 h-3.5 w-3.5" />
              恢复
            </RButton>
            <RButton variant="link" size="sm" @click="goDetail(wf)">详情</RButton>
          </div>
        </div>
      </div>

      <EmptyStateCta
        v-if="!workflowList.length"
        title="暂无工作流"
        description="点击「新建工作流」开始"
        :actions="[{ label: '新建工作流', type: 'primary', onClick: () => { showCreateDialog = true } }]"
      />
    </div>

    <RDialog v-model:open="showCreateDialog">
      <RDialogContent class="max-w-2xl">
        <RDialogHeader>
          <RDialogTitle>新建 AI 工作流</RDialogTitle>
        </RDialogHeader>
        <div class="grid gap-4 py-2">
          <FormField label="工作流名称" required>
            <RInput v-model="createForm.name" placeholder="如：高级前端-BOSS自动招聘" />
          </FormField>
          <FormField label="在招职位" required>
            <RSelect v-model="createForm.jobId" :options="jobSelectOptions" placeholder="选择招聘中的职位" />
          </FormField>
          <FormField label="检索平台" required>
            <div class="flex flex-wrap gap-4">
              <label class="flex items-center gap-2 text-sm">
                <RCheckbox :model-value="createForm.platforms.includes('BOSS')" @update:model-value="togglePlatform('BOSS', $event)" />
                Boss直聘
              </label>
              <label class="flex items-center gap-2 text-sm">
                <RCheckbox :model-value="createForm.platforms.includes('LIEPIN')" @update:model-value="togglePlatform('LIEPIN', $event)" />
                猎聘
              </label>
            </div>
          </FormField>
          <FormField label="检索关键词">
            <RTextarea
              v-model="keywordsText"
              placeholder="从JD标签自动生成，可手动调整（每行一个关键词）"
              :rows="3"
            />
          </FormField>
          <FormField label="每日上限">
            <div class="flex items-center gap-2">
              <NumberInput v-model="createForm.dailyLimit" :min="10" :max="500" :step="10" class="w-28" />
              <span class="text-sm text-muted-foreground">次/平台/天</span>
            </div>
          </FormField>
          <FormField label="打招呼话术">
            <RSelect v-model="createForm.templateId" :options="templateSelectOptions" placeholder="选择话术模板" clearable />
          </FormField>
        </div>
        <RDialogFooter>
          <RButton variant="outline" @click="showCreateDialog = false">取消</RButton>
          <RButton @click="handleCreate">创建并启动</RButton>
        </RDialogFooter>
      </RDialogContent>
    </RDialog>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  RefreshCw, Plus, Video, Pause, CircleCheck, MessageCircle, AlertTriangle,
  Search, ArrowRight, MessagesSquare, FileText, FolderPlus,
} from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import RpaSafetyBar from '@/components/agent/RpaSafetyBar.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import FormField from '@/components/app/FormField.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import {
  RButton, RInput, RSelect, RBadge, RCheckbox, RTextarea,
  RDialog, RDialogContent, RDialogHeader, RDialogTitle, RDialogFooter,
} from '@/components/ui'
import { getWorkflowList, createWorkflow, pauseWorkflow, resumeWorkflow } from '@/api/modules/agent'
import { getJobList } from '@/api/modules/job'
import { getTemplateList } from '@/api/modules/communication'

const router = useRouter()

const showCreateDialog = ref(false)
const workflowList = ref<any[]>([])
const jobOptions = ref<any[]>([])
const templateOptions = ref<any[]>([])

const createForm = reactive({
  name: '',
  jobId: null as number | null,
  mode: 'SEMI_AUTO' as string,
  platforms: ['BOSS', 'LIEPIN'] as string[],
  keywords: [] as string[],
  dailyLimit: 50,
  templateId: null as number | null,
})

const keywordsText = computed({
  get: () => createForm.keywords.join('\n'),
  set: (v: string) => {
    createForm.keywords = v.split('\n').map((s) => s.trim()).filter(Boolean)
  },
})

const jobSelectOptions = computed(() => jobOptions.value.map((j) => ({ label: j.title, value: j.id })))
const templateSelectOptions = computed(() =>
  templateOptions.value.map((t) => ({ label: t.templateName || t.name, value: t.id }))
)

const statusLabelMap: Record<string, string> = {
  RUNNING: '运行中',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
  DRAFT: '草稿',
}

function statusBadge(status: string) {
  if (status === 'RUNNING') return 'default'
  if (status === 'PAUSED') return 'outline'
  return 'secondary'
}

const stats = computed(() => {
  const running = workflowList.value.filter((w) => w.status === 'RUNNING').length
  const totalResumes = workflowList.value.reduce((s, w) => s + (w.stats?.resumes || 0), 0)
  const totalGreeted = workflowList.value.reduce((s, w) => s + (w.stats?.greeted || 0), 0)
  const alerts = workflowList.value.reduce((s, w) => s + (w.stats?.alerts || 0), 0)
  return { running, totalResumes, totalGreeted, alerts }
})

function togglePlatform(platform: string, checked: boolean) {
  if (checked && !createForm.platforms.includes(platform)) {
    createForm.platforms.push(platform)
  } else if (!checked) {
    createForm.platforms = createForm.platforms.filter((p) => p !== platform)
  }
}

function goDetail(wf: any) {
  if (wf.jobId) {
    router.push({ path: `/planning/jobs/${wf.jobId}`, query: { tab: 'sourcing' } })
  } else {
    router.push('/talent/channels/workflows')
  }
}

async function handlePause(wf: any) {
  try {
    await pauseWorkflow(wf.id)
    wf.status = 'PAUSED'
    toast.success('已暂停')
  } catch {
    toast.error('操作失败')
  }
}

async function handleResume(wf: any) {
  try {
    await resumeWorkflow(wf.id)
    wf.status = 'RUNNING'
    toast.success('已恢复')
  } catch {
    toast.error('操作失败')
  }
}

async function handleCreate() {
  if (!createForm.name || !createForm.jobId || !createForm.platforms.length) {
    toast.error('请填写必填项')
    return
  }
  try {
    await createWorkflow({
      name: createForm.name,
      jobId: createForm.jobId,
      mode: createForm.mode,
      platforms: createForm.platforms,
      keywords: createForm.keywords,
      dailyLimit: createForm.dailyLimit,
      templateId: createForm.templateId,
    })
    showCreateDialog.value = false
    toast.success('工作流已创建并启动')
    loadData()
  } catch {
    toast.error('创建失败')
  }
}

function handleRefresh() {
  loadData()
  toast.success('刷新成功')
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
    jobOptions.value = Array.isArray(data) ? data : data.records || data.list || []
  } catch {
    jobOptions.value = []
  }
}

async function loadTemplateOptions() {
  try {
    const res: any = await getTemplateList()
    const data = res.data || res
    templateOptions.value = Array.isArray(data) ? data : data.records || data.list || []
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
    color: #fff;
  }

  .stat-icon--primary { background: $primary-color; }
  .stat-icon--success { background: $success-color; }
  .stat-icon--warning { background: $warning-color; }
  .stat-icon--danger { background: $danger-color; }

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
  flex-wrap: wrap;
}

.pipeline-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
  transition: opacity $transition-fast;

  &.active { opacity: 1; }
}

.step-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: $bg-card;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $primary-color;
}

.step-info { text-align: center; }

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
  margin: 0 4px;
}

.wf-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
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
</style>
