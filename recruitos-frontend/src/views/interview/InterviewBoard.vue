<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">面试看板</h2>
        <p class="page-subtitle">拖拽管理面试流程，实时跟踪面试状态</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="handleArrange">
          <el-icon><Plus /></el-icon>
          安排面试
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <StatCard label="待安排" :value="stats.pending" icon="Clock" color="#D97706" />
      <StatCard label="已安排" :value="stats.arranged" icon="Calendar" color="#3B82F6" />
      <StatCard label="进行中" :value="stats.inProgress" icon="VideoCamera" color="#059669" />
      <StatCard label="已完成" :value="stats.completed" icon="CircleCheck" color="#6B7280" />
    </div>

    <!-- 轮次切换 -->
    <div class="round-tabs">
      <el-tabs v-model="activeRound" @tab-change="handleRoundChange">
        <el-tab-pane label="初面" name="FIRST" />
        <el-tab-pane label="复试" name="SECOND" />
      </el-tabs>
    </div>

    <!-- 看板列 -->
    <div class="kanban-board">
      <div v-for="col in columns" :key="col.status" class="kanban-column">
        <div class="column-header">
          <div class="column-title-row">
            <span class="column-dot" :style="{ background: col.color }"></span>
            <span class="column-title">{{ col.label }}</span>
          </div>
          <span class="column-count">{{ getColumnData(col.status).length }}</span>
        </div>
        <div class="column-body">
          <div
            v-for="item in getColumnData(col.status)"
            :key="item.id"
            class="interview-card"
            @click="handleCardClick(item)"
          >
            <div class="card-top">
              <span class="candidate-name">{{ item.candidateName }}</span>
              <span class="card-format" :class="item.format === 'ONLINE' ? 'format-online' : 'format-offline'">
                {{ item.format === 'ONLINE' ? '线上' : '线下' }}
              </span>
            </div>
            <div class="card-job">{{ item.jobTitle }}</div>
            <div class="card-info">
              <div class="info-row">
                <el-icon><User /></el-icon>
                <span>{{ item.interviewerName }}</span>
              </div>
              <div class="info-row" v-if="item.scheduledTime">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTime(item.scheduledTime) }}</span>
              </div>
              <div class="info-row" v-if="item.location">
                <el-icon><Location /></el-icon>
                <span>{{ item.location }}</span>
              </div>
            </div>
            <div class="card-actions" @click.stop>
              <template v-if="col.status === 'PENDING_ARRANGE'">
                <el-button size="small" type="primary" @click="handleArrangeItem(item)">安排</el-button>
              </template>
              <template v-else-if="col.status === 'ARRANGED'">
                <el-button size="small" type="primary" @click="handleStart(item)">开始</el-button>
                <el-button size="small" @click="handleCancel(item)">取消</el-button>
              </template>
              <template v-else-if="col.status === 'IN_PROGRESS'">
                <el-button size="small" type="primary" @click="handleComplete(item)">完成</el-button>
              </template>
              <template v-else-if="col.status === 'COMPLETED'">
                <el-button size="small" @click="handleEvaluate(item)">评价</el-button>
                <el-button size="small" type="primary" plain @click="handleNextRound(item)">下一轮</el-button>
              </template>
            </div>
          </div>
          <div v-if="getColumnData(col.status).length === 0" class="column-empty">
            <el-icon :size="28" color="#CBD5E1"><Calendar /></el-icon>
            <span>暂无面试</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 安排面试弹窗 -->
    <el-dialog v-model="arrangeDialogVisible" title="安排面试" width="560px" destroy-on-close>
      <el-form :model="arrangeForm" label-width="100px">
        <el-form-item label="候选人" required>
          <el-select
            v-model="arrangeForm.candidateId"
            filterable
            remote
            :remote-method="searchCandidates"
            placeholder="输入候选人姓名搜索"
            style="width: 100%"
          >
            <el-option v-for="c in candidateOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" required>
          <el-select v-model="arrangeForm.jobId" placeholder="选择岗位" style="width: 100%">
            <el-option v-for="j in jobOptions" :key="j.id" :label="j.title" :value="j.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试轮次" required>
          <el-select v-model="arrangeForm.round" style="width: 100%">
            <el-option label="初面" value="FIRST" />
            <el-option label="复试" value="SECOND" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试官" required>
          <el-select v-model="arrangeForm.interviewerId" placeholder="选择面试官" style="width: 100%">
            <el-option v-for="u in interviewerOptions" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试形式" required>
          <el-radio-group v-model="arrangeForm.format">
            <el-radio label="ONLINE">线上</el-radio>
            <el-radio label="OFFLINE">线下</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="面试时间" required>
          <el-date-picker
            v-model="arrangeForm.scheduledTime"
            type="datetime"
            placeholder="选择面试时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="面试地点">
          <el-input v-model="arrangeForm.location" placeholder="会议室 / 线上链接" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="arrangeForm.remark" type="textarea" :rows="3" placeholder="面试备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="arrangeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitArrange" :loading="submitting">确认安排</el-button>
      </template>
    </el-dialog>

    <!-- 取消面试弹窗 -->
    <el-dialog v-model="cancelDialogVisible" title="取消面试" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="取消原因" required>
          <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="请输入取消原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitCancel" :loading="submitting">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import {
  getInterviewList, arrangeInterview, startInterview,
  completeInterview, cancelInterview, triggerNextRound,
} from '@/api/modules/interview'
import { getCandidateList } from '@/api/modules/candidate'
import { getJobList } from '@/api/modules/job'
import { getUserList } from '@/api/modules/user'

