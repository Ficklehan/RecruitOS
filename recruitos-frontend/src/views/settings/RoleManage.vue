<template>
  <PageShell variant="list" title="角色管理" subtitle="配置系统角色与权限">
    <template #actions>
      <RButton @click="handleAdd">
        <Plus class="mr-2 h-4 w-4" />
        新增角色
      </RButton>
    </template>

    <div v-if="loading" class="py-12 text-center text-[13px] text-text-secondary">加载中...</div>

    <RTable v-else-if="roleList.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[180px]">角色名称</RTableTh>
          <RTableTh class="w-[180px]">角色编码</RTableTh>
          <RTableTh class="min-w-[200px]">描述</RTableTh>
          <RTableTh class="w-[100px] text-center">用户数</RTableTh>
          <RTableTh class="w-[100px] text-center">状态</RTableTh>
          <RTableTh class="w-[180px]">创建时间</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in roleList" :key="row.id">
          <RTableCell class="font-medium">{{ row.name }}</RTableCell>
          <RTableCell class="font-mono text-[12px]">{{ row.code }}</RTableCell>
          <RTableCell>{{ row.description || '—' }}</RTableCell>
          <RTableCell class="text-center">{{ row.userCount }}</RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </RBadge>
          </RTableCell>
          <RTableCell class="text-text-secondary">{{ row.createdAt }}</RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd: string) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <template #below>
      <RDialog :model-value="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" @update:model-value="dialogVisible = $event">
        <div class="grid gap-4 py-2">
          <FormField label="角色名称" required :error="formErrors.name">
            <RInput v-model="formData.name" placeholder="请输入角色名称" />
          </FormField>
          <FormField label="角色编码" required :error="formErrors.code">
            <RInput v-model="formData.code" placeholder="请输入角色编码" :disabled="isEdit" />
          </FormField>
          <FormField label="描述">
            <RTextarea v-model="formData.description" placeholder="请输入角色描述" :rows="3" />
          </FormField>
          <FormField label="状态">
            <div class="flex gap-6">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="formData.status" value="active" class="accent-primary" />
                <span class="text-[13px] text-text-primary">启用</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="formData.status" value="inactive" class="accent-primary" />
                <span class="text-[13px] text-text-primary">禁用</span>
              </label>
            </div>
          </FormField>
        </div>
        <template #footer>
          <RButton variant="outline" @click="dialogVisible = false">取消</RButton>
          <RButton @click="handleSubmit">确定</RButton>
        </template>
      </RDialog>

      <RDialog :model-value="permissionDialogVisible" title="权限分配" width="560px" @update:model-value="permissionDialogVisible = $event">
        <p class="text-[13px] text-text-secondary mb-3">当前角色：{{ currentRole?.name }}</p>
        <div class="max-h-[50vh] overflow-auto pr-2">
          <div class="space-y-1">
            <PermTreeNode
              v-for="node in permissionTree"
              :key="node.id"
              :node="node"
              :checked-ids="checkedKeys"
              @toggle="togglePerm"
            />
          </div>
        </div>
        <template #footer>
          <RButton variant="outline" @click="permissionDialogVisible = false">取消</RButton>
          <RButton @click="handleSavePermission">保存</RButton>
        </template>
      </RDialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive, defineComponent, h } from 'vue'
import { Plus, ChevronRight, ChevronDown } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import FormField from '@/components/app/FormField.vue'
import {
  Button,
  Input,
  Badge,
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Label,
  Textarea,
  Checkbox,
  RadioGroup,
  RadioGroupItem,
  ScrollArea,
} from '@/components/ui'
import {
  getRoleList, createRole, updateRole, deleteRole,
  getRolePermissions, assignRolePermissions,
} from '@/api/modules/role'
import { getPermissionTree } from '@/api/modules/permission'

