<template>
  <PageShell variant="list" title="奖励管理" subtitle="管理内推奖励审批与发放">
    <template #toolbar>
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card class="p-5">
          <div class="text-sm text-muted-foreground mb-2">待发放金额</div>
          <div class="text-2xl font-semibold text-amber-600">¥ {{ formatMoney(stats.pendingAmount) }}</div>
        </Card>
        <Card class="p-5">
          <div class="text-sm text-muted-foreground mb-2">已发放金额</div>
          <div class="text-2xl font-semibold text-green-600">¥ {{ formatMoney(stats.totalPaidAmount) }}</div>
        </Card>
        <Card class="p-5">
          <div class="text-sm text-muted-foreground mb-2">本月发放</div>
          <div class="text-2xl font-semibold text-primary">{{ stats.approvedRewards }}</div>
          <div class="text-xs text-muted-foreground mt-1">待发放笔数</div>
        </Card>
        <Card class="p-5">
          <div class="text-sm text-muted-foreground mb-2">奖励总数</div>
          <div class="text-2xl font-semibold">{{ stats.totalRewards }}</div>
        </Card>
      </div>
    </template>

    <template #filters>
      <Select
        v-model="queryParams.status"
        :options="statusOptions"
        placeholder="状态"
        clearable
        class="w-full sm:w-36"
      />
    </template>

    <template #filterActions>
      <Button @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </Button>
      <Button variant="outline" @click="handleReset">
        <RefreshCw class="mr-2 h-4 w-4" />
        重置
      </Button>
    </template>

    <Table v-if="rewardList.length">
      <TableHeader>
        <TableRow>
          <TableHead class="w-[120px]">推荐人</TableHead>
          <TableHead class="w-[120px]">候选人</TableHead>
          <TableHead class="w-[110px] text-center">奖励类型</TableHead>
          <TableHead class="w-[120px] text-right">金额</TableHead>
          <TableHead class="w-[110px] text-center">状态</TableHead>
          <TableHead class="w-[120px]">审批人</TableHead>
          <TableHead class="w-[100px] text-center">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-for="row in rewardList" :key="row.id">
          <TableCell>{{ row.referrerName }}</TableCell>
          <TableCell>{{ row.candidateName }}</TableCell>
          <TableCell class="text-center">
            <Badge :variant="elTagTypeToBadge(getRewardTypeTag(row.rewardType))">
              {{ getRewardTypeLabel(row.rewardType) }}
            </Badge>
          </TableCell>
          <TableCell class="text-right font-medium">
            <span v-if="row.rewardType === 'CASH'">¥ {{ formatMoney(row.amount) }}</span>
            <span v-else>{{ row.amount }}</span>
          </TableCell>
          <TableCell class="text-center">
            <Badge :variant="elTagTypeToBadge(getStatusType(row.status))">{{ getStatusLabel(row.status) }}</Badge>
          </TableCell>
          <TableCell>{{ row.approver || '—' }}</TableCell>
          <TableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="loadData"
    />
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, RefreshCw } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import {
  Button,
  Select,
  Badge,
  Card,
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/components/ui'
import {
  getReferralRewardList,
  getReferralRewardStats,
  approveReferralReward,
  payReferralReward,
} from '@/api/modules/referral'

const queryParams = reactive({
  status: '' as string | undefined,
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const rewardList = ref<any[]>([])
const stats = ref({
  totalRewards: 0,
  pendingRewards: 0,
  approvedRewards: 0,
  paidRewards: 0,
  totalPaidAmount: 0,
  pendingAmount: 0,
})

function formatMoney(v?: number) {
  return (v ?? 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function mapRewardRow(row: any) {
  return {
    ...row,
    amount: row.rewardAmount ?? row.amount ?? 0,
    candidateName: row.candidateName || '—',
    approver: row.approvedBy ? `用户#${row.approvedBy}` : '—',
  }
}

const statusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '已审批', value: 'APPROVED' },
  { label: '已发放', value: 'PAID' },
  { label: '已取消', value: 'CANCELLED' },
]

function getRewardTypeTag(type: string): string {
  const map: Record<string, string> = {
    CASH: 'success',
    GIFT: 'info',
    OTHER: 'info',
  }
  return map[type] || 'info'
}

function getRewardTypeLabel(type: string): string {
  const map: Record<string, string> = {
    CASH: '现金',
    GIFT: '礼品',
    OTHER: '其他',
  }
  return map[type] || type
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    APPROVED: 'info',
    PAID: 'success',
    CANCELLED: 'info',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待审批',
    APPROVED: '已审批',
    PAID: '已发放',
    CANCELLED: '已取消',
  }
  return map[status] || status
}

function getRowActions(row: any) {
  const actions: any[] = []
  if (row.status === 'PENDING') {
    actions.push({ command: 'approve', label: '审批', icon: 'Check', type: 'primary', primary: true })
  }
  if (row.status === 'APPROVED') {
    actions.push({ command: 'pay', label: '发放', icon: 'Money', type: 'primary', primary: true })
  }
  return actions
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'approve') handleApprove(row)
  else if (cmd === 'pay') handlePay(row)
}

async function loadData() {
  const params: any = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  if (queryParams.status) params.status = queryParams.status
  const res: any = await getReferralRewardList(params)
  const page = res.data ?? res
  const rows = page.records || page.list || page || []
  rewardList.value = (Array.isArray(rows) ? rows : []).map(mapRewardRow)
  total.value = page.total || rewardList.value.length
  try {
    const statRes: any = await getReferralRewardStats()
    const s = statRes.data ?? statRes
    stats.value = { ...stats.value, ...s }
  } catch { /* ignore */ }
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.status = undefined
  handleSearch()
}

async function handleApprove(row: any) {
  const ok = await confirm({
    title: '审批确认',
    message: `确定审批通过 ${row.referrerName} 推荐 ${row.candidateName} 的奖励吗？`,
  })
  if (!ok) return
  await approveReferralReward(row.id)
  toast.success('审批通过')
  loadData()
}

async function handlePay(row: any) {
  const ok = await confirm({
    title: '发放确认',
    message: `确定发放 ${row.referrerName} 的奖励 ¥${formatMoney(row.amount)} 吗？`,
  })
  if (!ok) return
  await payReferralReward(row.id)
  toast.success('奖励已发放')
  loadData()
}

onMounted(() => {
  loadData()
})
</script>
