<template>
  <PageShell>
<!-- Header -->
    <div class="page-top">
      <div class="page-nav">
        <Button variant="outline" size="sm" @click="router.push('/platform/tenants')">
          <ArrowLeft class="mr-1 h-4 w-4" />
          返回
        </Button>
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
        <Button variant="outline" @click="showPlanDialog">变更套餐</Button>
        <Button :variant="tenant.status === 1 ? 'destructive' : 'default'" @click="toggleStatus">
          {{ tenant.status === 1 ? '禁用租户' : '启用租户' }}
        </Button>
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

    <div v-if="loading" class="py-8 text-center text-sm text-muted-foreground">加载中...</div>

    <Dialog v-model="showPlan">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>变更套餐</DialogTitle>
        </DialogHeader>
        <p class="current-plan text-sm">
          当前套餐：
          <span class="plan-tag" :class="'plan-' + (tenant.plan || '').toLowerCase()">{{ tenant.plan }}</span>
        </p>
        <RadioGroup v-model="newPlan" class="plan-options gap-2">
          <label
            v-for="p in planOptions"
            :key="p.value"
            class="plan-option flex items-center gap-3"
            :class="{ active: newPlan === p.value }"
          >
            <RadioGroupItem :value="p.value" />
            <div class="plan-content">
              <span class="plan-name">{{ p.value }}</span>
              <span class="plan-desc">{{ p.desc }}</span>
            </div>
          </label>
        </RadioGroup>
        <DialogFooter>
          <Button variant="outline" @click="showPlan = false">取消</Button>
          <Button :disabled="changingPlan" @click="handleChangePlan">
            {{ changingPlan ? '变更中...' : '确认变更' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import {
  Button,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  RadioGroup,
  RadioGroupItem,
} from '@/components/ui'
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
    { label: '岗位数量', used: t.usedJobs || 0, max: t.maxJobs || 0, percent: pct(t.usedJobs, t.maxJobs), color: 'linear-gradient(90deg,$status-new,$status-interviewing)' },
    { label: 'Agent 数量', used: t.usedAgents || 0, max: t.maxAgents || 0, percent: pct(t.usedAgents, t.maxAgents), color: 'linear-gradient(90deg,$status-interviewing,#a78bfa)' },
    { label: '简历解析', used: t.resumeUsed || 0, max: t.resumeQuota || 0, percent: pct(t.resumeUsed, t.resumeQuota), color: 'linear-gradient(90deg,$status-offer,#34d399)' },
    { label: '消息额度', used: t.messageUsed || 0, max: t.messageQuota || 0, percent: pct(t.messageUsed, t.messageQuota), color: 'linear-gradient(90deg,#f59e0b,#fbbf24)' },
  ]
})

function pct(used: number, max: number) {
  if (!max) return 0
  return Math.min(Math.round((used || 0) / max * 100), 100)
}

function avatarColor(name: string) {
  const colors = ['$status-new','$status-interviewing','#ec4899','#f43f5e','#f97316','#eab308','#22c55e','#06b6d4','$primary-color']
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
    toast.success('套餐变更成功')
    showPlan.value = false
    loadDetail()
  } catch {}
  changingPlan.value = false
}

async function toggleStatus() {
  const newStatus = tenant.value.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  const ok = await confirm({
    title: '确认操作',
    message: `确定要${action}该租户吗？`,
    destructive: newStatus === 0,
  })
  if (!ok) return
  await updateTenantStatus(tenant.value.id, newStatus)
  toast.success(`已${action}`)
  loadDetail()
}

onMounted(() => loadDetail())
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

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
  border: none;
  border-radius: 8px;
  font-size: 13px;
  color: $neutral-600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-back:hover { background: $bg-warm; border-color: $border-color; }

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
  color: var(--r-bg-card);
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.page-title h2 {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.title-sub {
  font-size: 13px;
  color: $text-secondary;
}

.page-actions {
  display: flex;
  gap: 10px;
}

.btn-outline {
  height: 38px;
  padding: 0 18px;
  background: $bg-card;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: $neutral-600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-outline:hover { background: $bg-warm; border-color: $border-color; }

.btn-danger {
  height: 38px;
  padding: 0 18px;
  background: $bg-card;
  border: 1px solid #fecaca;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: $danger-color;
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
  color: $status-offer;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-success:hover { background: $success-lighter; }

.btn-primary {
  height: 38px;
  padding: 0 20px;
  background: linear-gradient(135deg, $status-new, $status-interviewing);
  color: var(--r-bg-card);
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
  background: $bg-muted;
  color: $neutral-600;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.btn-cancel:hover { background: $border-color; }

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
  color: $text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: $text-regular;
}

.status-line {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  color: $text-regular;
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-dot.active { background: $status-offer; box-shadow: 0 0 6px rgba(16,185,129,0.4); }
.status-dot.inactive { background: $text-secondary; }

/* Plan Tags */
.plan-tag {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.plan-starter { background: $bg-muted; color: $text-secondary; }
.plan-basic { background: #eff6ff; color: $primary-color; }
.plan-pro { background: $success-lighter; color: var(--r-success); }
.plan-enterprise { background: $warning-lighter; color: #ca8a04; }

/* Quota Section */
.quota-section {
  margin-bottom: 24px;
}

.quota-section h3, .license-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
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
  color: $text-secondary;
}

.quota-num {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
}

.quota-max {
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary;
}

.quota-bar {
  height: 8px;
  background: $bg-muted;
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
  color: $status-new;
}

.quota-rest {
  font-size: 12px;
  color: $text-secondary;
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
  color: $text-secondary;
  font-weight: 500;
}

.dl-value {
  font-size: 14px;
  font-weight: 500;
  color: $text-regular;
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
  border-bottom: 1px solid $bg-muted;
}
.modal-header h3 { font-size: 16px; font-weight: 600; color: $text-primary; margin: 0; }
.modal-close { color: $text-secondary; cursor: pointer; padding: 4px; border-radius: 6px; transition: all 0.2s; }
.modal-close:hover { color: $neutral-600; background: $bg-muted; }

.modal-body { padding: 24px; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid $bg-muted;
}

.current-plan {
  font-size: 14px;
  color: $neutral-600;
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
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.plan-option:hover { border-color: #c7d2fe; background: #fafbff; }
.plan-option.active { border-color: $status-new; background: #eef2ff; }
.plan-option input { display: none; }

.plan-content { display: flex; flex-direction: column; }
.plan-name { font-size: 14px; font-weight: 600; color: $text-primary; }
.plan-desc { font-size: 12px; color: $text-secondary; margin-top: 2px; }
</style>
