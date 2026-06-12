import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const insightRoutes: RouteRecordRaw[] = [
  {
    path: '/insight',
    component: AppLayout,
    redirect: '/insight/alerts',
    meta: { title: '洞察', permission: 'insight' },
    children: [
      // v8: AI主动推送 三级洞察
      {
        path: 'alerts',
        name: 'InsightAlerts',
        component: () => import('@/views/insight/InsightAlerts.vue'),
        meta: { title: '需要处理', permission: 'insight:funnel', icon: 'AlertTriangle' },
        props: { severity: 'CRITICAL' },
      },
      {
        path: 'attention',
        name: 'InsightAttention',
        component: () => import('@/views/insight/InsightAlerts.vue'),
        meta: { title: '值得关注', permission: 'insight:funnel', icon: 'Eye' },
        props: { severity: 'WARNING' },
      },
      {
        path: 'observe',
        name: 'InsightObserve',
        component: () => import('@/views/insight/InsightAlerts.vue'),
        meta: { title: '长期观察', permission: 'insight:funnel', icon: 'TrendingUp' },
        props: { severity: 'INFO' },
      },
      // 传统分析
      {
        path: 'funnel',
        name: 'InsightFunnel',
        component: () => import('@/views/analytics/AnalyticsFunnel.vue'),
        meta: { title: '招聘漏斗', permission: 'insight:funnel', icon: 'Funnel' },
      },
      {
        path: 'cycle',
        name: 'InsightCycle',
        component: () => import('@/views/analytics/AnalyticsCycle.vue'),
        meta: { title: '招聘周期', permission: 'insight:cycle', icon: 'Timer' },
      },
      {
        path: 'roi',
        name: 'InsightRoi',
        component: () => import('@/views/analytics/AnalyticsRoi.vue'),
        meta: { title: '渠道ROI', permission: 'insight:roi', icon: 'Coins' },
      },
      {
        path: 'interviewer',
        name: 'InsightInterviewer',
        component: () => import('@/views/analytics/AnalyticsInterviewer.vue'),
        meta: { title: '面试官效能', permission: 'insight:interviewer', icon: 'User' },
      },
    ],
  },
]

export default insightRoutes
