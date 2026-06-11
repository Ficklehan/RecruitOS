<template>
  <PageShell title="渠道与账号" subtitle="管理 Boss直聘、猎聘 招聘渠道及平台账号（用于渠道招聘）">
    <RpaSafetyBar @change="onRpaStatusChange" />

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="stat-card">
        <div class="stat-value">{{ stats.activeChannels ?? 0 }}/{{ stats.totalChannels ?? 0 }}</div>
        <div class="stat-label">启用渠道</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.activeAccounts ?? 0 }}/{{ stats.totalAccounts ?? 0 }}</div>
        <div class="stat-label">活跃账号</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ accountStats.avgHealth }}%</div>
        <div class="stat-label">健康度均值</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ accountStats.todayUsage }}</div>
        <div class="stat-label">今日用量</div>
      </div>
    </div>

    <div class="rounded-xl bg-card text-card-foreground shadow-soft relative">
      <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60 rounded-lg min-h-[120px]">
        <Loader2 class="h-6 w-6 animate-spin text-primary" />
      </div>

      <RTable>
        <RTableHead>
          <RTableRow>
            <RTableTh class="w-10" />
            <RTableTh>渠道</RTableTh>
            <RTableTh class="w-[100px]">平台编码</RTableTh>
            <RTableTh class="w-[90px] text-center">账号</RTableTh>
            <RTableTh class="w-[100px] text-center">渠道状态</RTableTh>
            <RTableTh>说明</RTableTh>
            <RTableTh class="w-[160px] text-right">操作</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <template v-for="row in channelList" :key="row.id">
            <RTableRow>
              <RTableCell>
                <RButton variant="ghost" size="sm" class="h-8 w-8 p-0" @click="toggleExpand(row)">
                  <ChevronRight :class="['h-4 w-4 transition-transform', expandedKeys.includes(String(row.id)) && 'rotate-90']" />
                </RButton>
              </RTableCell>
              <RTableCell>
                <RBadge :variant="row.platformCode === 'BOSS' ? 'default' : 'secondary'">
                  {{ row.channelName }}
                </RBadge>
              </RTableCell>
              <RTableCell>{{ row.platformCode }}</RTableCell>
              <RTableCell class="text-center">{{ row.activeAccountCount }}/{{ row.accountCount }}</RTableCell>
              <RTableCell class="text-center">
                <RSwitch
                  :model-value="row.status === 'ACTIVE'"
                  @update:model-value="(v) => onChannelStatusToggle(row, v)"
                />
              </RTableCell>
              <RTableCell class="max-w-[200px] truncate">{{ row.description }}</RTableCell>
              <RTableCell class="text-right">
                <RButton variant="link" size="sm" @click="toggleExpand(row)">
                  {{ expandedKeys.includes(String(row.id)) ? '收起账号' : '展开账号' }}
                </RButton>
                <RButton variant="link" size="sm" @click="openChannelDialog(row)">编辑说明</RButton>
              </RTableCell>
            </RTableRow>
            <RTableRow v-if="expandedKeys.includes(String(row.id))">
              <RTableCell :colspan="7" class="p-0">
                <div class="expand-panel">
                  <div class="expand-toolbar">
                    <span class="expand-title">{{ row.channelName }} · 平台账号</span>
                    <RButton
                      size="sm"
                      :disabled="row.status !== 'ACTIVE'"
                      @click="openAccountDialog(row)"
                    >
                      <Plus class="mr-2 h-4 w-4" />
                      添加账号
                    </RButton>
                  </div>
                  <RTable v-if="(accountsByChannel[row.id] || []).length">
                    <RTableHead>
                      <RTableRow>
                        <RTableTh>账号名称</RTableTh>
                        <RTableTh class="w-[140px]">账号ID</RTableTh>
                        <RTableTh class="w-[88px] text-center">状态</RTableTh>
                        <RTableTh class="w-[160px]">健康度</RTableTh>
                        <RTableTh class="w-[100px] text-center">今日用量</RTableTh>
                        <RTableTh class="w-[160px]">最近活跃</RTableTh>
                        <RTableTh class="w-[100px] text-center">操作</RTableTh>
                      </RTableRow>
                    </RTableHead>
                    <RTableBody>
                      <RTableRow v-for="acc in accountsByChannel[row.id] || []" :key="acc.id">
                        <RTableCell>{{ acc.accountName }}</RTableCell>
                        <RTableCell>{{ acc.accountId }}</RTableCell>
                        <RTableCell class="text-center">
                          <RSwitch
                            :model-value="acc.status === 'ACTIVE'"
                            @update:model-value="(v) => onAccountStatusToggle(acc, v)"
                          />
                        </RTableCell>
                        <RTableCell>
                          <div class="health-cell">
                            <span class="health-value" :style="{ color: healthColor(acc.healthScore) }">
                              {{ acc.healthScore ?? '—' }}
                            </span>
                            <RProgress
                              v-if="acc.healthScore != null"
                              :value="acc.healthScore"
                              class="h-1.5 flex-1"
                            />
                          </div>
                        </RTableCell>
                        <RTableCell class="text-center">
                          {{ acc.usedToday ?? 0 }}/{{ acc.dailyLimit ?? 0 }}
                        </RTableCell>
                        <RTableCell>{{ acc.lastActiveAt }}</RTableCell>
                        <RTableCell class="text-center">
                          <RowActions :actions="getAccountRowActions(acc)" @action="(cmd) => handleAccountCommand(cmd, acc, row)" />
                        </RTableCell>
                      </RTableRow>
                    </RTableBody>
                  </RTable>
                  <p v-else class="text-sm text-muted-foreground py-4 text-center">暂无账号，点击上方添加</p>
                </div>
              </RTableCell>
            </RTableRow>
          </template>
        </RTableBody>
      </RTable>
    </div>

    <RDialog v-model:open="channelDialogVisible">
      <RDialogContent class="max-w-md">
        <RDialogHeader>
          <RDialogTitle>编辑渠道说明</RDialogTitle>
        </RDialogHeader>
        <FormField label="渠道">
          <RInput :model-value="editingChannel?.channelName" disabled />
        </FormField>
        <FormField label="说明" class="mt-4">
          <RTextarea v-model="channelForm.description" :rows="3" />
        </FormField>
        <RDialogFooter>
          <RButton variant="outline" @click="channelDialogVisible = false">取消</RButton>
          <RButton @click="saveChannelDescription">保存</RButton>
        </RDialogFooter>
      </RDialogContent>
    </RDialog>

    <RDialog v-model:open="accountDialogVisible">
      <RDialogContent class="max-w-lg">
        <RDialogHeader>
          <RDialogTitle>{{ editingAccount ? '编辑账号' : '添加账号' }}</RDialogTitle>
        </RDialogHeader>
        <FormField label="所属渠道">
          <RInput :model-value="accountChannel?.channelName" disabled />
        </FormField>
        <FormField label="账号名称" required :error="accountErrors.accountName" class="mt-4">
          <RInput v-model="accountForm.accountName" placeholder="便于识别的名称" />
        </FormField>
        <FormField label="账号ID" required :error="accountErrors.accountId" class="mt-4">
          <RInput v-model="accountForm.accountId" placeholder="平台侧账号标识" />
        </FormField>
        <FormField label="日限额" required :error="accountErrors.dailyLimit" class="mt-4">
          <NumberInput v-model="accountForm.dailyLimit" :min="1" :max="9999" class="w-full" />
        </FormField>
        <FormField label="登录方式" class="mt-4">
          <RRadioGroup v-model="accountForm.authMode" class="flex flex-col gap-2">
            <label class="flex items-center gap-2 text-sm">
              <RRadioGroupItem value="manual" />
              扫码/手动（推荐）
            </label>
            <label class="flex items-center gap-2 text-sm">
              <RRadioGroupItem value="phone" />
              手机号密码
            </label>
          </RRadioGroup>
        </FormField>
        <template v-if="accountForm.authMode === 'phone'">
          <FormField label="登录手机" class="mt-4">
            <RInput v-model="accountForm.loginPhone" />
          </FormField>
          <FormField label="登录密码" class="mt-4">
            <RInput v-model="accountForm.loginPassword" type="password" />
          </FormField>
        </template>
        <FormField label="备注" class="mt-4">
          <RTextarea v-model="accountForm.remark" :rows="2" />
        </FormField>
        <RDialogFooter>
          <RButton variant="outline" @click="accountDialogVisible = false">取消</RButton>
          <RButton @click="submitAccount">确定</RButton>
        </RDialogFooter>
      </RDialogContent>
    </RDialog>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, ChevronRight, Loader2 } from 'lucide-vue-next'
