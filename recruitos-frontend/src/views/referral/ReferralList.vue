<template>
  <PageShell variant="list" title="内推列表" subtitle="管理员工内推记录与推荐进展">
    <template #actions>
      <Button variant="outline" @click="handleCreateLink">
        <Link class="mr-2 h-4 w-4" />
        生成分享链接
      </Button>
      <Button @click="handleCreate">
        <Plus class="mr-2 h-4 w-4" />
        发起内推
      </Button>
    </template>

    <template #filters>
      <Input
        v-model="queryParams.keyword"
        placeholder="搜索推荐人/候选人"
        class="w-full sm:w-56"
        @keyup.enter="handleSearch"
      />
      <Select
        v-model="queryParams.status"
        :options="statusOptions"
        placeholder="状态"
        clearable
        class="w-full sm:w-36"
      />
    </template>

    <template #filterActions>
      <Button @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </Button>
      <Button variant="outline" @click="handleReset">
        <RefreshCw class="mr-2 h-4 w-4" />
        重置
      </Button>
    </template>

    <Table v-if="referralList.length">
      <TableHeader>
        <TableRow>
          <TableHead class="w-[120px]">推荐人</TableHead>
          <TableHead class="w-[120px]">候选人</TableHead>
          <TableHead class="min-w-[180px]">推荐岗位</TableHead>
          <TableHead class="w-[110px] text-center">状态</TableHead>
          <TableHead class="w-[170px]">推荐时间</TableHead>
          <TableHead class="w-[100px] text-center">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-for="row in referralList" :key="row.id">
          <TableCell>{{ row.referrerName }}</TableCell>
          <TableCell>
            <button type="button" class="font-medium text-primary hover:underline" @click="handleView(row)">
              {{ row.candidateName }}
            </button>
          </TableCell>
          <TableCell>{{ row.jobTitle }}</TableCell>
          <TableCell class="text-center">
            <Badge :variant="elTagTypeToBadge(getStatusType(row.status))">{{ getStatusLabel(row.status) }}</Badge>
          </TableCell>
          <TableCell class="text-muted-foreground">{{ row.createdAt }}</TableCell>
          <TableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="loadData"
    />

    <template #below>
      <Dialog v-model="dialogVisible">
        <DialogContent class="max-w-lg">
          <DialogHeader>
            <DialogTitle>{{ isEdit ? '编辑内推' : '发起内推' }}</DialogTitle>
          </DialogHeader>
          <div class="grid gap-4 py-2">
            <FormField label="推荐人" required :error="formErrors.referrerName">
              <Input v-model="formData.referrerName" placeholder="请输入推荐人姓名" />
            </FormField>
            <FormField label="候选人" required :error="formErrors.candidateName">
              <Input v-model="formData.candidateName" placeholder="请输入候选人姓名" />
            </FormField>
            <FormField label="推荐岗位" required :error="formErrors.jobId">
              <Select
                v-model="formData.jobId"
                :options="jobSelectOptions"
                placeholder="请选择推荐岗位"
                class="w-full"
              />
            </FormField>
            <FormField label="备注">
              <Textarea v-model="formData.remark" placeholder="请输入推荐备注" :rows="3" />
            </FormField>
          </div>
          <DialogFooter>
            <Button variant="outline" @click="dialogVisible = false">取消</Button>
            <Button :disabled="submitLoading" @click="handleSubmit">确定</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Search, RefreshCw } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import FormField from '@/components/app/FormField.vue'
import {
  Button,
  Input,
  Select,
  Badge,
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Textarea,
} from '@/components/ui'
import { Link } from 'lucide-vue-next'
import { getJobList } from '@/api/modules/job'
import { getReferralList, createReferral, createReferralShareLink } from '@/api/modules/referral'

const queryParams = reactive({
  keyword: '',
  status: '' as string | undefined,
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const referralList = ref<any[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const editId = ref<number | null>(null)

const formData = reactive({
  referrerName: '',
  candidateName: '',
  jobId: null as number | null,
  remark: '',
})

const formErrors = reactive({
  referrerName: '',
  candidateName: '',
  jobId: '',
})

const jobOptions = ref<any[]>([])
const jobSelectOptions = computed(() => jobOptions.value.map((j) => ({ label: j.title, value: j.id })))

async function loadJobOptions() {
  try {
    const res: any = await getJobList({ pageNum: 1, pageSize: 50, status: 'ACTIVE' })
    const jobs = res?.data?.records ?? res?.records ?? []
    jobOptions.value = jobs.map((j: any) => ({ id: j.id, title: j.title }))
  } catch {
    jobOptions.value = []
  }
}

async function handleCreateLink() {
  await loadJobOptions()
  if (!jobOptions.value.length) {
    toast.error('暂无在招岗位')
    return
  }
  const jobId = jobOptions.value[0].id
  try {
    const res: any = await createReferralShareLink({ jobId })
    const data = res.data ?? res
    const path = data.url || `/referral/submit/${data.token}`
    const fullUrl = `${window.location.origin}${path}`
    await navigator.clipboard.writeText(fullUrl)
    toast.success(`已复制分享链接（${jobOptions.value[0].title}）`)
  } catch (e: any) {
    toast.error(e?.message || '生成链接失败')
  }
}

const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '面试中', value: 'INTERVIEWING' },
  { label: 'Offer', value: 'OFFER' },
  { label: '已入职', value: 'ONBOARD' },
  { label: '已拒绝', value: 'REJECTED' },
]

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

function getRowActions(_row: any) {
  return [
    { command: 'view', label: '查看详情', icon: 'View', primary: true },
    { command: 'edit', label: '编辑', icon: 'Edit' },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') handleView(row)
  else if (cmd === 'edit') handleEdit(row)
}

function validateForm(): boolean {
  formErrors.referrerName = formData.referrerName ? '' : '请输入推荐人姓名'
  formErrors.candidateName = formData.candidateName ? '' : '请输入候选人姓名'
  formErrors.jobId = formData.jobId ? '' : '请选择推荐岗位'
  return !Object.values(formErrors).some(Boolean)
}

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
  queryParams.status = undefined
  handleSearch()
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  formData.referrerName = ''
  formData.candidateName = ''
  formData.jobId = null
  formData.remark = ''
  Object.assign(formErrors, { referrerName: '', candidateName: '', jobId: '' })
  dialogVisible.value = true
}

function handleView(row: any) {
  toast.info(`查看内推详情: ${row.candidateName}`)
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
  if (!validateForm()) return
  submitLoading.value = true
  try {
    const payload = {
      referrerName: formData.referrerName,
      candidateName: formData.candidateName,
      jobId: formData.jobId,
      remark: formData.remark,
    }
    await createReferral(payload)
    toast.success('内推发起成功')
    dialogVisible.value = false
    loadData()
  } catch {
    /* interceptor */
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadJobOptions()
})
</script>
