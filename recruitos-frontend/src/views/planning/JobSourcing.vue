<template>
  <div class="job-sourcing">
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="animate-spin h-6 w-6 border-2 border-primary border-t-transparent rounded-full" />
    </div>
    <div class="toolbar">
      <RButton :disabled="!canStart" @click="openStartWizard">
        <Play class="h-4 w-4 mr-1" />
        {{ ACTIONS.startPlatformTask }}
      </RButton>
      <RButton v-if="activeCampaign" variant="outline" @click="refresh">刷新</RButton>
    </div>

    <template v-if="activeCampaign">
      <div class="stats-row">
        <div class="stat"><b>{{ stats.published || 0 }}</b><span>已发布</span></div>
        <div class="stat"><b>{{ stats.searched || 0 }}</b><span>已检索</span></div>
        <div class="stat"><b>{{ stats.greeted || 0 }}</b><span>已打招呼</span></div>
        <div class="stat"><b>{{ stats.resumes || 0 }}</b><span>已收简历</span></div>
        <div class="stat"><b>{{ stats.imported || 0 }}</b><span>已纳入候选人</span></div>
        <div class="stat"><b>{{ stats.screenSkipped || 0 }}</b><span>筛选淘汰</span></div>
        <div class="stat"><b>{{ stats.pendingScreening || 0 }}</b><span>待初筛</span></div>
      </div>

      <RAlert
        v-if="activeCampaign.status === 'RUNNING' && activeCampaign.opsPackVersion"
        variant="info"
        class="mb-4"
      >
        本任务使用招人方式 v{{ activeCampaign.opsPackVersion }}（启动时锁定）
      </RAlert>

      <h4 class="text-[15px] font-semibold text-text-primary mb-3">平台执行</h4>
      <RTable class="mb-4">
        <RTableHead>
          <RTableRow>
            <RTableTh>平台</RTableTh>
            <RTableTh>主账号</RTableTh>
            <RTableTh>当前步骤</RTableTh>
            <RTableTh>状态</RTableTh>
            <RTableTh>平台职位</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="row in (activeCampaign.platformRuns || [])" :key="row.platform">
            <RTableCell>{{ row.platform }}</RTableCell>
            <RTableCell>{{ row.primaryAccountName }}</RTableCell>
            <RTableCell>{{ row.currentStep }}</RTableCell>
            <RTableCell>{{ row.status }}</RTableCell>
            <RTableCell>
              <a v-if="row.platformJobUrl" :href="row.platformJobUrl" target="_blank" class="text-primary hover:underline">查看</a>
              <span v-else>-</span>
            </RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>

      <div v-if="pendingActions.length" class="pending-box">
        <h4 class="text-[15px] font-semibold text-text-primary mb-3">待你确认</h4>
        <div v-for="item in pendingActions" :key="item.id" class="pending-item">
          <span>{{ item.candidateName }} · {{ item.platform }}</span>
          <div>
            <RButton v-if="item.traceStatus === 'PENDING_GREET_CONFIRM'" size="sm"
              @click="confirmGreet(item.id)">{{ ACTIONS.confirmGreet }}</RButton>
            <RButton v-if="item.traceStatus === 'PENDING_IMPORT'" size="sm"
              @click="confirmImport(item.id)">{{ ACTIONS.importCandidate }}</RButton>
          </div>
        </div>
        <RButton v-if="needsPublishConfirm" size="sm" class="mt-2" @click="confirmPublish">确认发布到平台</RButton>
      </div>

      <h4 class="text-[15px] font-semibold text-text-primary mb-3 mt-4">候选人轨迹</h4>
      <RTable>
        <RTableHead>
          <RTableRow>
            <RTableTh>姓名</RTableTh>
            <RTableTh>平台</RTableTh>
            <RTableTh>招聘账号</RTableTh>
            <RTableTh>状态</RTableTh>
            <RTableTh>筛选阶段</RTableTh>
            <RTableTh>说明</RTableTh>
            <RTableTh>联系策略</RTableTh>
            <RTableTh>匹配建议</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="row in traces" :key="row.id">
            <RTableCell>{{ row.candidateName }}</RTableCell>
            <RTableCell>{{ row.platform }}</RTableCell>
            <RTableCell>{{ row.lockedByAccountName }}</RTableCell>
            <RTableCell>{{ traceStatusLabel(row.traceStatus) }}</RTableCell>
            <RTableCell>{{ screenStageLabel(row.screenStage) }}</RTableCell>
            <RTableCell>{{ row.skipReasonSummary || row.skipReason || '—' }}</RTableCell>
            <RTableCell>{{ greetStrategyLabel(row.greetStrategyApplied) }}</RTableCell>
            <RTableCell>
              <MatchVerdict
                :match-score="row.matchScore"
                :match-detail="row.matchDetail"
                mode="compact"
                :show-score="false"
              />
            </RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>

      <div class="actions mt-4 flex gap-2">
        <RButton v-if="activeCampaign.status === 'RUNNING'" variant="outline" @click="pauseCampaign">暂停</RButton>
        <RButton v-if="activeCampaign.status === 'PAUSED'" @click="resumeCampaign">恢复</RButton>
        <RButton variant="outline" @click="$router.push({ path: `/planning/jobs/${jobId}`, query: { tab: 'candidates' } })">
          查看在招候选人
        </RButton>
      </div>
    </template>

    <EmptyStateCta
      v-else
      title="尚未开始平台招人"
      description="请先在「规则 → 招人方式」中确认设置，再选择平台账号开始找人。"
      :actions="[
        { label: '设置招人方式', type: 'default', onClick: goSourcingMethod },
        { label: ACTIONS.startPlatformTask, type: 'primary', onClick: openStartWizard },
      ]"
    />

    <RDialog v-model:open="showStart" title="开启平台招人任务" width="640px">
      <!-- 步骤指示器 -->
      <div class="flex items-center justify-center gap-4 mb-6">
        <div v-for="(stepTitle, i) in ['确认招人方式', '选择平台', '运行方式']" :key="i"
          class="flex items-center gap-2" :class="i <= startStep ? 'text-primary' : 'text-text-placeholder'">
          <span class="w-6 h-6 rounded-full flex items-center justify-center text-[12px] font-medium"
            :class="i <= startStep ? 'bg-primary text-white' : 'bg-bg-muted'">{{ i + 1 }}</span>
          <span class="text-[13px]">{{ stepTitle }}</span>
          <span v-if="i < 2" class="w-8 h-px bg-border mx-1" />
        </div>
      </div>

      <div v-show="startStep === 0" class="step-body">
        <template v-if="startOpsPack">
          <p class="step-hint text-[13px] text-text-secondary mb-2">将使用以下已确认的招人方式（仅绑定到新任务）：</p>
          <ul class="summary-list list-disc pl-5 mb-3 text-[13px] text-text-regular space-y-1">
            <li v-for="(line, i) in startOpsSummary" :key="i">{{ line }}</li>
          </ul>
          <RBadge variant="success">v{{ startOpsPack.version }} 已生效</RBadge>
        </template>
        <EmptyStateCta
          v-else
          title="尚未确认招人方式"
          description="请先在职位工作台的「规则 → 招人方式」中生成并确认发布。"
          :actions="[{ label: '去设置', type: 'primary', onClick: goSourcingMethod }]"
        />
      </div>

      <div v-show="startStep === 1" class="step-body space-y-4">
        <div class="flex items-center gap-3">
          <label class="text-[13px] text-text-secondary w-[110px] shrink-0">任务名称</label>
          <RInput v-model="startForm.name" :placeholder="jobTitle + ' 平台招人'" class="flex-1" />
        </div>
        <div class="flex items-center gap-3">
          <label class="text-[13px] text-text-secondary w-[110px] shrink-0">搜寻来源</label>
          <RSelect v-model="startForm.searchSource" :options="searchSourceOptions" class="w-[200px]" />
        </div>
        <div class="flex items-center gap-3">
          <label class="text-[13px] text-text-secondary w-[110px] shrink-0">日配额</label>
          <div class="flex items-center gap-3">
            <span class="text-[13px]">Boss</span>
            <RInput v-model.number="startForm.platformQuotas.BOSS" type="number" class="w-20" />
            <span class="text-[13px]">猎聘</span>
            <RInput v-model.number="startForm.platformQuotas.LIEPIN" type="number" class="w-20" />
          </div>
        </div>
        <div class="flex items-center gap-3">
          <label class="text-[13px] text-text-secondary w-[110px] shrink-0">Boss直聘</label>
          <RCheckbox v-model="startForm.boss.enabled">启用</RCheckbox>
          <RSelect v-model="startForm.boss.primaryId" :options="bossAccounts.map(a => ({ label: a.accountName, value: a.id }))" placeholder="主账号" class="w-[200px] ml-3" />
        </div>
        <div class="flex items-center gap-3">
          <label class="text-[13px] text-text-secondary w-[110px] shrink-0">猎聘</label>
          <RCheckbox v-model="startForm.liepin.enabled">启用</RCheckbox>
          <RSelect v-model="startForm.liepin.primaryId" :options="liepinAccounts.map(a => ({ label: a.accountName, value: a.id }))" placeholder="主账号" class="w-[200px] ml-3" />
        </div>
      </div>

      <div v-show="startStep === 2" class="step-body space-y-4">
        <div class="flex items-start gap-3">
          <label class="text-[13px] text-text-secondary w-[110px] shrink-0 pt-1">运行方式</label>
          <div class="flex flex-col gap-2">
            <label v-for="mode in ['SEMI_AUTO', 'PUBLISH_SEARCH_ONLY']" :key="mode" class="flex items-center gap-2 text-[13px]">
              <input type="radio" :value="mode" v-model="startForm.mode" class="accent-primary" />
              {{ runModeLabel(mode) }}
            </label>
          </div>
        </div>
        <CollapsibleSection title="高级选项">
          <div class="space-y-3 mt-2">
            <label class="flex items-center gap-2 text-[13px]">
              <input type="radio" value="FULL_AUTO" v-model="startForm.mode" :disabled="!enableFullAuto || !tenantSafety.allowFullAuto" class="accent-primary" />
              {{ runModeLabel('FULL_AUTO') }}
            </label>
            <div v-if="tenantSafety.allowFullAuto" class="ml-6">
              <RCheckbox v-model="enableFullAuto">我确认启用全自动</RCheckbox>
            </div>
            <p v-else class="ml-6 text-[12px] text-text-placeholder">全自动运行未在租户设置中开启</p>
            <div class="flex items-start gap-3 mt-3">
              <label class="text-[13px] text-text-secondary w-[110px] shrink-0 pt-1">联系策略</label>
              <div class="flex flex-col gap-2">
                <label v-for="strategy in ['SCREEN_THEN_GREET', 'COLLECT_ONLY', 'CARD_GREET']" :key="strategy" class="flex items-center gap-2 text-[13px]">
                  <input type="radio" :value="strategy" v-model="startForm.greetStrategy" :disabled="strategy === 'CARD_GREET' && !allowCardGreet" class="accent-primary" />
                  {{ greetStrategyLabel(strategy) }}
                </label>
                <p v-if="!allowCardGreet" class="text-[12px] text-text-placeholder">「卡片即联系」未在租户设置中开启</p>
                <div v-if="startForm.greetStrategy === 'CARD_GREET'" class="mt-1">
                  <RCheckbox v-model="startForm.cardGreetRiskAccepted">
                    我了解该方式存在误触风险，适用于批量初级岗
                  </RCheckbox>
                </div>
              </div>
            </div>
          </div>
        </CollapsibleSection>
      </div>

      <template #footer>
        <RButton variant="outline" @click="showStart = false">取消</RButton>
        <RButton v-if="startStep > 0" variant="outline" @click="startStep--">上一步</RButton>
        <RButton v-if="startStep < 2" :disabled="startStep === 0 && !startOpsPack" @click="startStep++">
          下一步
        </RButton>
        <RButton v-else :loading="starting" @click="handleStart">开始招人</RButton>
      </template>
    </RDialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@/lib/notify'
