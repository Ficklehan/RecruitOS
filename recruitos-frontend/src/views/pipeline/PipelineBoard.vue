<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">招聘进展</h2>
        <p class="page-subtitle">按在招职位查看候选人当前进展</p>
      </div>
    </div>

    <JobContextBar v-model="selectedJobId" @update:model-value="loadBoard">
      <el-button v-if="selectedJobId" type="primary" link @click="goSourcing">
        渠道招聘
      </el-button>
    </JobContextBar>

    <EmptyStateCta
      v-if="!selectedJobId"
      title="还没有选定职位"
      description="请先在上方选择在招职位，再查看候选人招聘进展"
      :actions="[
        { label: '查看在招职位', type: 'primary', onClick: () => router.push('/planning/jobs') },
      ]"
    />

    <div v-else class="kanban" v-loading="loading">
      <div v-for="col in board.columns" :key="col.stage" class="kanban-col">
        <div class="col-header">
          <span>{{ col.label }}</span>
          <el-tag size="small" type="info">{{ col.items?.length || 0 }}</el-tag>
        </div>
        <div class="col-body">
          <div
            v-for="item in col.items"
            :key="item.id"
            class="kanban-card"
            @click="openCandidate(item)"
          >
            <div class="card-name">{{ item.candidateName }}</div>
            <div class="card-meta">{{ item.candidateCompany }}</div>
            <div class="card-match">
              <MatchVerdict
                :match-score="item.matchScore"
                :match-detail="item.matchDetail"
                mode="compact"
                :show-score="false"
              />
            </div>
            <div class="card-actions" @click.stop>
              <el-button
                v-if="col.stage === 'SOURCED'"
                size="small"
                type="primary"
                @click="advance(item, 'SCREENING')"
              >
                通过初筛
              </el-button>
              <el-dropdown v-else trigger="click" @command="(stage: string) => advance(item, stage)">
                <el-button size="small" link type="primary">推进阶段</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="s in nextStages(col.stage)" :key="s.code" :command="s.code">
                      {{ s.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button size="small" link @click="openMatchEval(item)">查看匹配</el-button>
            </div>
          </div>
          <div v-if="!col.items?.length" class="col-empty">暂无候选人</div>
        </div>
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :size="drawerSize"
      destroy-on-close
      class="candidate-drawer"
    >
      <template #header>
        <div class="drawer-header-bar">
          <div>
            <h3 class="drawer-title">{{ drawerTitle }}</h3>
            <p class="drawer-subtitle">简历为主 · 右侧可直接操作招聘进展</p>
          </div>
          <div class="drawer-header-actions">
            <el-button
              v-if="drawerCandidateId"
              link
              type="primary"
              @click="openFullCandidate"
            >
              全屏查看
            </el-button>
          </div>
        </div>
      </template>
      <CandidateWorkspace
        v-if="drawerCandidateId"
        :candidate-id-prop="drawerCandidateId"
        :job-id-prop="selectedJobId || null"
        drawer-mode
        @loaded="onCandidateLoaded"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import JobContextBar from '@/components/common/JobContextBar.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import CandidateWorkspace from '@/views/candidate/CandidateWorkspace.vue'
import { PIPELINE_STAGE } from '@/constants/businessLabels'
import { getPipelineBoard, advancePipelineStage } from '@/api/modules/pipeline'
import { getJobList } from '@/api/modules/job'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const selectedJobId = ref<number | null>(null)
const board = ref<any>({ columns: [] })

const drawerVisible = ref(false)
const drawerCandidateId = ref<number | null>(null)
const drawerCandidateName = ref('')
const drawerSize = '92%'
const drawerTitle = computed(() => drawerCandidateName.value || '候选人详情')

function onCandidateLoaded(payload: { name: string }) {
  drawerCandidateName.value = payload.name
}

function openFullCandidate() {
  if (!drawerCandidateId.value) return
  router.push({
    path: `/pipeline/candidates/${drawerCandidateId.value}`,
    query: selectedJobId.value ? { jobId: String(selectedJobId.value) } : {},
  })
}

const stageFlow = Object.entries(PIPELINE_STAGE).map(([code, v]) => ({
  code,
  label: v.column,
}))

function nextStages(current: string) {
  const idx = stageFlow.findIndex(s => s.code === current)
  const result = []
  if (idx >= 0 && idx < stageFlow.length - 1) result.push(stageFlow[idx + 1])
  if (current !== 'ARCHIVED') result.push(stageFlow.find(s => s.code === 'ARCHIVED')!)
  return result
}

async function loadBoard() {
  if (!selectedJobId.value) {
    board.value = { columns: [] }
    return
  }
  loading.value = true
  try {
    const { data } = await getPipelineBoard(selectedJobId.value)
    board.value = data
  } finally {
    loading.value = false
  }
}

function openCandidate(item: any) {
  drawerCandidateId.value = item.candidateId
  drawerCandidateName.value = item.candidateName || ''
  drawerVisible.value = true
}

function openMatchEval(item: any) {
  router.push({
    path: '/pipeline/decision',
    query: { candidateId: String(item.candidateId), jobId: String(selectedJobId.value) },
  })
}

function goSourcing() {
  if (!selectedJobId.value) return
  router.push({ path: `/planning/jobs/${selectedJobId.value}`, query: { tab: 'sourcing' } })
}

async function advance(item: any, stage: string) {
  try {
    await advancePipelineStage(item.id, { toStage: stage })
    ElMessage.success('进展已更新')
    loadBoard()
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  }
}

onMounted(async () => {
  const qJob = Number(route.query.jobId)
  if (qJob) selectedJobId.value = qJob
  else {
    const { data } = await getJobList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' })
    const jobs = data?.list || data?.records || []
    if (jobs.length) selectedJobId.value = jobs[0].id
  }
  await loadBoard()
})
</script>

<style scoped lang="scss">
.kanban { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 8px; }
.kanban-col { min-width: 260px; background: #f8fafc; border-radius: 10px; border: 1px solid #e2e8f0; }
.col-header { display: flex; justify-content: space-between; padding: 12px; font-weight: 600; font-size: 13px; }
.col-body { padding: 0 8px 8px; display: flex; flex-direction: column; gap: 8px; min-height: 120px; }
.kanban-card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px; cursor: pointer;
  &:hover { border-color: #3b82f6; }
}
.card-name { font-weight: 600; font-size: 14px; }
.card-meta { font-size: 12px; color: #64748b; margin-top: 4px; }
.card-match { margin-top: 8px; }
.card-actions { margin-top: 8px; display: flex; gap: 8px; flex-wrap: wrap; }
.col-empty { text-align: center; font-size: 12px; color: #94a3b8; padding: 16px 8px; }

.drawer-header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding-right: 8px;
}

.drawer-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.drawer-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}

:deep(.candidate-drawer .el-drawer__body) {
  padding-top: 8px;
}
</style>
