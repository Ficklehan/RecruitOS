<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">渠道与账号</h2>
        <p class="page-subtitle">管理 Boss直聘、猎聘 招聘渠道及平台账号（用于渠道招聘）</p>
      </div>
    </div>

    <div class="stats-row">
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

    <div class="data-card">
      <el-table
        :data="channelList"
        v-loading="loading"
        row-key="id"
        :expand-row-keys="expandedKeys"
        @expand-change="onExpandChange"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-panel">
              <div class="expand-toolbar">
                <span class="expand-title">{{ row.channelName }} · 平台账号</span>
                <el-button
                  type="primary"
                  size="small"
                  :disabled="row.status !== 'ACTIVE'"
                  @click="openAccountDialog(row)"
                >
                  <el-icon><Plus /></el-icon>
                  添加账号
                </el-button>
              </div>
              <el-table
                :data="accountsByChannel[row.id] || []"
                size="small"
                stripe
                empty-text="暂无账号，点击上方添加"
              >
                <el-table-column prop="accountName" label="账号名称" min-width="140" show-overflow-tooltip />
                <el-table-column prop="accountId" label="账号ID" width="140" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="88" align="center">
                  <template #default="{ row: acc }">
                    <el-switch
                      v-model="acc.status"
                      active-value="ACTIVE"
                      inactive-value="INACTIVE"
                      inline-prompt
                      active-text="启"
                      inactive-text="停"
                      @change="handleAccountStatusChange(acc)"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="健康度" width="160">
                  <template #default="{ row: acc }">
                    <div class="health-cell">
                      <span class="health-value" :style="{ color: healthColor(acc.healthScore) }">
                        {{ acc.healthScore ?? '—' }}
                      </span>
                      <el-progress
                        v-if="acc.healthScore != null"
                        :percentage="acc.healthScore"
                        :stroke-width="6"
                        :show-text="false"
                        :color="healthColor(acc.healthScore)"
                        style="flex: 1"
                      />
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="今日用量" width="100" align="center">
                  <template #default="{ row: acc }">
                    {{ acc.usedToday ?? 0 }}/{{ acc.dailyLimit ?? 0 }}
                  </template>
                </el-table-column>
                <el-table-column prop="lastActiveAt" label="最近活跃" width="160" />
                <el-table-column label="操作" width="240" fixed="right">
                  <template #default="{ row: acc }">
                    <el-button
                      type="success" link size="small"
                      :loading="rpaLoadingId === acc.id"
                      :disabled="acc.status !== 'ACTIVE'"
                      @click="handleRpaLogin(acc)"
                    >登录平台</el-button>
                    <el-button type="primary" link size="small" @click="openAccountDialog(row, acc)">编辑</el-button>
                    <el-button type="primary" link size="small" @click="viewLog(acc)">日志</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="channelName" label="渠道" min-width="120">
          <template #default="{ row }">
            <el-tag :type="platformTag(row.platformCode)" size="small">{{ row.channelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="platformCode" label="平台编码" width="100" />
        <el-table-column label="账号" width="90" align="center">
          <template #default="{ row }">
            {{ row.activeAccountCount }}/{{ row.accountCount }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="渠道状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="ACTIVE"
              inactive-value="DISABLED"
              @change="handleChannelStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="toggleExpand(row)">
              {{ expandedKeys.includes(String(row.id)) ? '收起账号' : '展开账号' }}
            </el-button>
            <el-button type="primary" link size="small" @click="openChannelDialog(row)">编辑说明</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 渠道说明 -->
    <el-dialog v-model="channelDialogVisible" title="编辑渠道说明" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="渠道">
          <el-input :model-value="editingChannel?.channelName" disabled />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="channelForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveChannelDescription">保存</el-button>
      </template>
    </el-dialog>

    <!-- 账号表单 -->
    <el-dialog
      v-model="accountDialogVisible"
      :title="editingAccount ? '编辑账号' : '添加账号'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="accountFormRef" :model="accountForm" :rules="accountRules" label-width="100px">
        <el-form-item label="所属渠道">
          <el-input :model-value="accountChannel?.channelName" disabled />
        </el-form-item>
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="accountForm.accountName" placeholder="便于识别的名称" />
        </el-form-item>
        <el-form-item label="账号ID" prop="accountId">
          <el-input v-model="accountForm.accountId" placeholder="平台侧账号标识" />
        </el-form-item>
        <el-form-item label="日限额" prop="dailyLimit">
          <el-input-number v-model="accountForm.dailyLimit" :min="1" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="登录方式">
          <el-radio-group v-model="accountForm.authMode">
            <el-radio value="manual">扫码/手动（推荐）</el-radio>
            <el-radio value="phone">手机号密码</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="accountForm.authMode === 'phone'">
          <el-form-item label="登录手机">
            <el-input v-model="accountForm.loginPhone" />
          </el-form-item>
          <el-form-item label="登录密码">
            <el-input v-model="accountForm.loginPassword" type="password" show-password />
          </el-form-item>
        </template>
        <el-form-item label="备注">
          <el-input v-model="accountForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAccount">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getChannelList, getChannelStats, updateChannel, updateChannelStatus,
} from '@/api/modules/channel'
import {
  getAgentAccountList, createAgentAccount, updateAgentAccount, rpaLoginAccount,
} from '@/api/modules/agent'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const channelList = ref<any[]>([])
const stats = ref<Record<string, number>>({})
const accountsByChannel = ref<Record<number, any[]>>({})
const expandedKeys = ref<string[]>([])
const rpaLoadingId = ref<number | null>(null)

const channelDialogVisible = ref(false)
const editingChannel = ref<any>(null)
const channelForm = reactive({ description: '' })

const accountDialogVisible = ref(false)
const accountFormRef = ref<FormInstance>()
const editingAccount = ref<any>(null)
const accountChannel = ref<any>(null)
const accountForm = reactive({
  accountName: '',
  accountId: '',
  dailyLimit: 300,
  remark: '',
  authMode: 'manual',
  loginPhone: '',
  loginPassword: '',
})

const accountRules: FormRules = {
  accountName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
  accountId: [{ required: true, message: '请输入账号ID', trigger: 'blur' }],
  dailyLimit: [{ required: true, message: '请设置日限额', trigger: 'change' }],
}

const allAccounts = computed(() =>
  Object.values(accountsByChannel.value).flat(),
)

const accountStats = computed(() => {
  const all = allAccounts.value
  const active = all.filter(a => a.status === 'ACTIVE')
  return {
    avgHealth: all.length
      ? Math.round(all.reduce((s, a) => s + (a.healthScore || 0), 0) / all.length)
      : 0,
    todayUsage: all.reduce((s, a) => s + (a.usedToday || 0), 0),
    active: active.length,
  }
})

function platformTag(code: string) {
  return code === 'BOSS' ? '' : 'info'
}

function healthColor(score: number) {
  if (score >= 80) return '#059669'
  if (score >= 60) return '#D97706'
  return '#DC2626'
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

function onExpandChange(row: any, expanded: any[]) {
  expandedKeys.value = expanded.map(r => String(r.id))
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
    ElMessage.success('说明已更新')
    channelDialogVisible.value = false
    loadChannels()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function handleChannelStatusChange(row: any) {
  try {
    await updateChannelStatus(row.id, row.status)
    ElMessage.success('渠道状态已更新')
    loadStats()
  } catch {
    ElMessage.error('更新失败')
    loadChannels()
  }
}

function openAccountDialog(channel: any, account?: any) {
  if (channel.status !== 'ACTIVE') {
    ElMessage.warning('请先启用该渠道')
    return
  }
  accountChannel.value = channel
  editingAccount.value = account || null
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
  await accountFormRef.value?.validate()
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
      ElMessage.success('账号已更新')
    } else {
      await createAgentAccount(payload)
      ElMessage.success('账号已添加')
    }
    accountDialogVisible.value = false
    await refreshAll()
    expandChannelById(ch.id)
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleRpaLogin(acc: any) {
  await ElMessageBox.confirm(
    '将打开浏览器完成 Boss/猎聘 登录，请在 5 分钟内完成扫码或验证。',
    '平台登录',
    { confirmButtonText: '开始登录', cancelButtonText: '取消', type: 'info' },
  ).catch(() => Promise.reject())
  rpaLoadingId.value = acc.id
  try {
    const res: any = await rpaLoginAccount(acc.id)
    ElMessage.success(res.data?.message || '登录成功')
    await loadAccounts()
  } catch {
    ElMessage.error('登录失败')
  } finally {
    rpaLoadingId.value = null
  }
}

async function handleAccountStatusChange(acc: any) {
  try {
    await updateAgentAccount(acc.id, { status: acc.status })
    ElMessage.success('账号状态已更新')
    loadStats()
  } catch {
    ElMessage.error('更新失败')
    loadAccounts()
  }
}

function viewLog(acc: any) {
  router.push({ path: '/talent/channels/logs', query: { accountId: String(acc.id) } })
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
  background: #fafbfc;
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
