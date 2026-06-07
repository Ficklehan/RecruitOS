<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">内推列表</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        发起内推
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索推荐人/候选人"
        :prefix-icon="Search"
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 140px">
        <el-option
          v-for="s in statusOptions"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
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
      <el-table :data="referralList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="referrerName" label="推荐人" width="120" />
        <el-table-column prop="candidateName" label="候选人" width="120">
          <template #default="{ row }">
            <span class="title-link" @click="handleView(row)">{{ row.candidateName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="jobTitle" label="推荐岗位" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="推荐时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
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

    <!-- 发起内推对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑内推' : '发起内推'" width="520px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="推荐人" prop="referrerName">
          <el-input v-model="formData.referrerName" placeholder="请输入推荐人姓名" />
        </el-form-item>
        <el-form-item label="候选人" prop="candidateName">
          <el-input v-model="formData.candidateName" placeholder="请输入候选人姓名" />
        </el-form-item>
        <el-form-item label="推荐岗位" prop="jobId">
          <el-select v-model="formData.jobId" placeholder="请选择推荐岗位" style="width: 100%">
            <el-option
              v-for="job in jobOptions"
              :key="job.id"
              :label="job.title"
              :value="job.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入推荐备注"
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, RefreshRight } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getReferralList, createReferral } from '@/api/modules/referral'

// 查询参数
const queryParams = reactive({
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const referralList = ref<any[]>([])

// 对话框相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const formData = reactive({
  referrerName: '',
  candidateName: '',
  jobId: null as number | null,
  remark: '',
})

const formRules: FormRules = {
  referrerName: [{ required: true, message: '请输入推荐人姓名', trigger: 'blur' }],
  candidateName: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }],
  jobId: [{ required: true, message: '请选择推荐岗位', trigger: 'change' }],
}

// 岗位选项
const jobOptions = ref<any[]>([])

async function loadJobOptions() {
  try {
    const res = await getReferralList({ pageSize: 999 })
    const jobs = new Map<number, string>()
    ;(res.data.records || res.data.list || res.data || []).forEach((item: any) => {
      if (item.jobId && item.jobTitle) jobs.set(item.jobId, item.jobTitle)
    })
    jobOptions.value = Array.from(jobs, ([id, title]) => ({ id, title }))
  } catch {
    // error handled by interceptor
  }
}

// 状态选项
const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '面试中', value: 'INTERVIEWING' },
  { label: 'Offer', value: 'OFFER' },
  { label: '已入职', value: 'ONBOARD' },
  { label: '已拒绝', value: 'REJECTED' },
]

// 状态标签映射
function getStatusType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    INTERVIEWING: 'info',
    OFFER: 'success',
    ONBOARD: 'success',
    REJECTED: 'danger',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    INTERVIEWING: '面试中',
    OFFER: 'Offer',
    ONBOARD: '已入职',
    REJECTED: '已拒绝',
  }
  return map[status] || status
}

// 加载数据
async function loadData() {
  const params: any = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  if (queryParams.keyword) params.keyword = queryParams.keyword
  if (queryParams.status) params.status = queryParams.status
  const res = await getReferralList(params)
  referralList.value = res.data.records || res.data.list || res.data || []
  total.value = res.data.total || referralList.value.length
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = ''
  handleSearch()
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  formData.referrerName = ''
  formData.candidateName = ''
  formData.jobId = null
  formData.remark = ''
  dialogVisible.value = true
}

function handleView(row: any) {
  ElMessage.info(`查看内推详情: ${row.candidateName}`)
}

function handleEdit(row: any) {
  isEdit.value = true
  editId.value = row.id
  formData.referrerName = row.referrerName
  formData.candidateName = row.candidateName
  formData.jobId = row.jobId
  formData.remark = row.remark
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const payload = {
          referrerName: formData.referrerName,
          candidateName: formData.candidateName,
          jobId: formData.jobId,
          remark: formData.remark,
        }
        await createReferral(payload)
        ElMessage.success('内推发起成功')
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
  loadJobOptions()
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
