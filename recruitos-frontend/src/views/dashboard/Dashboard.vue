<template>
  <div class="dashboard page-container page-stack">
    <header class="page-header">
      <div>
        <h2 class="page-title">{{ greeting }}，{{ userName }}</h2>
        <p class="page-subtitle">
          今日待办 <strong class="dashboard-highlight">{{ pendingCount }}</strong> 项
          <span v-if="stats.pendingInterviews"> · 面试 <strong class="dashboard-highlight">{{ stats.pendingInterviews }}</strong> 场</span>
          <span class="dashboard-date"> {{ todayStr }}</span>
        </p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/workspace/inbox')">收件箱</el-button>
        <el-button type="primary" @click="$router.push('/planning/jobs')">去在招职位</el-button>
      </div>
    </header>

    <div class="stat-row">
      <div class="stat-card" v-for="s in statCards" :key="s.label">
        <div class="stat-card-icon" :style="{ background: s.bg, color: s.color }">
          <el-icon :size="18"><component :is="s.icon" /></el-icon>
        </div>
        <div>
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.value }}</div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="dashboard-main data-card">
        <div class="card-title-row">
          <span class="card-title">待办事项</span>
          <el-button type="primary" link size="small" @click="$router.push('/workspace/inbox')">查看收件箱</el-button>
        </div>

        <div v-if="todoList.length" class="todo-list">
          <div v-for="(t, i) in todoList" :key="i" class="todo-row" @click="$router.push('/workspace/inbox')">
            <div class="todo-text">{{ t.text }}</div>
            <span v-if="t.urgent" class="todo-urgent">紧急</span>
          </div>
        </div>

        <div v-else class="dashboard-empty">暂无待办</div>
      </section>

      <aside class="dashboard-side">
        <section class="data-card">
          <div class="card-title-row">
            <span class="card-title">快捷入口</span>
          </div>
          <div class="action-row">
            <div class="action-btn" v-for="a in quickActions" :key="a.label" @click="$router.push(a.path)">
              <div class="action-icon-sm" :style="{ background: a.bg, color: a.color }">
                <el-icon :size="16"><component :is="a.icon" /></el-icon>
              </div>
              <span>{{ a.label }}</span>
            </div>
          </div>
        </section>

        <section class="data-card">
          <div class="card-title-row">
            <span class="card-title">最近活动</span>
            <el-button type="primary" link size="small" @click="$router.push('/workspace/inbox')">全部</el-button>
          </div>

          <div v-if="recentActivities.length" class="activity-list">
            <div v-for="(act, i) in recentActivities" :key="i" class="activity-row">
              <div class="act-dot" :style="{ background: act.color }"></div>
              <div class="act-body">
                <p>{{ act.text }}</p>
                <span>{{ act.time }}</span>
              </div>
            </div>
          </div>

          <div v-else class="dashboard-empty">暂无动态</div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { loadInboxItems, loadTodayInterviews } from '@/api/modules/workspace'
import { getMyNotifications } from '@/api/modules/notification'

