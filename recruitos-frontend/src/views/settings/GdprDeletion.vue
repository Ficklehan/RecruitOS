<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RCard, RButton, RBadge, RInput, RSelect, RSkeleton, RTable, RTableBody, RTableCell, RTableHead, RTableRow, RTableTh, RDialog } from '@/components/ui'
import { Shield, Trash2, UserX, Clock, CheckCircle2, AlertTriangle, Search } from 'lucide-vue-next'

const loading = ref(true)
const requests = ref<any[]>([])
const filterStatus = ref('')
const showCreateDialog = ref(false)
const showExecuteDialog = ref(false)
const selectedRequest = ref<any>(null)
const creating = ref(false)

const newRequest = ref({
  requestType: 'DELETE',
  targetType: 'CANDIDATE',
  targetId: '',
  targetIdentifier: '',
  requestorId: 1,
})

const policies = ref<any[]>([])
const policyLoading = ref(false)

const statusLabels: Record<string, string> = {
  PENDING: '待处理',
  IN_PROGRESS: '处理中',
  COMPLETED: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消',
}

const statusColor = (s: string) => {
  const map: Record<string, string> = {
    PENDING: 'bg-yellow-50 text-yellow-700 border-yellow-200',
    IN_PROGRESS: 'bg-blue-50 text-blue-700 border-blue-200',
    COMPLETED: 'bg-green-50 text-green-700 border-green-200',
    FAILED: 'bg-red-50 text-red-700 border-red-200',
    CANCELLED: 'bg-gray-50 text-gray-500 border-gray-200',
  }
  return map[s] || ''
}

async function fetchRequests() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (filterStatus.value) params.set('status', filterStatus.value)
    const res = await fetch(`/api/compliance/gdpr-requests?${params}`)
    const data = await res.json()
    requests.value = data.records || []
  } catch (e) { /* error handled */ }
  loading.value = false
}

async function fetchPolicies() {
  policyLoading.value = true
  try {
    const res = await fetch('/api/compliance/retention-policies')
    policies.value = await res.json()
  } catch (e) {}
  policyLoading.value = false
}

async function submitRequest() {
  creating.value = true
  try {
    const res = await fetch('/api/compliance/gdpr-requests', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        requestType: newRequest.value.requestType,
        targetType: newRequest.value.targetType,
        targetId: parseInt(newRequest.value.targetId),
        targetIdentifier: newRequest.value.targetIdentifier,
        requestorId: newRequest.value.requestorId,
      }),
    })
    const data = await res.json()
    if (data.status !== 'FAILED') {
      showCreateDialog.value = false
      newRequest.value = { requestType: 'DELETE', targetType: 'CANDIDATE', targetId: '', targetIdentifier: '', requestorId: 1 }
      fetchRequests()
    } else {
      alert(data.message)
    }
  } catch (e) { /* error handled */ }
  creating.value = false
}

async function executeDeletion() {
  if (!selectedRequest.value) return
  try {
    const res = await fetch(`/api/compliance/gdpr-requests/${selectedRequest.value.id}/execute`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ verifiedBy: 1 }),
    })
    const data = await res.json()
    showExecuteDialog.value = false
    fetchRequests()
    alert(data.message)
  } catch (e) { /* error handled */ }
}

function formatDate(d: string) {
  if (!d) return '—'
  return d.replace('T', ' ').substring(0, 16)
}

const retentionLabels: Record<string, string> = {
  CANDIDATE_RESUME: '候选人简历',
  INTERVIEW_RECORDING: '面试录音',
  COMMUNICATION_LOG: '沟通记录',
  OFFER_DOCUMENT: 'Offer 文档',
  AI_JUDGMENT: 'AI 判断',
  AUDIT_LOG: '审计日志',
  COGNITIVE_MEMORY: 'AI 记忆',
}

