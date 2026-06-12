<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RCard, RButton, RBadge, RInput, RSelect, RPagination, RSkeleton, RTable, RTableBody, RTableCell, RTableHead, RTableRow, RTableTh } from '@/components/ui'
import { Search, Filter, Download, Eye, Shield, Clock, User, Bot, RefreshCw } from 'lucide-vue-next'

const loading = ref(true)
const records = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(50)
const filterAction = ref('')
const filterTargetType = ref('')
const filterActorId = ref('')
const filterStartDate = ref('')
const filterEndDate = ref('')

const actions = ref<string[]>([])
const summary = ref<any>({})

const actionLabels: Record<string, string> = {
  'AI_ADVICE_GENERATED': 'AI 建议生成',
  'AI_ADVICE_ACCEPTED': '采纳 AI 建议',
  'AI_ADVICE_IGNORED': '忽略 AI 建议',
  'CANDIDATE_DECISION_HIRE': '录用决策',
  'CANDIDATE_DECISION_REJECT': '拒绝决策',
  'OFFER_CREATED': '创建 Offer',
  'OFFER_SENT': '发送 Offer',
  'OFFER_ACCEPTED': 'Offer 接受',
  'OFFER_REJECTED': 'Offer 拒绝',
  'DATA_EXPORT': '数据导出',
  'USER_LOGIN': '用户登录',
  'GDPR_DELETE_REQUEST': 'GDPR 删除请求',
  'GDPR_DELETE_EXECUTED': 'GDPR 删除执行',
  'GDPR_EXPORT_REQUEST': 'GDPR 导出请求',
  'GDPR_EXPORT_EXECUTED': 'GDPR 导出执行',
  'RETENTION_POLICY_UPDATED': '更新保留策略',
}

const severityColor = (action: string) => {
  if (action.includes('GDPR')) return 'bg-orange-50 text-orange-700 border-orange-200'
  if (action.includes('DELETE')) return 'bg-red-50 text-red-700 border-red-200'
  if (action.includes('AI')) return 'bg-blue-50 text-blue-700 border-blue-200'
  if (action.includes('EXPORT')) return 'bg-yellow-50 text-yellow-700 border-yellow-200'
  return 'bg-gray-50 text-gray-700 border-gray-200'
}

const actorIcon = (type: string) => {
  if (type === 'AI_AGENT') return Bot
  if (type === 'SYSTEM' || type === 'SCHEDULER') return RefreshCw
  return User
}

async function fetchData() {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: String(page.value), pageSize: String(pageSize.value) })
    if (filterAction.value) params.set('action', filterAction.value)
    if (filterTargetType.value) params.set('targetType', filterTargetType.value)
    if (filterActorId.value) params.set('actorId', filterActorId.value)
    if (filterStartDate.value) params.set('startDate', filterStartDate.value)
    if (filterEndDate.value) params.set('endDate', filterEndDate.value)

    const res = await fetch(`/api/compliance/audit-logs?${params}`)
    const data = await res.json()
    records.value = data.records || []
    total.value = data.total || 0
  } catch (e) { /* error handled */ }
  loading.value = false
}

async function fetchActions() {
  try {
    const res = await fetch('/api/compliance/audit-actions')
    actions.value = await res.json()
  } catch (e) {}
}

async function fetchSummary() {
  try {
    const res = await fetch('/api/compliance/summary')
    summary.value = await res.json()
  } catch (e) {}
}

function formatDate(d: string) {
  if (!d) return '—'
  return d.replace('T', ' ').substring(0, 16)
}

