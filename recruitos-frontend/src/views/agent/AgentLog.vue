<template>
  <div class="page-container page-stack">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">渠道运行日志</h2>
        <p class="page-subtitle">查看自动招聘任务在 Boss、猎聘等渠道的执行记录</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-ops">
        <div class="stat-icon"><el-icon :size="28"><Operation /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayOps }}</div>
          <div class="stat-label">今日操作数</div>
        </div>
      </div>
      <div class="stat-card stat-success">
        <div class="stat-icon"><el-icon :size="28"><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.successRate }}%</div>
          <div class="stat-label">成功率</div>
        </div>
      </div>
      <div class="stat-card stat-delay">
        <div class="stat-icon"><el-icon :size="28"><Timer /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgDelay }}ms</div>
          <div class="stat-label">平均延迟</div>
        </div>
      </div>
      <div class="stat-card stat-error">
        <div class="stat-icon"><el-icon :size="28"><WarningFilled /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.errorCount }}</div>
          <div class="stat-label">异常数</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-date-picker
        v-model="queryParams.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 280px"
        value-format="YYYY-MM-DD"
      />
      <el-select v-model="queryParams.actionType" placeholder="动作类型" clearable style="width: 140px">
        <el-option v-for="t in actionTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-select v-model="queryParams.success" placeholder="执行结果" clearable style="width: 120px">
        <el-option label="全部" value="" />
        <el-option label="成功" value="true" />
        <el-option label="失败" value="false" />
      </el-select>
      <el-select v-model="queryParams.accountId" placeholder="选择账号" clearable style="width: 180px">
        <el-option v-for="a in accountOptions" :key="a.value" :label="a.label" :value="a.value" />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">
        <el-icon><RefreshRight /></el-icon>
        重置
      </el-button>
    </div>

    <!-- 数据表格 -->
    <div class="data-card">
      <el-table :data="filteredLogs" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="taskId" label="任务ID" width="100" align="center" />
        <el-table-column prop="accountName" label="账号" width="160" show-overflow-tooltip />
        <el-table-column prop="actionType" label="动作类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getActionTypeTag(row.actionType)" size="small" disable-transitions>
              {{ getActionTypeLabel(row.actionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getPlatformTag(row.platform)" size="small" disable-transitions>
              {{ row.platform }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="success" label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.success" :size="18" color="#059669"><CircleCheck /></el-icon>
            <el-icon v-else :size="18" color="#DC2626"><CircleClose /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="delay" label="随机延迟(ms)" width="120" align="center" />
        <el-table-column prop="executedAt" label="执行时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="日志详情" size="480px">
      <template v-if="currentLog">
        <el-descriptions :column="1">
          <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
          <el-descriptions-item label="任务ID">{{ currentLog.taskId }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ currentLog.accountName }}</el-descriptions-item>
          <el-descriptions-item label="动作类型">
            <el-tag :type="getActionTypeTag(currentLog.actionType)" size="small">
              {{ getActionTypeLabel(currentLog.actionType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="目标名称">{{ currentLog.targetName }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ currentLog.platform }}</el-descriptions-item>
          <el-descriptions-item label="执行结果">
            <el-tag :type="currentLog.success ? 'success' : 'danger'" size="small">
              {{ currentLog.success ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="随机延迟">{{ currentLog.delay }}ms</el-descriptions-item>
          <el-descriptions-item label="执行时间">{{ currentLog.executedAt }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <h4>动作详情</h4>
          <div class="json-block">
            <pre>{{ JSON.stringify(currentLog.actionDetail, null, 2) }}</pre>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Search, RefreshRight, Operation, CircleCheck, CircleClose,
  Timer, WarningFilled,
} from '@element-plus/icons-vue'
import { getAgentLogList, getAgentAccountList } from '@/api/modules/agent'

const route = useRoute()

// 查询参数
const queryParams = reactive({
  dateRange: [] as string[],
  actionType: '',
  success: '',
  accountId: (route.query.accountId as string) || '',
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const drawerVisible = ref(false)
const currentLog = ref<any>(null)

// 选项
const actionTypeOptions = [
  { label: '全部', value: '' },
  { label: '查看资料', value: 'VIEW_PROFILE' },
  { label: '发送消息', value: 'SEND_MESSAGE' },
  { label: '跟进', value: 'FOLLOWUP' },
  { label: '安排面试', value: 'SCHEDULE_INTERVIEW' },
]

const accountOptions = ref<any[]>([])
const logList = ref<any[]>([])

// 统计
const stats = computed(() => {
  const all = logList.value
  const successCount = all.filter(l => l.success).length
  return {
    todayOps: all.length,
    successRate: all.length ? Math.round((successCount / all.length) * 100) : 0,
    avgDelay: all.length ? Math.round(all.reduce((s, l) => s + l.delay, 0) / all.length) : 0,
    errorCount: all.filter(l => !l.success).length,
  }
})

// 过滤
const filteredLogs = computed(() => {
  let list = [...logList.value]
  if (queryParams.actionType) {
    list = list.filter(l => l.actionType === queryParams.actionType)
  }
  if (queryParams.success !== '') {
    const want = queryParams.success === 'true'
    list = list.filter(l => l.success === want)
  }
  if (queryParams.accountId) {
    list = list.filter(l => l.accountId === queryParams.accountId)
  }
  total.value = list.length
  const start = (queryParams.pageNum - 1) * queryParams.pageSize
  return list.slice(start, start + queryParams.pageSize)
})

// 标签映射
function getActionTypeTag(type: string) {
  const map: Record<string, string> = {
    VIEW_PROFILE: '', SEND_MESSAGE: 'success', FOLLOWUP: 'warning', SCHEDULE_INTERVIEW: 'info',
  }
  return map[type] || 'info'
}

function getActionTypeLabel(type: string) {
  const map: Record<string, string> = {
    VIEW_PROFILE: '查看资料', SEND_MESSAGE: '发送消息', FOLLOWUP: '跟进', SCHEDULE_INTERVIEW: '安排面试',
  }
  return map[type] || type
}

function getPlatformTag(p: string) {
  const map: Record<string, string> = { BOSS: '', LAGOU: 'success', ZHILIAN: 'warning', LIEPIN: 'info' }
  return map[p] || 'info'
}

// 操作
function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.dateRange = []
  queryParams.actionType = ''
  queryParams.success = ''
  queryParams.accountId = ''
  handleSearch()
}

function openDetail(row: any) {
  currentLog.value = row
  drawerVisible.value = true
}

async function loadData() {
  try {
    const params: any = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
    }
    if (queryParams.actionType) params.actionType = queryParams.actionType
    if (queryParams.success !== '') params.success = queryParams.success
    if (queryParams.accountId) params.accountId = queryParams.accountId
    if (queryParams.dateRange?.length === 2) {
      params.startDate = queryParams.dateRange[0]
      params.endDate = queryParams.dateRange[1]
    }
    const res: any = await getAgentLogList(params)
    logList.value = res.data?.list || res.data?.records || res.data || []
    total.value = res.data?.total || logList.value.length
  } catch {
    logList.value = []
    total.value = 0
  }
}

async function loadAccountOptions() {
  try {
    const res: any = await getAgentAccountList({ pageSize: 100 })
    const accounts = res.data?.list || res.data?.records || res.data || []
    accountOptions.value = accounts.map((a: any) => ({
      label: a.accountName || a.name,
      value: a.id || a.accountId,
    }))
  } catch {
    accountOptions.value = []
  }
}

onMounted(() => {
  loadData()
  loadAccountOptions()
})
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

.stat-ops .stat-icon { background: linear-gradient(135deg, $primary-color, $primary-light); }
.stat-success .stat-icon { background: linear-gradient(135deg, $success-color, $success-color); }
.stat-delay .stat-icon { background: linear-gradient(135deg, $warning-color, $warning-color); }
.stat-error .stat-icon { background: linear-gradient(135deg, $danger-color, $danger-color); }

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

.detail-section {
  margin-top: 20px;

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12px;
  }
}

.json-block {
  background: $bg-muted;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;

  pre {
    margin: 0;
    font-size: 13px;
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
    color: $text-regular;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
