import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const analyticsRoutes: RouteRecordRaw[] = [
  {
    path: '/analytics',
    component: AppLayout,
    redirect: '/analytics/funnel',
    meta: { title: '数据分析', icon: 'DataAnalysis' },
    children: [
      {
        path: 'funnel',
        name: 'AnalyticsFunnel',
        component: () => import('@/views/analytics/AnalyticsFunnel.vue'),
        meta: { title: '招聘漏斗', icon: 'Filter' },
      },
      {
        path: 'roi',
        name: 'AnalyticsRoi',
        component: () => import('@/views/analytics/AnalyticsRoi.vue'),
        meta: { title: '渠道ROI', icon: 'Coin' },
      },
      {
        path: 'interviewer',
        name: 'AnalyticsInterviewer',
        component: () => import('@/views/analytics/AnalyticsInterviewer.vue'),
        meta: { title: '面试官效能', icon: 'UserFilled' },
      },
      {
        path: 'cycle',
        name: 'AnalyticsCycle',
        component: () => import('@/views/analytics/AnalyticsCycle.vue'),
        meta: { title: '招聘周期', icon: 'Timer' },
      },
    ],
  },
]

export default analyticsRoutes