const router = useRouter()
const activeRound = ref('FIRST')

const stats = reactive({ pending: 0, arranged: 0, inProgress: 0, completed: 0 })

const columns = [
  { status: 'PENDING_ARRANGE', label: '待安排', color: '#D97706' },
  { status: 'ARRANGED', label: '已安排', color: '#3B82F6' },
  { status: 'IN_PROGRESS', label: '进行中', color: '#059669' },
  { status: 'COMPLETED', label: '已完成', color: '#6B7280' },
]

const interviewList = ref<any[]>([])

function getColumnData(status: string) {
  return interviewList.value.filter((item) => item.status === status)
}

const arrangeDialogVisible = ref(false)
const submitting = ref(false)
const arrangeForm = reactive({
  candidateId: null as number | null,
  jobId: null as number | null,
  round: 'FIRST',
  interviewerId: null as number | null,
  format: 'ONLINE',
  scheduledTime: '',
  location: '',
  remark: '',
})

const candidateOptions = ref<any[]>([])
const jobOptions = ref<any[]>([])
const interviewerOptions = ref<any[]>([])

const cancelDialogVisible = ref(false)
const cancelReason = ref('')
const currentCancelItem = ref<any>(null)

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

async function loadData() {
  try {
    const res: any = await getInterviewList({ round: activeRound.value })
    const data = res.data || res
    const list = Array.isArray(data) ? data : data.records || []
    interviewList.value = list
    stats.pending = list.filter((i: any) => i.status === 'PENDING_ARRANGE').length
    stats.arranged = list.filter((i: any) => i.status === 'ARRANGED').length
    stats.inProgress = list.filter((i: any) => i.status === 'IN_PROGRESS').length
    stats.completed = list.filter((i: any) => i.status === 'COMPLETED').length
  } catch (err: any) {
    ElMessage.error(err?.message || '加载面试列表失败')
  }
}

async function searchCandidates(query: string) {
  if (!query) { candidateOptions.value = []; return }
  try {
    const res: any = await getCandidateList({ keyword: query })
    const data = res.data || res
    const list = Array.isArray(data) ? data : data.records || []
    candidateOptions.value = list.map((c: any) => ({ id: c.id, name: c.name }))
  } catch { candidateOptions.value = [] }
}

function handleRoundChange() { loadData() }
function handleRefresh() { loadData(); ElMessage.success('刷新成功') }

async function handleArrange() {
  Object.assign(arrangeForm, {
    candidateId: null, jobId: null, round: activeRound.value,
    interviewerId: null, format: 'ONLINE', scheduledTime: '', location: '', remark: '',
  })
  try {
    const [jobRes, userRes] = await Promise.all([getJobList({}), getUserList()])
    const jobData = jobRes.data || jobRes
    jobOptions.value = (Array.isArray(jobData) ? jobData : jobData.records || []).map((j: any) => ({ id: j.id, title: j.title || j.name }))
    const userData = userRes.data || userRes
    interviewerOptions.value = (Array.isArray(userData) ? userData : userData.records || []).map((u: any) => ({ id: u.id, name: u.name || u.username }))
  } catch (err: any) { ElMessage.error(err?.message || '加载选项失败') }
  arrangeDialogVisible.value = true
}

