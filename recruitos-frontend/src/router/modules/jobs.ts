import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

// v8: 在招岗位作为一级入口，映射到现有路由
const jobsRoutes: RouteRecordRaw[] = [
  {
    path: '/jobs',
    component: AppLayout,
    redirect: '/planning/jobs',
    meta: { title: '在招岗位', permission: 'planning' },
    children: [
      {
        path: '',
        name: 'JobsList',
        component: () => import('@/views/job/JobList.vue'),
        meta: { title: '全部岗位', permission: 'planning:job', icon: 'Briefcase' },
      },
    ],
  },
]

export default jobsRoutes
