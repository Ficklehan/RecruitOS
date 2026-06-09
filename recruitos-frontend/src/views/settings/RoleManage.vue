<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <h2 class="page-title">角色管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增角色
      </el-button>
    </div>

    <el-card>
      <el-table :data="roleList" stripe v-loading="loading">
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
import { onMounted, ref, reactive, nextTick } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRoleList, createRole, updateRole, deleteRole,
  getRolePermissions, assignRolePermissions,
} from '@/api/modules/role'
import { getPermissionTree } from '@/api/modules/permission'

const formRef = ref<FormInstance>()
const permTreeRef = ref()
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const isEdit = ref(false)
const loading = ref(false)
const currentRole = ref<any>(null)
const currentRoleId = ref<number | null>(null)
const checkedKeys = ref<number[]>([])

const roleList = ref<any[]>([])
const permissionTree = ref<any[]>([])

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

function mapPermTree(nodes: any[]): any[] {
  return (nodes || []).map(n => ({
    id: n.id,
    name: n.permName,
    children: mapPermTree(n.children),
  }))
}

function mapRole(r: any) {
  return {
    ...r,
    name: r.roleName,
    code: r.roleCode,
    status: r.status === 1 ? 'active' : 'inactive',
    userCount: r.userCount ?? '-',
    createdAt: r.createdAt || '-',
  }
}

async function loadRoles() {
  loading.value = true
  try {
    const res = await getRoleList()
    roleList.value = (res.data || []).map(mapRole)
  } finally {
    loading.value = false
  }
}

async function loadPermTree() {
  const res = await getPermissionTree()
  permissionTree.value = mapPermTree(res.data || [])
}

function handleAdd() {
  isEdit.value = false
  currentRoleId.value = null
  dialogVisible.value = true
  formData.name = ''
  formData.code = ''
  formData.description = ''
  formData.status = 'active'
}

function handleEdit(row: any) {
  isEdit.value = true
  currentRoleId.value = row.id
  dialogVisible.value = true
  formData.name = row.name
  formData.code = row.code
  formData.description = row.description || ''
  formData.status = row.status
}

async function handlePermission(row: any) {
  currentRole.value = row
  const res = await getRolePermissions(row.id)
  checkedKeys.value = res.data || []
  permissionDialogVisible.value = true
  await nextTick()
  permTreeRef.value?.setCheckedKeys(checkedKeys.value)
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除角色「${row.name}」吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch {
    // cancelled
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = {
      roleName: formData.name,
      roleCode: formData.code,
      description: formData.description,
      status: formData.status === 'active' ? 1 : 0,
    }
    if (isEdit.value && currentRoleId.value) {
      await updateRole(currentRoleId.value, payload)
    } else {
      await createRole(payload)
    }
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadRoles()
  })
}

async function handleSavePermission() {
  if (!currentRole.value) return
  const checked = permTreeRef.value?.getCheckedKeys(false) || []
  const half = permTreeRef.value?.getHalfCheckedKeys() || []
  await assignRolePermissions(currentRole.value.id, [...checked, ...half])
  ElMessage.success('权限保存成功')
  permissionDialogVisible.value = false
}

onMounted(async () => {
  await loadPermTree()
  await loadRoles()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.permission-header {
  margin-bottom: 16px;
  font-size: 14px;
  color: $text-regular;
}
</style>
