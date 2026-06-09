<template>
  <ListPageLayout
    title="候选人列表"
    subtitle="管理候选人信息；选择在招职位后可查看本职位进展与匹配评估"
  >
    <template #actions>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        添加候选人
      </el-button>
    </template>

    <template #toolbar>
      <JobContextBar v-model="queryParams.jobId" @update:model-value="handleSearch" />
      <el-alert
        v-if="!queryParams.jobId"
        type="info"
        :closable="false"
        show-icon
        class="job-hint-alert"
        title="未选择在招职位时，列表仅显示候选人全局信息。选择职位后可查看本职位进展与匹配建议。"
      />
    </template>

    <template #filters>
      <el-input
        v-model="queryParams.name"
        placeholder="候选人姓名"
        :prefix-icon="Search"
        clearable
        class="filter-field filter-field--md"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="queryParams.phone"
        placeholder="电话号码"
        clearable
        class="filter-field filter-field--sm"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="queryParams.company"
        placeholder="当前公司"
        clearable
        class="filter-field filter-field--sm"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="filter-field filter-field--xs">
        <el-option
          v-for="s in statusOptions"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
      </el-select>
      <el-select v-model="queryParams.source" placeholder="来源" clearable class="filter-field filter-field--xs">
        <el-option
          v-for="s in sourceOptions"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
      </el-select>
    </template>
    <template #filterActions>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">重置</el-button>
    </template>

    <el-table :data="candidateList" style="width: 100%">
        <el-table-column prop="name" label="姓名" width="100" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-link" @click="handleView(row)">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="当前公司" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.currentCompany || row.company || '-' }}</template>
        </el-table-column>
        <el-table-column label="当前职位" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.currentTitle || row.position || '-' }}</template>
        </el-table-column>
        <el-table-column prop="workYears" label="工作年限" width="90" align="center">
          <template #default="{ row }">
            {{ row.workYears }} 年
          </template>
        </el-table-column>
        <el-table-column label="学历" width="80" align="center">
          <template #default="{ row }">{{ formatEducation(row.education) }}</template>
        </el-table-column>
        <el-table-column label="期望薪资" width="100" align="center">
          <template #default="{ row }">
            <span class="salary-text">{{ formatSalary(row.expectedSalary) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getSourceType(row.source)"
              size="small"
              disable-transitions
            >
              {{ getSourceLabel(row.source) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="queryParams.jobId" label="本职位进展" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ pipelineStageLabel(row.pipelineStage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-else label="全局状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-badge" :class="`status-${row.status?.toLowerCase()}`">
              {{ candidateStatusLabel(row.status) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="queryParams.jobId" label="匹配建议" min-width="200">
          <template #default="{ row }">
            <MatchVerdict
              v-if="row.matchDetail"
              :match-score="row.matchScore"
              :match-detail="row.matchDetail"
              mode="compact"
              :show-score="false"
            />
            <span v-else class="text-muted">待评估</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleLinkJob(row)">关联在招职位</el-button>
            <el-button type="primary" link size="small" @click="handleScreening(row)">查看匹配</el-button>
          </template>
        </el-table-column>
      </el-table>

    <div class="data-card-footer">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>

    <template #below>
    <!-- 添加/编辑候选人 -->
    <el-dialog v-model="formVisible" :title="isEditing ? '编辑候选人' : '添加候选人'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="candidateForm" :rules="formRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="candidateForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="candidateForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="candidateForm.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="当前公司">
          <el-input v-model="candidateForm.currentCompany" />
        </el-form-item>
        <el-form-item label="当前职位">
          <el-input v-model="candidateForm.currentTitle" />
        </el-form-item>
        <el-form-item label="工作年限">
          <el-input-number v-model="candidateForm.workYears" :min="0" :max="40" />
        </el-form-item>
        <el-form-item label="学历">
          <el-select v-model="candidateForm.education" placeholder="请选择" style="width: 100%">
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望薪资(K)">
          <el-input-number v-model="candidateForm.expectedSalary" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="candidateForm.source" style="width: 100%">
            <el-option v-for="s in sourceOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitCandidateForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关联岗位对话框 -->
    <el-dialog v-model="linkJobVisible" title="关联在招职位" width="480px">
      <el-form label-width="80px">
        <el-form-item label="候选人">
          <el-input :model-value="currentCandidate?.name" disabled />
        </el-form-item>
        <el-form-item label="选择在招职位">
          <el-select v-model="selectedJobId" placeholder="请选择要关联的在招职位" style="width: 100%">
            <el-option
              v-for="job in jobOptions"
              :key="job.id"
              :label="job.title"
              :value="job.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkJobVisible = false">取消</el-button>
        <el-button type="primary" :loading="linkJobLoading" @click="confirmLinkJob">确定关联</el-button>
      </template>
    </el-dialog>
    </template>
  </ListPageLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import ListPageLayout from '@/components/Layout/ListPageLayout.vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import JobContextBar from '@/components/common/JobContextBar.vue'
import {
  candidateStatusLabel,
  pipelineStageLabel,
  sourceLabel,
  educationLabel,
} from '@/constants/businessLabels'
import { getCandidateList, addToJob, createCandidate, updateCandidate } from '@/api/modules/candidate'
import { getJobList } from '@/api/modules/job'

const route = useRoute()
const router = useRouter()

const queryParams = reactive({
  name: '',
  phone: '',
  company: '',
  status: '',
  source: '',
  jobId: null as number | null,
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const candidateList = ref<any[]>([])

const linkJobVisible = ref(false)
const linkJobLoading = ref(false)
const currentCandidate = ref<any>(null)
const selectedJobId = ref<number | null>(null)
const jobOptions = ref<any[]>([])

const formVisible = ref(false)
const formLoading = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const candidateForm = reactive({
  name: '',
  phone: '',
  email: '',
  currentCompany: '',
  currentTitle: '',
  workYears: 0,
  education: '',
  expectedSalary: undefined as number | undefined,
  source: 'DIRECT',
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
}

const statusOptions = [
  { label: '新简历', value: 'NEW' },
  { label: '筛选中', value: 'SCREENING' },
  { label: '面试中', value: 'INTERVIEWING' },
  { label: '已发Offer', value: 'OFFER' },
  { label: '已入职', value: 'ONBOARD' },
  { label: '人才库', value: 'POOL' },
  { label: '黑名单', value: 'BLACKLIST' },
]

const sourceOptions = [
  { label: '平台', value: 'PLATFORM' },
  { label: '内推', value: 'REFERRAL' },
  { label: '猎头', value: 'HEADHUNTER' },
  { label: '直招', value: 'DIRECT' },
  { label: '门户', value: 'PORTAL' },
]

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    NEW: 'info', SCREENING: 'warning', INTERVIEWING: 'primary',
    OFFER: 'success', ONBOARD: 'success', POOL: 'info', BLACKLIST: 'danger',
  }
  return map[status] || 'info'
}

function getSourceType(source: string): string {
  const map: Record<string, string> = {
    PLATFORM: 'primary', REFERRAL: 'success', HEADHUNTER: 'warning',
    DIRECT: 'info', PORTAL: 'danger',
  }
  return map[source] || 'info'
}

function getSourceLabel(source: string): string {
  return sourceLabel(source)
}

function formatEducation(edu?: string): string {
  return educationLabel(edu)
}

function formatSalary(val?: number): string {
  if (val == null) return '-'
  const k = val >= 1000 ? Math.round(val / 1000) : val
  return `${k}K`
}

async function loadData() {
  try {
    const res: any = await getCandidateList(queryParams)
    candidateList.value = res.data?.list || res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载候选人列表失败')
    candidateList.value = []
    total.value = 0
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.name = ''
  queryParams.phone = ''
  queryParams.company = ''
  queryParams.status = ''
  queryParams.source = ''
  queryParams.jobId = null
  handleSearch()
}

function resetCandidateForm() {
  Object.assign(candidateForm, {
    name: '', phone: '', email: '', currentCompany: '', currentTitle: '',
    workYears: 0, education: '', expectedSalary: undefined, source: 'DIRECT',
  })
}

function handleCreate() {
  isEditing.value = false
  editingId.value = null
  resetCandidateForm()
  formVisible.value = true
}

function handleView(row: any) {
  router.push(`/pipeline/candidates/${row.id}`)
}

function handleEdit(row: any) {
  isEditing.value = true
  editingId.value = row.id
  Object.assign(candidateForm, {
    name: row.name || '',
    phone: row.phone || '',
    email: row.email || '',
    currentCompany: row.currentCompany || row.company || '',
    currentTitle: row.currentTitle || row.position || '',
    workYears: row.workYears || 0,
    education: row.education || '',
    expectedSalary: row.expectedSalary,
    source: row.source || 'DIRECT',
  })
  formVisible.value = true
}

async function submitCandidateForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    formLoading.value = true
    try {
      if (isEditing.value && editingId.value) {
        await updateCandidate(editingId.value, { ...candidateForm })
        ElMessage.success('候选人已更新')
      } else {
        await createCandidate({ ...candidateForm })
        ElMessage.success('候选人已添加')
      }
      formVisible.value = false
      loadData()
    } catch {
      ElMessage.error('保存失败')
    } finally {
      formLoading.value = false
    }
  })
}

async function handleLinkJob(row: any) {
  currentCandidate.value = row
  selectedJobId.value = null
  linkJobVisible.value = true
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 100 })
    jobOptions.value = res.data?.list || res.data?.records || []
  } catch {
    ElMessage.error('加载在招职位列表失败')
    jobOptions.value = []
  }
}

async function confirmLinkJob() {
  if (!selectedJobId.value || !currentCandidate.value) return
  linkJobLoading.value = true
  try {
    await addToJob(currentCandidate.value.id, selectedJobId.value)
    ElMessage.success('已关联在招职位')
    linkJobVisible.value = false
    loadData()
  } catch {
    // 错误信息由 request 拦截器展示
  } finally {
    linkJobLoading.value = false
  }
}

function handleScreening(row: any) {
  const jobId = queryParams.jobId || row.jobId
  if (jobId) {
    router.push({
      path: '/pipeline/decision',
      query: { candidateId: String(row.id), jobId: String(jobId) },
    })
    return
  }
  currentCandidate.value = row
  selectedJobId.value = null
  linkJobVisible.value = true
  if (!jobOptions.value.length) {
    getJobList({ pageNum: 1, pageSize: 100 }).then((res: any) => {
      jobOptions.value = res.data?.list || res.data?.records || []
    })
  }
  ElMessage.info('请先选择在招职位，或关联职位后再查看匹配评估')
}

onMounted(async () => {
  const qJob = Number(route.query.jobId)
  if (qJob) queryParams.jobId = qJob
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' })
    jobOptions.value = res.data?.list || res.data?.records || []
  } catch { /* ignore */ }
  loadData()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.job-hint-alert {
  margin-top: $spacing-md;
}

.filter-field {
  width: 160px;

  &--md { width: 170px; }
  &--sm { width: 150px; }
  &--xs { width: 120px; }
}


.salary-text {
  font-weight: 500;
  color: $text-primary;
}

.text-muted {
  color: #94a3b8;
  font-size: 12px;
}

// 自定义状态 badge，比 el-tag 更有设计感
.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: $border-radius-full;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;

  &.status-new {
    background: #F1F5F9;
    color: $text-secondary;
  }

  &.status-screening {
    background: #FEF3C7;
    color: #92400E;
  }

  &.status-interviewing {
    background: #DBEAFE;
    color: #1E40AF;
  }

  &.status-offer {
    background: #D1FAE5;
    color: #065F46;
  }

  &.status-onboard {
    background: #D1FAE5;
    color: #065F46;
  }

  &.status-pool {
    background: #F1F5F9;
    color: $text-secondary;
  }

  &.status-blacklist {
    background: #FEE2E2;
    color: #991B1B;
  }
}
</style>
