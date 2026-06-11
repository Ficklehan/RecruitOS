<template>
  <PageShell variant="list"
    title="简历收件"
    subtitle="管理各渠道收到的简历，解析后可加入人才库或关联在招职位"
  >
    <template #actions>
      <RButton variant="outline" size="icon" @click="handleRefresh">
        <RefreshCw class="h-4 w-4" />
      </RButton>
      <RButton @click="router.push('/talent/resumes/upload')">
        <Upload class="mr-2 h-4 w-4" />
        上传简历
      </RButton>
    </template>

    <template #toolbar>
      <div class="stat-row">
        <StatCard label="简历总数" :value="stats.total" icon="Document" color="$primary-color" />
        <StatCard label="已解析" :value="stats.parsed" icon="CircleCheck" color="$success-color" />
        <StatCard label="待解析" :value="stats.pending" icon="Clock" color="$warning-color" />
        <StatCard label="解析失败" :value="stats.failed" icon="Warning" color="#DC2626" />
      </div>
    </template>

    <template #filters>
      <RInput
        v-model="searchKeyword"
        placeholder="搜索姓名、公司、技能..."
        class="w-full sm:w-64"
        @keyup.enter="handleSearch"
      />
      <RSelect
        v-model="filterSource"
        :options="sourceOptions"
        placeholder="来源渠道"
        clearable
        class="w-full sm:w-40"
      />
      <RSelect
        v-model="filterStatus"
        :options="statusOptions"
        placeholder="解析状态"
        clearable
        class="w-full sm:w-40"
      />
    </template>

    <template #filterActions>
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </RButton>
      <RButton variant="outline" @click="handleReset">重置</RButton>
      <RButton v-if="selectedIds.length" variant="secondary" @click="handleBatchImport">
        <FolderPlus class="mr-2 h-4 w-4" />
        批量加入人才库 ({{ selectedIds.length }})
      </RButton>
    </template>

    <RTable v-if="resumeList.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-10" />
          <RTableTh class="w-[120px]">姓名</RTableTh>
          <RTableTh class="w-[130px]">手机号</RTableTh>
          <RTableTh class="min-w-[160px]">当前公司</RTableTh>
          <RTableTh class="min-w-[140px]">当前职位</RTableTh>
          <RTableTh class="w-[90px] text-center">工作年限</RTableTh>
          <RTableTh class="w-[90px] text-center">学历</RTableTh>
          <RTableTh class="w-[100px] text-center">来源</RTableTh>
          <RTableTh class="w-[100px] text-center">解析状态</RTableTh>
          <RTableTh class="w-[160px]">上传时间</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in resumeList" :key="row.id">
          <RTableCell>
            <RCheckbox
              :model-value="selectedIds.includes(row.id)"
              @update:model-value="(v) => toggleSelect(row.id, v)"
            />
          </RTableCell>
          <RTableCell>
            <button type="button" class="font-medium text-primary hover:underline" @click="goDetail(row)">
              {{ row.name || '未识别' }}
            </button>
          </RTableCell>
          <RTableCell>{{ row.phone }}</RTableCell>
          <RTableCell>{{ row.company }}</RTableCell>
          <RTableCell>{{ row.position }}</RTableCell>
          <RTableCell class="text-center">{{ row.workYears ? row.workYears + '年' : '—' }}</RTableCell>
          <RTableCell class="text-center">{{ educationLabel(row.education) }}</RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(sourceTagMap[row.source])">{{ sourceLabel(row.source) }}</RBadge>
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(parseStatusTagMap[row.parseStatus])">
              {{ parseStatusLabelMap[row.parseStatus] }}
            </RBadge>
          </RTableCell>
          <RTableCell class="text-muted-foreground">{{ row.createdAt }}</RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta
      v-else
      title="暂无简历"
      description="上传简历后将自动进入收件列表，解析后可加入人才库进行匹配"
      :actions="[
        { label: '上传简历', type: 'primary', onClick: () => router.push('/talent/resumes/upload') },
        { label: '去人才库', type: 'default', onClick: () => router.push('/talent/pool') },
      ]"
    />

    <ListPagination
      v-if="total > 0"
      v-model:page-num="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadData"
    />
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, RefreshCw, Upload, FolderPlus } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import StatCard from '@/components/StatCard.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import ListPagination from '@/components/common/ListPagination.vue'
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
  RCheckbox,
} from '@/components/ui'
import { sourceLabel, educationLabel } from '@/constants/businessLabels'
import {
  getResumeList,
  parseResume,
  deleteResume,
  importToTalentPool,
  batchImportToTalentPool,
} from '@/api/modules/resume'

