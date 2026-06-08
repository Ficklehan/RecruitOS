<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">录用通知</h2>
        <p class="page-subtitle">管理候选人录用通知（Offer）的发送与审批状态</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建录用通知
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索候选人 / 在招职位"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.status" placeholder="通知状态" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="待审批" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已发送" value="SENT" />
        <el-option label="已接受" value="ACCEPTED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
      <el-date-picker
        v-model="queryParams.dateRange"
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
      <el-table :data="offerList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="candidateName" label="候选人" width="100">
          <template #default="{ row }">
            <span class="title-link">{{ row.candidateName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="jobTitle" label="在招职位" min-width="160" show-overflow-tooltip />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="salary" label="薪资" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
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
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="候选人" prop="candidateName">
          <el-input v-model="formData.candidateName" placeholder="请输入候选人姓名" />
        </el-form-item>
        <el-form-item label="在招职位" prop="jobTitle">
          <el-input v-model="formData.jobTitle" placeholder="请输入职位名称" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="formData.department" placeholder="请输入所属部门" />
        </el-form-item>
        <el-form-item label="薪资" prop="salary">
          <el-input v-model="formData.salary" placeholder="例如: 30K/月" />
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
import { offerStatusLabel } from '@/constants/businessLabels'
import { getOfferList, createOffer } from '@/api/modules/offer'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  keyword: '',
  status: '',
  dateRange: null as string[] | null,
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const offerList = ref<any[]>([])

// 对话框
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const currentEditId = ref<number | null>(null)

const dialogTitle = computed(() => dialogType.value === 'create' ? '创建录用通知' : '编辑录用通知')

const formData = reactive({
  candidateName: '',
  jobTitle: '',
  department: '',
  salary: '',
  remark: '',
})

const formRules: FormRules = {
  candidateName: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }],
  jobTitle: [{ required: true, message: '请输入职位名称', trigger: 'blur' }],
  department: [{ required: true, message: '请输入所属部门', trigger: 'blur' }],
  salary: [{ required: true, message: '请输入薪资', trigger: 'blur' }],
}

// 状态映射
function getStatusType(status: string): string {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    SENT: 'info',
    ACCEPTED: 'success',
    REJECTED: 'danger',
    EXPIRED: 'info',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  return offerStatusLabel(status)
}

// 加载数据
async function loadData() {
  const res: any = await getOfferList(queryParams)
  offerList.value = res.data?.list || res.data?.records || []
  total.value = res.data?.total || 0
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.dateRange = null
  handleSearch()
}

function resetForm() {
  formData.candidateName = ''
  formData.jobTitle = ''
  formData.department = ''
  formData.salary = ''
  formData.remark = ''
  currentEditId.value = null
}

function handleCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function handleView(row: any) {
  router.push(`/pipeline/offers/${row.id}`)
}

function handleEdit(row: any) {
  dialogType.value = 'edit'
  currentEditId.value = row.id
  formData.candidateName = row.candidateName
  formData.jobTitle = row.jobTitle
  formData.department = row.department
  formData.salary = row.salary
  formData.remark = row.remark || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (dialogType.value === 'create') {
          await createOffer({
            candidateName: formData.candidateName,
            jobTitle: formData.jobTitle,
            department: formData.department,
            salary: formData.salary,
            remark: formData.remark,
          })
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        loadData()
      } catch {
        // API error already displayed by interceptor
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
