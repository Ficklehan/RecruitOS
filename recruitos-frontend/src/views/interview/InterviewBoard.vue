<template>
  <PageShell title="面试看板" subtitle="拖拽管理面试流程，实时跟踪面试状态">
    <template #actions>
      <RButton variant="outline" @click="handleRefresh"><RefreshCw class="mr-2 h-4 w-4" />刷新</RButton>
      <RButton @click="handleArrange"><Plus class="mr-2 h-4 w-4" />安排面试</RButton>
    </template>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <StatCard label="待安排" :value="stats.pending" icon="Clock" color="#F59E0B" />
      <StatCard label="已安排" :value="stats.arranged" icon="Calendar" color="#4F6BED" />
      <StatCard label="进行中" :value="stats.inProgress" icon="VideoCamera" color="#16A34A" />
      <StatCard label="已完成" :value="stats.completed" icon="CircleCheck" color="#6B7280" />
    </div>

    <div class="round-tabs">
      <RTabs v-model="activeRound" @update:model-value="handleRoundChange">
        <RTabsList>
          <RTabsTrigger value="INITIAL">初面</RTabsTrigger>
          <RTabsTrigger value="SECOND">复试</RTabsTrigger>
        </RTabsList>
      </RTabs>
    </div>

    <div class="kanban-board -mx-6 px-6 w-[calc(100%+3rem)] max-w-none box-border">
      <div v-for="col in columns" :key="col.status" class="kanban-column">
        <div class="column-header">
          <div class="column-title-row">
            <span class="column-dot" :style="{ background: col.color }"></span>
            <span class="column-title">{{ col.label }}</span>
          </div>
          <span class="column-count">{{ getColumnData(col.status).length }}</span>
        </div>
        <div class="column-body flex-1 min-h-[120px] max-h-[calc(100vh-14rem)] overflow-y-auto p-2">
          <div
            v-for="item in getColumnData(col.status)"
            :key="item.id"
            class="interview-card"
            :style="{ '--card-accent': col.color }"
            @click="handleCardClick(item)"
          >
            <div class="card-accent-bar"></div>
            <div class="card-top">
              <span class="candidate-name">{{ item.candidateName }}</span>
              <span class="card-format" :class="item.format === 'ONLINE' ? 'format-online' : 'format-offline'">
                {{ item.format === 'ONLINE' ? '线上' : '线下' }}
              </span>
            </div>
            <div class="card-job">{{ item.jobTitle }}</div>
            <div class="card-info">
              <div class="info-row"><User class="h-3.5 w-3.5 text-muted-foreground" /><span>{{ item.interviewerName }}</span></div>
              <div class="info-row" v-if="item.scheduledTime"><Clock class="h-3.5 w-3.5 text-muted-foreground" /><span>{{ formatTime(item.scheduledTime) }}</span></div>
              <div class="info-row" v-if="item.location"><MapPin class="h-3.5 w-3.5 text-muted-foreground" /><span>{{ item.location }}</span></div>
            </div>
            <div class="card-actions" @click.stop>
              <RButton size="sm" variant="ghost" class="text-primary" @click="openCopilot(item)" title="AI 辅助">
                <Sparkles class="h-3.5 w-3.5" />
              </RButton>
              <template v-if="col.status === 'PENDING_ARRANGE'">
                <RButton size="sm" @click="handleArrangeItem(item)">安排</RButton>
              </template>
              <template v-else-if="col.status === 'ARRANGED'">
                <RButton size="sm" @click="handleStart(item)">开始</RButton>
                <RButton size="sm" variant="outline" @click="handleCancel(item)">取消</RButton>
              </template>
              <template v-else-if="col.status === 'IN_PROGRESS'">
                <RButton size="sm" @click="handleComplete(item)">完成</RButton>
              </template>
              <template v-else-if="col.status === 'COMPLETED'">
                <RButton size="sm" variant="outline" @click="handleEvaluate(item)">评价</RButton>
                <RButton size="sm" variant="outline" @click="handleNextRound(item)">下一轮</RButton>
              </template>
            </div>
          </div>
          <div v-if="getColumnData(col.status).length === 0" class="column-empty">
            <Calendar class="h-7 w-7 text-slate-300" />
            <span>暂无面试</span>
          </div>
        </div>
      </div>
    </div>

    <RDialog v-model:open="arrangeDialogVisible">
      <DialogContent class="max-w-lg">
        <DialogHeader><DialogTitle>安排面试</DialogTitle></DialogHeader>
        <div class="grid gap-4 py-2">
          <FormField label="候选人" required>
            <RInput v-model="candidateSearch" placeholder="输入候选人姓名搜索" @keyup.enter="searchCandidates(candidateSearch)" />
            <RSelect v-model="arrangeForm.candidateId" :options="candidateOptions.map(c => ({ label: c.name, value: c.id }))" placeholder="选择候选人" class="mt-2" />
          </FormField>
          <FormField label="岗位" required>
            <RSelect v-model="arrangeForm.jobId" :options="jobOptions.map(j => ({ label: j.title, value: j.id }))" placeholder="选择岗位" />
          </FormField>
          <FormField label="面试轮次" required>
            <RSelect v-model="arrangeForm.round" :options="[{ label: '初面', value: 'INITIAL' }, { label: '复试', value: 'SECOND' }]" />
          </FormField>
          <FormField label="面试官" required>
            <RSelect v-model="arrangeForm.interviewerId" :options="interviewerOptions.map(u => ({ label: u.name, value: u.id }))" placeholder="选择面试官" />
          </FormField>
          <FormField label="面试形式" required>
            <RadioGroup v-model="arrangeForm.format" class="flex gap-4">
              <label class="flex items-center gap-2 text-sm"><RadioGroupItem value="ONLINE" />线上</label>
              <label class="flex items-center gap-2 text-sm"><RadioGroupItem value="OFFLINE" />线下</label>
            </RadioGroup>
          </FormField>
          <FormField label="面试时间" required>
            <RInput v-model="arrangeForm.scheduledTime" type="datetime-local" />
          </FormField>
          <FormField label="面试地点">
            <RInput v-model="arrangeForm.location" placeholder="会议室 / 线上链接" />
          </FormField>
          <FormField label="备注">
            <RTextarea v-model="arrangeForm.remark" :rows="3" placeholder="面试备注" />
          </FormField>
        </div>
        <DialogFooter>
          <RButton variant="outline" @click="arrangeDialogVisible = false">取消</RButton>
          <RButton :disabled="submitting" @click="submitArrange">确认安排</RButton>
        </DialogFooter>
      </DialogContent>
    </RDialog>

    <RDialog v-model:open="cancelDialogVisible">
      <DialogContent class="max-w-md">
        <DialogHeader><DialogTitle>取消面试</DialogTitle></DialogHeader>
        <FormField label="取消原因" required>
          <RTextarea v-model="cancelReason" :rows="3" placeholder="请输入取消原因" />
        </FormField>
        <DialogFooter>
          <RButton variant="outline" @click="cancelDialogVisible = false">取消</RButton>
          <RButton variant="destructive" :disabled="submitting" @click="submitCancel">确认取消</RButton>
        </DialogFooter>
      </DialogContent>
    </RDialog>
  <InterviewAssistDrawer
    :open="copilotOpen"
    :prep="interviewPrep"
    :loading="prepLoading"
    @update:open="copilotOpen = $event"
  />
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, RefreshCw, User, Clock, MapPin, Calendar, Sparkles } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import FormField from '@/components/app/FormField.vue'
import StatCard from '@/components/StatCard.vue'
import InterviewAssistDrawer from '@/components/ai/InterviewAssistDrawer.vue'
import {
  RButton, RInput, RSelect, RTextarea, RTabs, RTabsList, RTabsTrigger,
  RDialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
  RadioGroup, RadioGroupItem,
} from '@/components/ui'
import { getInterviewPrep, type InterviewPrep } from '@/api/modules/brain'
import { getInterviewList, arrangeInterview, startInterview, completeInterview, cancelInterview, triggerNextRound } from '@/api/modules/interview'
import { getCandidateList } from '@/api/modules/candidate'
import { getJobList } from '@/api/modules/job'
import { getUserList } from '@/api/modules/user'

