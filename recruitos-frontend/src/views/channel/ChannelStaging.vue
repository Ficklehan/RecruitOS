<template>
  <PageShell variant="list"
    v-if="!embedded"
    title="跨职位待联系池"
    subtitle="先收藏、暂不联系的人选缓冲区。可批量发送首次联系或纳入本职位候选人。"
    :loading="loading"
    plain
  >
    <template #filters>
      <RSelect
        v-model="filters.jobId"
        :options="jobSelectOptions"
        placeholder="全部岗位"
        clearable
        class="w-full sm:w-56"
        @update:model-value="loadList"
      />
      <RSelect
        v-model="filters.platform"
        :options="platformOptions"
        placeholder="平台"
        clearable
        class="w-full sm:w-32"
        @update:model-value="loadList"
      />
      <RSelect
        v-model="filters.status"
        :options="statusOptions"
        placeholder="状态"
        clearable
        class="w-full sm:w-36"
        @update:model-value="loadList"
      />
      <RSelect
        v-model="filters.sort"
        :options="sortOptions"
        class="w-full sm:w-36"
        @update:model-value="loadList"
      />
    </template>

    <template #filterActions>
      <RButton variant="outline" @click="loadList">
        <RefreshCw class="mr-2 h-4 w-4" />
        刷新
      </RButton>
    </template>

    <div v-if="selectedIds.length" class="flex items-center gap-3 rounded-lg bg-sky-50 px-3 py-2.5 mb-3">
      <span class="text-sm">已选 {{ selectedIds.length }} 人</span>
      <RButton size="sm" @click="doBatchGreet">批量打招呼</RButton>
      <RButton size="sm" variant="secondary" @click="doBatchImport">纳入本职位候选人</RButton>
      <RButton size="sm" variant="outline" @click="doBatchReject">标记不合适</RButton>
    </div>

    <RTable v-if="rows.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-10" />
          <RTableTh class="w-[100px]">姓名</RTableTh>
          <RTableTh class="min-w-[140px]">岗位</RTableTh>
          <RTableTh class="w-[90px]">平台</RTableTh>
          <RTableTh class="w-[80px]">匹配分</RTableTh>
          <RTableTh class="w-[100px]">状态</RTableTh>
          <RTableTh class="min-w-[180px]">提取字段</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in rows" :key="row.id">
          <RTableCell>
            <RCheckbox
              :model-value="selectedIds.includes(row.id)"
              @update:model-value="(v) => toggleSelect(row.id, v)"
            />
          </RTableCell>
          <RTableCell>{{ row.candidateName }}</RTableCell>
          <RTableCell>{{ row.jobTitle }}</RTableCell>
          <RTableCell>{{ row.platform }}</RTableCell>
          <RTableCell>{{ row.matchScore }}</RTableCell>
          <RTableCell>{{ statusLabel(row.status) }}</RTableCell>
          <RTableCell>
            <span v-if="row.extractedFields?._lastAnswer" class="text-xs text-muted-foreground">
              {{ row.extractedFields._lastAnswer }}
            </span>
            <span v-else class="text-muted-foreground">—</span>
          </RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <ListPagination
      v-if="total > 0"
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadList"
    />

    <template #below>
      <RDialog v-model:open="askVisible">
        <RDialogContent class="max-w-lg">
          <RDialogHeader>
            <RDialogTitle>信息核对</RDialogTitle>
          </RDialogHeader>
          <RInput v-model="askQuestion" placeholder="例如：上一家公司是？工作年限？" />
          <div v-if="askAnswer" class="mt-3 rounded-lg bg-muted p-3 text-sm">{{ askAnswer }}</div>
          <RDialogFooter>
            <RButton variant="outline" @click="askVisible = false">关闭</RButton>
            <RButton :disabled="asking" @click="submitAsk">提问</RButton>
          </RDialogFooter>
        </RDialogContent>
      </RDialog>
    </template>
  </PageShell>

  <div v-else class="space-y-3" :class="{ 'opacity-60 pointer-events-none': loading }">
    <div class="flex flex-wrap gap-2">
      <RSelect
        v-model="filters.jobId"
        :options="jobSelectOptions"
        placeholder="全部岗位"
        clearable
        class="w-56"
        @update:model-value="loadList"
      />
      <RSelect
        v-model="filters.platform"
        :options="platformOptions"
        placeholder="平台"
        clearable
        class="w-32"
        @update:model-value="loadList"
      />
      <RSelect
        v-model="filters.status"
        :options="statusOptions"
        placeholder="状态"
        clearable
        class="w-36"
        @update:model-value="loadList"
      />
      <RSelect
        v-model="filters.sort"
        :options="sortOptions"
        class="w-36"
        @update:model-value="loadList"
      />
      <RButton variant="outline" size="sm" @click="loadList">
        <RefreshCw class="mr-2 h-4 w-4" />
        刷新
      </RButton>
    </div>

    <div v-if="selectedIds.length" class="flex items-center gap-3 rounded-lg bg-sky-50 px-3 py-2.5">
      <span class="text-sm">已选 {{ selectedIds.length }} 人</span>
      <RButton size="sm" @click="doBatchGreet">批量打招呼</RButton>
      <RButton size="sm" variant="secondary" @click="doBatchImport">纳入本职位候选人</RButton>
      <RButton size="sm" variant="outline" @click="doBatchReject">标记不合适</RButton>
    </div>

    <RTable v-if="rows.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-10" />
          <RTableTh class="w-[100px]">姓名</RTableTh>
          <RTableTh class="min-w-[140px]">岗位</RTableTh>
          <RTableTh class="w-[90px]">平台</RTableTh>
          <RTableTh class="w-[80px]">匹配分</RTableTh>
          <RTableTh class="w-[100px]">状态</RTableTh>
          <RTableTh class="min-w-[180px]">提取字段</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in rows" :key="row.id">
          <RTableCell>
            <RCheckbox
              :model-value="selectedIds.includes(row.id)"
              @update:model-value="(v) => toggleSelect(row.id, v)"
            />
          </RTableCell>
          <RTableCell>{{ row.candidateName }}</RTableCell>
          <RTableCell>{{ row.jobTitle }}</RTableCell>
          <RTableCell>{{ row.platform }}</RTableCell>
          <RTableCell>{{ row.matchScore }}</RTableCell>
          <RTableCell>{{ statusLabel(row.status) }}</RTableCell>
          <RTableCell>
            <span v-if="row.extractedFields?._lastAnswer" class="text-xs text-muted-foreground">
              {{ row.extractedFields._lastAnswer }}
            </span>
            <span v-else class="text-muted-foreground">—</span>
          </RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <ListPagination
      v-if="total > 0"
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadList"
    />

    <RDialog v-model:open="askVisible">
      <RDialogContent class="max-w-lg">
        <RDialogHeader>
          <RDialogTitle>信息核对</RDialogTitle>
        </RDialogHeader>
        <RInput v-model="askQuestion" placeholder="例如：上一家公司是？工作年限？" />
        <div v-if="askAnswer" class="mt-3 rounded-lg bg-muted p-3 text-sm">{{ askAnswer }}</div>
        <RDialogFooter>
          <RButton variant="outline" @click="askVisible = false">关闭</RButton>
          <RButton :disabled="asking" @click="submitAsk">提问</RButton>
        </RDialogFooter>
      </RDialogContent>
    </RDialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { prompt } from '@/lib/prompt'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import {
  RButton,
  RInput,
  RSelect,
  RTable,
  RTableHead,
  RTableBody,
  RTableRow,
  RTableTh,
  RTableCell,
  RCheckbox,
  RDialog,
  RDialogContent,
  RDialogHeader,
  RDialogTitle,
  RDialogFooter,
} from '@/components/ui'
import { ACTIONS } from '@/constants/businessLabels'
import {
  getChannelStagingList,
  askChannelStaging,
  batchStagingGreet,
  batchStagingImport,
  batchStagingReject,
} from '@/api/modules/agent'
import { getJobList } from '@/api/modules/job'

