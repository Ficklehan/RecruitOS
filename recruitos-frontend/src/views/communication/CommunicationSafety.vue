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
          <div class="stat-value">{{ safetyStats.todayTotal }}</div>
          <div class="stat-label">今日审查数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #FEE2E2; color: #DC2626">
          <el-icon :size="24"><CircleClose /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ safetyStats.blocked }}</div>
          <div class="stat-label">拦截数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #FEF3C7; color: #D97706">
          <el-icon :size="24"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ safetyStats.warned }}</div>
          <div class="stat-label">告警数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #D1FAE5; color: #059669">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ safetyStats.passRate }}%</div>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Document, CircleClose, Warning, CircleCheck, View, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getSafetyLogList, getSafetyStats, reviewSafetyLog } from '@/api/modules/communication'

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

const records = ref<SafetyRecord[]>([])
const safetyStats = reactive({
  todayTotal: 0,
  blocked: 0,
  warned: 0,
  passRate: '0',
})

function mapSafetyRow(row: any): SafetyRecord {
  return {
    id: row.id,
    conversationId: String(row.conversationId || ''),
    checkType: row.checkType as CheckType,
    result: (row.checkResult || row.result || 'PASS') as Result,
    matchedContent: row.matchedContent || '',
    matchedKeywords: [],
    riskLevel: (row.riskLevel || '') as RiskLevel | '',
    rule: row.rule || '',
    sender: row.sender || '-',
    receiver: row.receiver || '-',
    action: row.action || '-',
    checkTime: row.checkedAt || row.checkTime || row.createdAt || '',
    remark: row.remark || '',
  }
}

async function loadSafetyData() {
  try {
    const [listRes, statsRes] = await Promise.all([
      getSafetyLogList({ pageNum: 1, pageSize: 100 }),
      getSafetyStats(),
    ])
    const list = listRes.data?.list || listRes.data?.records || []
    records.value = list.map(mapSafetyRow)
    const s = statsRes.data || {}
    safetyStats.todayTotal = s.todayTotal || s.totalToday || records.value.length
    safetyStats.blocked = s.blocked || s.blockCount || 0
    safetyStats.warned = s.warned || s.warnCount || 0
    safetyStats.passRate = String(s.passRate ?? s.passRatePercent ?? 0)
  } catch {
    records.value = []
  }
}

onMounted(loadSafetyData)

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

async function handleReview(row: SafetyRecord) {
  try {
    await reviewSafetyLog(row.id, 'APPROVE')
    ElMessage.success('复审完成')
    loadSafetyData()
  } catch {
    ElMessage.error('复审失败')
  }
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
