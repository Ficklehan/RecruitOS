<template>
  <PageShell title="录用通知审批">
<RTabs v-model="activeTab" class="approval-tabs" @update:model-value="handleTabChange">
      <RTabsList>
        <RTabsTrigger value="pending">待我审批</RTabsTrigger>
        <RTabsTrigger value="approved">我已审批</RTabsTrigger>
        <RTabsTrigger value="rejected">已驳回</RTabsTrigger>
      </RTabsList>
    </RTabs>

    <div class="approval-list relative">
      <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-background/60 min-h-[120px]">
        <Loader2 class="h-6 w-6 animate-spin text-primary" />
      </div>

      <div v-if="filteredItems.length === 0 && !loading" class="empty-state">
        <EmptyStateCta description="当前没有需要您处理的录用通知" />
      </div>
      <div v-for="item in filteredItems" :key="item.id" class="approval-card">
        <div class="card-header">
          <div class="candidate-info">
            <h3 class="candidate-name">{{ item.candidateName }}</h3>
            <RBadge :variant="elTagTypeToBadge(getStatusType(item.status))">
              {{ getStatusLabel(item.status) }}
            </RBadge>
          </div>
          <span class="submit-time">{{ item.submitTime }}</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">在招职位</span>
              <span class="info-value">{{ item.jobTitle }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">薪资</span>
              <span class="info-value">{{ item.salary }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">部门</span>
              <span class="info-value">{{ item.department }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">提交人</span>
              <span class="info-value">{{ item.submitter }}</span>
            </div>
          </div>
          <div v-if="item.remark" class="remark-section">
            <span class="info-label">备注</span>
            <span class="remark-text">{{ item.remark }}</span>
          </div>
        </div>
        <div v-if="item.status === 'PENDING'" class="card-footer">
          <RButton variant="destructive" @click="handleReject(item)">
            <X class="mr-2 h-4 w-4" />
            驳回
          </RButton>
          <RButton class="bg-green-600 hover:bg-green-700" @click="handleApprove(item)">
            <Check class="mr-2 h-4 w-4" />
            通过
          </RButton>
        </div>
      </div>
    </div>

    <RDialog v-model:open="approvalDialogVisible">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>{{ approvalDialogTitle }}</DialogTitle>
        </DialogHeader>
        <div class="approval-candidate-info">
          <p><strong>候选人：</strong>{{ currentApprovalItem?.candidateName }}</p>
          <p><strong>岗位：</strong>{{ currentApprovalItem?.jobTitle }}</p>
          <p><strong>薪资：</strong>{{ currentApprovalItem?.salary }}</p>
        </div>
        <FormField label="备注">
          <RTextarea
            v-model="approvalRemark"
            :rows="3"
            :placeholder="approvalType === 'approve' ? '请输入审批意见（可选）' : '请输入驳回原因'"
          />
        </FormField>
        <DialogFooter>
          <RButton variant="outline" @click="approvalDialogVisible = false">取消</RButton>
          <RButton
            :variant="approvalType === 'approve' ? 'default' : 'destructive'"
            :class="approvalType === 'approve' ? 'bg-green-600 hover:bg-green-700' : ''"
            :disabled="approvalLoading"
            @click="confirmApproval"
          >
            {{ approvalType === 'approve' ? '确认通过' : '确认驳回' }}
          </RButton>
        </DialogFooter>
      </DialogContent>
    </RDialog>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, computed, onMounted, watch } from 'vue'
import { Check, X, Loader2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import FormField from '@/components/app/FormField.vue'
import {
  RButton, RBadge, RTabs, RTabsList, RTabsTrigger,
  RDialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, RTextarea,
} from '@/components/ui'
import { getOfferList, approveOffer, rejectOffer } from '@/api/modules/offer'

const activeTab = ref('pending')
const loading = ref(false)
const approvalDialogVisible = ref(false)
const approvalType = ref<'approve' | 'reject'>('approve')
const approvalRemark = ref('')
const approvalLoading = ref(false)
const currentApprovalItem = ref<any>(null)
const approvalItems = ref<any[]>([])

const approvalDialogTitle = computed(() =>
  approvalType.value === 'approve' ? '审批通过' : '审批驳回'
)

const tabStatusMap: Record<string, string> = {
  pending: 'PENDING',
  approved: 'APPROVED',
  rejected: 'REJECTED',
}

const filteredItems = computed(() => approvalItems.value)

async function loadData() {
  loading.value = true
  try {
    const res = await getOfferList({
      status: tabStatusMap[activeTab.value],
      pageNum: 1,
      pageSize: 50,
    })
    approvalItems.value = (res.data?.list || []).map((o: any) => ({
      ...o,
      submitTime: o.createdAt || '-',
      submitter: o.createdBy ? `用户#${o.createdBy}` : '-',
      salary: o.salary != null ? `${o.salary}` : '-',
    }))
  } finally {
    loading.value = false
  }
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回',
  }
  return map[status] || status
}

function handleTabChange() {
  loadData()
}

function handleApprove(item: any) {
  approvalType.value = 'approve'
  currentApprovalItem.value = item
  approvalRemark.value = ''
  approvalDialogVisible.value = true
}

function handleReject(item: any) {
  approvalType.value = 'reject'
  currentApprovalItem.value = item
  approvalRemark.value = ''
  approvalDialogVisible.value = true
}

async function confirmApproval() {
  if (approvalType.value === 'reject' && !approvalRemark.value.trim()) {
    toast.error('请输入驳回原因')
    return
  }
  if (!currentApprovalItem.value) return
  approvalLoading.value = true
  try {
    if (approvalType.value === 'approve') {
      await approveOffer(currentApprovalItem.value.id, approvalRemark.value)
      toast.success('审批通过')
    } else {
      await rejectOffer(currentApprovalItem.value.id, approvalRemark.value)
      toast.success('已驳回')
    }
    approvalDialogVisible.value = false
    loadData()
  } finally {
    approvalLoading.value = false
  }
}

watch(activeTab, loadData)
onMounted(loadData)
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.approval-tabs { margin-bottom: 20px; }
.approval-list { display: flex; flex-direction: column; gap: 16px; }

.approval-card {
  background: $bg-card;
  border-radius: 12px;
  padding: 20px;
  transition: all $transition-base;
  box-shadow: $shadow-soft;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-card-hover;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .candidate-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .candidate-name {
        font-size: 16px;
        font-weight: 600;
        color: $text-primary;
        margin: 0;
      }
    }

    .submit-time {
      font-size: 13px;
      color: $text-secondary;
    }
  }

  .card-body {
    .info-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
    }

    .info-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .info-label { font-size: 12px; color: $text-secondary; }
      .info-value { font-size: 14px; color: $text-primary; }
    }

    .remark-section {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid var(--r-divider);
      display: flex;
      gap: 8px;

      .remark-text { font-size: 13px; color: $text-regular; }
    }
  }

  .card-footer {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid var(--r-divider);
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

.approval-candidate-info {
  background: $bg-muted;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;

  p {
    margin: 0;
    padding: 4px 0;
    font-size: 14px;
    color: $text-regular;

    strong { color: $text-primary; }
  }
}

.empty-state { padding: 60px 0; }
</style>
