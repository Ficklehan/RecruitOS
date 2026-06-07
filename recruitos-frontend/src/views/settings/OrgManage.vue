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
          <el-form-item label="组织编码" prop="code">
            <el-input v-model="formData.code" placeholder="请输入组织编码" />
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
          <el-form-item label="负责人" prop="leader">
            <el-input v-model="formData.leader" placeholder="请输入负责人" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="formData.sort" :min="0" :max="999" />
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
import { ref, reactive } from 'vue'
import { Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'

const treeRef = ref()
const formRef = ref<FormInstance>()
const searchText = ref('')
const selectedOrg = ref<any>(null)
const isEditing = ref(false)

const orgTree = ref([
  {
    id: 1,
    name: 'RecruitOS 科技有限公司',
    code: 'HQ',
    children: [
      {
        id: 2,
        name: '技术部',
        code: 'TECH',
        children: [
          { id: 5, name: '前端组', code: 'FE' },
          { id: 6, name: '后端组', code: 'BE' },
          { id: 7, name: '测试组', code: 'QA' },
        ],
      },
      {
        id: 3,
        name: '产品部',
        code: 'PM',
        children: [
          { id: 8, name: '产品设计组', code: 'PD' },
          { id: 9, name: '用户体验组', code: 'UX' },
        ],
      },
      { id: 4, name: '人力资源部', code: 'HR' },
    ],
  },
])

const formData = reactive({
  name: '',
  code: '',
  parentId: null as number | null,
  leader: '',
  phone: '',
  sort: 0,
  status: 'active',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
}

function handleNodeClick(data: any) {
  selectedOrg.value = data
  isEditing.value = false
  formData.name = data.name
  formData.code = data.code
  formData.parentId = data.parentId || null
  formData.leader = data.leader || ''
  formData.phone = data.phone || ''
  formData.sort = data.sort || 0
  formData.status = data.status || 'active'
}

function handleAdd() {
  selectedOrg.value = {}
  isEditing.value = true
  formData.name = ''
  formData.code = ''
  formData.parentId = null
  formData.leader = ''
  formData.phone = ''
  formData.sort = 0
  formData.status = 'active'
}

function handleEdit() {
  isEditing.value = true
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除该组织吗？删除后不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('删除成功')
    selectedOrg.value = null
  } catch {
    // 取消操作
  }
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success('保存成功')
      isEditing.value = false
    }
  })
}
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
