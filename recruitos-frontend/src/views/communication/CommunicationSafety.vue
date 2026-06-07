<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">安全审查</h2>
    </div>

    <!-- Stats Cards -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: #EFF6FF; color: #3B82F6">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">1,286</div>
          <div class="stat-label">今日审查数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #FEE2E2; color: #DC2626">
          <el-icon :size="24"><CircleClose /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">23</div>
          <div class="stat-label">拦截数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #FEF3C7; color: #D97706">
          <el-icon :size="24"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">47</div>
          <div class="stat-label">告警数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #D1FAE5; color: #059669">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">94.5%</div>
          <div class="stat-label">通过率</div>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 280px"
        value-format="YYYY-MM-DD"
      />
      <el-select v-model="filters.checkType" placeholder="检查类型" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="关键词" value="KEYWORD" />
        <el-option label="AI" value="AI" />
        <el-option label="敏感词" value="SENSITIVE" />
      </el-select>
      <el-select v-model="filters.result" placeholder="检查结果" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="通过" value="PASS" />
        <el-option label="拦截" value="BLOCK" />
        <el-option label="告警" value="WARN" />
      </el-select>
      <el-select v-model="filters.riskLevel" placeholder="风险等级" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="低" value="LOW" />
        <el-option label="中" value="MEDIUM" />
        <el-option label="高" value="HIGH" />
      </el-select>
    </div>

    <!-- Table -->
    <el-table :data="filteredRecords" style="width: 100%">
      <el-table-column prop="conversationId" label="会话ID" width="120" />
      <el-table-column prop="checkType" label="检查类型" width="100">
        <template #default="{ row }">
          <el-tag :type="checkTypeTagMap[row.checkType]" size="small">
            {{ checkTypeLabelMap[row.checkType] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="result" label="检查结果" width="100">
        <template #default="{ row }">
          <el-tag :type="resultTagMap[row.result]" size="small">
            {{ resultLabelMap[row.result] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="matchedContent" label="匹配内容" min-width="220">
        <template #default="{ row }">
          <span v-if="row.matchedContent" v-html="highlightKeywords(row.matchedContent, row.matchedKeywords)" />
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="riskLevel" label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.riskLevel" :type="riskTagMap[row.riskLevel]" size="small" effect="dark">
            {{ riskLabelMap[row.riskLevel] }}
          </el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="action" label="处理动作" width="120" />
      <el-table-column prop="checkTime" label="检查时间" width="170" />
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="View" @click="openDrawer(row)">查看</el-button>
          <el-button type="warning" link :icon="RefreshRight" @click="handleReview(row)">复审</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- Detail Drawer -->
    <el-drawer v-model="drawerVisible" title="审查详情" size="520px">
      <template v-if="selectedRecord">
        <el-descriptions :column="1">
          <el-descriptions-item label="会话ID">{{ selectedRecord.conversationId }}</el-descriptions-item>
          <el-descriptions-item label="检查类型">
            <el-tag :type="checkTypeTagMap[selectedRecord.checkType]" size="small">
              {{ checkTypeLabelMap[selectedRecord.checkType] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检查结果">
            <el-tag :type="resultTagMap[selectedRecord.result]" size="small">
              {{ resultLabelMap[selectedRecord.result] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag v-if="selectedRecord.riskLevel" :type="riskTagMap[selectedRecord.riskLevel]" size="small" effect="dark">
              {{ riskLabelMap[selectedRecord.riskLevel] }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="匹配内容">
            <div v-if="selectedRecord.matchedContent" class="detail-matched" v-html="highlightKeywords(selectedRecord.matchedContent, selectedRecord.matchedKeywords)" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="匹配规则">{{ selectedRecord.rule || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发送人">{{ selectedRecord.sender }}</el-descriptions-item>
          <el-descriptions-item label="接收人">{{ selectedRecord.receiver }}</el-descriptions-item>
          <el-descriptions-item label="处理动作">{{ selectedRecord.action }}</el-descriptions-item>
          <el-descriptions-item label="检查时间">{{ selectedRecord.checkTime }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ selectedRecord.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { Document, CircleClose, Warning, CircleCheck, View, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

type CheckType = 'KEYWORD' | 'AI' | 'SENSITIVE'
type Result = 'PASS' | 'BLOCK' | 'WARN'
type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

interface SafetyRecord {
  id: number
  conversationId: string
  checkType: CheckType
  result: Result
  matchedContent: string
  matchedKeywords: string[]
  riskLevel: RiskLevel | ''
  rule: string
  sender: string
  receiver: string
  action: string
  checkTime: string
  remark: string
}

const checkTypeTagMap: Record<CheckType, string> = {
  KEYWORD: '',
  AI: 'primary',
  SENSITIVE: 'warning',
}

const checkTypeLabelMap: Record<CheckType, string> = {
  KEYWORD: '关键词',
  AI: 'AI',
  SENSITIVE: '敏感词',
}

const resultTagMap: Record<Result, string> = {
  PASS: 'success',
  BLOCK: 'danger',
  WARN: 'warning',
}

const resultLabelMap: Record<Result, string> = {
  PASS: '通过',
  BLOCK: '拦截',
  WARN: '告警',
}

const riskTagMap: Record<RiskLevel, string> = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger',
}

const riskLabelMap: Record<RiskLevel, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
}

// Mock data
const records = ref<SafetyRecord[]>([
  {
    id: 1,
    conversationId: 'CONV-20260606-001',
    checkType: 'KEYWORD',
    result: 'BLOCK',
    matchedContent: '您的银行账户信息需要核实，请提供银行卡号',
    matchedKeywords: ['银行卡号', '银行账户'],
    riskLevel: 'HIGH',
    rule: '金融信息索取规则',
    sender: '候选人-张三',
    receiver: 'AI助手',
    action: '消息已拦截',
    checkTime: '2026-06-06 14:32:15',
    remark: '疑似钓鱼信息',
  },
  {
    id: 2,
    conversationId: 'CONV-20260606-002',
    checkType: 'AI',
    result: 'WARN',
    matchedContent: '这个岗位的薪资待遇和具体工作内容能详细说一下吗',
    matchedKeywords: ['薪资'],
    riskLevel: 'MEDIUM',
    rule: 'AI语义分析-敏感话题',
    sender: '候选人-李四',
    receiver: 'HR-王经理',
    action: '已标记告警',
    checkTime: '2026-06-06 14:18:42',
    remark: '涉及薪资话题，建议谨慎回复',
  },
  {
    id: 3,
    conversationId: 'CONV-20260606-003',
    checkType: 'SENSITIVE',
    result: 'BLOCK',
    matchedContent: '我们公司可以帮你办假学历，不用担心背景调查',
    matchedKeywords: ['假学历', '背景调查'],
    riskLevel: 'HIGH',
    rule: '欺诈信息规则',
    sender: '未知用户',
    receiver: '候选人-王五',
    action: '消息已拦截并举报',
    checkTime: '2026-06-06 13:55:08',
    remark: '严重违规内容',
  },
  {
    id: 4,
    conversationId: 'CONV-20260606-004',
    checkType: 'KEYWORD',
    result: 'PASS',
    matchedContent: '',
    matchedKeywords: [],
    riskLevel: '',
    rule: '',
    sender: 'HR-赵经理',
    receiver: '候选人-赵六',
    action: '已放行',
    checkTime: '2026-06-06 13:40:33',
    remark: '',
  },
  {
    id: 5,
    conversationId: 'CONV-20260606-005',
    checkType: 'AI',
    result: 'PASS',
    matchedContent: '',
    matchedKeywords: [],
    riskLevel: '',
    rule: '',
    sender: 'AI助手',
    receiver: '候选人-孙七',
    action: '已放行',
    checkTime: '2026-06-06 12:28:19',
    remark: '',
  },
  {
    id: 6,
    conversationId: 'CONV-20260606-006',
    checkType: 'SENSITIVE',
    result: 'WARN',
    matchedContent: '你们公司加班严重吗？996吗？',
    matchedKeywords: ['996', '加班'],
    riskLevel: 'LOW',
    rule: '敏感话题规则',
    sender: '候选人-周八',
    receiver: 'HR-钱经理',
    action: '已标记告警',
    checkTime: '2026-06-06 11:15:50',
    remark: '涉及工作制度话题',
  },
  {
    id: 7,
    conversationId: 'CONV-20260606-007',
    checkType: 'KEYWORD',
    result: 'PASS',
    matchedContent: '',
    matchedKeywords: [],
    riskLevel: '',
    rule: '',
    sender: 'HR-吴经理',
    receiver: '候选人-吴九',
    action: '已放行',
    checkTime: '2026-06-06 10:45:22',
    remark: '',
  },
  {
    id: 8,
    conversationId: 'CONV-20260606-008',
    checkType: 'AI',
    result: 'BLOCK',
    matchedContent: '你把其他候选人的简历发给我看看，我帮你参考一下',
    matchedKeywords: ['其他候选人', '简历'],
    riskLevel: 'HIGH',
    rule: 'AI语义分析-隐私泄露',
    sender: '候选人-郑十',
    receiver: 'HR-冯经理',
    action: '消息已拦截',
    checkTime: '2026-06-06 09:30:11',
    remark: '涉嫌试图获取其他候选人隐私信息',
  },
])

// Filters
const filters = reactive({
  dateRange: null as [string, string] | null,
  checkType: '',
  result: '',
  riskLevel: '',
})

const filteredRecords = computed(() => {
  return records.value.filter((r) => {
    if (filters.checkType && r.checkType !== filters.checkType) return false
    if (filters.result && r.result !== filters.result) return false
    if (filters.riskLevel && r.riskLevel !== filters.riskLevel) return false
    return true
  })
})

// Pagination
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: computed(() => filteredRecords.value.length),
})

// Drawer
const drawerVisible = ref(false)
const selectedRecord = ref<SafetyRecord | null>(null)

function openDrawer(row: SafetyRecord) {
  selectedRecord.value = row
  drawerVisible.value = true
}

function handleReview(row: SafetyRecord) {
  ElMessage.info(`正在复审会话 ${row.conversationId}...`)
}

function highlightKeywords(content: string, keywords: string[]): string {
  if (!keywords || keywords.length === 0) return content
  let result = content
  for (const kw of keywords) {
    const regex = new RegExp(`(${kw})`, 'g')
    result = result.replace(regex, '<span class="highlight-keyword">$1</span>')
  }
  return result
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';
.page-container {
  padding: 20px;
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

/* Stats Cards */
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
  border: 1px solid $border-color-light;
  border-radius: 8px;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 4px;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.text-muted {
  color: $text-placeholder;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-matched {
  line-height: 1.6;
}

:deep(.highlight-keyword) {
  background: $danger-lighter;
  color: $danger-color;
  padding: 1px 4px;
  border-radius: 3px;
  font-weight: 500;
}
</style>