function handleArrangeItem(item: any) {
  handleArrange()
  arrangeForm.round = item.round || activeRound.value
}

async function submitArrange() {
  if (!arrangeForm.candidateId || !arrangeForm.jobId || !arrangeForm.interviewerId || !arrangeForm.scheduledTime) {
    ElMessage.warning('请填写必要信息'); return
  }
  submitting.value = true
  try {
    await arrangeInterview(arrangeForm)
    ElMessage.success('安排成功')
    arrangeDialogVisible.value = false
    loadData()
  } catch (err: any) { ElMessage.error(err?.message || '安排失败') }
  finally { submitting.value = false }
}

async function handleStart(item: any) {
  try { await startInterview(item.id); ElMessage.success('面试已开始'); loadData() }
  catch (err: any) { ElMessage.error(err?.message || '操作失败') }
}

async function handleComplete(item: any) {
  try { await completeInterview(item.id); ElMessage.success('面试已完成'); loadData() }
  catch (err: any) { ElMessage.error(err?.message || '操作失败') }
}

function handleCancel(item: any) {
  currentCancelItem.value = item; cancelReason.value = ''; cancelDialogVisible.value = true
}

async function submitCancel() {
  if (!cancelReason.value.trim()) { ElMessage.warning('请输入取消原因'); return }
  submitting.value = true
  try {
    await cancelInterview(currentCancelItem.value.id, cancelReason.value)
    ElMessage.success('面试已取消'); cancelDialogVisible.value = false; loadData()
  } catch (err: any) { ElMessage.error(err?.message || '取消失败') }
  finally { submitting.value = false }
}

function handleEvaluate(item: any) { router.push({ path: '/interview/evaluation', query: { interviewId: item.id } }) }

async function handleNextRound(item: any) {
  try { await ElMessageBox.confirm('确定要触发下一轮面试吗？', '确认', { type: 'info' }); await triggerNextRound(item.id); ElMessage.success('已触发下一轮面试'); loadData() }
  catch { /* 取消 */ }
}

function handleCardClick(item: any) { ElMessage.info(`查看面试详情：${item.candidateName}`) }

onMounted(() => { loadData() })
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: $spacing-xl;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.round-tabs {
  margin-bottom: $spacing-lg;

  :deep(.el-tabs__header) { margin-bottom: 0; }
  :deep(.el-tabs__nav-wrap::after) { height: 1px; background: $border-color-light; }
}

// ── 看板 ────────────────────────────────
.kanban-board {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 500px;

  @media (max-width: 1200px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}

.kanban-column {
  background: $bg-page;
  border-radius: $border-radius;
  display: flex;
  flex-direction: column;
  min-height: 400px;
}

.column-header {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.column-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.column-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.column-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.column-count {
  font-size: 12px;
  font-weight: 600;
  color: $text-secondary;
  background: $bg-muted;
  padding: 1px 8px;
  border-radius: $border-radius-full;
}

.column-body {
  padding: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  max-height: calc(100vh - 380px);
}

.column-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 120px;
  gap: 8px;
  color: $text-placeholder;
  font-size: 13px;
}

// ── 面试卡片 ────────────────────────────
.interview-card {
  background: $bg-card;
  border-radius: $border-radius-sm;
  padding: 14px;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    box-shadow: $shadow-sm;
  }
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.candidate-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.card-format {
  font-size: 11px;
  font-weight: 500;
  padding: 1px 7px;
  border-radius: $border-radius-full;

  &.format-online {
    background: #DBEAFE;
    color: #1E40AF;
  }

  &.format-offline {
    background: #FEF3C7;
    color: #92400E;
  }
}

.card-job {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 10px;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;

  .info-row {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: $text-secondary;

    .el-icon {
      font-size: 13px;
      color: $text-placeholder;
    }
  }
}

.card-actions {
  display: flex;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid $border-color-light;
}
</style>
