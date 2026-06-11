<template>
  <PageShell>
    <!-- Stats Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <component :is="s.icon" class="h-5 w-5" />
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ s.value }}</span>
          <span class="stat-label">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <!-- Table Card -->
    <div class="table-card">
      <div class="table-header">
        <h3>租户列表</h3>
        <div class="table-actions">
          <div class="search-box">
            <Search class="h-4 w-4 text-muted-foreground shrink-0" />
            <input v-model="keyword" placeholder="搜索租户名称或编码" @keyup.enter="loadData" />
          </div>
          <Button @click="showCreate = true">
            <Plus class="mr-2 h-4 w-4" />
            创建租户
          </Button>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>租户</th>
              <th>套餐</th>
              <th>状态</th>
              <th>岗位使用</th>
              <th>Agent 使用</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in tenantList" :key="row.id">
              <td>
                <div class="tenant-cell">
                  <div class="tenant-avatar" :style="{ background: avatarColor(row.companyName) }">
                    {{ row.companyName[0] }}
                  </div>
                  <div class="tenant-info">
                    <span class="tenant-name">{{ row.companyName }}</span>
                    <span class="tenant-code">{{ row.tenantCode }}</span>
                  </div>
                </div>
              </td>
              <td><span class="plan-tag" :class="'plan-' + row.plan?.toLowerCase()">{{ row.plan }}</span></td>
              <td>
                <span class="status-dot" :class="row.status === 1 ? 'active' : 'inactive'"></span>
                {{ row.status === 1 ? '启用' : '禁用' }}
              </td>
              <td>
                <div class="usage-bar">
                  <div class="usage-track">
                    <div class="usage-fill" :style="{ width: usagePercent(row.usedJobs, row.maxJobs) + '%' }"></div>
                  </div>
                  <span class="usage-text">{{ row.usedJobs || 0 }}/{{ row.maxJobs || 0 }}</span>
                </div>
              </td>
              <td>
                <div class="usage-bar">
                  <div class="usage-track">
                    <div class="usage-fill agent" :style="{ width: usagePercent(row.usedAgents, row.maxAgents) + '%' }"></div>
                  </div>
                  <span class="usage-text">{{ row.usedAgents || 0 }}/{{ row.maxAgents || 0 }}</span>
                </div>
              </td>
              <td class="text-muted">{{ formatTime(row.createdAt) }}</td>
              <td>
                <div class="row-actions">
                  <button class="btn-ghost" @click="viewDetail(row)">详情</button>
                  <button class="btn-ghost" @click="showPlanDialog(row)">套餐</button>
                  <button class="btn-ghost" :class="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!tenantList.length && !loading" class="empty-state">
          <Building2 class="h-12 w-12 text-muted-foreground/40" />
          <p>暂无租户数据</p>
        </div>
      </div>

      <div class="table-footer">
        <span class="page-info">共 {{ total }} 条</span>
        <div class="page-btns">
          <button :disabled="pageNum <= 1" @click="pageNum--; loadData()">上一页</button>
          <span class="page-num">{{ pageNum }}</span>
          <button :disabled="pageNum * pageSize >= total" @click="pageNum++; loadData()">下一页</button>
        </div>
      </div>
    </div>

    <Dialog v-model:open="showCreate">
      <DialogContent class="max-w-lg max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>创建租户</DialogTitle>
        </DialogHeader>
        <div class="space-y-6 py-2">
          <div>
            <p class="section-title">租户信息</p>
            <div class="form-grid">
              <FormField label="租户编码" required>
                <Input v-model="createForm.tenantCode" placeholder="如：ACME" />
              </FormField>
              <FormField label="公司名称" required>
                <Input v-model="createForm.companyName" placeholder="如：上海某科技有限公司" />
              </FormField>
              <FormField label="信用代码">
                <Input v-model="createForm.creditCode" placeholder="统一社会信用代码（选填）" />
              </FormField>
              <FormField label="套餐" required>
                <Select v-model="createForm.plan" :options="planSelectOptions" />
              </FormField>
            </div>
          </div>
          <div>
            <p class="section-title">管理员账号</p>
            <div class="form-grid">
              <FormField label="用户名" required>
                <Input v-model="createForm.adminUsername" placeholder="admin" />
              </FormField>
              <FormField label="密码" required>
                <Input v-model="createForm.adminPassword" type="password" placeholder="登录密码" />
              </FormField>
              <FormField label="姓名">
                <Input v-model="createForm.adminRealName" placeholder="管理员姓名" />
              </FormField>
              <FormField label="邮箱">
                <Input v-model="createForm.adminEmail" placeholder="admin@example.com" />
              </FormField>
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showCreate = false">取消</Button>
          <Button :disabled="creating" @click="handleCreate">
            {{ creating ? '创建中...' : '确认创建' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="showPlan">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>变更套餐</DialogTitle>
        </DialogHeader>
        <p class="current-plan text-sm">
          当前套餐：
          <span class="plan-tag" :class="'plan-' + editingTenant?.plan?.toLowerCase()">{{ editingTenant?.plan }}</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, Building2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import FormField from '@/components/app/FormField.vue'
import {
  Button,
  Input,
  Select,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  RadioGroup,
  RadioGroupItem,
} from '@/components/ui'
import { getPlatformTenantList, createPlatformTenant, updateTenantStatus, updateTenantPlan } from '@/api/modules/platform'

const router = useRouter()
const loading = ref(false)
const tenantList = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const stats = computed(() => [
  { label: '总租户', value: total.value, icon: Building2, bg: '$primary-lighter', color: '$primary-dark' },
  { label: '已启用', value: tenantList.value.filter(t => t.status === 1).length, icon: Building2, bg: '$success-light', color: '$success-color' },
  { label: '已禁用', value: tenantList.value.filter(t => t.status !== 1).length, icon: Building2, bg: '$warning-light', color: '$warning-color' },
])

const planSelectOptions = [
  { label: 'STARTER - 入门版', value: 'STARTER' },
  { label: 'BASIC - 基础版', value: 'BASIC' },
  { label: 'PRO - 专业版', value: 'PRO' },
  { label: 'ENTERPRISE - 企业版', value: 'ENTERPRISE' },
]

const planOptions = [
  { value: 'STARTER', desc: '5 岗位 / 1 Agent' },
  { value: 'BASIC', desc: '20 岗位 / 5 Agent' },
  { value: 'PRO', desc: '100 岗位 / 20 Agent' },
  { value: 'ENTERPRISE', desc: '不限' },
]

const showCreate = ref(false)
const creating = ref(false)
const createForm = reactive({
  tenantCode: '', companyName: '', creditCode: '', plan: 'STARTER',
  adminUsername: 'admin', adminPassword: '123', adminRealName: '管理员', adminEmail: '', adminPhone: '',
})

const showPlan = ref(false)
const editingTenant = ref<any>(null)
const newPlan = ref('STARTER')
const changingPlan = ref(false)

function avatarColor(name: string) {
  const colors = ['$status-new','$status-interviewing','#ec4899','#f43f5e','#f97316','#eab308','#22c55e','#06b6d4','$primary-color']
  let hash = 0
  for (const c of name) hash = c.charCodeAt(0) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

function usagePercent(used: number, max: number) {
  if (!max) return 0
  return Math.min(Math.round((used || 0) / max * 100), 100)
}

function formatTime(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getPlatformTenantList({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    tenantList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {}
  loading.value = false
}

async function handleCreate() {
  if (!createForm.tenantCode || !createForm.companyName) {
    toast.error('请填写必填项')
    return
  }
  creating.value = true
  try {
    await createPlatformTenant(createForm)
    toast.success('租户创建成功')
    showCreate.value = false
    loadData()
  } catch {}
  creating.value = false
}

function viewDetail(row: any) { router.push(`/platform/tenants/${row.id}`) }

function showPlanDialog(row: any) {
  editingTenant.value = row
  newPlan.value = row.plan
  showPlan.value = true
}

async function handleChangePlan() {
  if (!editingTenant.value) return
  changingPlan.value = true
  try {
    await updateTenantPlan(editingTenant.value.id, newPlan.value)
    toast.success('套餐变更成功')
    showPlan.value = false
    loadData()
  } catch {}
  changingPlan.value = false
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  const ok = await confirm({
    title: '确认操作',
    message: `确定要${action}「${row.companyName}」吗？`,
    destructive: newStatus === 0,
  })
  if (!ok) return
  await updateTenantStatus(row.id, newStatus)
  toast.success(`已${action}`)
  loadData()
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.page {
  min-height: 100%;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: $bg-card;
  border: none;
  border-radius: $border-radius;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: $border-radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 2px;
}

.table-card {
  background: $bg-card;
  border: none;
  border-radius: $border-radius;
  overflow: hidden;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 20px;
  border-bottom: 1px solid $border-color-light;
}

.table-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: $bg-card;
  border: none;
  border-radius: $border-radius-sm;
  padding: 0 12px;
  height: 36px;
  transition: border-color $transition-fast;
}
.search-box:focus-within { border-color: $primary-color; }
.search-box .el-icon { color: $text-placeholder; font-size: 16px; }
.search-box input {
  border: none;
  background: none;
  outline: none;
  font-size: 13px;
  color: $text-regular;
  width: 200px;
}
.search-box input::placeholder { color: $text-placeholder; }

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  background: $primary-color;
  color: var(--r-bg-card);
  border: none;
  border-radius: $border-radius-sm;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color $transition-fast;
}
.btn-primary:hover { background: $primary-dark; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-cancel {
  height: 36px;
  padding: 0 16px;
  background: $bg-card;
  color: $text-regular;
  border: none;
  border-radius: $border-radius-sm;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color $transition-fast, border-color $transition-fast;
}
.btn-cancel:hover { background: $bg-muted; }

.btn-ghost {
  padding: 4px 10px;
  background: none;
  border: none;
  color: $primary-color;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border-radius: $border-radius-sm;
  transition: background-color $transition-fast;
}
.btn-ghost:hover { background: $primary-lighter; }
.btn-ghost.danger { color: $danger-color; }
.btn-ghost.danger:hover { background: $danger-lighter; }
.btn-ghost.success { color: $success-color; }
.btn-ghost.success:hover { background: $success-lighter; }

/* Table */
.table-wrap { overflow-x: auto; }

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  padding: 11px 20px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: $text-secondary;
  letter-spacing: 0.02em;
  background: $bg-muted;
  border-bottom: 1px solid var(--r-divider);
}

td {
  padding: 14px 20px;
  font-size: 13px;
  color: $text-regular;
  border-bottom: 1px solid var(--r-divider);
}

tr:hover td { background: $primary-lighter; }

.tenant-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tenant-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--r-bg-card);
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.tenant-info {
  display: flex;
  flex-direction: column;
}

.tenant-name {
  font-weight: 600;
  color: $text-primary;
  font-size: 13px;
}

.tenant-code {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 1px;
}

/* Plan Tags */
.plan-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.plan-starter { background: $bg-muted; color: $text-secondary; }
.plan-basic { background: #eff6ff; color: $primary-color; }
.plan-pro { background: $success-lighter; color: var(--r-success); }
.plan-enterprise { background: $warning-lighter; color: #ca8a04; }

/* Status */
.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-dot.active { background: $status-offer; box-shadow: 0 0 6px rgba(16,185,129,0.4); }
.status-dot.inactive { background: $text-secondary; }

/* Usage Bar */
.usage-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.usage-track {
  flex: 1;
  height: 6px;
  background: $bg-muted;
  border-radius: 3px;
  overflow: hidden;
  min-width: 60px;
}

.usage-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, $status-new, $status-interviewing);
  transition: width 0.4s ease;
}
.usage-fill.agent { background: linear-gradient(90deg, $status-offer, #34d399); }

.usage-text {
  font-size: 12px;
  color: $text-secondary;
  white-space: nowrap;
  min-width: 40px;
}

.text-muted { color: $text-secondary; font-size: 12px; }

.row-actions {
  display: flex;
  gap: 4px;
}

/* Empty */
.empty-state {
  padding: 60px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.empty-state p { color: $text-secondary; font-size: 14px; }

/* Footer */
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid var(--r-divider);
}

.page-info { font-size: 13px; color: $text-secondary; }

.page-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-btns button {
  padding: 6px 14px;
  background: $bg-warm;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: $neutral-600;
  cursor: pointer;
  transition: all 0.2s;
}
.page-btns button:hover:not(:disabled) { background: $bg-muted; border-color: $border-color; }
.page-btns button:disabled { opacity: 0.4; cursor: not-allowed; }
.page-btns .page-num {
  font-size: 13px;
  font-weight: 600;
  color: $status-new;
  min-width: 24px;
  text-align: center;
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
  width: 560px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
  animation: slideUp 0.25s ease;
}
.modal-sm { width: 440px; }

@keyframes slideUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--r-divider);
}
.modal-header h3 { font-size: 16px; font-weight: 600; color: $text-primary; }
.modal-close { color: $text-secondary; cursor: pointer; padding: 4px; border-radius: 6px; transition: all 0.2s; }
.modal-close:hover { color: $neutral-600; background: $bg-muted; }

.modal-body { padding: 24px; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--r-divider);
}

/* Form */
.form-section { margin-bottom: 24px; }
.form-section:last-child { margin-bottom: 0; }
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--r-divider);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-item label {
  font-size: 13px;
  font-weight: 500;
  color: $neutral-600;
}
.req { color: $danger-color; }

.form-item input, .form-item select {
  height: 38px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  color: $text-regular;
  background: $bg-card;
  transition: border-color 0.2s;
  outline: none;
}
.form-item input:focus, .form-item select:focus { border-color: $status-new; box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }
.form-item input::placeholder { color: $text-secondary; }

/* Plan Options */
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
