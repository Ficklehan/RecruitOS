<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <div v-if="candidate">
        <h2 class="page-title">{{ candidate.name }}</h2>
        <p class="page-subtitle">{{ candidate.currentCompany }} · {{ candidate.currentTitle }}</p>
      </div>
    </div>

    <el-row :gutter="16" v-if="candidate">
      <el-col :span="6">
        <div class="data-card timeline-card">
          <h3>时间线</h3>
          <el-timeline>
            <el-timeline-item v-for="log in timeline" :key="log.id" :timestamp="log.createdAt">
              {{ log.fromStage }} → {{ log.toStage }}
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-col>
      <el-col :span="18">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="概览" name="overview">
            <div class="data-card">
              <p>电话：{{ candidate.phone }}</p>
              <p>邮箱：{{ candidate.email }}</p>
              <p>来源：{{ candidate.source }}</p>
              <p>状态：{{ candidate.status }}</p>
            </div>
          </el-tab-pane>
          <el-tab-pane label="在招职位" name="jobs">
            <div v-for="job in jobs" :key="job.id" class="data-card job-card">
              <div class="job-row">
                <span>职位 #{{ job.jobId }} · {{ pipelineStageLabel(job.pipelineStage || 'SOURCED') }}</span>
                <MatchVerdict
                  :match-score="job.matchScore"
                  :match-detail="job.matchDetail"
                  mode="compact"
                  :show-score="false"
                />
              </div>
              <div class="job-actions">
                <el-button size="small" @click="advanceJob(job, 'SCREENING')">进入初筛</el-button>
                <el-button size="small" @click="advanceJob(job, 'INTERVIEWING')">安排面试</el-button>
                <el-button size="small" type="primary" @click="openMatchAnalysis(job)">查看匹配评估</el-button>
                <el-button size="small" @click="openDecision(job)">录用意向</el-button>
                <el-button size="small" type="danger" @click="archiveJob(job)">淘汰归档</el-button>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="决策" name="decision">
            <div class="data-card">
              <el-form label-width="80px">
                <el-form-item label="决策">
                  <el-select v-model="decisionForm.decision" style="width: 200px">
                    <el-option label="录用" value="HIRE" />
                    <el-option label="待定" value="HOLD" />
                    <el-option label="不录用" value="REJECT" />
                  </el-select>
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="decisionForm.summary" type="textarea" rows="3" />
                </el-form-item>
                <el-button type="primary" @click="submitDecision">保存决策</el-button>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import { pipelineStageLabel } from '@/constants/businessLabels'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCandidate360, advancePipelineStage, saveHiringDecision } from '@/api/modules/pipeline'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('overview')
const candidate = ref<any>(null)
const jobs = ref<any[]>([])
const timeline = ref<any[]>([])
const decisionForm = ref({ decision: 'HIRE', summary: '', candidateJobId: 0, candidateId: 0, jobId: 0 })

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
  } finally {
    loading.value = false
  }
}

async function advanceJob(job: any, stage: string) {
  await advancePipelineStage(job.id, { toStage: stage })
  ElMessage.success('已推进')
  load()
}

function openMatchAnalysis(job: any) {
  router.push({
    path: '/pipeline/decision',
    query: { candidateId: String(job.candidateId), jobId: String(job.jobId) },
  })
}

function openDecision(job: any) {
  decisionForm.value.candidateJobId = job.id
  decisionForm.value.candidateId = job.candidateId
  decisionForm.value.jobId = job.jobId
  activeTab.value = 'decision'
}

async function archiveJob(job: any) {
  const { value } = await ElMessageBox.prompt('请输入淘汰说明', '淘汰归档', {
    inputPlaceholder: '说明',
  }).catch(() => ({ value: null }))
  if (!value) return
  await advancePipelineStage(job.id, {
    toStage: 'ARCHIVED',
    reasonCode: 'SKILL_MISMATCH',
    comment: value,
    archivedToPool: true,
  })
  ElMessage.success('已归档')
  load()
}

async function submitDecision() {
  await saveHiringDecision({ ...decisionForm.value })
  ElMessage.success('决策已保存')
}

onMounted(load)
</script>

<style scoped>
.timeline-card { padding: 16px; }
.job-card { margin-bottom: 12px; padding: 12px; }
.job-row { display: flex; justify-content: space-between; margin-bottom: 8px; }
.job-actions { display: flex; gap: 8px; flex-wrap: wrap; }
</style>
