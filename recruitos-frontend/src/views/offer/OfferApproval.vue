<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">Offer审批</h2>
    </div>

    <!-- Tab 导航 -->
    <el-tabs v-model="activeTab" class="approval-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="待我审批" name="pending" />
      <el-tab-pane label="我已审批" name="approved" />
      <el-tab-pane label="已驳回" name="rejected" />
    </el-tabs>

    <!-- 审批卡片列表 -->
    <div class="approval-list">
      <div v-if="filteredItems.length === 0" class="empty-state">
        <el-empty description="暂无数据" />
      </div>
      <div v-for="item in filteredItems" :key="item.id" class="approval-card">
        <div class="card-header">
          <div class="candidate-info">
            <h3 class="candidate-name">{{ item.candidateName }}</h3>
            <el-tag
              :type="getStatusType(item.status)"
              size="small"
              disable-transitions
            >
              {{ getStatusLabel(item.status) }}
            </el-tag>
          </div>
          <span class="submit-time">{{ item.submitTime }}</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">岗位</span>
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
          <el-button type="danger" @click="handleReject(item)">
            <el-icon><Close /></el-icon>
            驳回
          </el-button>
          <el-button type="success" @click="handleApprove(item)">
            <el-icon><Check /></el-icon>
            通过
          </el-button>
        </div>
      </div>
    </div>

    <!-- 审批对话框 -->
    <el-dialog
      v-model="approvalDialogVisible"
      :title="approvalDialogTitle"
      width="440px"
      destroy-on-close
    >
      <div class="approval-candidate-info">
        <p><strong>候选人：</strong>{{ currentApprovalItem?.candidateName }}</p>
        <p><strong>岗位：</strong>{{ currentApprovalItem?.jobTitle }}</p>
        <p><strong>薪资：</strong>{{ currentApprovalItem?.salary }}</p>
      </div>
      <el-form label-width="60px">
        <el-form-item label="备注">
          <el-input
            v-model="approvalRemark"
            type="textarea"
            :rows="3"
            :placeholder="approvalType === 'approve' ? '请输入审批意见（可选）' : '请输入驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button
          :type="approvalType === 'approve' ? 'success' : 'danger'"
          :loading="approvalLoading"
          @click="confirmApproval"
        >
          {{ approvalType === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'

const activeTab = ref('pending')

// 审批对话框
const approvalDialogVisible = ref(false)
const approvalType = ref<'approve' | 'reject'>('approve')
const approvalRemark = ref('')
const approvalLoading = ref(false)
const currentApprovalItem = ref<any>(null)

const approvalDialogTitle = computed(() =>
  approvalType.value === 'approve' ? '审批通过' : '审批驳回'
)

// Mock 数据
const approvalItems = ref([
  {
    id: 1,
    candidateName: '张伟',
    jobTitle: '高级前端工程师',
    department: '技术部',
    salary: '35K/月',
    submitter: '李经理',
    submitTime: '2026-06-05 10:30',
    status: 'PENDING',
    remark: '候选人技术能力突出，薪资在预算范围内',
  },
  {
    id: 2,
    candidateName: '李静',
    jobTitle: 'Java后端工程师',
    department: '技术部',
    salary: '40K/月',
    submitter: '王总监',
    submitTime: '2026-06-05 14:00',
    status: 'PENDING',
    remark: '有大厂经验，建议尽快确认',
  },
  {
    id: 3,
    candidateName: '周婷',
    jobTitle: '数据分析师',
    department: '数据部',
    salary: '30K/月',
    submitter: '赵经理',
    submitTime: '2026-06-04 16:20',
    status: 'PENDING',
    remark: '',
  },
  {
    id: 4,
    candidateName: '王磊',
    jobTitle: '产品经理',
    department: '产品部',
    salary: '45K/月',
    submitter: '孙经理',
    submitTime: '2026-06-03 09:15',
    status: 'APPROVED',
    remark: '审批通过，已安排发送Offer',
  },
  {
    id: 5,
    candidateName: '赵芳',
    jobTitle: 'UI设计师',
    department: '设计部',
    salary: '25K/月',
    submitter: '钱经理',
    submitTime: '2026-06-02 11:00',
    status: 'APPROVED',
    remark: '设计能力符合要求',
  },
  {
    id: 6,
    candidateName: '刘洋',
    jobTitle: '算法工程师',
    department: 'AI实验室',
    salary: '50K/月',
    submitter: '李总监',
    submitTime: '2026-06-01 15:30',
    status: 'REJECTED',
    remark: '薪资超出部门预算，建议调整后再提交',
  },
])

const filteredItems = computed(() => {
  return approvalItems.value.filter(item => {
    if (activeTab.value === 'pending') return item.status === 'PENDING'
    if (activeTab.value === 'approved') return item.status === 'APPROVED'
    if (activeTab.value === 'rejected') return item.status === 'REJECTED'
    return false
  })
})

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
  // 切换 tab 时可以重新加载数据
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

function confirmApproval() {
  if (approvalType.value === 'reject' && !approvalRemark.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  approvalLoading.value = true
  setTimeout(() => {
    const idx = approvalItems.value.findIndex(item => item.id === currentApprovalItem.value?.id)
    if (idx !== -1) {
      approvalItems.value[idx].status = approvalType.value === 'approve' ? 'APPROVED' : 'REJECTED'
      if (approvalRemark.value) {
        approvalItems.value[idx].remark = approvalRemark.value
      }
    }
    approvalLoading.value = false
    approvalDialogVisible.value = false
    ElMessage.success(approvalType.value === 'approve' ? '审批通过' : '已驳回')
  }, 500)
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.approval-tabs {
  margin-bottom: 20px;
}

.approval-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.approval-card {
  background: $bg-card;
  border: 1px solid $border-color-light;
  border-radius: 8px;
  padding: 20px;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
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

      .info-label {
        font-size: 12px;
        color: $text-secondary;
      }

      .info-value {
        font-size: 14px;
        color: $text-primary;
      }
    }

    .remark-section {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px dashed $border-color-light;
      display: flex;
      gap: 8px;

      .remark-text {
        font-size: 13px;
        color: $text-regular;
      }
    }
  }

  .card-footer {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid $border-color-light;
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

    strong {
      color: $text-primary;
    }
  }
}

.empty-state {
  padding: 60px 0;
}
</style>
