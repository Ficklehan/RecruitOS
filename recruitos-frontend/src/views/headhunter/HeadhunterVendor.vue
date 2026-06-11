<template>
  <PageShell variant="list" title="供应商管理" subtitle="管理猎头供应商信息与合同">
    <template #actions>
      <Button @click="handleCreate">
        <Plus class="mr-2 h-4 w-4" />
        添加供应商
      </Button>
    </template>

    <Table v-if="vendorList.length">
      <TableHeader>
        <TableRow>
          <TableHead class="min-w-[180px]">供应商名称</TableHead>
          <TableHead class="w-[100px]">联系人</TableHead>
          <TableHead class="w-[140px]">联系电话</TableHead>
          <TableHead class="w-[200px]">合同期限</TableHead>
          <TableHead class="w-[100px] text-center">佣金比例</TableHead>
          <TableHead class="w-[80px] text-center">状态</TableHead>
          <TableHead class="w-[90px] text-center">推荐数</TableHead>
          <TableHead class="w-[110px] text-center">成功入职数</TableHead>
          <TableHead class="w-[100px] text-center">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-for="row in vendorList" :key="row.id">
          <TableCell>
            <button type="button" class="font-medium text-primary hover:underline" @click="handleViewRecommendations(row)">
              {{ row.vendorName }}
            </button>
          </TableCell>
          <TableCell>{{ row.contactPerson }}</TableCell>
          <TableCell>{{ row.contactPhone }}</TableCell>
          <TableCell class="text-muted-foreground">{{ row.contractStart }} ~ {{ row.contractEnd }}</TableCell>
          <TableCell class="text-center">{{ row.commissionRate }}%</TableCell>
          <TableCell class="text-center">
            <Switch
              :model-value="row.status === 1"
              @update:model-value="(v) => handleStatusChange(row, v)"
            />
          </TableCell>
          <TableCell class="text-center">{{ row.recommendCount }}</TableCell>
          <TableCell class="text-center">{{ row.hireCount }}</TableCell>
          <TableCell class="text-center">
            <RowActions :actions="getRowActions(row)" @action="(cmd) => handleRowCommand(cmd, row)" />
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>

    <ListPagination
      v-if="total > 0"
      v-model:page-num="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @change="loadData"
    />

    <template #below>
      <Dialog v-model:open="dialogVisible">
        <DialogContent class="max-w-lg">
          <DialogHeader>
            <DialogTitle>{{ isEdit ? '编辑供应商' : '添加供应商' }}</DialogTitle>
          </DialogHeader>
          <div class="grid gap-4 py-2">
            <FormField label="供应商名称" required :error="formErrors.vendorName">
              <Input v-model="formData.vendorName" placeholder="请输入供应商名称" />
            </FormField>
            <FormField label="联系人" required :error="formErrors.contactPerson">
              <Input v-model="formData.contactPerson" placeholder="请输入联系人姓名" />
            </FormField>
            <FormField label="联系电话" required :error="formErrors.contactPhone">
              <Input v-model="formData.contactPhone" placeholder="请输入联系电话" />
            </FormField>
            <FormField label="联系邮箱" required :error="formErrors.contactEmail">
              <Input v-model="formData.contactEmail" placeholder="请输入联系邮箱" />
            </FormField>
            <FormField label="合同开始" required :error="formErrors.contractStart">
              <Input v-model="formData.contractStart" type="date" class="w-full" />
            </FormField>
            <FormField label="合同结束" required :error="formErrors.contractEnd">
              <Input v-model="formData.contractEnd" type="date" class="w-full" />
            </FormField>
            <FormField label="佣金比例" required :error="formErrors.commissionRate">
              <div class="flex items-center gap-2">
                <NumberInput v-model="formData.commissionRate" :min="0" :max="100" :step="0.5" class="flex-1" />
                <span class="text-xs text-muted-foreground shrink-0">单位：%</span>
              </div>
            </FormField>
          </div>
          <DialogFooter>
            <Button variant="outline" @click="dialogVisible = false">取消</Button>
            <Button :disabled="submitLoading" @click="handleSubmit">确定</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </template>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { toast } from '@/lib/notify'