import {
  RButton, RBadge, RInput, RSelect, RCheckbox, RDialog, RAlert,
  RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell,
} from '@/components/ui'
import { Play } from 'lucide-vue-next'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import CollapsibleSection from '@/components/app/CollapsibleSection.vue'
import {
  createWorkflow, getWorkflowList, getWorkflowDetail, pauseWorkflow, resumeWorkflow,
  getWorkflowCandidates, confirmWorkflowGreet, confirmWorkflowImport, confirmWorkflowPublish,
  getAgentAccountList,
} from '@/api/modules/agent'
import { getActiveOpsPack } from '@/api/modules/job'
import { ACTIONS, greetStrategyLabel, runModeLabel } from '@/constants/businessLabels'
import { loadTenantRecruitSafety } from '@/utils/tenantRecruitSafety'
import { opsPackHumanSummary } from '@/utils/opsPackSummary'

const router = useRouter()
const props = defineProps<{ jobId: number; jobTitle: string; jobStatus: string }>()
const emit = defineEmits<{ imported: [] }>()

const loading = ref(false)
const starting = ref(false)
const showStart = ref(false)
const startStep = ref(0)
const startOpsPack = ref<any>(null)
const startOpsSummary = ref<string[]>([])
const activeCampaign = ref<any>(null)
const traces = ref<any[]>([])
const bossAccounts = ref<any[]>([])
const liepinAccounts = ref<any[]>([])

