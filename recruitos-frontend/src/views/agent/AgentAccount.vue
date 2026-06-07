<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">平台账号</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        添加账号
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-total">
        <div class="stat-icon"><el-icon :size="28"><User /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总账号数</div>
        </div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-icon"><el-icon :size="28"><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.active }}</div>
          <div class="stat-label">活跃数</div>
        </div>
      </div>
      <div class="stat-card stat-health">
        <div class="stat-icon"><el-icon :size="28"><FirstAidKit /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgHealth }}%</div>
          <div class="stat-label">健康度均值</div>
        </div>
      </div>
      <div class="stat-card stat-usage">
        <div class="stat-icon"><el-icon :size="28"><DataLine /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayUsage }}</div>
          <div class="stat-label">今日使用量</div>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="data-card">
      <el-table :data="pagedAccounts" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="platform" label="平台" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getPlatformTag(row.platform)" size="small" disable-transitions>
              {{ row.platform }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="账号名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="accountId" label="账号ID" width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="ACTIVE"
              inactive-value="INACTIVE"
              inline-prompt
              active-text="启"
              inactive-text="停"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="健康度" width="200">
          <template #default="{ row }">
            <el-tooltip placement="top" :show-after="300">
              <template #content>
                <div class="health-tooltip">
                  <div>登录状态: {{ row.healthDetail.login }}</div>
                  <div>响应速度: {{ row.healthDetail.speed }}</div>
                  <div>操作成功率: {{ row.healthDetail.successRate }}</div>
                  <div>风控评分: {{ row.healthDetail.riskScore }}</div>
                </div>
              </template>
              <div class="health-cell">
                <span class="health-value" :style="{ color: getHealthColor(row.healthScore) }">
                  {{ row.healthScore }}
                </span>
                <el-progress
                  :percentage="row.healthScore"
                  :stroke-width="8"
                  :show-text="false"
                  :color="getHealthColor(row.healthScore)"
                  style="flex: 1"
                />
              </div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="今日用量" width="140" align="center">
          <template #default="{ row }">
            <span>{{ row.usedToday }}/{{ row.dailyLimit }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastActiveAt" label="最近活跃" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              link size="small"
              @click="toggleAccountStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '暂停' : '启用' }}
            </el-button>
            <el-button type="primary" link size="small" @click="handleViewLog(row)">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="accounts.length"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingAccount ? '编辑账号' : '添加账号'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="平台" prop="platform">
          <el-select v-model="formData.platform" placeholder="请选择平台" style="width: 100%">
            <el-option label="BOSS" value="BOSS" />
            <el-option label="LAGOU" value="LAGOU" />
            <el-option label="ZHILIAN" value="ZHILIAN" />
            <el-option label="LIEPIN" value="LIEPIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="formData.accountName" placeholder="请输入账号名称" />
        </el-form-item>
        <el-form-item label="账号ID" prop="accountId">
          <el-input v-model="formData.accountId" placeholder="请输入平台账号ID" />
        </el-form-item>
        <el-form-item label="日限额" prop="dailyLimit">
          <el-input-number v-model="formData.dailyLimit" :min="1" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Plus, User, CircleCheck, FirstAidKit, DataLine,
} from '@element-plus/icons-vue'
import { getAgentAccountList, createAgentAccount, updateAgentAccount, deleteAgentAccount } from '@/api/modules/agent'

const router = useRouter()

// 分页
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 数据
const accounts = ref<any[]>([])

// 分页数据
const pagedAccounts = computed(() => accounts.value)

// 统计
const stats = computed(() => {
  const all = accounts.value
  const activeList = all.filter(a => a.status === 'ACTIVE')
  return {
    total: total.value || all.length,
    active: activeList.length,
    avgHealth: all.length ? Math.round(all.reduce((s, a) => s + (a.healthScore || 0), 0) / all.length) : 0,
    todayUsage: all.reduce((s, a) => s + (a.usedToday || 0), 0),
  }
})

async function loadData() {
  try {
    const res: any = await getAgentAccountList({ page: pageNum.value, pageSize: pageSize.value })
    const data = res.data || res
    accounts.value = Array.isArray(data) ? data : data.records || []
    total.value = data.total || accounts.value.length
  } catch {
    accounts.value = []
  }
}

// 对话框
const dialogVisible = ref(false)
const editingAccount = ref<any>(null)
const formRef = ref<FormInstance>()
const formData = reactive({
  platform: '',
  accountName: '',
  accountId: '',
  dailyLimit: 300,
  remark: '',
})

const formRules: FormRules = {
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
  accountName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
  accountId: [{ required: true, message: '请输入账号ID', trigger: 'blur' }],
  dailyLimit: [{ required: true, message: '请设置日限额', trigger: 'change' }],
}

// 标签映射
function getPlatformTag(platform: string) {
  const map: Record<string, string> = { BOSS: '', LAGOU: 'success', ZHILIAN: 'warning', LIEPIN: 'info' }
  return map[platform] || 'info'
}

function getHealthColor(score: number) {
  if (score >= 80) return '#059669'
  if (score >= 60) return '#D97706'
  return '#DC2626'
}

// 操作
function openDialog(account?: any) {
  if (account) {
    editingAccount.value = account
    formData.platform = account.platform
    formData.accountName = account.accountName
    formData.accountId = account.accountId
    formData.dailyLimit = account.dailyLimit
    formData.remark = ''
  } else {
    editingAccount.value = null
    formData.platform = ''
    formData.accountName = ''
    formData.accountId = ''
    formData.dailyLimit = 300
    formData.remark = ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      if (editingAccount.value) {
        await updateAgentAccount(editingAccount.value.id, {
          platform: formData.platform,
          accountName: formData.accountName,
          accountId: formData.accountId,
          dailyLimit: formData.dailyLimit,
          remark: formData.remark,
        })
        ElMessage.success('账号更新成功')
      } else {
        await createAgentAccount({
          platform: formData.platform,
          accountName: formData.accountName,
          accountId: formData.accountId,
          dailyLimit: formData.dailyLimit,
          remark: formData.remark,
        })
        ElMessage.success('账号添加成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {
      ElMessage.error('操作失败')
    }
  })
}

async function handleStatusChange(row: any) {
  try {
    await updateAgentAccount(row.id, { status: row.status })
    ElMessage.success(`账号已${row.status === 'ACTIVE' ? '启用' : '暂停'}`)
  } catch {
    ElMessage.error('状态更新失败')
    loadData()
  }
}

function toggleAccountStatus(row: any) {
  const action = row.status === 'ACTIVE' ? '暂停' : '启用'
  ElMessageBox.confirm(`确定要${action}该账号吗？`, `${action}确认`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      await updateAgentAccount(row.id, { status: newStatus })
      row.status = newStatus
      ElMessage.success(`账号已${action}`)
    } catch {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

function handleViewLog(row: any) {
  router.push({ path: '/talent/channel/log', query: { accountId: row.accountId } })
}

onMounted(() => { loadData() })
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-total .stat-icon { background: linear-gradient(135deg, $primary-color, $primary-light); }
.stat-active .stat-icon { background: linear-gradient(135deg, $success-color, $success-color); }
.stat-health .stat-icon { background: linear-gradient(135deg, $warning-color, $warning-color); }
.stat-usage .stat-icon { background: linear-gradient(135deg, $text-secondary, #a6a9ad); }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 4px;
}

.health-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.health-value {
  font-size: 14px;
  font-weight: 600;
  min-width: 32px;
}

.health-tooltip {
  font-size: 12px;
  line-height: 1.8;
}
</style>
