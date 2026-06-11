<template>
  <PageShell>
    <template #heading>
      <h1 class="text-2xl font-semibold tracking-tight text-foreground leading-tight">收件箱</h1>
      <p class="mt-1 text-sm text-muted-foreground">
        <template v-if="urgentCount > 0">
          <strong class="text-destructive font-semibold">{{ urgentCount }}</strong> 项需要你立即处理
        </template>
        <template v-else>暂无紧急事项</template>
      </p>
    </template>
    <template #actions>
      <RButton variant="outline" :disabled="loading" @click="reload">
        <RefreshCw class="mr-2 h-4 w-4" :class="{ 'animate-spin': loading }" />
        刷新
      </RButton>
    </template>

    <div class="inbox-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="inbox-tab"
        :class="{ active: activeTab === tab.key, 'has-count': tabCounts[tab.key] > 0 }"
        @click="activeTab = tab.key"
      >
        <span class="inbox-tab__label">{{ tab.label }}</span>
        <span v-if="tabCounts[tab.key]" class="inbox-tab__badge">{{ tabCounts[tab.key] }}</span>
      </button>
    </div>

    <div class="inbox-body">
      <template v-if="groupedItems.length">
        <div v-for="group in groupedItems" :key="group.label" class="inbox-group">
          <div class="inbox-group__header">
            <span class="inbox-group__label">{{ group.label }}</span>
            <span class="inbox-group__count">{{ group.items.length }}</span>
          </div>
          <div class="inbox-group__list">
            <div
              v-for="item in group.items"
              :key="item.id"
              class="inbox-card"
              :class="`inbox-card--${item.type}`"
              tabindex="0"
              @click="go(item)"
              @keydown.enter="go(item)"
            >
              <div class="inbox-card__accent"></div>
              <div class="inbox-card__icon">
                <component :is="typeIcon(item.type)" class="h-[18px] w-[18px]" />
              </div>
              <div class="inbox-card__body">
                <div class="inbox-card__title">{{ item.title }}</div>
                <div class="inbox-card__meta">
                  <span class="inbox-card__type-label">{{ typeLabel(item.type) }}</span>
                  <span class="inbox-card__dot">·</span>
                  <span>{{ formatTime(item.time) }}</span>
                </div>
              </div>
              <div class="inbox-card__action" @click.stop>
                <RButton size="sm" variant="outline" @click="go(item)">
                  {{ actionLabel(item.type) }}
                </RButton>
              </div>
            </div>
          </div>
        </div>
      </template>

      <EmptyStateCta
        v-else-if="!loading"
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
</PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/Layout/PageShell.vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  RefreshCw, MessageCircle, Calendar, CheckCircle, Ticket, Send, Bell,
} from 'lucide-vue-next'
import { RButton } from '@/components/ui'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import InterviewEvalDrawer from '@/components/interview/InterviewEvalDrawer.vue'
import { loadInboxItems, type InboxItem, type InboxItemType } from '@/api/modules/workspace'

const route = useRoute()
const router = useRouter()
const activeTab = ref('all')
const loading = ref(false)
const items = ref<InboxItem[]>([])
const feedbackDrawerVisible = ref(false)
const activeFeedbackInterview = ref<Record<string, unknown> | null>(null)

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'confirm', label: '待确认' },
  { key: 'interview', label: '面试' },
  { key: 'hiring', label: '录用' },
  { key: 'approval', label: '审批' },
  { key: 'evolution', label: '建议' },
]

const TAB_MAP: Record<string, InboxItemType[] | 'all'> = {
  all: 'all',
  confirm: ['confirm'],
  interview: ['interview', 'feedback'],
  hiring: ['offer', 'onboard'],
  evolution: ['evolution'],
  approval: ['approval'],
}

const filteredItems = computed(() => {
  const mapping = TAB_MAP[activeTab.value]
  if (mapping === 'all') return items.value.filter(i => i.type !== 'message')
  if (Array.isArray(mapping)) return items.value.filter(i => mapping.includes(i.type))
  return items.value
})

