<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">招聘需求</h2>
        <p class="page-subtitle">部门提出的用人申请，审批通过后可创建在招职位</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建招聘需求
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-input
        v-model="queryParams.title"
        placeholder="搜索招聘需求"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.orgId" placeholder="所属部门" clearable style="width: 160px">
        <el-option v-for="dept in departmentOptions" :key="dept.value" :label="dept.label" :value="dept.value" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="需求状态" clearable style="width: 140px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="queryParams.urgency" placeholder="紧急程度" clearable style="width: 140px">
        <el-option v-for="u in urgencyOptions" :key="u.value" :label="u.label" :value="u.value" />
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
      <el-table v-if="demandList.length" :data="demandList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="demandNo" label="需求编号" width="140" show-overflow-tooltip />
        <el-table-column prop="title" label="需求标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-link" @click="handleView(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="headCount" label="招聘人数" width="100" align="center" />
        <el-table-column prop="jobLevel" label="级别" width="80" align="center" />
        <el-table-column label="薪酬范围" width="160" align="center">
          <template #default="{ row }">
            {{ formatSalary(row.salaryMin) }}K - {{ formatSalary(row.salaryMax) }}K
          </template>
        </el-table-column>
        <el-table-column prop="urgency" label="紧急程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getUrgencyType(row.urgency)" size="small" disable-transitions>
              {{ getUrgencyLabel(row.urgency) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              type="primary" link size="small"
              @click="handleEdit(row)"
            >编辑</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              type="success" link size="small"
              @click="handleSubmit(row)"
            >提交</el-button>
            <el-button
              v-if="row.status !== 'CLOSED' && row.status !== 'COMPLETED'"
              type="danger" link size="small"
              @click="handleClose(row)"
            >关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyStateCta
        v-else
        title="暂无招聘需求"
        description="创建招聘需求并提交审批后，可据此创建在招职位并开始招聘"
        :actions="[
          { label: '创建招聘需求', type: 'primary', onClick: handleCreate },
          { label: '查看在招职位', type: 'default', onClick: () => router.push('/planning/jobs') },
        ]"
      />

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, RefreshRight } from '@element-plus/icons-vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { demandStatusLabel } from '@/constants/businessLabels'
import { getDemandList, submitDemand, closeDemand } from '@/api/modules/demand'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  title: '',
  orgId: '',
  status: '',
  urgency: '',
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const demandList = ref<any[]>([])

// 下拉选项
const departmentOptions = [
  { label: '技术部', value: '技术部' },
  { label: '产品部', value: '产品部' },
  { label: '设计部', value: '设计部' },
  { label: '运营部', value: '运营部' },
  { label: '市场部', value: '市场部' },
  { label: '人力资源部', value: '人力资源部' },
]

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '审批中', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已创建职位', value: 'JOB_CREATED' },
  { label: '招聘中', value: 'RECRUITING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已关闭', value: 'CLOSED' },
]

const urgencyOptions = [
  { label: '普通', value: 'NORMAL' },
  { label: '紧急', value: 'URGENT' },
  { label: '特急', value: 'CRITICAL' },
]

// 格式化薪资
function formatSalary(val: any): string {
  if (val == null) return '-'
  const num = Number(val)
  return isNaN(num) ? String(val) : num.toFixed(0)
}

// 状态标签映射
function getStatusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    JOB_CREATED: 'primary',
    RECRUITING: 'warning',
    COMPLETED: 'success',
    CLOSED: 'info',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  return demandStatusLabel(status)
}

// 紧急程度标签映射
function getUrgencyType(level: string) {
  const map: Record<string, string> = {
    NORMAL: 'info',
    URGENT: 'warning',
    CRITICAL: 'danger',
  }
  return map[level] || 'info'
}

function getUrgencyLabel(level: string) {
  const map: Record<string, string> = {
    NORMAL: '普通',
    URGENT: '紧急',
    CRITICAL: '特急',
  }
  return map[level] || level
}

// 加载数据
async function loadData() {
  try {
    const res: any = await getDemandList(queryParams)
    demandList.value = res.data?.list || res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    demandList.value = []
    total.value = 0
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.title = ''
  queryParams.orgId = ''
  queryParams.status = ''
  queryParams.urgency = ''
  handleSearch()
}

function handleCreate() {
  router.push('/planning/demands/create')
}

function handleView(row: any) {
  router.push(`/planning/demands/${row.id}`)
}

function handleEdit(row: any) {
  router.push(`/planning/demands/create?id=${row.id}`)
}

async function handleSubmit(row: any) {
  try {
    await ElMessageBox.confirm('确定要提交该需求进行审批吗？', '提交确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await submitDemand(row.id)
    ElMessage.success('已提交审批')
    loadData()
  } catch {
    // 取消操作
  }
}

async function handleClose(row: any) {
  try {
    await ElMessageBox.confirm('确定要关闭该需求吗？关闭后不可恢复。', '关闭确认', {
      confirmButtonText: '确定关闭',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await closeDemand(row.id)
    ElMessage.success('需求已关闭')
    loadData()
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
