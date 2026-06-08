import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const insightRoutes: RouteRecordRaw[] = [
  {
    path: '/insight',
    component: AppLayout,
    redirect: '/insight/funnel',
    meta: { title: '数据洞察', permission: 'insight' },
    children: [
      {
        path: 'funnel',
        name: 'InsightFunnel',
        component: () => import('@/views/analytics/AnalyticsFunnel.vue'),
        meta: { title: '招聘漏斗', permission: 'insight:funnel', icon: 'Filter' },
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
        meta: { title: '渠道ROI', permission: 'insight:roi', icon: 'Coin' },
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
