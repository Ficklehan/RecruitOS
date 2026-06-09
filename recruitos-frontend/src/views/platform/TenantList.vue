<template>
  <div class="page page-container">
    <!-- Stats Cards -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <el-icon :size="20"><component :is="s.icon" /></el-icon>
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
            <el-icon><Search /></el-icon>
            <input v-model="keyword" placeholder="搜索租户名称或编码" @keyup.enter="loadData" />
          </div>
          <button class="btn-primary" @click="showCreate = true">
            <el-icon><Plus /></el-icon>
            <span>创建租户</span>
          </button>
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
          <el-icon :size="48" color="#cbd5e1"><OfficeBuilding /></el-icon>
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

    <!-- Create Dialog -->
    <Teleport to="body">
      <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
        <div class="modal">
          <div class="modal-header">
            <h3>创建租户</h3>
            <el-icon class="modal-close" @click="showCreate = false"><Close /></el-icon>
          </div>
          <div class="modal-body">
            <div class="form-section">
              <div class="section-title">租户信息</div>
              <div class="form-grid">
                <div class="form-item">
                  <label>租户编码 <span class="req">*</span></label>
                  <input v-model="createForm.tenantCode" placeholder="如：ACME" />
                </div>
                <div class="form-item">
                  <label>公司名称 <span class="req">*</span></label>
                  <input v-model="createForm.companyName" placeholder="如：上海某科技有限公司" />
                </div>
                <div class="form-item">
                  <label>信用代码</label>
                  <input v-model="createForm.creditCode" placeholder="统一社会信用代码（选填）" />
                </div>
                <div class="form-item">
                  <label>套餐 <span class="req">*</span></label>
                  <select v-model="createForm.plan">
                    <option value="STARTER">STARTER - 入门版</option>
                    <option value="BASIC">BASIC - 基础版</option>
                    <option value="PRO">PRO - 专业版</option>
                    <option value="ENTERPRISE">ENTERPRISE - 企业版</option>
                  </select>
                </div>
              </div>
            </div>
            <div class="form-section">
              <div class="section-title">管理员账号</div>
              <div class="form-grid">
                <div class="form-item">
                  <label>用户名 <span class="req">*</span></label>
                  <input v-model="createForm.adminUsername" placeholder="admin" />
                </div>
                <div class="form-item">
                  <label>密码 <span class="req">*</span></label>
                  <input v-model="createForm.adminPassword" type="password" placeholder="登录密码" />
                </div>
                <div class="form-item">
                  <label>姓名</label>
                  <input v-model="createForm.adminRealName" placeholder="管理员姓名" />
                </div>
                <div class="form-item">
                  <label>邮箱</label>
                  <input v-model="createForm.adminEmail" placeholder="admin@example.com" />
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="showCreate = false">取消</button>
            <button class="btn-primary" :disabled="creating" @click="handleCreate">
              {{ creating ? '创建中...' : '确认创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Plan Dialog -->
    <Teleport to="body">
      <div v-if="showPlan" class="modal-overlay" @click.self="showPlan = false">
        <div class="modal modal-sm">
          <div class="modal-header">
            <h3>变更套餐</h3>
            <el-icon class="modal-close" @click="showPlan = false"><Close /></el-icon>
          </div>
          <div class="modal-body">
            <p class="current-plan">当前套餐：<span class="plan-tag" :class="'plan-' + editingTenant?.plan?.toLowerCase()">{{ editingTenant?.plan }}</span></p>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, OfficeBuilding, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlatformTenantList, createPlatformTenant, updateTenantStatus, updateTenantPlan } from '@/api/modules/platform'

const router = useRouter()
const loading = ref(false)
const tenantList = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const stats = computed(() => [
  { label: '总租户', value: total.value, icon: OfficeBuilding, bg: '#EFF6FF', color: '#2563EB' },
  { label: '已启用', value: tenantList.value.filter(t => t.status === 1).length, icon: OfficeBuilding, bg: '#D1FAE5', color: '#059669' },
  { label: '已禁用', value: tenantList.value.filter(t => t.status !== 1).length, icon: OfficeBuilding, bg: '#FEF3C7', color: '#D97706' },
])

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
  const colors = ['#6366f1','#8b5cf6','#ec4899','#f43f5e','#f97316','#eab308','#22c55e','#06b6d4','#3b82f6']
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
    ElMessage.warning('请填写必填项')
    return
  }
  creating.value = true
  try {
    await createPlatformTenant(createForm)
    ElMessage.success('租户创建成功')
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
    ElMessage.success('套餐变更成功')
    showPlan.value = false
    loadData()
  } catch {}
  changingPlan.value = false
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}「${row.companyName}」吗？`, '确认操作', { type: 'warning' })
    await updateTenantStatus(row.id, newStatus)
    ElMessage.success(`已${action}`)
    loadData()
  } catch {}
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
  border: 1px solid $border-color;
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
  border: 1px solid $border-color;
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
  font-size: 15px;
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
  border: 1px solid $border-color;
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
  color: #fff;
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
  border: 1px solid $border-color;
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
  border-bottom: 1px solid $border-color-light;
}

td {
  padding: 14px 20px;
  font-size: 13px;
  color: $text-regular;
  border-bottom: 1px solid $border-color-light;
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
  color: #fff;
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
  color: #0f172a;
  font-size: 13px;
}

.tenant-code {
  font-size: 12px;
  color: #94a3b8;
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
.plan-starter { background: #f1f5f9; color: #64748b; }
.plan-basic { background: #eff6ff; color: #3b82f6; }
.plan-pro { background: #f0fdf4; color: #16a34a; }
.plan-enterprise { background: #fefce8; color: #ca8a04; }

/* Status */
.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-dot.active { background: #10b981; box-shadow: 0 0 6px rgba(16,185,129,0.4); }
.status-dot.inactive { background: #94a3b8; }

/* Usage Bar */
.usage-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.usage-track {
  flex: 1;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
  min-width: 60px;
}

.usage-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  transition: width 0.4s ease;
}
.usage-fill.agent { background: linear-gradient(90deg, #10b981, #34d399); }

.usage-text {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  min-width: 40px;
}

.text-muted { color: #94a3b8; font-size: 12px; }

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
.empty-state p { color: #94a3b8; font-size: 14px; }

/* Footer */
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
}

.page-info { font-size: 13px; color: #94a3b8; }

.page-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-btns button {
  padding: 6px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}
.page-btns button:hover:not(:disabled) { background: #f1f5f9; border-color: #cbd5e1; }
.page-btns button:disabled { opacity: 0.4; cursor: not-allowed; }
.page-btns .page-num {
  font-size: 13px;
  font-weight: 600;
  color: #6366f1;
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
  border-bottom: 1px solid #f1f5f9;
}
.modal-header h3 { font-size: 16px; font-weight: 600; color: #0f172a; }
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

/* Form */
.form-section { margin-bottom: 24px; }
.form-section:last-child { margin-bottom: 0; }
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
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
  color: #475569;
}
.req { color: #ef4444; }

.form-item input, .form-item select {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #334155;
  background: $bg-card;
  transition: border-color 0.2s;
  outline: none;
}
.form-item input:focus, .form-item select:focus { border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }
.form-item input::placeholder { color: #94a3b8; }

/* Plan Options */
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
