<template>
  <PageShell variant="list"
    title="在招职位"
    subtitle="管理正在招聘的职位，进入工作台查看进展与渠道招聘"
  >
    <template #actions>
      <RButton @click="handleCreate">
        <Plus class="mr-2 h-4 w-4" />
        创建在招职位
      </RButton>
    </template>

    <template #filters>
      <RInput
        v-model="queryParams.title"
        placeholder="搜索职位名称"
        class="w-full sm:w-64"
        @keyup.enter="handleSearch"
      />
      <RSelect
        v-model="queryParams.status"
        :options="statusOptions"
        placeholder="招聘状态"
        clearable
        class="w-full sm:w-40"
      />
      <RInput
        v-model="queryParams.demandNo"
        placeholder="关联需求编号"
        class="w-full sm:w-48"
        @keyup.enter="handleSearch"
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

    <RTable v-if="jobList.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[150px]">职位编号</RTableTh>
          <RTableTh class="min-w-[200px]">职位名称</RTableTh>
          <RTableTh class="w-[150px]">关联需求</RTableTh>
          <RTableTh class="w-[100px] text-center">招聘人数</RTableTh>
          <RTableTh class="w-[90px] text-center">已到岗</RTableTh>
          <RTableTh class="w-[100px] text-center">状态</RTableTh>
          <RTableTh class="w-[110px] text-center">招人状态</RTableTh>
          <RTableTh class="w-[170px]">创建时间</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in jobList" :key="row.id">
          <RTableCell class="font-mono text-xs text-muted-foreground">{{ row.jobNo }}</RTableCell>
          <RTableCell>
            <button
              type="button"
              class="font-medium text-primary hover:underline text-left"
              @click="handleView(row)"
            >
              {{ row.title }}
            </button>
          </RTableCell>
          <RTableCell>{{ row.demandNo || '—' }}</RTableCell>
          <RTableCell class="text-center">{{ row.headcount }}</RTableCell>
          <RTableCell class="text-center">
            <span :class="row.onboardCount >= row.headcount ? 'text-green-600 font-semibold' : ''">
              {{ row.onboardCount || 0 }}
            </span>
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="jobStatusBadge(row.status)">
              {{ jobStatusLabel(row.status) }}
            </RBadge>
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge v-if="recruitStatusMap.get(row.id)" variant="outline">
              {{ recruitStatusMap.get(row.id)!.label }}
            </RBadge>
            <span v-else class="text-muted-foreground">—</span>
          </RTableCell>
          <RTableCell class="text-center">
            <span v-if="aiHealthMap.get(row.id) === 'critical'" class="text-red-500 text-xs font-semibold">🔥 枯竭</span>
            <span v-else-if="aiHealthMap.get(row.id) === 'warning'" class="text-amber-500 text-xs">⚠️</span>
            <span v-else-if="aiHealthMap.get(row.id) === 'healthy'" class="text-green-500 text-xs">✅</span>
            <span v-else class="text-text-placeholder text-xs">—</span>
          </RTableCell>
          <RTableCell class="text-muted-foreground">{{ row.createdAt }}</RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row) as any" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta
      v-else
      title="暂无在招职位"
      description="创建职位并完善任职要求后，即可开始渠道招聘与候选人匹配"
      :actions="[
        { label: '创建在招职位', type: 'primary', onClick: handleCreate },
        { label: '查看招聘需求', type: 'default', onClick: () => router.push('/planning/demands') },
      ]"
    />

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="loadData"
    />

    <template #below>
      <RDialog :model-value="closeDialogVisible" title="关闭职位" @update:model-value="closeDialogVisible = $event">
        <div class="space-y-2">
          <RLabel for="close-reason">关闭原因</RLabel>
          <RTextarea id="close-reason" v-model="closeReason" placeholder="请输入关闭原因" :rows="3" />
        </div>
        <template #footer>
          <RButton variant="outline" @click="closeDialogVisible = false">取消</RButton>
          <RButton @click="confirmClose">确定关闭</RButton>
        </template>
      </RDialog>

      <ConfirmDialog
        v-model:open="confirmState.open"
        :title="confirmState.title"
        :message="confirmState.message"
        :destructive="confirmState.destructive"
        @confirm="confirmState.onConfirm?.()"
      />
    </template>
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
  RButton, RInput, RSelect, RBadge, RLabel, RTextarea,
  RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell,
  RDialog,
} from '@/components/ui'
import { jobStatusLabel } from '@/constants/businessLabels'
import { jobStatusBadge } from '@/lib/badgeVariants'
import { toast } from '@/lib/notify'
import { getJobList, activateJob, pauseJob, closeJob } from '@/api/modules/job'
import { getJudgment } from '@/api/modules/brain'
import { resolveJobRecruitStatuses, type JobRecruitStatus } from '@/utils/jobRecruitStatus'

