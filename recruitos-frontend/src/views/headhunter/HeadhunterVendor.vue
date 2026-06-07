<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">供应商管理</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        添加供应商
      </el-button>
    </div>

    <!-- 数据表格 -->
    <div class="data-card">
      <el-table :data="vendorList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="vendorName" label="供应商名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-link" @click="handleViewRecommendations(row)">{{ row.vendorName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column label="合同期限" width="200">
          <template #default="{ row }">
            {{ row.contractStart }} ~ {{ row.contractEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="commissionRate" label="佣金比例" width="100" align="center">
          <template #default="{ row }">
            {{ row.commissionRate }}%
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="recommendCount" label="推荐数" width="90" align="center" />
        <el-table-column prop="hireCount" label="成功入职数" width="110" align="center" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleViewRecommendations(row)">查看推荐</el-button>
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

    <!-- 添加/编辑供应商对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑供应商' : '添加供应商'" width="560px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="供应商名称" prop="vendorName">
          <el-input v-model="formData.vendorName" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="formData.contactPerson" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="contactEmail">
          <el-input v-model="formData.contactEmail" placeholder="请输入联系邮箱" />
        </el-form-item>
        <el-form-item label="合同开始" prop="contractStart">
          <el-date-picker
            v-model="formData.contractStart"
            type="date"
            placeholder="选择合同开始日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="合同结束" prop="contractEnd">
          <el-date-picker
            v-model="formData.contractEnd"
            type="date"
            placeholder="选择合同结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="佣金比例" prop="commissionRate">
          <el-input-number
            v-model="formData.commissionRate"
            :min="0"
            :max="100"
            :precision="1"
            :step="0.5"
            style="width: 100%"
          />
          <span class="form-tip">单位：%</span>
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
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getHeadhunterVendorList, createHeadhunterVendor, updateHeadhunterVendor } from '@/api/modules/headhunter'

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const vendorList = ref<any[]>([])

// 对话框相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const formData = reactive({
  vendorName: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  contractStart: '',
  contractEnd: '',
  commissionRate: 20,
})

const formRules: FormRules = {
  vendorName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  contactEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  contractStart: [{ required: true, message: '请选择合同开始日期', trigger: 'change' }],
  contractEnd: [{ required: true, message: '请选择合同结束日期', trigger: 'change' }],
  commissionRate: [{ required: true, message: '请输入佣金比例', trigger: 'blur' }],
}

// 加载数据
async function loadData() {
  const params: any = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  const res = await getHeadhunterVendorList(params)
  vendorList.value = res.data.records || res.data.list || res.data || []
  total.value = res.data.total || vendorList.value.length
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  formData.vendorName = ''
  formData.contactPerson = ''
  formData.contactPhone = ''
  formData.contactEmail = ''
  formData.contractStart = ''
  formData.contractEnd = ''
  formData.commissionRate = 20
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  editId.value = row.id
  formData.vendorName = row.vendorName
  formData.contactPerson = row.contactPerson
  formData.contactPhone = row.contactPhone
  formData.contactEmail = row.contactEmail
  formData.contractStart = row.contractStart
  formData.contractEnd = row.contractEnd
  formData.commissionRate = row.commissionRate
  dialogVisible.value = true
}

function handleViewRecommendations(row: any) {
  ElMessage.info(`查看 ${row.vendorName} 的推荐列表`)
}

async function handleStatusChange(row: any) {
  try {
    await updateHeadhunterVendor(row.id, { status: row.status })
    ElMessage.success(`${row.vendorName} 已${row.status === 1 ? '启用' : '停用'}`)
  } catch {
    row.status = row.status === 1 ? 0 : 1
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const payload = {
          vendorName: formData.vendorName,
          contactPerson: formData.contactPerson,
          contactPhone: formData.contactPhone,
          contactEmail: formData.contactEmail,
          contractStart: formData.contractStart,
          contractEnd: formData.contractEnd,
          commissionRate: formData.commissionRate,
        }
        if (isEdit.value) {
          await updateHeadhunterVendor(editId.value!, payload)
          ElMessage.success('供应商信息已更新')
        } else {
          await createHeadhunterVendor(payload)
          ElMessage.success('供应商添加成功')
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

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: $text-secondary;
}
</style>
