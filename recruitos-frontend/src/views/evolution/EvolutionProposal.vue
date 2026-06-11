<template>
  <PageShell variant="list"
    title="招人方式建议"
    subtitle="系统根据平台招人数据生成优化建议。采纳后仅影响新开启的招人任务，须你确认后才会生效。"
    :loading="loading"
  >
    <template #toolbar>
      <div class="job-selector">
        <span class="selector-label">筛选职位：</span>
        <RSelect
          v-model="filterJobId"
          :options="jobSelectOptions"
          placeholder="全部职位"
          clearable
          class="w-[240px]"
          @update:model-value="loadList"
        />
      </div>
    </template>

    <RTable v-if="proposals.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="min-w-[260px]">建议摘要</RTableTh>
          <RTableTh class="w-[140px]">职位</RTableTh>
          <RTableTh class="w-[140px]">类型</RTableTh>
          <RTableTh class="min-w-[160px]">依据</RTableTh>
          <RTableTh class="w-[160px]">有效期至</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in proposals" :key="row.id">
          <RTableCell>{{ row.title }}</RTableCell>
          <RTableCell>{{ jobTitle(row.jobId) }}</RTableCell>
          <RTableCell>
            <RBadge :variant="row.proposalType === 'ROLLBACK' ? 'destructive' : 'secondary'">
              {{ proposalTypeLabel(row.proposalType) }}
            </RBadge>
          </RTableCell>
          <RTableCell>
            <span v-if="row.evidence">
              {{ row.evidence.signalCount }} 条数据 · 淘汰 {{ row.evidence.skipCount }} / 通过 {{ row.evidence.passCount }}
            </span>
            <span v-else>—</span>
          </RTableCell>
          <RTableCell>{{ row.expiresAt }}</RTableCell>
          <RTableCell class="text-center">
            <div class="flex items-center gap-1"><RButton size="sm" variant="ghost" class="text-primary" @click="openCopilotForProposal(row)" title="AI 分析此提案"><Sparkles class="h-3.5 w-3.5" /></RButton><RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" /></div>
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta
      v-else
      title="暂无待处理建议"
      description="系统将根据招人数据自动生成优化建议"
    />

    <!-- AI 策略建议 (M5: Brain 分析人机差异) -->
    <div v-if="brainProposals.length" class="mt-8">
      <div class="flex items-center gap-2 mb-3">
        <Sparkles class="h-4 w-4 text-primary" />
        <span class="text-sm font-semibold text-foreground">AI 策略建议</span>
        <RBadge variant="outline" size="sm">Brain</RBadge>
      </div>
      <div class="space-y-3">
        <RCard v-for="bp in brainProposals" :key="bp.id" variant="flat" padding="md" class="!bg-primary-light/5 !border-primary/10">
          <div class="flex items-start justify-between">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-sm font-semibold text-text-primary">{{ bp.title }}</span>
                <RBadge variant="outline" size="sm" class="text-[10px]">置信度 {{ ((bp.confidence || 0) * 100).toFixed(0) }}%</RBadge>
              </div>
              <p class="text-xs text-text-secondary mb-2">{{ bp.description }}</p>
              <p class="text-xs text-primary font-medium">→ {{ bp.proposedAction }}</p>
            </div>
            <div class="flex items-center gap-2 ml-4 shrink-0">
              <RButton size="sm" variant="ghost" class="text-primary" @click="openCopilotForBrainProposal(bp)" title="Co-Pilot 分析">
                <Sparkles class="h-3.5 w-3.5" />
              </RButton>
              <RButton size="sm" variant="outline" @click="dismissBrainProposal(bp)">忽略</RButton>
            </div>
          </div>
        </RCard>
      </div>
    </div>

    <template #below>
      <RDialog v-model:open="detailVisible">
        <RDialogContent class="max-w-2xl">
          <RDialogHeader>
            <RDialogTitle>{{ OBJECTS.evolutionSuggestion }}</RDialogTitle>
          </RDialogHeader>
          <template v-if="current">
            <h4 class="detail-title">{{ current.title }}</h4>
            <dl class="detail-dl">
              <div><dt>职位</dt><dd>{{ jobTitle(current.jobId) }}</dd></div>
              <div><dt>类型</dt><dd>{{ proposalTypeLabel(current.proposalType) }}</dd></div>
              <div><dt>当前招人方式</dt><dd>v{{ current.baseOpsPackVersion }}</dd></div>
            </dl>

            <h5 class="section-heading">变更说明</h5>
            <ul class="human-list">
              <li v-for="(line, i) in diffLines" :key="i">{{ line }}</li>
            </ul>

            <h5 class="section-heading">采纳后摘要</h5>
            <ul class="human-list">
              <li v-for="(line, i) in proposedSummary" :key="'p' + i">{{ line }}</li>
            </ul>

            <RAlert variant="default" class="mt-4">
              <RAlertDescription>
                采纳后将发布新版招人方式，只影响此后新开启的平台招人任务；正在运行的任务不变。
              </RAlertDescription>
            </RAlert>
          </template>
          <RDialogFooter>
            <RButton variant="outline" @click="detailVisible = false">关闭</RButton>
            <RButton v-if="current" @click="handleConfirm(current)">采纳并发布</RButton>
          </RDialogFooter>
        </RDialogContent>
      </RDialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { toast } from '@/lib/notify'