import RowActions from '@/components/common/RowActions.vue'
import PageShell from '@/components/Layout/PageShell.vue'
import ListPagination from '@/components/common/ListPagination.vue'
import FormField from '@/components/app/FormField.vue'
import NumberInput from '@/components/app/NumberInput.vue'
import {
  Button,
  Input,
  Switch,
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
} from '@/components/ui'
import { getHeadhunterVendorList, createHeadhunterVendor, updateHeadhunterVendor } from '@/api/modules/headhunter'

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
})

const total = ref(0)
const vendorList = ref<any[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const editId = ref<number | null>(null)

const formData = reactive({
  vendorName: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  contractStart: '',
  contractEnd: '',
  commissionRate: 20 as number | null,
})

const formErrors = reactive({
  vendorName: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  contractStart: '',
  contractEnd: '',
  commissionRate: '',
})

function getRowActions(_row: any) {
  return [
    { command: 'view', label: '查看详情', icon: 'View', primary: true },
    { command: 'edit', label: '编辑', icon: 'Edit' },
  ]
}

function handleRowCommand(cmd: string, row: any) {
  if (cmd === 'view') handleViewRecommendations(row)
  else if (cmd === 'edit') handleEdit(row)
}

function validateForm(): boolean {
  formErrors.vendorName = formData.vendorName ? '' : '请输入供应商名称'
  formErrors.contactPerson = formData.contactPerson ? '' : '请输入联系人姓名'
  formErrors.contactPhone = formData.contactPhone ? '' : '请输入联系电话'
  const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.contactEmail)
  formErrors.contactEmail = !formData.contactEmail ? '请输入联系邮箱' : emailOk ? '' : '请输入正确的邮箱格式'
  formErrors.contractStart = formData.contractStart ? '' : '请选择合同开始日期'
  formErrors.contractEnd = formData.contractEnd ? '' : '请选择合同结束日期'
  formErrors.commissionRate = formData.commissionRate != null ? '' : '请输入佣金比例'
  return !Object.values(formErrors).some(Boolean)
}

async function loadData() {
  const params: any = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  const res = await getHeadhunterVendorList(params)
  vendorList.value = res.data.records || res.data.list || res.data || []
  total.value = res.data.total || vendorList.value.length
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  formData.vendorName = ''
  formData.contactPerson = ''
  formData.contactPhone = ''
  formData.contactEmail = ''
  formData.contractStart = ''
  formData.contractEnd = ''
  formData.commissionRate = 20
  Object.keys(formErrors).forEach((k) => ((formErrors as any)[k] = ''))
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  editId.value = row.id
  formData.vendorName = row.vendorName
  formData.contactPerson = row.contactPerson
  formData.contactPhone = row.contactPhone
  formData.contactEmail = row.contactEmail
  formData.contractStart = row.contractStart
  formData.contractEnd = row.contractEnd
  formData.commissionRate = row.commissionRate
  dialogVisible.value = true
}

function handleViewRecommendations(row: any) {
  toast.info(`查看 ${row.vendorName} 的推荐列表`)
}

async function handleStatusChange(row: any, enabled: boolean) {
  const newStatus = enabled ? 1 : 0
  const prevStatus = row.status
  row.status = newStatus
  try {
    await updateHeadhunterVendor(row.id, { status: newStatus })
    toast.success(`${row.vendorName} 已${newStatus === 1 ? '启用' : '停用'}`)
  } catch {
    row.status = prevStatus
  }
}

async function handleSubmit() {
  if (!validateForm()) return
  submitLoading.value = true
  try {
    const payload = {
      vendorName: formData.vendorName,
      contactPerson: formData.contactPerson,
      contactPhone: formData.contactPhone,
      contactEmail: formData.contactEmail,
      contractStart: formData.contractStart,
      contractEnd: formData.contractEnd,
      commissionRate: formData.commissionRate,
    }
    if (isEdit.value) {
      await updateHeadhunterVendor(editId.value!, payload)
      toast.success('供应商信息已更新')
    } else {
      await createHeadhunterVendor(payload)
      toast.success('供应商添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    /* interceptor */
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
