<template>
  <div class="dashboard">
    <!-- 欢迎区 -->
    <div class="welcome-row">
      <div class="welcome-text">
        <h1 class="greeting">{{ greeting }}，{{ userName }}</h1>
        <p class="greeting-sub">今天有 <strong>{{ pendingCount }}</strong> 项待处理</p>
      </div>
      <div class="welcome-date">{{ todayStr }}</div>
    </div>

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

      <!-- 招聘漏斗预览 -->
      <div class="bento-card bento-funnel">
        <div class="card-title-row">
          <span class="card-title">招聘漏斗</span>
          <el-button type="primary" link size="small" @click="$router.push('/insight/funnel')">详情</el-button>
        </div>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { loadInboxItems, loadTodayInterviews } from '@/api/modules/workspace'
import { getFunnelData } from '@/api/modules/analytics'
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
  { label: '创建需求', icon: 'Plus', path: '/planning/demands/create', color: '#6366F1', bg: '#EEF2FF' },
  { label: '候选人', icon: 'User', path: '/pipeline/candidates', color: '#059669', bg: '#D1FAE5' },
  { label: '安排面试', icon: 'Calendar', path: '/pipeline/calendar', color: '#D97706', bg: '#FEF3C7' },
  { label: '人才库', icon: 'UserFilled', path: '/talent/pool', color: '#6B7280', bg: '#F1F5F9' },
  { label: '渠道招聘', icon: 'ChatDotRound', path: '/planning/jobs', color: '#0891B2', bg: '#CFFAFE' },
]

const recentActivities = ref<{ text: string; time: string; color: string }[]>([])
const todoList = reactive<{ text: string; done: boolean; urgent: boolean }[]>([])
const funnelPreview = ref<{ name: string; count: number; pct: number; color: string }[]>([])

const funnelColors = ['#3B82F6', '#60A5FA', '#818CF8', '#A78BFA', '#059669']

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function loadDashboard() {
  try {
    const [inbox, todayInterviews, funnelRes, poolRes, notifRes] = await Promise.all([
      loadInboxItems().catch(() => []),
      loadTodayInterviews().catch(() => []),
      getFunnelData().catch(() => ({ data: null })),
      getTalentPool({ pageNum: 1, pageSize: 1 }).catch(() => ({ data: { total: 0 } })),
      getMyNotifications(8).catch(() => ({ data: [] })),
    ])

    pendingCount.value = inbox.length
    stats.pendingInterviews = todayInterviews.length
    stats.totalTalents = poolRes.data?.total || 0

    statCards[0].value = todayInterviews.length
    statCards[1].value = inbox.length
    statCards[3].value = (poolRes.data?.total || 0).toLocaleString()

    const stages = funnelRes.data?.stages || []
    if (stages.length) {
      const max = Math.max(...stages.map((s: any) => s.count || 0), 1)
      funnelPreview.value = stages.map((s: any, i: number) => ({
        name: s.stageName,
        count: s.count || 0,
        pct: Math.round(((s.count || 0) / max) * 100),
        color: funnelColors[i % funnelColors.length],
      }))
      const last = stages[stages.length - 1]
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

.dashboard {
  width: 100%;
}

// ── 欢迎 ────────────────────────────────
.welcome-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.greeting {
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: -0.02em;
}

.greeting-sub {
  font-size: 14px;
  color: $text-secondary;
  margin-top: 2px;
  strong { color: $primary-color; font-weight: 600; }
}

.welcome-date {
  font-size: 13px;
  color: $text-placeholder;
}

// ── Bento Grid ──────────────────────────
.bento-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
}

.bento-card {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 20px;
}

// 大统计卡：跨 3 列
.bento-hero {
  grid-column: span 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
  padding: 24px;
  min-height: 160px;
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
  font-size: 40px;
  font-weight: 800;
  color: $text-primary;
  line-height: 1;
  letter-spacing: -0.03em;
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

// 小统计卡
.bento-stat {
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
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat-trend-sm {
  font-size: 12px;
  font-weight: 500;
}

// 快捷入口：跨 3 列
.bento-actions {
  grid-column: span 3;
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
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $bg-muted;
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

// 活动列表
.bento-activity {
  grid-column: span 3;
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

// 待办
.bento-todo {
  grid-column: span 3;
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

// ── 响应式 ──────────────────────────────
@media (max-width: 1200px) {
  .bento-grid { grid-template-columns: repeat(3, 1fr); }
  .bento-hero, .bento-actions, .bento-activity, .bento-todo, .bento-funnel { grid-column: span 3; }
}

@media (max-width: 768px) {
  .bento-grid { grid-template-columns: repeat(2, 1fr); }
  .bento-hero, .bento-actions, .bento-activity, .bento-todo, .bento-funnel { grid-column: span 2; }
}

@media (max-width: 500px) {
  .bento-grid { grid-template-columns: 1fr; }
  .bento-hero, .bento-actions, .bento-activity, .bento-todo, .bento-funnel { grid-column: span 1; }
  .action-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