onMounted(() => {
  fetchRequests()
  fetchPolicies()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-bold text-gray-900">GDPR 数据管理</h2>
        <p class="text-sm text-gray-500 mt-1">处理数据主体删除请求，管理数据保留策略</p>
      </div>
      <RButton @click="showCreateDialog = true">
        <Trash2 class="h-4 w-4 mr-1" /> 新建删除请求
      </RButton>
    </div>

    <!-- Data Retention Policies -->
    <RCard class="p-5">
      <h3 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
        <Clock class="h-4 w-4 text-blue-500" /> 数据保留策略
      </h3>
      <RSkeleton v-if="policyLoading" class="h-32" />
      <div v-else class="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div v-for="p in policies" :key="p.id"
             class="p-3 border border-gray-100 rounded-lg">
          <div class="text-xs text-gray-500">{{ retentionLabels[p.dataCategory] || p.dataCategory }}</div>
          <div class="text-lg font-bold text-gray-900 mt-1">
            {{ p.retentionDays === 0 ? '永久' : `${p.retentionDays} 天` }}
          </div>
          <div class="flex gap-1.5 mt-1.5">
            <RBadge v-if="p.autoDelete" variant="default" class="text-xs bg-blue-50 text-blue-600 border-blue-200">自动删除</RBadge>
            <RBadge v-if="p.legalHold" variant="default" class="text-xs bg-red-50 text-red-600 border-red-200">司法冻结</RBadge>
          </div>
        </div>
      </div>
    </RCard>

    <!-- Request list -->
    <div class="flex gap-3 items-center">
      <select v-model="filterStatus"
              class="px-3 py-2 border border-gray-200 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-100 focus:border-blue-300"
              @change="fetchRequests">
        <option value="">全部状态</option>
        <option value="PENDING">待处理</option>
        <option value="IN_PROGRESS">处理中</option>
        <option value="COMPLETED">已完成</option>
        <option value="FAILED">失败</option>
      </select>
      <RButton variant="ghost" size="sm" @click="fetchRequests">刷新</RButton>
    </div>

    <RSkeleton v-if="loading" class="h-64" />
    <RCard v-else class="overflow-hidden">
      <RTable>
        <RTableHead>
          <RTableRow>
            <RTableTh>类型</RTableTh>
            <RTableTh>目标</RTableTh>
            <RTableTh>标识符</RTableTh>
            <RTableTh>状态</RTableTh>
            <RTableTh>创建时间</RTableTh>
            <RTableTh>执行时间</RTableTh>
            <RTableTh>操作</RTableTh>
          </RTableRow>
        </RTableHead>
        <RTableBody>
          <RTableRow v-for="r in requests" :key="r.id" class="hover:bg-gray-50">
            <RTableCell>
              <RBadge variant="secondary" class="text-xs">{{ r.requestType === 'DELETE' ? '删除' : r.requestType === 'EXPORT' ? '导出' : '匿名化' }}</RBadge>
            </RTableCell>
            <RTableCell class="text-xs">{{ r.targetType }} #{{ r.targetId }}</RTableCell>
            <RTableCell class="text-xs text-gray-500">{{ r.targetIdentifier || '—' }}</RTableCell>
            <RTableCell>
              <RBadge :class="statusColor(r.status)" class="text-xs">{{ statusLabels[r.status] || r.status }}</RBadge>
            </RTableCell>
            <RTableCell class="text-xs text-gray-500 whitespace-nowrap">{{ formatDate(r.createdAt) }}</RTableCell>
            <RTableCell class="text-xs text-gray-500 whitespace-nowrap">{{ formatDate(r.executedAt) }}</RTableCell>
            <RTableCell>
              <RButton v-if="r.status === 'PENDING'" size="xs" variant="outline"
                       class="text-red-600 border-red-200 hover:bg-red-50"
                       @click="selectedRequest = r; showExecuteDialog = true">
                执行删除
              </RButton>
              <span v-else-if="r.errorMessage" class="text-xs text-red-500" :title="r.errorMessage">失败</span>
            </RTableCell>
          </RTableRow>
          <RTableRow v-if="requests.length === 0">
            <RTableCell colspan="7" class="text-center text-gray-400 py-12">暂无 GDPR 请求</RTableCell>
          </RTableRow>
        </RTableBody>
      </RTable>
    </RCard>

    <!-- Create dialog -->
    <RDialog v-if="showCreateDialog" @close="showCreateDialog = false">
      <template #title>新建 GDPR 数据删除请求</template>
      <div class="space-y-4 py-4">
        <div class="bg-orange-50 border border-orange-200 rounded-lg p-3 flex items-start gap-2">
          <AlertTriangle class="h-4 w-4 text-orange-500 mt-0.5 shrink-0" />
          <div class="text-xs text-orange-700">
            GDPR 数据删除不可逆。请确认目标身份和删除范围后再提交。执行前需要二次确认。
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-medium text-gray-600 block mb-1">请求类型</label>
            <select v-model="newRequest.requestType"
                    class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm">
              <option value="DELETE">删除</option>
              <option value="ANONYMIZE">匿名化</option>
              <option value="EXPORT">数据导出</option>
            </select>
          </div>
          <div>
            <label class="text-xs font-medium text-gray-600 block mb-1">目标类型</label>
            <select v-model="newRequest.targetType"
                    class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm">
              <option value="CANDIDATE">候选人</option>
              <option value="EMPLOYEE">员工</option>
              <option value="INTERVIEWER">面试官</option>
            </select>
          </div>
          <div>
            <label class="text-xs font-medium text-gray-600 block mb-1">目标 ID</label>
            <input v-model="newRequest.targetId"
                   class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm"
                   placeholder="候选人/员工 ID" />
          </div>
          <div>
            <label class="text-xs font-medium text-gray-600 block mb-1">标识符（可选）</label>
            <input v-model="newRequest.targetIdentifier"
                   class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm"
                   placeholder="邮箱或手机号" />
          </div>
        </div>
      </div>
      <template #footer>
        <RButton variant="ghost" @click="showCreateDialog = false">取消</RButton>
        <RButton class="bg-red-500 hover:bg-red-600 text-white border-0" :disabled="creating" @click="submitRequest">
          {{ creating ? '提交中...' : '提交请求' }}
        </RButton>
      </template>
    </RDialog>

    <!-- Execute confirmation dialog -->
    <RDialog v-if="showExecuteDialog" @close="showExecuteDialog = false">
      <template #title>二次确认 — 执行数据删除</template>
      <div class="space-y-4 py-4">
        <div class="bg-red-50 border border-red-200 rounded-lg p-4">
          <div class="flex items-start gap-2 mb-2">
            <AlertTriangle class="h-5 w-5 text-red-500 mt-0.5 shrink-0" />
            <div class="font-medium text-red-700">此操作不可逆</div>
          </div>
          <div class="text-sm text-red-600 space-y-1">
            <p>即将对 <strong>{{ selectedRequest?.targetType }} #{{ selectedRequest?.targetId }}</strong> 执行数据删除。</p>
            <p>将匿名化：姓名、邮箱、电话、简历文本</p>
            <p>将清除：面试录音引用、敏感沟通内容</p>
            <p class="text-xs text-red-400 mt-2">此操作将被记录在审计日志中。</p>
          </div>
        </div>
      </div>
      <template #footer>
        <RButton variant="ghost" @click="showExecuteDialog = false">取消</RButton>
        <RButton class="bg-red-500 hover:bg-red-600 text-white border-0" @click="executeDeletion">
          <Trash2 class="h-4 w-4 mr-1" /> 确认删除
        </RButton>
      </template>
    </RDialog>
  </div>
</template>