const props = withDefaults(defineProps<{
  embedded?: boolean
  defaultJobId?: number
}>(), {
  embedded: false,
})

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const selectedIds = ref<number[]>([])
const jobOptions = ref<any[]>([])
const jobSelectOptions = computed(() => jobOptions.value.map((j) => ({ label: j.title, value: j.id })))

const platformOptions = [
  { label: 'Boss直聘', value: 'BOSS' },
  { label: '猎聘', value: 'LIEPIN' },
]

const statusOptions = [
  { label: '待处理', value: 'STAGED' },
  { label: '已打招呼', value: 'GREETED' },
  { label: '已纳入', value: 'IMPORTED' },
  { label: '不合适', value: 'REJECTED' },
]

const sortOptions = [
  { label: '按匹配分', value: 'matchScore' },
  { label: '按采集时间', value: 'createdAt' },
]

const filters = reactive({
  jobId: undefined as number | undefined,
  platform: undefined as string | undefined,
  status: undefined as string | undefined,
  sort: 'matchScore',
})

const askVisible = ref(false)
const askQuestion = ref('')
const askAnswer = ref('')
const asking = ref(false)
const askTargetId = ref<number | null>(null)

function statusLabel(s: string) {
  return { STAGED: '待处理', GREETED: '已打招呼', IMPORTED: '已纳入', REJECTED: '不合适' }[s] || s
}

function toggleSelect(id: number, checked: boolean) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter((i) => i !== id)
  }
}

async function loadJobs() {
  const res: any = await getJobList({ pageSize: 100 })
  jobOptions.value = res.data?.records || res.data || []
}

async function loadList() {
  loading.value = true
  try {
    const res: any = await getChannelStagingList({
      jobId: filters.jobId,
      platform: filters.platform,
      status: filters.status,
      sort: filters.sort,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    rows.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

async function doBatchGreet() {
  await batchStagingGreet(selectedIds.value)
  toast.success('已提交打招呼')
  loadList()
}

async function doBatchImport() {
  await batchStagingImport(selectedIds.value)
  toast.success(`已${ACTIONS.importCandidate}`)
  loadList()
}

async function doBatchReject() {
  const value = await prompt({ title: '标记不合适', message: '可选填写原因', placeholder: '原因（选填）' })
  if (value === null) return
  await batchStagingReject(selectedIds.value, value || undefined)
  toast.success('已标记')
  loadList()
}

function singleImport(id: number) {
  batchStagingImport([id]).then(() => { toast.success('已纳入候选人'); loadList() })
}

function openAsk(row: any) {
  askTargetId.value = row.id
  askQuestion.value = '上一家公司和工作年限？'
  askAnswer.value = row.extractedFields?._lastAnswer || ''
  askVisible.value = true
}

async function submitAsk() {
  if (!askTargetId.value || !askQuestion.value) return
  asking.value = true
  try {
    const res: any = await askChannelStaging(askTargetId.value, askQuestion.value)
    askAnswer.value = res.data?.answer || ''
    loadList()
  } finally {
    asking.value = false
  }
}

function getRowActions(_row: any) {
  return [
    { command: 'view', label: '查看详情', icon: 'View', primary: true },
    { command: 'import', label: '纳入候选人', icon: 'Promotion' },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') singleImport(row.id)
  else if (cmd === 'import') singleImport(row.id)
}

onMounted(async () => {
  if (props.defaultJobId) filters.jobId = props.defaultJobId
  await loadJobs()
  await loadList()
})
</script>
