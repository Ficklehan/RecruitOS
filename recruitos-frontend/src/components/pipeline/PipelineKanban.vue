<template>
  <div class="pipeline-kanban relative">
    <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60 rounded-lg min-h-[200px]">
      <Loader2 class="h-6 w-6 animate-spin text-primary" />
    </div>

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
          <Badge variant="secondary">{{ col.items?.length || 0 }}</Badge>
        </div>
        <div class="col-body">
          <div
            v-for="item in col.items"
            :key="item.id"
            class="kanban-card"
            @click="openCandidate(item)"
          >
            <div class="card-name flex items-center gap-2">{{ item.candidateName }}<IntentIndicator :intent="intentMap[item.candidateId]" :loading="intentLoading[item.candidateId]" /></div>
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
              <Button
                v-if="col.stage === 'SOURCED'"
                size="sm"
                @click="advance(item, 'SCREENING')"
              >
                {{ ACTIONS.passScreen }}
              </Button>
              <DropdownMenu v-else>
                <DropdownMenuTrigger>
                  <Button size="sm" variant="link">推进阶段</Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent>
                  <DropdownMenuItem
                    v-for="s in nextStages(col.stage)"
                    :key="s.code"
                    @click="advance(item, s.code)"
                  >
                    {{ s.label }}
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
              <Button size="sm" variant="link" @click="openMatchEval(item)">查看匹配</Button>
            </div>
          </div>
          <div v-if="!col.items?.length" class="col-empty">暂无候选人</div>
        </div>
      </div>
    </div>

    <Sheet v-model:open="drawerVisible" side="right">
      <SheetContent class="!w-[92%] !max-w-none overflow-y-auto p-0">
        <div class="drawer-header-bar p-6 pb-2">
          <div>
            <h3 class="drawer-title">{{ drawerTitle }}</h3>
            <p class="drawer-subtitle">查看简历并推进在招候选人</p>
          </div>
          <Button v-if="drawerCandidateId" variant="link" @click="openFullCandidate">
            全屏查看
          </Button>
        </div>
        <div class="px-6 pb-6">
          <CandidateWorkspace
            v-if="drawerCandidateId"
            :candidate-id-prop="drawerCandidateId"
            :job-id-prop="jobId"
            drawer-mode
            @loaded="onCandidateLoaded"
          />
        </div>
      </SheetContent>
    </Sheet>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Loader2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { Button, Badge, Sheet, SheetContent, DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem } from '@/components/ui'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import IntentIndicator from "@/components/ai/IntentIndicator.vue"
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import CandidateWorkspace from '@/views/candidate/CandidateWorkspace.vue'
import { ACTIONS, PIPELINE_STAGE, pipelineStageLabel } from '@/constants/businessLabels'

import { getCandidateIntent, type CandidateIntent } from "@/api/modules/brain"
import { advancePipelineStage, getPipelineBoard } from '@/api/modules/pipeline'

const props = defineProps<{ jobId: number | null }>()
const emit = defineEmits<{ loaded: [payload: { total: number; sourced: number }] }>()

const router = useRouter()
const loading = ref(false)
const board = ref<any>({ columns: [] })
const drawerVisible = ref(false)
const drawerCandidateId = ref<number | null>(null)
const drawerCandidateName = ref('')
const intentMap = ref<Record<number, CandidateIntent | null>>({})
const intentLoading = ref<Record<number, boolean>>({})
const drawerTitle = computed(() => drawerCandidateName.value || '候选人详情')

const stageFlow = Object.entries(PIPELINE_STAGE).map(([code, v]) => ({ code, label: v.column }))
const displayColumns = computed(() =>
  (board.value.columns || []).map((col: any) => ({
    ...col,
    label: pipelineStageLabel(col.stage, 'column'),
  }))
)

function nextStages(current: string) {
  const idx = stageFlow.findIndex((s) => s.code === current)
  const result = []
  if (idx >= 0 && idx < stageFlow.length - 1) result.push(stageFlow[idx + 1])
  if (current !== 'ARCHIVED') result.push(stageFlow.find((s) => s.code === 'ARCHIVED')!)
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
    loadIntents()
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
    query: { candidateId: String(item.candidateId), jobId: String(props.jobId) },
  })
}

async function advance(item: any, stage: string) {
  try {
    await advancePipelineStage(item.id, { toStage: stage })
    toast.success('进展已更新')
    await loadBoard()
  } catch (e: any) {
    toast.error(e.message || '更新失败')
  }
}

async function loadIntents() {
  const columns = board.value.columns || []
  for (const col of columns) {
    for (const item of (col.items || [])) {
      const cid = item.candidateId
      if (!cid) continue
      intentLoading.value[cid] = true
      try {
        const data = await getCandidateIntent(cid, props.jobId || 0)
        intentMap.value[cid] = data
      } catch { intentMap.value[cid] = null }
      finally { intentLoading.value[cid] = false }
    }
  }
}

watch(() => props.jobId, loadBoard, { immediate: true })
defineExpose({ reload: loadBoard })
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.pipeline-kanban { width: 100%; min-width: 0; }

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
}

.col-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  font-weight: 600;
  font-size: 13px;
  color: $text-primary;
  border-bottom: 1px solid var(--r-divider);
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
  border-radius: $border-radius-sm;
  padding: 12px;
  cursor: pointer;
  transition: all $transition-base;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

  &:hover {
    background: $primary-50;
    transform: translateY(-1px);
    box-shadow: $shadow-card-hover;
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
}
.drawer-title { margin: 0; font-size: 18px; font-weight: 600; color: #0f172a; }
.drawer-subtitle { margin: 4px 0 0; font-size: 13px; color: #64748b; }
</style>
