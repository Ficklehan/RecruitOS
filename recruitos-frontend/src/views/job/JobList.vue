<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">岗位列表</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建岗位
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-input
        v-model="queryParams.title"
        placeholder="搜索岗位标题"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.status" placeholder="岗位状态" clearable style="width: 140px">
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

    <!-- 数据表格 -->
    <div class="data-card">
      <el-table :data="jobList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="jobNo" label="岗位编号" width="150" show-overflow-tooltip />
        <el-table-column prop="title" label="岗位标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-link" @click="handleView(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="demandNo" label="关联需求" width="150" show-overflow-tooltip />
        <el-table-column prop="headcount" label="招聘人数" width="100" align="center" />
        <el-table-column prop="onboardCount" label="已到岗" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-success': row.onboardCount >= row.headcount }">
              {{ row.onboardCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              type="success" link size="small"
              @click="handleActivate(row)"
            >激活</el-button>
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
            <el-button
              type="primary" link size="small"
              @click="handleParseJd(row)"
            >解析JD</el-button>
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

    <!-- 关闭原因对话框 -->
    <el-dialog v-model="closeDialogVisible" title="关闭岗位" width="420px">
      <el-form label-width="80px">
        <el-form-item label="关闭原因">
          <el-input
            v-model="closeReason"
            type="textarea"
            :rows="3"
            placeholder="请输入关闭原因"
          />
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
import { getJobList, activateJob, pauseJob, closeJob, parseJd } from '@/api/modules/job'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  title: '',
  status: '',
  demandNo: '',
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const jobList = ref<any[]>([])

// 关闭对话框
const closeDialogVisible = ref(false)
const closeReason = ref('')
const closingJobId = ref<number | null>(null)

// 状态选项
const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '招聘中', value: 'ACTIVE' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已关闭', value: 'CLOSED' },
]

// 状态标签映射
function getStatusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info',
    ACTIVE: 'success',
    PAUSED: 'warning',
    CLOSED: 'info',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    ACTIVE: '招聘中',
    PAUSED: '已暂停',
    CLOSED: '已关闭',
  }
  return map[status] || status
}

// 加载数据
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
  router.push('/position/job/jd-editor')
}

function handleView(row: any) {
  router.push(`/position/job/jd-editor?id=${row.id}`)
}

function handleEdit(row: any) {
  router.push(`/position/job/jd-editor?id=${row.id}`)
}

async function handleActivate(row: any) {
  try {
    await ElMessageBox.confirm('确定要激活该岗位吗？激活后将开始招聘。', '激活确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await activateJob(row.id)
    ElMessage.success('岗位已激活')
    loadData()
  } catch {
    // 取消操作
  }
}

async function handlePause(row: any) {
  try {
    await ElMessageBox.confirm('确定要暂停该岗位的招聘吗？', '暂停确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await pauseJob(row.id)
    ElMessage.success('岗位已暂停')
    loadData()
  } catch {
    // 取消操作
  }
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
    ElMessage.success('岗位已关闭')
    closeDialogVisible.value = false
    loadData()
  } catch {
    // 操作失败
  }
}

async function handleParseJd(row: any) {
  try {
    await ElMessageBox.confirm('将使用 AI 解析该岗位的 JD 并提取标签，是否继续？', '解析JD', {
      confirmButtonText: '开始解析',
      cancelButtonText: '取消',
      type: 'info',
    })
    await parseJd(row.id)
    ElMessage.success('JD 解析完成')
    router.push(`/position/job/jd-editor?id=${row.id}`)
  } catch {
    // 取消操作
  }
}

onMounted(() => {
  loadData()
})
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
</style>
