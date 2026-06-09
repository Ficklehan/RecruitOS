<template>
  <div class="job-sourcing" v-loading="loading">
    <div class="toolbar">
      <el-button type="primary" :disabled="!canStart" @click="openStartWizard">
        <el-icon><VideoPlay /></el-icon>
        {{ ACTIONS.startPlatformTask }}
      </el-button>
      <el-button v-if="activeCampaign" @click="refresh">刷新</el-button>
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

      <el-alert
        v-if="activeCampaign.status === 'RUNNING' && activeCampaign.opsPackVersion"
        type="info"
        :closable="false"
        show-icon
        class="mb-12"
        :title="`本任务使用招人方式 v${activeCampaign.opsPackVersion}（启动时锁定）`"
      />

      <h4>平台执行</h4>
      <el-table :data="activeCampaign.platformRuns || []" size="small" class="mb-16">
        <el-table-column prop="platform" label="平台" width="100" />
        <el-table-column prop="primaryAccountName" label="主账号" />
        <el-table-column prop="currentStep" label="当前步骤" width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="平台职位">
          <template #default="{ row }">
            <a v-if="row.platformJobUrl" :href="row.platformJobUrl" target="_blank">查看</a>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="pendingActions.length" class="pending-box">
        <h4>待你确认</h4>
        <div v-for="item in pendingActions" :key="item.id" class="pending-item">
          <span>{{ item.candidateName }} · {{ item.platform }}</span>
          <div>
            <el-button v-if="item.traceStatus === 'PENDING_GREET_CONFIRM'" size="small" type="primary"
              @click="confirmGreet(item.id)">{{ ACTIONS.confirmGreet }}</el-button>
            <el-button v-if="item.traceStatus === 'PENDING_IMPORT'" size="small" type="primary"
              @click="confirmImport(item.id)">{{ ACTIONS.importCandidate }}</el-button>
          </div>
        </div>
        <el-button v-if="needsPublishConfirm" size="small" class="mt-8" @click="confirmPublish">确认发布到平台</el-button>
      </div>

      <h4>候选人轨迹</h4>
      <el-table :data="traces" size="small">
        <el-table-column prop="candidateName" label="姓名" />
        <el-table-column prop="platform" label="平台" width="90" />
        <el-table-column prop="lockedByAccountName" label="招聘账号" />
        <el-table-column prop="traceStatus" label="状态" width="140">
          <template #default="{ row }">{{ traceStatusLabel(row.traceStatus) }}</template>
        </el-table-column>
        <el-table-column prop="screenStage" label="筛选阶段" width="110">
          <template #default="{ row }">{{ screenStageLabel(row.screenStage) }}</template>
        </el-table-column>
        <el-table-column label="说明" min-width="180">
          <template #default="{ row }">{{ row.skipReasonSummary || row.skipReason || '—' }}</template>
        </el-table-column>
        <el-table-column label="联系策略" width="130">
          <template #default="{ row }">{{ greetStrategyLabel(row.greetStrategyApplied) }}</template>
        </el-table-column>
        <el-table-column label="匹配建议" min-width="160">
          <template #default="{ row }">
            <MatchVerdict
              :match-score="row.matchScore"
              :match-detail="row.matchDetail"
              mode="compact"
              :show-score="false"
            />
          </template>
        </el-table-column>
      </el-table>

      <div class="actions mt-12">
        <el-button v-if="activeCampaign.status === 'RUNNING'" @click="pauseCampaign">暂停</el-button>
        <el-button v-if="activeCampaign.status === 'PAUSED'" type="primary" @click="resumeCampaign">恢复</el-button>
        <el-button @click="$router.push({ path: `/planning/jobs/${jobId}`, query: { tab: 'candidates' } })">
          查看在招候选人
        </el-button>
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

    <el-dialog v-model="showStart" title="开启平台招人任务" width="640px" destroy-on-close @open="onStartDialogOpen">
      <el-steps :active="startStep" finish-status="success" align-center class="start-steps">
        <el-step title="确认招人方式" />
        <el-step title="选择平台" />
        <el-step title="运行方式" />
      </el-steps>

      <div v-show="startStep === 0" class="step-body">
        <template v-if="startOpsPack">
          <p class="step-hint">将使用以下已确认的招人方式（仅绑定到新任务）：</p>
          <ul class="summary-list">
            <li v-for="(line, i) in startOpsSummary" :key="i">{{ line }}</li>
          </ul>
          <el-tag type="success">v{{ startOpsPack.version }} 已生效</el-tag>
        </template>
        <EmptyStateCta
          v-else
          title="尚未确认招人方式"
          description="请先在职位工作台的「规则 → 招人方式」中生成并确认发布。"
          :actions="[{ label: '去设置', type: 'primary', onClick: goSourcingMethod }]"
        />
      </div>

      <div v-show="startStep === 1" class="step-body">
        <el-form label-width="110px">
          <el-form-item label="任务名称">
            <el-input v-model="startForm.name" :placeholder="jobTitle + ' 平台招人'" />
          </el-form-item>
          <el-form-item label="搜寻来源">
            <el-select v-model="startForm.searchSource" style="width: 200px">
              <el-option label="推荐人才" value="RECOMMEND" />
              <el-option label="主动搜索" value="SEARCH" />
              <el-option label="最新活跃" value="LATEST" />
            </el-select>
          </el-form-item>
          <el-form-item label="日配额">
            <div class="quota-row">
              <span>Boss</span>
              <el-input-number v-model="startForm.platformQuotas.BOSS" :min="1" :max="200" size="small" />
              <span>猎聘</span>
              <el-input-number v-model="startForm.platformQuotas.LIEPIN" :min="1" :max="200" size="small" />
            </div>
          </el-form-item>
          <el-form-item label="Boss直聘">
            <el-checkbox v-model="startForm.boss.enabled">启用</el-checkbox>
            <el-select v-model="startForm.boss.primaryId" placeholder="主账号" style="width: 200px; margin-left: 12px">
              <el-option v-for="a in bossAccounts" :key="a.id" :label="a.accountName" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="猎聘">
            <el-checkbox v-model="startForm.liepin.enabled">启用</el-checkbox>
            <el-select v-model="startForm.liepin.primaryId" placeholder="主账号" style="width: 200px; margin-left: 12px">
              <el-option v-for="a in liepinAccounts" :key="a.id" :label="a.accountName" :value="a.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <div v-show="startStep === 2" class="step-body">
        <el-form label-width="110px">
          <el-form-item label="运行方式" required>
            <el-radio-group v-model="startForm.mode">
              <el-radio label="SEMI_AUTO">{{ runModeLabel('SEMI_AUTO') }}</el-radio>
              <el-radio label="PUBLISH_SEARCH_ONLY">{{ runModeLabel('PUBLISH_SEARCH_ONLY') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-collapse>
            <el-collapse-item title="高级选项" name="adv">
              <el-radio-group v-model="startForm.mode">
                <el-radio label="FULL_AUTO" :disabled="!enableFullAuto || !tenantSafety.allowFullAuto">
                  {{ runModeLabel('FULL_AUTO') }}
                </el-radio>
              </el-radio-group>
              <div v-if="tenantSafety.allowFullAuto" class="mode-hint">
                <el-checkbox v-model="enableFullAuto">我确认启用全自动</el-checkbox>
              </div>
              <p v-else class="mode-hint">全自动运行未在租户设置中开启</p>
              <el-form-item label="联系策略" class="mt-12">
                <el-radio-group v-model="startForm.greetStrategy">
                  <el-radio label="SCREEN_THEN_GREET">{{ greetStrategyLabel('SCREEN_THEN_GREET') }}</el-radio>
                  <el-radio label="COLLECT_ONLY">{{ greetStrategyLabel('COLLECT_ONLY') }}</el-radio>
                  <el-radio label="CARD_GREET" :disabled="!allowCardGreet">
                    {{ greetStrategyLabel('CARD_GREET') }}
                  </el-radio>
                </el-radio-group>
                <p v-if="!allowCardGreet" class="mode-hint">「卡片即联系」未在租户设置中开启</p>
                <div v-if="startForm.greetStrategy === 'CARD_GREET'" class="mode-hint">
                  <el-checkbox v-model="startForm.cardGreetRiskAccepted">
                    我了解该方式存在误触风险，适用于批量初级岗
                  </el-checkbox>
                </div>
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showStart = false">取消</el-button>
        <el-button v-if="startStep > 0" @click="startStep--">上一步</el-button>
        <el-button v-if="startStep < 2" type="primary" :disabled="startStep === 0 && !startOpsPack" @click="startStep++">
          下一步
        </el-button>
        <el-button v-else type="primary" :loading="starting" @click="handleStart">开始招人</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
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
    ElMessage.warning('全自动模式需勾选确认开关')
    return
  }
  if (startForm.greetStrategy === 'CARD_GREET' && !startForm.cardGreetRiskAccepted) {
    ElMessage.warning('请勾选高风险联系方式的确认项')
    return
  }
  const platformConfigs = buildPlatformConfigs()
  if (!platformConfigs.length) {
    ElMessage.warning('请至少启用一个平台并选择主账号')
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
        ElMessage.warning('请先在「规则 → 招人方式」中确认发布')
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
    ElMessage.success('平台招人任务已开启')
    await loadCampaign()
  } catch {
    ElMessage.error('启动失败')
  } finally {
    starting.value = false
  }
}

async function pauseCampaign() {
  if (!activeCampaign.value) return
  await pauseWorkflow(activeCampaign.value.id)
  ElMessage.success('已暂停')
  loadCampaign()
}

async function resumeCampaign() {
  if (!activeCampaign.value) return
  await resumeWorkflow(activeCampaign.value.id)
  ElMessage.success('已恢复')
  loadCampaign()
}

async function confirmGreet(traceId: number) {
  await confirmWorkflowGreet(traceId)
  ElMessage.success('已发送打招呼')
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
  ElMessage.success('已确认发布')
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
