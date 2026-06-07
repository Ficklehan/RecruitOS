<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">候选人列表</h2>
        <p class="page-subtitle">管理所有候选人信息，跟踪招聘进度</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        添加候选人
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-input
        v-model="queryParams.name"
        placeholder="候选人姓名"
        :prefix-icon="Search"
        clearable
        style="width: 170px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="queryParams.phone"
        placeholder="电话号码"
        clearable
        style="width: 150px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="queryParams.company"
        placeholder="当前公司"
        clearable
        style="width: 150px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
        <el-option
          v-for="s in statusOptions"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
      </el-select>
      <el-select v-model="queryParams.source" placeholder="来源" clearable style="width: 120px">
        <el-option
          v-for="s in sourceOptions"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="data-card">
      <el-table :data="candidateList" style="width: 100%">
        <el-table-column prop="name" label="姓名" width="100" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-link" @click="handleView(row)">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="company" label="当前公司" min-width="150" show-overflow-tooltip />
        <el-table-column prop="position" label="当前职位" min-width="150" show-overflow-tooltip />
        <el-table-column prop="workYears" label="工作年限" width="90" align="center">
          <template #default="{ row }">
            {{ row.workYears }} 年
          </template>
        </el-table-column>
        <el-table-column prop="education" label="学历" width="80" align="center" />
        <el-table-column prop="expectedSalary" label="期望薪资" width="100" align="center">
          <template #default="{ row }">
            <span class="salary-text">{{ row.expectedSalary }}K</span>
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
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-badge" :class="`status-${row.status?.toLowerCase()}`">
              {{ getStatusLabel(row.status) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleLinkJob(row)">关联岗位</el-button>
            <el-button type="primary" link size="small" @click="handleScreening(row)">筛选</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 关联岗位对话框 -->
    <el-dialog v-model="linkJobVisible" title="关联岗位" width="480px">
      <el-form label-width="80px">
        <el-form-item label="候选人">
          <el-input :model-value="currentCandidate?.name" disabled />
        </el-form-item>
        <el-form-item label="选择岗位">
          <el-select v-model="selectedJobId" placeholder="请选择要关联的岗位" style="width: 100%">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, RefreshRight } from '@element-plus/icons-vue'
import { getCandidateList, addToJob } from '@/api/modules/candidate'
import { getJobList } from '@/api/modules/job'

const router = useRouter()

const queryParams = reactive({
  name: '',
  phone: '',
  company: '',
  status: '',
  source: '',
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

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    NEW: '新简历', SCREENING: '筛选中', INTERVIEWING: '面试中',
    OFFER: '已发Offer', ONBOARD: '已入职', POOL: '人才库', BLACKLIST: '黑名单',
  }
  return map[status] || status
}

function getSourceType(source: string): string {
  const map: Record<string, string> = {
    PLATFORM: 'primary', REFERRAL: 'success', HEADHUNTER: 'warning',
    DIRECT: 'info', PORTAL: 'danger',
  }
  return map[source] || 'info'
}

function getSourceLabel(source: string): string {
  const map: Record<string, string> = {
    PLATFORM: '平台', REFERRAL: '内推', HEADHUNTER: '猎头',
    DIRECT: '直招', PORTAL: '门户',
  }
  return map[source] || source
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
  handleSearch()
}

function handleCreate() {
  ElMessage.info('添加候选人功能开发中')
}

function handleView(row: any) {
  router.push(`/candidate/decision?candidateId=${row.id}&jobId=1`)
}

function handleEdit(row: any) {
  ElMessage.info(`编辑候选人: ${row.name}`)
}

async function handleLinkJob(row: any) {
  currentCandidate.value = row
  selectedJobId.value = null
  linkJobVisible.value = true
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 100 })
    jobOptions.value = res.data?.list || res.data?.records || []
  } catch {
    ElMessage.error('加载岗位列表失败')
    jobOptions.value = []
  }
}

async function confirmLinkJob() {
  if (!selectedJobId.value || !currentCandidate.value) return
  linkJobLoading.value = true
  try {
    await addToJob(currentCandidate.value.id, selectedJobId.value)
    ElMessage.success('关联岗位成功')
    linkJobVisible.value = false
  } catch {
    // 操作失败
  } finally {
    linkJobLoading.value = false
  }
}

function handleScreening(row: any) {
  router.push(`/candidate/decision?candidateId=${row.id}&jobId=1`)
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.salary-text {
  font-weight: 500;
  color: $text-primary;
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