const router = useRouter()
const activeRound = ref('INITIAL')
const stats = reactive({ pending: 0, arranged: 0, inProgress: 0, completed: 0 })

const columns = [
  { status: 'PENDING_ARRANGE', label: '待安排', color: '#F59E0B' },
  { status: 'ARRANGED', label: '已安排', color: '#4F6BED' },
  { status: 'IN_PROGRESS', label: '进行中', color: '#16A34A' },
  { status: 'COMPLETED', label: '已完成', color: '#6B7280' },
]

const interviewList = ref<any[]>([])
function getColumnData(status: string) { return interviewList.value.filter((item) => item.status === status) }

const arrangeDialogVisible = ref(false)
const submitting = ref(false)
const copilotOpen = ref(false)
const interviewPrep = ref<InterviewPrep | null>(null)
const prepLoading = ref(false)
async function openCopilot(item: any) {
  copilotOpen.value = true
  interviewPrep.value = null
  if (item.status !== 'PENDING_ARRANGE') {
    prepLoading.value = true
    try {
      const res = await getInterviewPrep(item.id)
      interviewPrep.value = res.data
    } catch {
      interviewPrep.value = null
    } finally { prepLoading.value = false }
  }
}
const arrangeForm = reactive({ candidateId: null as number | null, jobId: null as number | null, round: 'INITIAL', interviewerId: null as number | null, format: 'ONLINE', scheduledTime: '', location: '', remark: '' })
const candidateSearch = ref('')
const candidateOptions = ref<any[]>([])
const jobOptions = ref<any[]>([])
const interviewerOptions = ref<any[]>([])
const cancelDialogVisible = ref(false)
const cancelReason = ref('')
const currentCancelItem = ref<any>(null)

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadData() {
  try {
    const res: any = await getInterviewList({ round: activeRound.value })
    const data = res.data || res
    const list = Array.isArray(data) ? data : data.records || []
    interviewList.value = list
    stats.pending = list.filter((i: any) => i.status === 'PENDING_ARRANGE').length
    stats.arranged = list.filter((i: any) => i.status === 'ARRANGED').length
    stats.inProgress = list.filter((i: any) => i.status === 'IN_PROGRESS').length
    stats.completed = list.filter((i: any) => i.status === 'COMPLETED').length
  } catch (err: any) { toast.error(err?.message || '加载面试列表失败') }
}

