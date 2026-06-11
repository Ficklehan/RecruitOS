<template>
  <PageShell
    :title="isEdit ? '编辑招聘需求' : '创建招聘需求'"
    subtitle="审批通过后可据此创建在招职位并开始招聘"
  >
    <div class="form-card">
      <div class="demand-form">
        <div class="form-section">
          <h3 class="section-title">基本信息</h3>
          <div class="form-grid">
            <FormField label="招聘需求标题" required :error="formErrors.title" class="span-2">
              <RInput v-model="formData.title" placeholder="如：技术部-高级前端工程师" maxlength="100" />
            </FormField>
            <FormField label="所属部门" required :error="formErrors.orgId">
              <TreeSelect
                v-model="formData.orgId"
                :nodes="departmentTreeNodes"
                placeholder="请选择所属部门"
                class="w-full"
              />
            </FormField>
            <FormField label="招聘人数" required :error="formErrors.headCount">
              <NumberInput v-model="formData.headCount" :min="1" :max="99" class="w-full" />
            </FormField>
            <FormField label="职级" required :error="formErrors.jobLevel">
              <RSelect v-model="formData.jobLevel" :options="jobLevelOptions" placeholder="请选择职级" class="w-full" />
            </FormField>
          </div>
        </div>

        <div class="form-section">
          <h3 class="section-title">薪酬与时间</h3>
          <div class="form-grid">
            <FormField label="期望薪酬范围" class="span-2" :error="formErrors.salaryRange">
              <div class="salary-range">
                <NumberInput v-model="formData.salaryMin" :min="0" :max="formData.salaryMax || 999" placeholder="最低" class="flex-1" />
                <span class="range-separator">-</span>
                <NumberInput v-model="formData.salaryMax" :min="formData.salaryMin || 0" placeholder="最高" class="flex-1" />
                <span class="range-unit">K / 月</span>
              </div>
            </FormField>
            <FormField label="期望到岗日期">
              <RInput v-model="formData.expectedOnboardDate" type="date" :min="todayStr" class="w-full" />
            </FormField>
            <FormField label="紧急程度" required :error="formErrors.urgency">
              <SegmentedControl
                v-model="formData.urgency"
                :options="urgencyOptions"
              />
            </FormField>
          </div>
        </div>

        <div class="form-section">
          <h3 class="section-title">需求详情</h3>
          <FormField label="需求原因" required :error="formErrors.reason">
            <RSelect v-model="formData.reason" :options="reasonOptions" placeholder="请选择需求原因" class="w-full" />
          </FormField>
          <FormField label="职位描述" required :error="formErrors.jobDuty" class="mt-4">
            <RTextarea v-model="formData.jobDuty" :rows="5" placeholder="请描述该职位的主要工作内容" maxlength="2000" />
          </FormField>
          <FormField label="任职要求" required :error="formErrors.jobRequirement" class="mt-4">
            <RTextarea v-model="formData.jobRequirement" :rows="5" placeholder="请详细描述任职要求" maxlength="2000" />
          </FormField>
        </div>

        <div class="form-section">
          <h3 class="section-title">工作信息</h3>
          <div class="form-grid">
            <FormField label="工作地点">
              <div class="flex flex-wrap gap-3">
                <label
                  v-for="loc in locationOptions"
                  :key="loc"
                  class="flex items-center gap-2 text-sm"
                >
                  <RCheckbox
                    :model-value="selectedLocations.includes(loc)"
                    @update:model-value="toggleLocation(loc, $event)"
                  />
                  {{ loc }}
                </label>
              </div>
            </FormField>
            <FormField label="汇报对象">
              <RSelect
                v-model="formData.reporterId"
                :options="userSelectOptions"
                placeholder="请选择汇报对象"
                class="w-full"
                @update:model-value="onReporterChange"
              />
            </FormField>
          </div>
        </div>

        <div class="form-section">
          <h3 class="section-title">面试官配置</h3>
          <div class="form-grid">
            <FormField label="初面面试官">
              <RInput v-model="userSearchQuery" placeholder="搜索用户" class="mb-2" @input="searchUsers(userSearchQuery)" />
              <div v-if="userSearchLoading" class="text-sm text-muted-foreground">搜索中…</div>
              <div v-else class="flex flex-col gap-2 max-h-40 overflow-auto">
                <label
                  v-for="user in userOptions"
                  :key="'init-' + user.id"
                  class="flex items-center gap-2 text-sm"
                >
                  <RCheckbox
                    :model-value="initialInterviewerIds.includes(user.id)"
                    @update:model-value="toggleInterviewer('initial', user.id, $event)"
                  />
                  {{ user.name }}
                </label>
              </div>
            </FormField>
            <FormField label="复试面试官">
              <div class="flex flex-col gap-2 max-h-48 overflow-auto">
                <label
                  v-for="user in userOptions"
                  :key="'final-' + user.id"
                  class="flex items-center gap-2 text-sm"
                >
                  <RCheckbox
                    :model-value="finalInterviewerIds.includes(user.id)"
                    @update:model-value="toggleInterviewer('final', user.id, $event)"
                  />
                  {{ user.name }}
                </label>
                <p v-if="!userOptions.length" class="text-sm text-muted-foreground">请在上方搜索用户</p>
              </div>
            </FormField>
          </div>
        </div>

        <div class="form-actions">
          <RButton size="lg" variant="outline" @click="goBack">取消</RButton>
          <RButton size="lg" variant="outline" :disabled="saving" @click="handleSaveDraft">
            保存草稿
          </RButton>
          <RButton size="lg" :disabled="saving" @click="handleSubmit">
            提交审批
          </RButton>
        </div>
      </div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import FormField from '@/components/app/FormField.vue'
