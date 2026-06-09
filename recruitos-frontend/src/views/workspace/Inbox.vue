<template>
  <div class="page-container page-stack">
    <div class="page-header">
      <div>
        <h2 class="page-title">收件箱</h2>
        <p class="page-subtitle">今天需要你处理的事项：确认联系、面试、录用与招人方式建议</p>
      </div>
      <el-button @click="reload">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane :label="tabLabel('all')" name="all" />
      <el-tab-pane :label="tabLabel('confirm')" name="confirm" />
      <el-tab-pane :label="tabLabel('interview')" name="interview" />
      <el-tab-pane :label="tabLabel('hiring')" name="hiring" />
      <el-tab-pane :label="tabLabel('evolution')" name="evolution" />
      <el-tab-pane :label="tabLabel('approval')" name="approval" />
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

    <InterviewEvalDrawer
      v-model="feedbackDrawerVisible"
      :interview="activeFeedbackInterview"
      @submitted="onFeedbackSubmitted"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import InterviewEvalDrawer from '@/components/interview/InterviewEvalDrawer.vue'
import { inboxTabLabel, inboxTypeLabel } from '@/constants/businessLabels'
import { INBOX_TAB_TYPES, loadInboxItems, type InboxItem } from '@/api/modules/workspace'

const route = useRoute()
const router = useRouter()
const activeTab = ref('all')
const loading = ref(false)
const items = ref<InboxItem[]>([])
const feedbackDrawerVisible = ref(false)
const activeFeedbackInterview = ref<Record<string, unknown> | null>(null)

const filteredItems = computed(() => {
  const mapping = INBOX_TAB_TYPES[activeTab.value]
  if (mapping === 'all') {
    return items.value.filter(i => i.type !== 'message')
  }
  if (Array.isArray(mapping)) {
    return items.value.filter(i => mapping.includes(i.type))
  }
  return items.value
})

const tabCounts = computed(() => {
  const counts: Record<string, number> = { all: 0 }
  for (const item of items.value) {
    if (item.type === 'message') continue
    counts.all = (counts.all || 0) + 1
    for (const [tab, types] of Object.entries(INBOX_TAB_TYPES)) {
      if (tab === 'all' || !Array.isArray(types)) continue
      if (types.includes(item.type)) {
        counts[tab] = (counts[tab] || 0) + 1
      }
    }
  }
  return counts
})

function tabLabel(tab: string) {
  const n = tabCounts.value[tab] || 0
  const base = inboxTabLabel(tab)
  return n ? `${base} (${n})` : base
}

const emptyConfig = computed(() => {
  const map: Record<string, { title: string; description: string; actions: { label: string; type?: 'primary' | 'default'; onClick: () => void }[] }> = {
    all: {
      title: '暂无待办',
      description: '当前没有需要您处理的事项。可前往在招职位工作台继续找人或跟进在招候选人。',
      actions: [
        { label: '查看在招职位', type: 'primary', onClick: () => router.push('/planning/jobs') },
      ],
    },
    confirm: {
      title: '暂无待你确认的事项',
      description: '平台招人任务中，需要你确认首次联系或纳入候选人的事项会出现在这里。',
      actions: [
        { label: '查看在招职位', type: 'primary', onClick: () => router.push('/planning/jobs') },
      ],
    },
    interview: {
      title: '暂无待面试或待反馈',
      description: '已安排的面试与待提交反馈的面试会出现在这里',
      actions: [
        { label: '打开面试日历', type: 'primary', onClick: () => router.push('/pipeline/calendar') },
      ],
    },
    hiring: {
      title: '暂无录用相关待办',
      description: '候选人通过面试后，录用通知的审批与发送会出现在这里',
      actions: [
        { label: '录用通知列表', type: 'primary', onClick: () => router.push('/pipeline/offers') },
      ],
    },
    evolution: {
      title: '暂无招人方式建议',
      description: '系统根据招聘数据生成的招人方式优化建议会集中显示在这里',
      actions: [
        { label: '查看建议列表', type: 'primary', onClick: () => router.push('/planning/evolution/proposals') },
      ],
    },
    approval: {
      title: '暂无待审批事项',
      description: '招聘需求或录用通知审批通过后会出现在这里',
      actions: [
        { label: '招聘需求审批', type: 'primary', onClick: () => router.push('/planning/approvals/pending') },
      ],
    },
  }
  return map[activeTab.value] || map.all
})

function tagType(type: string) {
  const map: Record<string, string> = {
    confirm: 'primary',
    approval: 'warning',
    interview: 'primary',
    feedback: 'warning',
    offer: 'success',
    onboard: 'success',
    evolution: 'info',
    message: 'info',
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
  if (item.type === 'feedback' && item.interview) {
    activeFeedbackInterview.value = item.interview
    feedbackDrawerVisible.value = true
    return
  }
  router.push(item.path)
}

async function onFeedbackSubmitted() {
  await reload()
}

async function reload() {
  loading.value = true
  try {
    items.value = await loadInboxItems()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.tab && typeof route.query.tab === 'string') {
    activeTab.value = route.query.tab
  }
  reload()
})
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