import RowActions from '@/components/common/RowActions.vue'
import RpaSafetyBar from '@/components/agent/RpaSafetyBar.vue'
import FormField from '@/components/app/FormField.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import {
  RButton, RBadge, RSwitch, RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell,
  RDialog, RDialogContent, RDialogHeader, RDialogTitle, RDialogFooter,
  RInput, RTextarea, RProgress, RRadioGroup, RRadioGroupItem,
} from '@/components/ui'
import {
  getChannelList, getChannelStats, updateChannel, updateChannelStatus,
} from '@/api/modules/channel'
import {
  getAgentAccountList, createAgentAccount, updateAgentAccount, rpaLoginAccount,
} from '@/api/modules/agent'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const rpaSimulatedMode = ref(true)
function onRpaStatusChange(s: Record<string, unknown>) {
  rpaSimulatedMode.value = s.simulatedMode !== false
}
const channelList = ref<any[]>([])
const stats = ref<Record<string, number>>({})
const accountsByChannel = ref<Record<number, any[]>>({})
const expandedKeys = ref<string[]>([])
const rpaLoadingId = ref<number | null>(null)

const channelDialogVisible = ref(false)
const editingChannel = ref<any>(null)
const channelForm = reactive({ description: '' })

const accountDialogVisible = ref(false)
const editingAccount = ref<any>(null)
const accountChannel = ref<any>(null)
const accountForm = reactive({
  accountName: '',
  accountId: '',
  dailyLimit: 300 as number | null,
  remark: '',
  authMode: 'manual',
  loginPhone: '',
  loginPassword: '',
})

