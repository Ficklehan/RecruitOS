<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">组织架构</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增组织
      </el-button>
    </div>

    <div class="org-container">
      <!-- 左侧组织树 -->
      <el-card class="org-tree-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">组织架构</span>
            <el-input
              v-model="searchText"
              placeholder="搜索组织"
              :prefix-icon="Search"
              size="small"
              clearable
            />
          </div>
        </template>
        <el-tree
          ref="treeRef"
          :data="orgTree"
          :props="{ children: 'children', label: 'name' }"
          node-key="id"
          default-expand-all
          highlight-current
          @node-click="handleNodeClick"
        />
      </el-card>

      <!-- 右侧详情 -->
      <el-card class="org-detail-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">{{ isEditing ? '编辑组织' : '组织详情' }}</span>
            <div class="card-actions" v-if="selectedOrg && !isEditing">
              <el-button type="primary" size="small" @click="handleEdit">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete">删除</el-button>
            </div>
          </div>
        </template>

        <div v-if="!selectedOrg" class="empty-hint">
          <el-icon :size="48" color="#94A3B8"><OfficeBuilding /></el-icon>
          <p>请从左侧选择一个组织节点</p>
        </div>

        <el-form
          v-else
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="100px"
          :disabled="!isEditing"
        >
          <el-form-item label="组织名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入组织名称" />
          </el-form-item>
          <el-form-item label="组织类型" prop="type">
            <el-select v-model="formData.type" placeholder="请选择类型" style="width: 100%">
              <el-option label="公司" value="COMPANY" />
              <el-option label="部门" value="DEPARTMENT" />
              <el-option label="小组" value="TEAM" />
            </el-select>
          </el-form-item>
          <el-form-item label="上级组织" prop="parentId">
            <el-tree-select
              v-model="formData.parentId"
              :data="orgTree"
              :props="{ children: 'children', label: 'name', value: 'id' }"
              placeholder="请选择上级组织"
              check-strictly
              clearable
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio label="active">启用</el-radio>
              <el-radio label="inactive">禁用</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="isEditing">
            <el-button type="primary" @click="handleSave">保存</el-button>
            <el-button @click="isEditing = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrgTree, createOrg, updateOrg, deleteOrg } from '@/api/modules/org'

const treeRef = ref()
const formRef = ref<FormInstance>()
const searchText = ref('')
const selectedOrg = ref<any>(null)
const isEditing = ref(false)
const loading = ref(false)
const isNew = ref(false)

const orgTree = ref<any[]>([])

const formData = reactive({
  name: '',
  type: 'DEPARTMENT',
  parentId: null as number | null,
  sortOrder: 0,
  status: 'active',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择组织类型', trigger: 'change' }],
}

function toFormStatus(status: number | string) {
  return status === 1 || status === 'active' ? 'active' : 'inactive'
}

function toApiStatus(status: string) {
  return status === 'active' ? 1 : 0
}

async function loadTree() {
  loading.value = true
  try {
    const res = await getOrgTree()
    orgTree.value = res.data || []
  } finally {
    loading.value = false
  }
}

function fillForm(data: any) {
  formData.name = data.name || ''
  formData.type = data.type || 'DEPARTMENT'
  formData.parentId = data.parentId ?? null
  formData.sortOrder = data.sortOrder ?? 0
  formData.status = toFormStatus(data.status ?? 1)
}

function handleNodeClick(data: any) {
  selectedOrg.value = data
  isEditing.value = false
  isNew.value = false
  fillForm(data)
}

function handleAdd() {
  selectedOrg.value = { id: null }
  isEditing.value = true
  isNew.value = true
  formData.name = ''
  formData.type = 'DEPARTMENT'
  formData.parentId = null
  formData.sortOrder = 0
  formData.status = 'active'
}

function handleEdit() {
  isEditing.value = true
}

async function handleDelete() {
  if (!selectedOrg.value?.id) return
  try {
    await ElMessageBox.confirm('确定要删除该组织吗？删除后不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteOrg(selectedOrg.value.id)
    ElMessage.success('删除成功')
    selectedOrg.value = null
    loadTree()
  } catch {
    // cancelled
  }
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = {
      name: formData.name,
      type: formData.type,
      parentId: formData.parentId,
      sortOrder: formData.sortOrder,
      status: toApiStatus(formData.status),
    }
    if (isNew.value) {
      await createOrg(payload)
    } else if (selectedOrg.value?.id) {
      await updateOrg(selectedOrg.value.id, payload)
    }
    ElMessage.success('保存成功')
    isEditing.value = false
    isNew.value = false
    await loadTree()
  })
}

onMounted(loadTree)
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.org-container {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  min-height: calc(100vh - 200px);

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: $text-secondary;

  p {
    margin-top: 16px;
    font-size: 14px;
  }
}

.el-tree {
  :deep(.el-tree-node__content) {
    height: 36px;
    border-radius: 4px;

    &:hover {
      background-color: $bg-muted;
    }
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background-color: $primary-lighter;
    color: $primary-color;
  }
}
</style>
