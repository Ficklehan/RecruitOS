<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">奖励管理</h2>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">待发放金额</div>
        <div class="stat-value warning">¥ 18,000</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已发放金额</div>
        <div class="stat-value success">¥ 52,000</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本月发放</div>
        <div class="stat-value primary">¥ 8,000</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">奖励总数</div>
        <div class="stat-value">25</div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 140px">
        <el-option
          v-for="s in statusOptions"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
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
      <el-table :data="rewardList" stripe highlight-current-row style="width: 100%">
        <el-table-column prop="referrerName" label="推荐人" width="120" />
        <el-table-column prop="candidateName" label="候选人" width="120" />
        <el-table-column prop="rewardType" label="奖励类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getRewardTypeTag(row.rewardType)" size="small" disable-transitions>
              {{ getRewardTypeLabel(row.rewardType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.rewardType === 'CASH'" class="amount-text">¥ {{ row.amount.toLocaleString() }}</span>
            <span v-else class="amount-text">{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" disable-transitions>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approver" label="审批人" width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success" link size="small"
              @click="handleApprove(row)"
            >审批</el-button>
            <el-button
              v-if="row.status === 'APPROVED'"
              type="primary" link size="small"
              @click="handlePay(row)"
            >发放</el-button>
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { getReferralRewardList } from '@/api/modules/referral'

// 查询参数
const queryParams = reactive({
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const rewardList = ref<any[]>([])

// 状态选项
const statusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '已审批', value: 'APPROVED' },
  { label: '已发放', value: 'PAID' },
  { label: '已取消', value: 'CANCELLED' },
]

// 奖励类型标签
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

// 状态标签映射
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

// 加载数据
async function loadData() {
  const params: any = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  if (queryParams.status) params.status = queryParams.status
  const res = await getReferralRewardList(params)
  rewardList.value = res.data.records || res.data.list || res.data || []
  total.value = res.data.total || rewardList.value.length
}

function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.status = ''
  handleSearch()
}

function handleView(row: any) {
  ElMessage.info(`查看奖励详情: ${row.candidateName}`)
}

async function handleApprove(row: any) {
  try {
    await ElMessageBox.confirm(`确定审批通过 ${row.referrerName} 推荐 ${row.candidateName} 的奖励吗？`, '审批确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    row.status = 'APPROVED'
    row.approver = '当前用户'
    ElMessage.success('审批通过')
  } catch {
    // 取消操作
  }
}

async function handlePay(row: any) {
  try {
    await ElMessageBox.confirm(`确定发放 ${row.referrerName} 的奖励 ¥${row.amount.toLocaleString()} 吗？`, '发放确认', {
      confirmButtonText: '确定发放',
      cancelButtonText: '取消',
      type: 'info',
    })
    row.status = 'PAID'
    ElMessage.success('奖励已发放')
  } catch {
    // 取消操作
  }
}

onMounted(() => {
  loadData()
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
  background: $bg-card;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

  .stat-label {
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: $text-primary;

    &.primary {
      color: $primary-color;
    }

    &.success {
      color: $success-color;
    }

    &.warning {
      color: $warning-color;
    }
  }
}

.amount-text {
  font-weight: 500;
  color: $text-primary;
}
</style>
