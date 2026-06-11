<template>
  <PageShell variant="list"
    title="候选人列表"
    subtitle="管理候选人信息；选择在招职位后可查看本职位进展与 AI 评估"
  >
    <template #actions>
      <RButton @click="handleCreate">
        <Plus class="mr-2 h-4 w-4" />
        添加候选人
      </RButton>
    </template>

    <template #toolbar>
      <JobContextBar v-model="queryParams.jobId" @update:model-value="onJobChange" />
    </template>

    <template #filters>
      <RInput
        v-model="queryParams.name"
        placeholder="搜索姓名、公司、职位"
        class="w-full sm:w-64"
        @keyup.enter="handleSearch"
      />
      <RSelect
        v-model="queryParams.status"
        :options="statusOptions"
        placeholder="状态"
        clearable
        class="w-full sm:w-40"
      />
      <RSelect
        v-model="queryParams.source"
        :options="sourceOptions"
        placeholder="来源"
        clearable
        class="w-full sm:w-36"
      />
    </template>

    <template #filterActions>
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </RButton>
      <RButton variant="outline" @click="handleReset">重置</RButton>
      <RButton v-if="selectedRows.length" variant="secondary" @click="handleBatchAdvance">
        批量推进 ({{ selectedRows.length }})
      </RButton>
    </template>

    <RTable v-if="candidateList.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-10" />
          <RTableTh class="w-[110px]">姓名</RTableTh>
          <RTableTh class="min-w-[150px]">公司</RTableTh>
          <RTableTh class="min-w-[130px]">职位</RTableTh>
          <RTableTh class="w-[65px] text-center">年限</RTableTh>
          <RTableTh class="w-[80px] text-center">来源</RTableTh>
          <RTableTh v-if="queryParams.jobId" class="w-[100px] text-center">进展</RTableTh>
          <RTableTh v-else class="w-[90px] text-center">状态</RTableTh>
          <RTableTh v-if="queryParams.jobId" class="min-w-[180px]">匹配</RTableTh>
          <!-- AI 意向列 (仅选职位时显示) -->
          <RTableTh v-if="queryParams.jobId" class="w-[105px] text-center">
            <span class="flex items-center justify-center gap-1 whitespace-nowrap">
              <Sparkles class="h-3 w-3 text-primary shrink-0" />AI 意向
            </span>
          </RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow
          v-for="row in candidateList"
          :key="row.id"
          :class="rowClassName({ row })"
        >
          <RTableCell>
            <RCheckbox
              :model-value="selectedIds.includes(row.id)"
              @update:model-value="(v) => toggleSelect(row.id, v)"
            />
          </RTableCell>
          <RTableCell>
            <button type="button" class="font-medium text-primary hover:underline" @click="handleView(row)">
              {{ row.name }}
            </button>
          </RTableCell>
          <RTableCell>{{ row.currentCompany || row.company || '—' }}</RTableCell>
          <RTableCell>{{ row.currentTitle || row.position || '—' }}</RTableCell>
          <RTableCell class="text-center">{{ row.workYears ?? '—' }}</RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(getSourceType(row.source))">{{ getSourceLabel(row.source) }}</RBadge>
          </RTableCell>
          <RTableCell v-if="queryParams.jobId" class="text-center">
            <RBadge :variant="elTagTypeToBadge(stageTagType(row.pipelineStage))">
              {{ pipelineStageLabel(row.pipelineStage) }}
            </RBadge>
          </RTableCell>
          <RTableCell v-else class="text-center">
            <RBadge :variant="elTagTypeToBadge(getStatusType(row.status))">
              {{ candidateStatusLabel(row.status) }}
            </RBadge>
          </RTableCell>
          <RTableCell v-if="queryParams.jobId">
            <MatchVerdict
              v-if="row.matchDetail"
              :match-score="row.matchScore"
              :match-detail="row.matchDetail"
              mode="compact"
              :show-score="false"
            />
            <span v-else class="text-sm text-muted-foreground">待评估</span>
          </RTableCell>
          <!-- AI 意向评分 -->
          <RTableCell v-if="queryParams.jobId" class="text-center">
            <div v-if="aiScores[row.id]?.loading" class="flex justify-center">
              <Loader2 class="h-4 w-4 animate-spin text-text-placeholder" />
            </div>
            <div
              v-else-if="aiScores[row.id]?.intent"
              class="flex items-center justify-center gap-1.5 cursor-pointer hover:opacity-80 transition-opacity"
              @click="openAiIntent(row)"
              :title="`意向 ${aiScores[row.id]!.intent!.intentScore?.toFixed(0)} 分 · 置信度 ${((aiScores[row.id]!.intent!.confidence || 0) * 100).toFixed(0)}%`"
            >
              <span class="w-2 h-2 rounded-full shrink-0" :class="aiDotClass(aiScores[row.id]!.intent!.intentScore)" />
              <span class="text-[13px] font-semibold" :class="aiScoreClass(aiScores[row.id]!.intent!.intentScore)">
                {{ aiScores[row.id]!.intent!.intentScore?.toFixed(0) }}
              </span>
            </div>
            <span v-else class="text-[11px] text-text-placeholder">—</span>
          </RTableCell>
          <RTableCell class="text-center">
            <RowActions
              :actions="getVisibleActions(row)"
              @action="(cmd: string) => handleRowCommand(cmd, row)"
            />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="onPageChange"
    />

    <template #below>
      <RDialog v-model:open="formVisible">
        <DialogContent class="max-w-lg">
          <DialogHeader>
            <DialogTitle>{{ isEditing ? '编辑候选人' : '添加候选人' }}</DialogTitle>
          </DialogHeader>
          <div class="grid gap-4 py-2">
            <FormField label="姓名" required :error="formErrors.name">
              <RInput v-model="candidateForm.name" placeholder="请输入姓名" />
            </FormField>
            <FormField label="电话" required :error="formErrors.phone">
              <RInput v-model="candidateForm.phone" placeholder="请输入手机号" />
            </FormField>
            <FormField label="邮箱">
              <RInput v-model="candidateForm.email" placeholder="选填" />
            </FormField>
            <FormField label="当前公司">
              <RInput v-model="candidateForm.currentCompany" />
            </FormField>
            <FormField label="当前职位">
              <RInput v-model="candidateForm.currentTitle" />
            </FormField>
            <FormField label="工作年限">
              <NumberInput v-model="candidateForm.workYears" :min="0" :max="40" />
            </FormField>
            <FormField label="学历">
              <RSelect v-model="candidateForm.education" :options="educationOptions" placeholder="请选择" class="w-full" />
            </FormField>
            <FormField label="期望薪资(K)">
              <NumberInput v-model="candidateForm.expectedSalary" :min="0" :step="1" />
            </FormField>
            <FormField label="来源">
              <RSelect v-model="candidateForm.source" :options="sourceOptions" class="w-full" />
            </FormField>
          </div>
          <DialogFooter>
            <RButton variant="outline" @click="formVisible = false">取消</RButton>
            <RButton :disabled="formLoading" @click="submitCandidateForm">保存</RButton>
          </DialogFooter>
        </DialogContent>
      </RDialog>

      <RDialog v-model:open="linkJobVisible">
        <DialogContent class="max-w-md">
          <DialogHeader>
            <DialogTitle>关联在招职位</DialogTitle>
          </DialogHeader>
          <div class="grid gap-4 py-2">
            <FormField label="候选人">
              <RInput :model-value="currentCandidate?.name" disabled />
            </FormField>
            <FormField label="选择在招职位">
              <RSelect
                v-model="selectedJobId"
                :options="jobSelectOptions"
                placeholder="请选择要关联的在招职位"
                class="w-full"
              />
            </FormField>
          </div>
          <DialogFooter>
            <RButton variant="outline" @click="linkJobVisible = false">取消</RButton>
            <RButton :disabled="linkJobLoading" @click="confirmLinkJob">确定关联</RButton>
          </DialogFooter>
        </DialogContent>
      </RDialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Plus, Sparkles, Loader2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import { getCandidateIntent, batchGetIntent, type CandidateIntent } from '@/api/modules/brain'