const router = useRouter()
const recruitStatusMap = ref(new Map<number, JobRecruitStatus | null>())
const aiHealthMap = ref(new Map<number, string>())

const queryParams = reactive({
  title: '',
  status: '' as string | undefined,
  demandNo: '',
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const jobList = ref<any[]>([])
const closeDialogVisible = ref(false)
const closeReason = ref('')
const closingJobId = ref<number | null>(null)

const confirmState = reactive({
  open: false,
  title: '',
  message: '',
  destructive: false,
  onConfirm: null as null | (() => void),
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '招聘中', value: 'ACTIVE' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已关闭', value: 'CLOSED' },
]

function getRowActions(row: any) {
  return [
    { command: 'view', label: '工作台', icon: 'Monitor', type: 'primary', primary: true },
    { command: 'requirements', label: '任职要求', icon: 'Document' },
    { command: 'activate', label: '开始招聘', icon: 'VideoPlay' },
    { command: 'pause', label: '暂停招聘', icon: 'VideoPause' },
    { command: 'close', label: '关闭职位', icon: 'CircleClose', divided: true },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') handleView(row)
  else if (cmd === 'requirements') handleEditRequirements(row)
  else if (cmd === 'activate') handleActivate(row)
  else if (cmd === 'pause') handlePause(row)
  else if (cmd === 'close') handleClose(row)
}

function openConfirm(title: string, message: string, onConfirm: () => void, destructive = false) {
  confirmState.title = title
  confirmState.message = message
  confirmState.destructive = destructive
  confirmState.onConfirm = () => {
    confirmState.open = false
    onConfirm()
  }
  confirmState.open = true
}

async function loadData() {
  const res: any = await getJobList(queryParams)
  jobList.value = res.data?.list || res.data?.records || []
  total.value = Number(res.data?.total) || 0
  recruitStatusMap.value = await resolveJobRecruitStatuses(jobList.value)

  const healthMap = new Map<number, string>()
  for (const row of jobList.value) {
    try {
      const res = await getJudgment('JOB', row.id).catch(() => null)
      const data = (res as any)?.data
      if (data?.judgmentText) {
        const t = data.judgmentText
        if (t.includes('枯') || t.includes('危险')) healthMap.set(row.id, 'critical')
        else if (t.includes('缓慢') || t.includes('不足')) healthMap.set(row.id, 'warning')
        else healthMap.set(row.id, 'healthy')
      }
    } catch {}
  }
  aiHealthMap.value = healthMap
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.title = ''
  queryParams.status = undefined
  queryParams.demandNo = ''
  handleSearch()
}

function handleCreate() {
  router.push('/planning/jobs/create')
}

function handleView(row: any) {
  router.push(`/planning/jobs/${row.id}`)
}

function handleEditRequirements(row: any) {
  router.push(`/planning/jobs/${row.id}/jd`)
}

function handleActivate(row: any) {
  openConfirm('开始招聘', '确定开始该职位的招聘吗？', async () => {
    await activateJob(row.id)
    toast.success('职位已开始招聘')
    loadData()
  })
}

function handlePause(row: any) {
  openConfirm('暂停招聘', '确定暂停该职位的招聘吗？', async () => {
    await pauseJob(row.id)
    toast.success('职位已暂停')
    loadData()
  })
}

function handleClose(row: any) {
  closingJobId.value = row.id
  closeReason.value = ''
  closeDialogVisible.value = true
}

async function confirmClose() {
  if (!closingJobId.value) return
  try {
    await closeJob(closingJobId.value, closeReason.value)
    toast.success('职位已关闭')
    closeDialogVisible.value = false
    loadData()
  } catch {
    /* handled by interceptor */
  }
}

onMounted(() => loadData())
</script>
