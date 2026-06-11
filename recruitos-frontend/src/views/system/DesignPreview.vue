<template>
  <PageShell variant="list"
    v-if="isDev"
    title="UI 体系预览"
    subtitle="shadcn-vue + RecruitOS 设计令牌 — 开发环境验收用"
  >
    <template #actions>
      <Button variant="outline" @click="resetFilters">重置</Button>
      <Button>主操作</Button>
    </template>

    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-2">
      <StatCard
        v-for="s in stats"
        :key="s.label"
        :label="s.label"
        :value="s.value"
        :icon="s.icon"
        :color="s.color"
        :trend="s.trend"
      />
    </div>

    <template #filters>
      <Input v-model="keyword" placeholder="搜索候选人、职位、需求" class="w-full sm:w-72" />
      <Select v-model="status" :options="statusOptions" placeholder="状态" clearable class="w-full sm:w-40" />
    </template>
    <template #filterActions>
      <Button>搜索</Button>
    </template>

    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>名称</TableHead>
          <TableHead class="w-[120px]">负责人</TableHead>
          <TableHead class="w-[100px] text-center">状态</TableHead>
          <TableHead class="w-[180px]">更新时间</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-for="row in filteredRows" :key="row.name">
          <TableCell>
            <span class="font-medium text-primary">{{ row.name }}</span>
          </TableCell>
          <TableCell>{{ row.owner }}</TableCell>
          <TableCell class="text-center">
            <Badge :variant="row.badge">{{ row.status }}</Badge>
          </TableCell>
          <TableCell class="text-muted-foreground">{{ row.updatedAt }}</TableCell>
        </TableRow>
      </TableBody>
    </Table>

    <div class="mt-8 grid gap-4 lg:grid-cols-2">
      <Card class="p-6 space-y-4">
        <h3 class="font-semibold">按钮变体</h3>
        <div class="flex flex-wrap gap-2">
          <Button>Default</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="outline">Outline</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="destructive">Destructive</Button>
        </div>
      </Card>
      <Card class="p-6 space-y-4">
        <h3 class="font-semibold">表单控件</h3>
        <div class="space-y-3">
          <Input v-model="demoInput" placeholder="Input" />
          <Textarea v-model="demoText" placeholder="Textarea" :rows="2" />
          <Select v-model="demoSelect" :options="statusOptions" placeholder="Select" />
        </div>
      </Card>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Briefcase, Clock, CheckCircle2, AlertTriangle } from 'lucide-vue-next'
import PageShell from '@/components/Layout/PageShell.vue'
import StatCard from '@/components/StatCard.vue'
import {
  Button,
  Input,
  Select,
  Badge,
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Card,
  Textarea,
} from '@/components/ui'
import type { BadgeVariant } from '@/lib/badgeVariants'

const isDev = import.meta.env.DEV

const keyword = ref('')
const status = ref<string | undefined>()
const demoInput = ref('')
const demoText = ref('')
const demoSelect = ref<string | undefined>()

const statusOptions = [
  { label: '进行中', value: 'ACTIVE' },
  { label: '待处理', value: 'PENDING' },
  { label: '已完成', value: 'DONE' },
]

const stats = [
  { label: '进行中', value: 128, icon: Briefcase, color: '#4F6BED', trend: 12 },
  { label: '待处理', value: 36, icon: Clock, color: '#D97706', trend: -4 },
  { label: '已完成', value: 512, icon: CheckCircle2, color: '#16A34A', trend: 8 },
  { label: '高风险', value: 7, icon: AlertTriangle, color: '#DC2626', trend: -2 },
]

const rows: Array<{
  name: string
  owner: string
  status: string
  badge: BadgeVariant
  updatedAt: string
}> = [
  { name: '高级前端工程师 - 招聘池', owner: '李想', status: '进行中', badge: 'default', updatedAt: '2026-06-09 10:12' },
  { name: 'Java 后端候选人 - 王磊', owner: '赵敏', status: '待处理', badge: 'outline', updatedAt: '2026-06-09 09:41' },
  { name: '产品负责人 Offer 审批', owner: '陈晨', status: '已完成', badge: 'secondary', updatedAt: '2026-06-08 18:26' },
]

const filteredRows = computed(() => {
  return rows.filter((r) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || r.name.includes(kw) || r.owner.includes(kw)
    const matchStatus =
      !status.value ||
      (status.value === 'ACTIVE' && r.status === '进行中') ||
      (status.value === 'PENDING' && r.status === '待处理') ||
      (status.value === 'DONE' && r.status === '已完成')
    return matchKw && matchStatus
  })
})

function resetFilters() {
  keyword.value = ''
  status.value = undefined
}
</script>
