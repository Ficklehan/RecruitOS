<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">在招职位</h2>
        <p class="page-subtitle">管理正在招聘的职位，进入工作台查看进展与渠道招聘</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建在招职位
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="queryParams.title"
        placeholder="搜索职位名称"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.status" placeholder="招聘状态" clearable style="width: 140px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-input
        v-model="queryParams.demandNo"
        placeholder="关联需求编号"
        clearable
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">
        <el-icon><RefreshRight /></el-icon>
        重置
      </el-button>
    </div>

    <div class="data-card">
      <el-table v-if="jobList.length" :data="jobList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="jobNo" label="职位编号" width="150" show-overflow-tooltip />
        <el-table-column prop="title" label="职位名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-link" @click="handleView(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="demandNo" label="关联需求" width="150" show-overflow-tooltip />
        <el-table-column prop="headcount" label="招聘人数" width="100" align="center" />
        <el-table-column prop="onboardCount" label="已到岗" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-success': row.onboardCount >= row.headcount }">
              {{ row.onboardCount || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ jobStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">工作台</el-button>
            <el-button type="primary" link size="small" @click="handleEditRequirements(row)">任职要求</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              type="success" link size="small"
              @click="handleActivate(row)"
            >开始招聘</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="warning" link size="small"
              @click="handlePause(row)"
            >暂停</el-button>
            <el-button
              v-if="row.status !== 'CLOSED'"
              type="danger" link size="small"
              @click="handleClose(row)"
            >关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyStateCta
        v-else
        title="暂无在招职位"
        description="创建职位并完善任职要求后，即可开始渠道招聘与候选人匹配"
        :actions="[
          { label: '创建在招职位', type: 'primary', onClick: handleCreate },
          { label: '查看招聘需求', type: 'default', onClick: () => router.push('/planning/demands') },
        ]"
      />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>

    <el-dialog v-model="closeDialogVisible" title="关闭职位" width="420px">
      <el-form label-width="80px">
        <el-form-item label="关闭原因">
          <el-input v-model="closeReason" type="textarea" :rows="3" placeholder="请输入关闭原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmClose">确定关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, RefreshRight } from '@element-plus/icons-vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { jobStatusLabel } from '@/constants/businessLabels'
import { getJobList, activateJob, pauseJob, closeJob } from '@/api/modules/job'

const router = useRouter()

const queryParams = reactive({
  title: '',
  status: '',
  demandNo: '',
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const jobList = ref<any[]>([])
const closeDialogVisible = ref(false)
const closeReason = ref('')
const closingJobId = ref<number | null>(null)

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '招聘中', value: 'ACTIVE' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已关闭', value: 'CLOSED' },
]

function getStatusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', ACTIVE: 'success', PAUSED: 'warning', CLOSED: 'info',
  }
  return map[status] || 'info'
}

async function loadData() {
  const res: any = await getJobList(queryParams)
  jobList.value = res.data?.list || res.data?.records || []
  total.value = res.data?.total || 0
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.title = ''
  queryParams.status = ''
  queryParams.demandNo = ''
  handleSearch()
}

function handleCreate() {
  router.push('/planning/jobs/create')
}

function handleView(row: any) {
  router.push(`/planning/jobs/${row.id}`)
}

function handleEditRequirements(row: any) {
  router.push(`/planning/jobs/${row.id}/jd`)
}

async function handleActivate(row: any) {
  try {
    await ElMessageBox.confirm('确定开始该职位的招聘吗？', '开始招聘', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await activateJob(row.id)
    ElMessage.success('职位已开始招聘')
    loadData()
  } catch { /* cancel */ }
}

async function handlePause(row: any) {
  try {
    await ElMessageBox.confirm('确定暂停该职位的招聘吗？', '暂停招聘', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await pauseJob(row.id)
    ElMessage.success('职位已暂停')
    loadData()
  } catch { /* cancel */ }
}

function handleClose(row: any) {
  closingJobId.value = row.id
  closeReason.value = ''
  closeDialogVisible.value = true
}

async function confirmClose() {
  if (!closingJobId.value) return
  try {
    await closeJob(closingJobId.value, closeReason.value)
    ElMessage.success('职位已关闭')
    closeDialogVisible.value = false
    loadData()
  } catch { /* error */ }
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.title-link {
  color: $primary-color;
  cursor: pointer;
  font-weight: 500;
  &:hover { text-decoration: underline; }
}
.text-success { color: $success-color; font-weight: 600; }
.table-pagination { margin-top: 16px; justify-content: flex-end; }
</style>
