<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">收件箱</h2>
        <p class="page-subtitle">招聘专员每日待办：审批、面试、录用通知与系统提醒</p>
      </div>
      <el-button @click="reload">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane :label="`全部${countSuffix('all')}`" name="all" />
      <el-tab-pane :label="`待审批${countSuffix('approval')}`" name="approval" />
      <el-tab-pane :label="`待面试${countSuffix('interview')}`" name="interview" />
      <el-tab-pane :label="`录用通知${countSuffix('offer')}`" name="offer" />
      <el-tab-pane :label="`消息${countSuffix('message')}`" name="message" />
    </el-tabs>

    <div class="data-card" v-loading="loading">
      <div v-if="filteredItems.length" class="inbox-list">
        <div v-for="item in filteredItems" :key="item.id" class="inbox-item" @click="go(item)">
          <div class="inbox-item-main">
            <el-tag size="small" :type="tagType(item.type)" effect="plain" class="inbox-tag">
              {{ inboxTypeLabel(item.type) }}
            </el-tag>
            <div class="inbox-title">{{ item.title }}</div>
          </div>
          <div class="inbox-meta">{{ formatTime(item.time) }}</div>
        </div>
      </div>

      <EmptyStateCta
        v-else
        :title="emptyConfig.title"
        :description="emptyConfig.description"
        :actions="emptyConfig.actions"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { inboxTypeLabel } from '@/constants/businessLabels'
import { loadInboxItems, type InboxItem } from '@/api/modules/workspace'

const router = useRouter()
const activeTab = ref('all')
const loading = ref(false)
const items = ref<InboxItem[]>([])

const filteredItems = computed(() => {
  if (activeTab.value === 'all') return items.value
  return items.value.filter(i => i.type === activeTab.value)
})

const tabCounts = computed(() => {
  const counts: Record<string, number> = { all: items.value.length }
  for (const item of items.value) {
    counts[item.type] = (counts[item.type] || 0) + 1
  }
  return counts
})

function countSuffix(tab: string) {
  const n = tabCounts.value[tab] || 0
  return n ? ` (${n})` : ''
}

const emptyConfig = computed(() => {
  const map: Record<string, { title: string; description: string; actions: { label: string; type?: 'primary' | 'default'; onClick: () => void }[] }> = {
    all: {
      title: '暂无待办',
      description: '当前没有需要您处理的事项，可前往招聘进展或候选人列表继续跟进',
      actions: [
        { label: '查看招聘进展', type: 'primary', onClick: () => router.push('/pipeline/board') },
        { label: '查看候选人', type: 'default', onClick: () => router.push('/pipeline/candidates') },
      ],
    },
    approval: {
      title: '暂无待审批事项',
      description: '招聘需求或录用通知审批通过后会出现在这里',
      actions: [
        { label: '招聘需求审批', type: 'primary', onClick: () => router.push('/planning/approvals/pending') },
      ],
    },
    interview: {
      title: '暂无待面试安排',
      description: '可在面试日历中安排或查看候选人面试',
      actions: [
        { label: '打开面试日历', type: 'primary', onClick: () => router.push('/pipeline/calendar') },
      ],
    },
    offer: {
      title: '暂无录用通知待办',
      description: '候选人通过面试后，可在此跟进录用通知审批与发送',
      actions: [
        { label: '录用通知列表', type: 'primary', onClick: () => router.push('/pipeline/offers') },
      ],
    },
    message: {
      title: '暂无系统消息',
      description: '渠道招聘、匹配评估等系统提醒会集中显示在这里',
      actions: [
        { label: '查看在招职位', type: 'default', onClick: () => router.push('/planning/jobs') },
      ],
    },
  }
  return map[activeTab.value] || map.all
})

function tagType(type: string) {
  const map: Record<string, string> = {
    approval: 'warning', interview: 'primary', offer: 'success', message: 'info',
  }
  return map[type] || 'info'
}

function formatTime(t?: string) {
  if (!t) return '刚刚'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function go(item: InboxItem) {
  router.push(item.path)
}

function onTabChange() { /* reactive filter */ }

async function reload() {
  loading.value = true
  try {
    items.value = await loadInboxItems()
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped lang="scss">
.inbox-list { display: flex; flex-direction: column; }
.inbox-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  &:hover { background: #f8fafc; }
  &:last-child { border-bottom: none; }
}
.inbox-item-main { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.inbox-tag { flex-shrink: 0; }
.inbox-title { font-weight: 500; color: #334155; }
.inbox-meta { font-size: 12px; color: #94a3b8; flex-shrink: 0; }
</style>