const tabCounts = computed(() => {
  const counts: Record<string, number> = { all: 0 }
  for (const item of items.value) {
    if (item.type === 'message') continue
    counts.all++
    for (const [tab, types] of Object.entries(TAB_MAP)) {
      if (tab === 'all' || !Array.isArray(types)) continue
      if (types.includes(item.type)) counts[tab] = (counts[tab] || 0) + 1
    }
  }
  return counts
})

const urgentCount = computed(() =>
  items.value.filter(i => ['confirm', 'feedback', 'approval'].includes(i.type)).length
)

const TYPE_GROUPS: Record<string, { label: string; types: InboxItemType[] }> = {
  urgent: { label: '需要立即处理', types: ['confirm', 'feedback', 'approval'] },
  upcoming: { label: '即将进行', types: ['interview', 'offer', 'onboard'] },
  info: { label: '参考信息', types: ['evolution', 'message'] },
}

const groupedItems = computed(() => {
  const groups: { label: string; items: InboxItem[] }[] = []
  for (const [, group] of Object.entries(TYPE_GROUPS)) {
    const groupItems = filteredItems.value.filter(i => group.types.includes(i.type))
    if (groupItems.length) groups.push({ label: group.label, items: groupItems })
  }
  return groups
})

function typeIcon(type: InboxItemType) {
  return ({
    confirm: MessageCircle,
    approval: CheckCircle,
    interview: Calendar,
    feedback: MessageCircle,
    offer: Ticket,
    onboard: Send,
    evolution: Bell,
    message: Bell,
  } as Record<string, unknown>)[type] || Bell
}

function typeLabel(type: InboxItemType) {
  return ({
    confirm: '待确认',
    approval: '审批',
    interview: '面试',
    feedback: '反馈',
    offer: '录用',
    onboard: '入职',
    evolution: '建议',
    message: '通知',
  } as Record<string, string>)[type] || '待办'
}

function actionLabel(type: InboxItemType) {
  return ({
    confirm: '去确认',
    approval: '去审批',
    interview: '查看详情',
    feedback: '提交反馈',
    offer: '去处理',
    onboard: '去跟进',
    evolution: '查看建议',
    message: '查看',
  } as Record<string, string>)[type] || '查看'
}

function formatTime(t?: string) {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  if (diffHour < 24) return `${diffHour} 小时前`
  if (diffDay < 7) return `${diffDay} 天前`
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

async function onFeedbackSubmitted() { await reload() }
async function reload() { loading.value = true; try { items.value = await loadInboxItems() } finally { loading.value = false } }

const emptyConfig = computed(() => {
  const map: Record<string, { title: string; description: string; actions: { label: string; type?: 'primary' | 'default'; onClick: () => void }[] }> = {
    all: { title: '暂无待办', description: '当前没有需要您处理的事项', actions: [{ label: '查看在招职位', type: 'primary', onClick: () => router.push('/planning/jobs') }] },
    confirm: { title: '暂无待确认事项', description: '平台招人任务中需要你确认的事项会出现在这里', actions: [{ label: '查看在招职位', type: 'primary', onClick: () => router.push('/planning/jobs') }] },
    interview: { title: '暂无面试相关待办', description: '已安排的面试与待提交反馈的面试会出现在这里', actions: [{ label: '打开面试日历', type: 'primary', onClick: () => router.push('/pipeline/calendar') }] },
    hiring: { title: '暂无录用相关待办', description: '录用通知的审批与发送会出现在这里', actions: [{ label: '录用通知列表', type: 'primary', onClick: () => router.push('/pipeline/offers') }] },
    evolution: { title: '暂无招人方式建议', description: '系统生成的招人方式优化建议会显示在这里', actions: [{ label: '查看建议列表', type: 'primary', onClick: () => router.push('/planning/evolution/proposals') }] },
    approval: { title: '暂无待审批事项', description: '招聘需求或录用通知审批会出现在这里', actions: [{ label: '审批列表', type: 'primary', onClick: () => router.push('/planning/approvals/pending') }] },
  }
  return map[activeTab.value] || map.all
})

onMounted(() => { if (route.query.tab && typeof route.query.tab === 'string') activeTab.value = route.query.tab; reload() })
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.header-actions { display: flex; gap: $spacing-sm; }
.urgent-num { color: $danger-color; font-weight: 700; }

/* ── Tab 栏 ─────────────────────────── */
.inbox-tabs {
  display: flex;
  gap: $spacing-xs;
  border-bottom: 1px solid var(--r-divider);
  padding-bottom: 0;
}

.inbox-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: $text-body;
  font-weight: $font-weight-medium;
  color: $text-secondary;
  border-bottom: 2px solid transparent;
  transition: color $transition-fast, border-color $transition-fast;
  margin-bottom: -1px;

  &:hover { color: $text-primary; }
  &.active {
    color: $primary-color;
    border-bottom-color: $primary-color;
    font-weight: $font-weight-semibold;
  }
}

