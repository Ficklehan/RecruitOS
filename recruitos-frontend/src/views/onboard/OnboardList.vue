<template>
  <PageShell variant="list" title="入职列表" subtitle="管理候选人入职流程与状态">
    <template #actions>
      <RButton @click="handleCreate">
        <Plus class="mr-2 h-4 w-4" />
        新建入职
      </RButton>
    </template>

    <template #filters>
      <RInput
        v-model="queryParams.keyword"
        placeholder="搜索候选人/岗位"
        class="w-full sm:w-60"
        @keyup.enter="handleSearch"
      />
      <RSelect
        v-model="queryParams.status"
        :options="statusOptions"
        placeholder="入职状态"
        clearable
        class="w-full sm:w-40"
      />
      <DateRangePicker v-model="queryParams.date" />
    </template>

    <template #filterActions>
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </RButton>
      <RButton variant="outline" @click="handleReset">
        <RefreshCw class="mr-2 h-4 w-4" />
        重置
      </RButton>
    </template>

    <RTable v-if="onboardList.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[100px]">候选人</RTableTh>
          <RTableTh class="min-w-[160px]">岗位</RTableTh>
          <RTableTh class="w-[130px] text-center">入职日期</RTableTh>
          <RTableTh class="w-[110px]">HR负责人</RTableTh>
          <RTableTh class="w-[100px] text-center">状态</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in onboardList" :key="row.id">
          <RTableCell>
            <span class="font-medium text-primary">{{ row.candidateName }}</span>
          </RTableCell>
          <RTableCell>{{ row.jobTitle }}</RTableCell>
          <RTableCell class="text-center">{{ row.onboardDate }}</RTableCell>
          <RTableCell>{{ row.hrManager }}</RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(getStatusType(row.status))">{{ getStatusLabel(row.status) }}</RBadge>
          </RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="loadData"
    />

    <template #below>
      <RDialog v-model:open="dialogVisible">
        <DialogContent class="max-w-lg">
          <DialogHeader>
            <DialogTitle>{{ dialogTitle }}</DialogTitle>
          </DialogHeader>
          <div class="grid gap-4 py-2">
            <FormField label="候选人" required :error="formErrors.candidateName">
              <RInput v-model="formData.candidateName" placeholder="请输入候选人姓名" />
            </FormField>
            <FormField label="岗位" required :error="formErrors.jobTitle">
              <RInput v-model="formData.jobTitle" placeholder="请输入岗位名称" />
            </FormField>
            <FormField label="入职日期" required :error="formErrors.onboardDate">
              <RInput v-model="formData.onboardDate" type="date" class="w-full" />
            </FormField>
            <FormField label="HR负责人" required :error="formErrors.hrManager">
              <RInput v-model="formData.hrManager" placeholder="请输入HR负责人" />
            </FormField>
            <FormField label="备注">
              <RTextarea v-model="formData.remark" placeholder="请输入备注信息" :rows="3" />
            </FormField>
          </div>
          <DialogFooter>
            <RButton variant="outline" @click="dialogVisible = false">取消</RButton>
            <RButton :disabled="submitLoading" @click="handleSubmit">确定</RButton>
          </DialogFooter>
        </DialogContent>
      </RDialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, RefreshCw } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import FormField from '@/components/app/FormField.vue'
import DateRangePicker from '@/components/app/DateRangePicker.vue'
import {
  RButton,
  RInput,
  RSelect,
  RBadge,
  RTable,
  RTableHead,
  RTableBody,
  RTableRow,
  RTableTh,
  RTableCell,
  RDialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  RTextarea,
} from '@/components/ui'
import { getOnboardList, createOnboard, updateOnboardStatus } from '@/api/modules/onboard'

const router = useRouter()

const queryParams = reactive({
  keyword: '',
  status: '' as string | undefined,
  date: null as [string, string] | null,
  pageNum: 1,
  pageSize: 20,
})

const statusOptions = [
  { label: '待入职', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已完成', value: 'COMPLETED' },
]

const total = ref(0)
const onboardList = ref<any[]>([])
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)

const dialogTitle = computed(() => (dialogType.value === 'create' ? '新建入职' : '编辑入职'))

const formData = reactive({
  candidateName: '',
  jobTitle: '',
  onboardDate: '',
  hrManager: '',
  remark: '',
})

const formErrors = reactive({
  candidateName: '',
  jobTitle: '',
  onboardDate: '',
  hrManager: '',
})

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

function getRowActions(_row: any) {
  return [
    { command: 'task', label: '任务', icon: 'List', primary: true },
    { command: 'complete', label: '完成入职', icon: 'CircleCheck' },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'task') handleTask(row)
  else if (cmd === 'complete') handleComplete(row)
}

function validateForm(): boolean {
  formErrors.candidateName = formData.candidateName ? '' : '请输入候选人姓名'
  formErrors.jobTitle = formData.jobTitle ? '' : '请输入岗位名称'
  formErrors.onboardDate = formData.onboardDate ? '' : '请选择入职日期'
  formErrors.hrManager = formData.hrManager ? '' : '请输入HR负责人'
  return !Object.values(formErrors).some(Boolean)
}

async function loadData() {
  const params: any = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  if (queryParams.keyword) params.keyword = queryParams.keyword
  if (queryParams.status) params.onboardStatus = queryParams.status
  if (queryParams.date?.length === 2) {
    params.startDate = queryParams.date[0]
    params.endDate = queryParams.date[1]
  }
  const res = await getOnboardList(params)
  const rows = res.data?.list || res.data?.records || res.data || []
  onboardList.value = rows.map((row: any) => ({
    ...row,
    status: row.onboardStatus || row.status,
    hrManager: row.hrName || row.hrManager,
    onboardDate: row.onboardDate || '',
  }))
  total.value = Number(res.data?.total) || onboardList.value.length
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.date = null
  handleSearch()
}

function resetForm() {
  formData.candidateName = ''
  formData.jobTitle = ''
  formData.onboardDate = ''
  formData.hrManager = ''
  formData.remark = ''
  Object.keys(formErrors).forEach((k) => ((formErrors as any)[k] = ''))
}

function handleCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function handleTask(row: any) {
  router.push(`/onboard/task?id=${row.id}`)
}

async function handleComplete(row: any) {
  await updateOnboardStatus(row.id, 'COMPLETED')
  toast.success('入职已完成，编制已回写')
  loadData()
}

async function handleSubmit() {
  if (!validateForm()) return
  submitLoading.value = true
  try {
    await createOnboard({
      candidateName: formData.candidateName,
      jobTitle: formData.jobTitle,
      onboardDate: formData.onboardDate,
      hrName: formData.hrManager,
      remark: formData.remark,
    })
    toast.success('创建成功')
    dialogVisible.value = false
    loadData()
  } catch {
    /* interceptor */
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => loadData())
</script>
