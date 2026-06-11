<template>
  <PageShell title="组织架构">
    <template #actions>
      <Button @click="handleAdd">
        <Plus class="mr-2 h-4 w-4" />
        新增组织
      </Button>
    </template>

    <div class="org-container">
      <Card class="org-tree-card">
        <CardHeader class="pb-3">
          <div class="card-header">
            <CardTitle class="text-base">组织架构</CardTitle>
            <Input
              v-model="searchText"
              placeholder="搜索组织"
              class="w-40"
            />
          </div>
        </CardHeader>
        <CardContent>
          <div v-if="loading" class="py-8 text-center text-sm text-muted-foreground">加载中...</div>
          <div v-else class="org-tree">
            <OrgTreeNode
              v-for="node in filteredTree"
              :key="node.id"
              :node="node"
              :selected-id="selectedOrg?.id"
              :search-text="searchText"
              @select="handleNodeClick"
            />
          </div>
        </CardContent>
      </Card>

      <Card class="org-detail-card">
        <CardHeader class="pb-3">
          <div class="card-header">
            <CardTitle class="text-base">{{ isEditing ? '编辑组织' : '组织详情' }}</CardTitle>
            <div v-if="selectedOrg && !isEditing" class="card-actions">
              <Button size="sm" @click="handleEdit">编辑</Button>
              <Button size="sm" variant="destructive" @click="handleDelete">删除</Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div v-if="!selectedOrg" class="empty-hint">
            <Building2 class="h-12 w-12 text-muted-foreground" />
            <p>请从左侧选择一个组织节点</p>
          </div>

          <div v-else class="space-y-4 max-w-md">
            <FormField label="组织名称" required :error="formErrors.name">
              <Input v-model="formData.name" placeholder="请输入组织名称" :disabled="!isEditing" />
            </FormField>
            <FormField label="组织类型" required :error="formErrors.type">
              <Select
                v-model="formData.type"
                :options="typeOptions"
                placeholder="请选择类型"
                :disabled="!isEditing"
              />
            </FormField>
            <FormField label="上级组织">
              <Select
                v-model="formData.parentId"
                :options="parentOptions"
                placeholder="请选择上级组织"
                clearable
                :disabled="!isEditing"
              />
            </FormField>
            <FormField label="排序">
              <Input
                v-model.number="formData.sortOrder"
                type="number"
                min="0"
                max="999"
                :disabled="!isEditing"
              />
            </FormField>
            <FormField label="状态">
              <RadioGroup v-model="formData.status" class="flex gap-6" :disabled="!isEditing">
                <div class="flex items-center gap-2">
                  <RadioGroupItem value="active" id="org-status-active" :disabled="!isEditing" />
                  <Label for="org-status-active">启用</Label>
                </div>
                <div class="flex items-center gap-2">
                  <RadioGroupItem value="inactive" id="org-status-inactive" :disabled="!isEditing" />
                  <Label for="org-status-inactive">禁用</Label>
                </div>
              </RadioGroup>
            </FormField>
            <div v-if="isEditing" class="flex gap-2 pt-2">
              <Button @click="handleSave">保存</Button>
              <Button variant="outline" @click="isEditing = false">取消</Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { onMounted, ref, reactive, computed, defineComponent, h } from 'vue'
import { Plus, ChevronRight, ChevronDown, Building2 } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import FormField from '@/components/app/FormField.vue'
import {
  Button,
  Input,
  Select,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Label,
  RadioGroup,
  RadioGroupItem,
} from '@/components/ui'
import { getOrgTree, createOrg, updateOrg, deleteOrg } from '@/api/modules/org'