const enableFullAuto = ref(false)
const boundOpsPackId = ref<number | null>(null)
const boundOpsPackVersion = ref<number | null>(null)

const startForm = reactive({
  name: '',
  mode: 'SEMI_AUTO',
  greetStrategy: 'SCREEN_THEN_GREET',
  searchSource: 'RECOMMEND',
  cardGreetRiskAccepted: false,
  platformQuotas: { BOSS: 30, LIEPIN: 30 } as Record<string, number>,
  boss: { enabled: true, primaryId: null as number | null, auxIds: [] as number[] },
  liepin: { enabled: true, primaryId: null as number | null, auxIds: [] as number[] },
})

const searchSourceOptions = [
  { label: '推荐人才', value: 'RECOMMEND' },
  { label: '主动搜索', value: 'SEARCH' },
  { label: '最新活跃', value: 'LATEST' },
]

const tenantSafety = loadTenantRecruitSafety()
enableFullAuto.value = tenantSafety.allowFullAuto
const allowCardGreet = tenantSafety.allowCardGreet
if (tenantSafety.defaultRunMode) startForm.mode = tenantSafety.defaultRunMode
if (tenantSafety.allowCardGreet === false && startForm.greetStrategy === 'CARD_GREET') {
  startForm.greetStrategy = 'SCREEN_THEN_GREET'
}

