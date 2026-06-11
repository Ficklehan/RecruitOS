<template>
  <PageShell variant="list" title="渠道运行日志" subtitle="查看自动招聘任务在 Boss、猎聘等渠道的执行记录">
    <template #toolbar>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="stat-card stat-ops">
          <div class="stat-icon"><Settings2 class="h-7 w-7" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayOps }}</div>
            <div class="stat-label">今日操作数</div>
          </div>
        </div>
        <div class="stat-card stat-success">
          <div class="stat-icon"><CircleCheck class="h-7 w-7" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.successRate }}%</div>
            <div class="stat-label">成功率</div>
          </div>
        </div>
        <div class="stat-card stat-delay">
          <div class="stat-icon"><Timer class="h-7 w-7" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgDelay }}ms</div>
            <div class="stat-label">平均延迟</div>
          </div>
        </div>
        <div class="stat-card stat-error">
          <div class="stat-icon"><AlertTriangle class="h-7 w-7" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.errorCount }}</div>
            <div class="stat-label">异常数</div>
          </div>
        </div>
      </div>
    </template>

    <template #filters>
      <DateRangePicker v-model="queryParams.dateRange" />
      <RSelect
        v-model="queryParams.actionType"
        :options="actionTypeOptions"
        placeholder="动作类型"
        clearable
        class="w-full sm:w-36"
      />
      <RSelect
        v-model="queryParams.success"
        :options="successOptions"
        placeholder="执行结果"
        clearable
        class="w-full sm:w-32"
      />
      <RSelect
        v-model="queryParams.accountId"
        :options="accountOptions"
        placeholder="选择账号"
        clearable
        class="w-full sm:w-44"
      />
    </template>

    <template #filterActions>
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </RButton>
      <RButton variant="outline" @click="handleReset">
        <RefreshCw class="mr-2 h-4 w-4" />
        重置
      </RButton>
    </template>

    <RTable v-if="filteredLogs.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[100px] text-center">任务ID</RTableTh>
          <RTableTh class="w-[160px]">账号</RTableTh>
          <RTableTh class="w-[120px] text-center">动作类型</RTableTh>
          <RTableTh class="min-w-[180px]">目标名称</RTableTh>
          <RTableTh class="w-[100px] text-center">平台</RTableTh>
          <RTableTh class="w-[80px] text-center">结果</RTableTh>
          <RTableTh class="w-[120px] text-center">随机延迟(ms)</RTableTh>
          <RTableTh class="w-[170px]">执行时间</RTableTh>
          <RTableTh class="w-[80px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in filteredLogs" :key="row.id">
          <RTableCell class="text-center">{{ row.taskId }}</RTableCell>
          <RTableCell class="truncate max-w-[160px]">{{ row.accountName }}</RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(getActionTypeTag(row.actionType))">
              {{ getActionTypeLabel(row.actionType) }}
            </RBadge>
          </RTableCell>
          <RTableCell class="truncate max-w-[200px]">{{ row.targetName }}</RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="elTagTypeToBadge(getPlatformTag(row.platform))">{{ row.platform }}</RBadge>
          </RTableCell>
          <RTableCell class="text-center">
            <CircleCheck v-if="row.success" class="h-4 w-4 text-green-600 inline" />
            <CircleX v-else class="h-4 w-4 text-destructive inline" />
          </RTableCell>
          <RTableCell class="text-center">{{ row.delay }}</RTableCell>
          <RTableCell class="text-muted-foreground">{{ row.executedAt }}</RTableCell>
          <RTableCell class="text-center">
            <RButton variant="link" size="sm" @click="openDetail(row)">详情</RButton>
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta v-else title="暂无日志" description="渠道任务执行后将在此显示记录" />

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="loadData"
    />

    <template #below>
      <RSheet v-model:open="drawerVisible">
        <RSheetContent class="overflow-y-auto">
          <h3 class="text-lg font-semibold mb-4">日志详情</h3>
          <template v-if="currentLog">
            <dl class="detail-dl">
              <div><dt>日志ID</dt><dd>{{ currentLog.id }}</dd></div>
              <div><dt>任务ID</dt><dd>{{ currentLog.taskId }}</dd></div>
              <div><dt>账号</dt><dd>{{ currentLog.accountName }}</dd></div>
              <div>
                <dt>动作类型</dt>
                <dd>
                  <RBadge :variant="elTagTypeToBadge(getActionTypeTag(currentLog.actionType))">
                    {{ getActionTypeLabel(currentLog.actionType) }}
                  </RBadge>
                </dd>
              </div>
              <div><dt>目标名称</dt><dd>{{ currentLog.targetName }}</dd></div>
              <div><dt>平台</dt><dd>{{ currentLog.platform }}</dd></div>
              <div>
                <dt>执行结果</dt>
                <dd>
                  <RBadge :variant="currentLog.success ? 'default' : 'destructive'">
                    {{ currentLog.success ? '成功' : '失败' }}
                  </RBadge>
                </dd>
              </div>
              <div><dt>随机延迟</dt><dd>{{ currentLog.delay }}ms</dd></div>
              <div><dt>执行时间</dt><dd>{{ currentLog.executedAt }}</dd></div>
            </dl>

            <div class="detail-section">
              <h4>动作详情</h4>
              <div class="json-block">
                <pre>{{ JSON.stringify(currentLog.actionDetail, null, 2) }}</pre>
              </div>
            </div>
          </template>
        </RSheetContent>
      </RSheet>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Search, RefreshCw, Settings2, CircleCheck, CircleX, Timer, AlertTriangle,
} from 'lucide-vue-next'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import PageShell from '@/components/Layout/PageShell.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import DateRangePicker from '@/components/app/DateRangePicker.vue'
import {
  RButton,
  RSelect,
  RBadge,
  RTable,
  RTableHead,
  RTableBody,
  RTableRow,
  RTableTh,
  RTableCell,
  RSheet,
  RSheetContent,
} from '@/components/ui'
import { getAgentLogList, getAgentAccountList } from '@/api/modules/agent'

