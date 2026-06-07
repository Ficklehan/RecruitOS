<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">角色管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增角色
      </el-button>
    </div>

    <el-card>
      <el-table :data="roleList" stripe>
        <el-table-column prop="name" label="角色名称" width="180" />
        <el-table-column prop="code" label="角色编码" width="180" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="userCount" label="用户数" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handlePermission(row)">
              权限
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="500px"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
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

    <!-- 权限分配弹窗 -->
    <el-dialog
      v-model="permissionDialogVisible"
      title="权限分配"
      width="600px"
    >
      <div class="permission-header">
        <span>当前角色：{{ currentRole?.name }}</span>
      </div>
      <el-tree
        ref="permTreeRef"
        :data="permissionTree"
        :props="{ children: 'children', label: 'name' }"
        show-checkbox
        node-key="id"
        default-expand-all
        :default-checked-keys="checkedKeys"
      />
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermission">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'

const formRef = ref<FormInstance>()
const permTreeRef = ref()
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const isEdit = ref(false)
const currentRole = ref<any>(null)
const checkedKeys = ref<number[]>([])

const roleList = ref([
  { id: 1, name: '超级管理员', code: 'SUPER_ADMIN', description: '系统超级管理员，拥有所有权限', userCount: 2, status: 'active', createdAt: '2024-01-01 00:00:00' },
  { id: 2, name: 'HR管理员', code: 'HR_ADMIN', description: 'HR部门管理员', userCount: 5, status: 'active', createdAt: '2024-01-15 10:00:00' },
  { id: 3, name: '面试官', code: 'INTERVIEWER', description: '面试官角色', userCount: 20, status: 'active', createdAt: '2024-02-01 14:00:00' },
  { id: 4, name: '招聘专员', code: 'RECRUITER', description: '招聘专员角色', userCount: 8, status: 'active', createdAt: '2024-02-15 09:00:00' },
  { id: 5, name: '部门经理', code: 'DEPT_MANAGER', description: '部门经理角色', userCount: 10, status: 'inactive', createdAt: '2024-03-01 16:00:00' },
])

const permissionTree = ref([
  {
    id: 1,
    name: '招聘需求',
    children: [
      { id: 11, name: '查看需求' },
      { id: 12, name: '创建需求' },
      { id: 13, name: '编辑需求' },
      { id: 14, name: '删除需求' },
      { id: 15, name: '审批需求' },
    ],
  },
  {
    id: 2,
    name: '候选人管理',
    children: [
      { id: 21, name: '查看候选人' },
      { id: 22, name: '创建候选人' },
      { id: 23, name: '编辑候选人' },
      { id: 24, name: '删除候选人' },
    ],
  },
  {
    id: 3,
    name: '面试管理',
    children: [
      { id: 31, name: '查看面试' },
      { id: 32, name: '安排面试' },
      { id: 33, name: '评价面试' },
    ],
  },
  {
    id: 4,
    name: '系统设置',
    children: [
      { id: 41, name: '租户设置' },
      { id: 42, name: '组织管理' },
      { id: 43, name: '角色管理' },
      { id: 44, name: '用户管理' },
    ],
  },
])

const formData = reactive({
  name: '',
  code: '',
  description: '',
  status: 'active',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false
  dialogVisible.value = true
  formData.name = ''
  formData.code = ''
  formData.description = ''
  formData.status = 'active'
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogVisible.value = true
  formData.name = row.name
  formData.code = row.code
  formData.description = row.description
  formData.status = row.status
}

function handlePermission(row: any) {
  currentRole.value = row
  checkedKeys.value = [11, 12, 13, 21, 22, 31] // 模拟已选权限
  permissionDialogVisible.value = true
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除角色「${row.name}」吗？`, '警告', {
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

function handleSavePermission() {
  const checked = permTreeRef.value?.getCheckedKeys(false) || []
  console.log('保存权限:', checked)
  ElMessage.success('权限保存成功')
  permissionDialogVisible.value = false
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.permission-header {
  margin-bottom: 16px;
  font-size: 14px;
  color: $text-regular;
}
</style>
