<template>
  <ListPageLayout
    v-if="isDev"
    title="UI 体系预览"
    subtitle="用于快速验证 token、骨架、筛选、统计、表格节奏是否收敛为统一系统"
  >
    <template #actions>
      <el-button @click="resetFilters">重置</el-button>
      <el-button type="primary">主操作</el-button>
    </template>

    <div class="stat-row">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">{{ s.icon }}</div>
        <div>
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.value }}</div>
        </div>
      </div>
    </div>

    <template #filters>
      <el-input v-model="keyword" placeholder="搜索候选人、职位、需求" clearable class="filter-field filter-field--lg" />
      <el-select v-model="status" placeholder="状态" clearable class="filter-field filter-field--sm">
        <el-option label="全部" value="" />
        <el-option label="进行中" value="ACTIVE" />
        <el-option label="待处理" value="PENDING" />
      </el-select>
    </template>
    <template #filterActions>
      <el-button type="primary">搜索</el-button>
    </template>

    <el-table :data="rows" style="width: 100%">
      <el-table-column prop="name" label="名称" min-width="160">
        <template #default="{ row }">
          <span class="title-link">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="owner" label="负责人" width="120" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.statusType">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
    </el-table>
  </ListPageLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ListPageLayout from '@/components/Layout/ListPageLayout.vue'

const isDev = import.meta.env.DEV

const keyword = ref('')
const status = ref('')

const stats = [
  { label: '进行中', value: 128, icon: '◎', bg: '#EFF6FF', color: '#3B82F6' },
  { label: '待处理', value: 36, icon: '!', bg: '#FEF3C7', color: '#D97706' },
  { label: '已完成', value: 512, icon: '✓', bg: '#D1FAE5', color: '#059669' },
  { label: '高风险', value: 7, icon: '△', bg: '#FEE2E2', color: '#DC2626' },
]

const rows = [
  { name: '高级前端工程师 - 招聘池', owner: '李想', status: '进行中', statusType: 'success', updatedAt: '2026-06-09 10:12' },
  { name: 'Java 后端候选人 - 王磊', owner: '赵敏', status: '待处理', statusType: 'warning', updatedAt: '2026-06-09 09:41' },
  { name: '产品负责人 Offer 审批', owner: '陈晨', status: '已完成', statusType: 'info', updatedAt: '2026-06-08 18:26' },
]

function resetFilters() {
  keyword.value = ''
  status.value = ''
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.header-actions { display: flex; gap: $spacing-sm; }

.stat-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $border-radius;
  padding: $spacing-lg $spacing-xl;
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: $border-radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.stat-label { font-size: 12px; color: $text-secondary; }
.stat-value { font-size: 20px; font-weight: 700; color: $text-primary; }
</style>
