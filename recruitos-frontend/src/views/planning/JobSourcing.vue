<template>
  <div class="job-sourcing">
    <div class="toolbar">
      <RButton :disabled="!canStart" @click="openStartWizard">
        <Play class="h-4 w-4" />
        {{ ACTIONS.startPlatformTask }}
      </RButton>
      <RButton v-if="activeCampaign" variant="ghost" @click="refresh">刷新</RButton>
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

      <RAlert v-if="activeCampaign.status === 'RUNNING' && activeCampaign.opsPackVersion" variant="info" class="mb-12">
        本任务使用招人方式 v{{ activeCampaign.opsPackVersion }}（启动时锁定）
      </RAlert>

      <h4>平台执行</h4>
      <div class="data-table mb-16">
        <table class="w-full text-[13px]">
          <thead><tr><th>平台</th><th>主账号</th><th>当前步骤</th><th>状态</th><th>平台职位</th></tr></thead>
          <tbody>
            <tr v-for="(run, i) in (activeCampaign.platformRuns || [])" :key="i">
              <td>{{ run.platform }}</td>
              <td>{{ run.primaryAccountName }}</td>
              <td>{{ run.currentStep }}</td>
              <td>{{ run.status }}</td>
              <td>
                <a v-if="run.platformJobUrl" :href="run.platformJobUrl" target="_blank" class="text-primary">查看</a>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="pendingActions.length" class="pending-box">
        <h4>待你确认</h4>
        <div v-for="item in pendingActions" :key="item.id" class="pending-item">
          <span>{{ item.candidateName }} · {{ item.platform }}</span>
          <div class="flex gap-2">
            <RButton v-if="item.traceStatus === 'PENDING_GREET_CONFIRM'" size="sm" @click="confirmGreet(item.id)">{{ ACTIONS.confirmGreet }}</RButton>
            <RButton v-if="item.traceStatus === 'PENDING_IMPORT'" size="sm" @click="confirmImport(item.id)">{{ ACTIONS.importCandidate }}</RButton>
          </div>
        </div>
        <RButton v-if="needsPublishConfirm" size="sm" variant="ghost" class="mt-8" @click="confirmPublish">确认发布到平台</RButton>
      </div>

      <h4>候选人轨迹</h4>
      <div class="data-table">
        <table class="w-full text-[13px]">
          <thead><tr><th>姓名</th><th>平台</th><th>招聘账号</th><th>状态</th><th>筛选阶段</th><th>说明</th><th>联系策略</th><th>匹配建议</th></tr></thead>
          <tbody>
            <tr v-for="row in traces" :key="row.id">
              <td>{{ row.candidateName }}</td>
              <td>{{ row.platform }}</td>
              <td>{{ row.lockedByAccountName }}</td>
              <td>{{ traceStatusLabel(row.traceStatus) }}</td>
              <td>{{ screenStageLabel(row.screenStage) }}</td>
              <td>{{ row.skipReasonSummary || row.skipReason || '—' }}</td>
              <td>{{ greetStrategyLabel(row.greetStrategyApplied) }}</td>
              <td>
                <MatchVerdict :match-score="row.matchScore" :match-detail="row.matchDetail" mode="compact" :show-score="false" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="actions mt-12">
        <RButton v-if="activeCampaign.status === 'RUNNING'" variant="ghost" @click="pauseCampaign">暂停</RButton>
        <RButton v-if="activeCampaign.status === 'PAUSED'" @click="resumeCampaign">恢复</RButton>
        <RButton variant="ghost" @click="$router.push({ path: `/planning/jobs/${jobId}`, query: { tab: 'candidates' } })">
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

    <RDialog v-model="showStart" title="开启平台招人任务" width="640px">
      <div class="start-steps">
        <div class="step" v-for="(s, i) in ['确认招人方式', '选择平台', '运行方式']" :key="i" :class="{ active: startStep === i, done: startStep > i }">
          <div class="step-dot">{{ startStep > i ? '✓' : i + 1 }}</div>
          <span>{{ s }}</span>
        </div>
      </div>

      <div v-show="startStep === 0" class="step-body">
        <template v-if="startOpsPack">
          <p class="step-hint">将使用以下已确认的招人方式（仅绑定到新任务）：</p>
          <ul class="summary-list">
            <li v-for="(line, i) in startOpsSummary" :key="i">{{ line }}</li>
          </ul>
          <RBadge variant="success" size="sm">v{{ startOpsPack.version }} 已生效</RBadge>
        </template>
        <EmptyStateCta
          v-else
          title="尚未确认招人方式"
          description="请先在职位工作台的「规则 → 招人方式」中生成并确认发布。"
          :actions="[{ label: '去设置', type: 'primary', onClick: goSourcingMethod }]"
        />
      </div>

      <div v-show="startStep === 1" class="step-body">
        <div class="form-grid">
          <label class="form-label">任务名称</label>
          <RInput v-model="startForm.name" :placeholder="jobTitle + ' 平台招人'" />

          <label class="form-label">搜寻来源</label>
          <RSelect v-model="startForm.searchSource" :options="[{ label: '推荐人才', value: 'RECOMMEND' }, { label: '主动搜索', value: 'SEARCH' }, { label: '最新活跃', value: 'LATEST' }]" class="w-[200px]" />

          <label class="form-label">日配额</label>
          <div class="quota-row">
            <span>Boss</span>
            <input type="number" v-model.number="startForm.platformQuotas.BOSS" :min="1" :max="200" class="w-20 h-8 px-2 text-[13px] bg-bg-muted rounded" />
            <span>猎聘</span>
            <input type="number" v-model.number="startForm.platformQuotas.LIEPIN" :min="1" :max="200" class="w-20 h-8 px-2 text-[13px] bg-bg-muted rounded" />
          </div>

          <label class="form-label">Boss直聘</label>
          <div class="flex items-center gap-3">
            <RCheckbox v-model="startForm.boss.enabled">启用</RCheckbox>
            <RSelect v-model="startForm.boss.primaryId" :options="bossAccounts.map(a => ({ label: a.accountName, value: a.id }))" placeholder="主账号" class="w-[200px]" />
          </div>

          <label class="form-label">猎聘</label>
          <div class="flex items-center gap-3">
            <RCheckbox v-model="startForm.liepin.enabled">启用</RCheckbox>
            <RSelect v-model="startForm.liepin.primaryId" :options="liepinAccounts.map(a => ({ label: a.accountName, value: a.id }))" placeholder="主账号" class="w-[200px]" />
          </div>
        </div>
      </div>

      <div v-show="startStep === 2" class="step-body">
        <div class="form-grid">
          <label class="form-label">运行方式</label>
          <RRadioGroup v-model="startForm.mode" class="flex flex-col gap-2">
            <RRadio value="SEMI_AUTO">{{ runModeLabel('SEMI_AUTO') }}</RRadio>
            <RRadio value="PUBLISH_SEARCH_ONLY">{{ runModeLabel('PUBLISH_SEARCH_ONLY') }}</RRadio>
          </RRadioGroup>

          <div class="col-span-2 mt-4">
            <CollapsibleSection title="高级选项">
              <RRadioGroup v-model="startForm.mode" class="flex flex-col gap-2">
                <RRadio value="FULL_AUTO" :disabled="!enableFullAuto || !tenantSafety.allowFullAuto">
                  {{ runModeLabel('FULL_AUTO') }}
                </RRadio>
              </RRadioGroup>
              <div v-if="tenantSafety.allowFullAuto" class="mode-hint mt-2">
                <RCheckbox v-model="enableFullAuto">我确认启用全自动</RCheckbox>
              </div>
              <p v-else class="mode-hint">全自动运行未在租户设置中开启</p>

              <label class="form-label mt-4">联系策略</label>
              <RRadioGroup v-model="startForm.greetStrategy" class="flex flex-col gap-2">
                <RRadio value="SCREEN_THEN_GREET">{{ greetStrategyLabel('SCREEN_THEN_GREET') }}</RRadio>
                <RRadio value="COLLECT_ONLY">{{ greetStrategyLabel('COLLECT_ONLY') }}</RRadio>
                <RRadio value="CARD_GREET" :disabled="!allowCardGreet">
                  {{ greetStrategyLabel('CARD_GREET') }}
                </RRadio>
              </RRadioGroup>
              <p v-if="!allowCardGreet" class="mode-hint">「卡片即联系」未在租户设置中开启</p>
              <div v-if="startForm.greetStrategy === 'CARD_GREET'" class="mode-hint mt-2">
                <RCheckbox v-model="startForm.cardGreetRiskAccepted">
                  我了解该方式存在误触风险，适用于批量初级岗
                </RCheckbox>
              </div>
            </CollapsibleSection>
          </div>
        </div>
      </div>

      <template #footer>
        <RButton variant="ghost" @click="showStart = false">取消</RButton>
        <RButton v-if="startStep > 0" variant="ghost" @click="startStep--">上一步</RButton>
        <RButton v-if="startStep < 2" :disabled="startStep === 0 && !startOpsPack" @click="startStep++">下一步</RButton>
        <RButton v-else :loading="starting" @click="handleStart">开始招人</RButton>
      </template>
    </RDialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Play } from 'lucide-vue-next'
