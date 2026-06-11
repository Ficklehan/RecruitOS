<template>
  <PageShell variant="list" title="用户管理" subtitle="管理系统用户账号与角色分配">
    <template #actions>
      <RButton @click="handleAdd">
        <Plus class="mr-2 h-4 w-4" />
        新增用户
      </RButton>
    </template>

    <template #filters>
      <RInput
        v-model="searchForm.keyword"
        placeholder="搜索用户名/姓名/手机号"
        class="w-full sm:w-72"
        @keyup.enter="handleSearch"
      />
      <RSelect
        v-model="searchForm.status"
        :options="statusOptions"
        placeholder="状态"
        clearable
        class="w-full sm:w-32"
      />
    </template>

    <template #filterActions>
      <RButton @click="handleSearch">
        <Search class="mr-2 h-4 w-4" />
        搜索
      </RButton>
      <RButton variant="outline" @click="handleReset">重置</RButton>
    </template>

    <div v-if="loading" class="py-12 text-center text-[13px] text-text-secondary">加载中...</div>

    <RTable v-else-if="userList.length">
      <RTableHead>
        <RTableRow>
          <RTableTh class="w-[120px]">用户名</RTableTh>
          <RTableTh class="w-[100px]">姓名</RTableTh>
          <RTableTh class="min-w-[200px]">邮箱</RTableTh>
          <RTableTh class="w-[140px]">手机号</RTableTh>
          <RTableTh class="w-[180px]">角色</RTableTh>
          <RTableTh class="w-[80px] text-center">状态</RTableTh>
          <RTableTh class="w-[180px]">最后登录</RTableTh>
          <RTableTh class="w-[100px] text-center">操作</RTableTh>
        </RTableRow>
      </RTableHead>
      <RTableBody>
        <RTableRow v-for="row in userList" :key="row.id">
          <RTableCell>{{ row.username }}</RTableCell>
          <RTableCell>{{ row.realName }}</RTableCell>
          <RTableCell>{{ row.email }}</RTableCell>
          <RTableCell>{{ row.phone }}</RTableCell>
          <RTableCell>
            <RBadge v-for="role in row.roles" :key="role" variant="outline" class="mr-1 mb-1">
              {{ role }}
            </RBadge>
          </RTableCell>
          <RTableCell class="text-center">
            <RBadge :variant="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </RBadge>
          </RTableCell>
          <RTableCell class="text-text-secondary">{{ row.lastLogin }}</RTableCell>
          <RTableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd: string) => handleRowCommand(cmd, row)" />
          </RTableCell>
        </RTableRow>
      </RTableBody>
    </RTable>

    <ListPagination
      v-if="total > 0"
      v-model:page-num="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="total"
      @change="loadUsers"
    />

    <template #below>
      <RDialog :model-value="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" @update:model-value="dialogVisible = $event">
        <div class="grid gap-4 py-2 sm:grid-cols-2">
          <FormField label="用户名" required :error="formErrors.username">
            <RInput v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
          </FormField>
          <FormField label="姓名" required :error="formErrors.realName">
            <RInput v-model="formData.realName" placeholder="请输入姓名" />
          </FormField>
          <FormField label="邮箱" :error="formErrors.email">
            <RInput v-model="formData.email" placeholder="请输入邮箱" />
          </FormField>
          <FormField label="手机号" :error="formErrors.phone">
            <RInput v-model="formData.phone" placeholder="请输入手机号" />
          </FormField>
          <FormField v-if="!isEdit" label="密码" class="sm:col-span-2" :error="formErrors.password">
            <RInput v-model="formData.password" type="password" placeholder="请输入密码" />
          </FormField>
          <FormField label="角色" required class="sm:col-span-2" :error="formErrors.roleIds">
            <div class="flex flex-wrap gap-3 rounded-[var(--r-radius)] bg-bg-muted p-3">
              <label
                v-for="role in roleOptions"
                :key="role.id"
                class="flex items-center gap-2 text-[13px] cursor-pointer"
              >
                <input
                  type="checkbox"
                  :checked="formData.roleIds.includes(role.id)"
                  class="accent-primary"
                  @change="(e) => toggleRole(role.id, (e.target as HTMLInputElement).checked)"
                />
                {{ role.name }}
              </label>
            </div>
          </FormField>
          <FormField label="状态" class="sm:col-span-2">
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
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { Plus, Search } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import { prompt } from '@/lib/prompt'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import FormField from '@/components/app/FormField.vue'
import {
  RButton, RInput, RSelect, RBadge,
  RTable, RTableHead, RTableBody, RTableRow, RTableTh, RTableCell,
  RDialog,
} from '@/components/ui'
import { getUserList, assignUserRoles, resetUserPassword } from '@/api/modules/user'
import { getRoleList } from '@/api/modules/role'

const dialogVisible = ref(false)
const isEdit = ref(false)
const loading = ref(false)
const currentUserId = ref<number | null>(null)

const searchForm = reactive({
  keyword: '',
  status: '' as string | undefined,
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const total = ref(0)
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

const formErrors = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  password: '',
  roleIds: '',
})

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '禁用', value: 'inactive' },
]

function toggleRole(id: number, checked: boolean) {
  if (checked) {
    if (!formData.roleIds.includes(id)) formData.roleIds.push(id)
  } else {
    formData.roleIds = formData.roleIds.filter((r) => r !== id)
  }
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
    total.value = res.data?.total || list.length
    pagination.total = total.value
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
  searchForm.status = undefined
  handleSearch()
}

function handleAdd() {
  toast.info('用户创建请通过注册或管理员后台开通，当前仅支持角色分配')
}

function handleEdit(row: any) {
  isEdit.value = true
  currentUserId.value = row.id
  dialogVisible.value = true
  formData.username = row.username
  formData.realName = row.realName
  formData.email = row.email
  formData.phone = row.phone
  formData.roleIds = [...(row.roleIds || [])]
  formData.status = row.status
  Object.keys(formErrors).forEach((k) => ((formErrors as any)[k] = ''))
}

async function handleResetPwd(row: any) {
  const value = await prompt({
    title: `重置「${row.realName}」密码`,
    message: '请输入新密码（至少6位）',
    placeholder: '新密码',
    validator: (v) => (v && v.length >= 6) || '密码至少6位',
  })
  if (!value) return
  await resetUserPassword(row.id, value)
  toast.success('密码重置成功')
}

async function handleDelete(_row?: any) {
  toast.info('删除用户功能暂未开放')
}

function validateForm(): boolean {
  formErrors.roleIds = formData.roleIds.length ? '' : '请选择角色'
  return !formErrors.roleIds
}

async function handleSubmit() {
  if (!currentUserId.value || !validateForm()) return
  await assignUserRoles(currentUserId.value, formData.roleIds)
  toast.success('角色已更新')
  dialogVisible.value = false
  loadUsers()
}

onMounted(async () => {
  await loadRoles()
  await loadUsers()
})

function getRowActions(_row: any) {
  return [
    { command: 'edit', label: '编辑', icon: 'Edit', type: 'primary', primary: true },
    { command: 'resetPwd', label: '重置密码', icon: 'Key' },
    { command: 'delete', label: '删除', icon: 'Delete', divided: true },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'edit') handleEdit(row)
  else if (cmd === 'resetPwd') handleResetPwd(row)
  else if (cmd === 'delete') handleDelete(row)
}
</script>