async function searchCandidates(query: string) {
  if (!query) { candidateOptions.value = []; return }
  try { const res: any = await getCandidateList({ keyword: query }); const data = res.data || res; candidateOptions.value = (Array.isArray(data) ? data : data.records || []).map((c: any) => ({ id: c.id, name: c.name })) } catch { candidateOptions.value = [] }
}

function handleRoundChange() { loadData() }
function handleRefresh() { loadData(); toast.success('刷新成功') }

async function handleArrange() {
  Object.assign(arrangeForm, { candidateId: null, jobId: null, round: activeRound.value, interviewerId: null, format: 'ONLINE', scheduledTime: '', location: '', remark: '' })
  try {
    const [jobRes, userRes] = await Promise.all([getJobList({}), getUserList()])
    jobOptions.value = (Array.isArray(jobRes.data) ? jobRes.data : jobRes.data?.records || []).map((j: any) => ({ id: j.id, title: j.title || j.name }))
    interviewerOptions.value = (Array.isArray(userRes.data) ? userRes.data : userRes.data?.records || []).map((u: any) => ({ id: u.id, name: u.name || u.username }))
  } catch {}
  arrangeDialogVisible.value = true
}

function handleArrangeItem(item: any) { handleArrange(); arrangeForm.round = item.round || activeRound.value }