const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.realName || '管理员')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayStr = computed(() => {
  const d = new Date()
  const w = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}月${d.getDate()}日 星期${w[d.getDay()]}`
})

const pendingCount = ref(0)

const stats = reactive({
  pendingInterviews: 0,
  weeklyOnboard: 0,
  totalTalents: 0,
})

const statCards = reactive([
  { label: '今日面试', value: 0, icon: 'Calendar', color: '#D97706', bg: '#FEF3C7' },
  { label: '待办', value: 0, icon: 'Document', color: '#3B82F6', bg: '#EFF6FF' },
  { label: '本周入职', value: 0, icon: 'Promotion', color: '#059669', bg: '#D1FAE5' },
  { label: '人才库', value: '0', icon: 'UserFilled', color: '#6B7280', bg: '#F1F5F9' },
])

const quickActions = [
  { label: '招聘进展', icon: 'Grid', path: '/pipeline/board', color: '#3B82F6', bg: '#EFF6FF' },
  { label: '创建需求', icon: 'Plus', path: '/planning/demands/create', color: '#2563EB', bg: '#EFF6FF' },
  { label: '候选人', icon: 'User', path: '/pipeline/candidates', color: '#059669', bg: '#D1FAE5' },
  { label: '安排面试', icon: 'Calendar', path: '/pipeline/calendar', color: '#D97706', bg: '#FEF3C7' },
  { label: '人才库', icon: 'UserFilled', path: '/talent/pool', color: '#6B7280', bg: '#F1F5F9' },
  { label: '渠道招聘', icon: 'ChatDotRound', path: '/planning/jobs', color: '#0891B2', bg: '#CFFAFE' },
]

const recentActivities = ref<{ text: string; time: string; color: string }[]>([])
const todoList = reactive<{ text: string; done: boolean; urgent: boolean }[]>([])

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function loadDashboard() {
  try {
    const [inbox, todayInterviews, notifRes] = await Promise.all([
      loadInboxItems().catch(() => []),
      loadTodayInterviews().catch(() => []),
      getMyNotifications(6).catch(() => ({ data: [] })),
    ])

    pendingCount.value = inbox.length
    stats.pendingInterviews = todayInterviews.length

    statCards[0].value = todayInterviews.length
    statCards[1].value = inbox.length
    statCards[2].value = stats.weeklyOnboard
    statCards[3].value = stats.totalTalents.toLocaleString()

    const notifications = notifRes.data || []
    recentActivities.value = notifications.slice(0, 5).map((n: any) => ({
      text: n.title || n.content || '系统通知',
      time: formatTime(n.createdAt),
      color: '#3B82F6',
    }))

    todoList.splice(0, todoList.length)
    inbox.slice(0, 5).forEach((item) => {
      todoList.push({ text: item.title, done: false, urgent: item.type === 'approval' })
    })
  } catch {
    // keep defaults
  }
}

onMounted(loadDashboard)
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';
.header-actions { display: flex; gap: $spacing-sm; }
.dashboard-highlight { color: $primary-color; font-weight: 600; }
.dashboard-date { color: $text-placeholder; margin-left: $spacing-sm; }

.stat-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $border-radius;
  padding: $spacing-lg $spacing-xl;
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.stat-card-icon {
  width: 36px;
  height: 36px;
  border-radius: $border-radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: $spacing-section;
  align-items: start;
}

.dashboard-main .card-title-row,
.dashboard-side .card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-lg;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-sm;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: background-color $transition-fast, border-color $transition-fast;

  &:hover {
    background: $bg-muted;
    border-color: $border-color;
  }

  span {
    font-size: 13px;
    font-weight: 500;
    color: $text-regular;
  }
}

.action-icon-sm {
  width: 32px;
  height: 32px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.activity-list {
  display: flex;
  flex-direction: column;
}

.activity-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 9px 0;

  & + .activity-row { border-top: 1px solid $border-color-light; }
}

.act-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 7px;
  flex-shrink: 0;
}

.act-body {
  p { font-size: 13px; color: $text-regular; line-height: 1.5; margin-bottom: 1px; }
  span { font-size: 12px; color: $text-placeholder; }
}

.todo-list { display: flex; flex-direction: column; }
.todo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  cursor: pointer;

  & + .todo-row { border-top: 1px solid $border-color-light; }
}

.todo-text { font-size: 13px; color: $text-regular; }
.todo-urgent {
  font-size: 11px;
  font-weight: 600;
  color: $danger-color;
  background: $danger-lighter;
  padding: 1px 7px;
  border-radius: $border-radius-full;
}

.dashboard-empty {
  color: $text-placeholder;
  font-size: 13px;
  padding: $spacing-xl 0 $spacing-sm;
}

@media (max-width: 960px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
