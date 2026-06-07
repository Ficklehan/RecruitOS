<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" text>
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2 class="page-title">{{ isEdit ? '编辑需求' : '创建需求' }}</h2>
      </div>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        class="demand-form"
      >
        <!-- 基本信息 -->
        <div class="form-section">
          <h3 class="section-title">基本信息</h3>
          <div class="form-grid">
            <el-form-item label="需求标题" prop="title" class="span-2">
              <el-input v-model="formData.title" placeholder="请输入需求标题" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="所属部门" prop="orgId">
              <el-tree-select
                v-model="formData.orgId"
                :data="departmentTree"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="请选择所属部门"
                check-strictly
                clearable
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="招聘人数" prop="headCount">
              <el-input-number v-model="formData.headCount" :min="1" :max="99" style="width: 100%" />
            </el-form-item>
            <el-form-item label="岗位级别" prop="jobLevel">
              <el-select v-model="formData.jobLevel" placeholder="请选择岗位级别" style="width: 100%">
                <el-option label="P6 - 高级工程师" value="P6" />
                <el-option label="P7 - 资深工程师" value="P7" />
                <el-option label="P8 - 技术专家" value="P8" />
                <el-option label="P9 - 高级技术专家" value="P9" />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <!-- 薪酬与时间 -->
        <div class="form-section">
          <h3 class="section-title">薪酬与时间</h3>
          <div class="form-grid">
            <el-form-item label="期望薪酬范围" prop="salaryRange" class="span-2">
              <div class="salary-range">
                <el-input-number
                  v-model="formData.salaryMin"
                  :min="0"
                  :max="formData.salaryMax || 999"
                  placeholder="最低"
                  controls-position="right"
                  style="flex: 1"
                />
                <span class="range-separator">-</span>
                <el-input-number
                  v-model="formData.salaryMax"
                  :min="formData.salaryMin || 0"
                  placeholder="最高"
                  controls-position="right"
                  style="flex: 1"
                />
                <span class="range-unit">K / 月</span>
              </div>
            </el-form-item>
            <el-form-item label="期望到岗日期" prop="expectedOnboardDate">
              <el-date-picker
                v-model="formData.expectedOnboardDate"
                type="date"
                placeholder="请选择到岗日期"
                :disabled-date="disablePastDate"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="紧急程度" prop="urgency">
              <el-radio-group v-model="formData.urgency">
                <el-radio-button value="NORMAL">普通</el-radio-button>
                <el-radio-button value="URGENT">紧急</el-radio-button>
                <el-radio-button value="CRITICAL">特急</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </div>
        </div>

        <!-- 需求详情 -->
        <div class="form-section">
          <h3 class="section-title">需求详情</h3>
          <el-form-item label="需求原因" prop="reason">
            <el-select v-model="formData.reason" placeholder="请选择需求原因" style="width: 100%">
              <el-option label="新增编制" value="NEW" />
              <el-option label="离职替补" value="REPLACEMENT" />
              <el-option label="业务扩张" value="EXPANSION" />
              <el-option label="项目临时用工" value="TEMPORARY" />
            </el-select>
          </el-form-item>
          <el-form-item label="岗位职责" prop="jobDuty">
            <el-input
              v-model="formData.jobDuty"
              type="textarea"
              :rows="5"
              placeholder="请详细描述岗位职责"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="任职要求" prop="jobRequirement">
            <el-input
              v-model="formData.jobRequirement"
              type="textarea"
              :rows="5"
              placeholder="请详细描述任职要求"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>
        </div>

        <!-- 工作信息 -->
        <div class="form-section">
          <h3 class="section-title">工作信息</h3>
          <div class="form-grid">
            <el-form-item label="工作地点" prop="workLocations">
              <el-select
                v-model="formData.workLocations"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="请选择或输入工作地点"
                style="width: 100%"
              >
                <el-option label="北京" value="北京" />
                <el-option label="上海" value="上海" />
                <el-option label="深圳" value="深圳" />
                <el-option label="杭州" value="杭州" />
                <el-option label="成都" value="成都" />
                <el-option label="广州" value="广州" />
              </el-select>
            </el-form-item>
            <el-form-item label="汇报对象" prop="reporterId">
              <el-select
                v-model="formData.reporterId"
                filterable
                remote
                :remote-method="searchUsers"
                placeholder="请输入搜索汇报对象"
                :loading="userSearchLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="user.name"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <!-- 面试官配置 -->
        <div class="form-section">
          <h3 class="section-title">面试官配置</h3>
          <div class="form-grid">
            <el-form-item label="初面面试官" prop="initialInterviewerIds">
              <el-select
                v-model="formData.initialInterviewerIds"
                multiple
                filterable
                remote
                :remote-method="searchUsers"
                placeholder="请输入搜索初面面试官"
                :loading="userSearchLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="user.name"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="复试面试官" prop="finalInterviewerIds">
              <el-select
                v-model="formData.finalInterviewerIds"
                multiple
                filterable
                remote
                :remote-method="searchUsers"
                placeholder="请输入搜索复试面试官"
                :loading="userSearchLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="user.name"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="form-actions">
          <el-button size="large" @click="goBack">取消</el-button>
          <el-button size="large" @click="handleSaveDraft" :loading="saving">
            保存草稿
          </el-button>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="saving">
            提交审批
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getDemandDetail, createDemand, updateDemand, submitDemand } from '@/api/modules/demand'
import { getUserList } from '@/api/modules/user'
import { getOrgTree } from '@/api/modules/org'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const saving = ref(false)
const userSearchLoading = ref(false)