function exportCSV() {
  const headers = ['时间', '操作者', '操作', '对象类型', '对象', 'IP']
  const rows = records.value.map(r => [
    formatDate(r.createdAt), `${r.actorType} #${r.actorId}`,
    actionLabels[r.action] || r.action, r.targetType, r.targetLabel || `#${r.targetId}`, r.ipAddress || ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `audit-log-${new Date().toISOString().substring(0,10)}.csv`; a.click()
}

onMounted(() => {
  fetchActions()
  fetchSummary()
  fetchData()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-bold text-gray-900">合规审计日志</h2>
        <p class="text-sm text-gray-500 mt-1">追踪所有 AI 决策、操作记录和 GDPR 请求</p>
      </div>
      <div class="flex gap-2">
        <RButton variant="outline" size="sm" @click="fetchData">
          <RefreshCw class="h-4 w-4 mr-1" /> 刷新
        </RButton>
        <RButton variant="outline" size="sm" @click="exportCSV">
          <Download class="h-4 w-4 mr-1" /> 导出 CSV
        </RButton>
      </div>
    </div>

    <!-- Summary cards -->
    <div class="grid grid-cols-3 gap-4">
      <RCard class="p-4">
        <div class="text-2xl font-bold text-gray-900">{{ summary.auditCount30d || 0 }}</div>
        <div class="text-xs text-gray-500 mt-1">近 30 天审计记录</div>
      </RCard>
      <RCard class="p-4 border-l-4 border-orange-400">
        <div class="text-2xl font-bold text-orange-600">{{ summary.pendingGdprRequests || 0 }}</div>
        <div class="text-xs text-gray-500 mt-1">待处理 GDPR 请求</div>
      </RCard>
      <RCard class="p-4 border-l-4 border-green-400">
        <div class="text-2xl font-bold text-green-600">{{ summary.completedGdprRequests || 0 }}</div>
        <div class="text-xs text-gray-500 mt-1">已完成 GDPR 请求</div>
      </RCard>
    </div>

    <!-- Filters -->
    <div class="flex flex-wrap gap-3">
      <select v-model="filterAction"
              class="px-3 py-2 border border-gray-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-100 focus:border-blue-300">
        <option value="">全部操作</option>
        <option v-for="a in actions" :key="a" :value="a">{{ actionLabels[a] || a }}</option>
      </select>
      <input v-model="filterStartDate" type="date"
             class="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-100 focus:border-blue-300" />
      <span class="self-center text-gray-400 text-sm">至</span>
      <input v-model="filterEndDate" type="date"
             class="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-100 focus:border-blue-300" />
      <RButton size="sm" @click="page=1; fetchData()">筛选</RButton>
    </div>

    <!-- Table -->
    <RSkeleton v-if="loading" class="h-64" />
    <RCard v-else class="overflow-hidden">
      <RTable>
        <RTableHead>
          <RTableRow>
            <RTableTh class="w-40">时间</RTableTh>
            <RTableTh class="w-24">操作者</RTableTh>
            <RTableTh class="w-32">操作</RTableTh>
            <RTableTh class="w-24">对象类型</RTableTh>
            <RTableTh>对象</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="r in records" :key="r.id" class="hover:bg-gray-50">
            <RTableCell class="text-xs text-gray-500 font-mono whitespace-nowrap">{{ formatDate(r.createdAt) }}</RTableCell>
            <RTableCell>
              <div class="flex items-center gap-1.5">
                <component :is="actorIcon(r.actorType)" class="h-3.5 w-3.5 text-gray-400" />
                <span class="text-xs">{{ r.actorType === 'SYSTEM' ? '系统' : r.actorType === 'AI_AGENT' ? 'AI' : '用户' }} #{{ r.actorId }}</span>
              </div>
            </RTableCell>
            <RTableCell>
              <RBadge :class="severityColor(r.action)" class="text-xs whitespace-nowrap">
                {{ actionLabels[r.action] || r.action }}
              </RBadge>
            </RTableCell>
            <RTableCell class="text-xs text-gray-500">{{ r.targetType }}</RTableCell>
            <RTableCell class="text-xs">{{ r.targetLabel || `#${r.targetId}` }}</RTableCell>
          </RTableRow>
          <RTableRow v-if="records.length === 0">
            <RTableCell colspan="5" class="text-center text-gray-400 py-12">暂无审计记录</RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </RCard>

    <div class="flex justify-between items-center">
      <span class="text-sm text-gray-500">共 {{ total }} 条记录</span>
      <RPagination
        :current="page"
        :total="Math.ceil(total / pageSize)"
        @change="(p: number) => { page = p; fetchData() }"
      />
    </div>
  </div>
</template>