const accountErrors = reactive({
  accountName: '',
  accountId: '',
  dailyLimit: '',
})

const allAccounts = computed(() =>
  Object.values(accountsByChannel.value).flat(),
)

const accountStats = computed(() => {
  const all = allAccounts.value
  return {
    avgHealth: all.length
      ? Math.round(all.reduce((s, a) => s + (a.healthScore || 0), 0) / all.length)
      : 0,
    todayUsage: all.reduce((s, a) => s + (a.usedToday || 0), 0),
    active: all.filter(a => a.status === 'ACTIVE').length,
  }
})

function healthColor(score: number) {
  if (score >= 80) return '#16a34a'
  if (score >= 60) return '#ca8a04'
  return '#DC2626'
}

function validateAccountForm(): boolean {
  accountErrors.accountName = accountForm.accountName.trim() ? '' : '请输入账号名称'
  accountErrors.accountId = accountForm.accountId.trim() ? '' : '请输入账号ID'
  accountErrors.dailyLimit = accountForm.dailyLimit != null && accountForm.dailyLimit >= 1 ? '' : '请设置日限额'
  return !accountErrors.accountName && !accountErrors.accountId && !accountErrors.dailyLimit
}

async function loadStats() {
  try {
    const res: any = await getChannelStats()
    stats.value = res.data || res || {}
  } catch { /* ignore */ }
}

async function loadChannels() {
  loading.value = true
  try {
    const res: any = await getChannelList({ pageNum: 1, pageSize: 20 })
    const data = res.data || res
    channelList.value = data.list || data.records || []
  } catch {
    channelList.value = []
  } finally {
    loading.value = false
  }
}

async function loadAccounts() {
  try {
    const res: any = await getAgentAccountList({ pageNum: 1, pageSize: 200 })
    const data = res.data || res
    const list = Array.isArray(data) ? data : data.records || []
    const map: Record<number, any[]> = {}
    for (const acc of list) {
      const cid = acc.channelId
      if (cid == null) continue
      if (!map[cid]) map[cid] = []
      map[cid].push(acc)
    }
    accountsByChannel.value = map
  } catch {
    accountsByChannel.value = {}
  }
}

async function refreshAll() {
  await Promise.all([loadChannels(), loadAccounts(), loadStats()])
}

function toggleExpand(row: any) {
  const key = String(row.id)
  if (expandedKeys.value.includes(key)) {
    expandedKeys.value = []
  } else {
    expandedKeys.value = [key]
  }
}

function expandChannelById(channelId: number) {
  expandedKeys.value = [String(channelId)]
}

function openChannelDialog(row: any) {
  editingChannel.value = row
  channelForm.description = row.description || ''
  channelDialogVisible.value = true
}

async function saveChannelDescription() {
  try {
    await updateChannel(editingChannel.value.id, { description: channelForm.description })
    toast.success('说明已更新')
    channelDialogVisible.value = false
    loadChannels()
  } catch {
    toast.error('保存失败')
  }
}

