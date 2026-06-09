<template>
  <div class="pipeline-kanban" v-loading="loading">
    <div v-if="!jobId" class="kanban-empty">
      <EmptyStateCta
        title="还没有选定职位"
        description="请选择在招职位后查看在招候选人"
        :image-size="56"
      />
    </div>

    <div v-else class="kanban">
      <div v-for="col in displayColumns" :key="col.stage" class="kanban-col">
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
            <div class="card-meta">{{ item.candidateCompany || item.candidateTitle || '—' }}</div>
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
                {{ ACTIONS.passScreen }}
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
            <p class="drawer-subtitle">查看简历并推进在招候选人</p>
          </div>
          <el-button v-if="drawerCandidateId" link type="primary" @click="openFullCandidate">
            全屏查看
          </el-button>
        </div>
      </template>
      <CandidateWorkspace
        v-if="drawerCandidateId"
        :candidate-id-prop="drawerCandidateId"
        :job-id-prop="jobId"
        drawer-mode
        @loaded="onCandidateLoaded"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import CandidateWorkspace from '@/views/candidate/CandidateWorkspace.vue'
import { ACTIONS, PIPELINE_STAGE, pipelineStageLabel } from '@/constants/businessLabels'
import { advancePipelineStage, getPipelineBoard } from '@/api/modules/pipeline'

const props = defineProps<{
  jobId: number | null
}>()

const emit = defineEmits<{
  loaded: [payload: { total: number; sourced: number }]
}>()

const router = useRouter()
const loading = ref(false)
const board = ref<any>({ columns: [] })

const drawerVisible = ref(false)
const drawerCandidateId = ref<number | null>(null)
const drawerCandidateName = ref('')
const drawerSize = '92%'
const drawerTitle = computed(() => drawerCandidateName.value || '候选人详情')

const stageFlow = Object.entries(PIPELINE_STAGE).map(([code, v]) => ({
  code,
  label: v.column,
}))

const displayColumns = computed(() =>
  (board.value.columns || []).map((col: any) => ({
    ...col,
    label: pipelineStageLabel(col.stage, 'column'),
  })),
)

function nextStages(current: string) {
  const idx = stageFlow.findIndex(s => s.code === current)
  const result = []
  if (idx >= 0 && idx < stageFlow.length - 1) result.push(stageFlow[idx + 1])
  if (current !== 'ARCHIVED') result.push(stageFlow.find(s => s.code === 'ARCHIVED')!)
  return result
}

function onCandidateLoaded(payload: { name: string }) {
  drawerCandidateName.value = payload.name
}

function openFullCandidate() {
  if (!drawerCandidateId.value) return
  router.push({
    path: `/pipeline/candidates/${drawerCandidateId.value}`,
    query: props.jobId ? { jobId: String(props.jobId) } : {},
  })
}

async function loadBoard() {
  if (!props.jobId) {
    board.value = { columns: [] }
    emit('loaded', { total: 0, sourced: 0 })
    return
  }
  loading.value = true
  try {
    const { data } = await getPipelineBoard(props.jobId)
    board.value = data || { columns: [] }
    const columns = board.value.columns || []
    const total = columns.reduce((n: number, c: any) => n + (c.items?.length || 0), 0)
    const sourcedCol = columns.find((c: any) => c.stage === 'SOURCED')
    emit('loaded', { total, sourced: sourcedCol?.items?.length || 0 })
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
    query: {
      candidateId: String(item.candidateId),
      jobId: String(props.jobId),
    },
  })
}

async function advance(item: any, stage: string) {
  try {
    await advancePipelineStage(item.id, { toStage: stage })
    ElMessage.success('进展已更新')
    await loadBoard()
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  }
}

watch(() => props.jobId, loadBoard, { immediate: true })

defineExpose({ reload: loadBoard })
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.pipeline-kanban {
  width: 100%;
  min-width: 0;
}

.kanban {
  display: flex;
  gap: 12px;
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  padding-bottom: 4px;
}

.kanban-col {
  flex: 1 1 240px;
  min-width: 220px;
  background: $bg-muted;
  border-radius: $border-radius;
  border: 1px solid $border-color;
}

.col-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  font-weight: 600;
  font-size: 13px;
  color: $text-primary;
  border-bottom: 1px solid $border-color-light;
}

.col-body {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 160px;
}

.kanban-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $border-radius-sm;
  padding: 12px;
  cursor: pointer;
  transition: border-color $transition-fast, background-color $transition-fast;

  &:hover {
    border-color: $primary-light;
    background: $primary-lighter;
  }
}

.card-name { font-weight: 600; font-size: 14px; color: $text-primary; }
.card-meta { font-size: 12px; color: $text-secondary; margin-top: 4px; }
.card-match { margin-top: 8px; }
.card-actions { margin-top: 10px; display: flex; gap: 6px; flex-wrap: wrap; }
.col-empty { text-align: center; font-size: 12px; color: $text-placeholder; padding: 20px 8px; }
.kanban-empty { padding: 24px 0; width: 100%; }

.drawer-header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding-right: 8px;
}
.drawer-title { margin: 0; font-size: 18px; font-weight: 600; color: #0f172a; }
.drawer-subtitle { margin: 4px 0 0; font-size: 13px; color: #64748b; }
:deep(.candidate-drawer .el-drawer__body) { padding-top: 8px; }
</style>
