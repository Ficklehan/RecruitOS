import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const dashboardRoutes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '数据概览', icon: 'Odometer' },
      },
    ],
  },
]

export default dashboardRoutes
