<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <div>
        <h2 class="page-title">招聘需求审批</h2>
        <p class="page-subtitle">处理待审批的招聘需求，通过后申请人可创建在招职位</p>
      </div>
    </div>

    <div class="approval-tabs">
      <RTabs v-model="activeTab" @update:model-value="handleTabChange">
        <RTabsList>
          <RTabsTrigger value="PENDING">待审批</RTabsTrigger>
          <RTabsTrigger value="APPROVED">已审批</RTabsTrigger>
          <RTabsTrigger value="REJECTED">已驳回</RTabsTrigger>
        </RTabsList>
      </RTabs>
    </div>

    <div class="approval-list">
      <div v-for="item in approvalList" :key="item.id" class="approval-card">
        <div class="card-main">
          <div class="card-header">
            <span class="card-title">{{ item.title }}</span>
            <RBadge :variant="getUrgencyVariant(item.urgencyLevel)" size="sm">
              {{ getUrgencyLabel(item.urgencyLevel) }}
            </RBadge>
          </div>
          <div class="card-meta">
            <div class="meta-item">
              <User class="h-4 w-4" />
              <span>申请人：{{ item.applicant }}</span>
            </div>
            <div class="meta-item">
              <Clock class="h-4 w-4" />
              <span>申请时间：{{ item.applyTime }}</span>
            </div>
            <div class="meta-item">
              <Users class="h-4 w-4" />
              <span>招聘人数：{{ item.headcount }} 人</span>
            </div>
            <div class="meta-item">
              <Building2 class="h-4 w-4" />
              <span>部门：{{ item.department }}</span>
            </div>
          </div>
          <div class="card-footer" v-if="activeTab === 'APPROVED' || activeTab === 'REJECTED'">
            <div class="footer-item">
              <span class="footer-label">审批时间：</span>
              <span>{{ item.approvalTime }}</span>
            </div>
            <div class="footer-item" v-if="item.comment">
              <span class="footer-label">审批意见：</span>
              <span>{{ item.comment }}</span>
            </div>
          </div>
        </div>
        <div class="card-actions">
          <template v-if="activeTab === 'PENDING'">
            <RButton variant="primary" @click="handleApprove(item)">
              <Check class="h-4 w-4" />
              通过
            </RButton>
            <RButton variant="danger" @click="openRejectDialog(item)">
              <X class="h-4 w-4" />
              驳回
            </RButton>
          </template>
          <RButton variant="ghost" @click="handleViewDetail(item)">
            <Eye class="h-4 w-4" />
            详情
          </RButton>
        </div>
      </div>

      <div v-if="approvalList.length === 0" class="empty-state">
        <CheckCircle2 class="h-16 w-16 text-text-placeholder" />
        <h3>{{ emptyText }}</h3>
        <p>{{ emptyDesc }}</p>
      </div>
    </div>

    <div class="pagination-wrapper" v-if="total > 0">
      <RPagination
        v-model="queryParams.pageNum"
        :total="total"
        :page-size="queryParams.pageSize"
        @update:model-value="loadData"
      />
    </div>

    <RDialog v-model="rejectDialogVisible" title="驳回审批" width="480px">
      <div class="reject-form">
        <div class="form-field">
          <label class="form-label">需求标题</label>
          <RInput :model-value="rejectTarget?.title" disabled />
        </div>
        <div class="form-field">
          <label class="form-label">驳回原因 <span class="text-danger">*</span></label>
          <RInput
            v-model="rejectForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因（必填）"
          />
          <p v-if="rejectError" class="form-error">{{ rejectError }}</p>
        </div>
      </div>
      <template #footer>
        <RButton variant="outline" @click="rejectDialogVisible = false">取消</RButton>
        <RButton variant="danger" :loading="rejecting" @click="handleReject">确认驳回</RButton>
      </template>
    </RDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Clock, Users, Building2, Check, X, Eye, CheckCircle2 } from 'lucide-vue-next'
import { RButton, RBadge, RDialog, RInput, RPagination, RTabs, RTabsList, RTabsTrigger } from '@/components/ui'
import { toast } from '@/lib/toast'
import { getMyApprovals, approveDemand, rejectDemand } from '@/api/modules/demand'

const router = useRouter()

const activeTab = ref('PENDING')
const total = ref(0)
const approvalList = ref<any[]>([])

const queryParams = reactive({
  status: 'PENDING',
  pageNum: 1,
  pageSize: 20,
})

const rejectDialogVisible = ref(false)
const rejecting = ref(false)
const rejectTarget = ref<any>(null)
const rejectForm = reactive({ comment: '' })
const rejectError = ref('')

const emptyText = computed(() => {
  const map: Record<string, string> = {
    PENDING: '暂无待审批需求',
    APPROVED: '暂无已审批需求',
    REJECTED: '暂无已驳回需求',
  }
  return map[activeTab.value] || '暂无数据'
})

const emptyDesc = computed(() => {
  const map: Record<string, string> = {
    PENDING: '所有需求都已处理完毕',
    APPROVED: '您还没有审批过需求',
    REJECTED: '您还没有驳回过需求',
  }
  return map[activeTab.value] || ''
})

function getUrgencyVariant(level: string): 'info' | 'warning' | 'danger' {
  const map: Record<string, 'info' | 'warning' | 'danger'> = { NORMAL: 'info', URGENT: 'warning', CRITICAL: 'danger' }
  return map[level] || 'info'
}

function getUrgencyLabel(level: string) {
  const map: Record<string, string> = { NORMAL: '普通', URGENT: '紧急', CRITICAL: '特急' }
  return map[level] || level
}

async function loadData() {
  const res: any = await getMyApprovals(queryParams)
  approvalList.value = res.data?.list || res.data?.records || []
  total.value = res.data?.total || 0
}

function handleTabChange(tab: string | number) {
  queryParams.status = tab as string
  queryParams.pageNum = 1
  loadData()
}

async function handleApprove(item: any) {
  await approveDemand(item.id)
  toast.success('审批已通过')
  loadData()
}

function openRejectDialog(item: any) {
  rejectTarget.value = item
  rejectForm.comment = ''
  rejectError.value = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!rejectForm.comment || rejectForm.comment.length < 2) {
    rejectError.value = '请输入至少2个字符的驳回原因'
    return
  }
  rejectError.value = ''

  rejecting.value = true
  try {
    await rejectDemand(rejectTarget.value.id, rejectForm.comment)
    toast.success('已驳回')
    rejectDialogVisible.value = false
    loadData()
  } finally {
    rejecting.value = false
  }
}

function handleViewDetail(item: any) {
  router.push(`/planning/demands/${item.demandId || item.id}`)
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.approval-tabs {
  background: var(--r-bg-card);
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  padding: 8px 24px;
  margin-bottom: 16px;
}

.approval-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.approval-card {
  background: var(--r-bg-card);
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  padding: 24px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
  }
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--r-text-primary);
  }
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;

  .meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--r-text-secondary);
  }
}

.card-footer {
  padding-top: 12px;
  border-top: 1px solid var(--r-border-divider);

  .footer-item {
    font-size: 13px;
    color: var(--r-text-secondary);
    margin-bottom: 4px;

    .footer-label {
      color: var(--r-text-placeholder);
    }
  }
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  gap: 12px;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--r-text-primary);
    margin: 0;
  }

  p {
    font-size: 13px;
    color: var(--r-text-secondary);
    margin: 0;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.reject-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--r-text-primary);
}

.form-error {
  font-size: 12px;
  color: var(--r-color-danger);
  margin: 0;
}
</style>