const router = useRouter()
const searchKeyword = ref('')
const filterSource = ref<string | undefined>()
const filterStatus = ref<string | undefined>()
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const resumeList = ref<any[]>([])
const selectedIds = ref<number[]>([])

const sourceOptions = [
  { label: 'Boss直聘', value: 'BOSS' },
  { label: '猎聘', value: 'LIEPIN' },
  { label: '手动上传', value: 'MANUAL' },
  { label: '内推', value: 'REFERRAL' },
  { label: '猎头', value: 'HEADHUNTER' },
]

const statusOptions = [
  { label: '已解析', value: 'PARSED' },
  { label: '待解析', value: 'PENDING' },
  { label: '解析失败', value: 'FAILED' },
]

const stats = computed(() => {
  const t = resumeList.value.length
  const parsed = resumeList.value.filter((r) => r.parseStatus === 'PARSED').length
  const pending = resumeList.value.filter((r) => r.parseStatus === 'PENDING').length
  const failed = resumeList.value.filter((r) => r.parseStatus === 'FAILED').length
  return { total: t, parsed, pending, failed }
})

const sourceTagMap: Record<string, string> = {
  BOSS: '',
  LIEPIN: 'danger',
  MANUAL: 'info',
  REFERRAL: 'success',
  HEADHUNTER: 'warning',
}
const parseStatusTagMap: Record<string, string> = {
  PARSED: 'success',
  PENDING: 'warning',
  FAILED: 'danger',
}
const parseStatusLabelMap: Record<string, string> = {
  PARSED: '已解析',
  PENDING: '待解析',
  FAILED: '解析失败',
}

function toggleSelect(id: number, checked: boolean) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter((x) => x !== id)
  }
}

function getRowActions(_row: any) {
  return [
    { command: 'view', label: '查看详情', icon: 'View', primary: true },
    { command: 'parse', label: '解析', icon: 'MagicStick' },
    { command: 'import', label: '加入人才库', icon: 'FolderAdd' },
    { command: 'delete', label: '删除', icon: 'Delete', divided: true },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') goDetail(row)
  else if (cmd === 'parse') handleParse(row)
  else if (cmd === 'import') handleImportPool(row)
  else if (cmd === 'delete') handleDelete(row)
}

function goDetail(row: any) {
  router.push(`/talent/resumes/${row.id}`)
}

async function handleParse(row: any) {
  try {
    await parseResume(row.id)
    row.parseStatus = 'PARSED'
    toast.success('解析完成')
  } catch {
    toast.error('解析失败')
  }
}

async function handleImportPool(row: any) {
  try {
    await importToTalentPool(row.id)
    toast.success('已加入人才库')
  } catch {
    toast.error('加入失败')
  }
}

async function handleBatchImport() {
  try {
    await batchImportToTalentPool(selectedIds.value)
    toast.success('批量加入成功')
    selectedIds.value = []
    loadData()
  } catch {
    toast.error('批量加入失败')
  }
}

async function handleDelete(row: any) {
  const ok = await confirm({ title: '删除确认', message: '确定删除该简历？', destructive: true })
  if (!ok) return
  try {
    await deleteResume(row.id)
    resumeList.value = resumeList.value.filter((r) => r.id !== row.id)
    toast.success('已删除')
  } catch {
    toast.error('删除失败')
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function handleReset() {
  searchKeyword.value = ''
  filterSource.value = undefined
  filterStatus.value = undefined
  currentPage.value = 1
  loadData()
}

function handleRefresh() {
  loadData()
  toast.success('已刷新')
}

async function loadData() {
  try {
    const res: any = await getResumeList({
      keyword: searchKeyword.value,
      source: filterSource.value,
      parseStatus: filterStatus.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    const data = res.data || res
    resumeList.value = Array.isArray(data) ? data : data.records || data.list || []
    total.value = data.total || resumeList.value.length
  } catch {
    resumeList.value = []
    total.value = 0
  }
}

onMounted(() => loadData())
</script>