const isEdit = computed(() => !!route.query.id)
const editId = computed(() => Number(route.query.id) || 0)

// 部门树
const departmentTree = ref<any[]>([])

// 用户选项
const userOptions = ref<any[]>([])

// 表单数据
const formData = reactive({
  title: '',
  orgId: undefined as number | undefined,
  headCount: 1,
  jobLevel: '',
  salaryMin: undefined as number | undefined,
  salaryMax: undefined as number | undefined,
  expectedOnboardDate: '',
  urgency: 'NORMAL',
  reason: '',
  jobDuty: '',
  jobRequirement: '',
  workLocations: '' as string,
  reporterId: undefined as number | undefined,
  initialInterviewerIds: '' as string,
  finalInterviewerIds: '' as string,
})

// 表单验证规则
const formRules: FormRules = {
  title: [
    { required: true, message: '请输入需求标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度为 2 到 100 个字符', trigger: 'blur' },
  ],
  orgId: [
    { required: true, message: '请选择所属部门', trigger: 'change' },
  ],
  headCount: [
    { required: true, message: '请输入招聘人数', trigger: 'change' },
  ],
  jobLevel: [
    { required: true, message: '请选择岗位级别', trigger: 'change' },
  ],
  urgency: [
    { required: true, message: '请选择紧急程度', trigger: 'change' },
  ],
  reason: [
    { required: true, message: '请选择需求原因', trigger: 'change' },
  ],
  jobDuty: [
    { required: true, message: '请输入岗位职责', trigger: 'blur' },
  ],
  jobRequirement: [
    { required: true, message: '请输入任职要求', trigger: 'blur' },
  ],
}

// 薪酬范围自定义校验
function validateSalaryRange(): boolean {
  if (formData.salaryMin !== undefined && formData.salaryMax !== undefined) {
    if (formData.salaryMin >= formData.salaryMax) {
      ElMessage.warning('最低薪酬必须小于最高薪酬')
      return false
    }
  }
  return true
}

// 禁用过去的日期
function disablePastDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

// 加载部门树
async function loadDepartmentTree() {
  try {
    const res: any = await getOrgTree()
    departmentTree.value = res.data || []
  } catch {
    departmentTree.value = []
  }
}

// 搜索用户
async function searchUsers(query: string) {
  if (!query) return
  userSearchLoading.value = true
  try {
    const res: any = await getUserList({ pageNum: 1, pageSize: 20 })
    userOptions.value = (res.data?.list || []).map((u: any) => ({
      id: u.id,
      name: u.realName || u.username,
    }))
  } catch {
    userOptions.value = []
  } finally {
    userSearchLoading.value = false
  }
}

// 加载编辑数据
async function loadDetail() {
  if (!isEdit.value) return
  try {
    const res: any = await getDemandDetail(editId.value)
    const data = res.data || {}
    Object.assign(formData, {
      title: data.title || '',
      orgId: data.orgId,
      headCount: data.headCount || 1,
      jobLevel: data.jobLevel || '',
      salaryMin: data.salaryMin,
      salaryMax: data.salaryMax,
      expectedOnboardDate: data.expectedOnboardDate || '',
      urgency: data.urgency || 'NORMAL',
      reason: data.reason || '',
      jobDuty: data.jobDuty || '',
      jobRequirement: data.jobRequirement || '',
      workLocations: data.workLocations || '',
      reporterId: data.reporterId,
      initialInterviewerIds: data.initialInterviewerIds || '',
      finalInterviewerIds: data.finalInterviewerIds || '',
    })
  } catch {
    // 编辑模式下加载失败
  }
}

// 保存草稿
async function handleSaveDraft() {
  saving.value = true
  try {
    if (isEdit.value) {
      await updateDemand(editId.value, formData)
      ElMessage.success('草稿已保存')
    } else {
      await createDemand(formData)
      ElMessage.success('草稿已保存')
    }
    router.push('/position/demand')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 提交审批
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    ElMessage.warning('请填写所有必填项')
    return
  }

  if (!validateSalaryRange()) return

  saving.value = true
  try {
    if (isEdit.value) {
      await updateDemand(editId.value, formData)
      await submitDemand(editId.value)
    } else {
      const res: any = await createDemand(formData)
      const newId = res.data?.id
      if (newId) {
        await submitDemand(newId)
      }
    }
    ElMessage.success('已提交审批')
    router.push('/position/demand')
  } catch {
    ElMessage.error('提交失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/position/demand')
}

onMounted(() => {
  loadDepartmentTree()
  loadDetail()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-card {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  padding: 32px;
}

.form-section {
  margin-bottom: 32px;

  &:last-of-type {
    margin-bottom: 0;
  }
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-color-light;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;

  .span-2 {
    grid-column: 1 / -1;
  }
}

.salary-range {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;

  .range-separator {
    color: $text-secondary;
    font-weight: 500;
  }

  .range-unit {
    color: $text-regular;
    font-size: 14px;
    white-space: nowrap;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid $border-color-light;
}

// 响应式
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-card {
    padding: 20px;
  }
}
</style>
