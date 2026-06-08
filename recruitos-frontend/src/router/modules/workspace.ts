import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const workspaceRoutes: RouteRecordRaw[] = [
  {
    path: '/workspace',
    component: AppLayout,
    redirect: '/workspace/inbox',
    meta: { title: '工作台', permission: 'workspace' },
    children: [
      {
        path: 'inbox',
        name: 'WorkspaceInbox',
        component: () => import('@/views/workspace/Inbox.vue'),
        meta: { title: '收件箱', permission: 'workspace:inbox', icon: 'Message' },
      },
      {
        path: 'today',
        name: 'WorkspaceToday',
        component: () => import('@/views/workspace/Today.vue'),
        meta: { title: '今日', permission: 'workspace:today', icon: 'Calendar' },
      },
      {
        path: 'dashboard',
        name: 'WorkspaceDashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '驾驶舱', permission: 'workspace:dashboard', icon: 'Odometer' },
      },
    ],
  },
]

export default workspaceRoutes
