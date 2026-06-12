<template>
  <PageShell variant="list" title="话术管理" subtitle="管理多渠道沟通话术与 A/B 测试变体">
    <template #actions>
      <RButton @click="openDialog()">
        <Plus class="mr-2 h-4 w-4" />
        新建话术
      </RButton>
    </template>

    <template #filters>
      <RInput v-model="filters.keyword" placeholder="搜索模板名称" class="w-full sm:w-60" />
      <RSelect v-model="filters.type" :options="typeOptions" placeholder="类型" clearable class="w-full sm:w-32" />
      <RSelect v-model="filters.status" :options="statusOptions" placeholder="状态" clearable class="w-full sm:w-32" />
    </template>

    <RTable v-if="paginatedTemplates.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="min-w-[150px]">模板名称</RTableTh>
          <RTableTh class="w-[100px]">类型</RTableTh>
          <RTableTh class="min-w-[220px]">内容预览</RTableTh>
          <RTableTh class="w-[100px]">A/B测试</RTableTh>
          <RTableTh class="w-[100px] text-center">使用次数</RTableTh>
          <RTableTh class="w-[160px]">成功率</RTableTh>
          <RTableTh class="w-[80px] text-center">状态</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in paginatedTemplates" :key="row.id">
          <RTableCell class="font-medium">{{ row.name }}</RTableCell>
          <RTableCell>
            <RBadge :variant="elTagTypeToBadge(typeTagMap[row.type])">{{ typeLabelMap[row.type] }}</RBadge>
          </RTableCell>
          <RTableCell><span class="content-preview">{{ row.content }}</span></RTableCell>
          <RTableCell>
            <RBadge v-if="row.variant" variant="outline">{{ row.variant }}</RBadge>
            <span v-else class="text-muted-foreground">-</span>
          </RTableCell>
          <RTableCell class="text-center">{{ row.usageCount }}</RTableCell>
          <RTableCell>
            <div class="success-cell">
              <RProgress :value="row.successRate" class="h-3.5 flex-1" />
              <span class="text-xs text-muted-foreground">{{ row.successRate }}%</span>
            </div>
          </RTableCell>
          <RTableCell class="text-center">
            <RSwitch :model-value="row.enabled" @update:model-value="(v) => handleStatusChange(row, v)" />
          </RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <EmptyStateCta v-else title="暂无话术模板" description="创建话术模板用于多渠道沟通" />

    <ListPagination
      v-if="filteredTemplates.length > 0"
      v-model:page-num="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="filteredTemplates.length"
    />

    <template #below>
      <RDialog v-model:open="dialogVisible">
        <RDialogContent class="max-w-2xl">
          <RDialogHeader>
            <RDialogTitle>{{ isEditing ? '编辑话术' : '新建话术' }}</RDialogTitle>
          </RDialogHeader>
          <div class="grid gap-4 py-2">
            <FormField label="模板名称" required :error="formErrors.name">
              <RInput v-model="form.name" placeholder="请输入模板名称" />
            </FormField>
            <FormField label="模板类型" required :error="formErrors.type">
              <RSelect v-model="form.type" :options="formTypeOptions" placeholder="请选择类型" />
            </FormField>
            <FormField label="话术内容" required :error="formErrors.content">
              <RTextarea
                v-model="form.content"
                :rows="6"
                placeholder="请输入话术内容，可使用变量：{candidateName} {positionName} {companyName} {interviewTime}"
              />
              <div class="variable-hints">
                <span>可用变量：</span>
                <RBadge
                  v-for="v in variables"
                  :key="v"
                  variant="outline"
                  class="variable-tag cursor-pointer"
                  @click="insertVariable(v)"
                >{{ v }}</RBadge>
              </div>
            </FormField>
            <FormField label="状态">
              <div class="flex items-center gap-2">
                <RSwitch v-model="form.enabled" />
                <span class="text-sm text-muted-foreground">{{ form.enabled ? '启用' : '停用' }}</span>
              </div>
            </FormField>
          </div>
          <RDialogFooter>
            <RButton variant="outline" @click="dialogVisible = false">取消</RButton>
            <RButton @click="handleSubmit">确定</RButton>
          </RDialogFooter>
        </RDialogContent>
      </RDialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { elTagTypeToBadge } from '@/lib/badgeVariants'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import FormField from '@/components/app/FormField.vue'
