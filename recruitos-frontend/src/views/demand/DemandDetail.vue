<template>
  <PageShell :title="detail.title || '招聘需求详情'">
    <template #actions>
      <div class="header-actions flex flex-wrap gap-2">
        <RButton
          v-if="detail.status === 'DRAFT' || detail.status === 'REJECTED'"
          variant="outline"
          @click="handleEdit"
        >
          <Pencil class="mr-2 h-4 w-4" />
          编辑
        </RButton>
        <RButton
          v-if="detail.status === 'DRAFT' || detail.status === 'REJECTED'"
          @click="handleSubmit"
        >
          <Send class="mr-2 h-4 w-4" />
          提交审批
        </RButton>
        <RButton
          v-if="detail.status === 'APPROVED' || detail.status === 'JOB_CREATED'"
          class="bg-green-600 hover:bg-green-700"
          @click="handleCreateJob"
        >
          创建在招职位
        </RButton>
        <RButton
          v-if="detail.status !== 'CLOSED' && detail.status !== 'COMPLETED'"
          variant="destructive"
          @click="handleClose"
        >
          <X class="mr-2 h-4 w-4" />
          关闭需求
        </RButton>
      </div>
    </template>

    <div class="detail-grid">
      <div class="info-card">
        <h3 class="card-title">基本信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">需求编号</span>
            <span class="info-value">{{ detail.demandNo || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">所属部门</span>
            <span class="info-value">{{ detail.orgId || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">职级</span>
            <span class="info-value">{{ detail.jobLevel || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">招聘人数</span>
            <span class="info-value">{{ detail.headCount || '-' }} 人</span>
          </div>
          <div class="info-item">
            <span class="info-label">薪酬范围</span>
            <span class="info-value">{{ formatSalary(detail.salaryMin) }}K - {{ formatSalary(detail.salaryMax) }}K / 月</span>
          </div>
          <div class="info-item">
            <span class="info-label">紧急程度</span>
            <span class="info-value">
              <RBadge :variant="urgencyBadge(detail.urgency)">
                {{ getUrgencyLabel(detail.urgency) }}
              </RBadge>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">期望到岗日期</span>
            <span class="info-value">{{ detail.expectedOnboardDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">需求原因</span>
            <span class="info-value">{{ getReasonLabel(detail.reason) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">工作地点</span>
            <span class="info-value">
              <RBadge v-for="loc in parseLocations(detail.workLocations)" :key="loc" variant="secondary" class="mr-2">
                {{ loc }}
              </RBadge>
              <span v-if="!detail.workLocations">-</span>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">汇报对象</span>
            <span class="info-value">{{ detail.reportToName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建人</span>
            <span class="info-value">{{ detail.createdBy || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ detail.createdAt || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="info-card">
      <h3 class="card-title">职位描述</h3>
      <div class="card-content">
        <pre class="text-block">{{ detail.jobDuty || '暂无内容' }}</pre>
      </div>
    </div>

    <div class="info-card">
      <h3 class="card-title">任职要求</h3>
      <div class="card-content">
        <pre class="text-block">{{ detail.jobRequirement || '暂无内容' }}</pre>
      </div>
    </div>

    <div class="info-card">
      <h3 class="card-title">面试官配置</h3>
      <div class="interviewer-grid">
        <div class="interviewer-section">
          <h4 class="interviewer-label">初面面试官</h4>
          <div class="interviewer-list">
            <RBadge
              v-for="person in parseIds(detail.initialInterviewerIds)"
              :key="person"
              variant="outline"
              class="interviewer-tag"
            >
              <User class="h-3 w-3" />
              {{ person }}
            </RBadge>
            <span v-if="!detail.initialInterviewerIds" class="empty-text">暂未配置</span>
          </div>
        </div>
        <div class="interviewer-section">
          <h4 class="interviewer-label">复试面试官</h4>
          <div class="interviewer-list">
            <RBadge
              v-for="person in parseIds(detail.finalInterviewerIds)"
              :key="person"
              variant="outline"
              class="interviewer-tag"
            >
              <User class="h-3 w-3" />
              {{ person }}
            </RBadge>
            <span v-if="!detail.finalInterviewerIds" class="empty-text">暂未配置</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="linkedJobs.length" class="info-card">
      <h3 class="card-title">关联职位招聘进展</h3>
      <p class="section-hint">部门负责人可在此查看各在招职位的候选人漏斗</p>
      <div v-for="job in linkedJobs" :key="job.id" class="job-funnel-block">
        <div class="job-funnel-head">
          <router-link :to="`/planning/jobs/${job.id}`" class="job-link">{{ job.title }}</router-link>
          <RBadge :variant="job.status === 'ACTIVE' ? 'default' : 'secondary'">
            {{ jobStatusLabel(job.status) }}
          </RBadge>
        </div>
        <div v-if="jobFunnels[job.id]" class="funnel-row">
          <div v-for="cell in jobFunnels[job.id]" :key="cell.stage" class="funnel-cell">
            <span class="funnel-num">{{ cell.count }}</span>
            <span class="funnel-label">{{ cell.label }}</span>
          </div>
        </div>
        <div v-else class="empty-text">加载中…</div>
      </div>
    </div>

    <div class="info-card">
      <h3 class="card-title">审批记录</h3>
      <div v-if="approvalRecords.length > 0" class="approval-timeline">
        <div
          v-for="(record, index) in approvalRecords"
          :key="index"
          class="timeline-item"
        >
          <div class="timeline-marker" :class="getApprovalTimelineClass(record.action)" />
          <div class="timeline-content">
            <div class="timeline-header">
              <span class="timeline-node">{{ record.nodeName }}</span>
              <RBadge :variant="elTagTypeToBadge(getApprovalTagType(record.action))">
                {{ getApprovalLabel(record.action) }}
              </RBadge>
              <span class="timeline-time">{{ record.time }}</span>
            </div>
            <div class="timeline-info">
              <span class="timeline-approver">
                <User class="h-3.5 w-3.5" />
                {{ record.approver }}
              </span>
            </div>
            <div v-if="record.comment" class="timeline-comment">
              <MessageSquare class="h-3.5 w-3.5 shrink-0 mt-0.5" />
              {{ record.comment }}
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-text">暂无审批记录</div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Pencil, Send, X, User, MessageSquare } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { demandStatusBadge, urgencyBadge, elTagTypeToBadge } from '@/lib/badgeVariants'
import { RButton, RBadge } from '@/components/ui'
import { demandStatusLabel, jobStatusLabel, pipelineStageLabel } from '@/constants/businessLabels'
import { getDemandDetail, submitDemand, closeDemand } from '@/api/modules/demand'
import { getJobList } from '@/api/modules/job'
import { getPipelineBoard } from '@/api/modules/pipeline'

const router = useRouter()
const route = useRoute()
const demandId = Number(route.params.id)

const detail = ref<any>({})
const approvalRecords = ref<any[]>([])
const linkedJobs = ref<any[]>([])
const jobFunnels = ref<Record<number, { stage: string; label: string; count: number }[]>>({})

function formatSalary(val: any): string {
  if (val == null) return '-'
  const num = Number(val)
  return isNaN(num) ? String(val) : num.toFixed(0)
}

function parseIds(idsStr: string | null): string[] {
  if (!idsStr) return []
  try {
    const parsed = JSON.parse(idsStr)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return idsStr.split(',').map(s => s.trim()).filter(Boolean)
  }
}

function parseLocations(locStr: string | null): string[] {
  if (!locStr) return []
  try {
    const parsed = JSON.parse(locStr)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return [locStr]
  }
}

function getStatusLabel(status: string) {
  return demandStatusLabel(status)
}

function handleCreateJob() {
  router.push({
    path: '/planning/jobs/create',
    query: { demandNo: detail.value.demandNo || '', demandId: String(demandId) },
  })
}

function getUrgencyLabel(level: string) {
  const map: Record<string, string> = { NORMAL: '普通', URGENT: '紧急', CRITICAL: '特急' }
  return map[level] || level
}

function getReasonLabel(reason: string) {
  const map: Record<string, string> = {
    NEW: '新增编制', REPLACEMENT: '离职替补',
    EXPANSION: '业务扩张', TEMPORARY: '项目临时用工',
  }
  return map[reason] || reason || '-'
}

function getApprovalTimelineClass(action: string) {
  const map: Record<string, string> = { approve: 'is-success', reject: 'is-danger', submit: 'is-primary' }
  return map[action] || 'is-primary'
}

function getApprovalTagType(action: string) {
  const map: Record<string, string> = { approve: 'success', reject: 'danger', submit: 'info' }
  return map[action] || 'info'
}

function getApprovalLabel(action: string) {
  const map: Record<string, string> = { approve: '通过', reject: '驳回', submit: '提交' }
  return map[action] || action
}

async function loadLinkedJobs() {
  const demandNo = detail.value?.demandNo
  if (!demandNo) {
    linkedJobs.value = []
    jobFunnels.value = {}
    return
  }
  try {
    const res: any = await getJobList({ demandNo, pageNum: 1, pageSize: 20 })
    linkedJobs.value = res.data?.list || res.data?.records || []
    const funnels: typeof jobFunnels.value = {}
    await Promise.all(
      linkedJobs.value.map(async (job: any) => {
        try {
          const boardRes: any = await getPipelineBoard(job.id)
          const columns = boardRes.data?.columns || []
          funnels[job.id] = columns
            .filter((c: any) => (c.items?.length || 0) > 0)
            .map((c: any) => ({
              stage: c.stage,
              label: pipelineStageLabel(c.stage, 'column'),
              count: c.items?.length || 0,
            }))
        } catch {
          funnels[job.id] = []
        }
      }),
    )
    jobFunnels.value = funnels
  } catch {
    linkedJobs.value = []
    jobFunnels.value = {}
  }
}

async function loadDetail() {
  if (!demandId || isNaN(demandId)) return
  try {
    const res: any = await getDemandDetail(demandId)
    detail.value = res.data || {}
    approvalRecords.value = res.data?.approvalRecords || []
    await loadLinkedJobs()
  } catch {
    detail.value = {}
    approvalRecords.value = []
    linkedJobs.value = []
    jobFunnels.value = {}
  }
}

function handleEdit() {
  router.push(`/planning/demands/create?id=${demandId}`)
}

async function handleSubmit() {
  const ok = await confirm({
    title: '提交确认',
    message: '确定要提交该需求进行审批吗？',
    confirmText: '确定',
  })
  if (!ok) return
  await submitDemand(demandId)
  toast.success('已提交审批')
  loadDetail()
}

async function handleClose() {
  const ok = await confirm({
    title: '关闭确认',
    message: '确定要关闭该需求吗？关闭后不可恢复。',
    confirmText: '确定关闭',
    destructive: true,
  })
  if (!ok) return
  await closeDemand(demandId)
  toast.success('需求已关闭')
  loadDetail()
}

function goBack() {
  router.push('/planning/demands')
}

onMounted(() => {
  loadDetail()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.info-card {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 16px;
  transition: box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
  }
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-color-light;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 13px;
  color: $text-secondary;
}

.info-value {
  font-size: 14px;
  color: $text-primary;
  font-weight: 500;
}

.text-block {
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  color: $text-regular;
  line-height: 1.8;
  margin: 0;
}

.interviewer-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.interviewer-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-regular;
  margin-bottom: 12px;
}

.interviewer-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.interviewer-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.section-hint {
  font-size: 13px;
  color: $text-secondary;
  margin: -12px 0 16px;
}

.job-funnel-block {
  padding: 12px 0;
  border-bottom: 1px solid $border-color-light;
  &:last-child { border-bottom: none; }
}

.job-funnel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.job-link {
  font-weight: 600;
  color: $primary-color;
  text-decoration: none;
  &:hover { text-decoration: underline; }
}

.funnel-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.funnel-cell {
  min-width: 72px;
  padding: 8px 12px;
  background: $bg-warm;
  border-radius: 8px;
  text-align: center;
}

.funnel-num {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
}

.funnel-label {
  font-size: 12px;
  color: $text-secondary;
}

.empty-text {
  font-size: 14px;
  color: $text-placeholder;
}

.approval-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding-left: 8px;
}

.timeline-item {
  display: flex;
  gap: 16px;
  padding-bottom: 20px;
  position: relative;

  &:not(:last-child)::before {
    content: '';
    position: absolute;
    left: 5px;
    top: 14px;
    bottom: 0;
    width: 2px;
    background: $border-color-light;
  }
}

.timeline-marker {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-top: 4px;
  flex-shrink: 0;
  background: $border-color;
  border: 2px solid $bg-card;

  &.is-success { background: $success-color; }
  &.is-danger { background: var(--r-danger); }
  &.is-primary { background: $primary-color; }
}

.timeline-content {
  flex: 1;
  padding-bottom: 4px;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.timeline-node {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.timeline-time {
  font-size: 12px;
  color: $text-secondary;
  margin-left: auto;
}

.timeline-info {
  margin-bottom: 4px;
}

.timeline-approver {
  font-size: 13px;
  color: $text-regular;
  display: flex;
  align-items: center;
  gap: 4px;
}

.timeline-comment {
  font-size: 13px;
  color: $text-secondary;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 4px;
  padding: 8px 12px;
  background: $bg-muted;
  border-radius: 4px;
}

@media (max-width: 1024px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .interviewer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