async function submitArrange() {
  if (!arrangeForm.candidateId || !arrangeForm.jobId || !arrangeForm.interviewerId || !arrangeForm.scheduledTime) { toast.error('请填写必要信息'); return }
  submitting.value = true
  try { await arrangeInterview(arrangeForm); toast.success('安排成功'); arrangeDialogVisible.value = false; loadData() } catch (err: any) { toast.error(err?.message || '安排失败') } finally { submitting.value = false }
}

async function handleStart(item: any) { try { await startInterview(item.id); toast.success('面试已开始'); loadData() } catch (err: any) { toast.error(err?.message || '操作失败') } }
async function handleComplete(item: any) { try { await completeInterview(item.id); toast.success('面试已完成'); loadData() } catch (err: any) { toast.error(err?.message || '操作失败') } }
function handleCancel(item: any) { currentCancelItem.value = item; cancelReason.value = ''; cancelDialogVisible.value = true }

async function submitCancel() {
  if (!cancelReason.value.trim()) { toast.error('请输入取消原因'); return }
  submitting.value = true
  try { await cancelInterview(currentCancelItem.value.id, cancelReason.value); toast.success('面试已取消'); cancelDialogVisible.value = false; loadData() } catch (err: any) { toast.error(err?.message || '取消失败') } finally { submitting.value = false }
}

function handleEvaluate(item: any) { router.push({ path: '/interview/evaluation', query: { interviewId: item.id } }) }
async function handleNextRound(item: any) {
  const ok = await confirm({ title: '确认', message: '确定要触发下一轮面试吗？' })
  if (!ok) return
  await triggerNextRound(item.id)
  toast.success('已触发下一轮面试')
  loadData()
}
function handleCardClick(item: any) { toast.info(`查看面试详情：${item.candidateName}`) }

onMounted(() => { loadData() })
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.header-actions { display: flex; gap: $spacing-sm; }

.round-tabs { margin-bottom: $spacing-lg; }

.kanban-board {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 500px;
  @media (max-width: 1200px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: #{$bp-tablet}) { grid-template-columns: 1fr; }
}

.kanban-column {
  background: $bg-muted;
  border: none;
  border-radius: $border-radius;
  display: flex;
  flex-direction: column;
  min-height: 400px;
  min-width: 0;
}

.column-header {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.column-title-row { display: flex; align-items: center; gap: 8px; }
.column-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.column-title { font-size: 14px; font-weight: 600; color: $text-primary; }
.column-count { font-size: 12px; font-weight: 600; color: $text-secondary; background: $bg-card; padding: 1px 8px; border-radius: $border-radius-full; }

.column-body { padding: 0; flex: 1; display: flex; flex-direction: column; gap: 10px; min-height: 0; }

.column-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 120px;
  gap: 8px;
  color: $text-placeholder;
  font-size: 13px;
}

.interview-card {
  position: relative;
  background: $bg-card;
  border-radius: $border-radius-sm;
  padding: 14px;
  padding-top: 17px;
  cursor: pointer;
  transition: all $transition-base;
  overflow: hidden;

  &:hover { box-shadow: $shadow-card-hover; transform: translateY(-1px); }
}

.card-accent-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--card-accent, $border-color);
}

.card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.candidate-name { font-size: 14px; font-weight: 600; color: $text-primary; }

.card-format {
  font-size: 11px;
  font-weight: 500;
  padding: 1px 7px;
  border-radius: $border-radius-full;
  &.format-online { background: #DBEAFE; color: #1E40AF; }
  &.format-offline { background: $warning-light; color: #92400E; }
}

.card-job { font-size: 12px; color: $text-secondary; margin-bottom: 10px; }

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
  .info-row { display: flex; align-items: center; gap: 6px; font-size: 12px; color: $text-secondary; }
}

.card-actions {
  display: flex;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid $border-color-light;
}
</style>