import {
  RButton, RInput, RSelect, RBadge, RSwitch, RTextarea, RProgress,
  RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell,
  RDialog, RDialogContent, RDialogHeader, RDialogTitle, RDialogFooter,
} from '@/components/ui'
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

const typeTagMap: Record<string, string> = { SMS: '', EMAIL: 'success', WECHAT: 'success', FEISHU: 'primary' }
const typeLabelMap: Record<string, string> = { SMS: '短信', EMAIL: '邮件', WECHAT: '企微', FEISHU: '飞书' }

const typeOptions = [
  { label: '短信', value: 'SMS' },
  { label: '邮件', value: 'EMAIL' },
  { label: '企微', value: 'WECHAT' },
  { label: '飞书', value: 'FEISHU' },
]

const formTypeOptions = typeOptions

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' },
]

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

const filters = reactive({ keyword: '', type: '' as string | undefined, status: '' as string | undefined })

const filteredTemplates = computed(() =>
  templates.value.filter((t) => {
    if (filters.keyword && !t.name.includes(filters.keyword)) return false
    if (filters.type && t.type !== filters.type) return false
    if (filters.status === 'active' && !t.enabled) return false
    if (filters.status === 'inactive' && t.enabled) return false
    return true
  })
)

const pagination = reactive({ page: 1, pageSize: 10 })

const paginatedTemplates = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredTemplates.value.slice(start, start + pagination.pageSize)
})

const dialogVisible = ref(false)
const isEditing = ref(false)
let editingId: number | null = null

const formErrors = reactive({ name: '', type: '', content: '' })

const defaultForm = () => ({
  name: '',
  type: '' as '' | 'SMS' | 'EMAIL' | 'WECHAT' | 'FEISHU',
  content: '',
  enabled: true,
})

const form = reactive(defaultForm())

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
  formErrors.name = ''
  formErrors.type = ''
  formErrors.content = ''
  dialogVisible.value = true
}

function insertVariable(v: string) {
  form.content += v
}

function validateForm() {
  formErrors.name = form.name.trim() ? '' : '请输入模板名称'
  formErrors.type = form.type ? '' : '请选择模板类型'
  formErrors.content = form.content.trim() ? '' : '请输入话术内容'
  return !formErrors.name && !formErrors.type && !formErrors.content
}

async function handleSubmit() {
  if (!validateForm()) return
  const payload = {
    templateName: form.name,
    templateType: form.type,
    content: form.content,
    status: form.enabled ? 'ACTIVE' : 'INACTIVE',
  }
  try {
    if (isEditing.value && editingId !== null) {
      await updateTemplate(editingId, payload)
      toast.success('模板已更新')
    } else {
      await createTemplate(payload)
      toast.success('模板已创建')
    }
    dialogVisible.value = false
    loadTemplates()
  } catch {
    toast.error('保存失败')
  }
}

async function handleStatusChange(row: Template, enabled: boolean) {
  const prev = row.enabled
  row.enabled = enabled
  try {
    await updateTemplate(row.id, {
      templateName: row.name,
      templateType: row.type,
      content: row.content,
      status: enabled ? 'ACTIVE' : 'INACTIVE',
    })
    toast.success(`模板「${row.name}」已${enabled ? '启用' : '停用'}`)
  } catch {
    row.enabled = prev
    toast.error('状态更新失败')
  }
}

async function handleDelete(row: Template) {
  try {
    await deleteTemplate(row.id)
    toast.success('模板已删除')
    loadTemplates()
  } catch {
    toast.error('删除失败')
  }
}

function getRowActions(_row: Template) {
  return [
    { command: 'edit', label: '编辑', icon: 'Edit', type: 'default' as const, primary: true },
    { command: 'delete', label: '删除', icon: 'Delete', divided: true },
  ]
}

function handleRowCommand(cmd: string, row: Template) {
  if (cmd === 'edit') openDialog(row)
  else if (cmd === 'delete') handleDelete(row)
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

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

.success-cell {
  display: flex;
  align-items: center;
  gap: 8px;
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

.variable-tag:hover {
  color: $primary-color;
  border-color: $primary-color;
}
</style>
