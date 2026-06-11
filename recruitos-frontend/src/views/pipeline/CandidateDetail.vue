<template>
  <PageShell>
<div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

    <div class="ws-breadcrumb">
      <RButton variant="ghost" @click="$router.back()">
        <ArrowLeft class="mr-2 h-4 w-4" />返回
      </RButton>
    </div>

    <ObjectHeader
      v-if="candidate"
      :name="candidate.name"
      :avatar-text="candidate.name"
      :meta="`${candidate.currentCompany || '—'} · ${candidate.currentTitle || '—'}`"
    />

    <div v-if="candidate" class="grid grid-cols-1 lg:grid-cols-4 gap-4">
      <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6 timeline-card lg:col-span-1">
        <h4>时间线</h4>
        <div class="timeline-list">
          <div v-for="log in timeline" :key="log.id" class="timeline-item">
            <div class="timeline-time">{{ log.createdAt }}</div>
            <div>{{ log.fromStage }} → {{ log.toStage }}</div>
          </div>
        </div>
      </div>

      <div class="lg:col-span-3">
        <RTabs v-model="activeTab">
          <RTabsList>
            <RTabsTrigger value="overview">概览</RTabsTrigger>
            <RTabsTrigger value="jobs">在招职位</RTabsTrigger>
            <RTabsTrigger value="decision">决策</RTabsTrigger>
          </RTabsList>

          <RTabsContent value="overview">
            <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6">
              <dl class="desc-grid">
                <div><dt>电话</dt><dd>{{ candidate.phone }}</dd></div>
                <div><dt>邮箱</dt><dd>{{ candidate.email }}</dd></div>
                <div><dt>性别</dt><dd>{{ candidate.gender === 'MALE' ? '男' : candidate.gender === 'FEMALE' ? '女' : '-' }}</dd></div>
                <div><dt>工作年限</dt><dd>{{ candidate.workYears ? candidate.workYears + ' 年' : '-' }}</dd></div>
                <div><dt>学历</dt><dd>{{ formatEducation(candidate.education) }}</dd></div>
                <div><dt>毕业院校</dt><dd>{{ candidate.school || '-' }}</dd></div>
                <div><dt>专业</dt><dd>{{ candidate.major || '-' }}</dd></div>
                <div><dt>期望薪资</dt><dd>{{ formatSalary(candidate.expectedSalary) }}</dd></div>
                <div><dt>期望城市</dt><dd>{{ candidate.workLocation || '-' }}</dd></div>
                <div><dt>来源</dt><dd>{{ candidate.source }} {{ candidate.sourceDetail ? '· ' + candidate.sourceDetail : '' }}</dd></div>
                <div><dt>状态</dt><dd>{{ candidate.status }}</dd></div>
                <div class="col-span-2"><dt>备注</dt><dd>{{ candidate.remark || '-' }}</dd></div>
              </dl>
              <div v-if="candidate.resumeId" class="mt-3">
                <RButton variant="link" @click="$router.push(`/talent/resumes/${candidate.resumeId}`)">查看完整简历</RButton>
              </div>
            </div>
          </RTabsContent>

          <RTabsContent value="jobs">
            <div v-for="job in jobs" :key="job.id" class="rounded-xl bg-card text-card-foreground shadow-soft p-6 job-card">
              <div class="job-row">
                <span>职位 #{{ job.jobId }} · {{ pipelineStageLabel(job.pipelineStage || 'SOURCED') }}</span>
                <MatchVerdict :match-score="job.matchScore" :match-detail="job.matchDetail" mode="compact" :show-score="false" />
              </div>
              <div class="job-actions">
                <RButton size="sm" variant="outline" @click="advanceJob(job, 'SCREENING')">进入初筛</RButton>
                <RButton size="sm" variant="outline" @click="advanceJob(job, 'INTERVIEWING')">安排面试</RButton>
                <RButton size="sm" @click="openMatchAnalysis(job)">查看匹配评估</RButton>
                <RButton size="sm" variant="outline" @click="openDecision(job)">录用意向</RButton>
                <RButton size="sm" variant="destructive" @click="archiveJob(job)">淘汰归档</RButton>
              </div>
            </div>
          </RTabsContent>

          <RTabsContent value="decision">
            <div class="rounded-xl bg-card text-card-foreground shadow-soft p-6 max-w-lg grid gap-4">
              <FormField label="决策">
                <RSelect v-model="decisionForm.decision" :options="decisionOptions" />
              </FormField>
              <FormField label="说明">
                <RTextarea v-model="decisionForm.summary" :rows="3" />
              </FormField>
              <RButton @click="submitDecision">保存决策</RButton>
            </div>
          </RTabsContent>
        </RTabs>
      </div>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import ObjectHeader from '@/components/Layout/ObjectHeader.vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2 } from 'lucide-vue-next'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import FormField from '@/components/app/FormField.vue'
