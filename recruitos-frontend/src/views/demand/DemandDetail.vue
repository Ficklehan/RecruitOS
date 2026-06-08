<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" text>
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2 class="page-title">{{ detail.title || '招聘需求详情' }}</h2>
        <el-tag :type="getStatusType(detail.status)" size="large" disable-transitions>
          {{ getStatusLabel(detail.status) }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button
          v-if="detail.status === 'DRAFT' || detail.status === 'REJECTED'"
          @click="handleEdit"
        >
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button
          v-if="detail.status === 'DRAFT' || detail.status === 'REJECTED'"
          type="primary"
          @click="handleSubmit"
        >
          <el-icon><Promotion /></el-icon>
          提交审批
        </el-button>
        <el-button
          v-if="detail.status === 'APPROVED' || detail.status === 'JOB_CREATED'"
          type="success"
          @click="handleCreateJob"
        >
          创建在招职位
        </el-button>
        <el-button
          v-if="detail.status !== 'CLOSED' && detail.status !== 'COMPLETED'"
          type="danger"
          @click="handleClose"
        >
          <el-icon><Close /></el-icon>
          关闭需求
        </el-button>
      </div>
    </div>

    <!-- 基本信息卡片 -->
    <div class="detail-grid">
      <div class="info-card">
        <h3 class="card-title">基本信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">需求编号</span>
            <span class="info-value">{{ detail.demandNo || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">所属部门</span>
            <span class="info-value">{{ detail.orgId || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">职级</span>
            <span class="info-value">{{ detail.jobLevel || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">招聘人数</span>
            <span class="info-value">{{ detail.headCount || '-' }} 人</span>
          </div>
          <div class="info-item">
            <span class="info-label">薪酬范围</span>
            <span class="info-value">{{ formatSalary(detail.salaryMin) }}K - {{ formatSalary(detail.salaryMax) }}K / 月</span>
          </div>
          <div class="info-item">
            <span class="info-label">紧急程度</span>
            <span class="info-value">
              <el-tag :type="getUrgencyType(detail.urgency)" size="small">
                {{ getUrgencyLabel(detail.urgency) }}
              </el-tag>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">期望到岗日期</span>
            <span class="info-value">{{ detail.expectedOnboardDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">需求原因</span>
            <span class="info-value">{{ getReasonLabel(detail.reason) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">工作地点</span>
            <span class="info-value">
              <el-tag v-for="loc in parseLocations(detail.workLocations)" :key="loc" size="small" class="mr-8">{{ loc }}</el-tag>
              <span v-if="!detail.workLocations">-</span>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">汇报对象</span>
            <span class="info-value">{{ detail.reportToName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建人</span>
            <span class="info-value">{{ detail.createdBy || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ detail.createdAt || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 岗位职责卡片 -->
    <div class="info-card">
      <h3 class="card-title">职位描述</h3>
      <div class="card-content">
        <pre class="text-block">{{ detail.jobDuty || '暂无内容' }}</pre>
      </div>
    </div>

    <!-- 任职要求卡片 -->
    <div class="info-card">
      <h3 class="card-title">任职要求</h3>
      <div class="card-content">
        <pre class="text-block">{{ detail.jobRequirement || '暂无内容' }}</pre>
      </div>
    </div>

    <!-- 面试官配置卡片 -->
    <div class="info-card">
      <h3 class="card-title">面试官配置</h3>
      <div class="interviewer-grid">
        <div class="interviewer-section">
          <h4 class="interviewer-label">初面面试官</h4>
          <div class="interviewer-list">
            <el-tag
              v-for="person in parseIds(detail.initialInterviewerIds)"
              :key="person"
              type="info"
              effect="plain"
              class="interviewer-tag"
            >
              <el-icon><User /></el-icon>
              {{ person }}
            </el-tag>
            <span v-if="!detail.initialInterviewerIds" class="empty-text">暂未配置</span>
          </div>
        </div>
        <div class="interviewer-section">
          <h4 class="interviewer-label">复试面试官</h4>
          <div class="interviewer-list">
            <el-tag
              v-for="person in parseIds(detail.finalInterviewerIds)"
              :key="person"
              type="info"
              effect="plain"
              class="interviewer-tag"
            >
              <el-icon><User /></el-icon>
              {{ person }}
            </el-tag>
            <span v-if="!detail.finalInterviewerIds" class="empty-text">暂未配置</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 审批记录卡片 -->
    <div class="info-card">
      <h3 class="card-title">审批记录</h3>
      <div class="approval-timeline" v-if="approvalRecords.length > 0">
        <el-timeline>
          <el-timeline-item
            v-for="(record, index) in approvalRecords"
            :key="index"
            :timestamp="record.time"
            :type="getApprovalTimelineType(record.action)"
            :hollow="index > 0"
            placement="top"
          >
            <div class="timeline-content">
              <div class="timeline-header">
                <span class="timeline-node">{{ record.nodeName }}</span>
                <el-tag :type="getApprovalTagType(record.action)" size="small">
                  {{ getApprovalLabel(record.action) }}
                </el-tag>
              </div>
              <div class="timeline-info">
                <span class="timeline-approver">
                  <el-icon><User /></el-icon>
                  {{ record.approver }}
                </span>
              </div>
              <div v-if="record.comment" class="timeline-comment">
                <el-icon><ChatLineSquare /></el-icon>
                {{ record.comment }}
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <div v-else class="empty-text">暂无审批记录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, Promotion, Close, User, ChatLineSquare } from '@element-plus/icons-vue'
import { demandStatusLabel } from '@/constants/businessLabels'
import { getDemandDetail, submitDemand, closeDemand } from '@/api/modules/demand'

const router = useRouter()
const route = useRoute()
const demandId = Number(route.params.id)

const detail = ref<any>({})
const approvalRecords = ref<any[]>([])

// 格式化薪资
function formatSalary(val: any): string {
  if (val == null) return '-'
  const num = Number(val)
  return isNaN(num) ? String(val) : num.toFixed(0)
}

// 解析 JSON 数组字符串
function parseIds(idsStr: string | null): string[] {
  if (!idsStr) return []
  try {
    const parsed = JSON.parse(idsStr)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return idsStr.split(',').map(s => s.trim()).filter(Boolean)
  }
}

// 解析工作地点
function parseLocations(locStr: string | null): string[] {
  if (!locStr) return []
  try {
    const parsed = JSON.parse(locStr)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return [locStr]
  }
}

// 状态映射
function getStatusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger',
    JOB_CREATED: 'primary', RECRUITING: 'warning', COMPLETED: 'success', CLOSED: 'info',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  return demandStatusLabel(status)
}

function handleCreateJob() {
  router.push({
    path: '/planning/jobs/create',
    query: { demandNo: detail.value.demandNo || '', demandId: String(demandId) },
  })
}

function getUrgencyType(level: string) {
  const map: Record<string, string> = { NORMAL: 'info', URGENT: 'warning', CRITICAL: 'danger' }
  return map[level] || 'info'
}

function getUrgencyLabel(level: string) {
  const map: Record<string, string> = { NORMAL: '普通', URGENT: '紧急', CRITICAL: '特急' }
  return map[level] || level
}

function getReasonLabel(reason: string) {
  const map: Record<string, string> = {
    NEW: '新增编制', REPLACEMENT: '离职替补',
    EXPANSION: '业务扩张', TEMPORARY: '项目临时用工',
  }
  return map[reason] || reason || '-'
}

// 审批时间线类型
function getApprovalTimelineType(action: string) {
  const map: Record<string, string> = { approve: 'success', reject: 'danger', submit: 'primary' }
  return map[action] || 'primary'
}

function getApprovalTagType(action: string) {
  const map: Record<string, string> = { approve: 'success', reject: 'danger', submit: 'info' }
  return map[action] || 'info'
}

function getApprovalLabel(action: string) {
  const map: Record<string, string> = { approve: '通过', reject: '驳回', submit: '提交' }
  return map[action] || action
}

// 加载详情
async function loadDetail() {
  if (!demandId || isNaN(demandId)) {
    return
  }
  try {
    const res: any = await getDemandDetail(demandId)
    detail.value = res.data || {}
    approvalRecords.value = res.data?.approvalRecords || []
  } catch {
    detail.value = {}
    approvalRecords.value = []
  }
}

function handleEdit() {
  router.push(`/planning/demands/create?id=${demandId}`)
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确定要提交该需求进行审批吗？', '提交确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await submitDemand(demandId)
    ElMessage.success('已提交审批')
    loadDetail()
  } catch {
    // 取消
  }
}

async function handleClose() {
  try {
    await ElMessageBox.confirm('确定要关闭该需求吗？关闭后不可恢复。', '关闭确认', {
      confirmButtonText: '确定关闭',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await closeDemand(demandId)
    ElMessage.success('需求已关闭')
    loadDetail()
  } catch {
    // 取消
  }
}

function goBack() {
  router.push('/planning/demands')
}

onMounted(() => {
  loadDetail()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.info-card {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 16px;
  transition: box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
  }
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-color-light;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 13px;
  color: $text-secondary;
}

.info-value {
  font-size: 14px;
  color: $text-primary;
  font-weight: 500;
}

.text-block {
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  color: $text-regular;
  line-height: 1.8;
  margin: 0;
}

.interviewer-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.interviewer-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-regular;
  margin-bottom: 12px;
}

.interviewer-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.interviewer-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-text {
  font-size: 14px;
  color: $text-placeholder;
}

// 审批时间线
.timeline-content {
  padding-bottom: 8px;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.timeline-node {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.timeline-info {
  margin-bottom: 4px;
}

.timeline-approver {
  font-size: 13px;
  color: $text-regular;
  display: flex;
  align-items: center;
  gap: 4px;
}

.timeline-comment {
  font-size: 13px;
  color: $text-secondary;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin-top: 4px;
  padding: 8px 12px;
  background: $bg-muted;
  border-radius: 4px;
}

.mr-8 {
  margin-right: 8px;
}

// 响应式
@media (max-width: 1024px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .interviewer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