const PermTreeNode = defineComponent({
  name: 'PermTreeNode',
  props: {
    node: { type: Object, required: true },
    checkedIds: { type: Array as () => number[], required: true },
    depth: { type: Number, default: 0 },
  },
  emits: ['toggle'],
  setup(props, { emit }) {
    const expanded = ref(true)
    const hasChildren = () => (props.node.children?.length ?? 0) > 0
    const isChecked = () => props.checkedIds.includes(props.node.id)
    return () => h('div', { class: 'perm-node' }, [
      h('div', {
        class: 'flex items-center gap-2 py-1.5 rounded hover:bg-muted/50',
        style: { paddingLeft: `${props.depth * 16}px` },
      }, [
        hasChildren()
          ? h(Button, {
              variant: 'ghost',
              size: 'icon' as any,
              class: 'h-6 w-6 shrink-0',
              onClick: () => { expanded.value = !expanded.value },
            }, () => expanded.value
              ? h(ChevronDown, { class: 'h-4 w-4' })
              : h(ChevronRight, { class: 'h-4 w-4' }))
          : h('span', { class: 'w-6 shrink-0' }),
        h(Checkbox, {
          modelValue: isChecked(),
          'onUpdate:modelValue': () => emit('toggle', props.node),
        }),
        h('span', { class: 'text-sm' }, props.node.name),
      ]),
      expanded.value && hasChildren()
        ? props.node.children.map((child: any) =>
            h(PermTreeNode, {
              key: child.id,
              node: child,
              checkedIds: props.checkedIds,
              depth: props.depth + 1,
              onToggle: (n: any) => emit('toggle', n),
            })
          )
        : null,
    ])
  },
})

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

const formErrors = reactive({
  name: '',
  code: '',
})

function mapPermTree(nodes: any[]): any[] {
  return (nodes || []).map((n) => ({
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

function collectDescendantIds(node: any): number[] {
  const ids = [node.id]
  for (const child of node.children || []) {
    ids.push(...collectDescendantIds(child))
  }
  return ids
}

function togglePerm(node: any) {
  const set = new Set(checkedKeys.value)
  const ids = collectDescendantIds(node)
  if (set.has(node.id)) {
    ids.forEach((id) => set.delete(id))
  } else {
    ids.forEach((id) => set.add(id))
  }
  checkedKeys.value = [...set]
}

function collectKeysForSave(nodes: any[], checked: Set<number>): number[] {
  const result = new Set<number>()
  function walk(node: any): boolean {
    let childChecked = false
    for (const child of node.children || []) {
      if (walk(child)) childChecked = true
    }
    const selfChecked = checked.has(node.id)
    if (selfChecked || childChecked) {
      result.add(node.id)
      return true
    }
    return false
  }
  nodes.forEach(walk)
  return [...result]
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
  formErrors.name = ''
  formErrors.code = ''
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
}

async function handleDelete(row: any) {
  const ok = await confirm({
    title: '警告',
    message: `确定要删除角色「${row.name}」吗？`,
    destructive: true,
  })
  if (!ok) return
  await deleteRole(row.id)
  toast.success('删除成功')
  loadRoles()
}

function validateForm(): boolean {
  formErrors.name = formData.name ? '' : '请输入角色名称'
  formErrors.code = formData.code ? '' : '请输入角色编码'
  return !Object.values(formErrors).some(Boolean)
}

async function handleSubmit() {
  if (!validateForm()) return
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
  toast.success(isEdit.value ? '编辑成功' : '新增成功')
  dialogVisible.value = false
  loadRoles()
}

async function handleSavePermission() {
  if (!currentRole.value) return
  const checked = new Set(checkedKeys.value)
  const keys = collectKeysForSave(permissionTree.value, checked)
  await assignRolePermissions(currentRole.value.id, keys)
  toast.success('权限保存成功')
  permissionDialogVisible.value = false
}

onMounted(async () => {
  await loadPermTree()
  await loadRoles()
})

function getRowActions(_row: any) {
  return [
    { command: 'permission', label: '权限配置', icon: 'Lock', type: 'primary', primary: true },
    { command: 'edit', label: '编辑', icon: 'Edit' },
    { command: 'delete', label: '删除', icon: 'Delete', divided: true },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'permission') handlePermission(row)
  else if (cmd === 'edit') handleEdit(row)
  else if (cmd === 'delete') handleDelete(row)
}
</script>