const canStart = computed(() => props.jobStatus === 'ACTIVE')
const stats = computed(() => activeCampaign.value?.stats || {})
const pendingActions = computed(() => activeCampaign.value?.pendingActions || [])
const needsPublishConfirm = computed(() =>
  activeCampaign.value?.publishConfirmRequired && pendingActions.value.some((p: any) => p.traceStatus === 'PUBLISH_PENDING'))

function modeLabel(mode: string) {
  return runModeLabel(mode)
}

function screenStageLabel(stage?: string) {
  if (!stage) return '—'
  return { CARD: '卡片筛选', FULL_RESUME: '简历筛选', PASSED: '已通过' }[stage] || stage
}

function traceStatusLabel(status?: string) {
  const map: Record<string, string> = {
    PENDING_GREET_CONFIRM: '待确认联系',
    PENDING_IMPORT: '待纳入候选人',
    PUBLISH_PENDING: '待发布',
    GREETED: '已联系',
    IMPORTED: '已纳入',
    SKIPPED: '已跳过',
  }
  return map[status || ''] || status || '—'
}

function openStartWizard() {
  startStep.value = 0
  showStart.value = true
}

async function onStartDialogOpen() {
  startStep.value = 0
  try {
    const packRes: any = await getActiveOpsPack(props.jobId)
    startOpsPack.value = packRes.data || null
    startOpsSummary.value = startOpsPack.value?.pack
      ? opsPackHumanSummary(startOpsPack.value.pack)
      : []
    if (startOpsPack.value?.pack?.platformQuotas) {
      Object.assign(startForm.platformQuotas, startOpsPack.value.pack.platformQuotas)
    }
    if (startOpsPack.value?.pack?.greetStrategy) {
      startForm.greetStrategy = startOpsPack.value.pack.greetStrategy
    }
    if (!allowCardGreet && startForm.greetStrategy === 'CARD_GREET') {
      startForm.greetStrategy = 'SCREEN_THEN_GREET'
    }
  } catch {
    startOpsPack.value = null
    startOpsSummary.value = []
  }
}

