<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">入职列表</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建入职
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索候选人/岗位"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.status" placeholder="入职状态" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="待入职" value="PENDING" />
        <el-option label="已确认" value="CONFIRMED" />
        <el-option label="已取消" value="CANCELLED" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-date-picker
        v-model="queryParams.date"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
        value-format="YYYY-MM-DD"
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
      <el-table :data="onboardList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="candidateName" label="候选人" width="100">
          <template #default="{ row }">
            <span class="title-link">{{ row.candidateName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="jobTitle" label="岗位" min-width="160" show-overflow-tooltip />
        <el-table-column prop="onboardDate" label="入职日期" width="130" align="center" />
        <el-table-column prop="hrManager" label="HR负责人" width="110" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="warning" link size="small" @click="handleTask(row)">任务</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
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

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="候选人" prop="candidateName">
          <el-input v-model="formData.candidateName" placeholder="请输入候选人姓名" />
        </el-form-item>
        <el-form-item label="岗位" prop="jobTitle">
          <el-input v-model="formData.jobTitle" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="入职日期" prop="onboardDate">
          <el-date-picker
            v-model="formData.onboardDate"
            type="date"
            placeholder="选择入职日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="HR负责人" prop="hrManager">
          <el-input v-model="formData.hrManager" placeholder="请输入HR负责人" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Plus, RefreshRight } from '@element-plus/icons-vue'
import { getOnboardList, createOnboard, updateOnboard } from '@/api/modules/onboard'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  keyword: '',
  status: '',
  date: null as string[] | null,
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const onboardList = ref<any[]>([])

// 对话框
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const currentEditId = ref<number | null>(null)

const dialogTitle = computed(() => dialogType.value === 'create' ? '新建入职' : '编辑入职')

const formData = reactive({
  candidateName: '',
  jobTitle: '',
  onboardDate: '',
  hrManager: '',
  remark: '',
})

const formRules: FormRules = {
  candidateName: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }],
  jobTitle: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  onboardDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
  hrManager: [{ required: true, message: '请输入HR负责人', trigger: 'blur' }],
}

// 状态映射
function getStatusType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    CONFIRMED: 'info',
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

async function loadData() {
  const params: any = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  if (queryParams.keyword) params.keyword = queryParams.keyword
  if (queryParams.status) params.status = queryParams.status
  if (queryParams.date && queryParams.date.length === 2) {
    params.startDate = queryParams.date[0]
    params.endDate = queryParams.date[1]
  }
  const res = await getOnboardList(params)
  onboardList.value = res.data.records || res.data.list || res.data || []
  total.value = res.data.total || onboardList.value.length
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.date = null
  handleSearch()
}

function resetForm() {
  formData.candidateName = ''
  formData.jobTitle = ''
  formData.onboardDate = ''
  formData.hrManager = ''
  formData.remark = ''
  currentEditId.value = null
}

function handleCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function handleView(row: any) {
  ElMessage.info(`查看入职详情: ${row.candidateName}`)
}

function handleTask(row: any) {
  router.push(`/onboard/task?id=${row.id}`)
}

function handleEdit(row: any) {
  dialogType.value = 'edit'
  currentEditId.value = row.id
  formData.candidateName = row.candidateName
  formData.jobTitle = row.jobTitle
  formData.onboardDate = row.onboardDate
  formData.hrManager = row.hrManager
  formData.remark = row.remark || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const payload = {
          candidateName: formData.candidateName,
          jobTitle: formData.jobTitle,
          onboardDate: formData.onboardDate,
          hrManager: formData.hrManager,
          remark: formData.remark,
        }
        if (dialogType.value === 'create') {
          await createOnboard(payload)
          ElMessage.success('创建成功')
        } else {
          await updateOnboard(currentEditId.value!, payload)
          ElMessage.success('更新成功')
        }
        dialogVisible.value = false
        loadData()
      } catch {
        // error handled by interceptor
      } finally {
        submitLoading.value = false
      }
    }
  })
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