async function onChannelStatusToggle(row: any, active: boolean) {
  const newStatus = active ? 'ACTIVE' : 'DISABLED'
  row.status = newStatus
  try {
    await updateChannelStatus(row.id, newStatus)
    toast.success('渠道状态已更新')
    loadStats()
  } catch {
    toast.error('更新失败')
    loadChannels()
  }
}

function openAccountDialog(channel: any, account?: any) {
  if (channel.status !== 'ACTIVE') {
    toast.error('请先启用该渠道')
    return
  }
  accountChannel.value = channel
  editingAccount.value = account || null
  Object.assign(accountErrors, { accountName: '', accountId: '', dailyLimit: '' })
  if (account) {
    accountForm.accountName = account.accountName
    accountForm.accountId = account.accountId
    accountForm.dailyLimit = account.dailyLimit
    accountForm.remark = account.remark || ''
  } else {
    accountForm.accountName = ''
    accountForm.accountId = ''
    accountForm.dailyLimit = 300
    accountForm.remark = ''
  }
  accountForm.authMode = 'manual'
  accountForm.loginPhone = ''
  accountForm.loginPassword = ''
  accountDialogVisible.value = true
}

async function submitAccount() {
  if (!validateAccountForm()) return
  const ch = accountChannel.value
  try {
    const payload = {
      channelId: ch.id,
      platform: ch.platformCode,
      accountName: accountForm.accountName,
      accountId: accountForm.accountId,
      dailyLimit: accountForm.dailyLimit,
      remark: accountForm.remark,
      authMode: accountForm.authMode,
      loginPhone: accountForm.loginPhone,
      loginPassword: accountForm.loginPassword || undefined,
    }
    if (editingAccount.value) {
      await updateAgentAccount(editingAccount.value.id, payload)
      toast.success('账号已更新')
    } else {
      await createAgentAccount(payload)
      toast.success('账号已添加')
    }
    accountDialogVisible.value = false
    await refreshAll()
    expandChannelById(ch.id)
  } catch {
    toast.error('操作失败')
  }
}

async function handleRpaLogin(acc: any) {
  if (rpaSimulatedMode.value) {
    toast.error('测试模式未开启平台访问，无法登录 Boss/猎聘')
    return
  }
  const ok = await confirm({
    title: '平台登录',
    message: '将打开浏览器完成 Boss/猎聘 登录，请在 5 分钟内完成扫码或验证。',
    confirmText: '开始登录',
  })
  if (!ok) return
  rpaLoadingId.value = acc.id
  try {
    const res: any = await rpaLoginAccount(acc.id)
    toast.success(res.data?.message || '登录成功')
    await loadAccounts()
  } catch {
    toast.error('登录失败')
  } finally {
    rpaLoadingId.value = null
  }
}

async function onAccountStatusToggle(acc: any, active: boolean) {
  const newStatus = active ? 'ACTIVE' : 'INACTIVE'
  acc.status = newStatus
  try {
    await updateAgentAccount(acc.id, { status: newStatus })
    toast.success('账号状态已更新')
    loadStats()
  } catch {
    toast.error('更新失败')
    loadAccounts()
  }
}

function getAccountRowActions(acc: any) {
  return [
    { command: 'edit', label: '编辑', icon: 'Edit', type: 'primary' },
    { command: 'login', label: '平台登录', icon: 'Setting' },
    { command: 'log', label: '查看日志', icon: 'View' },
  ]
}

function handleAccountCommand(cmd: string, acc: any, channel: any) {
  if (cmd === 'edit') openAccountDialog(channel, acc)
  else if (cmd === 'login') handleRpaLogin(acc)
  else if (cmd === 'log') router.push({ path: '/talent/channels/logs', query: { accountId: String(acc.id) } })
}

onMounted(async () => {
  await refreshAll()
  const qid = route.query.channelId
  if (qid) {
    expandChannelById(Number(qid))
    router.replace({ path: '/talent/channels' })
  }
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: $text-secondary;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  padding: 20px;
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: $text-primary;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 4px;
}

.expand-panel {
  padding: 8px 48px 16px 56px;
  background: $bg-warm;
}

.expand-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.expand-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.health-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.health-value {
  font-size: 13px;
  font-weight: 600;
  min-width: 28px;
}
</style>
