<template>
  <PageShell variant="list"
    title="招聘需求"
    subtitle="部门提出的用人申请，审批通过后可创建在招职位"
  >
    <template #actions>
      <RButton @click="handleCreate">
        <Plus class="mr-2 h-4 w-4" />
        创建招聘需求
      </RButton>
    </template>

    <template #filters>
      <RInput
        v-model="queryParams.title"
        placeholder="搜索招聘需求"
        class="w-full sm:w-64"
        @keyup.enter="handleSearch"
      />
      <RSelect
        v-model="queryParams.orgId"
        :options="departmentOptions"
        placeholder="所属部门"
        clearable
        class="w-full sm:w-44"
      />
      <RSelect
        v-model="queryParams.status"
        :options="statusOptions"
        placeholder="需求状态"
        clearable
        class="w-full sm:w-40"
      />
      <RSelect
        v-model="queryParams.urgency"
        :options="urgencyOptions"
        placeholder="紧急程度"
        clearable
        class="w-full sm:w-40"
      />
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

    <RTable v-if="demandList.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[140px]">需求编号</RTableTh>
          <RTableTh class="min-w-[200px]">需求标题</RTableTh>
          <RTableTh class="w-[120px]">部门</RTableTh>
          <RTableTh class="w-[100px] text-center">招聘人数</RTableTh>
          <RTableTh class="w-[80px] text-center">级别</RTableTh>
          <RTableTh class="w-[160px] text-center">薪酬范围</RTableTh>
          <RTableTh class="w-[100px] text-center">紧急程度</RTableTh>
          <RTableTh class="w-[100px] text-center">状态</RTableTh>
          <RTableTh class="w-[100px]">创建人</RTableTh>
          <RTableTh class="w-[170px]">创建时间</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in demandList" :key="row.id">
          <RTableCell class="font-mono text-xs text-muted-foreground">{{ row.demandNo }}</RTableCell>
          <RTableCell>
            <button type="button" class="font-medium text-primary hover:underline" @click="handleView(row)">
              {{ row.title }}
            </button>
          </RTableCell>
          <RTableCell>{{ row.department }}</RTableCell>
          <RTableCell class="text-center">{{ row.headCount }}</RTableCell>
          <RTableCell class="text-center">{{ row.jobLevel }}</RTableCell>
          <RTableCell class="text-center text-sm">
            {{ formatSalary(row.salaryMin) }}K - {{ formatSalary(row.salaryMax) }}K
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="urgencyBadge(row.urgency)">{{ getUrgencyLabel(row.urgency) }}</RBadge>
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="demandStatusBadge(row.status)">{{ getStatusLabel(row.status) }}</RBadge>
          </RTableCell>
          <RTableCell>{{ row.createdBy }}</RTableCell>
          <RTableCell class="text-muted-foreground">{{ row.createdAt }}</RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row) as any" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta
      v-else
      title="暂无招聘需求"
      description="创建招聘需求并提交审批后，可据此创建在招职位并开始招聘"
      :actions="[
        { label: '创建招聘需求', type: 'primary', onClick: handleCreate },
        { label: '查看在招职位', type: 'default', onClick: () => router.push('/planning/jobs') },
      ]"
    />

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="loadData"
    />

    <ConfirmDialog
      v-model:open="confirmState.open"
      :title="confirmState.title"
      :message="confirmState.message"
      :destructive="confirmState.destructive"
      :confirm-text="confirmState.confirmText"
      @confirm="confirmState.onConfirm?.()"
    />
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, RefreshCw } from 'lucide-vue-next'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
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
} from '@/components/ui'
import { demandStatusLabel } from '@/constants/businessLabels'
import { demandStatusBadge, urgencyBadge } from '@/lib/badgeVariants'
import { toast } from '@/lib/notify'
import { getDemandList, submitDemand, closeDemand } from '@/api/modules/demand'

const router = useRouter()

const queryParams = reactive({
  title: '',
  orgId: undefined as string | undefined,
  status: undefined as string | undefined,
  urgency: undefined as string | undefined,
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const demandList = ref<any[]>([])

const confirmState = reactive({
  open: false,
  title: '',
  message: '',
  confirmText: '确定',
  destructive: false,
  onConfirm: null as null | (() => void),
})

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

function formatSalary(val: unknown): string {
  if (val == null) return '-'
  const num = Number(val)
  return Number.isNaN(num) ? String(val) : num.toFixed(0)
}

function getStatusLabel(status: string) {
  return demandStatusLabel(status)
}

function getUrgencyLabel(level: string) {
  const map: Record<string, string> = {
    NORMAL: '普通',
    URGENT: '紧急',
    CRITICAL: '特急',
  }
  return map[level] || level
}

function openConfirm(
  title: string,
  message: string,
  onConfirm: () => void,
  opts?: { destructive?: boolean; confirmText?: string }
) {
  confirmState.title = title
  confirmState.message = message
  confirmState.destructive = opts?.destructive ?? false
  confirmState.confirmText = opts?.confirmText ?? '确定'
  confirmState.onConfirm = () => {
    confirmState.open = false
    onConfirm()
  }
  confirmState.open = true
}

function getRowActions(_row: unknown) {
  return [
    { command: 'view', label: '查看', icon: 'View', type: 'primary', primary: true },
    { command: 'edit', label: '编辑', icon: 'Edit' },
    { command: 'submit', label: '提交审批', icon: 'Promotion' },
    { command: 'close', label: '关闭', icon: 'CircleClose', divided: true },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') handleView(row)
  else if (cmd === 'edit') handleEdit(row)
  else if (cmd === 'submit') handleSubmit(row)
  else if (cmd === 'close') handleClose(row)
}

async function loadData() {
  try {
    const res: any = await getDemandList(queryParams)
    demandList.value = res.data?.list || res.data?.records || []
    total.value = Number(res.data?.total) || 0
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
  queryParams.orgId = undefined
  queryParams.status = undefined
  queryParams.urgency = undefined
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

function handleSubmit(row: any) {
  openConfirm('提交确认', '确定要提交该需求进行审批吗？', async () => {
    await submitDemand(row.id)
    toast.success('已提交审批')
    loadData()
  })
}

function handleClose(row: any) {
  openConfirm('关闭确认', '确定要关闭该需求吗？关闭后不可恢复。', async () => {
    await closeDemand(row.id)
    toast.success('需求已关闭')
    loadData()
  }, { destructive: true, confirmText: '确定关闭' })
}

onMounted(() => loadData())
</script>