import { RButton, RBadge, RDialog, RInput, RSelect, RCheckbox, RRadioGroup, RRadio, RAlert } from '@/components/ui'
import { toast } from '@/lib/toast'
import CollapsibleSection from '@/components/app/CollapsibleSection.vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
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

function screenStageLabel(stage?: string) {
  if (!stage) return '—'
  return ({ CARD: '卡片筛选', FULL_RESUME: '简历筛选', PASSED: '已通过' } as Record<string, string>)[stage] || stage
}

function traceStatusLabel(status?: string) {
  const map: Record<string, string> = {
    PENDING_GREET_CONFIRM: '待确认联系', PENDING_IMPORT: '待纳入候选人',
    PUBLISH_PENDING: '待发布', GREETED: '已联系', IMPORTED: '已纳入', SKIPPED: '已跳过',
  }
  return map[status || ''] || status || '—'
}

function openStartWizard() { startStep.value = 0; showStart.value = true }

async function onStartDialogOpen() {
  startStep.value = 0
  try {
    const packRes: any = await getActiveOpsPack(props.jobId)
    startOpsPack.value = packRes.data || null
    startOpsSummary.value = startOpsPack.value?.pack ? opsPackHumanSummary(startOpsPack.value.pack) : []
    if (startOpsPack.value?.pack?.platformQuotas) Object.assign(startForm.platformQuotas, startOpsPack.value.pack.platformQuotas)
    if (startOpsPack.value?.pack?.greetStrategy) startForm.greetStrategy = startOpsPack.value.pack.greetStrategy
    if (!allowCardGreet && startForm.greetStrategy === 'CARD_GREET') startForm.greetStrategy = 'SCREEN_THEN_GREET'
  } catch { startOpsPack.value = null; startOpsSummary.value = [] }
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
    if (!running) { activeCampaign.value = null; traces.value = []; return }
    const detail: any = await getWorkflowDetail(running.id)
    activeCampaign.value = detail.data || detail
    const tr: any = await getWorkflowCandidates(running.id)
    traces.value = tr.data || []
  } finally { loading.value = false }
}

