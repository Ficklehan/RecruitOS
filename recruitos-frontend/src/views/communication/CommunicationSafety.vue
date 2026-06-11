<template>
  <PageShell variant="list" title="安全审查" subtitle="监控沟通内容合规与风险拦截记录">
    <template #toolbar>
      <div class="stat-row">
        <div class="stat-card">
          <div class="stat-icon stat-icon--primary"><FileText class="h-6 w-6" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ safetyStats.todayTotal }}</div>
            <div class="stat-label">今日审查数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon--danger"><CircleX class="h-6 w-6" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ safetyStats.blocked }}</div>
            <div class="stat-label">拦截数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon--warning"><AlertTriangle class="h-6 w-6" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ safetyStats.warned }}</div>
            <div class="stat-label">告警数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon--success"><CircleCheck class="h-6 w-6" /></div>
          <div class="stat-info">
            <div class="stat-value">{{ safetyStats.passRate }}%</div>
            <div class="stat-label">通过率</div>
          </div>
        </div>
      </div>
    </template>

    <template #filters>
      <DateRangePicker v-model="filters.dateRange" />
      <RSelect v-model="filters.checkType" :options="checkTypeOptions" placeholder="检查类型" clearable class="w-full sm:w-36" />
      <RSelect v-model="filters.result" :options="resultOptions" placeholder="检查结果" clearable class="w-full sm:w-36" />
      <RSelect v-model="filters.riskLevel" :options="riskLevelOptions" placeholder="风险等级" clearable class="w-full sm:w-36" />
    </template>

    <RTable v-if="paginatedRecords.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[120px]">会话ID</RTableTh>
          <RTableTh class="w-[100px]">检查类型</RTableTh>
          <RTableTh class="w-[100px]">检查结果</RTableTh>
          <RTableTh class="min-w-[220px]">匹配内容</RTableTh>
          <RTableTh class="w-[100px]">风险等级</RTableTh>
          <RTableTh class="w-[120px]">处理动作</RTableTh>
          <RTableTh class="w-[170px]">检查时间</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in paginatedRecords" :key="row.id">
          <RTableCell>{{ row.conversationId }}</RTableCell>
          <RTableCell>
            <RBadge :variant="elTagTypeToBadge(checkTypeTagMap[row.checkType])">{{ checkTypeLabelMap[row.checkType] }}</RBadge>
          </RTableCell>
          <RTableCell>
            <RBadge :variant="elTagTypeToBadge(resultTagMap[row.result])">{{ resultLabelMap[row.result] }}</RBadge>
          </RTableCell>
          <RTableCell>
            <span v-if="row.matchedContent" v-html="highlightKeywords(row.matchedContent, row.matchedKeywords)" />
            <span v-else class="text-muted-foreground">-</span>
          </RTableCell>
          <RTableCell>
            <RBadge v-if="row.riskLevel" :variant="elTagTypeToBadge(riskTagMap[row.riskLevel])">{{ riskLabelMap[row.riskLevel] }}</RBadge>
            <span v-else class="text-muted-foreground">-</span>
          </RTableCell>
          <RTableCell>{{ row.action }}</RTableCell>
          <RTableCell class="text-muted-foreground">{{ row.checkTime }}</RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta v-else title="暂无审查记录" description="沟通内容审查后将在此显示" />

    <ListPagination
      v-if="filteredRecords.length > 0"
      v-model:page-num="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="filteredRecords.length"
    />

    <template #below>
      <RSheet v-model:open="drawerVisible">
        <RSheetContent class="overflow-y-auto">
          <h3 class="text-lg font-semibold mb-4">审查详情</h3>
          <template v-if="selectedRecord">
            <dl class="detail-dl">
              <div><dt>会话ID</dt><dd>{{ selectedRecord.conversationId }}</dd></div>
              <div>
                <dt>检查类型</dt>
                <dd>
                  <RBadge :variant="elTagTypeToBadge(checkTypeTagMap[selectedRecord.checkType])">
                    {{ checkTypeLabelMap[selectedRecord.checkType] }}
                  </RBadge>
                </dd>
              </div>
              <div>
                <dt>检查结果</dt>
                <dd>
                  <RBadge :variant="elTagTypeToBadge(resultTagMap[selectedRecord.result])">
                    {{ resultLabelMap[selectedRecord.result] }}
                  </RBadge>
                </dd>
              </div>
              <div>
                <dt>风险等级</dt>
                <dd>
                  <RBadge v-if="selectedRecord.riskLevel" :variant="elTagTypeToBadge(riskTagMap[selectedRecord.riskLevel])">
                    {{ riskLabelMap[selectedRecord.riskLevel] }}
                  </RBadge>
                  <span v-else>-</span>
                </dd>
              </div>
              <div>
                <dt>匹配内容</dt>
                <dd>
                  <div v-if="selectedRecord.matchedContent" class="detail-matched" v-html="highlightKeywords(selectedRecord.matchedContent, selectedRecord.matchedKeywords)" />
                  <span v-else>-</span>
                </dd>
              </div>
              <div><dt>匹配规则</dt><dd>{{ selectedRecord.rule || '-' }}</dd></div>
              <div><dt>发送人</dt><dd>{{ selectedRecord.sender }}</dd></div>
              <div><dt>接收人</dt><dd>{{ selectedRecord.receiver }}</dd></div>
              <div><dt>处理动作</dt><dd>{{ selectedRecord.action }}</dd></div>
              <div><dt>检查时间</dt><dd>{{ selectedRecord.checkTime }}</dd></div>
              <div><dt>备注</dt><dd>{{ selectedRecord.remark || '-' }}</dd></div>
            </dl>
          </template>
        </RSheetContent>
      </RSheet>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { FileText, CircleX, AlertTriangle, CircleCheck } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import DateRangePicker from '@/components/app/DateRangePicker.vue'
