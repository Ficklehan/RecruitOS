<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">话术管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建话术</el-button>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索模板名称"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
      />
      <el-select v-model="filters.type" placeholder="类型" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="短信" value="SMS" />
        <el-option label="邮件" value="EMAIL" />
        <el-option label="企微" value="WECHAT" />
        <el-option label="飞书" value="FEISHU" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="启用" value="active" />
        <el-option label="停用" value="inactive" />
      </el-select>
    </div>

    <!-- Table -->
    <el-table :data="filteredTemplates" style="width: 100%">
      <el-table-column prop="name" label="模板名称" min-width="150" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTagMap[row.type]" size="small">{{ typeLabelMap[row.type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容预览" min-width="220">
        <template #default="{ row }">
          <span class="content-preview">{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="variant" label="A/B测试" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.variant" type="info" size="small" effect="plain">{{ row.variant }}</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="usageCount" label="使用次数" width="100" align="center" />
      <el-table-column prop="successRate" label="成功率" width="160">
        <template #default="{ row }">
          <el-progress
            :percentage="row.successRate"
            :color="row.successRate >= 80 ? '#059669' : row.successRate >= 50 ? '#D97706' : '#DC2626'"
            :stroke-width="14"
            :text-inside="true"
          />
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleStatusChange(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除该模板？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button type="danger" link :icon="Delete">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑话术' : '新建话术'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="短信" value="SMS" />
            <el-option label="邮件" value="EMAIL" />
            <el-option label="企微" value="WECHAT" />
            <el-option label="飞书" value="FEISHU" />
          </el-select>
        </el-form-item>
        <el-form-item label="话术内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入话术内容，可使用变量：{candidateName} {positionName} {companyName} {interviewTime}"
          />
          <div class="variable-hints">
            <span>可用变量：</span>
            <el-tag
              v-for="v in variables"
              :key="v"
              size="small"
              effect="plain"
              class="variable-tag"
              @click="insertVariable(v)"
            >{{ v }}</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio :value="true">启用</el-radio>
            <el-radio :value="false">停用</el-radio>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getTemplateList, createTemplate, updateTemplate, deleteTemplate } from '@/api/modules/communication'

interface Template {
  id: number
  name: string
  type: 'SMS' | 'EMAIL' | 'WECHAT' | 'FEISHU'
  content: string
  variant: string
  usageCount: number
  successRate: number
  enabled: boolean
}

const typeTagMap: Record<string, string> = {
  SMS: '',
  EMAIL: 'success',
  WECHAT: 'success',
  FEISHU: 'primary',
}

const typeLabelMap: Record<string, string> = {
  SMS: '短信',
  EMAIL: '邮件',
  WECHAT: '企微',
  FEISHU: '飞书',
}

const variables = ['{candidateName}', '{positionName}', '{companyName}', '{interviewTime}']

const templates = ref<Template[]>([])

function mapTemplate(row: any): Template {
  return {
    id: row.id,
    name: row.templateName || row.name,
    type: row.templateType || row.type,
    content: row.content || '',
    variant: row.abTestGroup || row.variant || '',
    usageCount: row.usageCount || 0,
    successRate: Number(row.successRate) || 0,
    enabled: row.status === 'ACTIVE' || row.enabled === true,
  }
}

async function loadTemplates() {
  try {
    const res: any = await getTemplateList({ pageNum: 1, pageSize: 200 })
    const list = res.data?.list || res.data?.records || res.data || []
    templates.value = (Array.isArray(list) ? list : []).map(mapTemplate)
  } catch {
    templates.value = []
  }
}

onMounted(loadTemplates)

// Filters
const filters = reactive({
  keyword: '',
  type: '',
  status: '',
})

const filteredTemplates = computed(() => {
  return templates.value.filter((t) => {
    if (filters.keyword && !t.name.includes(filters.keyword)) return false
    if (filters.type && t.type !== filters.type) return false
    if (filters.status === 'active' && !t.enabled) return false
    if (filters.status === 'inactive' && t.enabled) return false
    return true
  })
})

// Pagination
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: computed(() => filteredTemplates.value.length),
})

// Dialog
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
let editingId: number | null = null

const defaultForm = () => ({
  name: '',
  type: '' as '' | 'SMS' | 'EMAIL' | 'WECHAT' | 'FEISHU',
  content: '',
  enabled: true,
})

const form = reactive(defaultForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入话术内容', trigger: 'blur' }],
}

function openDialog(row?: Template) {
  if (row) {
    isEditing.value = true
    editingId = row.id
    Object.assign(form, { name: row.name, type: row.type, content: row.content, enabled: row.enabled })
  } else {
    isEditing.value = false
    editingId = null
    Object.assign(form, defaultForm())
  }
  dialogVisible.value = true
}

function insertVariable(v: string) {
  form.content += v
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload = {
    templateName: form.name,
    templateType: form.type,
    content: form.content,
    status: form.enabled ? 'ACTIVE' : 'INACTIVE',
  }
  try {
    if (isEditing.value && editingId !== null) {
      await updateTemplate(editingId, payload)
      ElMessage.success('模板已更新')
    } else {
      await createTemplate(payload)
      ElMessage.success('模板已创建')
    }
    dialogVisible.value = false
    loadTemplates()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function handleStatusChange(row: Template) {
  try {
    await updateTemplate(row.id, {
      templateName: row.name,
      templateType: row.type,
      content: row.content,
      status: row.enabled ? 'ACTIVE' : 'INACTIVE',
    })
    ElMessage.success(`模板「${row.name}」已${row.enabled ? '启用' : '停用'}`)
  } catch {
    row.enabled = !row.enabled
    ElMessage.error('状态更新失败')
  }
}

async function handleDelete(row: Template) {
  try {
    await deleteTemplate(row.id)
    ElMessage.success('模板已删除')
    loadTemplates()
  } catch {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';
.page-container {
  padding: 20px;
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.content-preview {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  color: $text-regular;
  line-height: 1.5;
}

.text-muted {
  color: $text-placeholder;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.variable-hints {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;

  span {
    font-size: 12px;
    color: $text-secondary;
  }
}

.variable-tag {
  cursor: pointer;

  &:hover {
    color: $primary-color;
    border-color: $primary-color;
  }
}
</style>
