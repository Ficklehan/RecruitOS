<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">招聘需求审批</h2>
        <p class="page-subtitle">处理待审批的招聘需求，通过后申请人可创建在招职位</p>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="approval-tabs">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待审批" name="PENDING" />
        <el-tab-pane label="已审批" name="APPROVED" />
        <el-tab-pane label="已驳回" name="REJECTED" />
      </el-tabs>
    </div>

    <!-- 审批卡片列表 -->
    <div class="approval-list">
      <div v-for="item in approvalList" :key="item.id" class="approval-card">
        <div class="card-main">
          <div class="card-header">
            <span class="card-title">{{ item.title }}</span>
            <el-tag :type="getUrgencyType(item.urgencyLevel)" size="small" disable-transitions>
              {{ getUrgencyLabel(item.urgencyLevel) }}
            </el-tag>
          </div>
          <div class="card-meta">
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>申请人：{{ item.applicant }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Clock /></el-icon>
              <span>申请时间：{{ item.applyTime }}</span>
            </div>
            <div class="meta-item">
              <el-icon><UserFilled /></el-icon>
              <span>招聘人数：{{ item.headcount }} 人</span>
            </div>
            <div class="meta-item">
              <el-icon><OfficeBuilding /></el-icon>
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
            <el-button type="success" @click="handleApprove(item)">
              <el-icon><Check /></el-icon>
              通过
            </el-button>
            <el-button type="danger" @click="openRejectDialog(item)">
              <el-icon><Close /></el-icon>
              驳回
            </el-button>
          </template>
          <el-button @click="handleViewDetail(item)">
            <el-icon><View /></el-icon>
            详情
          </el-button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="approvalList.length === 0" class="empty-state">
        <el-icon :size="64" color="#94A3B8"><Checked /></el-icon>
        <h3>{{ emptyText }}</h3>
        <p>{{ emptyDesc }}</p>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <!-- 驳回弹窗 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="驳回审批"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules">
        <el-form-item label="需求标题">
          <el-input :model-value="rejectTarget?.title" disabled />
        </el-form-item>
        <el-form-item label="驳回原因" prop="comment">
          <el-input
            v-model="rejectForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因（必填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Clock, UserFilled, OfficeBuilding, Check, Close, View, Checked } from '@element-plus/icons-vue'
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

// 驳回弹窗
const rejectDialogVisible = ref(false)
const rejecting = ref(false)
const rejectTarget = ref<any>(null)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({ comment: '' })
const rejectRules: FormRules = {
  comment: [
    { required: true, message: '请输入驳回原因', trigger: 'blur' },
    { min: 2, max: 500, message: '驳回原因长度为 2 到 500 个字符', trigger: 'blur' },
  ],
}

// 空状态文案
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

function getUrgencyType(level: string) {
  const map: Record<string, string> = { NORMAL: 'info', URGENT: 'warning', CRITICAL: 'danger' }
  return map[level] || 'info'
}

function getUrgencyLabel(level: string) {
  const map: Record<string, string> = { NORMAL: '普通', URGENT: '紧急', CRITICAL: '特急' }
  return map[level] || level
}

// 加载数据
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

// 通过审批
async function handleApprove(item: any) {
  await approveDemand(item.id)
  ElMessage.success('审批已通过')
  loadData()
}

// 打开驳回弹窗
function openRejectDialog(item: any) {
  rejectTarget.value = item
  rejectForm.comment = ''
  rejectDialogVisible.value = true
}

// 驳回审批
async function handleReject() {
  try {
    await rejectFormRef.value?.validate()
  } catch {
    return
  }

  rejecting.value = true
  try {
    await rejectDemand(rejectTarget.value.id, rejectForm.comment)
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadData()
  } finally {
    rejecting.value = false
  }
}

// 查看详情
function handleViewDetail(item: any) {
  // 尝试通过 demandId 跳转到需求详情
  router.push(`/planning/demands/${item.demandId || item.id}`)
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.approval-tabs {
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.04);
  padding: 0 24px;
  margin-bottom: 16px;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
}

.approval-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.approval-card {
  background: $bg-card;
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
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-secondary;

  .el-icon {
    font-size: 14px;
  }
}

.card-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid $border-color-light;
}

.footer-item {
  font-size: 13px;
  color: $text-regular;
  margin-bottom: 4px;

  &:last-child {
    margin-bottom: 0;
  }
}

.footer-label {
  color: $text-secondary;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;

  h3 {
    margin-top: 16px;
    font-size: 16px;
    font-weight: 600;
    color: $text-regular;
  }

  p {
    margin-top: 8px;
    font-size: 14px;
    color: $text-secondary;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

// 响应式
@media (max-width: 768px) {
  .approval-card {
    flex-direction: column;
  }

  .card-actions {
    flex-direction: row;
    width: 100%;
  }

  .card-meta {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