import TreeSelect, { type TreeNode } from '@/components/app/TreeSelect.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import SegmentedControl from '@/components/app/SegmentedControl.vue'
import { RButton, RInput, RSelect, RTextarea, RCheckbox } from '@/components/ui'
import { getDemandDetail, createDemand, updateDemand, submitDemand } from '@/api/modules/demand'
import { getUserList } from '@/api/modules/user'
import { getOrgTree } from '@/api/modules/org'

const router = useRouter()
const route = useRoute()
const saving = ref(false)
const userSearchLoading = ref(false)
const userSearchQuery = ref('')

const isEdit = computed(() => !!route.query.id)
const editId = computed(() => Number(route.query.id) || 0)
const todayStr = computed(() => new Date().toISOString().slice(0, 10))

const departmentTree = ref<any[]>([])
const departmentTreeNodes = computed<TreeNode[]>(() => toTreeNodes(departmentTree.value))
const userOptions = ref<{ id: number; name: string }[]>([])

const locationOptions = ['北京', '上海', '深圳', '杭州', '成都', '广州']
const selectedLocations = ref<string[]>([])
const initialInterviewerIds = ref<number[]>([])
const finalInterviewerIds = ref<number[]>([])

const jobLevelOptions = [
  { label: 'P6 - 高级工程师', value: 'P6' },
  { label: 'P7 - 资深工程师', value: 'P7' },
  { label: 'P8 - 技术专家', value: 'P8' },
  { label: 'P9 - 高级技术专家', value: 'P9' },
]

const reasonOptions = [
  { label: '新增编制', value: 'NEW' },
  { label: '离职替补', value: 'REPLACEMENT' },
  { label: '业务扩张', value: 'EXPANSION' },
  { label: '项目临时用工', value: 'TEMPORARY' },
]

const urgencyOptions = [
  { label: '普通', value: 'NORMAL' },
  { label: '紧急', value: 'URGENT' },
  { label: '特急', value: 'CRITICAL' },
]

const userSelectOptions = computed(() =>
  userOptions.value.map(u => ({ label: u.name, value: u.id }))
)