import PageShell from '@/components/Layout/PageShell.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import FormField from '@/components/app/FormField.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import JobContextBar from '@/components/common/JobContextBar.vue'
import RowActions from '@/components/common/RowActions.vue'
import type { ActionItem } from '@/components/common/RowActions.vue'
import {
  RButton, RInput, RSelect, RBadge,
  RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell, RCheckbox,
  RDialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui'
import { candidateStatusLabel, pipelineStageLabel, sourceLabel } from '@/constants/businessLabels'
import { getCandidateList, addToJob, createCandidate, updateCandidate } from '@/api/modules/candidate'
import { getJobList } from '@/api/modules/job'

const route = useRoute()
const router = useRouter()

const queryParams = reactive({ name: '', status: '' as string | undefined, source: '' as string | undefined, jobId: null as number | null, pageNum: 1, pageSize: 20 })
const total = ref(0)
const candidateList = ref<any[]>([])
const selectedIds = ref<number[]>([])
const selectedRows = computed(() => candidateList.value.filter(r => selectedIds.value.includes(r.id)))
const linkJobVisible = ref(false); const linkJobLoading = ref(false)
const currentCandidate = ref<any>(null); const selectedJobId = ref<number | null>(null)
const jobOptions = ref<any[]>([])
const jobSelectOptions = computed(() => jobOptions.value.map(j => ({ label: j.title, value: j.id })))
const formVisible = ref(false); const formLoading = ref(false)
const isEditing = ref(false); const editingId = ref<number | null>(null)
const candidateForm = reactive({ name: '', phone: '', email: '', currentCompany: '', currentTitle: '', workYears: 0, education: '', expectedSalary: undefined as number | undefined | null, source: 'DIRECT' })
const formErrors = reactive({ name: '', phone: '' })

// ===== AI 意向评分 =====
const aiScores = ref<Record<number, { intent?: CandidateIntent | null; loading?: boolean }>>({})

function aiDotClass(score: number) {
  if (score >= 70) return 'bg-success shadow-[0_0_4px_rgba(22,163,74,0.4)]'
  if (score >= 40) return 'bg-warning shadow-[0_0_4px_rgba(245,158,11,0.4)]'
  return 'bg-danger shadow-[0_0_4px_rgba(239,68,68,0.4)]'
}
function aiScoreClass(score: number) {
  if (score >= 70) return 'text-success'
  if (score >= 40) return 'text-warning'
  return 'text-danger'
}
function openAiIntent(row: any) {
  const intent = aiScores.value[row.id]?.intent
  if (!intent || !queryParams.jobId) return
  router.push({
    path: `/ai/intent/${row.id}`,
    query: { jobId: String(queryParams.jobId), candidateName: row.name, jobTitle: '' },
  })
}
// =====

const statusOptions = [
  { label: '新简历', value: 'NEW' }, { label: '筛选中', value: 'SCREENING' }, { label: '面试中', value: 'INTERVIEWING' },
  { label: '已发Offer', value: 'OFFER' }, { label: '已入职', value: 'ONBOARD' }, { label: '人才库', value: 'POOL' }, { label: '黑名单', value: 'BLACKLIST' },
]
const sourceOptions = [
  { label: '平台', value: 'PLATFORM' }, { label: '内推', value: 'REFERRAL' }, { label: '猎头', value: 'HEADHUNTER' },
  { label: '直招', value: 'DIRECT' }, { label: '门户', value: 'PORTAL' },
]
const educationOptions = [
  { label: '大专', value: '大专' }, { label: '本科', value: '本科' }, { label: '硕士', value: '硕士' }, { label: '博士', value: '博士' },
]

function getStatusType(s: string) { return ({ NEW: 'info', SCREENING: 'warning', INTERVIEWING: '', OFFER: 'success', ONBOARD: 'success', POOL: 'info', BLACKLIST: 'danger' } as Record<string, string>)[s] || 'info' }
function stageTagType(s?: string) { return ({ SOURCED: 'info', SCREENING: 'warning', CONTACTED: '', INTERVIEWING: '', EVALUATED: 'success', OFFER: 'success', HIRED: 'success', ARCHIVED: 'info' } as Record<string, string>)[s || ''] || 'info' }
function getSourceType(s: string) { return ({ PLATFORM: 'primary', REFERRAL: 'success', HEADHUNTER: 'warning', DIRECT: 'info', PORTAL: 'danger' } as Record<string, string>)[s] || 'info' }
function getSourceLabel(s: string) { return sourceLabel(s) }
function rowClassName({ row }: { row: any }) {
  const s = (row.pipelineStage || row.status || '').toLowerCase()
  if (s === 'new') return 'status-bar--new'
  if (s === 'screening') return 'status-bar--screening'
  if (s === 'contacted') return 'status-bar--contacted'
  if (s === 'interviewing') return 'status-bar--interviewing'
  if (s === 'evaluated') return 'status-bar--evaluated'
  if (s === 'offer') return 'status-bar--offer'
  if (s === 'hired' || s === 'onboard') return 'status-bar--hired'
  if (s === 'archived' || s === 'pool') return 'status-bar--archived'
  if (s === 'blacklist') return 'status-bar--blacklist'
  return ''
}

function toggleSelect(id: number, checked: boolean) {
  if (checked) { if (!selectedIds.value.includes(id)) selectedIds.value.push(id) }
  else { selectedIds.value = selectedIds.value.filter(i => i !== id) }
}

function getVisibleActions(_row: any) {
  const base: ActionItem[] = [
    { command: 'view', label: '查看详情', icon: 'View', primary: true },
    { command: 'edit', label: '编辑', icon: 'Edit' },
  ]
  if (!queryParams.jobId) base.push({ command: 'link', label: '关联职位', icon: 'Connection' })
  if (queryParams.jobId) base.push({ command: 'ai-intent', label: 'AI 评估', icon: 'Sparkles' })
  base.push({ command: 'match', label: '查看匹配', icon: 'DataAnalysis' })
  return base
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') handleView(row)
  else if (cmd === 'edit') handleEdit(row)
  else if (cmd === 'link') handleLinkJob(row)
  else if (cmd === 'ai-intent') openAiIntent(row)
  else if (cmd === 'match') handleScreening(row)
}

function validateForm(): boolean { formErrors.name = candidateForm.name ? '' : '请输入姓名'; formErrors.phone = candidateForm.phone ? '' : '请输入电话'; return !Object.values(formErrors).some(Boolean) }

async function loadData() {
  try { const res: any = await getCandidateList(queryParams); candidateList.value = res.data?.list || res.data?.records || []; total.value = res.data?.total || 0 } catch { candidateList.value = []; total.value = 0 }
}
async function handleSearch() { queryParams.pageNum = 1; await loadData(); if (queryParams.jobId) loadAiScores() }
function handleReset() { queryParams.name = ''; queryParams.status = undefined; queryParams.source = undefined; queryParams.jobId = null; aiScores.value = {}; handleSearch() }
async function onJobChange() { await handleSearch() }
async function onPageChange() { await loadData(); if (queryParams.jobId) loadAiScores() }
function resetCandidateForm() { Object.assign(candidateForm, { name: '', phone: '', email: '', currentCompany: '', currentTitle: '', workYears: 0, education: '', expectedSalary: undefined, source: 'DIRECT' }); formErrors.name = ''; formErrors.phone = '' }
function handleCreate() { isEditing.value = false; editingId.value = null; resetCandidateForm(); formVisible.value = true }
function handleView(row: any) { router.push({ path: `/pipeline/candidates/${row.id}`, query: queryParams.jobId ? { jobId: String(queryParams.jobId) } : {} }) }
function handleEdit(row: any) {
  isEditing.value = true; editingId.value = row.id
  Object.assign(candidateForm, { name: row.name || '', phone: row.phone || '', email: row.email || '', currentCompany: row.currentCompany || row.company || '', currentTitle: row.currentTitle || row.position || '', workYears: row.workYears || 0, education: row.education || '', expectedSalary: row.expectedSalary, source: row.source || 'DIRECT' })
  formVisible.value = true
}
async function submitCandidateForm() {
  if (!validateForm()) return; formLoading.value = true
  try {
    if (isEditing.value && editingId.value) { await updateCandidate(editingId.value, { ...candidateForm }); toast.success('候选人已更新') }
    else { await createCandidate({ ...candidateForm }); toast.success('候选人已添加') }
    formVisible.value = false; handleSearch()
  } catch { toast.error('保存失败') } finally { formLoading.value = false }
}
async function handleLinkJob(row: any) { currentCandidate.value = row; selectedJobId.value = null; linkJobVisible.value = true; try { const res: any = await getJobList({ pageNum: 1, pageSize: 100 }); jobOptions.value = res.data?.list || res.data?.records || [] } catch { jobOptions.value = [] } }
async function confirmLinkJob() { if (!selectedJobId.value || !currentCandidate.value) return; linkJobLoading.value = true; try { await addToJob(currentCandidate.value.id, selectedJobId.value); toast.success('已关联在招职位'); linkJobVisible.value = false; handleSearch() } catch {} finally { linkJobLoading.value = false } }

// ===== AI 意向批量加载 =====
async function loadAiScores() {
  if (!queryParams.jobId || candidateList.value.length === 0) return
  const jobId = queryParams.jobId
  const ids = candidateList.value.map(r => r.id).filter(Boolean) as number[]
  if (ids.length === 0) return
  // Mark all as loading
  ids.forEach(id => { aiScores.value[id] = { loading: true } })
  try {
    const res: any = await batchGetIntent(ids, jobId)
    const results = res?.data?.results || (res as any)?.results || {}
    ids.forEach(id => {
      const entry = results[String(id)]
      if (entry) {
        aiScores.value[id] = {
          intent: {
            candidateId: id,
            candidateName: '',
            jobId,
            jobTitle: '',
            intentScore: entry.intentScore,
            intentLevel: entry.intentLevel,
            confidence: entry.confidence,
            riskFactors: entry.riskFactors || [],
            interventionSuggestions: entry.interventionSuggestions || [],
            updatedAt: entry.updatedAt || '',
          },
          loading: false,
        }
      } else {
        aiScores.value[id] = { intent: null, loading: false }
      }
    })
  } catch {
    ids.forEach(id => { aiScores.value[id] = { intent: null, loading: false } })
  }
}
// =====

function handleScreening(row: any) {
  const jobId = queryParams.jobId || row.jobId
  if (jobId) { router.push({ path: '/pipeline/decision', query: { candidateId: String(row.id), jobId: String(jobId) } }); return }
  currentCandidate.value = row; selectedJobId.value = null; linkJobVisible.value = true
  if (!jobOptions.value.length) getJobList({ pageNum: 1, pageSize: 100 }).then((res: any) => { jobOptions.value = res.data?.list || res.data?.records || [] })
  toast.info('请先选择在招职位')
}
function handleBatchAdvance() { toast.info(`批量推进 ${selectedRows.value.length} 位候选人（功能开发中）`) }

onMounted(async () => {
  const qJob = Number(route.query.jobId); if (qJob) queryParams.jobId = qJob
  try { const res: any = await getJobList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' }); jobOptions.value = res.data?.list || res.data?.records || [] } catch {}
  await loadData()
  if (queryParams.jobId) loadAiScores()
})
</script>