import { Sparkles } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { confirm } from '@/lib/confirm'
import { prompt } from '@/lib/prompt'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import {
  RButton,
  RSelect,
  RBadge,
  RTable,
  RTableHead,
  RTableBody,
  RTableRow,
  RTableTh,
  RTableCell,
  RDialog,
  RDialogContent,
  RDialogHeader,
  RDialogTitle,
  RDialogFooter,
  RAlert,
  RAlertDescription,
} from '@/components/ui'
import { OBJECTS } from '@/constants/businessLabels'
import { diffHumanLines, opsPackHumanSummary, proposalTypeLabel } from '@/utils/opsPackSummary'
import { getEvolutionProposalList, getEvolutionProposal, confirmEvolutionProposal, rejectEvolutionProposal } from '@/api/modules/evolution'
import { getJobList } from '@/api/modules/job'

const loading = ref(false)
const router = useRouter()

const brainProposals = ref<any[]>([])

async function loadBrainProposals() {
  try {
    const res: any = await import('@/api/modules/brain').then(m => m.getStrategyProposals())
    brainProposals.value = (res as any)?.data || (res as any) || []
  } catch { brainProposals.value = [] }
}

function openCopilotForBrainProposal(bp: any) {
  const context = JSON.stringify({
    type: 'strategy_proposal',
    title: bp.title,
    description: bp.description,
    proposedAction: bp.proposedAction,
    evidence: bp.evidence,
    confidence: bp.confidence,
  })
  router.push({ path: '/ai/copilot', query: { context } })
}

function dismissBrainProposal(bp: any) {
  brainProposals.value = brainProposals.value.filter((p: any) => p.id !== bp.id)
  toast.info('建议已忽略')
}


function openCopilotForProposal(row: any) {
  const context = JSON.stringify({
    title: row.title,
    proposalType: proposalTypeLabel(row.proposalType),
    jobTitle: jobTitle(row.jobId),
    evidence: row.evidence,
    currentVersion: row.baseOpsPackVersion,
    diff: diffHumanLines(row.diff),
  })
  router.push({ path: '/ai/copilot', query: { context } })
}
const proposals = ref<any[]>([])
const jobOptions = ref<any[]>([])
const filterJobId = ref<number | undefined>()
const detailVisible = ref(false)
const current = ref<any>(null)

const jobSelectOptions = computed(() =>
  jobOptions.value.map((j) => ({ label: j.title, value: j.id }))
)

const diffLines = computed(() => diffHumanLines(current.value?.diff))
const proposedSummary = computed(() => opsPackHumanSummary(current.value?.proposedOpsPack?.pack || current.value?.proposedOpsPack))

function jobTitle(jobId?: number) {
  if (!jobId) return '—'
  return jobOptions.value.find((o) => o.id === jobId)?.title || `职位 #${jobId}`
}

function getRowActions(_row: any) {
  return [
    { command: 'detail', label: '查看详情', icon: 'View', type: 'primary', primary: true },
    { command: 'confirm', label: '采纳', icon: 'CircleCheck' },
    { command: 'reject', label: '暂不采纳', icon: 'CircleClose', divided: true },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'detail') openDetail(row)
  else if (cmd === 'confirm') handleConfirm(row)
  else if (cmd === 'reject') handleReject(row.id)
}

async function loadJobs() {
  const res: any = await getJobList({ pageSize: 100, status: 'ACTIVE' })
  jobOptions.value = res.data?.records || res.data?.list || res.data || []
}

async function loadList() {
  loading.value = true
  try {
    const res: any = await getEvolutionProposalList({ jobId: filterJobId.value, status: 'PENDING', pageSize: 50 })
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
  const summary = opsPackHumanSummary(row.proposedOpsPack?.pack || row.proposedOpsPack).join('\n')
  const ok = await confirm({
    title: '确认采纳建议',
    message: `将发布新版招人方式。\n\n${summary || row.title}\n\n只影响此后新开启的平台招人任务；正在运行的任务不变。`,
    confirmText: '采纳并发布',
    cancelText: '取消',
  })
  if (!ok) return
  await confirmEvolutionProposal(row.id)
  toast.success('已采纳，新版招人方式已发布')
  detailVisible.value = false
  loadList()
}

async function handleReject(id: number) {
  const value = await prompt({
    title: '暂不采纳',
    message: '请说明暂不采纳的原因（至少 10 字）',
    confirmText: '提交',
    cancelText: '取消',
    validator: (v) => (v && v.trim().length >= 10) || '请至少填写 10 字',
  })
  if (value === null) return
  await rejectEvolutionProposal(id, value.trim())
  toast.success('已记录你的反馈')
  loadList()
}

onMounted(async () => {
  await loadJobs()
  await loadList()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.job-selector {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.selector-label {
  color: $text-secondary;
  font-size: $text-caption;
}

.detail-title {
  margin: 0 0 $spacing-md;
  font-size: $text-heading-md;
}

.section-heading {
  margin: $spacing-md 0 $spacing-sm;
  font-size: 14px;
  font-weight: 600;
}

.detail-dl {
  display: grid;
  gap: 8px;
  margin-bottom: $spacing-md;
  font-size: 14px;

  div {
    display: grid;
    grid-template-columns: 120px 1fr;
    gap: 8px;
  }

  dt {
    color: $text-secondary;
  }

  dd {
    margin: 0;
    color: $text-primary;
  }
}

.human-list {
  margin: $spacing-sm 0 $spacing-base 18px;
  color: $text-regular;
  line-height: 1.7;
}
</style>
