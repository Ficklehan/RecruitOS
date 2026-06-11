<template>
  <PageShell variant="list"
    title="录用通知"
    subtitle="管理候选人录用通知（Offer）的发送与审批状态"
  >
    <template #actions>
      <RButton @click="handleCreate">
        <Plus class="mr-2 h-4 w-4" />
        创建录用通知
      </RButton>
    </template>

    <template #filters>
      <RInput
        v-model="queryParams.keyword"
        placeholder="搜索候选人 / 在招职位"
        class="w-full sm:w-64"
        @keyup.enter="handleSearch"
      />
      <RSelect
        v-model="queryParams.status"
        :options="statusFilterOptions"
        placeholder="通知状态"
        clearable
        class="w-full sm:w-40"
      />
      <DateRangePicker v-model="queryParams.dateRange" />
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

    <RTable v-if="offerList.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[100px]">候选人</RTableTh>
          <RTableTh class="min-w-[160px]">在招职位</RTableTh>
          <RTableTh class="w-[120px]">部门</RTableTh>
          <RTableTh class="w-[120px] text-center">薪资</RTableTh>
          <RTableTh class="w-[100px] text-center">状态</RTableTh>
          <RTableTh class="w-[180px]">创建时间</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in offerList" :key="row.id">
          <RTableCell>
            <button type="button" class="font-medium text-primary hover:underline" @click="handleView(row)">
              {{ row.candidateName }}
            </button>
          </RTableCell>
          <RTableCell>{{ row.jobTitle }}</RTableCell>
          <RTableCell>{{ row.department }}</RTableCell>
          <RTableCell class="text-center">{{ row.salary }}</RTableCell>
          <RTableCell class="text-center">
            <div class="flex items-center justify-center gap-1.5">
              <RBadge :variant="offerStatusBadge(row.status)">{{ getStatusLabel(row.status) }}</RBadge>
              <button v-if="row.candidateId" type="button" class="text-primary hover:text-primary/70" title="AI Offer 策略" @click="router.push(`/ai/offer-strategy/${row.candidateId}`)">
                <Sparkles class="h-3.5 w-3.5" />
              </button>
            </div>
          </RTableCell>
          <RTableCell class="text-muted-foreground">{{ row.createdAt }}</RTableCell>
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
            <FormField label="在招职位" required :error="formErrors.jobTitle">
              <RInput v-model="formData.jobTitle" placeholder="请输入职位名称" />
            </FormField>
            <FormField label="部门" required :error="formErrors.department">
              <RInput v-model="formData.department" placeholder="请输入所属部门" />
            </FormField>
            <FormField label="薪资" required :error="formErrors.salary">
              <RInput v-model="formData.salary" placeholder="例如: 30K/月" />
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
import { Plus, Search, RefreshCw, Sparkles } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { offerStatusBadge } from '@/lib/badgeVariants'
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
import { offerStatusLabel } from '@/constants/businessLabels'
import { getOfferList, createOffer } from '@/api/modules/offer'

const router = useRouter()

const queryParams = reactive({
  keyword: '',
  status: '' as string | undefined,
  dateRange: null as [string, string] | null,
  pageNum: 1,
  pageSize: 20,
})

const statusFilterOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已发送', value: 'SENT' },
  { label: '已接受', value: 'ACCEPTED' },
  { label: '已拒绝', value: 'REJECTED' },
]

const total = ref(0)
const offerList = ref<any[]>([])
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const currentEditId = ref<number | null>(null)

const dialogTitle = computed(() => (dialogType.value === 'create' ? '创建录用通知' : '编辑录用通知'))

const formData = reactive({
  candidateName: '',
  jobTitle: '',
  department: '',
  salary: '',
  remark: '',
})

const formErrors = reactive({
  candidateName: '',
  jobTitle: '',
  department: '',
  salary: '',
})

function getStatusLabel(status: string): string {
  return offerStatusLabel(status)
}

function getRowActions(row: any) {
  const actions = [
    { command: 'view', label: '查看', icon: 'View', primary: true },
    { command: 'edit', label: '编辑', icon: 'Edit' },
  ]
  if (row.candidateId) {
    actions.push({ command: 'ai-strategy', label: 'AI 策略', icon: 'Sparkles' })
  }
  return actions
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') handleView(row)
  else if (cmd === 'edit') handleEdit(row)
  else if (cmd === 'ai-strategy') router.push(`/ai/offer-strategy/${row.candidateId}`)
}

function validateForm(): boolean {
  formErrors.candidateName = formData.candidateName ? '' : '请输入候选人姓名'
  formErrors.jobTitle = formData.jobTitle ? '' : '请输入职位名称'
  formErrors.department = formData.department ? '' : '请输入所属部门'
  formErrors.salary = formData.salary ? '' : '请输入薪资'
  return !Object.values(formErrors).some(Boolean)
}

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
  queryParams.status = undefined
  queryParams.dateRange = null
  handleSearch()
}

function resetForm() {
  formData.candidateName = ''
  formData.jobTitle = ''
  formData.department = ''
  formData.salary = ''
  formData.remark = ''
  Object.keys(formErrors).forEach((k) => ((formErrors as any)[k] = ''))
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
  if (!validateForm()) return
  submitLoading.value = true
  try {
    if (dialogType.value === 'create') {
      await createOffer({ ...formData })
      toast.success('创建成功')
    }
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
