<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <h2 class="page-title">招人方式建议</h2>
      <p class="page-desc">系统根据平台招人数据生成优化建议。采纳后仅影响新开启的招人任务，须你确认后才会生效。</p>
    </div>

    <div class="job-selector">
      <span class="selector-label">筛选职位：</span>
      <el-select v-model="filterJobId" clearable placeholder="全部职位" style="width: 240px" @change="loadList">
        <el-option v-for="job in jobOptions" :key="job.id" :label="job.title" :value="job.id" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="proposals" size="small" class="mt-16">
      <el-table-column prop="title" label="建议摘要" min-width="260" />
      <el-table-column label="职位" width="140">
        <template #default="{ row }">{{ jobTitle(row.jobId) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.proposalType === 'ROLLBACK'" type="danger" size="small">
            {{ proposalTypeLabel(row.proposalType) }}
          </el-tag>
          <el-tag v-else size="small" type="info">{{ proposalTypeLabel(row.proposalType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="依据" min-width="160">
        <template #default="{ row }">
          <span v-if="row.evidence">
            {{ row.evidence.signalCount }} 条数据 · 淘汰 {{ row.evidence.skipCount }} / 通过 {{ row.evidence.passCount }}
          </span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="expiresAt" label="有效期至" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleConfirm(row)">采纳</el-button>
          <el-button size="small" @click="handleReject(row.id)">暂不采纳</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" :title="OBJECTS.evolutionSuggestion" width="640px" destroy-on-close>
      <template v-if="current">
        <h4 class="detail-title">{{ current.title }}</h4>
        <el-descriptions :column="1" border size="small" class="mb-12">
          <el-descriptions-item label="职位">{{ jobTitle(current.jobId) }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ proposalTypeLabel(current.proposalType) }}</el-descriptions-item>
          <el-descriptions-item label="当前招人方式">v{{ current.baseOpsPackVersion }}</el-descriptions-item>
        </el-descriptions>

        <h5>变更说明</h5>
        <ul class="human-list">
          <li v-for="(line, i) in diffLines" :key="i">{{ line }}</li>
        </ul>

        <h5>采纳后摘要</h5>
        <ul class="human-list">
          <li v-for="(line, i) in proposedSummary" :key="'p'+i">{{ line }}</li>
        </ul>

        <el-alert type="info" :closable="false" show-icon class="mt-12"
          title="采纳后将发布新版招人方式，只影响此后新开启的平台招人任务；正在运行的任务不变。" />
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current" type="primary" @click="handleConfirm(current)">采纳并发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { OBJECTS } from '@/constants/businessLabels'
import { diffHumanLines, opsPackHumanSummary, proposalTypeLabel } from '@/utils/opsPackSummary'
import {
  getEvolutionProposalList,
  getEvolutionProposal,
  confirmEvolutionProposal,
  rejectEvolutionProposal,
} from '@/api/modules/evolution'
import { getJobList } from '@/api/modules/job'

const loading = ref(false)
const proposals = ref<any[]>([])
const jobOptions = ref<any[]>([])
const filterJobId = ref<number | undefined>()
const detailVisible = ref(false)
const current = ref<any>(null)

const diffLines = computed(() => diffHumanLines(current.value?.diff))
const proposedSummary = computed(() =>
  opsPackHumanSummary(current.value?.proposedOpsPack?.pack || current.value?.proposedOpsPack),
)

function jobTitle(jobId?: number) {
  if (!jobId) return '—'
  const j = jobOptions.value.find(o => o.id === jobId)
  return j?.title || `职位 #${jobId}`
}

async function loadJobs() {
  const res: any = await getJobList({ pageSize: 100, status: 'ACTIVE' })
  jobOptions.value = res.data?.records || res.data?.list || res.data || []
}

async function loadList() {
  loading.value = true
  try {
    const res: any = await getEvolutionProposalList({
      jobId: filterJobId.value,
      status: 'PENDING',
      pageSize: 50,
    })
    proposals.value = res.data?.list || res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
}

async function openDetail(row: any) {
  const res: any = await getEvolutionProposal(row.id)
  current.value = res.data || res
  detailVisible.value = true
}

async function handleConfirm(row: any) {
  const id = row.id
  const summary = opsPackHumanSummary(row.proposedOpsPack?.pack || row.proposedOpsPack).join('\n')
  try {
    await ElMessageBox.confirm(
      `将发布新版招人方式。\n\n${summary || row.title}\n\n只影响此后新开启的平台招人任务；正在运行的任务不变。`,
      '确认采纳建议',
      { confirmButtonText: '采纳并发布', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await confirmEvolutionProposal(id)
  ElMessage.success('已采纳，新版招人方式已发布')
  detailVisible.value = false
  loadList()
}

async function handleReject(id: number) {
  const { value } = await ElMessageBox.prompt('请说明暂不采纳的原因（至少 10 字）', '暂不采纳', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    inputValidator: (v) => (v && v.trim().length >= 10) || '请至少填写 10 字',
  }).catch(() => ({ value: null }))
  if (value === null) return
  await rejectEvolutionProposal(id, value.trim())
  ElMessage.success('已记录你的反馈')
  loadList()
}

onMounted(async () => {
  await loadJobs()
  await loadList()
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-desc { color: #64748b; font-size: 13px; margin-top: 4px; }
.job-selector { display: flex; align-items: center; gap: 8px; }
.selector-label { color: #64748b; font-size: 13px; }
.mt-16 { margin-top: 16px; }
.mb-12 { margin-bottom: 12px; }
.mt-12 { margin-top: 12px; }
.detail-title { margin: 0 0 12px; font-size: 16px; }
.human-list { margin: 8px 0 16px 18px; color: #334155; line-height: 1.7; }
</style>
