<template>
  <div class="page" v-loading="loading">
    <!-- Header -->
    <div class="page-top">
      <div class="page-nav">
        <button class="btn-back" @click="router.push('/platform/tenants')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <div class="page-title">
          <div class="title-avatar" :style="{ background: avatarColor(tenant.companyName || '') }">
            {{ (tenant.companyName || '?')[0] }}
          </div>
          <div>
            <h2>{{ tenant.companyName || '-' }}</h2>
            <span class="title-sub">{{ tenant.tenantCode }}</span>
          </div>
        </div>
      </div>
      <div class="page-actions">
        <button class="btn-outline" @click="showPlanDialog">变更套餐</button>
        <button :class="tenant.status === 1 ? 'btn-danger' : 'btn-success'" @click="toggleStatus">
          {{ tenant.status === 1 ? '禁用租户' : '启用租户' }}
        </button>
      </div>
    </div>

    <!-- Info Cards -->
    <div class="info-grid">
      <div class="info-card">
        <div class="info-label">套餐</div>
        <span class="plan-tag" :class="'plan-' + (tenant.plan || '').toLowerCase()">{{ tenant.plan || '-' }}</span>
      </div>
      <div class="info-card">
        <div class="info-label">状态</div>
        <div class="status-line">
          <span class="status-dot" :class="tenant.status === 1 ? 'active' : 'inactive'"></span>
          {{ tenant.status === 1 ? '启用' : '禁用' }}
        </div>
      </div>
      <div class="info-card">
        <div class="info-label">信用代码</div>
        <span class="info-value">{{ tenant.creditCode || '-' }}</span>
      </div>
      <div class="info-card">
        <div class="info-label">创建时间</div>
        <span class="info-value">{{ formatTime(tenant.createdAt) }}</span>
      </div>
      <div class="info-card">
        <div class="info-label">试用截止</div>
        <span class="info-value">{{ formatTime(tenant.trialEndTime) }}</span>
      </div>
      <div class="info-card">
        <div class="info-label">License 到期</div>
        <span class="info-value">{{ tenant.licenseEndDate || '-' }}</span>
      </div>
    </div>

    <!-- Quota Section -->
    <div class="quota-section">
      <h3>配额使用</h3>
      <div class="quota-grid">
        <div class="quota-card" v-for="q in quotas" :key="q.label">
          <div class="quota-top">
            <span class="quota-label">{{ q.label }}</span>
            <span class="quota-num">{{ q.used }}<span class="quota-max">/{{ q.max }}</span></span>
          </div>
          <div class="quota-bar">
            <div class="quota-fill" :style="{ width: q.percent + '%', background: q.color }"></div>
          </div>
          <div class="quota-bottom">
            <span class="quota-percent">{{ q.percent }}%</span>
            <span class="quota-rest">剩余 {{ q.max - q.used }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- License Detail -->
    <div class="license-card">
      <h3>License 信息</h3>
      <div class="license-grid">
        <div class="license-item">
          <span class="dl-label">开始日期</span>
          <span class="dl-value">{{ tenant.licenseStartDate || '-' }}</span>
        </div>
        <div class="license-item">
          <span class="dl-label">到期日期</span>
          <span class="dl-value">{{ tenant.licenseEndDate || '-' }}</span>
        </div>
        <div class="license-item">
          <span class="dl-label">宽限天数</span>
          <span class="dl-value">{{ tenant.graceDays || 0 }} 天</span>
        </div>
        <div class="license-item">
          <span class="dl-label">License 状态</span>
          <span class="dl-value">
            <span class="status-dot" :class="tenant.licenseStatus === 1 ? 'active' : 'inactive'"></span>
            {{ tenant.licenseStatus === 1 ? '有效' : '过期' }}
          </span>
        </div>
      </div>
    </div>

    <!-- Plan Dialog -->
    <Teleport to="body">
      <div v-if="showPlan" class="modal-overlay" @click.self="showPlan = false">
        <div class="modal">
          <div class="modal-header">
            <h3>变更套餐</h3>
            <el-icon class="modal-close" @click="showPlan = false"><Close /></el-icon>
          </div>
          <div class="modal-body">
            <p class="current-plan">当前套餐：<span class="plan-tag" :class="'plan-' + (tenant.plan || '').toLowerCase()">{{ tenant.plan }}</span></p>
            <div class="plan-options">
              <label v-for="p in planOptions" :key="p.value" class="plan-option" :class="{ active: newPlan === p.value }">
                <input type="radio" v-model="newPlan" :value="p.value" />
                <div class="plan-content">
                  <span class="plan-name">{{ p.value }}</span>
                  <span class="plan-desc">{{ p.desc }}</span>
                </div>
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="showPlan = false">取消</button>
            <button class="btn-primary" :disabled="changingPlan" @click="handleChangePlan">
              {{ changingPlan ? '变更中...' : '确认变更' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlatformTenantDetail, updateTenantStatus, updateTenantPlan } from '@/api/modules/platform'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const tenant = ref<any>({})

const showPlan = ref(false)
const newPlan = ref('STARTER')
const changingPlan = ref(false)

const planOptions = [
  { value: 'STARTER', desc: '5 岗位 / 1 Agent / 100 简历' },
  { value: 'BASIC', desc: '20 岗位 / 5 Agent / 500 简历' },
  { value: 'PRO', desc: '100 岗位 / 20 Agent / 2000 简历' },
  { value: 'ENTERPRISE', desc: '不限' },
]

const quotas = computed(() => {
  const t = tenant.value
  return [
    { label: '岗位数量', used: t.usedJobs || 0, max: t.maxJobs || 0, percent: pct(t.usedJobs, t.maxJobs), color: 'linear-gradient(90deg,#6366f1,#8b5cf6)' },
    { label: 'Agent 数量', used: t.usedAgents || 0, max: t.maxAgents || 0, percent: pct(t.usedAgents, t.maxAgents), color: 'linear-gradient(90deg,#8b5cf6,#a78bfa)' },
    { label: '简历解析', used: t.resumeUsed || 0, max: t.resumeQuota || 0, percent: pct(t.resumeUsed, t.resumeQuota), color: 'linear-gradient(90deg,#10b981,#34d399)' },
    { label: '消息额度', used: t.messageUsed || 0, max: t.messageQuota || 0, percent: pct(t.messageUsed, t.messageQuota), color: 'linear-gradient(90deg,#f59e0b,#fbbf24)' },
  ]
})

function pct(used: number, max: number) {
  if (!max) return 0
  return Math.min(Math.round((used || 0) / max * 100), 100)
}

function avatarColor(name: string) {
  const colors = ['#6366f1','#8b5cf6','#ec4899','#f43f5e','#f97316','#eab308','#22c55e','#06b6d4','#3b82f6']
  let hash = 0
  for (const c of name) hash = c.charCodeAt(0) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

function formatTime(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

async function loadDetail() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res: any = await getPlatformTenantDetail(id)
    tenant.value = res.data || {}
  } catch {}
  loading.value = false
}

function showPlanDialog() {
  newPlan.value = tenant.value.plan
  showPlan.value = true
}

async function handleChangePlan() {
  changingPlan.value = true
  try {
    await updateTenantPlan(tenant.value.id, newPlan.value)
    ElMessage.success('套餐变更成功')
    showPlan.value = false
    loadDetail()
  } catch {}
  changingPlan.value = false
}

async function toggleStatus() {
  const newStatus = tenant.value.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}该租户吗？`, '确认操作', { type: 'warning' })
    await updateTenantStatus(tenant.value.id, newStatus)
    ElMessage.success(`已${action}`)
    loadDetail()
  } catch {}
}

onMounted(() => loadDetail())
</script>

<style scoped>
.page {
  padding: 28px 32px;
  min-height: 100%;
}

/* Page Top */
.page-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  background: $bg-card;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-back:hover { background: #f8fafc; border-color: #cbd5e1; }

.page-title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.title-avatar {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.page-title h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.title-sub {
  font-size: 13px;
  color: #94a3b8;
}

.page-actions {
  display: flex;
  gap: 10px;
}

.btn-outline {
  height: 38px;
  padding: 0 18px;
  background: $bg-card;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-outline:hover { background: #f8fafc; border-color: #cbd5e1; }

.btn-danger {
  height: 38px;
  padding: 0 18px;
  background: $bg-card;
  border: 1px solid #fecaca;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-danger:hover { background: #fef2f2; }

.btn-success {
  height: 38px;
  padding: 0 18px;
  background: $bg-card;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #10b981;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-success:hover { background: #f0fdf4; }

.btn-primary {
  height: 38px;
  padding: 0 20px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.btn-primary:hover { opacity: 0.9; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-cancel {
  height: 38px;
  padding: 0 18px;
  background: #f1f5f9;
  color: #475569;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.btn-cancel:hover { background: #e2e8f0; }

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.info-card {
  background: $bg-card;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.info-label {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.status-line {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-dot.active { background: #10b981; box-shadow: 0 0 6px rgba(16,185,129,0.4); }
.status-dot.inactive { background: #94a3b8; }

/* Plan Tags */
.plan-tag {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.plan-starter { background: #f1f5f9; color: #64748b; }
.plan-basic { background: #eff6ff; color: #3b82f6; }
.plan-pro { background: #f0fdf4; color: #16a34a; }
.plan-enterprise { background: #fefce8; color: #ca8a04; }

/* Quota Section */
.quota-section {
  margin-bottom: 24px;
}

.quota-section h3, .license-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 16px;
}

.quota-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.quota-card {
  background: $bg-card;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.quota-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.quota-label {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}

.quota-num {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.quota-max {
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
}

.quota-bar {
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 10px;
}

.quota-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.quota-bottom {
  display: flex;
  justify-content: space-between;
}

.quota-percent {
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
}

.quota-rest {
  font-size: 12px;
  color: #94a3b8;
}

/* License Card */
.license-card {
  background: $bg-card;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.license-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.license-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dl-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.dl-value {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  display: flex;
  align-items: center;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.modal {
  background: $bg-card;
  border-radius: 16px;
  width: 440px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
  animation: slideUp 0.25s ease;
}

@keyframes slideUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}
.modal-header h3 { font-size: 16px; font-weight: 600; color: #0f172a; margin: 0; }
.modal-close { color: #94a3b8; cursor: pointer; padding: 4px; border-radius: 6px; transition: all 0.2s; }
.modal-close:hover { color: #475569; background: #f1f5f9; }

.modal-body { padding: 24px; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
}

.current-plan {
  font-size: 14px;
  color: #475569;
  margin-bottom: 16px;
}

.plan-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.plan-option:hover { border-color: #c7d2fe; background: #fafbff; }
.plan-option.active { border-color: #6366f1; background: #eef2ff; }
.plan-option input { display: none; }

.plan-content { display: flex; flex-direction: column; }
.plan-name { font-size: 14px; font-weight: 600; color: #0f172a; }
.plan-desc { font-size: 12px; color: #94a3b8; margin-top: 2px; }
</style>
