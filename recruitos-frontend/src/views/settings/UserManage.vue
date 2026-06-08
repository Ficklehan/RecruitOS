<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </div>

    <el-card>
      <!-- 搜索栏 -->
      <div class="filter-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索用户名/姓名/手机号"
          :prefix-icon="Search"
          clearable
          style="width: 280px"
        />
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" value="active" />
          <el-option label="禁用" value="inactive" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="userList" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="roles" label="角色" width="180">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" class="role-tag">
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLogin" label="最后登录" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </el-card>

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="600px"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="formData.realName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="formData.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, assignUserRoles, resetUserPassword } from '@/api/modules/user'
import { getRoleList } from '@/api/modules/role'

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const loading = ref(false)
const currentUserId = ref<number | null>(null)

const searchForm = reactive({
  keyword: '',
  status: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const userList = ref<any[]>([])
const roleOptions = ref<{ id: number; name: string }[]>([])

const formData = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  password: '',
  roleIds: [] as number[],
  status: 'active',
})

const rules: FormRules = {
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function mapStatus(status: number | string) {
  if (status === 1 || status === 'active') return 'active'
  return 'inactive'
}

function formatUser(row: any) {
  return {
    ...row,
    status: mapStatus(row.status),
    lastLogin: row.lastLoginAt || '-',
  }
}

async function loadRoles() {
  const res = await getRoleList()
  roleOptions.value = (res.data || []).map((r: any) => ({
    id: r.id,
    name: r.roleName,
  }))
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
    })
    let list = (res.data?.list || []).map(formatUser)
    if (searchForm.keyword) {
      const kw = searchForm.keyword.toLowerCase()
      list = list.filter((u: any) =>
        [u.username, u.realName, u.phone].some((v: string) => v?.toLowerCase().includes(kw))
      )
    }
    if (searchForm.status) {
      list = list.filter((u: any) => u.status === searchForm.status)
    }
    userList.value = list
    pagination.total = res.data?.total || list.length
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadUsers()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = ''
  handleSearch()
}

function handleAdd() {
  ElMessage.info('用户创建请通过注册或管理员后台开通，当前仅支持角色分配')
}

function handleEdit(row: any) {
  isEdit.value = true
  currentUserId.value = row.id
  dialogVisible.value = true
  formData.username = row.username
  formData.realName = row.realName
  formData.email = row.email
  formData.phone = row.phone
  formData.roleIds = row.roleIds || []
  formData.status = row.status
}

async function handleResetPwd(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码（至少6位）', `重置「${row.realName}」密码`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputValidator: (v) => (v && v.length >= 6) || '密码至少6位',
    })
    await resetUserPassword(row.id, value)
    ElMessage.success('密码重置成功')
  } catch {
    // cancelled
  }
}

async function handleDelete() {
  ElMessage.info('删除用户功能暂未开放')
}

async function handleSubmit() {
  if (!formRef.value || !currentUserId.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    await assignUserRoles(currentUserId.value!, formData.roleIds)
    ElMessage.success('角色已更新')
    dialogVisible.value = false
    loadUsers()
  })
}

watch(() => [pagination.page, pagination.pageSize], loadUsers)

onMounted(async () => {
  await loadRoles()
  await loadUsers()
})
</script>

<style lang="scss" scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.role-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}
</style>