import { pipelineStageLabel } from '@/constants/businessLabels'
import { toast } from '@/lib/notify'
import { prompt } from '@/lib/prompt'
import { RButton, RTabs, RTabsList, RTabsTrigger, RTabsContent, RSelect, RTextarea } from '@/components/ui'
import { getCandidate360, advancePipelineStage, saveHiringDecision } from '@/api/modules/pipeline'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('overview')
const candidate = ref<any>(null)
const jobs = ref<any[]>([])
const timeline = ref<any[]>([])
const decisionForm = ref({ decision: 'HIRE', summary: '', candidateJobId: 0, candidateId: 0, jobId: 0 })

const decisionOptions = [
  { label: '录用', value: 'HIRE' },
  { label: '待定', value: 'HOLD' },
  { label: '不录用', value: 'REJECT' },
]

async function load() {
  loading.value = true
  try {
    const { data } = await getCandidate360(Number(route.params.id))
    candidate.value = data.candidate
    jobs.value = data.jobs || []
    timeline.value = data.timeline || []
    if (jobs.value[0]) {
      decisionForm.value.candidateJobId = jobs.value[0].id
      decisionForm.value.candidateId = jobs.value[0].candidateId
      decisionForm.value.jobId = jobs.value[0].jobId
    }
  } finally { loading.value = false }
}

async function advanceJob(job: any, stage: string) { await advancePipelineStage(job.id, { toStage: stage }); toast.success('已推进'); load() }
function openMatchAnalysis(job: any) { router.push({ path: '/pipeline/decision', query: { candidateId: String(job.candidateId), jobId: String(job.jobId) } }) }
function openDecision(job: any) { decisionForm.value.candidateJobId = job.id; decisionForm.value.candidateId = job.candidateId; decisionForm.value.jobId = job.jobId; activeTab.value = 'decision' }

async function archiveJob(job: any) {
  const value = await prompt({ title: '淘汰归档', message: '请输入淘汰说明', placeholder: '说明' })
  if (!value) return
  await advancePipelineStage(job.id, { toStage: 'ARCHIVED', reasonCode: 'SKILL_MISMATCH', comment: value, archivedToPool: true })
  toast.success('已归档')
  load()
}

async function submitDecision() { await saveHiringDecision({ ...decisionForm.value }); toast.success('决策已保存') }
function formatEducation(edu?: string) { if (!edu) return '-'; return ({ MASTER: '硕士', BACHELOR: '本科', PHD: '博士', COLLEGE: '大专' } as Record<string, string>)[edu.toUpperCase()] || edu }
function formatSalary(val?: number) { if (val == null) return '-'; return `${val >= 1000 ? Math.round(val / 1000) : val}K` }

onMounted(load)
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';
.ws-breadcrumb { margin-bottom: $spacing-sm; }
.timeline-card { h4 { margin-bottom: $spacing-md; } }
.timeline-list { display: flex; flex-direction: column; gap: 12px; }
.timeline-time { font-size: 12px; color: $text-secondary; }
.job-card { margin-bottom: $spacing-md; }
.job-row { display: flex; justify-content: space-between; margin-bottom: $spacing-sm; }
.job-actions { display: flex; gap: $spacing-sm; flex-wrap: wrap; }
.desc-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
  dt { font-size: 12px; color: $text-secondary; margin-bottom: 2px; }
  dd { margin: 0; color: $text-primary; }
  .col-span-2 { grid-column: 1 / -1; }
}
</style>