const OrgTreeNode = defineComponent({
  name: 'OrgTreeNode',
  props: {
    node: { type: Object, required: true },
    selectedId: { type: Number, default: null },
    searchText: { type: String, default: '' },
    depth: { type: Number, default: 0 },
  },
  emits: ['select'],
  setup(props, { emit }) {
    const expanded = ref(true)
    const hasChildren = () => (props.node.children?.length ?? 0) > 0
    const matchesSearch = (name: string) =>
      !props.searchText || name.toLowerCase().includes(props.searchText.toLowerCase())
    const nodeVisible = computed(() => {
      if (!props.searchText) return true
      function hasMatch(n: any): boolean {
        if (matchesSearch(n.name)) return true
        return (n.children || []).some(hasMatch)
      }
      return hasMatch(props.node)
    })
    const isSelected = () => props.selectedId === props.node.id
    return () => {
      if (!nodeVisible.value) return null
      return h('div', { class: 'org-tree-node' }, [
        h('div', {
          class: [
            'flex items-center gap-1 py-1.5 px-2 rounded cursor-pointer text-sm transition-colors',
            isSelected() ? 'bg-primary/10 text-primary font-medium' : 'hover:bg-muted',
          ],
          style: { paddingLeft: `${props.depth * 12 + 8}px` },
          onClick: () => emit('select', props.node),
        }, [
          hasChildren()
            ? h(Button, {
                variant: 'ghost',
                size: 'icon' as any,
                class: 'h-6 w-6 shrink-0',
                onClick: (e: Event) => { e.stopPropagation(); expanded.value = !expanded.value },
              }, () => expanded.value
                ? h(ChevronDown, { class: 'h-4 w-4' })
                : h(ChevronRight, { class: 'h-4 w-4' }))
            : h('span', { class: 'w-6 shrink-0' }),
          h('span', { class: 'truncate' }, props.node.name),
        ]),
        expanded.value && hasChildren()
          ? props.node.children.map((child: any) =>
              h(OrgTreeNode, {
                key: child.id,
                node: child,
                selectedId: props.selectedId,
                searchText: props.searchText,
                depth: props.depth + 1,
                onSelect: (n: any) => emit('select', n),
              })
            )
          : null,
      ])
    }
  },
})

const searchText = ref('')
const selectedOrg = ref<any>(null)
const isEditing = ref(false)
const loading = ref(false)
const isNew = ref(false)

const orgTree = ref<any[]>([])

const formData = reactive({
  name: '',
  type: 'DEPARTMENT',
  parentId: undefined as number | undefined,
  sortOrder: 0,
  status: 'active',
})

const formErrors = reactive({
  name: '',
  type: '',
})

const typeOptions = [
  { label: '公司', value: 'COMPANY' },
  { label: '部门', value: 'DEPARTMENT' },
  { label: '小组', value: 'TEAM' },
]

const filteredTree = computed(() => orgTree.value)

function flattenTree(nodes: any[], prefix = ''): { label: string; value: number }[] {
  const result: { label: string; value: number }[] = []
  for (const n of nodes) {
    if (selectedOrg.value?.id !== n.id) {
      result.push({ label: prefix + n.name, value: n.id })
    }
    if (n.children?.length) {
      result.push(...flattenTree(n.children, prefix + n.name + ' / '))
    }
  }
  return result
}

const parentOptions = computed(() => flattenTree(orgTree.value))

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
  formData.parentId = data.parentId ?? undefined
  formData.sortOrder = data.sortOrder ?? 0
  formData.status = toFormStatus(data.status ?? 1)
}

function handleNodeClick(data: any) {
  selectedOrg.value = data
  isEditing.value = false
  isNew.value = false
  fillForm(data)
  formErrors.name = ''
  formErrors.type = ''
}

function handleAdd() {
  selectedOrg.value = { id: null }
  isEditing.value = true
  isNew.value = true
  formData.name = ''
  formData.type = 'DEPARTMENT'
  formData.parentId = undefined
  formData.sortOrder = 0
  formData.status = 'active'
  formErrors.name = ''
  formErrors.type = ''
}

function handleEdit() {
  isEditing.value = true
}

async function handleDelete() {
  if (!selectedOrg.value?.id) return
  const ok = await confirm({
    title: '警告',
    message: '确定要删除该组织吗？删除后不可恢复。',
    destructive: true,
  })
  if (!ok) return
  await deleteOrg(selectedOrg.value.id)
  toast.success('删除成功')
  selectedOrg.value = null
  loadTree()
}

function validateForm(): boolean {
  formErrors.name = formData.name ? '' : '请输入组织名称'
  formErrors.type = formData.type ? '' : '请选择组织类型'
  return !Object.values(formErrors).some(Boolean)
}

async function handleSave() {
  if (!validateForm()) return
  const payload = {
    name: formData.name,
    type: formData.type,
    parentId: formData.parentId ?? null,
    sortOrder: formData.sortOrder,
    status: toApiStatus(formData.status),
  }
  if (isNew.value) {
    await createOrg(payload)
  } else if (selectedOrg.value?.id) {
    await updateOrg(selectedOrg.value.id, payload)
  }
  toast.success('保存成功')
  isEditing.value = false
  isNew.value = false
  await loadTree()
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
  gap: 12px;
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
</style>