function goSourcingMethod() {
  router.push({ path: `/planning/jobs/${props.jobId}`, query: { tab: 'rules', sub: 'method' } })
  showStart.value = false
}

async function loadAccounts() {
  const res: any = await getAgentAccountList({ pageSize: 100 })
  const rows = res.data?.records || res.data || []
  bossAccounts.value = rows.filter((a: any) => a.platform === 'BOSS' && a.status === 'ACTIVE')
  liepinAccounts.value = rows.filter((a: any) => a.platform === 'LIEPIN' && a.status === 'ACTIVE')
  if (bossAccounts.value.length) startForm.boss.primaryId = bossAccounts.value[0].id
  if (liepinAccounts.value.length) startForm.liepin.primaryId = liepinAccounts.value[0].id
}

async function loadCampaign() {
  loading.value = true
  try {
    const res: any = await getWorkflowList({ jobId: props.jobId })
    const list = res.data || []
    const running = list.find((c: any) => c.status === 'RUNNING' || c.status === 'PAUSED') || list[0]
    if (!running) {
      activeCampaign.value = null
      traces.value = []
      return
    }
    const detail: any = await getWorkflowDetail(running.id)
    activeCampaign.value = detail.data || detail
    const tr: any = await getWorkflowCandidates(running.id)
    traces.value = tr.data || []
  } finally {
    loading.value = false
  }
}

function buildPlatformConfigs() {
  const configs: any[] = []
  if (startForm.boss.enabled && startForm.boss.primaryId) {
    configs.push({
      platform: 'BOSS',
      primaryAccountId: startForm.boss.primaryId,
      auxiliaryAccountIds: startForm.boss.auxIds.filter(id => id !== startForm.boss.primaryId),
    })
  }
  if (startForm.liepin.enabled && startForm.liepin.primaryId) {
    configs.push({
      platform: 'LIEPIN',
      primaryAccountId: startForm.liepin.primaryId,
      auxiliaryAccountIds: startForm.liepin.auxIds.filter(id => id !== startForm.liepin.primaryId),
    })
  }
  return configs
}

watch(enableFullAuto, (on) => {
  if (!on && startForm.mode === 'FULL_AUTO') startForm.mode = 'SEMI_AUTO'
})