.inbox-tab__badge {
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  padding: 2px 6px;
  border-radius: $border-radius-full;
  background: $bg-muted;
  color: $text-secondary;

  .has-count & {
    background: $primary-lighter;
    color: $primary-color;
  }
}

/* ── 分组 ───────────────────────────── */
.inbox-group {
  margin-bottom: $spacing-xl;
}

.inbox-group__header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.inbox-group__label {
  font-size: $text-caption;
  font-weight: $font-weight-semibold;
  color: $text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.inbox-group__count {
  font-size: 11px;
  font-weight: 600;
  color: $text-placeholder;
  background: $bg-muted;
  padding: 1px 6px;
  border-radius: $border-radius-full;
}

/* ── 卡片 ───────────────────────────── */
.inbox-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-base $spacing-lg;
  background: $bg-card;
  border: none;
  border-radius: $border-radius-sm;
  margin-bottom: $spacing-sm;
  cursor: pointer;
  transition: border-color $transition-fast, box-shadow $transition-fast, transform $transition-fast;

  &:hover {
    border-color: $primary-light;
    box-shadow: $shadow-sm;
    transform: translateY(-1px);
  }

  &:focus-visible {
    outline: 2px solid $primary-color;
    outline-offset: 2px;
  }
}

.inbox-card__accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: $border-radius-sm 0 0 $border-radius-sm;
}

.inbox-card--confirm .inbox-card__accent { background: $primary-color; }
.inbox-card--feedback .inbox-card__accent { background: $warning-color; }
.inbox-card--approval .inbox-card__accent { background: $warning-color; }
.inbox-card--interview .inbox-card__accent { background: $status-interviewing; }
.inbox-card--offer .inbox-card__accent { background: $success-color; }
.inbox-card--onboard .inbox-card__accent { background: $status-hired; }
.inbox-card--evolution .inbox-card__accent { background: $info-color; }
.inbox-card--message .inbox-card__accent { background: $border-color; }

.inbox-card__icon {
  width: 36px;
  height: 36px;
  border-radius: $border-radius-xs;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: $bg-muted;
  color: $text-secondary;
}

.inbox-card--confirm .inbox-card__icon { background: $primary-lighter; color: $primary-color; }
.inbox-card--feedback .inbox-card__icon { background: $warning-lighter; color: $warning-color; }
.inbox-card--approval .inbox-card__icon { background: $warning-lighter; color: $warning-color; }
.inbox-card--interview .inbox-card__icon { background: #EDE9FE; color: $status-interviewing; }
.inbox-card--offer .inbox-card__icon { background: $success-lighter; color: $success-color; }
.inbox-card--onboard .inbox-card__icon { background: $success-lighter; color: $status-hired; }

.inbox-card__body {
  flex: 1;
  min-width: 0;
}

.inbox-card__title {
  font-size: $text-body;
  font-weight: $font-weight-medium;
  color: $text-primary;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inbox-card__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
  font-size: $text-caption;
  color: $text-secondary;
}

.inbox-card__type-label {
  font-weight: $font-weight-medium;
}

.inbox-card__dot {
  color: $border-color;
}

.inbox-card__action {
  flex-shrink: 0;
}
</style>
