<template>
  <div class="job-sourcing" v-loading="loading">
    <div class="toolbar">
      <el-button type="primary" :disabled="!canStart" @click="showStart = true">
        <el-icon><VideoPlay /></el-icon>
        开始渠道招聘
      </el-button>
      <el-button v-if="activeCampaign" @click="refresh">刷新</el-button>
    </div>

    <template v-if="activeCampaign">
      <div class="stats-row">
        <div class="stat"><b>{{ stats.published || 0 }}</b><span>已发布</span></div>
        <div class="stat"><b>{{ stats.searched || 0 }}</b><span>已检索</span></div>
        <div class="stat"><b>{{ stats.greeted || 0 }}</b><span>已打招呼</span></div>
        <div class="stat"><b>{{ stats.resumes || 0 }}</b><span>已收简历</span></div>
        <div class="stat"><b>{{ stats.imported || 0 }}</b><span>已加入候选人</span></div>
        <div class="stat"><b>{{ stats.pendingScreening || 0 }}</b><span>待初筛</span></div>
      </div>

      <el-alert v-if="activeCampaign.status === 'RUNNING'" type="success" :closable="false" show-icon
        :title="`活动运行中 · ${modeLabel(activeCampaign.mode)}`" class="mb-12" />

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
        <h4>待人工确认</h4>
        <div v-for="item in pendingActions" :key="item.id" class="pending-item">
          <span>{{ item.candidateName }} · {{ item.platform }} · {{ item.traceStatus }}</span>
          <div>
            <el-button v-if="item.traceStatus === 'PENDING_GREET_CONFIRM'" size="small" type="primary"
              @click="confirmGreet(item.id)">确认打招呼</el-button>
            <el-button v-if="item.traceStatus === 'PENDING_IMPORT'" size="small" type="primary"
              @click="confirmImport(item.id)">确认加入候选人</el-button>
          </div>
        </div>
        <el-button v-if="needsPublishConfirm" size="small" class="mt-8" @click="confirmPublish">确认发布到平台</el-button>
      </div>

      <h4>候选人轨迹</h4>
      <el-table :data="traces" size="small">
        <el-table-column prop="candidateName" label="姓名" />
        <el-table-column prop="platform" label="平台" width="90" />
        <el-table-column prop="lockedByAccountName" label="招聘账号" />
        <el-table-column prop="traceStatus" label="状态" width="140" />
        <el-table-column prop="skipReason" label="备注" />
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
        <el-button @click="$router.push({ path: '/pipeline/board', query: { jobId: jobId } })">查看招聘进展</el-button>
      </div>
    </template>

    <EmptyStateCta
      v-else
      title="尚未开始渠道招聘"
      description="配置 Boss直聘 / 猎聘账号后，可自动发布职位并搜寻候选人"
      :actions="[
        { label: '配置招聘账号', type: 'default', onClick: () => $router.push('/talent/channels') },
        { label: '开始渠道招聘', type: 'primary', onClick: () => showStart = true },
      ]"
    />

    <el-dialog v-model="showStart" title="开始渠道招聘" width="640px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="活动名称">
          <el-input v-model="startForm.name" :placeholder="jobTitle + ' 渠道招聘'" />
        </el-form-item>
        <el-form-item label="运行模式" required>
          <el-radio-group v-model="startForm.mode">
            <el-radio label="SEMI_AUTO">半自动（默认，系统仅建议）</el-radio>
            <el-radio label="PUBLISH_SEARCH_ONLY">仅发布+搜索</el-radio>
            <el-radio label="FULL_AUTO" :disabled="!enableFullAuto">全自动</el-radio>
          </el-radio-group>
          <div class="mode-hint">
            <el-checkbox v-model="enableFullAuto">
              我确认启用全自动执行（打招呼、加入候选人等将无需逐步确认）
            </el-checkbox>
          </div>
        </el-form-item>
        <el-form-item label="Boss直聘">
          <el-checkbox v-model="startForm.boss.enabled">启用</el-checkbox>
          <el-select v-model="startForm.boss.primaryId" placeholder="主账号" style="width: 200px; margin-left: 12px">
            <el-option v-for="a in bossAccounts" :key="a.id" :label="a.accountName" :value="a.id" />
          </el-select>
          <el-select v-model="startForm.boss.auxIds" multiple placeholder="辅账号(可选)" style="width: 220px; margin-left: 8px">
            <el-option v-for="a in bossAccounts" :key="'aux'+a.id" :label="a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="猎聘">
          <el-checkbox v-model="startForm.liepin.enabled">启用</el-checkbox>
          <el-select v-model="startForm.liepin.primaryId" placeholder="主账号" style="width: 200px; margin-left: 12px">
            <el-option v-for="a in liepinAccounts" :key="a.id" :label="a.accountName" :value="a.id" />
          </el-select>
          <el-select v-model="startForm.liepin.auxIds" multiple placeholder="辅账号(可选)" style="width: 220px; margin-left: 8px">
            <el-option v-for="a in liepinAccounts" :key="'aux'+a.id" :label="a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStart = false">取消</el-button>
        <el-button type="primary" :loading="starting" @click="handleStart">启动</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import MatchVerdict from '@/components/match/MatchVerdict.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import {
  createWorkflow, getWorkflowList, getWorkflowDetail, pauseWorkflow, resumeWorkflow,
  getWorkflowCandidates, confirmWorkflowGreet, confirmWorkflowImport, confirmWorkflowPublish,
  getAgentAccountList,
} from '@/api/modules/agent'

const props = defineProps<{ jobId: number; jobTitle: string; jobStatus: string }>()

const loading = ref(false)
const starting = ref(false)
const showStart = ref(false)
const activeCampaign = ref<any>(null)
const traces = ref<any[]>([])
const bossAccounts = ref<any[]>([])
const liepinAccounts = ref<any[]>([])

const enableFullAuto = ref(false)

const startForm = reactive({
  name: '',
  mode: 'SEMI_AUTO',
  boss: { enabled: true, primaryId: null as number | null, auxIds: [] as number[] },
  liepin: { enabled: true, primaryId: null as number | null, auxIds: [] as number[] },
})

const canStart = computed(() => props.jobStatus === 'ACTIVE')
const stats = computed(() => activeCampaign.value?.stats || {})
const pendingActions = computed(() => activeCampaign.value?.pendingActions || [])
const needsPublishConfirm = computed(() =>
  activeCampaign.value?.publishConfirmRequired && pendingActions.value.some((p: any) => p.traceStatus === 'PUBLISH_PENDING'))

function modeLabel(mode: string) {
  return { FULL_AUTO: '全自动', SEMI_AUTO: '半自动', PUBLISH_SEARCH_ONLY: '仅发布+搜索' }[mode] || mode
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
  const platformConfigs = buildPlatformConfigs()
  if (!platformConfigs.length) {
    ElMessage.warning('请至少启用一个平台并选择主账号')
    return
  }
  starting.value = true
  try {
    await createWorkflow({
      name: startForm.name || `${props.jobTitle} 渠道招聘`,
      jobId: props.jobId,
      mode: startForm.mode,
      platformConfigs,
      platforms: platformConfigs.map(p => p.platform),
    })
    showStart.value = false
    ElMessage.success('渠道招聘已启动')
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
  ElMessage.success('已加入候选人')
  loadCampaign()
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
.mt-12 { margin-top: 12px; }
.pending-box { background: #fffbeb; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
.pending-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; }
.mode-hint { margin-top: 8px; font-size: 12px; color: #64748b; }
</style>