function buildPlatformConfigs() {
  const configs: any[] = []
  if (startForm.boss.enabled && startForm.boss.primaryId) {
    configs.push({ platform: 'BOSS', primaryAccountId: startForm.boss.primaryId, auxiliaryAccountIds: startForm.boss.auxIds.filter(id => id !== startForm.boss.primaryId) })
  }
  if (startForm.liepin.enabled && startForm.liepin.primaryId) {
    configs.push({ platform: 'LIEPIN', primaryAccountId: startForm.liepin.primaryId, auxiliaryAccountIds: startForm.liepin.auxIds.filter(id => id !== startForm.liepin.primaryId) })
  }
  return configs
}

watch(enableFullAuto, (on) => { if (!on && startForm.mode === 'FULL_AUTO') startForm.mode = 'SEMI_AUTO' })

async function handleStart() {
  if (startForm.mode === 'FULL_AUTO' && !enableFullAuto.value) { toast.warning('全自动模式需勾选确认开关'); return }
  if (startForm.greetStrategy === 'CARD_GREET' && !startForm.cardGreetRiskAccepted) { toast.warning('请勾选高风险联系方式的确认项'); return }
  const platformConfigs = buildPlatformConfigs()
  if (!platformConfigs.length) { toast.warning('请至少启用一个平台并选择主账号'); return }
  starting.value = true
  try {
    let opsPackId = boundOpsPackId.value
    let opsPackVersion = boundOpsPackVersion.value
    if (!opsPackId) {
      const packRes: any = await getActiveOpsPack(props.jobId)
      const pack = packRes.data
      if (!pack?.id) { toast.warning('请先在「规则 → 招人方式」中确认发布'); return }
      opsPackId = pack.id; opsPackVersion = pack.version
    }
    await createWorkflow({
      name: startForm.name || `${props.jobTitle} 渠道招聘`,
      jobId: props.jobId, mode: startForm.mode, opsPackId, opsPackVersion,
      greetStrategy: startForm.greetStrategy, searchSource: startForm.searchSource,
      platformQuotas: startForm.platformQuotas, cardGreetRiskAccepted: startForm.cardGreetRiskAccepted,
      platformConfigs, platforms: platformConfigs.map(p => p.platform),
    })
    showStart.value = false
    toast.success('平台招人任务已开启')
    await loadCampaign()
  } catch { toast.error('启动失败') } finally { starting.value = false }
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
.stat { background: var(--r-bg-muted); border-radius: 8px; padding: 12px 16px; min-width: 90px; text-align: center; }
.stat b { display: block; font-size: 20px; }
.stat span { color: var(--r-text-secondary); font-size: 12px; }
.mb-12 { margin-bottom: 12px; }
.mb-16 { margin-bottom: 16px; }
.mt-8 { margin-top: 8px; }
.mt-12 { margin-top: 12px; }
.w-\[200px\] { width: 200px; }

.start-steps { display: flex; gap: 24px; margin-bottom: 24px; justify-content: center; }
.step { display: flex; align-items: center; gap: 8px; color: var(--r-text-placeholder); font-size: 13px; }
.step.active, .step.done { color: var(--r-color-primary); font-weight: 500; }
.step-dot {
  width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: var(--r-bg-muted); font-size: 12px; font-weight: 600;
}
.step.active .step-dot { background: var(--r-color-primary); color: white; }
.step.done .step-dot { background: var(--r-color-success); color: white; }

.step-body { min-height: 200px; padding: 8px 0; }
.step-hint { color: var(--r-text-secondary); font-size: 13px; margin-bottom: 12px; }
.summary-list { margin: 0 0 12px 18px; color: var(--r-text-primary); font-size: 14px; line-height: 1.8; }

.form-grid { display: grid; grid-template-columns: 100px 1fr; gap: 12px; align-items: start; }
.form-label { font-size: 13px; font-weight: 500; color: var(--r-text-primary); padding-top: 8px; }
.col-span-2 { grid-column: 1 / -1; }

.pending-box { background: #fffbeb; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
.pending-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; }
.mode-hint { font-size: 12px; color: var(--r-text-secondary); }
.quota-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.quota-row span { color: var(--r-text-secondary); font-size: 13px; }

.data-table { border: 1px solid var(--r-border-divider); border-radius: 8px; overflow: hidden; }
.data-table th { background: var(--r-bg-muted); padding: 8px 12px; text-align: left; font-weight: 500; color: var(--r-text-secondary); }
.data-table td { padding: 8px 12px; border-top: 1px solid var(--r-border-divider); }
</style>