const route = useRoute()

const queryParams = reactive({
  dateRange: null as [string, string] | null,
  actionType: '' as string | undefined,
  success: '' as string | undefined,
  accountId: (route.query.accountId as string) || undefined,
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const drawerVisible = ref(false)
const currentLog = ref<any>(null)

const actionTypeOptions = [
  { label: '查看资料', value: 'VIEW_PROFILE' },
  { label: '发送消息', value: 'SEND_MESSAGE' },
  { label: '跟进', value: 'FOLLOWUP' },
  { label: '安排面试', value: 'SCHEDULE_INTERVIEW' },
]

const successOptions = [
  { label: '成功', value: 'true' },
  { label: '失败', value: 'false' },
]

const accountOptions = ref<{ label: string; value: string | number }[]>([])
const logList = ref<any[]>([])

const stats = computed(() => {
  const all = logList.value
  const successCount = all.filter((l) => l.success).length
  return {
    todayOps: all.length,
    successRate: all.length ? Math.round((successCount / all.length) * 100) : 0,
    avgDelay: all.length ? Math.round(all.reduce((s, l) => s + l.delay, 0) / all.length) : 0,
    errorCount: all.filter((l) => !l.success).length,
  }
})

const filteredLogs = computed(() => {
  let list = [...logList.value]
  if (queryParams.actionType) list = list.filter((l) => l.actionType === queryParams.actionType)
  if (queryParams.success !== undefined && queryParams.success !== '') {
    const want = queryParams.success === 'true'
    list = list.filter((l) => l.success === want)
  }
  if (queryParams.accountId) list = list.filter((l) => l.accountId === queryParams.accountId)
  total.value = list.length
  const start = (queryParams.pageNum - 1) * queryParams.pageSize
  return list.slice(start, start + queryParams.pageSize)
})

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

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.dateRange = null
  queryParams.actionType = undefined
  queryParams.success = undefined
  queryParams.accountId = undefined
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
    if (queryParams.success !== undefined && queryParams.success !== '') params.success = queryParams.success
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

.detail-dl {
  display: grid;
  gap: 10px;
  font-size: 14px;

  div {
    display: grid;
    grid-template-columns: 90px 1fr;
    gap: 8px;
  }

  dt { color: $text-secondary; }
  dd { margin: 0; }
}

.detail-section {
  margin-top: 20px;

  h4 {
    font-size: 14px;
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
