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
      <el-table :data="userList" stripe>
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
import { ref, reactive } from 'vue'
import { Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const isEdit = ref(false)

const searchForm = reactive({
  keyword: '',
  status: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 50,
})

const userList = ref([
  { id: 1, username: 'admin', realName: '管理员', email: 'admin@recruitos.com', phone: '13800000001', roles: ['超级管理员'], status: 'active', lastLogin: '2024-03-15 10:30:00' },
  { id: 2, username: 'zhangsan', realName: '张三', email: 'zhangsan@recruitos.com', phone: '13800000002', roles: ['HR管理员'], status: 'active', lastLogin: '2024-03-15 09:00:00' },
  { id: 3, username: 'lisi', realName: '李四', email: 'lisi@recruitos.com', phone: '13800000003', roles: ['面试官'], status: 'active', lastLogin: '2024-03-14 16:00:00' },
  { id: 4, username: 'wangwu', realName: '王五', email: 'wangwu@recruitos.com', phone: '13800000004', roles: ['招聘专员'], status: 'inactive', lastLogin: '2024-03-10 14:00:00' },
])

const roleOptions = ref([
  { id: 1, name: '超级管理员' },
  { id: 2, name: 'HR管理员' },
  { id: 3, name: '面试官' },
  { id: 4, name: '招聘专员' },
  { id: 5, name: '部门经理' },
])

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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function handleSearch() {
  pagination.page = 1
  // TODO: 调用搜索接口
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogVisible.value = true
  formData.username = ''
  formData.realName = ''
  formData.email = ''
  formData.phone = ''
  formData.password = ''
  formData.roleIds = []
  formData.status = 'active'
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogVisible.value = true
  formData.username = row.username
  formData.realName = row.realName
  formData.email = row.email
  formData.phone = row.phone
  formData.roleIds = [row.id] // 简化处理
  formData.status = row.status
}

async function handleResetPwd(row: any) {
  try {
    await ElMessageBox.confirm(`确定要重置用户「${row.realName}」的密码吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('密码重置成功')
  } catch {
    // 取消操作
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.realName}」吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('删除成功')
  } catch {
    // 取消操作
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
      dialogVisible.value = false
    }
  })
}
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