import {
  RSelect, RBadge, RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell, RSheet, RSheetContent,
} from '@/components/ui'
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

const checkTypeTagMap: Record<CheckType, string> = { KEYWORD: '', AI: 'primary', SENSITIVE: 'warning' }
const checkTypeLabelMap: Record<CheckType, string> = { KEYWORD: '关键词', AI: 'AI', SENSITIVE: '敏感词' }
const resultTagMap: Record<Result, string> = { PASS: 'success', BLOCK: 'danger', WARN: 'warning' }
const resultLabelMap: Record<Result, string> = { PASS: '通过', BLOCK: '拦截', WARN: '告警' }
const riskTagMap: Record<RiskLevel, string> = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }
const riskLabelMap: Record<RiskLevel, string> = { LOW: '低', MEDIUM: '中', HIGH: '高' }

const checkTypeOptions = [
  { label: '关键词', value: 'KEYWORD' },
  { label: 'AI', value: 'AI' },
  { label: '敏感词', value: 'SENSITIVE' },
]

const resultOptions = [
  { label: '通过', value: 'PASS' },
  { label: '拦截', value: 'BLOCK' },
  { label: '告警', value: 'WARN' },
]

const riskLevelOptions = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' },
]

const records = ref<SafetyRecord[]>([])
const safetyStats = reactive({ todayTotal: 0, blocked: 0, warned: 0, passRate: '0' })

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

const filters = reactive({
  dateRange: null as [string, string] | null,
  checkType: '' as string | undefined,
  result: '' as string | undefined,
  riskLevel: '' as string | undefined,
})

const filteredRecords = computed(() =>
  records.value.filter((r) => {
    if (filters.checkType && r.checkType !== filters.checkType) return false
    if (filters.result && r.result !== filters.result) return false
    if (filters.riskLevel && r.riskLevel !== filters.riskLevel) return false
    return true
  })
)

const pagination = reactive({ page: 1, pageSize: 10 })

const paginatedRecords = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredRecords.value.slice(start, start + pagination.pageSize)
})

const drawerVisible = ref(false)
const selectedRecord = ref<SafetyRecord | null>(null)

function openDrawer(row: SafetyRecord) {
  selectedRecord.value = row
  drawerVisible.value = true
}

async function handleReview(row: SafetyRecord) {
  try {
    await reviewSafetyLog(row.id, 'APPROVE')
    toast.success('复审完成')
    loadSafetyData()
  } catch {
    toast.error('复审失败')
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

function getRowActions(_row: SafetyRecord) {
  return [
    { command: 'view', label: '查看详情', icon: 'View', type: 'primary', primary: true },
    { command: 'edit', label: '复审', icon: 'Edit' },
  ]
}

function handleRowCommand(cmd: string, row: SafetyRecord) {
  if (cmd === 'view') openDrawer(row)
  else if (cmd === 'edit') handleReview(row)
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: $bg-card;
  border: none;
  border-radius: $border-radius;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.stat-icon--primary { background: $primary-color; }
.stat-icon--danger { background: $danger-color; }
.stat-icon--warning { background: $warning-color; }
.stat-icon--success { background: $success-color; }

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
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

  div { display: grid; grid-template-columns: 90px 1fr; gap: 8px; }
  dt { color: $text-secondary; }
  dd { margin: 0; }
}

.detail-matched { line-height: 1.6; }

:deep(.highlight-keyword) {
  background: $danger-lighter;
  color: $danger-color;
  padding: 1px 4px;
  border-radius: 3px;
  font-weight: 500;
}
</style>