async function handleStart() {
  if (startForm.mode === 'FULL_AUTO' && !enableFullAuto.value) {
    toast.warning('全自动模式需勾选确认开关')
    return
  }
  if (startForm.greetStrategy === 'CARD_GREET' && !startForm.cardGreetRiskAccepted) {
    toast.warning('请勾选高风险联系方式的确认项')
    return
  }
  const platformConfigs = buildPlatformConfigs()
  if (!platformConfigs.length) {
    toast.warning('请至少启用一个平台并选择主账号')
    return
  }
  starting.value = true
  try {
    let opsPackId = boundOpsPackId.value
    let opsPackVersion = boundOpsPackVersion.value
    if (!opsPackId) {
      const packRes: any = await getActiveOpsPack(props.jobId)
      const pack = packRes.data
      if (!pack?.id) {
        toast.warning('请先在「规则 → 招人方式」中确认发布')
        return
      }
      opsPackId = pack.id
      opsPackVersion = pack.version
    }
    await createWorkflow({
      name: startForm.name || `${props.jobTitle} 渠道招聘`,
      jobId: props.jobId,
      mode: startForm.mode,
      opsPackId,
      opsPackVersion,
      greetStrategy: startForm.greetStrategy,
      searchSource: startForm.searchSource,
      platformQuotas: startForm.platformQuotas,
      cardGreetRiskAccepted: startForm.cardGreetRiskAccepted,
      platformConfigs,
      platforms: platformConfigs.map(p => p.platform),
    })
    showStart.value = false
    toast.success('平台招人任务已开启')
    await loadCampaign()
  } catch {
    toast.error('启动失败')
  } finally {
    starting.value = false
  }
}

async function pauseCampaign() {
  if (!activeCampaign.value) return
  await pauseWorkflow(activeCampaign.value.id)
  toast.success('已暂停')
  loadCampaign()
}

async function resumeCampaign() {
  if (!activeCampaign.value) return
  await resumeWorkflow(activeCampaign.value.id)
  toast.success('已恢复')
  loadCampaign()
}

async function confirmGreet(traceId: number) {
  await confirmWorkflowGreet(traceId)
  toast.success('已发送打招呼')
  loadCampaign()
}

async function confirmImport(traceId: number) {
  await confirmWorkflowImport(traceId)
  await loadCampaign()
  emit('imported')
}

async function confirmPublish() {
  if (!activeCampaign.value) return
  await confirmWorkflowPublish(activeCampaign.value.id)
  toast.success('已确认发布')
  loadCampaign()
}

function refresh() { loadCampaign() }

watch(() => props.jobId, loadCampaign)
onMounted(async () => {
  const q = router.currentRoute.value.query
  if (q.opsPackId) boundOpsPackId.value = Number(q.opsPackId)
  if (q.opsPackVersion) boundOpsPackVersion.value = Number(q.opsPackVersion)
  await loadAccounts()
  await loadCampaign()
})
</script>

<style scoped>
.stats-row { display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.stat { background: #f8fafc; border-radius: 8px; padding: 12px 16px; min-width: 90px; text-align: center; }
.stat b { display: block; font-size: 20px; }
.stat span { color: #64748b; font-size: 12px; }
.mb-12 { margin-bottom: 12px; }
.mb-16 { margin-bottom: 16px; }
.mt-8 { margin-top: 8px; }
.start-steps { margin-bottom: 24px; }
.step-body { min-height: 200px; padding: 8px 0; }
.step-hint { color: #64748b; font-size: 13px; margin-bottom: 12px; }
.summary-list { margin: 0 0 12px 18px; color: #334155; font-size: 14px; line-height: 1.8; }
.mt-12 { margin-top: 12px; }
.pending-box { background: #fffbeb; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
.pending-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; }
.mode-hint { margin-top: 8px; font-size: 12px; color: #64748b; }
.quota-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.quota-row span { color: #64748b; font-size: 13px; }
</style>
