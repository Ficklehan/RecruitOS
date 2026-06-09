<template>
  <div class="dashboard page-container page-stack">
    <header class="dashboard-header">
      <div>
        <h1 class="greeting">{{ greeting }}，{{ userName }}</h1>
        <p class="greeting-sub">
          今日待办 <strong>{{ pendingCount }}</strong> 项
          <span v-if="stats.pendingInterviews" class="greeting-meta">· 面试 {{ stats.pendingInterviews }} 场</span>
        </p>
      </div>
      <time class="welcome-date">{{ todayStr }}</time>
    </header>

    <!-- Bento Grid：大小卡片混排 -->
    <div class="bento-grid">
      <!-- 大统计卡：跨 2 列 -->
      <div class="bento-card bento-hero">
        <div class="hero-left">
          <div class="hero-label">本周招聘进度</div>
          <div class="hero-value">
            <span class="hero-num">{{ stats.weeklyOnboard }}</span>
            <span class="hero-unit">人入职</span>
          </div>
          <div class="hero-trend trend-up">
            <el-icon :size="14"><Top /></el-icon>
            较上周增长 20%
          </div>
        </div>
        <div class="hero-chart">
          <!-- 简易迷你折线图 -->
          <svg viewBox="0 0 200 80" class="mini-chart">
            <defs>
              <linearGradient id="chartGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#3B82F6" stop-opacity="0.15"/>
                <stop offset="100%" stop-color="#3B82F6" stop-opacity="0"/>
              </linearGradient>
            </defs>
            <path d="M0 60 L30 45 L60 52 L90 30 L120 38 L150 20 L180 25 L200 15 L200 80 L0 80 Z" fill="url(#chartGrad)"/>
            <polyline points="0,60 30,45 60,52 90,30 120,38 150,20 180,25 200,15" fill="none" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="200" cy="15" r="3" fill="#3B82F6"/>
          </svg>
        </div>
      </div>

      <!-- 小统计卡 x4 -->
      <div class="bento-card bento-stat" v-for="s in statCards" :key="s.label">
        <div class="stat-icon-sm" :style="{ background: s.bg, color: s.color }">
          <el-icon :size="18"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-label-sm">{{ s.label }}</div>
        <div class="stat-value-sm">{{ s.value }}</div>
        <div class="stat-trend-sm" :class="s.trend >= 0 ? 'trend-up' : 'trend-down'">
          {{ s.trend >= 0 ? '+' : '' }}{{ s.trend }}%
        </div>
      </div>

      <!-- 快捷入口：跨 2 列 -->
      <div class="bento-card bento-actions">
        <div class="card-title-row">
          <span class="card-title">快捷入口</span>
        </div>
        <div class="action-row">
          <div class="action-btn" v-for="a in quickActions" :key="a.label" @click="$router.push(a.path)">
            <div class="action-icon-sm" :style="{ background: a.bg, color: a.color }">
              <el-icon :size="18"><component :is="a.icon" /></el-icon>
            </div>
            <span>{{ a.label }}</span>
          </div>
        </div>
      </div>

      <!-- 最近活动 -->
      <div class="bento-card bento-activity">
        <div class="card-title-row">
          <span class="card-title">最近活动</span>
          <el-button type="primary" link size="small" @click="$router.push('/workspace/inbox')">全部</el-button>
        </div>
        <div class="activity-list">
          <div v-for="(act, i) in recentActivities" :key="i" class="activity-row">
            <div class="act-dot" :style="{ background: act.color }"></div>
            <div class="act-body">
              <p>{{ act.text }}</p>
              <span>{{ act.time }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 待办事项 -->
      <div class="bento-card bento-todo">
        <div class="card-title-row">
          <span class="card-title">待办事项</span>
          <el-button type="primary" link size="small" @click="$router.push('/workspace/inbox')">全部</el-button>
        </div>
        <div class="todo-list">
          <div v-for="(t, i) in todoList" :key="i" class="todo-row">
            <el-checkbox v-model="t.done" />
            <span :class="['todo-text', { done: t.done }]">{{ t.text }}</span>
            <span v-if="t.urgent && !t.done" class="todo-urgent">紧急</span>
          </div>
        </div>
      </div>

      <!-- 双漏斗：管人 + 找人 -->
      <div class="bento-card bento-dual-funnel">
        <div class="card-title-row">
          <span class="card-title">招聘双漏斗</span>
        </div>
        <div class="dual-funnel-grid">
          <div class="funnel-panel">
            <div class="funnel-panel-title">管人 · 在招候选人</div>
            <div class="funnel-mini">
              <div v-for="f in funnelPreview" :key="f.name" class="funnel-row">
                <span class="funnel-name">{{ f.name }}</span>
                <div class="funnel-bar-bg">
                  <div class="funnel-bar-fill" :style="{ width: f.pct + '%', background: f.color }"></div>
                </div>
                <span class="funnel-val">{{ f.count }}</span>
              </div>
            </div>
          </div>
          <div class="funnel-panel">
            <div class="funnel-panel-title">找人 · 平台招人</div>
            <div class="sourcing-cells">
              <div v-for="c in sourcingFunnel" :key="c.label" class="sourcing-cell">
                <span class="sourcing-num">{{ c.count }}</span>
                <span class="sourcing-label">{{ c.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 异常告警 -->
      <div v-if="alerts.length" class="bento-card bento-alerts">
        <div class="card-title-row">
          <span class="card-title">需关注</span>
        </div>
        <div class="alert-list">
          <div
            v-for="a in alerts"
            :key="a.id"
            class="alert-row"
            :class="`alert-${a.level}`"
            @click="$router.push(a.path)"
          >
            <el-icon><Warning /></el-icon>
            <span>{{ a.title }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, ref, onMounted } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { loadInboxItems, loadTodayInterviews } from '@/api/modules/workspace'
import {
  loadDashboardAlerts,
  loadDashboardSourcingFunnel,
  loadPipelineFunnel,
} from '@/api/modules/dashboard'
import { getTalentPool } from '@/api/modules/candidate'
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
  newDemands: 0,
  pendingInterviews: 0,
  weeklyOnboard: 0,
  totalTalents: 0,
})

const statCards = reactive([
  { label: '今日面试', value: 0, icon: 'Calendar', color: '#D97706', bg: '#FEF3C7', trend: 0 },
  { label: '待办', value: 0, icon: 'Document', color: '#3B82F6', bg: '#EFF6FF', trend: 0 },
  { label: '本周入职', value: 0, icon: 'Promotion', color: '#059669', bg: '#D1FAE5', trend: 0 },
  { label: '人才库', value: '0', icon: 'UserFilled', color: '#6B7280', bg: '#F1F5F9', trend: 0 },
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
const funnelPreview = ref<{ name: string; count: number; pct: number; color: string }[]>([])
const sourcingFunnel = ref<{ label: string; count: number }[]>([])
const alerts = ref<{ id: string; level: string; title: string; path: string }[]>([])

const funnelColors = ['#3B82F6', '#60A5FA', '#818CF8', '#A78BFA', '#059669']

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function loadDashboard() {
  try {
    const [inbox, todayInterviews, stages, sourcingCells, alertList, poolRes, notifRes] = await Promise.all([
      loadInboxItems().catch(() => []),
      loadTodayInterviews().catch(() => []),
      loadPipelineFunnel(),
      loadDashboardSourcingFunnel(),
      loadDashboardAlerts(),
      getTalentPool({ pageNum: 1, pageSize: 1 }).catch(() => ({ data: { total: 0 } })),
      getMyNotifications(8).catch(() => ({ data: [] })),
    ])

    alerts.value = alertList
    sourcingFunnel.value = sourcingCells

    pendingCount.value = inbox.length
    stats.pendingInterviews = todayInterviews.length
    stats.totalTalents = poolRes.data?.total || 0

    statCards[0].value = todayInterviews.length
    statCards[1].value = inbox.length
    statCards[3].value = (poolRes.data?.total || 0).toLocaleString()

    const funnelStages = stages || []
    if (funnelStages.length) {
      const max = Math.max(...funnelStages.map((s: any) => s.count || 0), 1)
      funnelPreview.value = funnelStages.map((s: any, i: number) => ({
        name: s.stageName,
        count: s.count || 0,
        pct: Math.round(((s.count || 0) / max) * 100),
        color: funnelColors[i % funnelColors.length],
      }))
      const last = funnelStages[funnelStages.length - 1]
      stats.weeklyOnboard = last?.count || 0
      statCards[2].value = last?.count || 0
    }

    const notifications = notifRes.data || []
    recentActivities.value = notifications.slice(0, 4).map((n: any) => ({
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

.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: $spacing-lg;
  width: 100%;
}

.greeting {
  font-size: 22px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.greeting-sub {
  font-size: 14px;
  color: $text-secondary;
  margin-top: 6px;

  strong {
    color: $primary-color;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
}

.greeting-meta {
  color: $text-placeholder;
}

.welcome-date {
  font-size: 13px;
  color: $text-secondary;
  white-space: nowrap;
  padding-top: 4px;
}

.bento-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 16px;
  width: 100%;
}

.bento-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $border-radius;
  padding: 20px;
}

.bento-hero {
  grid-column: span 6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 22px 24px;
  min-height: 148px;
}

.hero-label {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 6px;
}

.hero-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;
}

.hero-num {
  font-size: 36px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.hero-unit {
  font-size: 16px;
  font-weight: 500;
  color: $text-secondary;
}

.trend-up {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  font-weight: 500;
  color: $success-color;
}

.trend-down {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  font-weight: 500;
  color: $danger-color;
}

.hero-chart {
  width: 260px;
  flex-shrink: 0;
}

.mini-chart {
  width: 100%;
  height: auto;
}

.bento-stat {
  grid-column: span 3;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-icon-sm {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-label-sm {
  font-size: 12px;
  color: $text-secondary;
}

.stat-value-sm {
  font-size: 22px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: -0.02em;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-trend-sm {
  font-size: 12px;
  font-weight: 500;
}

.bento-actions {
  grid-column: span 6;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
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

.bento-activity {
  grid-column: span 6;
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

.bento-todo {
  grid-column: span 6;
}

.todo-list {
  display: flex;
  flex-direction: column;
}

.todo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;

  & + .todo-row { border-top: 1px solid $border-color-light; }
}

.todo-text {
  flex: 1;
  font-size: 13px;
  color: $text-regular;
  &.done { text-decoration: line-through; color: $text-placeholder; }
}

.todo-urgent {
  font-size: 11px;
  font-weight: 500;
  color: $danger-color;
  background: #FEE2E2;
  padding: 1px 7px;
  border-radius: $border-radius-full;
}

.bento-dual-funnel {
  grid-column: span 12;
}

.dual-funnel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.funnel-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 12px;
}

.sourcing-cells {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.sourcing-cell {
  padding: 14px 12px;
  background: $bg-muted;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-sm;
  text-align: center;
}

.sourcing-num {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
}

.sourcing-label {
  font-size: 12px;
  color: $text-secondary;
}

.bento-alerts {
  grid-column: span 12;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  &:hover { opacity: 0.9; }
}

.alert-danger { background: #fef2f2; color: #b91c1c; }
.alert-warning { background: #fffbeb; color: #b45309; }
.alert-info { background: #eff6ff; color: #1d4ed8; }

// 漏斗预览
.bento-funnel {
  grid-column: span 3;
}

.funnel-mini {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.funnel-row {
  display: grid;
  grid-template-columns: 48px 1fr 48px;
  align-items: center;
  gap: 10px;
}

.funnel-name {
  font-size: 12px;
  color: $text-secondary;
  text-align: right;
}

.funnel-bar-bg {
  height: 20px;
  background: $bg-muted;
  border-radius: 4px;
  overflow: hidden;
}

.funnel-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.funnel-val {
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
}

@media (max-width: 1200px) {
  .bento-hero,
  .bento-stat,
  .bento-actions,
  .bento-activity,
  .bento-todo {
    grid-column: span 6;
  }
}

@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
  }

  .bento-hero,
  .bento-stat,
  .bento-actions,
  .bento-activity,
  .bento-todo,
  .bento-dual-funnel,
  .bento-alerts {
    grid-column: span 12;
  }

  .action-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .dual-funnel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