const formData = reactive({
  title: '',
  orgId: undefined as number | undefined,
  headCount: 1 as number | null,
  jobLevel: '',
  salaryMin: undefined as number | null | undefined,
  salaryMax: undefined as number | null | undefined,
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

const formErrors = reactive({
  title: '',
  orgId: '',
  headCount: '',
  jobLevel: '',
  urgency: '',
  reason: '',
  jobDuty: '',
  jobRequirement: '',
  salaryRange: '',
})

function toTreeNodes(nodes: any[]): TreeNode[] {
  return (nodes || []).map(n => ({
    id: n.id,
    label: n.name,
    children: n.children?.length ? toTreeNodes(n.children) : undefined,
  }))
}

function parseJsonArray(val: string | null): string[] {
  if (!val) return []
  try {
    const p = JSON.parse(val)
    return Array.isArray(p) ? p.map(String) : []
  } catch {
    return val ? [val] : []
  }
}

function parseIdArray(val: string | null): number[] {
  if (!val) return []
  try {
    const p = JSON.parse(val)
    return Array.isArray(p) ? p.map(Number).filter(n => !isNaN(n)) : []
  } catch {
    return val.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
  }
}

function syncArraysToForm() {
  formData.workLocations = selectedLocations.value.length
    ? JSON.stringify(selectedLocations.value)
    : ''
  formData.initialInterviewerIds = initialInterviewerIds.value.length
    ? JSON.stringify(initialInterviewerIds.value)
    : ''
  formData.finalInterviewerIds = finalInterviewerIds.value.length
    ? JSON.stringify(finalInterviewerIds.value)
    : ''
}

function toggleLocation(loc: string, checked: boolean | 'indeterminate') {
  if (checked === true) {
    if (!selectedLocations.value.includes(loc)) selectedLocations.value.push(loc)
  } else {
    selectedLocations.value = selectedLocations.value.filter(l => l !== loc)
  }
}

function toggleInterviewer(which: 'initial' | 'final', id: number, checked: boolean | 'indeterminate') {
  const list = which === 'initial' ? initialInterviewerIds : finalInterviewerIds
  if (checked === true) {
    if (!list.value.includes(id)) list.value.push(id)
  } else {
    list.value = list.value.filter(i => i !== id)
  }
}

function onReporterChange() {
  /* Select handles v-model */
}

function validateSalaryRange(): boolean {
  if (formData.salaryMin != null && formData.salaryMax != null) {
    if (formData.salaryMin >= formData.salaryMax) {
      formErrors.salaryRange = '最低薪酬必须小于最高薪酬'
      return false
    }
  }
  formErrors.salaryRange = ''
  return true
}

function validateForm(): boolean {
  formErrors.title = formData.title.trim().length >= 2 ? '' : '请输入需求标题（2-100字）'
  formErrors.orgId = formData.orgId != null ? '' : '请选择所属部门'
  formErrors.headCount = formData.headCount != null && formData.headCount >= 1 ? '' : '请输入招聘人数'
  formErrors.jobLevel = formData.jobLevel ? '' : '请选择职级'
  formErrors.urgency = formData.urgency ? '' : '请选择紧急程度'
  formErrors.reason = formData.reason ? '' : '请选择需求原因'
  formErrors.jobDuty = formData.jobDuty.trim() ? '' : '请输入职位描述'
  formErrors.jobRequirement = formData.jobRequirement.trim() ? '' : '请输入任职要求'
  const salaryOk = validateSalaryRange()
  return salaryOk && !Object.values(formErrors).some(e => e)
}

async function loadDepartmentTree() {
  try {
    const res: any = await getOrgTree()
    departmentTree.value = res.data || []
  } catch {
    departmentTree.value = []
  }
}

async function searchUsers(query: string) {
  userSearchLoading.value = true
  try {
    const res: any = await getUserList({ pageNum: 1, pageSize: 20, keyword: query || undefined })
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
    selectedLocations.value = parseJsonArray(data.workLocations)
    initialInterviewerIds.value = parseIdArray(data.initialInterviewerIds)
    finalInterviewerIds.value = parseIdArray(data.finalInterviewerIds)
    await searchUsers('')
  } catch {
    /* ignore */
  }
}

async function handleSaveDraft() {
  syncArraysToForm()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateDemand(editId.value, formData)
      toast.success('草稿已保存')
    } else {
      await createDemand(formData)
      toast.success('草稿已保存')
    }
    router.push('/planning/demands')
  } catch {
    toast.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!validateForm()) {
    toast.error('请填写所有必填项')
    return
  }
  syncArraysToForm()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateDemand(editId.value, formData)
      await submitDemand(editId.value)
    } else {
      const res: any = await createDemand(formData)
      const newId = res.data?.id
      if (newId) await submitDemand(newId)
    }
    toast.success('已提交审批')
    router.push('/planning/demands')
  } catch {
    toast.error('提交失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/planning/demands')
}

onMounted(() => {
  loadDepartmentTree()
  searchUsers('')
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

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-card {
    padding: 20px;
  }
}
</style>
