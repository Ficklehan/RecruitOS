import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const onboardRoutes: RouteRecordRaw[] = [
  {
    path: '/onboard',
    component: AppLayout,
    redirect: '/onboard/list',
    meta: { title: '入职管理', icon: 'Promotion' },
    children: [
      {
        path: 'list',
        name: 'OnboardList',
        component: () => import('@/views/onboard/OnboardList.vue'),
        meta: { title: '入职列表', icon: 'List' },
      },
      {
        path: 'task',
        name: 'OnboardTask',
        component: () => import('@/views/onboard/OnboardTask.vue'),
        meta: { title: '入职任务', icon: 'Finished' },
      },
    ],
  },
]

export default onboardRoutes
