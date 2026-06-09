<template>
  <div :class="embedded ? 'staging-embed' : 'page-container'" v-loading="loading">
    <div v-if="!embedded" class="page-header">
      <h2 class="page-title">跨职位待联系池</h2>
      <p class="page-desc">先收藏、暂不联系的人选缓冲区。可批量发送首次联系或纳入本职位候选人。</p>
    </div>

    <div class="toolbar">
      <el-select v-model="filters.jobId" clearable placeholder="全部岗位" style="width: 220px" @change="loadList">
        <el-option v-for="j in jobOptions" :key="j.id" :label="j.title" :value="j.id" />
      </el-select>
      <el-select v-model="filters.platform" clearable placeholder="平台" style="width: 120px" @change="loadList">
        <el-option label="Boss直聘" value="BOSS" />
        <el-option label="猎聘" value="LIEPIN" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态" style="width: 130px" @change="loadList">
        <el-option label="待处理" value="STAGED" />
        <el-option label="已打招呼" value="GREETED" />
        <el-option label="已纳入" value="IMPORTED" />
        <el-option label="不合适" value="REJECTED" />
      </el-select>
      <el-select v-model="filters.sort" style="width: 140px" @change="loadList">
        <el-option label="按匹配分" value="matchScore" />
        <el-option label="按采集时间" value="createdAt" />
      </el-select>
      <el-button @click="loadList">刷新</el-button>
    </div>

    <div v-if="selectedIds.length" class="batch-bar">
      <span>已选 {{ selectedIds.length }} 人</span>
      <el-button size="small" type="primary" @click="doBatchGreet">批量打招呼</el-button>
      <el-button size="small" type="success" @click="doBatchImport">纳入本职位候选人</el-button>
      <el-button size="small" @click="doBatchReject">标记不合适</el-button>
    </div>

    <el-table :data="rows" size="small" @selection-change="onSelect">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="candidateName" label="姓名" width="100" />
      <el-table-column prop="jobTitle" label="岗位" min-width="140" />
      <el-table-column prop="platform" label="平台" width="90" />
      <el-table-column prop="matchScore" label="匹配分" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">{{ statusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column label="提取字段" min-width="180">
        <template #default="{ row }">
          <span v-if="row.extractedFields?._lastAnswer" class="field-hint">
            {{ row.extractedFields._lastAnswer }}
          </span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAsk(row)">AI 问答</el-button>
          <el-button v-if="row.status === 'STAGED'" link @click="singleGreet(row.id)">打招呼</el-button>
          <el-button v-if="row.status !== 'IMPORTED'" link type="success" @click="singleImport(row.id)">纳入候选人</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="mt-16"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="pageNum"
      @current-change="loadList"
    />

    <el-dialog v-model="askVisible" title="信息核对" width="520px" destroy-on-close>
      <el-input v-model="askQuestion" placeholder="例如：上一家公司是？工作年限？" />
      <div v-if="askAnswer" class="ask-answer">{{ askAnswer }}</div>
      <template #footer>
        <el-button @click="askVisible = false">关闭</el-button>
        <el-button type="primary" :loading="asking" @click="submitAsk">提问</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

function onSelect(list: any[]) {
  selectedIds.value = list.map(r => r.id)
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
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function doBatchGreet() {
  await batchStagingGreet(selectedIds.value)
  ElMessage.success('已提交打招呼')
  loadList()
}

async function doBatchImport() {
  await batchStagingImport(selectedIds.value)
  ElMessage.success(`已${ACTIONS.importCandidate}`)
  loadList()
}

async function doBatchReject() {
  const { value } = await ElMessageBox.prompt('可选填写原因', '标记不合适', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  }).catch(() => ({ value: null }))
  if (value === null) return
  await batchStagingReject(selectedIds.value, value || undefined)
  ElMessage.success('已标记')
  loadList()
}

function singleGreet(id: number) {
  batchStagingGreet([id]).then(() => { ElMessage.success('已打招呼'); loadList() })
}

function singleImport(id: number) {
  batchStagingImport([id]).then(() => { ElMessage.success('已纳入候选人'); loadList() })
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

onMounted(async () => {
  if (props.defaultJobId) filters.jobId = props.defaultJobId
  await loadJobs()
  await loadList()
})
</script>

<style scoped>
.staging-embed { padding: 0; }
.page-desc { color: #64748b; font-size: 13px; margin-top: 4px; }
.toolbar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.batch-bar {
  display: flex; align-items: center; gap: 12px;
  background: #f0f9ff; padding: 10px 12px; border-radius: 8px; margin-bottom: 12px;
}
.field-hint { font-size: 12px; color: #475569; }
.muted { color: #94a3b8; }
.ask-answer { margin-top: 12px; padding: 12px; background: #f8fafc; border-radius: 8px; font-size: 13px; }
.mt-16 { margin-top: 16px; }
</style>
